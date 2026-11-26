package sanch3zss.raven.mixin.client.chat;

import java.time.Instant;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.network.ChatReceiveEvent;
import sanch3zss.raven.client.chat.ChatMessageType;

@Mixin({ChatListener.class})
public class MessageHandlerMixin {

    @Shadow
    private long previousMessageTime;

    @Inject(method = {"handleDisguisedChatMessage"}, at = {@At("HEAD")}, cancellable = true)
    private void injectDisguisedChatMessage(Component text, ChatType.Bound parameters, CallbackInfo callbackInfo) {
        if (raven$emitChatEvent(parameters, text, ChatMessageType.DISGUISED_CHAT_MESSAGE)) {
            this.previousMessageTime = Util.getMillis();
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"showMessageToPlayer"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", ordinal = 0, shift = At.Shift.BEFORE)}, cancellable = true)
    private void injectChatMessage1(ChatType.Bound parameters, PlayerChatMessage signedMessage, Component text, GameProfile gameProfile, boolean z, Instant instant, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (raven$emitChatEvent(null, text, ChatMessageType.CHAT_MESSAGE)) {
            this.previousMessageTime = Util.getMillis();
            callbackInfoReturnable.cancel();
        }
    }

    @Inject(method = {"showMessageToPlayer"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addPlayerMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", ordinal = 1, shift = At.Shift.BEFORE)}, cancellable = true)
    private void injectChatMessage2(ChatType.Bound parameters, PlayerChatMessage signedMessage, Component text, GameProfile gameProfile, boolean z, Instant instant, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        if (raven$emitChatEvent(parameters, text, ChatMessageType.CHAT_MESSAGE)) {
            this.previousMessageTime = Util.getMillis();
            callbackInfoReturnable.cancel();
        }
    }

    @Inject(method = {"handleSystemMessage"}, at = {@At("HEAD")}, cancellable = true)
    private void injectGameMessage(Component text, boolean z, CallbackInfo callbackInfo) {
        if (raven$emitChatEvent(null, text, z ? ChatMessageType.DISGUISED_CHAT_MESSAGE : ChatMessageType.GAME_MESSAGE)) {
            this.previousMessageTime = Util.getMillis();
            callbackInfo.cancel();
        }
    }

    @Unique
    private boolean raven$emitChatEvent(ChatType.Bound parameters, Component text, ChatMessageType chatMessageType) {
        ChatReceiveEvent event = new ChatReceiveEvent(text.getString(), text, chatMessageType, text2 -> {
            return parameters != null ? parameters.decorate(text) : text;
        });
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        return event.isCancelled();
    }
}
