package sanch3zss.raven.feature.inventory;

import net.minecraft.world.InteractionHand;
import sanch3zss.raven.client.*;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.player.*;
import sanch3zss.raven.util.math.rotation.RotationManager;

public class InventoryService {
    public final ItemInteractionHelper itemInteractor = new ItemInteractionHelper();

    public final SilentSlotManager hotbarSlotSwapper = new SilentSlotManager();

    public final InventoryItemFinder searcher = new InventoryItemFinder();

    public InventoryService() {
        Raven.INSTANCE.eventDispatcher().register(PlayerTickEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded() && event.isPre()) {
                this.hotbarSlotSwapper.update();
            }
        });
    }

    public void addTask(InventoryTask inventoryTask, Module module) {
        if (Mc.INSTANCE.getPlayer() == null) {
            return;
        }
        SlotSearchResult2 result = inventoryTask.result();
        if (result.found()) {
            SwapUtil.swapAndExecute(result.slotReference().increasedSlot(), () -> {
                PlayerActionUtil.INSTANCE.interactItem(InteractionHand.MAIN_HAND, RotationManager.INSTANCE.getCurrentRotation(), false);
            });
        }
    }

    public ItemInteractionHelper itemInteractor() {
        return this.itemInteractor;
    }

    public InventoryItemFinder searcher() {
        return this.searcher;
    }

    public SilentSlotManager hotbarSlotSwapper() {
        return this.hotbarSlotSwapper;
    }
}
