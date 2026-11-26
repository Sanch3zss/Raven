package sanch3zss.raven.mixin.client;


import sanch3zss.raven.render.theme.ColorUtil;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import sanch3zss.raven.client.*;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.event.network.WorldLoadEvent;
import sanch3zss.raven.event.player.UseItemEvent;import sanch3zss.raven.feature.module.render.armtweaks.knife.KnifeRenderer;
import sanch3zss.raven.feature.module.player.FakePlayerModule;

@Mixin({Minecraft.class})
public class MinecraftClientMixin {

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void raven$attackFakePlayer(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (Raven.INSTANCE.moduleRepository().get(FakePlayerModule.class).attackIfLookedAt()) {
            callbackInfo.setReturnValue(true);
            return;
        }
        Minecraft client = (Minecraft) (Object) this;
        KnifeRenderer.INSTANCE.prepareAttack(client.player, client.hitResult);
    }

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Final
    private Window window;

    @Inject(method = {"tick"}, at = {@At("HEAD")})
    private void onTick(CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new ClientTickEvent());
    }

    @ModifyExpressionValue(method = {"startUseItem"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isHandsBusy()Z")})
    public boolean doItemUseHook(boolean z) {
        UseItemEvent event = new UseItemEvent();
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            return true;
        }
        return z;
    }

    @Inject(method = {"updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V"}, at = {@At("HEAD")})
    private void setWorld(ClientLevel clientWorld, boolean loading, CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new WorldLoadEvent(clientWorld));
    }

    @Inject(method = "renderFrame", at = @At("HEAD"))
    private void beginGradientFrame(boolean tick, CallbackInfo callbackInfo) {
        ColorUtil.beginGradientFrame(System.nanoTime());
    }

    @Inject(method = "renderFrame", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuSurface;blitFromTexture(Lcom/mojang/blaze3d/systems/CommandEncoder;Lcom/mojang/blaze3d/textures/GpuTextureView;)V"))
    private void runRavenUiRender(boolean tick, CallbackInfo callbackInfo) {
        Raven.INSTANCE.windowControllerAdapter().preBlitFramebufferToBackbuffer(this.window.handle());
    }

    @Inject(at = {@At("HEAD")}, method = {"close"})
    private void stop(CallbackInfo callbackInfo) {
        Raven.INSTANCE.discordManager().stopRPC();
    }

    @Inject(method = {"<init>"}, at = {@At("RETURN")})
    public void init(GameConfig runArgs, CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new ClientInitEvent());
    }
}
