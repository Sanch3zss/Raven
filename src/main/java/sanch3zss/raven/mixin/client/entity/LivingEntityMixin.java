package sanch3zss.raven.mixin.client.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.event.render.HandSwingEvent;
import sanch3zss.raven.feature.module.render.KillEffectsModule;
import sanch3zss.raven.feature.module.render.armtweaks.knife.KnifeRenderer;
import sanch3zss.raven.util.math.rotation.*;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin {

    @Unique
    private JumpEvent lastJumpEvent;

    @Unique
    private InteractionHand raven$pendingSwingHand;

    @Shadow
    public float yBodyRot;

    @Shadow
    public abstract void setSprinting(boolean z);

    @Shadow
    protected abstract float getMaxHeadRotationRelativeToBody();

    @Inject(method = {"jumpFromGround"}, at = {@At("HEAD")})
    private void jump(CallbackInfo callbackInfo) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (livingEntity instanceof LocalPlayer) {
            JumpEvent event = new JumpEvent(livingEntity.getYRot());
            Raven.INSTANCE.eventDispatcher().dispatch(event);
            this.lastJumpEvent = event;
        }
    }

    @Inject(method = "makePoofParticles", at = @At("HEAD"), cancellable = true)
    private void raven$hideDeathPoof(CallbackInfo callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide() && entity.isDeadOrDying()
                && Raven.INSTANCE.moduleRepository().get(KillEffectsModule.class).isState()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleEntityEvent", at = @At("HEAD"))
    private void onDeath(byte id, CallbackInfo callbackInfo) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (id == 3 && entity.level().isClientSide()) {
            Raven.INSTANCE.eventDispatcher().dispatch(
                    new EntityDeathEvent(entity, entity.damageSources().generic())
            );
        }
    }

    @Inject(method = "handleDamageEvent", at = @At("HEAD"))
    private void raven$onPlayerDamage(DamageSource source, CallbackInfo callbackInfo) {
        if ((Object) this instanceof Player target && source.getEntity() instanceof Player attacker) {
            Raven.INSTANCE.eventDispatcher().dispatch(new PlayerDamageEvent(target, attacker));
        }
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("HEAD"))
    private void raven$beginSwing(InteractionHand hand, boolean sendToSwingingEntity, CallbackInfo callbackInfo) {
        this.raven$pendingSwingHand = hand;
    }

    @Inject(method = "swing(Lnet/minecraft/world/InteractionHand;Z)V", at = @At("RETURN"))
    private void raven$endSwing(InteractionHand hand, boolean sendToSwingingEntity, CallbackInfo callbackInfo) {
        if ((Object) this instanceof LocalPlayer player && player.swingTime == -1
                && player.swingingArm == hand) {
            KnifeRenderer.INSTANCE.onSwing(hand, player.getItemInHand(hand));
        }
        this.raven$pendingSwingHand = null;
    }

    @Inject(method = {"getCurrentSwingDuration"}, at = {@At("HEAD")}, cancellable = true)
    private void getHandSwingDuration(CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        if (!(livingEntity instanceof LocalPlayer)) {
            return;
        }
        InteractionHand hand = this.raven$pendingSwingHand != null ? this.raven$pendingSwingHand
                : livingEntity.swingingArm != null ? livingEntity.swingingArm : InteractionHand.MAIN_HAND;
        HandSwingEvent event2 = new HandSwingEvent(hand);
        Raven.INSTANCE.eventDispatcher().dispatch(event2);
        if (event2.isCancelled()) {
            callbackInfoReturnable.setReturnValue(Integer.valueOf((int) event2.swingSpeed()));
        }
    }

    @ModifyExpressionValue(method = {"updateFallFlyingMovement"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F")})
    private float hookModifyFallFlyingPitch(float f) {
        if (!(((LivingEntity) (Object) this) instanceof LocalPlayer)) {
            return f;
        }
        Rotation currentRotation = RotationManager.INSTANCE.getCurrentRotation();
        return (currentRotation == null || RotationManager.INSTANCE.getCurrentStrategy() == null || !RotationManager.INSTANCE.getCurrentStrategy().moveCorrection()) ? f : currentRotation.getPitch();
    }

    @ModifyExpressionValue(method = {"updateFallFlyingMovement"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;")})
    private Vec3 hookModifyFallFlyingRotationVector(Vec3 vec3d) {
        if (!(((LivingEntity) (Object) this) instanceof LocalPlayer)) {
            return vec3d;
        }
        Rotation currentRotation = RotationManager.INSTANCE.getCurrentRotation();
        return (currentRotation == null || RotationManager.INSTANCE.getCurrentStrategy() == null || !RotationManager.INSTANCE.getCurrentStrategy().moveCorrection()) ? vec3d : currentRotation.getDirectionVector();
    }

    @ModifyExpressionValue(method = {"jumpFromGround"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F")})
    private float getYaw(float f) {
        return (this.lastJumpEvent == null || !(((LivingEntity) (Object) this) instanceof LocalPlayer)) ? f : this.lastJumpEvent.getYaw();
    }

    @ModifyExpressionValue(method = {"tickHeadTurn"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;wrapDegrees(F)F", ordinal = 1)})
    private float turnHeadHook(float original) {
        if ((Object) this != Minecraft.getInstance().player) {
            return original;
        }
        Rotation rotation = RotationManager.INSTANCE.getCurrentRotation();
        return rotation == null ? original : Mth.wrapDegrees(rotation.getYaw() - this.yBodyRot);
    }
}
