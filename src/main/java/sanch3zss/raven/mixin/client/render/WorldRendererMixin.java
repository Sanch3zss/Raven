package sanch3zss.raven.mixin.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;
import sanch3zss.raven.feature.module.render.CustomTagsModule;
import sanch3zss.raven.feature.module.render.KillEffectsModule;
import sanch3zss.raven.feature.module.render.jumpcircle.JumpCircleModule;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;
import sanch3zss.raven.feature.module.render.armtweaks.knife.Cs2Legs;
import sanch3zss.raven.render.gl.MatrixCaptureHandler;
import sanch3zss.raven.render.effect.WorldEffectsRenderer;
import sanch3zss.raven.render.effect.SelectiveBloomRenderer;

@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class WorldRendererMixin {
    @Shadow @Final private LevelTargetBundle targets;
    @Unique private WorldRenderEvent raven$effectsEvent;

    @Inject(method = "render", at = @At("HEAD"))
    private void raven$prepareWorldEffects(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker,
                                           boolean renderOutline, CameraRenderState cameraState,
                                           Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog,
                                           Vector4f fogColor, boolean shouldRenderSky, CallbackInfo callbackInfo) {
        Matrix4f modelView = new Matrix4f(modelViewMatrix);
        PoseStack matrixStack = new PoseStack();
        matrixStack.mulPose(modelView);
        raven$effectsEvent = new WorldRenderEvent(matrixStack, modelView,
                MatrixCaptureHandler.worldProjection(cameraState.projectionMatrix),
                deltaTracker, cameraState.cullFrustum);
    }

    @Inject(method = "addAlwaysOnTopPass", at = @At("HEAD"))
    private void raven$addWorldEffectsPass(FrameGraphBuilder frame,
                                           FeatureRenderDispatcher.PreparedFrame featureFrame,
                                           GpuBufferSlice fog, CallbackInfo callbackInfo) {
        FramePass pass = frame.addPass("raven_world_effects");
        targets.main = pass.readsAndWrites(targets.main);
        WorldRenderEvent event = raven$effectsEvent;
        pass.executes(() -> {
            if (event == null) return;
            Raven.INSTANCE.moduleRepository().get(JumpCircleModule.class).renderShaderCircles(event);
            WorldEffectsRenderer.INSTANCE.render(event);
        });
    }

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void raven$submitLegs(PoseStack poseStack, LevelRenderState levelRenderState,
                                  SubmitNodeCollector output, CallbackInfo callbackInfo) {
        Cs2Legs.INSTANCE.submit(poseStack, output, levelRenderState.cameraRenderState.pos);
        Raven.INSTANCE.moduleRepository().get(KillEffectsModule.class).submit(poseStack, output, levelRenderState.cameraRenderState.pos);
        Raven.INSTANCE.moduleRepository().get(CustomTagsModule.class).submit(poseStack, output, levelRenderState.cameraRenderState.pos);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GraphicsResourceAllocator resourceAllocator, DeltaTracker deltaTracker, boolean renderOutline,
                          CameraRenderState cameraState, Matrix4fc modelViewMatrix, GpuBufferSlice terrainFog,
                          Vector4f fogColor, boolean shouldRenderSky, CallbackInfo callbackInfo) {
        raven$renderWorldOverlays(raven$createWorldEvent(deltaTracker, cameraState, modelViewMatrix));
        raven$effectsEvent = null;
    }

    @Unique
    private WorldRenderEvent raven$createWorldEvent(DeltaTracker deltaTracker, CameraRenderState cameraState,
                                                     Matrix4fc modelViewMatrix) {
        Matrix4f modelView = new Matrix4f(modelViewMatrix);
        PoseStack matrixStack = new PoseStack();
        matrixStack.mulPose(modelView);
        return new WorldRenderEvent(matrixStack, modelView,
                MatrixCaptureHandler.worldProjection(cameraState.projectionMatrix),
                deltaTracker, cameraState.cullFrustum);
    }

    @Unique
    private void raven$renderWorldOverlays(WorldRenderEvent event) {
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        SelectiveBloomRenderer.INSTANCE.render(event);
        ArmTweaksModule armTweaks = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (armTweaks.shouldRenderGlassHands()) {
            GlassHandsRenderer.INSTANCE.captureScene(Math.round(armTweaks.handBlur.currentValue()));
        }
    }
}
