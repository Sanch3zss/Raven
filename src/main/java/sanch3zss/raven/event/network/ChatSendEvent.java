package sanch3zss.raven.event.network;

import sanch3zss.raven.event.CancellableEvent;

public class ChatSendEvent extends CancellableEvent {
    public final String originalText;
    public String changedText = "";

    public String getOriginalText() {
        return this.originalText;
    }

    public String getChangedText() {
        return this.changedText;
    }

    public void setChangedText(String str) {
        this.changedText = str;
    }

    public ChatSendEvent(String str) {
        this.originalText = str;
    }
}
