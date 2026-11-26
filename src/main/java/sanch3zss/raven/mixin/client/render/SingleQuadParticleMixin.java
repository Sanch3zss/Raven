package sanch3zss.raven.mixin.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TotemParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.poptweaks.PopTweaksModule;
import sanch3zss.raven.feature.module.render.targetesp.TargetESPModule;
import sanch3zss.raven.render.shape.ShapeRenderer;
import sanch3zss.raven.mixin.accessor.ParticleAccessor;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleMixin {
    @Shadow protected float rCol;
    @Shadow protected float gCol;
    @Shadow protected float bCol;
    @Shadow protected float alpha;

    @Inject(method = "extract", at = @At("HEAD"), cancellable = true)
    private void raven$replaceTotemSprite(QuadParticleRenderState renderState, Camera camera,
                                          float partialTick, CallbackInfo callbackInfo) {
        if (!((Object) this instanceof TotemParticle)) {
            return;
        }
        Raven raven = Raven.INSTANCE;
        if (raven == null) {
            return;
        }
        PopTweaksModule module = raven.moduleRepository().get(PopTweaksModule.class);
        if (!module.shouldReplaceTotemParticles()) {
            return;
        }
        SingleQuadParticle particle = (SingleQuadParticle) (Object) this;
        ParticleAccessor accessor = (ParticleAccessor) this;
        int lifetime = Math.max(1, particle.getLifetime());
        float progress = Mth.clamp((accessor.raven$getAge() + partialTick) / lifetime, 0.0f, 1.0f);
        float fade = 1.0f - (float) Mth.smoothstep(
                Mth.clamp((progress - 0.35f) / 0.65f, 0.0f, 1.0f));
        int red = Mth.clamp(Math.round(this.rCol * 255.0f), 0, 255);
        int green = Mth.clamp(Math.round(this.gCol * 255.0f), 0, 255);
        int blue = Mth.clamp(Math.round(this.bCol * 255.0f), 0, 255);
        int opacity = Mth.clamp(Math.round(this.alpha * fade * 255.0f), 0, 255);
        int color = opacity << 24 | red << 16 | green << 8 | blue;
        double x = Mth.lerp(partialTick, accessor.raven$getPreviousX(), accessor.raven$getX());
        double y = Mth.lerp(partialTick, accessor.raven$getPreviousY(), accessor.raven$getY());
        double z = Mth.lerp(partialTick, accessor.raven$getPreviousZ(), accessor.raven$getZ());
        float size = particle.getQuadSize(partialTick) * 2.0f;
        if (opacity > 0 && size > 0.0f) {
            ShapeRenderer.INSTANCE.addWorldBillboard(x, y, z, size,
                    TargetESPModule.soulsTexture, color, true, true, true);
        }
        callbackInfo.cancel();
    }
}
