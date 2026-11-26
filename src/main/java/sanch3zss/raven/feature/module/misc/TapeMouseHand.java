package sanch3zss.raven.feature.module.misc;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum TapeMouseHand implements DisplayNamed {
    RIGHT(Lang.TAPEMOUSE_HANDMODE_RIGHT),
    LEFT(Lang.TAPEMOUSE_HANDMODE_LEFT);

    final Translation displayName;

    TapeMouseHand(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
