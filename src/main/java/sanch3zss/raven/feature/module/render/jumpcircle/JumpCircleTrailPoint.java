package sanch3zss.raven.feature.module.render.jumpcircle;

import net.minecraft.world.phys.Vec3;
import sanch3zss.raven.feature.module.render.jumpcircle.JumpCircleModule;

public final class JumpCircleTrailPoint {
    public final long spawnTime = System.currentTimeMillis();
    public final Vec3 position;
    final JumpCircleModule module;

    public JumpCircleTrailPoint(JumpCircleModule jumpCircleModule, Vec3 vec3d) {
        this.module = jumpCircleModule;
        this.position = vec3d;
    }

    public float getProgress() {
        return (System.currentTimeMillis() - this.spawnTime) / this.module.lifetimeSetting.currentValue();
    }
}
