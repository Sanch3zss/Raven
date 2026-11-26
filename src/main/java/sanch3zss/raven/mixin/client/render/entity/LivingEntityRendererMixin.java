package sanch3zss.raven.mixin.client.render.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import sanch3zss.raven.util.math.rotation.RotationManager;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.hitcolor.HitColorModule;
import sanch3zss.raven.feature.module.render.hitcolor.HitColorRenderState;
import sanch3zss.raven.feature.module.render.party.PartyModule;
import sanch3zss.raven.feature.module.render.party.PartyRenderState;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState> {
    @ModifyExpressionValue(method = "submit", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/ARGB;multiply(II)I"))
    private int raven$partyTint(int original, @Local(argsOnly = true) S state) {
        if (Raven.INSTANCE == null || Raven.INSTANCE.moduleRepository == null) return original;
        PartyModule module = Raven.INSTANCE.moduleRepository().get(PartyModule.class);
        return module.tint(((PartyRenderState) state).raven$partyEntityId(), original);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void raven$hurtProgress(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
        float progress = 1.0f;
        if (entity.deathTime <= 0 && entity.hurtTime > 0 && entity.hurtDuration > 0) {
            progress = 1.0f - Mth.clamp((entity.hurtTime - partialTick) / entity.hurtDuration, 0.0f, 1.0f);
        }
        ((HitColorRenderState) state).raven$hurtProgress(progress);
    }

    @Inject(method = "getOverlayCoords", at = @At("RETURN"), cancellable = true)
    private static void raven$hitColor(LivingEntityRenderState state, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if (!state.hasRedOverlay || Raven.INSTANCE == null || Raven.INSTANCE.moduleRepository == null) return;
        HitColorModule module = Raven.INSTANCE.moduleRepository().get(HitColorModule.class);
        if (!module.isState()) return;
        cir.setReturnValue(OverlayTexture.pack(OverlayTexture.u(whiteOverlayProgress),
                module.overlayRow(((HitColorRenderState) state).raven$hurtProgress())));
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F"))
    private float lerpAngleDegreesHook(float original, @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) float delta) {
        if (!entity.equals(Minecraft.getInstance().player)) {
            return original;
        }
        RotationManager manager = RotationManager.INSTANCE;
        return Mth.rotLerp(delta, manager.getPreviousRotation().getYaw(), manager.getCurrentRotation().getYaw());
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F"))
    private float getLerpedPitchHook(float original, @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true) float delta) {
        if (!entity.equals(Minecraft.getInstance().player)) {
            return original;
        }
        RotationManager manager = RotationManager.INSTANCE;
        return Mth.lerp(delta, manager.getPreviousRotation().getPitch(), manager.getCurrentRotation().getPitch());
    }
}
