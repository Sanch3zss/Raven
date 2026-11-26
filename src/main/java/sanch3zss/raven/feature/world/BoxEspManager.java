package sanch3zss.raven.feature.world;

import java.util.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import sanch3zss.raven.client.*;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.render.shape.ShapeRenderer;
import sanch3zss.raven.feature.world.BlockUpdateEntry;

public class BoxEspManager {
    public static final BoxEspManager INSTANCE = new BoxEspManager();
    public static final int defaultColor = -16733441;
    public final Map<Block, Integer> blocks = new HashMap<>();
    public final Map<EntityType<?>, Integer> entities = new HashMap<>();
    public final Map<BlockPos, Integer> trackedPositions = new HashMap<>();

    public BoxEspManager() {
        Raven.INSTANCE.eventDispatcher().register(BlockUpdateEvent.class, event -> {
            switch (event.type()) {
                case LOAD:
                    for (BlockUpdateEntry blockUpdateEntry2 : event.list()) {
                        Integer num = this.blocks.get(blockUpdateEntry2.state().getBlock());
                        if (num != null) {
                            this.trackedPositions.put(blockUpdateEntry2.pos(), resolveColor(num));
                        }
                    }
                    break;
                case UPDATE:
                    for (BlockUpdateEntry blockUpdateEntry : event.list()) {
                        BlockPos blockPosPos = blockUpdateEntry.pos();
                        BlockState blockStateState = blockUpdateEntry.state();
                        this.trackedPositions.remove(blockPosPos);
                        Integer num2 = this.blocks.get(blockStateState.getBlock());
                        if (num2 != null) {
                            this.trackedPositions.put(blockPosPos, resolveColor(num2));
                        }
                    }
                    break;
                case UNLOAD:
                    for (BlockUpdateEntry entry : event.list()) {
                        this.trackedPositions.remove(entry.pos());
                    }
                    break;
            }
        });
        Raven.INSTANCE.eventDispatcher().register(PlayerInitEvent.class, event -> {
            this.trackedPositions.clear();
        });
        Raven.INSTANCE.eventDispatcher().register(WorldRenderEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (mc.isWorldLoaded()) {
                if (this.trackedPositions.isEmpty() && this.entities.isEmpty()) {
                    return;
                }
                Matrix4f positionMatrix = event.matrixStack().last().pose();
                this.trackedPositions.forEach((blockPos, color) -> {
                    AABB box = new AABB(blockPos);
                    ShapeRenderer.INSTANCE.addBox(positionMatrix, box, withAlpha(color, 30));
                    ShapeRenderer.INSTANCE.addOutlineWireframe(positionMatrix, box, color, withAlpha(color, 80), 1.5f);
                });
                if (this.entities.isEmpty()) {
                    return;
                }
                mc.getWorld().getEntitiesOfClass(Entity.class, mc.getPlayer().getBoundingBox().inflate(256.0d),
                        entity -> this.entities.containsKey(entity.getType())).forEach(entity -> {
                    int entityColor = resolveColor(this.entities.get(entity.getType()));
                    AABB boundingBox = entity.getBoundingBox();
                    ShapeRenderer.INSTANCE.addBox(positionMatrix, boundingBox, withAlpha(entityColor, 30));
                    ShapeRenderer.INSTANCE.addOutlineWireframe(positionMatrix, boundingBox, entityColor, withAlpha(entityColor, 80), 1.5f);
                });
            }
        });
    }

    public Map<Block, Integer> getBlocks() {
        return this.blocks;
    }

    public Map<EntityType<?>, Integer> getEntities() {
        return this.entities;
    }

    public void clearAll() {
        this.blocks.clear();
        this.entities.clear();
        this.trackedPositions.clear();
    }

    public static int resolveColor(int color) {
        return color == 0 ? defaultColor : color;
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
