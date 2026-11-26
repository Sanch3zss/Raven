package sanch3zss.raven.mixin.client.render.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.resources.Identifier;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.render.*;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;
import sanch3zss.raven.feature.module.render.armtweaks.knife.KnifeRenderer;

@Mixin({ItemInHandRenderer.class})
public class HeldItemRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private ItemModelResolver itemModelResolver;

    @Shadow
    private ItemStack offHandItem;

    @Shadow
    private float offHandHeight;

    @Shadow
    private float oOffHandHeight;

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void raven$renderKnife(AbstractClientPlayer player, float frameInterp, float xRot,
                                       InteractionHand hand, float attack, ItemStack stack,
                                       float inverseArmHeight, PoseStack poseStack,
                                       SubmitNodeCollector collector, int light,
                                       CallbackInfo callbackInfo) {
        if (hand == InteractionHand.OFF_HAND
                && KnifeRenderer.INSTANCE.renderOffhand(player, stack, frameInterp, inverseArmHeight, collector, light)) {
            callbackInfo.cancel();
            return;
        }
        float offhandLowered = itemModelResolver.swapAnimationScale(offHandItem)
                * (1.0f - Mth.lerp(frameInterp, oOffHandHeight, offHandHeight));
        if (KnifeRenderer.INSTANCE.render(player, hand, stack, offHandItem, offhandLowered,
                poseStack, collector, light)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void raven$keepOffhandRaisedOnUse(InteractionHand hand, CallbackInfo callbackInfo) {
        if (KnifeRenderer.INSTANCE.suppressesUseReequip(hand)) callbackInfo.cancel();
    }

    @WrapOperation(method = {"submitArmWithItem"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"))
    private void armTweaksEquipTransform(ItemInHandRenderer renderer, PoseStack matrixStack, HumanoidArm arm,
                                         float equipProgress, Operation<Void> original,
                                         @Local(argsOnly = true) ItemStack stack) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        float progress = module.shouldOverrideSwing(stack) ? 0.0f : equipProgress;
        original.call(renderer, matrixStack, arm, progress);
    }

    @Inject(method = {"submitArmWithItem"}, at = {@At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER)})
    private void renderFirstPersonItemHook(AbstractClientPlayer abstractClientPlayerEntity, float f, float f2, InteractionHand hand, float f3, ItemStack itemStack, float f4, PoseStack matrixStack, SubmitNodeCollector commandQueue, int i, CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new HeldItemRenderEvent(hand, matrixStack, itemStack));
    }

    @WrapOperation(method = {"submitArmWithItem"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V")})
    private void handAnimationHook(ItemInHandRenderer heldItemRenderer, float f, PoseStack matrixStack, int i, HumanoidArm arm, Operation<Void> operation, @Local(ordinal = 0, argsOnly = true) InteractionHand hand, @Local(argsOnly = true) ItemStack itemStack) {
        ArmRenderEvent event = new ArmRenderEvent(hand, arm, matrixStack, f, itemStack);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (!event.isCancelled()) {
            operation.call(new Object[]{heldItemRenderer, Float.valueOf(f), matrixStack, Integer.valueOf(i), arm});
        }
    }

    @WrapOperation(method = {"renderPlayerArm", "renderMapHand"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderRightHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"))
    private void renderGlassRightHand(AvatarRenderer<?> renderer, PoseStack matrices, SubmitNodeCollector collector,
                                      int light, Identifier texture, boolean sleeve, Operation<Void> original) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        module.renderGlassHand(HumanoidArm.RIGHT, texture,
                () -> original.call(renderer, matrices, collector, light, texture, sleeve));
    }

    @WrapOperation(method = {"renderPlayerArm", "renderMapHand"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/player/AvatarRenderer;renderLeftHand(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;Z)V"))
    private void renderGlassLeftHand(AvatarRenderer<?> renderer, PoseStack matrices, SubmitNodeCollector collector,
                                     int light, Identifier texture, boolean sleeve, Operation<Void> original) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        module.renderGlassHand(HumanoidArm.LEFT, texture,
                () -> original.call(renderer, matrices, collector, light, texture, sleeve));
    }

    @ModifyExpressionValue(method = {"tick"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;")})
    private ItemStack injectSilentHotbar(ItemStack itemStack) {
        return this.minecraft.player != null
                ? this.minecraft.player.getInventory().getNonEquipmentItems()
                .get(Raven.INSTANCE.inventoryService().hotbarSlotSwapper().getClientsideSlot())
                : itemStack;
    }
}
