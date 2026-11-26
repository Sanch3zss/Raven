package sanch3zss.raven.event.player;

import net.minecraft.world.inventory.Slot;
import sanch3zss.raven.event.Event;

public final class FocusedSlotEvent implements Event {
    public final Slot focusedSlot;
    public final int key;

    public FocusedSlotEvent(Slot slot) {
        this(slot, -1);
    }

    public FocusedSlotEvent(Slot slot, int key) {
        this.focusedSlot = slot;
        this.key = key;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "focusedSlot=" + this.focusedSlot + ", " + "key=" + this.key + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.focusedSlot, this.key);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FocusedSlotEvent)) return false;
        FocusedSlotEvent o = (FocusedSlotEvent) obj;
        return java.util.Objects.equals(this.focusedSlot, o.focusedSlot) && this.key == o.key;
    }
public Slot focusedSlot() {
        return this.focusedSlot;
    }

    public int key() {
        return this.key;
    }
}
