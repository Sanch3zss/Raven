package sanch3zss.raven.feature.module.misc;

import sanch3zss.raven.client.chat.*;

public enum LockedHotbarSlot implements DisplayNamed {
    SLOT_1("1", 0), SLOT_2("2", 1), SLOT_3("3", 2), SLOT_4("4", 3), SLOT_5("5", 4),
    SLOT_6("6", 5), SLOT_7("7", 6), SLOT_8("8", 7), SLOT_9("9", 8);

    private final Translation displayName;
    private final int slot;

    LockedHotbarSlot(String name, int slot) {
        this.displayName = Translation.clearText(name);
        this.slot = slot;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public int slot() {
        return this.slot;
    }
}
