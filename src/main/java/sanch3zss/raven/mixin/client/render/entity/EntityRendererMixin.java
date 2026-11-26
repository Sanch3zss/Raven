package sanch3zss.raven.mixin.client.render.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.feature.module.render.KillEffectRenderState;
import sanch3zss.raven.feature.module.render.KillEffectsModule;
import sanch3zss.raven.feature.module.render.removals.RemovalsModule;
import sanch3zss.raven.feature.module.render.TwoDItemsModule;
import sanch3zss.raven.feature.module.render.removals.RemovedVisualType;
import sanch3zss.raven.feature.module.render.party.PartyRenderState;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerRenderState;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V", at = @At("TAIL"))
    private void raven$submitLeaveTimer(EntityRenderState state, PoseStack poseStack,
                                        SubmitNodeCollector collector, CameraRenderState camera,
                                        int offset, CallbackInfo callbackInfo) {
        LeaveTrackerRenderState ghost = (LeaveTrackerRenderState) state;
        if (!ghost.raven$isLeaveTrackerGhost() || state.nameTagAttachment == null) {
            return;
        }
        long seconds = Math.max(0L, (System.nanoTime() - ghost.raven$leftAtNanos()) / 1_000_000_000L);
        String timer = String.format("Не в сети: %02d:%02d", seconds / 60L, seconds % 60L);
        poseStack.pushPose();
        poseStack.translate(0.0, state.scoreText != null ? 0.52 : 0.26, 0.0);
        collector.submitNameTag(poseStack, state.nameTagAttachment, offset,
                Component.literal(timer).withStyle(ChatFormatting.GRAY), !state.isDiscrete,
                state.lightCoords, camera);
        poseStack.popPose();
    }

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void raven$friendAlias(Entity entity, CallbackInfoReturnable<Component> callback) {
        callback.setReturnValue(FriendManager.applyAliases(callback.getReturnValue()));
    }

    @Inject(method = "getShadowRadius", at = @At("HEAD"), cancellable = true)
    private void raven$removeTwoDItemShadow(EntityRenderState state, CallbackInfoReturnable<Float> callback) {
        if (state instanceof ItemEntityRenderState && Raven.INSTANCE != null && Raven.INSTANCE.moduleRepository != null) {
            TwoDItemsModule module = Raven.INSTANCE.moduleRepository.get(TwoDItemsModule.class);
            if (module.isState() && !module.castShadows.isValue()) {
                callback.setReturnValue(0f);
            }
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void raven$removeGlowing(Entity entity, EntityRenderState state, float partialTicks, CallbackInfo callbackInfo) {
        ((PartyRenderState) state).raven$partyEntityId(entity.getUUID());
        ((KillEffectRenderState) state).raven$killEffectHidden(
                Raven.INSTANCE.moduleRepository().get(KillEffectsModule.class).hides(entity));
        if (Raven.INSTANCE.moduleRepository().get(RemovalsModule.class).removes(RemovedVisualType.GLOWING)) {
            state.outlineColor = 0;
        }
    }
}
