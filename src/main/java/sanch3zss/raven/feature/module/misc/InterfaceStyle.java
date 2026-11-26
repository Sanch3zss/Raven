package sanch3zss.raven.feature.module.misc;

import sanch3zss.raven.client.chat.*;

public enum InterfaceStyle implements DisplayNamed {
    DARK(Translation.clearText("Тёмная")),
    LIGHT(Translation.clearText("Светлая"));

    final Translation displayName;

    InterfaceStyle(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
