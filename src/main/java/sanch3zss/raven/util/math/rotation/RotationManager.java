package sanch3zss.raven.util.math.rotation;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.config.RotationConfig;
import sanch3zss.raven.client.service.scheduling.*;
import sanch3zss.raven.event.EventPhase;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.event.network.*;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.util.math.MathUtil;

public class RotationManager {
    public static final RotationManager INSTANCE = new RotationManager();
    public final TickScheduler<ScheduledRotation> scheduler = new TickScheduler<>();

    public ScheduledRotation lastStrategy;
    public Rotation currentRotationOriginal;
    public Rotation previousRotation;
    public Rotation serverRotation;

    public RotationManager() {
        Raven.INSTANCE.eventDispatcher().register(PacketSendEvent.class, event6 -> {
            if (event6.isCancelled()) {
                return;
            }
            net.minecraft.network.protocol.Packet<?> packet = event6.getPacket();
            Rotation normalize = null;
            if (packet instanceof ServerboundMovePlayerPacket) {
                ServerboundMovePlayerPacket playerMoveC2SPacket = (ServerboundMovePlayerPacket) packet;
                if (playerMoveC2SPacket.hasRotation()) {
                    normalize = new Rotation(playerMoveC2SPacket.getYRot(1.0f), playerMoveC2SPacket.getXRot(1.0f)).normalize();
                }
            }
            if (packet instanceof ServerboundUseItemPacket) {
                ServerboundUseItemPacket playerInteractItemC2SPacket = (ServerboundUseItemPacket) packet;
                normalize = new Rotation(playerInteractItemC2SPacket.getYRot(), playerInteractItemC2SPacket.getXRot()).normalize();
            }
            if (normalize != null) {
                this.serverRotation = normalize;
            }
        });
        Raven.INSTANCE.eventDispatcher().register(PacketReceiveEvent.class, event5 -> {
            if (event5.isCancelled()) {
                return;
            }
            if ((event5.getPacket()) instanceof ClientboundPlayerPositionPacket packet ) {
                ClientboundPlayerPositionPacket playerPositionLookS2CPacket = packet;
                this.serverRotation = new Rotation(playerPositionLookS2CPacket.change().yRot(), playerPositionLookS2CPacket.change().xRot()).normalize();
            }
        });
        Raven.INSTANCE.eventDispatcher().register(ClientTickEvent.class, event3 -> {
            Raven.INSTANCE.eventDispatcher().dispatch(new RotationUpdateEvent(EventPhase.PRE));
            updateRotation();
            Raven.INSTANCE.eventDispatcher().dispatch(new RotationUpdateEvent(EventPhase.POST));
        });
        Raven.INSTANCE.eventDispatcher().register(MovementYawEvent.class, event -> {
            ScheduledRotation currentStrategy = getCurrentStrategy();
            Rotation rotation2 = this.currentRotationOriginal;
            if (currentStrategy == null || rotation2 == null || !currentStrategy.moveCorrection()) {
                return;
            }
            event.setYaw(rotation2.getYaw());
        });
        Raven.INSTANCE.eventDispatcher().register(JumpEvent.class, event2 -> {
            ScheduledRotation currentStrategy = getCurrentStrategy();
            Rotation rotation2 = this.currentRotationOriginal;
            if (currentStrategy == null || rotation2 == null || !currentStrategy.moveCorrection()) {
                return;
            }
            event2.setYaw(rotation2.getYaw());
        });
    }

    public Rotation getPreviousRotation() {
        return (this.currentRotationOriginal == null || this.previousRotation == null) ? new Rotation(Mc.INSTANCE.getPlayer().yRotO, Mc.INSTANCE.getPlayer().xRotO) : this.previousRotation;
    }

    public Rotation getServerRotation() {
        return this.serverRotation != null ? this.serverRotation : Rotation.playerRotation();
    }

    public Rotation getCurrentRotation() {
        return this.currentRotationOriginal != null ? this.currentRotationOriginal : Rotation.playerRotation();
    }

    public Rotation getMoveRotation() {
        ScheduledRotation currentStrategy = getCurrentStrategy();
        return (this.currentRotationOriginal == null || currentStrategy == null || !currentStrategy.moveCorrection()) ? Rotation.playerRotation() : this.currentRotationOriginal;
    }

    public Rotation getCurrentRotationOriginal() {
        return this.currentRotationOriginal;
    }

