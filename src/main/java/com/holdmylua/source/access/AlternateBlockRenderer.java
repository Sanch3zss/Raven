package com.holdmylua.source.access;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;

public class AlternateBlockRenderer {
    private static final BlockModelRenderState RENDER_STATE = new BlockModelRenderState();

    public static void renderSingleBlockWithEmission(BlockState blockState, PoseStack poseStack, SubmitNodeCollector queue, int combinedLight, ClientLevel world, AbstractClientPlayer player) {
        if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
            return;
        }
        int light = LightCoordsUtil.lightCoordsWithEmission(combinedLight, blockState.getLightEmission());
        BlockModel model = Minecraft.getInstance().getModelManager().getBlockModelSet().get(blockState);
        RENDER_STATE.clear();
        model.update(RENDER_STATE, blockState, BlockDisplayContext.create(), 42L);
        GlassHandsRenderer.INSTANCE.renderHmiBlock(
                () -> RENDER_STATE.submit(poseStack, queue, light, OverlayTexture.NO_OVERLAY, 0));
    }
}
