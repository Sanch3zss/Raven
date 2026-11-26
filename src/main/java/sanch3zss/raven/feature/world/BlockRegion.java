package sanch3zss.raven.feature.world;

import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.feature.world.BlockRegionIterator;

public class BlockRegion implements Iterable<BlockPos> {
    public final BlockPos min;
    public final BlockPos max;
    public final int volume;
    public static final BlockRegion EMPTY = new BlockRegion(BlockPos.ZERO, BlockPos.ZERO);

    public static BlockRegion quadAround(BlockPos blockPos, int i, int i2) {
        return new BlockRegion(blockPos.offset(-i, -i2, -i), blockPos.offset(i, i2, i));
    }

    public static BlockRegion from(BlockPos blockPos) {
        return new BlockRegion(blockPos, blockPos);
    }

    public static BlockRegion from(ChunkAccess chunk) {
        BlockPos startPos = chunk.getPos().getWorldPosition();
        return new BlockRegion(new BlockPos(startPos.getX(), chunk.getMinY(), startPos.getZ()), new BlockPos(startPos.getX() + 15, chunk.getMaxY(), startPos.getZ() + 15));
    }

    public static BlockRegion fromChunkPos(int i, int i2) {
        return new BlockRegion(new BlockPos(i << 4, Mc.INSTANCE.getWorld().getMinY(), i2 << 4), new BlockPos((i << 4) | 15, Mc.INSTANCE.getWorld().getMaxY(), (i2 << 4) | 15));
    }

    public static AABB getBox(BlockRegion blockRegion) {
        return new AABB(0.0d, 0.0d, 0.0d, ((double) (blockRegion.max.getX() - blockRegion.min.getX())) + 1.0d, ((double) (blockRegion.max.getY() - blockRegion.min.getY())) + 1.0d, ((double) (blockRegion.max.getZ() - blockRegion.min.getZ())) + 1.0d);
    }

    public BlockRegion(BlockPos blockPos, BlockPos blockPos2) {
        BlockPos blockPos3 = new BlockPos(Math.min(blockPos.getX(), blockPos2.getX()), Math.min(blockPos.getY(), blockPos2.getY()), Math.min(blockPos.getZ(), blockPos2.getZ()));
        BlockPos blockPos4 = new BlockPos(Math.max(blockPos.getX(), blockPos2.getX()), Math.max(blockPos.getY(), blockPos2.getY()), Math.max(blockPos.getZ(), blockPos2.getZ()));
        this.min = blockPos3;
        this.max = blockPos4;
        this.volume = (blockPos4.getX() - blockPos3.getX()) * (blockPos4.getY() - blockPos3.getY()) * (blockPos4.getZ() - blockPos3.getZ());
    }

    public int[] xBounds() {
        return new int[]{this.min.getX(), this.max.getX()};
    }

    public int[] yBounds() {
        return new int[]{this.min.getY(), this.max.getY()};
    }

    public int[] zBounds() {
        return new int[]{this.min.getZ(), this.max.getZ()};
    }

    public boolean isEmpty() {
        return this.volume == 0;
    }

    public boolean contains(BlockRegion blockRegion) {
        int[] xBounds = xBounds();
        int[] yBounds = yBounds();
        int[] zBounds = zBounds();
        int[] targetX = {blockRegion.min.getX(), blockRegion.max.getX()};
        int[] targetY = {blockRegion.min.getY(), blockRegion.max.getY()};
        int[] targetZ = {blockRegion.min.getZ(), blockRegion.max.getZ()};
        return targetX[0] >= xBounds[0] && targetX[1] <= xBounds[1] && targetY[0] >= yBounds[0] && targetY[1] <= yBounds[1] && targetZ[0] >= zBounds[0] && targetZ[1] <= zBounds[1];
    }

    public boolean contains(BlockPos blockPos) {
        return blockPos.getX() >= this.min.getX() && blockPos.getX() <= this.max.getX() && blockPos.getY() >= this.min.getY() && blockPos.getY() <= this.max.getY() && blockPos.getZ() >= this.min.getZ() && blockPos.getZ() <= this.max.getZ();
    }

    public boolean intersects(BlockRegion blockRegion) {
        return overlaps(new Vec3i(blockRegion.min.getX(), blockRegion.min.getY(), blockRegion.min.getZ()), new Vec3i(blockRegion.max.getX(), blockRegion.max.getY(), blockRegion.max.getZ()));
    }

    public boolean overlaps(Vec3i vec3i, Vec3i vec3i2) {
        return this.max.getX() > vec3i.getX() && this.min.getX() < vec3i2.getX() && this.max.getY() > vec3i.getY() && this.min.getY() < vec3i2.getY() && this.max.getZ() > vec3i.getZ() && this.min.getZ() < vec3i2.getZ();
    }

    public Vec3 getBottomFaceCenter() {
        return new Vec3(((double) ((this.min.getX() + this.max.getX()) + 1)) / 2.0d, this.min.getY(), ((double) ((this.min.getZ() + this.max.getZ()) + 1)) / 2.0d);
    }

    public AABB getBoundingBox() {
        return new AABB(this.min.getX(), this.min.getY(), this.min.getZ(), ((double) this.max.getX()) + 1.0d, ((double) this.max.getY()) + 1.0d, ((double) this.max.getZ()) + 1.0d);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BlockRegion)) {
            return false;
        }
        BlockRegion blockRegion = (BlockRegion) obj;
        return this.min.equals(blockRegion.min) && this.max.equals(blockRegion.max);
    }

    @Override
    public int hashCode() {
        return (31 * this.min.hashCode()) + this.max.hashCode();
    }

    public BlockRegion intersection(BlockRegion blockRegion) {
        return new BlockRegion(new BlockPos(Math.max(this.min.getX(), blockRegion.min.getX()), Math.max(this.min.getY(), blockRegion.min.getY()), Math.max(this.min.getZ(), blockRegion.min.getZ())), new BlockPos(Math.min(this.max.getX(), blockRegion.max.getX()), Math.min(this.max.getY(), blockRegion.max.getY()), Math.min(this.max.getZ(), blockRegion.max.getZ())));
    }

    public BlockRegion union(BlockRegion blockRegion) {
        return new BlockRegion(new BlockPos(Math.min(this.min.getX(), blockRegion.min.getX()), Math.min(this.min.getY(), blockRegion.min.getY()), Math.min(this.min.getZ(), blockRegion.min.getZ())), new BlockPos(Math.max(this.max.getX(), blockRegion.max.getX()), Math.max(this.max.getY(), blockRegion.max.getY()), Math.max(this.max.getZ(), blockRegion.max.getZ())));
    }

    @Override
    public String toString() {
        return "[" + this.min.getX() + "," + this.min.getY() + "," + this.min.getZ() + "] -> [" + this.max.getX() + "," + this.max.getY() + "," + this.max.getZ() + "]";
    }

    @Override
    @NotNull
    public Iterator<BlockPos> iterator() {
        return new BlockRegionIterator(this);
    }
}
