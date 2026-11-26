package sanch3zss.raven.mixin.client.render.entity;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sanch3zss.raven.feature.module.render.CustomHitBoxModule;

@Mixin(DebugRenderer.class)
public abstract class DebugRendererMixin {
    @Shadow @Final private List<DebugRenderer.SimpleDebugRenderer> renderers;
    @Unique private boolean raven$customHitBoxActive;

    @Shadow public abstract void refreshRendererList();

    @Inject(method = "emitGizmos", at = @At("HEAD"))
    private void raven$refreshCustomHitBox(Frustum frustum, double camX, double camY, double camZ,
                                           float partialTicks, CallbackInfo callbackInfo) {
        boolean active = CustomHitBoxModule.active() != null;
        if (active != this.raven$customHitBoxActive) {
            this.raven$customHitBoxActive = active;
            refreshRendererList();
        }
    }

    @Inject(method = "refreshRendererList", at = @At("TAIL"))
    private void raven$showCustomHitBox(CallbackInfo callbackInfo) {
        if (CustomHitBoxModule.active() != null
                && this.renderers.stream().noneMatch(EntityHitboxDebugRenderer.class::isInstance)) {
            this.renderers.add(new EntityHitboxDebugRenderer(Minecraft.getInstance()));
        }
    }
}
