package sanch3zss.raven.feature.module.render.projectile;


import sanch3zss.raven.feature.entity.EntityLifecycleAction;
import sanch3zss.raven.util.math.trajectory.TrajectoryPoint;
import java.util.*;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.ClientLevel;
import sanch3zss.raven.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import sanch3zss.raven.client.*;
import sanch3zss.raven.event.EventPriority;
import sanch3zss.raven.event.network.ChunkLoadEvent;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.feature.module.render.projectile.ProjectilePredictionModule;
import sanch3zss.raven.feature.module.render.projectile.DummyLivingEntity;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.util.math.trajectory.ProjectileTrajectory;

public class PlayerSnapshotManager {
    public static final PlayerSnapshotManager INSTANCE = new PlayerSnapshotManager();
    public final List<ProjectileTrajectory> projectiles = new ArrayList<>();
    public final Mc wrapper = Mc.INSTANCE;
    public final int maxTicks = 40;
    public final Map<Integer, ArrayDeque<PlayerSnapshot>> historyById = new HashMap<>();

    public PlayerSnapshotManager() {
        Raven.INSTANCE.eventDispatcher().register(PlayerInitEvent.class, event4 -> {
            this.historyById.clear();
            this.projectiles.clear();
        });
        Raven.INSTANCE.eventDispatcher().register(ChunkLoadEvent.class, event -> {
            ChunkPos chunkPos = event.chunkPos();
            this.projectiles.stream().filter(traj -> traj.chunks.contains(chunkPos)).forEach(traj -> {
                ProjectilePredictionModule projectilePredictionModule = (ProjectilePredictionModule) Raven.INSTANCE.moduleRepository().get(ProjectilePredictionModule.class);
                TrajectoryPoint trajectoryPoint = traj.steps.getFirst();
                Entity entity = traj.entity;
                Tuple<List<ChunkPos>, List<TrajectoryPoint>> pairPredictEntity = projectilePredictionModule.predictEntity(entity, trajectoryPoint.velocity, trajectoryPoint.position);
                traj.chunks.clear();
                traj.steps.clear();
                traj.chunks.addAll(pairPredictEntity.getA());
                traj.steps.addAll(pairPredictEntity.getB());
            });
        });
        Raven.INSTANCE.eventDispatcher().register(EntityLifecycleEvent.class, event2 -> {
            ProjectilePredictionModule projectilePredictionModule = (ProjectilePredictionModule) Raven.INSTANCE.moduleRepository().get(ProjectilePredictionModule.class);
            Entity thrownItemEntityEntity = event2.entity();
            if (thrownItemEntityEntity == Mc.INSTANCE.getPlayer() || event2.type() != EntityLifecycleAction.ADD) {
                return;
            }
            if (thrownItemEntityEntity instanceof ThrowableItemProjectile thrownItemEntity) {
                registerProjectile(thrownItemEntity, thrownItemEntity.getItem(), projectilePredictionModule.predictEntity(thrownItemEntity, thrownItemEntity.getDeltaMovement(), thrownItemEntity.position()));
            } else if (thrownItemEntityEntity instanceof AbstractArrow persistentProjectileEntity) {
                registerProjectile(persistentProjectileEntity, persistentProjectileEntity.getPickupItemStackOrigin(), projectilePredictionModule.predictEntity(persistentProjectileEntity, persistentProjectileEntity.getDeltaMovement(), persistentProjectileEntity.position()));
            }
        });
        Raven.INSTANCE.eventDispatcher().register(PlayerTickEvent.class, event3 -> {
            ClientLevel world;
            if (event3.isPre() && (world = this.wrapper.getWorld()) != null) {
                this.projectiles.removeIf(ProjectileTrajectory::tick);
                while (this.projectiles.size() > 150) {
                    this.projectiles.remove(0);
                }
                HashSet<Integer> hashSet = new HashSet<>(512);
                for (Entity livingEntity : world.entitiesForRendering()) {
                    if (livingEntity instanceof LivingEntity livingEntity2) {
                        int id = livingEntity2.getId();
                        hashSet.add(id);
                        ArrayDeque<PlayerSnapshot> arrayDequeComputeIfAbsent = this.historyById.computeIfAbsent(id, num -> new ArrayDeque<>(41));
                        arrayDequeComputeIfAbsent.addFirst(PlayerSnapshot.from(livingEntity2));
                        while (arrayDequeComputeIfAbsent.size() > 41) {
                            arrayDequeComputeIfAbsent.removeLast();
                        }
                    }
                }
                this.historyById.keySet().removeIf(id -> !hashSet.contains(id));
            }
        }, EventPriority.HIGHEST);
    }

    public LivingEntity createEntity(EntityType<?> entityType) {
        return new DummyLivingEntity(this, entityType, this.wrapper.getWorld());
    }

    public Optional<PlayerSnapshot> getSnapshot(LivingEntity livingEntity, int i) {
        ArrayDeque<PlayerSnapshot> arrayDeque = this.historyById.get(livingEntity.getId());
        if (arrayDeque == null) {
            return Optional.empty();
        }
        if (i < 0 || i > 40) {
            return Optional.empty();
        }
        int i2 = 0;
        for (PlayerSnapshot playerSnapshot : arrayDeque) {
            int i3 = i2;
            i2++;
            if (i3 == i) {
                return Optional.of(playerSnapshot);
            }
        }
        return Optional.empty();
    }

    public void registerProjectile(Entity entity, ItemStack itemStack, Tuple<List<ChunkPos>, List<TrajectoryPoint>> pair) {
        this.projectiles.removeIf(traj -> traj.entity.getUUID().equals(entity.getUUID()));
        this.projectiles.add(new ProjectileTrajectory(entity, itemStack, pair.getA(), pair.getB()));
    }

    public PlayerSnapshot getSnapshotOrDefault(LivingEntity livingEntity, int i) {
        return getSnapshot(livingEntity, i).orElse(PlayerSnapshot.from(livingEntity));
    }

    public Stream<PlayerSnapshot> getSnapshots(LivingEntity livingEntity, int i) {
        ArrayDeque<PlayerSnapshot> arrayDeque = this.historyById.get(livingEntity.getId());
        if (arrayDeque == null) {
            return Stream.empty();
        }
        return arrayDeque.stream().limit(Math.min(Math.max(i, 0), 40) + 1);
    }

    public List<ProjectileTrajectory> getProjectiles() {
        return this.projectiles;
    }

    public Mc getWrapper() {
        return this.wrapper;
    }

    public int getMAX_TICKS() {
        return 40;
    }

    public Map<Integer, ArrayDeque<PlayerSnapshot>> getHistoryById() {
        return this.historyById;
    }
}
