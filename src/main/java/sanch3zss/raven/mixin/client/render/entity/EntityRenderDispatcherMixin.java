package sanch3zss.raven.mixin.client.render.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import sanch3zss.raven.feature.module.render.armtweaks.knife.Cs2Body;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.KillEffectRenderState;
import sanch3zss.raven.feature.module.render.KillEffectsModule;
import sanch3zss.raven.feature.module.render.removals.RemovalsModule;
import sanch3zss.raven.feature.module.render.removals.RemovedVisualType;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerRenderContext;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerRenderState;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @WrapMethod(method = "submit")
    private void raven$submitLeaveTrackerGhost(EntityRenderState state, CameraRenderState camera,
                                                double x, double y, double z, PoseStack poseStack,
                                                SubmitNodeCollector collector, Operation<Void> original) {
        if (((KillEffectRenderState) state).raven$killEffectHidden()) {
            return;
        }
        if (((LeaveTrackerRenderState) state).raven$isLeaveTrackerGhost()) {
            LeaveTrackerRenderContext.INSTANCE.submit(
                    () -> original.call(state, camera, x, y, z, poseStack, collector));
        } else {
            original.call(state, camera, x, y, z, poseStack, collector);
        }
    }

    @WrapOperation(method = "submit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void raven$submitCs2Body(EntityRenderer<?, ?> renderer, EntityRenderState state, PoseStack poseStack,
                                     SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original) {
        if (state instanceof AvatarRenderState avatar && renderer instanceof AvatarRenderer<?> avatarRenderer
                && Cs2Body.INSTANCE.submit(avatar, poseStack, collector, avatarRenderer.getRenderOffset(avatar), !camera.initialized)) {
            return;
        }
        original.call(renderer, state, poseStack, collector, camera);
    }

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void removeEntities(Entity entity, Frustum culler, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (Raven.INSTANCE.moduleRepository().get(KillEffectsModule.class).hides(entity)) {
            callbackInfo.setReturnValue(false);
            return;
        }
        RemovalsModule module = Raven.INSTANCE.moduleRepository().get(RemovalsModule.class);
        if (module.removes(RemovedVisualType.HOLOGRAMS)
                && (entity instanceof ArmorStand || entity instanceof Display.TextDisplay)) {
            callbackInfo.setReturnValue(false);
            return;
        }
        if (module.removes(RemovedVisualType.FISHING_BOBBER)
                && entity instanceof FishingHook fishingHook
                && fishingHook.getPlayerOwner() == Minecraft.getInstance().player) {
            callbackInfo.setReturnValue(false);
        }
    }
}
