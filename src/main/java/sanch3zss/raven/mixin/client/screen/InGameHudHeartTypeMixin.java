package sanch3zss.raven.mixin.client.screen;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.render.RenderOverlayEvent;
import sanch3zss.raven.event.render.RenderOverlayType;

@Mixin(targets = {"net.minecraft.client.gui.Hud$HeartType"})
public class InGameHudHeartTypeMixin {
    @Inject(method = {"forPlayer"}, at = {@At("RETURN")}, cancellable = true)
    private static void onFromPlayerState(Player playerEntity, CallbackInfoReturnable<Enum> callbackInfoReturnable) {
        if (((Enum) callbackInfoReturnable.getReturnValue()).name().equals("WITHERED")) {
            RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayType.WITHER_HEARTS);
            Raven.INSTANCE.eventDispatcher().dispatch(event);
            if (event.isCancelled()) {
                callbackInfoReturnable.setReturnValue(Enum.valueOf(((Enum) callbackInfoReturnable.getReturnValue()).getDeclaringClass(), "NORMAL"));
            }
        }
    }
}
