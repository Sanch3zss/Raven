package sanch3zss.raven.feature.module.misc;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.KeyPressState;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.player.MovementInputHelper;
import sanch3zss.raven.feature.setting.ButtonAction;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.KeybindSetting;

@Aliases(aliases = {"ElytraSwap", "Elytra Swap", "Chestplate Swap"})
public class ElytraSwapModule extends Module {
    private static final int CHEST_HANDLER_SLOT = 6;
    private static final long SWAP_COOLDOWN_MS = 200L;
    private static final long STOP_BEFORE_SWAP_MS = 100L;
    private long lastSwapAt;
    private long pendingSince;
    private int pendingSlot = -1;
    private boolean pendingSwap;
    private boolean swapPerformed;
    private final KeybindSetting swapKey = new KeybindSetting(Translation.clearText("Кнопка свапа"));

    public ElytraSwapModule() {
        super(ModuleTab.MISC, "ElytraSwap");
        addSettings(this.swapKey);
        register(KeyInputEvent.class, event -> {
            if (event.action() == KeyPressState.PRESS && event.key() == this.swapKey.getKey()) {
                swap();
            }
        });
        register(MouseButtonEvent.class, event -> {
            if (event.action() == ButtonAction.PRESS && event.button() == this.swapKey.getKey()) {
                swap();
            }
        });
        register(PlayerTickEvent.class, event -> {
            if (event.isPre()) {
                tickPendingSwap();
            }
        });
    }

    private void swap() {
        if (!isState() || !Mc.INSTANCE.isWorldLoaded() || Mc.INSTANCE.getCurrentScreen() != null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (this.pendingSwap || now - this.lastSwapAt < SWAP_COOLDOWN_MS) {
            return;
        }
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (!player.containerMenu.getCarried().isEmpty()) {
            return;
        }
        boolean wearingElytra = player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        int inventorySlot = findTarget(player, wearingElytra);
        if (inventorySlot < 0) {
            player.sendOverlayMessage(Component.literal(wearingElytra
                    ? "Свап отменён: нет нагрудника"
                    : "Свап отменён: нет элитр"));
            return;
        }
        this.lastSwapAt = now;
        this.pendingSince = now;
        this.pendingSlot = inventorySlot;
        this.pendingSwap = true;
        this.swapPerformed = false;
        MovementInputHelper.suppressMovementKeys();
    }

    private void tickPendingSwap() {
        if (!this.pendingSwap) {
            return;
        }
        if (!isState() || !Mc.INSTANCE.isWorldLoaded() || Mc.INSTANCE.getCurrentScreen() != null) {
            finishPendingSwap();
            return;
        }
        MovementInputHelper.suppressMovementKeys();
        long elapsed = System.currentTimeMillis() - this.pendingSince;
        if (!this.swapPerformed && elapsed >= STOP_BEFORE_SWAP_MS) {
            LocalPlayer player = Mc.INSTANCE.getPlayer();
            int handlerSlot = AutoSwapModule.toHandlerSlot(this.pendingSlot);
            int syncId = player.containerMenu.containerId;
            Mc.INSTANCE.getInteractionManager().handleContainerInput(syncId, handlerSlot, 0, ContainerInput.PICKUP, player);
            Mc.INSTANCE.getInteractionManager().handleContainerInput(syncId, CHEST_HANDLER_SLOT, 0, ContainerInput.PICKUP, player);
            Mc.INSTANCE.getInteractionManager().handleContainerInput(syncId, handlerSlot, 0, ContainerInput.PICKUP, player);
            this.swapPerformed = true;
        }
        if (elapsed >= SWAP_COOLDOWN_MS) {
            finishPendingSwap();
        }
    }

    private void finishPendingSwap() {
        this.pendingSwap = false;
        this.swapPerformed = false;
        this.pendingSlot = -1;
        MovementInputHelper.refreshPressedKeys();
    }

    private static int findTarget(LocalPlayer player, boolean needChestplate) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!needChestplate && stack.is(Items.ELYTRA)) {
                return slot;
            }
            if (needChestplate && isChestplate(stack)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isChestplate(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.ELYTRA)) {
            return false;
        }
        Equippable equipment = stack.get(DataComponents.EQUIPPABLE);
        return equipment != null && equipment.slot() == EquipmentSlot.CHEST;
    }
}
