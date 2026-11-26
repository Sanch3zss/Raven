package sanch3zss.raven.feature.module.misc;


import sanch3zss.raven.client.chat.ChatUtil;
import sanch3zss.raven.feature.server.ServerUtil;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector2f;
import org.joml.Vector3i;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.audio.WavSoundPlayer;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.event.network.*;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.event.render.Render2DEvent;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.misc.FtTrackedBoss;
import sanch3zss.raven.feature.server.funtime.FunTimeActivityService;
import sanch3zss.raven.feature.world.*;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.ColorSetting;
import sanch3zss.raven.ui.setting.ExpandableSetting;
import sanch3zss.raven.util.math.*;

@Aliases(aliases = {"Funtime Helper", "FT Helper"})
public class FTHelperModule extends Module {
    private static final Pattern EVENT_COORDINATES = Pattern.compile(
            "(?:Координаты|Появился на координатах):?\\s*\\[?(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)\\]?",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern ANARCHY_NUMBER = Pattern.compile(
            "(?:анархия|anarchy|an|funtime)\\D{0,8}(\\d{1,4})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    public final BooleanSetting autoPoint = new BooleanSetting(
            Lang.FTHELPER_AUTO_POINT, Lang.FTHELPER_AUTO_POINT_DESC);
    private final BooleanSetting trapRange = new BooleanSetting(
            Translation.clearText("Зона трапки")).setValue(true);
    private final BooleanSetting plastRange = new BooleanSetting(
            Translation.clearText("Зона пласта")).setValue(true);
    private final BooleanSetting disorientationRange = new BooleanSetting(
            Translation.clearText("Радиус дезориентации")).setValue(true);
    private final BooleanSetting dustRange = new BooleanSetting(
            Translation.clearText("Радиус явной пыли")).setValue(true);
    private final BooleanSetting largeRange = new BooleanSetting(
            Translation.clearText("Драконский скин"))
            .visible(() -> this.trapRange.isValue() || this.plastRange.isValue());
    private final ColorSetting rangeColor = new ColorSetting(
            Translation.clearText("Цвет зоны")).value(0xFFFFFFFF);
    private final List<FtTrackedBoss> trackedBosses = new ArrayList<>();
    private final Map<MarkedEvent, String> markedEvents = new HashMap<>();
    private long lastAutoPointSync;
    private int lastAutoPointAnarchy = -1;
    private final Map<String, GlTexture> iconCache = new HashMap<>();
    private final Map<String, String> iconPaths = Map.ofEntries(
            Map.entry("Мистический сундук", "/icons/fthelper/mystic.png"),
            Map.entry("Вулкан", "/icons/fthelper/volcano.png"),
            Map.entry("Метеоритный дождь", "/icons/fthelper/shower.png"),
            Map.entry("Маяк убийца", "/icons/fthelper/lighthouse.png"),
            Map.entry("Мистический Алтарь", "/icons/fthelper/altar.png"),
            Map.entry("Загадочный маяк", "/icons/fthelper/lighthouse.png"),
            Map.entry("Сундук смерти", "/icons/fthelper/deadchest.png"),
            Map.entry("Адская резня", "/icons/fthelper/infernal.png"));

    private record MarkedEvent(int anarchy, String id, int x, int y, int z) {
    }

    public FTHelperModule() {
        super(ModuleTab.MISC, "FT Helper");
        ExpandableSetting zones = new ExpandableSetting(Translation.clearText("Зоны"))
                .navigationOnly().setSubSettings(List.of(this.trapRange, this.plastRange,
                        this.disorientationRange, this.dustRange, this.largeRange,
                        this.rangeColor, this.rangeColor.themeSyncSetting()));
        addSettings(this.autoPoint, zones);
        register(WorldRenderEvent.class, this::renderRanges);
        register(PacketReceiveEvent.class, event -> {
            if (!isState() || !this.autoPoint.isValue() || !Mc.INSTANCE.isWorldLoaded()
                    || !(event.getPacket() instanceof ClientboundSystemChatPacket packet)) {
                return;
            }
            Component content = packet.content();
            String text = content.getString();
            String name = StringUtils.substringBetween(text, "|||   [", "]   ");
            if (name == null) {
                return;
            }
            String gps = StringUtils.substringBetween(content.toString(), "value='/gps ", "'");
            Matcher coordinateMatch = EVENT_COORDINATES.matcher(text);
            if (gps == null && coordinateMatch.find()) {
                gps = coordinateMatch.group(1) + " " + coordinateMatch.group(2)
                        + " " + coordinateMatch.group(3);
            }
            String level = StringUtils.substringBetween(text, "Уровень лута: ", "\n ║");
            String owner = StringUtils.substringBetween(text, "Призван игроком: ", "\n ║");
            if (gps == null) {
                if (name.equals("Сундук смерти")) {
                    addTrackedBoss(name, level, owner,
                            Vec3.atCenterOf(new BlockPos(-155, 64, 205)), "lobby", 300, 0);
                } else if (name.equals("Адская резня")) {
                    addTrackedBoss(name, level, owner,
                            Vec3.atCenterOf(new BlockPos(48, 87, 73)), "lobby", 180, 120);
                }
                return;
            }
            String[] coordinates = gps.split(" ");
            if (coordinates.length != 3) {
                return;
            }
            Vec3 position;
            try {
                position = Vec3.atCenterOf(new BlockPos(Integer.parseInt(coordinates[0]),
                        Integer.parseInt(coordinates[1]), Integer.parseInt(coordinates[2])));
            } catch (NumberFormatException ignored) {
                return;
            }
            switch (name) {
                case "Мистический сундук" ->
                        addTrackedBoss(name, level, owner, position, "overworld", 300, 0);
                case "Вулкан" ->
                        addTrackedBoss(name, level, owner, position, "overworld", 300, 120);
                case "Метеоритный дождь", "Маяк убийца", "Мистический Алтарь" ->
                        addTrackedBoss(name, level, owner, position, "overworld", 360, 0);
                case "Загадочный маяк" ->
                        addTrackedBoss(name, level, owner, position, "overworld", 60, 180);
                default -> {
                }
            }
        });
        register(PlayerTickEvent.class, event -> {
            if (event.isPre()) {
                this.trackedBosses.removeIf(boss ->
                        boss.timeEnd + 90_000.0d <= System.currentTimeMillis());
                updateAutoPoints();
            }
        });
        register(Render2DEvent.class, event -> {
            if (!isState() || !this.autoPoint.isValue() || !Mc.INSTANCE.isWorldLoaded()
                    || !event.isPre()) {
                return;
            }
            DrawEngine draw = Raven.INSTANCE.drawEngine();
            draw.begin();
            for (FtTrackedBoss boss : this.trackedBosses) {
                if (boss.anarchy != ServerUtil.getAnarchy()
                        || !ServerUtil.getWorldType().equals(boss.world)) {
                    continue;
                }
                Optional<Vector2f> projected = ProjectionUtil.worldToScreen(boss.vec);
                if (projected.isEmpty()) {
                    continue;
                }
                Vector2f point = projected.get();
                String path = this.iconPaths.get(boss.name);
                if (path != null) {
                    GlTexture icon = this.iconCache.computeIfAbsent(path,
                            key -> new GlTexture(new ClasspathResource(key)));
                    draw.texture(event.matrixStack().last().pose(), point.x - 8.0f, point.y,
                            16.0f, 16.0f, draw.bindTexture(icon.textureWithSTB()),
                            draw.colorStack().white());
                }
                String distance = " [" + MathUtil.round(Mc.INSTANCE.getEntityRenderDispatcher()
                        .camera.position().distanceTo(boss.vec), 0.1d) + "m]";
                draw.msdfFont(event.matrixStack().last().pose(), Fonts.INTER_MEDIUM.get(),
                        boss.name + distance, point.x, point.y + 17.0f,
                        10.0f, 0.0f, draw.colorStack().computeColor(0xFFFFFFFF));
            }
            draw.end();
        });
    }

    private void updateAutoPoints() {
        if (!isState() || !this.autoPoint.isValue() || !Mc.INSTANCE.isWorldLoaded()
                || !ServerUtil.isConnectedToAllFuntimeServers()) {
            return;
        }
        int anarchy = scoreboardAnarchy();
        if (anarchy <= 0) {
            return;
        }
        FunTimeActivityService service = Raven.INSTANCE.tabsController().funTimeActivityService();
        service.refreshIfStale();
        FunTimeActivityService.Snapshot snapshot = service.snapshot();
        if (snapshot.syncedAt() == 0L
                || System.currentTimeMillis() - snapshot.syncedAt() > 30_000L
                || snapshot.syncedAt() == this.lastAutoPointSync
                && anarchy == this.lastAutoPointAnarchy) {
            return;
        }
        this.lastAutoPointSync = snapshot.syncedAt();
        this.lastAutoPointAnarchy = anarchy;
        Set<MarkedEvent> currentEvents = new HashSet<>();
        for (FunTimeActivityService.Event event : snapshot.events()) {
            if (anarchyNumber(event.server()) != anarchy
                    || !event.hasLocation() || "CLOSED".equals(event.phase())
                    || "altarundead".equals(event.id())
                    || "myst_beacon".equals(event.id())
                    || "airdrop".equals(event.id())) {
                continue;
            }
            MarkedEvent marked = new MarkedEvent(anarchy, event.id(),
                    event.x(), event.y(), event.z());
            currentEvents.add(marked);
            if (this.markedEvents.containsKey(marked)) {
                continue;
            }
            if (Raven.INSTANCE.waysRepository().getWaypoints().stream().anyMatch(waypoint ->
                    waypoint.vector() != null && waypoint.vector().x() == marked.x()
                            && waypoint.vector().z() == marked.z())) {
                this.markedEvents.put(marked, null);
                continue;
            }
            String name = eventName(event.id()) + " an" + anarchy
                    + " (" + event.x() + ", " + event.z() + ")";
            Raven.INSTANCE.waysRepository().add(name,
                    new Vector3i(event.x(), event.y(), event.z()));
            this.markedEvents.put(marked, name);
            ChatUtil.addChatMessage("Метка ивента поставлена: " + name);
        }
        Iterator<Map.Entry<MarkedEvent, String>> iterator = this.markedEvents.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MarkedEvent, String> entry = iterator.next();
            if (entry.getKey().anarchy() != anarchy || currentEvents.contains(entry.getKey())) continue;
            if (entry.getValue() != null && Raven.INSTANCE.waysRepository().remove(entry.getValue())) {
                ChatUtil.addChatMessage("Ивент закончился, метка удалена: " + entry.getValue());
            }
            iterator.remove();
        }
    }

