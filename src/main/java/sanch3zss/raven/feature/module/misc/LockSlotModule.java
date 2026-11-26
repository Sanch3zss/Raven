package sanch3zss.raven.feature.module.misc;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.event.player.DropItemEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"LockSlot", "Lock Slot", "Slot Lock"})
public class LockSlotModule extends Module {
    private final BooleanSetting lockHotbar =
            new BooleanSetting(Translation.clearText("Блокировать хотбар")).setValue(true);
    private final MultiSelectSetting<LockedHotbarSlot> slots =
            new MultiSelectSetting<LockedHotbarSlot>(Translation.clearText("Слоты"))
                    .values(LockedHotbarSlot.class).select(LockedHotbarSlot.values())
                    .visible(this.lockHotbar::isValue);
    private final BooleanSetting onlyValuable =
            new BooleanSetting(Translation.clearText("Только ценные предметы"));

    public LockSlotModule() {
        super(ModuleTab.MISC, "LockSlot");
        addSettings(this.lockHotbar, this.slots, this.onlyValuable);
        register(DropItemEvent.class, this::onDrop);
    }

    private void onDrop(DropItemEvent event) {
        if (!isState() || !this.lockHotbar.isValue() || event.inventorySlot() < 0
                || event.inventorySlot() > 8 || !isLocked(event.inventorySlot())) {
            return;
        }
        ItemStack stack = Mc.INSTANCE.getPlayer().getInventory().getItem(event.inventorySlot());
        if (!this.onlyValuable.isValue() || isValuable(stack)) {
            event.cancel();
        }
    }

    private boolean isLocked(int slot) {
        return this.slots.selectedValues().stream().anyMatch(value -> value.slot() == slot);
    }

    private static boolean isValuable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.get(DataComponents.EQUIPPABLE) != null
                || stack.get(DataComponents.TOOL) != null || stack.isEnchanted()) {
            return true;
        }
        String name = stack.getHoverName().getString();
        if (name.contains("★") || stack.getHoverName().getStyle().isObfuscated()) {
            return true;
        }
        return stack.is(Items.TOTEM_OF_UNDYING) || stack.is(Items.ELYTRA)
                || stack.is(Items.SHIELD) || stack.is(Items.CROSSBOW) || stack.is(Items.BOW)
                || stack.is(Items.TRIDENT) || stack.is(Items.ENDER_PEARL)
                || stack.is(Items.CHORUS_FRUIT) || stack.is(Items.GOLDEN_APPLE)
                || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }
}
