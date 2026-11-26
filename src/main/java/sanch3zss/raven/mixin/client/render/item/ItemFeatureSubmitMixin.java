package sanch3zss.raven.mixin.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;
import sanch3zss.raven.feature.module.render.armtweaks.GlassItemSubmit;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerRenderContext;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerItemSubmit;

@Mixin(ItemFeatureRenderer.Submit.class)
public class ItemFeatureSubmitMixin implements LeaveTrackerItemSubmit, GlassItemSubmit {
    @Unique private boolean raven$leaveTrackerItem;
    @Unique private boolean raven$glassViewModel;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void raven$captureLeaveTrackerItem(PoseStack.Pose pose, ItemDisplayContext displayContext,
                                                 int lightCoords, int overlayCoords, int outlineColor,
                                                 int[] tintLayers, List<BakedQuad> quads,
                                                 ItemStackRenderState.FoilType foilType, CallbackInfo callbackInfo) {
        this.raven$leaveTrackerItem = LeaveTrackerRenderContext.INSTANCE.isActive();
        this.raven$glassViewModel = GlassHandsRenderer.INSTANCE.isViewModelItemActive();
    }

    @Override
    public boolean raven$isGlassViewModel() {
        return this.raven$glassViewModel;
    }

    @Inject(method = "hasTranslucency", at = @At("HEAD"), cancellable = true)
    private void raven$makeLeaveTrackerItemTranslucent(CallbackInfoReturnable<Boolean> callbackInfo) {
        if (this.raven$leaveTrackerItem) {
            callbackInfo.setReturnValue(true);
        }
    }

    @Override
    public boolean raven$isLeaveTrackerItem() {
        return this.raven$leaveTrackerItem;
    }
}
