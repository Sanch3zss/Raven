package sanch3zss.raven.mixin.client.screen;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.player.StatusEffectOverlayEvent;
import sanch3zss.raven.event.render.*;
import sanch3zss.raven.feature.module.render.WidgetsModule;@Mixin({Hud.class})
public abstract class InGameHudMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @ModifyExpressionValue(method = {"tick()V"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getSelectedItem()Lnet/minecraft/world/item/ItemStack;")})
    private ItemStack injectSilent(ItemStack itemStack) {
        return this.minecraft.player != null ? (ItemStack) (Object) this.minecraft.player.getInventory().getNonEquipmentItems().get(Raven.INSTANCE.inventoryService().hotbarSlotSwapper().getClientsideSlot()) : itemStack;
    }

    @Inject(method = {"extractRenderState"}, at = {@At("HEAD")})
    private void preRender(GuiGraphicsExtractor drawContext, DeltaTracker renderTickCounter, CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new Render2DEvent(
                new PoseStack(), Render2DStage.PRE, renderTickCounter, drawContext));
    }

    @Inject(method = {"extractRenderState"}, at = {@At("TAIL")})
    private void postExtractRender(GuiGraphicsExtractor drawContext, DeltaTracker renderTickCounter, CallbackInfo callbackInfo) {
        Raven.INSTANCE.moduleRepository().get(WidgetsModule.class).renderHud(drawContext);
        Raven.INSTANCE.widgetStack().flushVanillaItems();
    }

    @Inject(method = {"extractCrosshair"}, at = {@At("HEAD")}, cancellable = true)
    private void crosshair(GuiGraphicsExtractor drawContext, DeltaTracker renderTickCounter, CallbackInfo callbackInfo) {
        CrosshairRenderEvent event3 = new CrosshairRenderEvent();
        Raven.INSTANCE.eventDispatcher().dispatch(event3);
        if (event3.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @ModifyExpressionValue(method = {"extractCrosshair"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;")})
    private CameraType hookPerspectiveEventOnCrosshair(CameraType perspective) {
        PerspectiveEvent event4 = new PerspectiveEvent(perspective);
        Raven.INSTANCE.eventDispatcher().dispatch(event4);
        return event4.perspective();
    }

    @ModifyExpressionValue(method = {"extractCameraOverlays"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;")})
    private CameraType hookPerspectiveEventOnMiscOverlays(CameraType perspective) {
        PerspectiveEvent event4 = new PerspectiveEvent(perspective);
        Raven.INSTANCE.eventDispatcher().dispatch(event4);
        return event4.perspective();
    }

    @Inject(method = {"extractEffects"}, at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(GuiGraphicsExtractor drawContext, DeltaTracker renderTickCounter, CallbackInfo callbackInfo) {
        StatusEffectOverlayEvent event = new StatusEffectOverlayEvent();
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "extractVignette", at = @At("HEAD"), cancellable = true)
    private void renderVignette(GuiGraphicsExtractor drawContext, Entity camera, CallbackInfo callbackInfo) {
        RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayType.VIGNETTE);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"displayScoreboardSidebar"}, at = @At("HEAD"), cancellable = true)
    private void renderScoreboard(GuiGraphicsExtractor drawContext, Objective scoreboardObjective, CallbackInfo callbackInfo) {
        RenderOverlayEvent event2 = new RenderOverlayEvent(RenderOverlayType.SCOREBOARD);
        Raven.INSTANCE.eventDispatcher().dispatch(event2);
        if (event2.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
