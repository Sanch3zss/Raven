package sanch3zss.raven.mixin.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.world.level.material.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.render.RenderOverlayEvent;
import sanch3zss.raven.event.render.RenderOverlayType;
import sanch3zss.raven.feature.module.render.removals.RemovalsModule;
import sanch3zss.raven.feature.module.render.removals.RemovedVisualType;

@Mixin(FogRenderer.class)
public class BackgroundRendererMixin {

    @Inject(method = "getFogType", at = @At("RETURN"), cancellable = true)
    private void raven$removeLavaFog(Camera camera, CallbackInfoReturnable<FogType> callbackInfo) {
        if (callbackInfo.getReturnValue() == FogType.LAVA
                && Raven.INSTANCE.moduleRepository().get(RemovalsModule.class).removes(RemovedVisualType.LAVA_OVERLAY)) {
            callbackInfo.setReturnValue(FogType.ATMOSPHERIC);
        }
    }

    @Inject(method = "computeFogColor", at = @At("HEAD"), cancellable = true)
    private void modifyFogColor(Camera camera, float tickDelta, ClientLevel world, int viewDistance, float skyDarkness, Vector4f fogColor, CallbackInfo callbackInfo) {
        if (camera.getFluidInCamera() == FogType.LAVA) {
            RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayType.LAVA_OVERLAY);
            Raven.INSTANCE.eventDispatcher().dispatch(event);
            if (event.isCancelled()) {
                fogColor.set(0.0f, 0.0f, 0.0f, 0.0f);
                callbackInfo.cancel();
            }
            return;
        }
    }
}
