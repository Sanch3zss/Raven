package sanch3zss.raven.feature.module.misc;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum AutoAuthMode implements DisplayNamed {
    RANDOM(Lang.AUTOAUTH_MODE_RANDOM),
    CUSTOM(Lang.AUTOAUTH_MODE_CUSTOM);

    final Translation displayName;

    AutoAuthMode(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
