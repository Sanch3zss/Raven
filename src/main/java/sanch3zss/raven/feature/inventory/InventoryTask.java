package sanch3zss.raven.feature.inventory;

import java.util.function.Predicate;

import sanch3zss.raven.feature.inventory.SlotSearchResult2;
import sanch3zss.raven.util.math.rotation.*;

public class InventoryTask {
    private final Predicate predicate;
    private final SlotSearchResult2 result;
    private final boolean z;
    private final boolean needStop;
    private final boolean z3;
    private Rotation rotation = null;

    private InventoryTask(Predicate predicate, SlotSearchResult2 result, boolean z, boolean needStop, boolean z3) {
        this.predicate = predicate;
        this.result = result;
        this.z = z;
        this.needStop = needStop;
        this.z3 = z3;
    }

    public static InventoryTask create(Predicate predicate, SlotSearchResult2 slotResult, boolean z, boolean z2, boolean z3) {
        return new InventoryTask(predicate, slotResult, z, z2, z3);
    }

    public boolean needStop() {
        return this.needStop;
    }

    public SlotSearchResult2 result() {
        return this.result;
    }

    public Rotation rotation() {
        return this.rotation != null ? this.rotation : sanch3zss.raven.util.math.rotation.RotationManager.INSTANCE.getCurrentRotation();
    }

    public InventoryTask withRotation(Rotation rotation) {
        this.rotation = rotation;
        return this;
    }
}
