package sanch3zss.raven.feature.module.render.targetesp;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum TargetEspMode implements DisplayNamed {
    CUBE(Lang.TARGETESP_MODE_CUBE),
    GHOSTS(Lang.TARGETESP_MODE_GHOSTS),
    CRYSTALS(Lang.TARGETESP_MODE_CRYSTALS),
    JELLO(Translation.clearText("Кольцо"));

    public final Translation displayName;

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    TargetEspMode(Translation translation) {
        this.displayName = translation;
    }
}
