package sanch3zss.raven.feature.module.render.party;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.entity.Entity;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.GamePaths;
import sanch3zss.raven.client.config.io.AtomicFileWriter;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.feature.module.render.party.PartyModule;
import sanch3zss.raven.client.chat.ChatUtil;

public final class PartyService {
    public static final int[] COLORS = {
            0xFF65C7F7, 0xFFFF8A65, 0xFFB494F7, 0xFF75D69C,
            0xFFF3C76A, 0xFFEF79A9, 0xFF77D7D1, 0xFFB4D56D
    };
    private static final String ENDPOINT = "wss://raven-14314f-visuals.gujejianisandro.workers.dev/ws";
    private static final String CODE_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom CODE_RANDOM = new SecureRandom();
    private static final Path SESSION_FILE = GamePaths.configDirectory().resolve("party-session.json");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .pingInterval(20, TimeUnit.SECONDS).build();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "Raven Party heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final Map<String, Position> positions = new LinkedHashMap<>();
    private final Map<String, Mark> marks = new LinkedHashMap<>();
    private final Deque<Long> recentMarks = new ArrayDeque<>();
    private volatile WebSocket socket;
    private volatile boolean heartbeatSupported;
    private Session session;
    private String pendingAction;
    private String pendingCode;
    private boolean legacyFallback;
    private boolean roomProtocol;
    private int createRetries;
    private long connectionVersion;
    private boolean connecting;
    private boolean authenticated;
    private long nextConnectAt;
    private long lastPositionAt;
    private boolean wasInWorld;
    private boolean visualsActivatedForSession;

    public PartyService() {
        this.session = loadSession();
        Raven.INSTANCE.eventDispatcher().register(ClientTickEvent.class, event -> tick());
        this.heartbeat.scheduleAtFixedRate(() -> {
            WebSocket connection = this.socket;
            if (connection != null && this.heartbeatSupported) connection.send("ping");
        }, 20L, 20L, TimeUnit.SECONDS);
    }

    public Collection<Member> members() {
        return java.util.List.copyOf(this.members.values());
    }

    public Collection<Position> positions() {
        return java.util.List.copyOf(this.positions.values());
    }

    public Collection<Mark> marks() {
        return java.util.List.copyOf(this.marks.values());
    }

    public String ownId() {
        return this.session == null ? null : this.session.id();
    }

    public boolean isConnected() {
        return this.socket != null && this.session != null && this.authenticated;
    }

    public String code() {
        return this.session == null ? null : this.session.code();
    }

    public boolean isPending() {
        return this.pendingAction != null;
    }

    public Position position(String id) {
        return this.positions.get(id);
    }

    public void create() {
        requestParty("create", null);
    }

    public void join(String code) {
        requestParty("join", code);
    }

    private void requestParty(String action, String code) {
        if (!Mc.INSTANCE.isWorldLoaded()) {
            message("Сначала зайдите на сервер.");
            return;
        }
        if (this.session != null) {
            message("Вы уже состоите в Party. Сначала используйте .party leave.");
            return;
        }
        if (this.pendingAction != null) {
            message("Дождитесь ответа Party.");
            return;
        }
        this.pendingAction = action;
        this.pendingCode = action.equals("create") ? newCode() : code;
        this.legacyFallback = false;
        this.createRetries = 0;
        if (this.socket != null) closeConnection();
        connect();
    }

