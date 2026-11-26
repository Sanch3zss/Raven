package sanch3zss.raven.client.chat;

public enum ChatLimitType {
    HISTORY,
    LIMIT;

    public boolean isHistory() {
        return this == HISTORY;
    }

    public boolean isLimit() {
        return this == LIMIT;
    }
}
