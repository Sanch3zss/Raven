package sanch3zss.raven.util.math.rotation;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RotationMath {
    public static final RotationMath INSTANCE = new RotationMath();

    public Vec3 rotationToVector(Rotation rotation2) {
        float radians = (float) Math.toRadians(rotation2.getPitch());
        float radians2 = (float) Math.toRadians(-rotation2.getYaw());
        float fCos = Mth.cos(radians2);
        float fSin = Mth.sin(radians2);
        float fCos2 = Mth.cos(radians);
        return new Vec3(fSin * fCos2, -Mth.sin(radians), fCos * fCos2);
    }

    public Rotation fromVec3d(Vec3 vec3d) {
        return new Rotation((float) Mth.wrapDegrees(Math.toDegrees(Math.atan2(vec3d.z, vec3d.x)) - 90.0d), (float) Mth.wrapDegrees(Math.toDegrees(-Math.atan2(vec3d.y, Math.hypot(vec3d.x, vec3d.z)))));
    }

    public Rotation fromVec3d(Vec3 vec3d, Vec3 vec3d2) {
        return fromVec3d(vec3d.subtract(vec3d2));
    }

    public Rotation calculateRotationDifference(Rotation rotation2, Rotation rotation) {
        return new Rotation(Mth.wrapDegrees(rotation2.getYaw() - rotation.getYaw()), rotation2.getPitch() - rotation.getPitch());
    }
}
