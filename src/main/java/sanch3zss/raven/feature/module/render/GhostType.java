package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;

public enum GhostType implements DisplayNamed {
    FIRST(Translation.clearText("Первый")),
    SECOND(Translation.clearText("Второй")),
    THIRD(Translation.clearText("Третий"));

    private final Translation displayName;

    GhostType(Translation displayName) {
        this.displayName = displayName;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
