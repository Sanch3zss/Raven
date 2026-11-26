package sanch3zss.raven.feature.inventory;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.util.math.MathUtil;

public class InventoryUtil {
    public static final InventoryUtil INSTANCE = new InventoryUtil();
    public String getPotionDuration(MobEffectInstance statusEffectInstance) {
        int duration = statusEffectInstance.getDuration();
        return (statusEffectInstance.isInfiniteDuration() || duration < 0 || duration >= 1000000) ? "**:**" : MathUtil.tickToElapsedTime(duration);
    }

    public void windowClick(ContainerInput slotActionType, int i, int i2, boolean z) {
        if (i == -1) {
            return;
        }
        Mc mc = Mc.INSTANCE;
        LocalPlayer player = mc.getPlayer();
        mc.getInteractionManager().handleContainerInput(player.containerMenu.containerId, i, i2, slotActionType, player);
        if (z) {
            player.connection.send(new ServerboundContainerClosePacket(player.containerMenu.containerId));
        }
    }

    public void windowClick(ContainerInput slotActionType, int i, int i2) {
        if (i == -1) {
            return;
        }
        Mc mc = Mc.INSTANCE;
        LocalPlayer player = mc.getPlayer();
        mc.getInteractionManager().handleContainerInput(player.containerMenu.containerId, i, i2, slotActionType, player);
    }

    public int findItemSlot(Item item, boolean z, boolean z2) {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        int i = z ? 9 : 0;
        int i2 = z ? 36 : 8;
        int i3 = i;
        while (i3 <= i2) {
            if (player.getInventory().getItem(i3).getItem() == item) {
                if (z2) {
                    return (i3 >= 9 || i3 == -1) ? i3 : i3 + 36;
                }
                return i3;
            }
            i3++;
        }
        return -1;
    }

}
