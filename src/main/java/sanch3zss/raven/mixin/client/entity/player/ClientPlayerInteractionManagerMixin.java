package sanch3zss.raven.mixin.client.entity.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.player.*;

@Mixin({MultiPlayerGameMode.class})
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    private void raven$beforeThrowSlot(int syncId, int slotId, int button, ContainerInput actionType,
                                      Player player, CallbackInfo callbackInfo) {
        if (actionType != ContainerInput.THROW) {
            return;
        }
        int inventorySlot = slotId;
        if (slotId >= 36 && slotId < 45) {
            inventorySlot = slotId - 36;
        }
        DropItemEvent event = new DropItemEvent(inventorySlot, button == 1);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"startDestroyBlock"}, at = {@At("HEAD")}, cancellable = true)
    private void attackBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        BlockBreakEvent2 blockBreakEvent2 = new BlockBreakEvent2();
        Raven.INSTANCE.eventDispatcher().dispatch(blockBreakEvent2);
        if (blockBreakEvent2.isCancelled()) {
            callbackInfoReturnable.setReturnValue(false);
        }
    }

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void continueBlock(BlockPos blockPos, Direction direction,
                               CallbackInfoReturnable<Boolean> result) {
        BlockBreakEvent2 event = new BlockBreakEvent2();
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) result.setReturnValue(false);
    }

    @ModifyExpressionValue(method = {"ensureHasSentCarriedItem"}, at = {@At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I")}, require = 0)
    private int hookCustomSelectedSlot(int i) {
        return Raven.INSTANCE.inventoryService().hotbarSlotSwapper().getServersideSlot();
    }

    @Inject(method = {"attack"}, at = {@At("HEAD")}, cancellable = true)
    private void attackEntity(Player playerEntity, Entity entity, CallbackInfo callbackInfo) {
        Minecraft client = Minecraft.getInstance();
        if (playerEntity == client.player
                && (!playerEntity.isWithinAttackRange(playerEntity.getMainHandItem(), entity.getBoundingBox(), 0.0)
                || !(client.hitResult instanceof EntityHitResult hitResult)
                || hitResult.getEntity() != entity)) {
            callbackInfo.cancel();
            return;
        }

        AttackEntityEvent event = new AttackEntityEvent(entity);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
