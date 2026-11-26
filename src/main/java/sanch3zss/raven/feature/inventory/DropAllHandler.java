package sanch3zss.raven.feature.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.config.repository.ClientHandler;
import sanch3zss.raven.client.input.KeyboardUtil;
import sanch3zss.raven.event.player.FocusedSlotEvent;

public class DropAllHandler implements ClientHandler {
    public DropAllHandler() {
        Raven.INSTANCE.eventDispatcher().register(FocusedSlotEvent.class, event -> {
            AbstractContainerMenu screenHandler;
            Slot slotFocusedSlot = event.focusedSlot();
            if (!isHotkeyPressed() || slotFocusedSlot == null || slotFocusedSlot.getItem() == null || (screenHandler = Mc.INSTANCE.getPlayer().containerMenu) == null) {
                return;
            }
            dropMatchingItems(screenHandler, slotFocusedSlot.index, slotFocusedSlot.getItem().getItem());
        });
    }

    private void dropMatchingItems(AbstractContainerMenu screenHandler, int focusedSlot, Item item) {
        if (screenHandler instanceof ChestMenu container) {
            int containerSize = container.getContainer().getContainerSize();
            int start = focusedSlot < container.getRowCount() * 9 ? 0 : containerSize;
            int end = start == 0 ? containerSize : screenHandler.slots.size();
            dropMatchingItems(screenHandler.slots, start, end, item, false);
        } else if (screenHandler instanceof InventoryMenu) {
            dropMatchingItems(screenHandler.slots, 0, screenHandler.slots.size(), item, true);
        }
    }

    private void dropMatchingItems(NonNullList<Slot> slots, int start, int end, Item item, boolean skipArmor) {
        for (int slotIndex = start; slotIndex < end; slotIndex++) {
            if (skipArmor && slotIndex >= 5 && slotIndex <= 8) {
                continue;
            }
            Slot slot = slots.get(slotIndex);
            if (!slot.getItem().isEmpty() && slot.getItem().getItem() == item) {
                PlayerActionUtil.INSTANCE.windowClick(ContainerInput.THROW, slotIndex, 1);
            }
        }
    }

    public boolean isHotkeyPressed() {
        return KeyboardUtil.isKeyPressed(341) && KeyboardUtil.isKeyPressed(340) && KeyboardUtil.isKeyPressed(81);
    }
}
