package sanch3zss.raven.mixin.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.render.RenderOverlayEvent;
import sanch3zss.raven.event.render.RenderOverlayType;

@Mixin({BossHealthOverlay.class})
public class BossBarHudMixin {
    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphicsExtractor drawContext, CallbackInfo callbackInfo) {
        RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayType.BOSS_BAR);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
