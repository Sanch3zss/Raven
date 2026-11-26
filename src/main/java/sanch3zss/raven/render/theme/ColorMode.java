package sanch3zss.raven.render.theme;

import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;

public enum ColorMode implements DisplayNamed {
    STATIC(Lang.COLOR_MODE_STATIC),
    GRADIENT(Lang.COLOR_MODE_GRADIENT);

    private final Translation displayName;

    ColorMode(Translation displayName) {
        this.displayName = displayName;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
