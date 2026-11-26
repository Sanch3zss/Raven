package sanch3zss.raven.mixin.client.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import sanch3zss.raven.client.Raven;

@Mixin({Player.class})
public class PlayerEntityMixin {
    @ModifyExpressionValue(method = {"tick"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getMainHandItem()Lnet/minecraft/world/item/ItemStack;")})
    private ItemStack injectSilentHotbar(ItemStack itemStack) {
        Player playerEntity = (Player) (Object) this;
        return playerEntity instanceof LocalPlayer ? (ItemStack) playerEntity.getInventory().getNonEquipmentItems().get(Raven.INSTANCE.inventoryService().hotbarSlotSwapper().getClientsideSlot()) : itemStack;
    }
}
