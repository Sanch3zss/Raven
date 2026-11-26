package sanch3zss.raven.feature.server;

import sanch3zss.raven.client.chat.*;

public enum ServerType implements DisplayNamed {
    FUNTIME(Translation.clearText("FunTime")),
    HOLYWORLD(Translation.clearText("HolyWorld")),
    REALLYWORLD(Translation.clearText("ReallyWorld"));

    public final Translation displayName;

    ServerType(Translation displayName) {
        this.displayName = displayName;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Translation displayName() {
        return this.displayName;
    }
}
