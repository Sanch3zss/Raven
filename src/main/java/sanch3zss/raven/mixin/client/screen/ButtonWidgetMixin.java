package sanch3zss.raven.mixin.client.screen;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Button.Builder.class})
public class ButtonWidgetMixin {

    @Shadow
    @Final
    private Component message;

    @Shadow
    private int x;

    @Inject(method = {"pos"}, at = {@At("TAIL")})
    private void builderHook(int i, int i2, CallbackInfoReturnable<Button.Builder> callbackInfoReturnable) {
        if (this.message.getString().equals("ViaFabricPlus")) {
            this.x = 5;
        }
    }
}
