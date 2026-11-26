package sanch3zss.raven.event.network;


import sanch3zss.raven.client.chat.ChatDecorator;
import sanch3zss.raven.client.chat.ChatMessageType;
import net.minecraft.network.chat.Component;
import sanch3zss.raven.event.CancellableEvent;
import sanch3zss.raven.feature.world.*;

public class ChatReceiveEvent extends CancellableEvent {
    public final String message;
    public final Component textData;

    public final ChatMessageType type;

    public final ChatDecorator applyChatDecoration;

    public String message() {
        return this.message;
    }

    public Component textData() {
        return this.textData;
    }

    public ChatMessageType type() {
        return this.type;
    }

    public ChatDecorator applyChatDecoration() {
        return this.applyChatDecoration;
    }

    public ChatReceiveEvent(String str, Component text, ChatMessageType chatMessageType, ChatDecorator chatDecorator) {
        this.message = str;
        this.textData = text;
        this.type = chatMessageType;
        this.applyChatDecoration = chatDecorator;
    }
}
