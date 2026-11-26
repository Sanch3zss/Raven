package sanch3zss.raven.feature.inventory;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.feature.inventory.*;

public class SilentSlotManager {
    public PendingSlotSwap pendingSwap;
    public final Mc mc = Mc.INSTANCE;
    public int tickCounter = 0;

    public int getServersideSlot() {
        if (this.pendingSwap != null && this.pendingSwap.pendingSlot != null) {
            return this.pendingSwap.pendingSlot.slot();
        }
        if (this.mc.getPlayer() != null) {
            return this.mc.getPlayer().getInventory().getSelectedSlot();
        }
        return 0;
    }

    public int getClientsideSlot() {
        if (this.pendingSwap != null) {
            return this.pendingSwap.clientsideSlot();
        }
        if (this.mc.getPlayer() != null) {
            return this.mc.getPlayer().getInventory().getSelectedSlot();
        }
        return 0;
    }

    public void update() {
        if (this.pendingSwap != null) {
            int i = this.tickCounter;
            this.tickCounter = i + 1;
            if (i >= this.pendingSwap.ticksUntilReset()) {
                this.pendingSwap = null;
                if (this.mc.isWorldLoaded() && this.mc.getInteractionManager() != null) {
                    this.mc.getInteractionManager().ensureHasSentCarriedItem();
                }
            }
        }
    }

    public void swapTo(Object obj, ScopedSlot scopedSlot, int i) {
        this.pendingSwap = new PendingSlotSwap(obj, scopedSlot, getClientsideSlot(), i);
        this.tickCounter = 0;
    }
}
