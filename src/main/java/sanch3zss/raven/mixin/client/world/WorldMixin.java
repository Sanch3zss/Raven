package sanch3zss.raven.mixin.client.world;

import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.player.BlockUpdateEvent;
import sanch3zss.raven.feature.world.BlockUpdateType;
import sanch3zss.raven.feature.world.BlockUpdateEntry;

@Mixin({Level.class})
public abstract class WorldMixin {
    @Inject(method = {"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"}, at = {@At("RETURN")})
    private void setBlockStateHook(BlockPos blockPos, BlockState blockState, int i, int i2, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if ((Object) this instanceof ClientLevel) {
            BlockPos immutable = blockPos.immutable();
            Raven.INSTANCE.eventDispatcher().dispatch(new BlockUpdateEvent(List.of(new BlockUpdateEntry(immutable, blockState, 0)), BlockUpdateType.UPDATE));
        }
    }
}
