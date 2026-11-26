package sanch3zss.raven.mixin.client.entity.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.TickStage;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.util.math.rotation.*;

@Mixin(LocalPlayer.class)
public abstract class ClientPlayerEntityMixin extends AbstractClientPlayer {
    @Shadow
    public ClientInput input;

    public ClientPlayerEntityMixin(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    private void onInit(Minecraft client, ClientLevel world,
                        ClientPacketListener networkHandler, StatsCounter statHandler,
                        ClientRecipeBook recipeBook, Input lastPlayerInput,
                        boolean lastSneaking, net.minecraft.client.multiplayer.chat.ChatAbilities chatAbilities, CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new PlayerInitEvent());
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void preTick(CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new PlayerTickEvent(TickStage.PRE));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void postTick(CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new PlayerTickEvent(TickStage.POST));
    }

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void raven$beforeDropSelectedItem(boolean entireStack,
                                               CallbackInfoReturnable<Boolean> callbackInfo) {
        DropItemEvent event = new DropItemEvent(getInventory().getSelectedSlot(), entireStack);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.setReturnValue(false);
        }
    }

    @ModifyExpressionValue(method = "sendPosition",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"),
            require = 0)
    private float hookSilentRotationYaw(float vanillaYaw) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotationOriginal();
        return rotation == null ? vanillaYaw : rotation.getYaw();
    }

    @ModifyExpressionValue(method = "sendPosition",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"),
            require = 0)
    private float hookSilentRotationPitch(float vanillaPitch) {
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotationOriginal();
        return rotation == null ? vanillaPitch : rotation.getPitch();
    }
}
