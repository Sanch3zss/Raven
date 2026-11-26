package com.holdmylua.source.mixin.render;

import sanch3zss.raven.integration.hmi.HmiModeBridge;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;
import com.holdmylua.source.global.GlobalsStorage;
import com.holdmylua.source.global.item_model.ItemModelContext;
import com.holdmylua.source.global.item_model.ItemModelStorage;
import com.holdmylua.source.lua_runtime.ModelScriptCache;
import com.holdmylua.source.lua_runtime.ScriptHolder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.QuadInstance;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ItemFeatureRenderer.class})
public abstract class ItemRendererMixin
extends RenderTypeFeatureRenderer<ItemFeatureRenderer.Submit> {
    @Shadow
    @Final
    private QuadInstance quadInstance;

    @Unique
    private static int hmi$getLayerColorSafe(int[] layers, int layer) {
        if (layers != null && layer >= 0 && layer < layers.length) {
            return layers[layer];
        }
        return -1;
    }

    @Inject(method={"prepareMainSubmit"}, at={@At(value="HEAD")}, cancellable=true)
    private void onPrepareMainSubmit(ItemFeatureRenderer.Submit submit, CallbackInfo ci) {
        if (!HmiModeBridge.isActive()) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        ItemModelContext context = ItemModelStorage.get(submit.displayContext());
        boolean hmiFirstPersonItem = (submit.displayContext() == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || submit.displayContext() == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                && Minecraft.getInstance().getEntityRenderDispatcher().options.getCameraType().isFirstPerson()
                && player == Minecraft.getInstance().player && !context.item.isEmpty();
        if (hmiFirstPersonItem) {
            ItemStack renderedItem = context.item;
            ScriptHolder.itemModelCache.executeModel(context, renderedItem, (AbstractClientPlayer)player, GlobalsStorage.modelPartAnimator);
            for (ModelScriptCache cache : ScriptHolder.itemModelAddonsCache) {
                cache.executeModel(context, renderedItem, (AbstractClientPlayer)player, GlobalsStorage.modelPartAnimator);
            }
        }
        this.quadInstance.setLightCoords(submit.lightCoords());
        this.quadInstance.setOverlayCoords(GlassHandsRenderer.INSTANCE.replaceHmiItemOverlay(
                submit.overlayCoords(), submit.displayContext()));
        int index = 0;
        PoseStack poseStack = new PoseStack();
        for (BakedQuad quad : submit.quads()) {
            BakedQuad.MaterialInfo material = quad.materialInfo();
            RenderType renderType = hmiFirstPersonItem
                    ? GlassHandsRenderer.INSTANCE.replaceHmiItemRenderType(
                            material.itemRenderType(), submit.displayContext(), material.sprite().atlasLocation())
                    : material.itemRenderType();
            int color = hmi$getLayerColorSafe(submit.tintLayers(), material.tintIndex());
            this.quadInstance.setColor(hmiFirstPersonItem
                    ? GlassHandsRenderer.INSTANCE.replaceHmiItemColor(color, submit.displayContext())
                    : color);

            poseStack.pushPose();
            poseStack.last().pose().set(submit.pose().pose());
            poseStack.last().normal().set(submit.pose().normal());
            GlobalsStorage.modelPartAnimator.applyPoses(index, poseStack);
            this.getVertexBuilder(renderType).putBakedQuad(poseStack.last(), quad, this.quadInstance);
            poseStack.popPose();
            ++index;
        }
        GlobalsStorage.modelPartAnimator.clear();
        ci.cancel();
    }
}
