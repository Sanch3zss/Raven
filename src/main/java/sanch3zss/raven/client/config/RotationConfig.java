package sanch3zss.raven.client.config;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import sanch3zss.raven.util.math.rotation.*;

public class RotationConfig {
    public static final RotationConfig LINEAR = new RotationConfig(new SpeedRotationMode(200.0f, 180.0f), false, false, false);
    public static final RotationConfig LINEAR_WITH_CORRECTION = new RotationConfig(new SpeedRotationMode(180.0f, 180.0f), false, true, false);
    public static final RotationConfig BLOCK_WITH_CORRECTION = new RotationConfig(new BlockRotationMode(), false, true, false);
    public static final RotationConfig LINEAR_WITH_FOCUSED_CORRECTION = new RotationConfig(new SpeedRotationMode(180.0f, 180.0f), false, true, true);
    public final RotationMode rotationMode;
    public final boolean clientRotation;
    public final boolean moveCorrection;
    public final boolean focusedCorrection;

    public RotationConfig() {
        this(new SpeedRotationMode(180.0f, 180.0f), false, false, false);
    }

    public RotationConfig(RotationMode rotationMode, boolean z, boolean z2) {
        this(rotationMode, false, z, z2);
    }

    public RotationConfig(boolean z, boolean z2, boolean z3) {
        this(new SpeedRotationMode(180.0f, 180.0f), z, z2, z3);
    }

    public RotationConfig(boolean z, boolean z2) {
        this(new SpeedRotationMode(180.0f, 180.0f), z, true, z2);
    }

    public ScheduledRotation createRotationStrategy(Rotation rotation, Vec3 vec3d, Entity entity, int i) {
        return new ScheduledRotation(rotation, this.rotationMode, vec3d, entity, 3, i, this.clientRotation, this.moveCorrection, this.focusedCorrection);
    }

    public ScheduledRotation createRotationStrategy(Rotation rotation, int i) {
        return new ScheduledRotation(rotation, this.rotationMode, null, null, 3, i, this.clientRotation, this.moveCorrection, this.focusedCorrection);
    }

    public ScheduledRotation createRotationStrategy(Rotation rotation, Vec3 vec3d, Entity entity, boolean z, boolean z2, boolean z3, int i) {
        return new ScheduledRotation(rotation, this.rotationMode, vec3d, entity, 3, i, z, z2, z3);
    }

    public RotationConfig(RotationMode rotationMode, boolean z, boolean z2, boolean z3) {
        this.rotationMode = rotationMode;
        this.clientRotation = z;
        this.moveCorrection = z2;
        this.focusedCorrection = z3;
    }
}
