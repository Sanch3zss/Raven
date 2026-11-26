package sanch3zss.raven.feature.inventory;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.feature.inventory.*;

public class InventoryItemFinder {
    public Optional<SlotSearchResult2> findItem(Predicate<ItemStack> predicate, InventoryScope... arr) {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null) {
            return Optional.empty();
        }
        for (InventoryScope inventoryScope : normalizeScopes(arr)) {
            for (int iStart = inventoryScope.start(); iStart <= inventoryScope.end(); iStart++) {
                ItemStack stack = player.getInventory().getItem(iStart);
                if (predicate.test(stack)) {
                    return Optional.of(new SlotSearchResult2(new ScopedSlot(iStart, getScopeForSlot(iStart)), stack));
                }
            }
        }
        return Optional.empty();
    }

    public List<SlotSearchResult2> findAllItems(Predicate<ItemStack> predicate, InventoryScope... arr) {
        ArrayList arrayList = new ArrayList();
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null) {
            return arrayList;
        }
        for (InventoryScope inventoryScope : normalizeScopes(arr)) {
            for (int iStart = inventoryScope.start(); iStart <= inventoryScope.end(); iStart++) {
                ItemStack stack = player.getInventory().getItem(iStart);
                if (predicate.test(stack)) {
                    arrayList.add(new SlotSearchResult2(new ScopedSlot(iStart, getScopeForSlot(iStart)), stack));
                }
            }
        }
        return arrayList;
    }

    public static InventoryScope[] normalizeScopes(InventoryScope[] arr) {
        return arr.length == 0 ? new InventoryScope[]{InventoryScope.ALL} : arr;
    }

    public static InventoryScope getScopeForSlot(int i) {
        if (i >= InventoryScope.HOTBAR.start() && i <= InventoryScope.HOTBAR.end()) {
            return InventoryScope.HOTBAR;
        }
        if (i >= InventoryScope.INVENTORY.start() && i <= InventoryScope.INVENTORY.end()) {
            return InventoryScope.INVENTORY;
        }
        if (i < InventoryScope.ARMOR.start() || i > InventoryScope.ARMOR.end()) {
            return (i < InventoryScope.OFFHAND.start() || i > InventoryScope.OFFHAND.end()) ? InventoryScope.ALL : InventoryScope.OFFHAND;
        }
        return InventoryScope.ARMOR;
    }
}
