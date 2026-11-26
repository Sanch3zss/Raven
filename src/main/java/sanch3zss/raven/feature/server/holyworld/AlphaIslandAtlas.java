package sanch3zss.raven.feature.server.holyworld;

import com.mojang.blaze3d.platform.NativeImage;
import hw.zako.alphamap.protocol.AtlasGeometry;
import hw.zako.alphamap.protocol.MapProtocol;
import hw.zako.alphamap.protocol.ServerMessage;
import hw.zako.alphamap.protocol.TileCache;
import hw.zako.alphamap.protocol.TileQueue;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.joml.Vector3i;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.storage.Waypoint;
import sanch3zss.raven.render.gl.FramebufferUtil;

public final class AlphaIslandAtlas {
    private static final Identifier CHANNEL = Identifier.fromNamespaceAndPath("alphaisland", "map");
    private static final CustomPacketPayload.Type<MapPayload> TYPE = new CustomPacketPayload.Type<>(CHANNEL);
    private static final StreamCodec<FriendlyByteBuf, MapPayload> CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeBytes(payload.data()), buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new MapPayload(data);
            });

    private final TileQueue queue = new TileQueue();
    private AtlasGeometry geometry;
    private DynamicTexture[] textures;
    private Identifier[] textureNames;
    private byte[][] hashes;
    private byte[][] loadedHashes;
    private List<ServerMessage.Marker> markers = List.of();
    private ClientLevel lastLevel;
    private long lastHello;
    private int unanswered;
    private int generation;
    private String status = "Карта появится после подключения к AlphaSmp";

    public void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        ClientPlayNetworking.registerGlobalReceiver(TYPE,
                (payload, context) -> context.client().execute(() -> receive(payload.data())));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(Minecraft client) {
        if (client.level != this.lastLevel) {
            reset();
            this.lastLevel = client.level;
        }
        if (client.player == null || Raven.INSTANCE == null
                || Raven.INSTANCE.tabsController().current() != Raven.INSTANCE.tabsController().worldMap
                || !Raven.INSTANCE.menuWindow().menuOpen) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.geometry == null || now - this.lastHello >= 60_000L) {
            hello(now);
        }
        if (this.geometry != null) {
            List<Integer> batch = this.queue.nextBatch(now);
            if (batch != null) {
                ClientPlayNetworking.send(new MapPayload(MapProtocol.request(batch, this.geometry)));
            }
        }
    }

    private void hello(long now) {
        if (this.unanswered >= 3 || now - this.lastHello < 5_000L) {
            return;
        }
        if (!ClientPlayNetworking.canSend(TYPE)) {
            this.status = "Сервер не предоставляет карту AlphaSmp";
            return;
        }
        this.lastHello = now;
        this.unanswered++;
        this.status = this.geometry == null ? "Загрузка карты острова…" : this.status;
        ClientPlayNetworking.send(new MapPayload(MapProtocol.hello()));
    }

    private void receive(byte[] data) {
        this.unanswered = 0;
        ServerMessage message;
        try {
            message = ServerMessage.parse(data);
        } catch (RuntimeException exception) {
            Raven.LOGGER.warn("Invalid AlphaSmp map packet", exception);
            return;
        }
        switch (message) {
            case ServerMessage.Manifest manifest -> manifest(manifest);
            case ServerMessage.Tile tile -> tile(tile);
            case ServerMessage.Markers received -> {
                this.markers = List.copyOf(received.markers());
                relocateWaypoints(true);
            }
            case ServerMessage.Unavailable unavailable -> this.status = switch (unavailable.reason()) {
                case NO_MAP -> "Карта этого острова недоступна";
                case NOT_READY -> "Карта острова ещё готовится";
                case WRONG_VERSION -> "Несовместимая версия карты AlphaSmp";
                case TOO_FAST, UNKNOWN -> "Сервер временно не отдал карту";
            };
        }
    }

    private void manifest(ServerMessage.Manifest manifest) {
        if (manifest.protocolVersion() != MapProtocol.VERSION) {
            this.status = "Несовместимая версия карты AlphaSmp";
            return;
        }
        AtlasGeometry next = manifest.geometry();
        byte[][] nextHashes = manifest.hashes();
        if (nextHashes.length != next.tileCount()) {
            this.status = "Повреждены данные карты острова";
            return;
        }
        if (!next.equals(this.geometry)) {
            release();
            if (this.geometry != null) this.markers = List.of();
            this.geometry = next;
            this.textures = new DynamicTexture[next.tileCount()];
            this.textureNames = new Identifier[next.tileCount()];
            this.loadedHashes = new byte[next.tileCount()][];
            this.generation++;
            relocateWaypoints(false);
        }
        this.hashes = nextHashes;
        this.queue.clear();
        this.status = "";
        int currentGeneration = this.generation;
        byte[][] loaded = this.loadedHashes;
        TileCache cache = cache();
        Thread.ofVirtual().name("raven-alpha-map-cache").start(() -> {
            for (int index = 0; index < nextHashes.length; index++) {
                if (MapProtocol.isBlank(nextHashes[index])
                        || Arrays.equals(loaded[index], nextHashes[index])) {
                    continue;
                }
                int tileIndex = index;
                NativeImage image = decode(cache.read(next.id(), nextHashes[index]));
                Minecraft.getInstance().execute(() -> {
                    if (currentGeneration != this.generation
                            || !Arrays.equals(this.hashes[tileIndex], nextHashes[tileIndex])) {
                        if (image != null) image.close();
                    } else if (image == null) {
                        this.queue.need(tileIndex);
                    } else {
                        put(tileIndex, nextHashes[tileIndex], image);
                    }
                });
            }
        });
    }

    private void tile(ServerMessage.Tile tile) {
        AtlasGeometry current = this.geometry;
        if (current == null || !current.contains(tile.tileX(), tile.tileZ())) {
            return;
        }
        int index = current.index(tile.tileX(), tile.tileZ());
        if (!Arrays.equals(this.hashes[index], tile.hash())) {
            this.queue.need(index);
            return;
        }
        int currentGeneration = this.generation;
        Thread.ofVirtual().name("raven-alpha-map-tile").start(() -> {
            cache().write(current.id(), tile.hash(), tile.png());
            NativeImage image = decode(tile.png());
            Minecraft.getInstance().execute(() -> {
                if (currentGeneration != this.generation) {
                    if (image != null) image.close();
                    return;
                }
                this.queue.answered(index);
                if (image != null) {
                    put(index, tile.hash(), image);
                }
            });
        });
    }

    private void relocateWaypoints(boolean markersKnown) {
        AtlasGeometry current = this.geometry;
        if (current == null || Raven.INSTANCE == null) return;
        Map<Waypoint, Waypoint> changes = new HashMap<>();
        for (Waypoint waypoint : Raven.INSTANCE.waysRepository().getWaypoints()) {
            Waypoint.AlphaBinding binding = waypoint.alpha();
            if (binding == null || waypoint.vector() == null || binding.map() == current.id()) continue;
            if (binding.marker() != null && !markersKnown) continue;
            int x = waypoint.vector().x() - binding.originX() + current.originX();
            int z = waypoint.vector().z() - binding.originZ() + current.originZ();
            ServerMessage.Marker marker = binding.marker() == null ? null : closestMarker(binding, x, z);
            if (marker != null) {
                x = marker.x();
                z = marker.z();
            }
            changes.put(waypoint, waypoint.relocated(new Vector3i(x, waypoint.vector().y(), z),
                    bind(current, marker == null ? binding.marker() : marker.icon(),
                            marker == null ? binding.markerLabel() : marker.label())));
        }
        Raven.INSTANCE.waysRepository().replace(changes);
    }

    private ServerMessage.Marker closestMarker(Waypoint.AlphaBinding binding, int x, int z) {
        List<ServerMessage.Marker> sameKind = this.markers.stream()
                .filter(marker -> binding.marker().equalsIgnoreCase(marker.icon()))
                .toList();
        List<ServerMessage.Marker> sameLabel = sameKind.stream()
                .filter(marker -> binding.markerLabel() != null && binding.markerLabel().equals(marker.label()))
                .toList();
        return (sameLabel.isEmpty() ? sameKind : sameLabel).stream()
                .min(Comparator.comparingLong(marker -> {
                    long dx = marker.x() - x;
                    long dz = marker.z() - z;
                    return dx * dx + dz * dz;
                }))
                .orElse(null);
    }

    public static Waypoint.AlphaBinding bind(AtlasGeometry geometry, String marker, String markerLabel) {
        return new Waypoint.AlphaBinding(geometry.id(), geometry.originX(), geometry.originZ(), marker, markerLabel);
    }

    private void put(int index, byte[] hash, NativeImage image) {
        Identifier name = Identifier.fromNamespaceAndPath("raven", "alpha_map/tile_" + index);
        DynamicTexture previous = this.textures[index];
        if (previous != null) {
            Minecraft.getInstance().getTextureManager().release(name);
            previous.close();
        }
        DynamicTexture texture = new DynamicTexture(() -> "AlphaSmp map tile " + index, image);
        Minecraft.getInstance().getTextureManager().register(name, (AbstractTexture) texture);
        clampEdges(FramebufferUtil.getTextureId(texture));
        this.textures[index] = texture;
        this.textureNames[index] = name;
        this.loadedHashes[index] = hash;
    }

    private static void clampEdges(int textureId) {
        if (textureId <= 0) return;
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
    }

    private void reset() {
        release();
        this.geometry = null;
        this.hashes = null;
        this.loadedHashes = null;
        this.textures = null;
        this.textureNames = null;
        this.markers = List.of();
        this.queue.clear();
        this.lastHello = 0L;
        this.unanswered = 0;
        this.status = "Карта появится после подключения к AlphaSmp";
        this.generation++;
    }

    private void release() {
        if (this.textures == null) {
            return;
        }
        for (int index = 0; index < this.textures.length; index++) {
            if (this.textures[index] == null) continue;
            Minecraft.getInstance().getTextureManager().release(this.textureNames[index]);
            this.textures[index].close();
        }
    }

    private static TileCache cache() {
        Path root = Minecraft.getInstance().gameDirectory.toPath().resolve("raven/alphamap");
        return new TileCache(root);
    }

    private static NativeImage decode(byte[] png) {
        if (png == null) return null;
        try {
            return NativeImage.read(png);
        } catch (IOException exception) {
            return null;
        }
    }

    public AtlasGeometry geometry() {
        return this.geometry;
    }

    public Identifier texture(int tileX, int tileZ) {
        if (this.geometry == null || !this.geometry.contains(tileX, tileZ)) return null;
        return this.textureNames[this.geometry.index(tileX, tileZ)];
    }

    public List<ServerMessage.Marker> markers() {
        return this.markers;
    }

    public String status() {
        if (Minecraft.getInstance().getCurrentServer() == null) {
            return "Чтобы увидеть карту, зайдите на сервер AlphaSmp";
        }
        return this.status;
    }

    private record MapPayload(byte[] data) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
