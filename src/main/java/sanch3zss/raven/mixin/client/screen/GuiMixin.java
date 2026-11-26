package sanch3zss.raven.mixin.client.screen;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.client.ScreenOpenEvent;

@Mixin(Gui.class)
public class GuiMixin {
    @Unique
    private boolean raven$replacingScreen;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo callbackInfo) {
        if (this.raven$replacingScreen) {
            return;
        }
        ScreenOpenEvent event = new ScreenOpenEvent(screen);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
            return;
        }
        if (event.screen() != screen) {
            callbackInfo.cancel();
            this.raven$replacingScreen = true;
            try {
                ((Gui) (Object) this).setScreen(event.screen());
            } finally {
                this.raven$replacingScreen = false;
            }
        }
    }
}
