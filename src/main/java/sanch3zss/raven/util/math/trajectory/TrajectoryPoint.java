package sanch3zss.raven.util.math.trajectory;

import net.minecraft.world.phys.Vec3;

public class TrajectoryPoint {
    public final int tick;
    public final Vec3 velocity;
    public final Vec3 position;
    public final Vec3 pos;

    public TrajectoryPoint(int i, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3) {
        this.tick = i;
        this.velocity = vec3d;
        this.position = vec3d2;
        this.pos = vec3d3;
    }

    public Vec3 pos() {
        return this.pos;
    }

    public int tick() {
        return this.tick;
    }
}
