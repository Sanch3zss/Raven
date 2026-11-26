package sanch3zss.raven.mixin.client.render.gui;

import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.ui.hud.VanillaItemAlphaContext;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.waypoint.WaypointRenderer;

@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {
    @Inject(method = "render()V", at = @At("HEAD"))
    private void raven$renderCustomHudBehindVanillaItems(CallbackInfo callbackInfo) {
        Raven.INSTANCE.widgetStack().captureHudBlur();
        WaypointRenderer.renderPrepared();
        Raven.INSTANCE.widgetStack().renderPreparedCustomHud();
    }

    @Inject(method = "render()V", at = @At("RETURN"))
    private void raven$clearItemAlphaState(CallbackInfo callbackInfo) {
        VanillaItemAlphaContext.clearPoses();
    }
}
