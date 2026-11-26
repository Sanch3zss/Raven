package sanch3zss.raven.mixin.client.render.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import sanch3zss.raven.util.math.rotation.*;

@Mixin({Item.class})
public abstract class ItemMixin {
    @ModifyExpressionValue(method = {"getPlayerPOVHitResult"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;")})
    private static Vec3 hookFixRotation(Vec3 vec3d, Level world, Player playerEntity, ClipContext.Fluid fluidHandling) {
        Rotation currentRotation;
        return (playerEntity != Minecraft.getInstance().player || (currentRotation = RotationManager.INSTANCE.getCurrentRotation()) == null) ? vec3d : currentRotation.getDirectionVector();
    }
}
