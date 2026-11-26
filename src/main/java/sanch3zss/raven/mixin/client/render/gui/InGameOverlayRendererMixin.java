package sanch3zss.raven.mixin.client.render.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.render.RenderOverlayEvent;
import sanch3zss.raven.event.render.RenderOverlayType;

@Mixin({ScreenEffectRenderer.class})
public class InGameOverlayRendererMixin {
    @Inject(method = "submitWater", at = @At("HEAD"), cancellable = true)
    private static void renderWaterOverlay(Minecraft minecraft, PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, CallbackInfo callbackInfo) {
        RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayType.WATER_OVERLAY);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "submitFire", at = @At("HEAD"), cancellable = true)
    private static void renderFireOverlay(PoseStack matrixStack, SubmitNodeCollector submitNodeCollector, TextureAtlasSprite sprite, CallbackInfo callbackInfo) {
        RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayType.FIRE_OVERLAY);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
