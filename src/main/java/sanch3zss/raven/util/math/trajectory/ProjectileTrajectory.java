package sanch3zss.raven.util.math.trajectory;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import sanch3zss.raven.util.math.trajectory.TrajectoryPoint;

public class ProjectileTrajectory {
    public final List<ChunkPos> chunks;
    public final Entity entity;
    public final ItemStack stack;
    public final List<TrajectoryPoint> steps;

    public ProjectileTrajectory(Entity entity, ItemStack itemStack, List list, List list2) {
        this.entity = entity;
        this.stack = itemStack;
        this.chunks = list;
        this.steps = list2;
    }

    public Entity entity() {
        return this.entity;
    }

    public ItemStack stack() {
        return this.stack;
    }

    public List steps() {
        return this.steps;
    }

    public boolean tick() {
        return this.entity.isRemoved();
    }
}
