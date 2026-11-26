package sanch3zss.raven.event.network;

import sanch3zss.raven.event.CancellableEvent;

public final class OutgoingChatEvent extends CancellableEvent {
    private final String message;

    public OutgoingChatEvent(String message) {
        this.message = message;
    }

    public String message() {
        return this.message;
    }
}
