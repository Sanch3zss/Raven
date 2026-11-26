package sanch3zss.raven.feature.module.misc.minehelper;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum MineHelperAction implements DisplayNamed {
    NEXT_MINE(Lang.MINE_HELPER_HELP_TYPE_NEXT_MINE),
    CLEAN_INVENTORY(Lang.MINE_HELPER_HELP_TYPE_CLEAN_INVENTORY),
    SAVE_PICKAXE(Lang.MINE_HELPER_HELP_TYPE_SAVE_PICKAXE);

    public final Translation displayName;

    MineHelperAction(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
