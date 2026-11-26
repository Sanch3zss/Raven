package sanch3zss.raven.event.player;

import sanch3zss.raven.event.CancellableEvent;

public final class DropItemEvent extends CancellableEvent {
    private final int inventorySlot;
    private final boolean entireStack;

    public DropItemEvent(int inventorySlot, boolean entireStack) {
        this.inventorySlot = inventorySlot;
        this.entireStack = entireStack;
    }

    public int inventorySlot() {
        return this.inventorySlot;
    }

    public boolean entireStack() {
        return this.entireStack;
    }
}
