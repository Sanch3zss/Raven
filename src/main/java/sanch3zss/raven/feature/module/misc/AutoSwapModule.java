package sanch3zss.raven.feature.module.misc;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
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
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"AutoSwap", "Item Swap", "Offhand Swap"})
public class AutoSwapModule extends Module {
    private static final long SWAP_COOLDOWN_MS = 200L;
    private static final long STOP_BEFORE_SWAP_MS = 100L;
    private long lastSwapAt;
    private long pendingSince;
    private int pendingSlot = -1;
    private boolean pendingSwap;
    private boolean swapPerformed;
    private final ModeSetting<SwapItem> swapFrom =
            new ModeSetting<SwapItem>(Translation.clearText("Менять с")).values(SwapItem.class)
                    .select(SwapItem.SPHERE);
    private final ModeSetting<SwapItem> swapTo =
            new ModeSetting<SwapItem>(Translation.clearText("Менять на")).values(SwapItem.class)
                    .select(SwapItem.TOTEM);
    private final KeybindSetting swapKey = new KeybindSetting(Translation.clearText("Кнопка свапа"));

    public AutoSwapModule() {
        super(ModuleTab.MISC, "AutoSwap");
        addSettings(this.swapFrom, this.swapTo, this.swapKey);
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
        if (now - this.lastSwapAt < SWAP_COOLDOWN_MS) {
            return;
        }
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        SwapItem target = player.getOffhandItem().is(this.swapFrom.currentValue().item())
                ? this.swapTo.currentValue() : this.swapFrom.currentValue();
        int inventorySlot = findInventorySlot(player, target);
        if (inventorySlot < 0) {
            player.sendOverlayMessage(Component.literal("Предмет для свапа не найден"));
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
            Mc.INSTANCE.getInteractionManager().handleContainerInput(player.containerMenu.containerId,
                    toHandlerSlot(this.pendingSlot), 40, ContainerInput.SWAP, player);
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

    private static int findInventorySlot(LocalPlayer player, SwapItem target) {
        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).is(target.item())) {
                return slot;
            }
        }
        return -1;
    }

    static int toHandlerSlot(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }
}
