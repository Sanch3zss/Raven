package sanch3zss.raven.feature.inventory;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import sanch3zss.raven.client.*;
import sanch3zss.raven.feature.server.ServerUtil;
import sanch3zss.raven.util.math.rotation.Rotation;

public class PlayerActionUtil {
    public static final PlayerActionUtil INSTANCE = new PlayerActionUtil();

    public float getRemainingCooldownSeconds(Item item) {
        Mc mc = Mc.INSTANCE;
        ItemCooldowns itemCooldownManager = mc.getPlayer().getCooldowns();
        ItemCooldowns.CooldownInstance entry = (ItemCooldowns.CooldownInstance) itemCooldownManager.cooldowns.get(BuiltInRegistries.ITEM.getKey(item));
        if (entry == null) {
            return 0.0f;
        }
        return Math.max(1, (int) Math.ceil(itemCooldownManager.getCooldownPercent(item.getDefaultInstance(), mc.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false)) * (entry.endTime - entry.startTime))) / 20.0f;
    }

    public void interactItem(InteractionHand hand, Rotation rotation, boolean z) {
        Mc mc = Mc.INSTANCE;
        LocalPlayer player = mc.getPlayer();
        Raven.INSTANCE.inventoryService().itemInteractor().interactItem(player, hand, rotation);
        if (z) {
            mc.getPlayer().connection.send(new ServerboundSwingPacket(hand));
        }
    }

    public int getEmptySlots(Inventory playerInventory) {
        int i = 0;
        for (int i2 = 0; i2 < 36; i2++) {
            if (playerInventory.getItem(i2).isEmpty()) {
                i++;
            }
        }
        return i;
    }

    public void swapHand(int i, InteractionHand hand) {
        if (i == -1) {
            return;
        }
        windowClick(ContainerInput.SWAP, i, hand.equals(InteractionHand.MAIN_HAND) ? Mc.INSTANCE.getPlayer().getInventory().getSelectedSlot() : 40);
    }

    public void clickSlot(int i, int i2, int i3, ContainerInput slotActionType, boolean z) {
        windowClick(slotActionType, i2, i3, false);
    }
    public void swapHand(int i, InteractionHand hand, boolean z, boolean z2) {
        if (i == -1) {
            return;
        }
        windowClick(ContainerInput.SWAP, i, hand.equals(InteractionHand.MAIN_HAND) ? Mc.INSTANCE.getPlayer().getInventory().getSelectedSlot() : 40, !z && z2);
        if (z) {
            updateSlots(z2);
        }
    }

    public void swapHand(int i, InteractionHand hand, boolean z) {
        if (i == -1) {
            return;
        }
        windowClick(ContainerInput.SWAP, i, hand.equals(InteractionHand.MAIN_HAND) ? Mc.INSTANCE.getPlayer().getInventory().getSelectedSlot() : 40);
        if (z) {
            updateSlots(false);
        }
    }

    public void swapHand(int i, int i2, boolean z) {
        if (i == -1) {
            return;
        }
        windowClick(ContainerInput.SWAP, i, i2);
        if (z) {
            updateSlots(false);
        }
    }

    public void swapHand(int i, int i2, boolean z, boolean z2) {
        if (i == -1) {
            return;
        }
        windowClick(ContainerInput.SWAP, i, i2, !z && z2);
        if (z) {
            updateSlots(z2);
        }
    }

    public void updateSlots(boolean z) {
        if (ServerUtil.getProtocolVersion() < 755) {
            return;
        }
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        int slotId = ((Integer) player.containerMenu.slots.stream().filter(slot -> {
            return !slot.getItem().isEmpty();
        }).map(slot2 -> {
            return Integer.valueOf(slot2.index);
        }).findFirst().orElse(0)).intValue();
        Mc.INSTANCE.getInteractionManager().handleContainerInput(player.containerMenu.containerId, slotId, 0, ContainerInput.PICKUP_ALL, player);
        if (z && player.containerMenu.containerId != 0) {
            player.connection.send(new ServerboundContainerClosePacket(player.containerMenu.containerId));
        }
    }

    public void windowClick(ContainerInput slotActionType, int i, int i2, boolean z) {
        Mc mc;
        LocalPlayer player;
        if (i == -1 || (player = (mc = Mc.INSTANCE).getPlayer()) == null) {
            return;
        }
        int i3 = player.containerMenu.containerId;
        mc.getInteractionManager().handleContainerInput(i3, i, i2, slotActionType, player);
        System.out.println("windowClick successful: Slot ID = " + i + ", Button = " + i2 + ", Type = " + String.valueOf(slotActionType));
        if (z && i3 != 0) {
            player.connection.send(new ServerboundContainerClosePacket(i3));
        }
    }

    public void windowClick(ContainerInput slotActionType, int i, int i2) {
        Mc mc = Mc.INSTANCE;
        LocalPlayer player = mc.getPlayer();
        if (player == null) {
            return;
        }
        mc.getInteractionManager().handleContainerInput(player.containerMenu.containerId, i, i2, slotActionType, player);
        System.out.println("windowClick successful: Slot ID = " + i + ", Button = " + i2 + ", Type = " + String.valueOf(slotActionType));
    }
}