    public Rotation getInterpolatedRotation() {
        return MathUtil.interpolate(getPreviousRotation(), getCurrentRotation());
    }

    public void clear() {
        this.scheduler.activeTasks.clear();
        this.scheduler.tickCounter = 0;
    }

    public void scheduleRotation(RotationVector rotationVector, LivingEntity livingEntity, RotationConfig rotationConfig, int i, Module module, int i2) {
        scheduleRotation(rotationConfig.createRotationStrategy(rotationVector.rotation(), rotationVector.vec(), livingEntity, i2), i, module);
    }

    public void scheduleRotation(Rotation rotation2, RotationConfig rotationConfig, int i, Module module, int i2) {
        scheduleRotation(rotationConfig.createRotationStrategy(rotation2, i2), i, module);
    }

    public void scheduleRotation(RotationVector rotationVector, LivingEntity livingEntity, RotationConfig rotationConfig, int i, int i2, Module module) {
        scheduleRotation(rotationConfig.createRotationStrategy(rotationVector.rotation(), rotationVector.vec(), livingEntity, i2), i, module);
    }

    public void scheduleRotation(Rotation rotation2, RotationConfig rotationConfig, int i, int i2, Module module) {
        scheduleRotation(rotationConfig.createRotationStrategy(rotation2, i2), i, module);
    }

    public void scheduleRotation(ScheduledRotation scheduledRotation, int i, Module module) {
        this.scheduler.addTask(new ScheduledTask<>(1, i, module, scheduledRotation));
    }

    public void scheduleRotation(ScheduledRotation scheduledRotation, int i, Module module, int i2) {
        this.scheduler.addTask(new ScheduledTask<>(i2, i, module, scheduledRotation));
    }

    public void updateRotation() {
        ScheduledRotation currentStrategy;
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null || (currentStrategy = getCurrentStrategy()) == null) {
            return;
        }
        Rotation rotation2 = new Rotation(player.getYRot(), player.getXRot());
        Rotation processedRotation = currentStrategy.towards(
                this.currentRotationOriginal != null
                        ? this.currentRotationOriginal
                        : rotation2,
                this.scheduler.getValue() == null
        );
        Rotation normalize =
                processedRotation.normalize();
        float angleTo = normalize.angleTo(rotation2);
        int i = this.scheduler.tickCounter;
        int ticksUntilReset = currentStrategy.ticksUntilReset();
        if (this.lastStrategy == null || this.scheduler.getValue() != null || ticksUntilReset > i || (angleTo >= 2.0f && !currentStrategy.clientRotation())) {
            setRotation(normalize);
            this.lastStrategy = currentStrategy;
            if (currentStrategy.clientRotation()) {
                player.xRotO = player.getXRot();
                player.yRotO = player.getYRot();
                player.setYRot(normalize.getYaw());
                player.setXRot(normalize.getPitch());
            }
            this.scheduler.tick(1);
            return;
        }
        if (this.currentRotationOriginal != null) {
            float fWithFixedYaw = rotation2.withFixedYaw(this.currentRotationOriginal);
            LocalPlayer player2 = Mc.INSTANCE.getPlayer();
            player2.setYRot(fWithFixedYaw);
            player2.yRotO = fWithFixedYaw;
        }
        clear();
        this.lastStrategy = null;
        this.currentRotationOriginal = null;
    }

    public void setRotation(Rotation rotation2) {
        if (rotation2 == null) {
            LocalPlayer player = Mc.INSTANCE.getPlayer();
            this.previousRotation = this.currentRotationOriginal != null ? this.currentRotationOriginal : new Rotation(player.getYRot(), player.getXRot());
        } else {
            this.previousRotation = this.currentRotationOriginal;
        }
        this.currentRotationOriginal = rotation2;
    }

    public ScheduledRotation getCurrentStrategy() {
        return this.scheduler.getValue() != null ? this.scheduler.getValue() : this.lastStrategy;
    }

    public static double computeRotationDifference(Rotation rotation2, Rotation rotation) {
        return Math.hypot(Math.abs(computeAngleDifference(rotation2.getYaw(), rotation.getYaw())), Math.abs(rotation2.getPitch() - rotation.getPitch()));
    }

    public static float computeAngleDifference(float f, float f2) {
        return Mth.wrapDegrees(f - f2);
    }

}
