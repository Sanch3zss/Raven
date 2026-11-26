package sanch3zss.raven.mixin.client.render.item;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;

@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin {
    @Shadow
    ItemDisplayContext displayContext;

    @WrapMethod(method = "submit")
    private void raven$submitGlassSpecialItems(PoseStack poseStack, SubmitNodeCollector collector,
                                                int lightCoords, int overlayCoords, int outlineColor,
                                                Operation<Void> original) {
        GlassHandsRenderer.INSTANCE.renderItemSubmission(this.displayContext,
                () -> original.call(poseStack, collector, lightCoords, overlayCoords, outlineColor));
    }
}
