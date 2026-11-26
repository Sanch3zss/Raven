package sanch3zss.raven.mixin.client.input.keyboard;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.input.KeyPressState;
import sanch3zss.raven.event.client.KeyInputEvent;

@Mixin({KeyboardHandler.class})
public class KeyboardMixin {
    @Inject(method = {"keyPress"}, at = {@At("HEAD")})
    private void onKey(long j, int action, KeyEvent input, CallbackInfo callbackInfo) {
        KeyPressState keyPressState2;
        int i = input.key();
        int i3 = action;
        Mc mc = Mc.INSTANCE;
        if (j == mc.getWindow().handle() && mc.getCurrentScreen() == null) {
            switch (i3) {
                case 0:
                    keyPressState2 = KeyPressState.RELEASE;
                    break;
                case 1:
                    keyPressState2 = KeyPressState.PRESS;
                    break;
                default:
                    keyPressState2 = null;
                    break;
            }
            KeyPressState keyPressState = keyPressState2;
            if (keyPressState == null || i == -1) {
                return;
            }
            Raven.INSTANCE.eventDispatcher().dispatch(new KeyInputEvent(keyPressState, i));
        }
    }
}
