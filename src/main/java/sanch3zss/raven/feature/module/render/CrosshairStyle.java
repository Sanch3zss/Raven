package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum CrosshairStyle implements DisplayNamed {
    DEFAULT(Lang.CROSSHAIR_TYPE_DEFAULT),
    CIRCLE(Lang.CROSSHAIR_TYPE_CIRCLE);

    final Translation displayName;

    CrosshairStyle(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
