package sanch3zss.raven.mixin.client.entity.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import sanch3zss.raven.client.Raven;

@Mixin({Inventory.class})
public class PlayerInventoryMixin {

    @Shadow
    public int selected;

    @ModifyExpressionValue(method = "removeFromSelected", at = {@At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Inventory;selected:I")})
    private int hookOverrideOriginalSlot(int i) {
        return ((Inventory) (Object) this).player == Minecraft.getInstance().player ? Raven.INSTANCE.inventoryService().hotbarSlotSwapper().getServersideSlot() : i;
    }
}
