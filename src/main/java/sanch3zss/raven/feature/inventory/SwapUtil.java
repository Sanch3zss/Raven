package sanch3zss.raven.feature.inventory;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.service.ActionSequenceController;

public final class SwapUtil {
    public static void swapAndExecute(int slot, Runnable action) {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null || !ActionSequenceController.isIdle()) {
            return;
        }

        int selectedSlot = player.getInventory().getSelectedSlot();
        ActionSequenceController.sequence()
                .addTickStep(0, () -> PlayerActionUtil.INSTANCE.clickSlot(
                        player.containerMenu.containerId,
                        slot,
                        selectedSlot,
                        ContainerInput.SWAP,
                        true))
                .addTickStep(1, action::run)
                .addTickStep(2, () -> {
                    PlayerActionUtil.INSTANCE.clickSlot(
                            player.containerMenu.containerId,
                            slot,
                            selectedSlot,
                            ContainerInput.SWAP,
                            true);
                    PlayerActionUtil.INSTANCE.updateSlots(true);
                });
    }

    public static void swapAction(Runnable action) {
        ActionSequenceController.submit(action);
    }

    private SwapUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
