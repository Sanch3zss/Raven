package sanch3zss.raven.feature.module.render.chinahat;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.chinahat.ChinaHatModule;
import sanch3zss.raven.feature.module.render.chinahat.ChinaHatRenderState;

public final class ChinaHatRenderLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final int SEGMENTS = 180;
    private static final float[] SIN = new float[SEGMENTS + 1];
    private static final float[] COS = new float[SEGMENTS + 1];
    private static final RenderType CONE = RenderType.create("raven_china_hat", RenderSetup.builder(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("raven", "pipeline/china_hat"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
                    .withColorTargetState(ColorTargetState.DEFAULT)
                    .withCull(false)
                    .build()).createRenderSetup());
    public static boolean isHatRenderType(RenderType renderType) {
        return renderType == CONE;
    }

    static {
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = Math.PI * 2.0 * i / SEGMENTS;
            SIN[i] = (float) Math.sin(angle);
            COS[i] = (float) Math.cos(angle);
        }
    }

    public ChinaHatRenderLayer(RenderLayerParent<AvatarRenderState, PlayerModel> renderer) {
        super(renderer);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int lightCoords,
                       AvatarRenderState state, float yRot, float xRot) {
        Raven raven = Raven.INSTANCE;
        ChinaHatRenderState hatState = (ChinaHatRenderState) state;
        if (raven == null || !hatState.raven$isLocalPlayer()
                || Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return;
        }
        ChinaHatModule module = raven.moduleRepository().get(ChinaHatModule.class);
        if (!module.isState()) {
            return;
        }

        float radius = hatState.raven$hatRadius();
        float yOffset = -0.44f;
        if (hatState.raven$hasHeadArmor()) {
            yOffset -= 0.04f;
        }
        poseStack.pushPose();
        Model model = getParentModel();
        model.root().translateAndRotate(poseStack);
        ((HeadedModel) model).translateToHead(poseStack);
        poseStack.translate(0.0f, yOffset, 0.0f);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0f));

        collector.submitCustomGeometry(poseStack, CONE,
                (pose, buffer) -> emitCone(pose, buffer, radius, module));
        poseStack.popPose();
    }

    public static void submitOnHead(PoseStack poseStack, SubmitNodeCollector collector, AvatarRenderState state,
                                    float radius) {
        Raven raven = Raven.INSTANCE;
        ChinaHatRenderState hatState = (ChinaHatRenderState) state;
        if (raven == null || !hatState.raven$isLocalPlayer()) return;
        ChinaHatModule module = raven.moduleRepository().get(ChinaHatModule.class);
        if (!module.isState()) return;
        collector.submitCustomGeometry(poseStack, CONE, (pose, buffer) -> emitCone(pose, buffer, radius, module));
    }

    private static void emitCone(PoseStack.Pose pose, VertexConsumer buffer, float radius,
                                 ChinaHatModule module) {
        for (int i = 0; i < SEGMENTS; i++) {
            int ring = module.color.getAnimatedGradientColor(i / (float) SEGMENTS) | 0xFF000000;
            int nextRing = module.color.getAnimatedGradientColor((i + 1) / (float) SEGMENTS) | 0xFF000000;
            int apex = module.color.getAnimatedGradientColor((i + 0.5f) / SEGMENTS) | 0xFF000000;
            buffer.addVertex(pose, SIN[i] * radius, 0.0f, COS[i] * radius)
                    .setColor(ring);
            buffer.addVertex(pose, 0.0f, radius * 0.5f, 0.0f).setColor(apex);
            buffer.addVertex(pose, SIN[i + 1] * radius, 0.0f, COS[i + 1] * radius)
                    .setColor(nextRing);
        }
    }
}
