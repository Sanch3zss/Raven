package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum AspectRatioPreset implements DisplayNamed {
    RESOLUTION_16_9(Lang.ASPECTRATIO_RESOLUTION_16_9),
    RESOLUTION_16_10(Lang.ASPECTRATIO_RESOLUTION_16_10),
    RESOLUTION_21_9(Lang.ASPECTRATIO_RESOLUTION_21_9),
    RESOLUTION_4_3(Lang.ASPECTRATIO_RESOLUTION_4_3),
    CUSTOM(Lang.ASPECTRATIO_RESOLUTION_CUSTOM);

    final Translation displayName;

    AspectRatioPreset(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
