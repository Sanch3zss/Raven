package sanch3zss.raven.mixin.client.render.gui;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.ui.hud.VanillaItemAlphaContext;

@Mixin(BlitRenderState.class)
public abstract class TexturedQuadGuiElementRenderStateMixin {
    @Inject(method = "buildVertices", at = @At("HEAD"), cancellable = true)
    private void raven$applyVanillaItemAlpha(VertexConsumer vertices, CallbackInfo callbackInfo) {
        BlitRenderState state =
                (BlitRenderState) (Object) this;
        float alpha = VanillaItemAlphaContext.alphaForPose(state.pose());
        if (alpha >= 0.999f) {
            return;
        }
        int channel = Math.round(Math.max(0.0f, alpha) * 255.0f);
        int color = channel << 24 | channel << 16 | channel << 8 | channel;
        vertices.addVertexWith2DPose(state.pose(), state.x0(), state.y0()).setUv(state.u0(), state.v0()).setColor(color);
        vertices.addVertexWith2DPose(state.pose(), state.x0(), state.y1()).setUv(state.u0(), state.v1()).setColor(color);
        vertices.addVertexWith2DPose(state.pose(), state.x1(), state.y1()).setUv(state.u1(), state.v1()).setColor(color);
        vertices.addVertexWith2DPose(state.pose(), state.x1(), state.y0()).setUv(state.u1(), state.v0()).setColor(color);
        callbackInfo.cancel();
    }
}
