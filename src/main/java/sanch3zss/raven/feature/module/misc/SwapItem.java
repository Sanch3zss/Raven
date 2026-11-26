package sanch3zss.raven.feature.module.misc;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import sanch3zss.raven.client.chat.*;

public enum SwapItem implements DisplayNamed {
    SPHERE("Сфера", Items.PLAYER_HEAD),
    TOTEM("Тотем", Items.TOTEM_OF_UNDYING);

    private final Translation displayName;
    private final Item item;

    SwapItem(String name, Item item) {
        this.displayName = Translation.clearText(name);
        this.item = item;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Item item() {
        return this.item;
    }
}
