package sanch3zss.raven.mixin.client.render.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.hitcolor.HitColorModule;

@Mixin(EquipmentLayerRenderer.class)
public class EquipmentLayerRendererMixin {
    private static final String RENDER_LAYERS = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V";

    @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType raven$hurtArmorType(Identifier texture, Operation<RenderType> original,
                                           @Local(argsOnly = true) Object state) {
        if (Raven.INSTANCE.moduleRepository().get(HitColorModule.class).tintsArmor(state)) {
            return RenderTypes.entityCutout(texture);
        }
        return original.call(texture);
    }

    @WrapOperation(method = RENDER_LAYERS, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            ordinal = 0))
    private void raven$armorHitColor(OrderedSubmitNodeCollector collector, Model<?> model, Object state, PoseStack poseStack,
                                     RenderType renderType, int light, int overlay, int color, TextureAtlasSprite sprite,
                                     int outline, ModelFeatureRenderer.CrumblingOverlay crumbling, Operation<Void> original) {
        if (Raven.INSTANCE.moduleRepository().get(HitColorModule.class).tintsArmor(state)) {
            overlay = LivingEntityRenderer.getOverlayCoords((LivingEntityRenderState) state, 0.0f);
        }
        original.call(collector, model, state, poseStack, renderType, light, overlay, color, sprite, outline, crumbling);
    }
}