    private static int scoreboardAnarchy() {
        Scoreboard scoreboard = Mc.INSTANCE.getWorld().getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            return -1;
        }
        int anarchy = anarchyNumber(sidebar.getDisplayName().getString());
        if (anarchy > 0) {
            return anarchy;
        }
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
            String line = PlayerTeam.formatNameForTeam(
                    scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()).getString();
            anarchy = anarchyNumber(line);
            if (anarchy > 0) {
                return anarchy;
            }
        }
        return -1;
    }

    private static int anarchyNumber(String text) {
        Matcher matcher = ANARCHY_NUMBER.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private static String eventName(String id) {
        return switch (id) {
            case "beacon" -> "Маяк убийца";
            case "vulkan" -> "Вулкан";
            case "meteor_rain" -> "Метеоритный дождь";
            case "hellm" -> "Адская резня";
            case "geyser" -> "Гейзер";
            case "deathchest" -> "Сундук смерти";
            default -> id;
        };
    }

    private void renderRanges(WorldRenderEvent event) {
        Mc mc = Mc.INSTANCE;
        if (!isState() || !mc.isWorldLoaded()) return;
        var player = mc.getPlayer();
        BlockPos center = player.blockPosition();
        int color = this.rangeColor.getColor();
        int radius = this.largeRange.isValue() ? 3 : 2;
        int height = this.largeRange.isValue() ? 6 : 4;
        if (this.trapRange.isValue()
                && CustomItemRangeRenderer.held(player, Items.NETHERITE_SCRAP)) {
            CustomItemRangeRenderer.outlineBox(event,
                    CustomItemRangeRenderer.box(center, -radius, 0, -radius,
                            radius + 1, height, radius + 1), color);
        }
        if (this.plastRange.isValue()
                && CustomItemRangeRenderer.held(player, Items.DRIED_KELP)) {
            renderPlastRange(event, center, player.getYRot(), player.getXRot(), color);
        }
        if ((this.disorientationRange.isValue()
                && CustomItemRangeRenderer.held(player, Items.ENDER_EYE))
                || (this.dustRange.isValue()
                && CustomItemRangeRenderer.held(player, Items.SUGAR))) {
            CustomItemRangeRenderer.circle(event, player.getPosition(mc.getTickDelta()),
                    10.0, color);
        }
    }

    private void renderPlastRange(WorldRenderEvent event, BlockPos playerPos,
                                  float yaw, float pitch, int color) {
        boolean large = this.largeRange.isValue();
        int radius = large ? 3 : 2;
        if (Math.abs(pitch) > 45.0f) {
            int bottom = pitch < -45.0f ? 3 : -3;
            int top = pitch < -45.0f ? large ? 7 : 5 : -1;
            CustomItemRangeRenderer.outlineBox(event,
                    CustomItemRangeRenderer.box(playerPos, -radius, bottom, -radius,
                            radius + 1, top, radius + 1), color);
            return;
        }

        float direction = (yaw % 360.0f + 360.0f) % 360.0f;
        int diagonal = -1;
        for (int angle : new int[]{45, 135, 225, 315}) {
            if (Math.abs(direction - angle) < 22.0f) {
                diagonal = angle;
                break;
            }
        }
        int wallHeight = large ? 7 : 5;
        if (diagonal != -1) {
            int dx = diagonal == 45 || diagonal == 135 ? -3 : 2;
            int dz = diagonal == 45 || diagonal == 315 ? 2 : -2;
            BlockPos wallCenter = playerPos.offset(dx, -1, dz);
            for (int offset = -radius; offset <= radius; offset++) {
                int z = diagonal == 45 || diagonal == 225 ? offset : -offset;
                CustomItemRangeRenderer.outlineBox(event,
                        CustomItemRangeRenderer.box(wallCenter, offset, 0, z,
                                offset + 2, wallHeight, z + 1), color);
            }
            return;
        }

        BlockPos wallCenter;
        boolean alongX;
        if (direction >= 315.0f || direction < 45.0f) {
            wallCenter = playerPos.offset(0, -1, 3);
            alongX = true;
        } else if (direction < 135.0f) {
            wallCenter = playerPos.offset(-3, -1, 0);
            alongX = false;
        } else if (direction < 225.0f) {
            wallCenter = playerPos.offset(0, -1, -3);
            alongX = true;
        } else {
            wallCenter = playerPos.offset(3, -1, 0);
            alongX = false;
        }
        CustomItemRangeRenderer.outlineBox(event, alongX
                ? CustomItemRangeRenderer.box(wallCenter, -radius, 0, -1,
                        radius + 1, wallHeight, 1)
                : CustomItemRangeRenderer.box(wallCenter, -1, 0, -radius,
                        1, wallHeight, radius + 1), color);
    }

    private void addTrackedBoss(String name, String level, String owner, Vec3 position,
                                String world, int duration, int extraDuration) {
        if (this.trackedBosses.stream().anyMatch(boss -> boss.vec.equals(position))) {
            return;
        }
        long opening = System.currentTimeMillis() + duration * 1000L;
        this.trackedBosses.add(new FtTrackedBoss(name, level, owner, position, world,
                ServerUtil.getAnarchy(), opening, opening + extraDuration * 1000L));
        Raven.INSTANCE.notificationRepository().post(NotificationType.EVENT,
                Component.literal("Начался ивент: " + name), 10L, TimeUnit.SECONDS);
        WavSoundPlayer.INSTANCE.playSound("apple_pay", 80.0f, false);
    }

    @Override
    public void deactivate() {
        this.trackedBosses.clear();
        this.markedEvents.clear();
        this.lastAutoPointSync = 0L;
        this.lastAutoPointAnarchy = -1;
        super.deactivate();
    }
}