    private static String newCode() {
        StringBuilder code = new StringBuilder(6);
        for (int index = 0; index < 6; index++) {
            code.append(CODE_ALPHABET.charAt(CODE_RANDOM.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    public void leave() {
        exitParty("leave");
    }

    public void disband() {
        exitParty("disband");
    }

    private void exitParty(String type) {
        if (this.session == null) {
            message("Вы не состоите в Party.");
            return;
        }
        if (!isConnected()) {
            message("Нет связи с Party. Подождите переподключения.");
            return;
        }
        if (type.equals("disband") && !isOwner()) {
            message("Распустить Party может только владелец. Чтобы выйти, используйте .party leave.");
            return;
        }
        JsonObject request = new JsonObject();
        request.addProperty("type", type);
        this.socket.send(request.toString());
    }

    public void info() {
        if (this.session == null) {
            message("Вы не состоите в Party.");
            return;
        }
        if (!isConnected()) {
            message("Party " + this.session.code() + ": переподключение...");
            return;
        }
        this.socket.send("{\"type\":\"info\"}");
    }

    public void mark(Entity entity, int color) {
        if (!isConnected()) {
            message("Сначала создайте Party или войдите в неё.");
            return;
        }
        long now = System.currentTimeMillis();
        while (!this.recentMarks.isEmpty() && now - this.recentMarks.peekFirst() >= 10000L) {
            this.recentMarks.removeFirst();
        }
        if (this.recentMarks.size() >= 5 || !this.recentMarks.isEmpty()
                && now - this.recentMarks.peekLast() < 1000L) return;
        this.recentMarks.addLast(now);
        long expiresAt = now + 5000L;
        this.marks.put(this.session.id(), new Mark(this.session.id(), entity.getUUID(),
                server(), dimension(), 0xFF000000 | (color & 0xFFFFFF), expiresAt));
        JsonObject request = new JsonObject();
        request.addProperty("type", "mark");
        request.addProperty("entity", entity.getUUID().toString());
        request.addProperty("server", server());
        request.addProperty("dimension", dimension());
        request.addProperty("color", color & 0xFFFFFF);
        this.socket.send(request.toString());
    }

    public static String server() {
        var server = Mc.INSTANCE.getCurrentServer();
        return server == null ? "singleplayer" : normalizeServer(server.ip);
    }

    public static String normalizeServer(String address) {
        String value = address.trim().toLowerCase(Locale.ROOT);
        int separator = value.lastIndexOf(':');
        String port = "";
        if (separator > 0 && separator == value.indexOf(':')) {
            port = value.substring(separator);
            value = value.substring(0, separator);
        }
        if (value.endsWith(".")) value = value.substring(0, value.length() - 1);
        return value + (port.equals(":25565") ? "" : port);
    }

    public static String dimension() {
        return Mc.INSTANCE.getWorld().dimension().identifier().toString();
    }

    public boolean isOwner() {
        if (this.session == null) return false;
        Member self = this.members.get(this.session.id());
        return self != null && self.owner();
    }

    private void tick() {
        boolean inWorld = Mc.INSTANCE.isWorldLoaded();
        if (!inWorld) {
            if (this.wasInWorld) closeConnection();
            this.wasInWorld = false;
            this.positions.clear();
            this.marks.clear();
            return;
        }
        this.wasInWorld = true;
        long now = System.currentTimeMillis();
        this.marks.values().removeIf(mark -> mark.expiresAt() <= now);
        this.positions.values().removeIf(position -> now - position.receivedAt() > 5000L);
        if (this.socket == null && !this.connecting && now >= this.nextConnectAt
                && (this.session != null || this.pendingAction != null)) connect();
        if (isConnected() && now - this.lastPositionAt >= 500L) {
            this.lastPositionAt = now;
            var player = Mc.INSTANCE.getPlayer();
            JsonObject request = new JsonObject();
            request.addProperty("type", "position");
            request.addProperty("x", player.getX());
            request.addProperty("y", player.getY());
            request.addProperty("z", player.getZ());
            request.addProperty("server", server());
            request.addProperty("dimension", dimension());
            this.socket.send(request.toString());
        }
    }

    private void connect() {
        if (this.connecting || this.socket != null) return;
        String code = this.pendingAction == null
                ? this.session == null ? null : this.session.code() : this.pendingCode;
        this.roomProtocol = this.pendingAction == null
                ? this.session != null && this.session.v2() : !this.legacyFallback;
        if (this.roomProtocol && code == null) return;
        String endpoint = this.roomProtocol ? ENDPOINT + "/" + code : ENDPOINT;
        long version = ++this.connectionVersion;
        this.connecting = true;
        this.nextConnectAt = System.currentTimeMillis() + 3000L;
        this.client.newWebSocket(new Request.Builder().url(endpoint).build(), new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                Minecraft.getInstance().execute(() -> {
                    if (version != connectionVersion || !Mc.INSTANCE.isWorldLoaded()) {
                        webSocket.close(1000, "Not in world");
                        if (version == connectionVersion) connecting = false;
                        return;
                    }
                    socket = webSocket;
                    connecting = false;
                    authenticated = false;
                    if (pendingAction != null) sendPending();
                    else if (session != null) {
                        JsonObject request = new JsonObject();
                        request.addProperty("type", "resume");
                        request.addProperty("code", session.code());
                        request.addProperty("token", session.token());
                        webSocket.send(request.toString());
                    }
                });
            }

            @Override
            public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                if (text.equals("pong")) return;
                Minecraft.getInstance().execute(() -> {
                    if (version == connectionVersion && socket == webSocket) handleMessage(text);
                });
            }

            @Override
            public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                webSocket.close(code, reason);
                Minecraft.getInstance().execute(() -> disconnected(webSocket, version));
            }

            @Override
            public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable error, Response response) {
                Minecraft.getInstance().execute(() -> {
                    disconnected(webSocket, version);
                    if (version != connectionVersion || response == null) return;
                    if (response.code() == 429) {
                        nextConnectAt = System.currentTimeMillis() + 60000L;
                        message("Слишком много подключений к Party. Повторная попытка через минуту.");
                    } else if (response.code() == 404 && roomProtocol
                            && "join".equals(pendingAction)) {
                        legacyFallback = true;
                        nextConnectAt = 0L;
                        connect();
                    } else if (response.code() == 404 && roomProtocol
                            && "create".equals(pendingAction)) {
                        message("Party Worker ещё не обновлён. Новую Party пока создать нельзя.");
                        pendingAction = null;
                        pendingCode = null;
                    }
                });
            }
        });
    }

    private void disconnected(WebSocket webSocket, long version) {
        if (version != this.connectionVersion || this.socket != null && this.socket != webSocket) return;
        this.socket = null;
        this.connecting = false;
        this.authenticated = false;
        this.heartbeatSupported = false;
        this.nextConnectAt = System.currentTimeMillis() + 3000L;
        this.positions.clear();
    }

    private void closeConnection() {
        WebSocket previous = this.socket;
        this.connectionVersion++;
        this.socket = null;
        this.connecting = false;
        this.authenticated = false;
        this.heartbeatSupported = false;
        this.nextConnectAt = 0L;
        if (previous != null) previous.close(1000, "Switching Party connection");
    }

    private void sendPending() {
        if (this.socket == null || this.pendingAction == null) return;
        JsonObject request = new JsonObject();
        request.addProperty("type", this.pendingAction);
        request.addProperty("name", Mc.INSTANCE.getSession().getName());
        if (this.pendingCode != null) request.addProperty("code", this.pendingCode);
        this.socket.send(request.toString());
    }

    private void handleMessage(String text) {
        JsonObject data;
        try {
            data = JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException exception) {
            Raven.LOGGER.warn("Invalid Party response", exception);
            return;
        }
        String type = string(data, "type");
        switch (type) {
            case "created", "joined", "resumed" -> {
                this.session = new Session(string(data, "code"), string(data, "id"),
                        string(data, "token"), this.roomProtocol);
                this.heartbeatSupported = data.has("heartbeat") && data.get("heartbeat").getAsBoolean();
                this.pendingAction = null;
                this.pendingCode = null;
                this.createRetries = 0;
                this.authenticated = true;
                saveSession();
                updateRoster(data.getAsJsonArray("members"));
                if (!this.visualsActivatedForSession) {
                    this.visualsActivatedForSession = true;
                    PartyModule visuals = Raven.INSTANCE.moduleRepository().get(PartyModule.class);
                    visuals.setState(true);
                    if (!visuals.isState()) message("PartyHelper недоступен на этом сервере.");
                }
                if (type.equals("created")) {
                    ChatUtil.addChatMessage(Component.literal("Party создана. [")
                            .append(Component.literal(this.session.code()).withStyle(style -> style
                                    .withColor(ChatFormatting.WHITE)
                                    .withClickEvent(new ClickEvent.CopyToClipboard(this.session.code()))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("Нажмите, чтобы скопировать код")))))
                            .append(Component.literal("]")));
                } else {
                    message(type.equals("joined") ? "Вы вошли в Party " + this.session.code()
                            : "Вы вернулись в Party " + this.session.code());
                }
            }
            case "info" -> {
                updateRoster(data.getAsJsonArray("members"));
                message("Party " + string(data, "code") + " — " + this.members.size() + "/8:");
                long now = System.currentTimeMillis();
                for (Member member : this.members.values()) {
                    String status = member.connected() ? "в сети" : "вернётся в течение "
                            + Math.max(0L, (member.expiresAt() - now + 999L) / 1000L) + " сек.";
                    message(member.name() + (member.owner() ? " [владелец]" : "") + " — " + status);
                }
            }
            case "member_joined" -> {
                Member member = parseMember(data.getAsJsonObject("member"));
                this.members.put(member.id(), member);
                message(member.name() + " вошёл в Party.");
            }
            case "member_offline", "member_online" -> {
                Member member = this.members.get(string(data, "id"));
                if (member != null) {
                    boolean online = type.equals("member_online");
                    this.members.put(member.id(), member.withConnection(online,
                            online ? 0L : number(data, "expiresAt")));
                    if (!online) this.positions.remove(member.id());
                    message(member.name() + (online ? " вернулся в Party." : " вышел с сервера. Ожидание 5 минут."));
                }
            }
            case "member_left" -> {
                Member member = this.members.remove(string(data, "id"));
                if (member != null) message(member.name() + " покинул Party.");
                this.positions.remove(string(data, "id"));
                this.marks.remove(string(data, "id"));
            }
            case "owner_changed" -> {
                String ownerId = string(data, "id");
                for (Member member : java.util.List.copyOf(this.members.values())) {
                    this.members.put(member.id(), member.withOwner(member.id().equals(ownerId)));
                }
                Member owner = this.members.get(ownerId);
                if (owner == null) break;
                message(owner.id().equals(ownId()) ? "Теперь вы владелец Party."
                        : owner.name() + " теперь владелец Party.");
            }
            case "left", "disbanded" -> {
                message(type.equals("left") ? "Вы вышли из Party." : "Party распущена.");
                clearSession();
            }
            case "position" -> {
                String id = string(data, "id");
                double x = data.get("x").getAsDouble();
                double y = data.get("y").getAsDouble();
                double z = data.get("z").getAsDouble();
                Position previous = this.positions.get(id);
                boolean sameWorld = previous != null
                        && normalizeServer(previous.server()).equals(normalizeServer(string(data, "server")))
                        && previous.dimension().equals(string(data, "dimension"));
                this.positions.put(id, new Position(id,
                        sameWorld ? previous.interpolatedX() : x,
                        sameWorld ? previous.interpolatedY() : y,
                        sameWorld ? previous.interpolatedZ() : z,
                        x, y, z, string(data, "server"),
                        string(data, "dimension"), System.currentTimeMillis()));
            }
            case "mark" -> {
                try {
                    String id = string(data, "id");
                    this.marks.put(id, new Mark(id, UUID.fromString(string(data, "entity")),
                            string(data, "server"), string(data, "dimension"),
                            0xFF000000 | data.get("color").getAsInt(), System.currentTimeMillis() + 5000L));
                } catch (RuntimeException exception) {
                    Raven.LOGGER.warn("Invalid Party mark", exception);
                }
            }
            case "error" -> {
                String reason = string(data, "reason");
                if (reason.equals("code_taken") && this.roomProtocol
                        && "create".equals(this.pendingAction) && this.createRetries++ < 5) {
                    this.pendingCode = newCode();
                    closeConnection();
                    connect();
                    return;
                }
                if (reason.equals("party_not_found") && this.roomProtocol
                        && "join".equals(this.pendingAction)) {
                    this.legacyFallback = true;
                    closeConnection();
                    connect();
                    return;
                }
                message(errorMessage(reason));
                if (reason.equals("session_expired")) clearSession();
                else if (this.pendingAction != null) {
                    this.pendingAction = null;
                    this.pendingCode = null;
                    closeConnection();
                }
            }
            default -> {
            }
        }
    }

    private void updateRoster(JsonArray array) {
        this.members.clear();
        this.positions.clear();
        if (array == null) return;
        for (JsonElement element : array) {
            Member member = parseMember(element.getAsJsonObject());
            this.members.put(member.id(), member);
        }
    }

    private static Member parseMember(JsonObject data) {
        return new Member(string(data, "id"), string(data, "name"),
                data.get("color").getAsInt(), data.get("owner").getAsBoolean(),
                data.get("connected").getAsBoolean(),
                data.has("expiresAt") && !data.get("expiresAt").isJsonNull()
                        ? data.get("expiresAt").getAsLong() : 0L);
    }

    private void clearSession() {
        closeConnection();
        this.authenticated = false;
        this.visualsActivatedForSession = false;
        this.session = null;
        this.pendingAction = null;
        this.pendingCode = null;
        this.legacyFallback = false;
        this.members.clear();
        this.positions.clear();
        this.marks.clear();
        this.recentMarks.clear();
        saveSession();
    }

    private static Session loadSession() {
        if (!Files.exists(SESSION_FILE)) return null;
        try {
            JsonObject data = JsonParser.parseString(Files.readString(SESSION_FILE, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            String code = string(data, "code");
            String id = string(data, "id");
            String token = string(data, "token");
            return code.length() == 6 && !id.isBlank() && !token.isBlank()
                    ? new Session(code, id, token, data.has("v2") && data.get("v2").getAsBoolean()) : null;
        } catch (IOException | RuntimeException exception) {
            Raven.LOGGER.warn("Failed to load Party session", exception);
            return null;
        }
    }

    private void saveSession() {
        try {
            Files.createDirectories(SESSION_FILE.getParent());
            if (this.session == null) {
                Files.deleteIfExists(SESSION_FILE);
                return;
            }
            JsonObject data = new JsonObject();
            data.addProperty("code", this.session.code());
            data.addProperty("id", this.session.id());
            data.addProperty("token", this.session.token());
            data.addProperty("v2", this.session.v2());
            AtomicFileWriter.writeBytes(SESSION_FILE, data.toString().getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException exception) {
            Raven.LOGGER.error("Failed to save Party session", exception);
        }
    }

    private static String errorMessage(String reason) {
        return switch (reason) {
            case "party_not_found" -> "Party с таким кодом не найдена.";
            case "party_full" -> "В Party уже 8 участников.";
            case "session_expired" -> "Время на возвращение в Party истекло.";
            case "not_owner" -> "Распустить Party может только владелец.";
            case "invalid_code" -> "Код должен состоять из 6 латинских букв или цифр.";
            case "invalid_name" -> "Недопустимый ник для Party.";
            case "code_taken" -> "Не удалось подобрать свободный код Party.";
            default -> "ошибка сервера (" + reason + ").";
        };
    }

    private static String string(JsonObject data, String key) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsString() : "";
    }

    private static long number(JsonObject data, String key) {
        return data.has(key) && !data.get(key).isJsonNull() ? data.get(key).getAsLong() : 0L;
    }

    private static void message(String text) {
        ChatUtil.addChatMessage(text);
    }

    public record Session(String code, String id, String token, boolean v2) {
    }

    public record Member(String id, String name, int color, boolean owner, boolean connected, long expiresAt) {
        public Member withConnection(boolean value, long expiry) {
            return new Member(this.id, this.name, this.color, this.owner, value, expiry);
        }

        public Member withOwner(boolean value) {
            return new Member(this.id, this.name, this.color, value, this.connected, this.expiresAt);
        }
    }

    public record Position(String id, double fromX, double fromY, double fromZ,
                           double x, double y, double z, String server, String dimension,
                           long receivedAt) {
        private double progress() {
            return Math.clamp((System.currentTimeMillis() - this.receivedAt) / 500.0d, 0.0d, 1.0d);
        }

        public double interpolatedX() {
            return this.fromX + (this.x - this.fromX) * progress();
        }

        public double interpolatedY() {
            return this.fromY + (this.y - this.fromY) * progress();
        }

        public double interpolatedZ() {
            return this.fromZ + (this.z - this.fromZ) * progress();
        }
    }

    public record Mark(String id, UUID entity, String server, String dimension, int color, long expiresAt) {
    }
}
