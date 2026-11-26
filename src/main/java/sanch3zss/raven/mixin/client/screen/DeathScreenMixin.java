package sanch3zss.raven.mixin.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.DeathScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.player.DeathTickEvent;

@Mixin({DeathScreen.class})
public class DeathScreenMixin {

    @Shadow
    private int delayTicker;

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void render(GuiGraphicsExtractor drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new DeathTickEvent(this.delayTicker));
    }
}
