package sanch3zss.raven.mixin.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sanch3zss.raven.feature.module.render.CustomSkyRenderer;

@Mixin(SkyRenderer.class)
public class SkyRendererMixin {
    @Shadow @Final private RenderTarget renderTarget;

    @Inject(method = "renderSkyDisc", at = @At("HEAD"), cancellable = true)
    private void raven$customSky(int skyColor, CallbackInfo callbackInfo) {
        if (CustomSkyRenderer.INSTANCE.render(this.renderTarget, skyColor)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderDarkDisc", at = @At("HEAD"), cancellable = true)
    private void raven$hideDarkDisc(CallbackInfo callbackInfo) {
        if (CustomSkyRenderer.INSTANCE.active()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"))
    private void raven$captureCelestial(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle,
                                        MoonPhase moonPhase, float rainBrightness, float starBrightness,
                                        CallbackInfo callbackInfo) {
        CustomSkyRenderer.INSTANCE.starBrightness(starBrightness);
    }

    @Inject(method = "renderStars", at = @At("HEAD"), cancellable = true)
    private void raven$hideStars(float starBrightness, PoseStack poseStack, CallbackInfo callbackInfo) {
        if (CustomSkyRenderer.INSTANCE.active()) {
            callbackInfo.cancel();
        }
    }
}
