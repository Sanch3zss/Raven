package sanch3zss.raven.feature.module.render.projectile;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class PlayerSnapshot {
    public final AABB box;
    public final Vec3 pos;
    public final boolean verticalCollision;

    public PlayerSnapshot(AABB box, Vec3 vec3d, boolean z) {
        this.box = box;
        this.pos = vec3d;
        this.verticalCollision = z;
    }

    public static PlayerSnapshot from(LivingEntity livingEntity) {
        return new PlayerSnapshot(livingEntity.getBoundingBox(), livingEntity.position(), livingEntity.verticalCollision);
    }
}
