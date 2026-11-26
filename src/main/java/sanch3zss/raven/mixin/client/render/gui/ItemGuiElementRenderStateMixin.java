package sanch3zss.raven.mixin.client.render.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.ui.hud.VanillaItemAlphaContext;
import sanch3zss.raven.ui.hud.AlphaItemRenderState;

@Mixin(GuiItemRenderState.class)
public abstract class ItemGuiElementRenderStateMixin implements AlphaItemRenderState {
    @Unique
    private float raven$alpha = 1.0f;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void raven$captureAlpha(Matrix3x2f pose, TrackingItemStackRenderState state,
                                    int x, int y, ScreenRectangle scissor,
                                    CallbackInfo callbackInfo) {
        this.raven$alpha = VanillaItemAlphaContext.currentAlpha();
        VanillaItemAlphaContext.registerPose(pose, this.raven$alpha);
    }

    @Override
    public float raven$getAlpha() {
        return this.raven$alpha;
    }

    @Override
    public void raven$setAlpha(float alpha) {
        this.raven$alpha = Math.max(0.0f, Math.min(1.0f, alpha));
    }
}
