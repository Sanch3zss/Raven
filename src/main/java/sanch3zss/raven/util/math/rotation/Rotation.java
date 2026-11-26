package sanch3zss.raven.util.math.rotation;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.util.math.MathUtil;

public class Rotation {
    public float yaw;
    public float pitch;
    public static Rotation ZERO = new Rotation(0.0f, 0.0f);

    public static Rotation playerRotation() {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        return player == null ? ZERO : new Rotation(player.getYRot(), player.getXRot());
    }

    public Rotation random(float f) {
        return add(MathUtil.getRandom(-f, f), MathUtil.getRandom(-f, f));
    }

    public Rotation add(float f, float f2) {
        return new Rotation(this.yaw + f, MathUtil.clamp(this.pitch + f2, -90.0f, 90.0f));
    }

    public Rotation add(Rotation rotation) {
        return new Rotation(this.yaw + rotation.yaw, this.pitch + rotation.pitch);
    }

    public Vec3 getDirectionVector() {
        return RotationMath.INSTANCE.rotationToVector(this);
    }

    public float withFixedYaw(Rotation rotation) {
        return rotation.getYaw() + angleDifference(Mc.INSTANCE.getPlayer().getYRot(), rotation.getYaw());
    }

    public static float angleDifference(float f, float f2) {
        return Mth.wrapDegrees(f - f2);
    }

    public float angleTo(Rotation rotation) {
        return Math.min(rotationDeltaTo(rotation).length(), 180.0f);
    }

    public RotationDelta rotationDeltaTo(Rotation rotation) {
        return new RotationDelta(angleDifference(rotation.yaw, this.yaw), angleDifference(rotation.pitch, this.pitch));
    }

    public Rotation normalize() {
        Rotation currentRotation = RotationManager.INSTANCE.getCurrentRotation();
        double dComputeGcd = MathUtil.computeGcd();
        RotationDelta rotationDeltaTo = currentRotation.rotationDeltaTo(this);
        return new Rotation(currentRotation.yaw + ((float) (((double) ((int) (((double) rotationDeltaTo.deltaYaw()) / dComputeGcd))) * dComputeGcd)), MathUtil.clamp(currentRotation.pitch + ((float) (((double) ((int) (((double) rotationDeltaTo.deltaPitch()) / dComputeGcd))) * dComputeGcd)), -90.0f, 90.0f));
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setYaw(float f) {
        this.yaw = f;
    }

    public void setPitch(float f) {
        this.pitch = f;
    }

    public Rotation(float f, float f2) {
        this.yaw = f;
        this.pitch = f2;
    }
}
