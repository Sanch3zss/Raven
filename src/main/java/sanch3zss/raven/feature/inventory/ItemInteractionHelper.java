package sanch3zss.raven.feature.inventory;

import java.util.Objects;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import org.apache.commons.lang3.mutable.MutableObject;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.util.math.rotation.Rotation;

public class ItemInteractionHelper {
    public InteractionResult interactItem(Player playerEntity, InteractionHand hand, Rotation rotation) {
        MultiPlayerGameMode interactionManager = Mc.INSTANCE.getInteractionManager();
        if (interactionManager.getPlayerMode() == GameType.SPECTATOR) {
            return InteractionResult.PASS;
        }
        interactionManager.ensureHasSentCarriedItem();
        MutableObject mutableObject = new MutableObject();
        interactionManager.startPrediction(Mc.INSTANCE.getWorld(), i -> {
            ServerboundUseItemPacket playerInteractItemC2SPacket = new ServerboundUseItemPacket(hand, i, rotation.getYaw(), rotation.getPitch());
            ItemStack stackInHand = playerEntity.getItemInHand(hand);
            if (playerEntity.getCooldowns().isOnCooldown(stackInHand)) {
                mutableObject.setValue(InteractionResult.PASS);
                return playerInteractItemC2SPacket;
            }
            InteractionResult.Success successUse = (InteractionResult.Success) (stackInHand.use(Mc.INSTANCE.getWorld(), playerEntity, hand));
            ItemStack stackInHand2 = successUse instanceof InteractionResult.Success ? (ItemStack) Objects.requireNonNullElseGet(successUse.heldItemTransformedTo(), () -> {
                return playerEntity.getItemInHand(hand);
            }) : playerEntity.getItemInHand(hand);
            if (stackInHand2 != stackInHand) {
                playerEntity.setItemInHand(hand, stackInHand2);
            }
            mutableObject.setValue(successUse);
            return playerInteractItemC2SPacket;
        });
        return (InteractionResult) mutableObject.getValue();
    }
}
