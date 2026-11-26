package sanch3zss.raven.util.math.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class SpeedRotationMode implements RotationMode {
    public final float yawSpeed;
    public final float pitchSpeed;

    public SpeedRotationMode(float yawSpeed, float pitchSpeed) {
        this.yawSpeed = yawSpeed;
        this.pitchSpeed = pitchSpeed;
    }

    @Override
    public Rotation process(Rotation current, Rotation target) {
        return process(current, target, null, null);
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3 targetVec, Entity targetEntity) {
        if (current == null) return target;
        float yawDiff = Mth.wrapDegrees(target.getYaw() - current.getYaw());
        float pitchDiff = target.getPitch() - current.getPitch();
        float yawStep = Math.min(Math.abs(yawDiff), this.yawSpeed) * Math.signum(yawDiff);
        float pitchStep = Math.min(Math.abs(pitchDiff), this.pitchSpeed) * Math.signum(pitchDiff);
        return new Rotation(current.getYaw() + yawStep, current.getPitch() + pitchStep);
    }
}
