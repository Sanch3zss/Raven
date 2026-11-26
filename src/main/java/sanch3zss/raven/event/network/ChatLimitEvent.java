package sanch3zss.raven.event.network;

import sanch3zss.raven.event.CancellableEvent;
import sanch3zss.raven.client.chat.ChatLimitType;

public class ChatLimitEvent extends CancellableEvent {
    public final ChatLimitType type;

    public ChatLimitType getType() {
        return this.type;
    }

    public ChatLimitEvent(ChatLimitType chatLimitType) {
        this.type = chatLimitType;
    }
}
