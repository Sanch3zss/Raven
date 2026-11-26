package sanch3zss.raven.mixin.client.render;


import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.render.gl.MatrixCaptureHandler;
import sanch3zss.raven.render.gl.StencilBufferUtil;
import sanch3zss.raven.render.theme.ColorUtil;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Matrix4f;

import sanch3zss.raven.client.*;
import sanch3zss.raven.event.render.*;
import sanch3zss.raven.feature.module.render.*;
@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {

    @Shadow @Final private FogRenderer fogRenderer;
    @Shadow @Final private GuiRenderer guiRenderer;

    @Inject(method = "render", at = @At("HEAD"))
    private void beginRavenGradientFrame(DeltaTracker renderTickCounter, boolean renderLevel, CallbackInfo callbackInfo) {
        ColorUtil.beginGradientFrame(System.nanoTime());
    }

    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"), index = 0)
    private Matrix4f raven$captureWorldProjection(Matrix4f projectionMatrix) {
        MatrixCaptureHandler.captureWorldProjection(projectionMatrix);
        return projectionMatrix;
    }

    @Inject(method = "resize", at = @At("RETURN"))
    private void raven$resizeCustomRenderTargets(int width, int height, CallbackInfo callbackInfo) {
        StencilBufferUtil.initFramebuffer(width, height,
                FramebufferUtil.getColorAttachmentId(Mc.INSTANCE.getFramebuffer()));
        Raven.INSTANCE.windowControllerAdapter().handleResize(width, height);
    }

    @ModifyExpressionValue(method = {"extractOptions"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;")})
    private CameraType hookPerspectiveEvent(CameraType perspective) {
        PerspectiveEvent event2 = new PerspectiveEvent(perspective);
        Raven.INSTANCE.eventDispatcher().dispatch(event2);
        return event2.perspective();
    }

    @Inject(method = {"bobHurt"}, at = {@At("HEAD")}, cancellable = true)
    private void hurtCam(CameraRenderState cameraRenderState, PoseStack matrixStack, CallbackInfo callbackInfo) {
        RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayType.CAMERA_HURT);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void postRender(DeltaTracker renderTickCounter, boolean renderLevel, CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new Render2DEvent(new PoseStack(), Render2DStage.POST, renderTickCounter, null));
    }

}
