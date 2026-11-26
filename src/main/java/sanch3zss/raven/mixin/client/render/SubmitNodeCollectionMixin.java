package sanch3zss.raven.mixin.client.render;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.SimpleFeatureRenderPhase;
import net.minecraft.client.renderer.rendertype.RenderType;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.CustomTagsModule;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerRenderContext;
import sanch3zss.raven.feature.module.render.chinahat.ChinaHatRenderLayer;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin {
    @Shadow @Final public SimpleFeatureRenderPhase solid;

    @Inject(method = "submitCustomGeometry", at = @At("HEAD"), cancellable = true)
    private void raven$submitChinaHatBeforeTranslucent(PoseStack poseStack, RenderType renderType,
                                                        SubmitNodeCollector.CustomGeometryRenderer renderer,
                                                        CallbackInfo callbackInfo) {
        if (ChinaHatRenderLayer.isHatRenderType(renderType)) {
            this.solid.submit(new CustomFeatureRenderer.Submit(poseStack.last().copy(), renderType, renderer));
            callbackInfo.cancel();
        }
    }

    @ModifyVariable(method = "submitModel", at = @At("HEAD"), argsOnly = true, index = 4)
    private RenderType replaceHandRenderType(RenderType original) {
        return LeaveTrackerRenderContext.INSTANCE.renderType(
                GlassHandsRenderer.INSTANCE.replaceRenderType(original));
    }

    @ModifyVariable(method = "submitModel", at = @At("HEAD"), argsOnly = true, index = 7)
    private int replaceHandColor(int original) {
        return LeaveTrackerRenderContext.INSTANCE.color(
                GlassHandsRenderer.INSTANCE.replaceColor(original));
    }

    @ModifyVariable(method = "submitModel", at = @At("HEAD"), argsOnly = true, index = 6)
    private int replaceHandOverlay(int original) {
        return GlassHandsRenderer.INSTANCE.replaceOverlay(original);
    }

    @ModifyExpressionValue(method = "submitNameTag", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/ARGB;color(FI)I"))
    private int raven$hideNameTagBackground(int original) {
        return Raven.INSTANCE.moduleRepository().get(CustomTagsModule.class).hidesBackground() ? 0 : original;
    }

    @ModifyVariable(method = "submitBlockModel", at = @At("HEAD"), argsOnly = true, index = 2)
    private RenderType replaceHmiBlockRenderType(RenderType original) {
        return GlassHandsRenderer.INSTANCE.replaceHmiBlockRenderType(original);
    }

    @ModifyVariable(method = "submitBlockModel", at = @At("HEAD"), argsOnly = true, index = 6)
    private int replaceHmiBlockOverlay(int original) {
        return GlassHandsRenderer.INSTANCE.replaceHmiBlockOverlay(original);
    }
}
