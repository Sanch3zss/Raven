package sanch3zss.raven.feature.module.render.leavetracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.event.network.WorldLoadEvent;
import sanch3zss.raven.event.player.EntityLifecycleEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.entity.EntityLifecycleAction;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerRenderState;
import sanch3zss.raven.feature.module.render.party.PartyRenderState;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

@Aliases(aliases = {"Leave Tracker", "Logout Spot", "Logout Ghost"})
public class LeaveTrackerModule extends Module {
    private static final long RECENT_NANOS = 3_000_000_000L;
    private final Map<UUID, LastSeen> lastSeen = new HashMap<>();
    private final Map<UUID, Ghost> ghosts = new LinkedHashMap<>();
    private final Set<UUID> awaitingRemoval = new HashSet<>();
    private ClientPacketListener sessionConnection;

    public LeaveTrackerModule() {
        super(ModuleTab.VISUALS, "LeaveTracker");
        register(EntityLifecycleEvent.class, this::onEntityLifecycle);
        register(WorldLoadEvent.class, event -> {
            this.lastSeen.clear();
            this.awaitingRemoval.clear();
            if (event.world() == null) {
                this.ghosts.clear();
                this.sessionConnection = null;
            } else {
                ClientPacketListener connection = Mc.INSTANCE.getNetworkHandler();
                if (this.sessionConnection != null && this.sessionConnection != connection) {
                    this.ghosts.clear();
                }
                this.sessionConnection = connection;
            }
        });
        register(ClientTickEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (!mc.isWorldLoaded()) {
                this.lastSeen.clear();
                this.awaitingRemoval.clear();
                return;
            }
            this.ghosts.keySet().removeIf(id -> mc.getNetworkHandler().getPlayerInfo(id) != null);
            long now = System.nanoTime();
            for (UUID id : new ArrayList<>(this.awaitingRemoval)) {
                if (mc.getNetworkHandler().getPlayerInfo(id) != null) {
                    this.awaitingRemoval.remove(id);
                } else if (mc.getWorld().getPlayerByUUID(id) == null) {
                    recordGhost(id, now);
                }
            }
        });
    }

    public void capture(List<EntityRenderState> states) {
        Mc mc = Mc.INSTANCE;
        if (!mc.isWorldLoaded()) {
            return;
        }
        long now = System.nanoTime();
        for (EntityRenderState state : states) {
            if (!(state instanceof AvatarRenderState avatar)) {
                continue;
            }
            UUID id = ((PartyRenderState) state).raven$partyEntityId();
            if (id == null || id.equals(mc.getPlayer().getUUID())
                    || mc.getNetworkHandler().getPlayerInfo(id) == null) {
                continue;
            }
            Player player = mc.getWorld().getPlayerByUUID(id);
            if (player == null) {
                continue;
            }
            String profileName = player.getName().getString().toLowerCase(Locale.ROOT);
            clearOtherCopies(id, profileName);
            this.lastSeen.put(id, new LastSeen(avatar, mc.getWorld().dimension(), profileName,
                    player.getDisplayName(), player.belowNameDisplay(), now));
            this.ghosts.remove(id);
            this.awaitingRemoval.remove(id);
        }
    }

    public void onProfilesRemoved(List<UUID> ids) {
        if (!Mc.INSTANCE.isWorldLoaded()) {
            return;
        }
        long now = System.nanoTime();
        for (UUID id : ids) {
            if (this.lastSeen.containsKey(id)) {
                this.awaitingRemoval.add(id);
                if (Mc.INSTANCE.getWorld().getPlayerByUUID(id) == null) {
                    recordGhost(id, now);
                }
            }
        }
    }

    private void onEntityLifecycle(EntityLifecycleEvent event) {
        if (!(event.entity() instanceof Player player) || player == Mc.INSTANCE.getPlayer()) {
            return;
        }
        UUID id = player.getUUID();
        if (event.type() == EntityLifecycleAction.ADD) {
            clearOtherCopies(id, player.getName().getString().toLowerCase(Locale.ROOT));
            this.ghosts.remove(id);
            this.awaitingRemoval.remove(id);
        } else {
            LastSeen seen = this.lastSeen.get(id);
            if (seen != null) {
                seen.state.x = player.getX();
                seen.state.y = player.getY();
                seen.state.z = player.getZ();
                this.lastSeen.put(id, new LastSeen(seen.state, seen.dimension, seen.profileName,
                        player.getDisplayName(), player.belowNameDisplay(), System.nanoTime()));
            }
            if (this.awaitingRemoval.contains(id)) {
                recordGhost(id, System.nanoTime());
            }
        }
    }

    private void recordGhost(UUID id, long now) {
        this.awaitingRemoval.remove(id);
        LastSeen seen = this.lastSeen.remove(id);
        if (seen == null || now - seen.updatedAt > RECENT_NANOS) {
            return;
        }
        this.ghosts.values().removeIf(ghost -> ghost.profileName.equals(seen.profileName));
        AvatarRenderState state = seen.state;
        if (state.nameTag == null) {
            state.nameTag = seen.name;
        }
        if (state.scoreText == null) {
            state.scoreText = seen.score;
        }
        if (state.nameTagAttachment == null) {
            state.nameTagAttachment = new Vec3(0.0, state.boundingBoxHeight, 0.0);
        }
        ((LeaveTrackerRenderState) state).raven$setLeaveTrackerGhost(true);
        ((LeaveTrackerRenderState) state).raven$setLeftAtNanos(now);
        state.shadowPieces.clear();
        state.shadowRadius = 0.0f;
        state.displayFireAnimation = false;
        state.outlineColor = 0;
        state.leashStates = null;
        this.ghosts.put(id, new Ghost(state, seen.dimension, seen.profileName));
    }

    private void clearOtherCopies(UUID id, String profileName) {
        this.awaitingRemoval.removeIf(other -> !other.equals(id)
                && this.lastSeen.containsKey(other)
                && this.lastSeen.get(other).profileName.equals(profileName));
        this.lastSeen.entrySet().removeIf(entry -> !entry.getKey().equals(id)
                && entry.getValue().profileName.equals(profileName));
        this.ghosts.values().removeIf(ghost -> ghost.profileName.equals(profileName));
    }

    public void appendGhosts(Frustum frustum, LevelRenderState output) {
        Mc mc = Mc.INSTANCE;
        if (!isState() || !mc.isWorldLoaded() || this.ghosts.isEmpty()) {
            return;
        }
        Vec3 camera = mc.getCamera().position();
        double maxDistance = Math.max(64, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16);
        Set<String> renderedNames = new HashSet<>();
        for (Map.Entry<UUID, Ghost> entry : this.ghosts.entrySet()) {
            Ghost ghost = entry.getValue();
            AvatarRenderState state = ghost.state;
            if (!ghost.dimension.equals(mc.getWorld().dimension())
                    || mc.getWorld().getPlayerByUUID(entry.getKey()) != null
                    || camera.distanceToSqr(state.x, state.y, state.z) > maxDistance * maxDistance) {
                continue;
            }
            double halfWidth = state.boundingBoxWidth * 0.5;
            AABB bounds = new AABB(state.x - halfWidth, state.y, state.z - halfWidth,
                    state.x + halfWidth, state.y + state.boundingBoxHeight, state.z + halfWidth);
            if (frustum.isVisible(bounds) && renderedNames.add(ghost.profileName)) {
                output.entityRenderStates.add(state);
            }
        }
        output.lastEntityRenderStateCount = output.entityRenderStates.size();
    }

    private record LastSeen(AvatarRenderState state, ResourceKey<Level> dimension, String profileName,
                            Component name, Component score, long updatedAt) {
    }

    private record Ghost(AvatarRenderState state, ResourceKey<Level> dimension, String profileName) {
    }
}
