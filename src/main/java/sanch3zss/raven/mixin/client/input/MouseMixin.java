package sanch3zss.raven.mixin.client.input;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.*;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.feature.setting.ButtonAction;
import sanch3zss.raven.feature.module.player.ItemScrollerController;

@Mixin({MouseHandler.class})
public class MouseMixin {

    @WrapOperation(method = "handleAccumulatedMovement", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/Screen;mouseDragged(Lnet/minecraft/client/input/MouseButtonEvent;DD)Z"))
    private boolean raven$itemScrollerDrag(Screen screen, net.minecraft.client.input.MouseButtonEvent event,
                                            double deltaX, double deltaY,
                                            Operation<Boolean> original) {
        if (ItemScrollerController.onMouseDragged(screen, event)) {
            return true;
        }
        return original.call(screen, event, deltaX, deltaY);
    }

    @WrapWithCondition(method = {"turnPlayer"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V")}, require = 1, allow = 1)
    private boolean modifyMouseRotationInput(LocalPlayer clientPlayerEntity, double d, double d2) {
        ChangeLookDirectionEvent event4 = new ChangeLookDirectionEvent(d, d2);
        Raven.INSTANCE.eventDispatcher().dispatch(event4);
        if (event4.isCancelled()) {
            return false;
        }
        clientPlayerEntity.turn(d, d2);
        return false;
    }

    @Inject(method = {"onButton"}, at = {@At("HEAD")})
    private void onMouseButton(long j, MouseButtonInfo input, int action, CallbackInfo callbackInfo) {
        ButtonAction buttonAction2;
        int i = input.button();
        int i2 = action;
        Mc mc = Mc.INSTANCE;
        if (j == mc.getWindow().handle()) {
            switch (i2) {
                case 0:
                    buttonAction2 = ButtonAction.RELEASE;
                    break;
                case 1:
                    buttonAction2 = ButtonAction.PRESS;
                    break;
                default:
                    buttonAction2 = null;
                    break;
            }
            ButtonAction buttonAction = buttonAction2;
            if (buttonAction == null || i == -1) {
                return;
            }
            if (mc.getCurrentScreen() == null) {
                Raven.INSTANCE.eventDispatcher().dispatch(new MouseButtonEvent(buttonAction, i));
            } else {
                Raven.INSTANCE.eventDispatcher().dispatch(new MouseButtonEvent2(buttonAction, i));
            }
        }
    }
}
