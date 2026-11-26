package sanch3zss.raven.feature.server.holyworld;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.misc.ElytraSwapModule;
import sanch3zss.raven.feature.module.render.projectile.ProjectilePredictionModule;
import sanch3zss.raven.ui.menu.card.ModuleCard;
import sanch3zss.raven.ui.menu.layout.MenuTabElement;

public final class HolyWorldFeatureControl {
    private static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("liteapi", "feature-control");
    private static final CustomPacketPayload.Type<FeaturePayload> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    private static final StreamCodec<FriendlyByteBuf, FeaturePayload> CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeBytes(payload.data()), buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new FeaturePayload(data);
            });
    private static final long COOLDOWN_MS = 10_000L;
    private static final long TIMEOUT_MS = 8_000L;

    private final Set<Module> restoreOnLeave = ConcurrentHashMap.newKeySet();
    private volatile Set<Module> blocked = Set.of();
    private volatile Set<Module> hostBlocked = Set.of();
    private String pendingId;
    private long lastRequest;
    private boolean checkOnJoin;
    private boolean channelAvailable;
    private boolean hostChecked;
    private ClientLevel lastLevel;

    public void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        ClientPlayNetworking.registerGlobalReceiver(TYPE,
                (payload, context) -> context.client().execute(() -> receive(payload.data())));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            reset();
            applyHostPolicy(client.getCurrentServer());
            this.checkOnJoin = true;
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    public boolean isBlocked(Module module) {
        return this.blocked.contains(module) || this.hostBlocked.contains(module);
    }

    public void rememberRequestedActivation(Module module) {
        if (isBlocked(module)) {
            this.restoreOnLeave.add(module);
        }
    }

    public boolean shouldSaveEnabled(Module module) {
        return module.isState() || this.restoreOnLeave.contains(module);
    }

    private void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (!this.hostChecked) {
            applyHostPolicy(client.getCurrentServer());
        }
        long now = System.currentTimeMillis();
        if (this.pendingId != null && now - this.lastRequest >= TIMEOUT_MS) {
            this.pendingId = null;
            Raven.LOGGER.debug("HolyWorld feature check timed out");
        }
        boolean available = ClientPlayNetworking.canSend(TYPE);
        if (client.level != this.lastLevel || (available && !this.channelAvailable)) {
            this.checkOnJoin = true;
        }
        this.lastLevel = client.level;
        if (this.checkOnJoin) {
            this.checkOnJoin = !request(client);
        }
        this.channelAvailable = available;
    }

    private boolean request(Minecraft client) {
        if (client.player == null || client.level == null || client.isLocalServer()) {
            return true;
        }
        if (!ClientPlayNetworking.canSend(TYPE) && !isHolyWorld(client.getCurrentServer())) {
            return true;
        }
        if (this.pendingId != null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastRequest < COOLDOWN_MS) {
            return false;
        }
        Map<String, Module> features = features();
        JsonObject payload = new JsonObject();
        payload.addProperty("client", "raven");
        JsonArray featureNames = new JsonArray();
        features.keySet().forEach(featureNames::add);
        payload.add("features", featureNames);
        JsonObject request = new JsonObject();
        this.pendingId = UUID.randomUUID().toString();
        request.addProperty("id", this.pendingId);
        request.addProperty("method", "checkFeatures");
        request.add("payload", payload);
        this.lastRequest = now;
        try {
            ClientPlayNetworking.send(new FeaturePayload(request.toString().getBytes(StandardCharsets.UTF_8)));
            return true;
        } catch (RuntimeException exception) {
            this.pendingId = null;
            Raven.LOGGER.warn("Unable to send HolyWorld feature check", exception);
            return true;
        }
    }

    private void receive(byte[] data) {
        if (this.pendingId == null) {
            return;
        }
        JsonObject response;
        try {
            JsonElement parsed = JsonParser.parseString(new String(data, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                return;
            }
            response = parsed.getAsJsonObject();
            if (!this.pendingId.equals(response.get("id").getAsString())) {
                return;
            }
            if (!response.get("ok").getAsBoolean()) {
                this.pendingId = null;
                this.checkOnJoin = false;
                String error = response.has("error") ? response.get("error").getAsString() : "неизвестная ошибка";
                Raven.LOGGER.debug("HolyWorld feature check returned error {}", error);
                return;
            }
            JsonArray blocklist = response.getAsJsonObject("payload").getAsJsonArray("blocklist");
            Map<String, Module> features = features();
            Set<Module> serverBlocked = new HashSet<>();
            for (JsonElement entry : blocklist) {
                Module module = features.get(entry.getAsString());
                if (module != null) {
                    serverBlocked.add(module);
                }
            }
            this.pendingId = null;
            this.checkOnJoin = false;
            applyBlocklist(serverBlocked);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("LiteApi успешно отключил все запрещённые функции, удачной игры!"));
            }
        } catch (RuntimeException exception) {
            this.pendingId = null;
            this.checkOnJoin = false;
            Raven.LOGGER.warn("Invalid HolyWorld feature check response", exception);
        }
    }

    private void applyBlocklist(Set<Module> serverBlocked) {
        Set<Module> previous = this.blocked;
        this.blocked = Set.copyOf(serverBlocked);
        for (Module module : previous) {
            if (!isBlocked(module) && this.restoreOnLeave.remove(module)) {
                module.setState(true);
            }
        }
        for (Module module : this.blocked) {
            if (module.isState()) {
                this.restoreOnLeave.add(module);
                module.setState(false);
            }
        }
        if (!previous.equals(this.blocked)) {
            refreshMenu();
        }
    }

    private void applyHostPolicy(ServerData server) {
        if (server == null) {
            return;
        }
        this.hostChecked = true;
        if (!isHolyWorld(server)) {
            return;
        }
        this.hostBlocked = Set.of(
                Raven.INSTANCE.moduleRepository().get(ElytraSwapModule.class),
                Raven.INSTANCE.moduleRepository().get(ProjectilePredictionModule.class));
        for (Module module : this.hostBlocked) {
            if (module.isState()) {
                this.restoreOnLeave.add(module);
                module.setState(false);
            }
        }
        refreshMenu();
    }

    private void refreshMenu() {
        Raven raven = Raven.INSTANCE;
        for (MenuTabElement tab : raven.tabsController().tabElements()) {
            for (var frame : tab.frames()) {
                if (frame instanceof ModuleCard card && isBlocked(card.module())) {
                    card.hideForServerPolicy();
                }
            }
            tab.applyCategoryFilter(tab.activeCategories);
        }
        raven.menuWindow().searchContainer().close();
        raven.menuWindow().searchContainer().openAnimator.force(false);
    }

    private void reset() {
        this.pendingId = null;
        this.lastRequest = 0L;
        this.checkOnJoin = false;
        this.channelAvailable = false;
        this.hostChecked = false;
        this.lastLevel = null;
        this.blocked = Set.of();
        this.hostBlocked = Set.of();
        for (Module module : Set.copyOf(this.restoreOnLeave)) {
            module.setState(true);
        }
        this.restoreOnLeave.clear();
        if (Raven.INSTANCE != null && Raven.INSTANCE.menuWindow() != null) {
            refreshMenu();
        }
    }

    private static boolean isHolyWorld(ServerData server) {
        if (server == null) {
            return false;
        }
        String host = server.ip.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("holyworld.me") || host.endsWith(".holyworld.me")
                || host.equals("holyworld.ru") || host.endsWith(".holyworld.ru");
    }

    private static Map<String, Module> features() {
        List<Module> modules = new ArrayList<>(Raven.INSTANCE.moduleRepository().getModules());
        modules.sort(Comparator.comparing(HolyWorldFeatureControl::id));
        Map<String, Module> features = new LinkedHashMap<>();
        for (Module module : modules) {
            features.put(id(module), module);
        }
        return features;
    }

    private static String id(Module module) {
        String name = module.getClass().getSimpleName();
        return (name.endsWith("Module") ? name.substring(0, name.length() - 6) : name)
                .toLowerCase(Locale.ROOT);
    }

    private record FeaturePayload(byte[] data) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
