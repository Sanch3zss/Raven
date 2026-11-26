package sanch3zss.raven.feature.inventory;

public class PendingSlotSwap {
    public Object owner;
    public ScopedSlot pendingSlot;
    public int clientsideSlot;
    public int ticksUntilReset;

    public PendingSlotSwap() {
    }

    public PendingSlotSwap(Object obj, ScopedSlot scopedSlot, int i, int i2) {
        this.owner = obj;
        this.pendingSlot = scopedSlot;
        this.clientsideSlot = i;
        this.ticksUntilReset = i2;
    }

    public ScopedSlot pendingSlot() {
        return this.pendingSlot;
    }

    public int clientsideSlot() {
        return this.clientsideSlot;
    }

    public int ticksUntilReset() {
        return this.ticksUntilReset;
    }
}
