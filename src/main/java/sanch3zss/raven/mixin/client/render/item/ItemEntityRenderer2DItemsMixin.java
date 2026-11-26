package sanch3zss.raven.mixin.client.render.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.TwoDItemsModule;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRenderer2DItemsMixin {
    @WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V"))
    private void raven$faceDroppedItemsToCamera(PoseStack poseStack, Quaternionfc originalRotation,
                                                 Operation<Void> original, ItemEntityRenderState state,
                                                 PoseStack ignoredPoseStack, SubmitNodeCollector collector,
                                                 CameraRenderState camera) {
        TwoDItemsModule module = Raven.INSTANCE == null || Raven.INSTANCE.moduleRepository == null
                ? null : Raven.INSTANCE.moduleRepository.get(TwoDItemsModule.class);
        if (module == null || !module.isState() || state.item.isEmpty()) {
            original.call(poseStack, originalRotation);
            return;
        }
        boolean flat = state.item.getModelBoundingBox().getZsize() <= 0.0625;
        if (!flat && !module.affect3DModels.isValue()) {
            original.call(poseStack, originalRotation);
            return;
        }
        if (flat && !module.renderSides.isValue()) {
            for (int i = 0; i < state.item.activeLayerCount; i++) {
                ItemStackRenderState.LayerRenderState layer = state.item.layers[i];
                if (layer.prepareQuadList().stream().anyMatch(quad -> quad.direction() == Direction.SOUTH)) {
                    layer.prepareQuadList().removeIf(quad -> quad.direction() != Direction.SOUTH);
                }
            }
        }
        original.call(poseStack, camera.orientation);
    }
}
