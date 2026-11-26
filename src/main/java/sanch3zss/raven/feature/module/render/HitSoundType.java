package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum HitSoundType implements DisplayNamed {
    TYPE_1(Lang.HITSOUNDS_TYPE_1),
    TYPE_2(Lang.HITSOUNDS_TYPE_2),
    TYPE_3(Lang.HITSOUNDS_TYPE_3),
    TYPE_4(Lang.HITSOUNDS_TYPE_4);

    final Translation displayName;

    HitSoundType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
