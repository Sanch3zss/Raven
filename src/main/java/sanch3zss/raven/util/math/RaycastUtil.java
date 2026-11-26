package sanch3zss.raven.util.math;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.util.math.rotation.*;

public final class RaycastUtil {
    public static final Mc mc = Mc.INSTANCE;

    public static BlockHitResult raycast(double d, Rotation rotation, boolean z) {
        Entity cameraEntity = Mc.INSTANCE.getCameraEntity();
        if (cameraEntity == null) {
            return null;
        }
        Vec3[] vec3dArrMethod002 = getRayEndpoints(cameraEntity, rotation, d);
        return Mc.INSTANCE.getWorld().clip(new ClipContext(vec3dArrMethod002[0], vec3dArrMethod002[1], ClipContext.Block.OUTLINE, z ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, cameraEntity));
    }

    public static BlockHitResult raycast(float f, double d, Rotation rotation, boolean z) {
        return raycast(mc.getPlayer().getEyePosition(f), d, rotation, z);
    }

    public static BlockHitResult raycast(Vec3 vec3d, double d, Rotation rotation, boolean z) {
        return mc.getWorld().clip(new ClipContext(vec3d, vec3d.add(RotationMath.INSTANCE.rotationToVector(rotation)).scale(d), ClipContext.Block.OUTLINE, z ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, mc.getPlayer()));
    }

    public static BlockHitResult raycast(Vec3 vec3d, Vec3 vec3d2, ClipContext.Block shapeType) {
        return raycast(vec3d, vec3d2, shapeType, (Entity) mc.getPlayer());
    }

    public static BlockHitResult raycast(Vec3 vec3d, Vec3 vec3d2, ClipContext.Block shapeType, ClipContext.Fluid fluidHandling, Entity entity) {
        return mc.getWorld().clip(new ClipContext(vec3d, vec3d2, shapeType, fluidHandling, entity));
    }

    public static BlockHitResult raycast(Vec3 vec3d, Vec3 vec3d2, ClipContext.Block shapeType, Entity entity) {
        return mc.getWorld().clip(new ClipContext(vec3d, vec3d2, shapeType, ClipContext.Fluid.NONE, entity));
    }

    public static Vec3[] getRayEndpoints(Entity entity, Rotation rotation, double d) {
        Vec3 eyePos = entity.getEyePosition();
        return new Vec3[]{eyePos, eyePos.add(RotationMath.INSTANCE.rotationToVector(rotation).scale(d))};
    }

    public RaycastUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
