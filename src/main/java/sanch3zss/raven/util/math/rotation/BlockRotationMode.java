package sanch3zss.raven.util.math.rotation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class BlockRotationMode implements RotationMode {
    @Override
    public Rotation process(Rotation current, Rotation target) {
        return target;
    }

    @Override
    public Rotation process(Rotation current, Rotation target, Vec3 targetVec, Entity targetEntity) {
        return target;
    }
}
