package sanch3zss.raven.mixin.client.chat;

import java.util.List;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.accessor.ChatLineIdAccessor;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.network.ChatLimitEvent;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.client.chat.ChatLimitType;

@Mixin({ChatComponent.class})
public abstract class ChatHudMixin {

    @Shadow
    @Final
    public List<GuiMessage.Line> trimmedMessages;

    @Shadow
    @Final
    public List<GuiMessage> allMessages;

    @Shadow
    private boolean newMessageSinceScroll;

    @Shadow
    private int chatScrollbarPos;

    @Shadow
    public abstract void scrollChat(int i);

    @Shadow
    public abstract boolean isChatFocused();

    @Shadow
    public abstract int getWidth();

    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true)
    private Component raven$friendAliases(Component contents) {
        return FriendManager.applyAliases(contents);
    }

    @Redirect(method = {"addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"}, at = @At(value = "INVOKE", target = "Ljava/util/List;size()I", ordinal = 0))
    public int hookGetSize2(List<GuiMessage.Line> list) {
        ChatLimitEvent event = new ChatLimitEvent(ChatLimitType.LIMIT);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            return -1;
        }
        return list.size();
    }

    @Inject(method = {"addMessageToDisplayQueue"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;isChatFocused()Z", shift = At.Shift.BEFORE)}, cancellable = true)
    public void hookAddVisibleMessage(GuiMessage chatHudLine, CallbackInfo callbackInfo, @Local List<FormattedCharSequence> list) {
        boolean zMethod_1819 = isChatFocused();
        String strRaven_ru$getId = ((ChatLineIdAccessor) ChatLineIdAccessor.class.cast(chatHudLine)).raven$getId();
        int i = 0;
        while (i < list.size()) {
            FormattedCharSequence orderedText = list.get(i);
            if (zMethod_1819 && this.chatScrollbarPos > 0) {
                this.newMessageSinceScroll = true;
                scrollChat(1);
            }
            GuiMessage.Line visible = new GuiMessage.Line(chatHudLine, orderedText, i == list.size() - 1);
            ((ChatLineIdAccessor) ChatLineIdAccessor.class.cast(visible)).raven$setId(strRaven_ru$getId);
            this.trimmedMessages.addFirst(visible);
            i++;
        }
        ChatLimitEvent event = new ChatLimitEvent(ChatLimitType.LIMIT);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (!event.isCancelled()) {
            while (this.trimmedMessages.size() > 100) {
                this.trimmedMessages.removeLast();
            }
        }
        callbackInfo.cancel();
    }

    @Inject(method = {"clearMessages"}, at = {@At("HEAD")}, cancellable = true)
    private void onClear(boolean z, CallbackInfo callbackInfo) {
        ChatLimitEvent event = new ChatLimitEvent(ChatLimitType.HISTORY);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
