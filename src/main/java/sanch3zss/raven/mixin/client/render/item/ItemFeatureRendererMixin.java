package sanch3zss.raven.mixin.client.render.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;
import sanch3zss.raven.feature.module.render.armtweaks.GlassItemSubmit;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerRenderContext;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerItemSubmit;

@Mixin(ItemFeatureRenderer.class)
public abstract class ItemFeatureRendererMixin {
    @ModifyExpressionValue(method = "prepareMainSubmit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType raven$replaceFirstPersonItemRenderType(RenderType original,
                                                               @Local(argsOnly = true) ItemFeatureRenderer.Submit submit,
                                                               @Local BakedQuad.MaterialInfo material) {
        if (((LeaveTrackerItemSubmit) (Object) submit).raven$isLeaveTrackerItem()) {
            return RenderTypes.entityTranslucent(material.sprite().atlasLocation());
        }
        return GlassHandsRenderer.INSTANCE.replaceItemRenderType(original, submit.displayContext(),
                ((GlassItemSubmit) (Object) submit).raven$isGlassViewModel(), material.sprite().atlasLocation());
    }

    @WrapOperation(method = "prepareMainSubmit", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setColor(I)V"))
    private void raven$replaceFirstPersonItemColor(QuadInstance instance, int color, Operation<Void> operation,
                                                   @Local(argsOnly = true) ItemFeatureRenderer.Submit submit) {
        int renderedColor = GlassHandsRenderer.INSTANCE.replaceItemColor(color, submit.displayContext(),
                ((GlassItemSubmit) (Object) submit).raven$isGlassViewModel());
        if (((LeaveTrackerItemSubmit) (Object) submit).raven$isLeaveTrackerItem()) {
            renderedColor = LeaveTrackerRenderContext.INSTANCE.ghostColor(renderedColor);
        }
        operation.call(instance, renderedColor);
    }

    @WrapOperation(method = "prepareMainSubmit", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/QuadInstance;setOverlayCoords(I)V"))
    private void raven$replaceFirstPersonItemOverlay(QuadInstance instance, int overlay, Operation<Void> operation,
                                                     @Local(argsOnly = true) ItemFeatureRenderer.Submit submit) {
        operation.call(instance, GlassHandsRenderer.INSTANCE.replaceItemOverlay(overlay, submit.displayContext(),
                ((GlassItemSubmit) (Object) submit).raven$isGlassViewModel()));
    }

    @Inject(method = "prepareFoilSubmit", at = @At("HEAD"), cancellable = true)
    private void raven$skipLeaveTrackerItemFoil(ItemFeatureRenderer.Submit submit, CallbackInfo callbackInfo) {
        if (((LeaveTrackerItemSubmit) (Object) submit).raven$isLeaveTrackerItem()) {
            callbackInfo.cancel();
        }
    }
}
