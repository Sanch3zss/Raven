package sanch3zss.raven.mixin.client.screen;

import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.network.OutgoingChatEvent;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void raven$beforeSendMessage(String message, boolean addToHistory, CallbackInfo callbackInfo) {
        OutgoingChatEvent event = new OutgoingChatEvent(message);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
