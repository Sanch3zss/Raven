package sanch3zss.raven.util.math.rotation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public interface RotationMode {
    Rotation process(Rotation current, Rotation target);
    Rotation process(Rotation current, Rotation target, Vec3 targetVec, Entity targetEntity);
}
