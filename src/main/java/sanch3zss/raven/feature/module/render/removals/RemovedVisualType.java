package sanch3zss.raven.feature.module.render.removals;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum RemovedVisualType implements DisplayNamed {
    CAMERA_HURT(Lang.REMOVALS_CAMERA_HURT),
    FIRE_OVERLAY(Lang.REMOVALS_FIRE_OVERLAY),
    LAVA_OVERLAY(Lang.REMOVALS_LAVA_OVERLAY),
    WATER_OVERLAY(Lang.REMOVALS_WATER_OVERLAY),
    VIGNETTE(Lang.REMOVALS_VIGNETTE),
    HOLOGRAMS(Lang.REMOVALS_HOLOGRAMS),
    FISHING_BOBBER(Lang.REMOVALS_FISHING_BOBBER),
    SCOREBOARD(Lang.REMOVALS_SCOREBOARD),
    BOSS_BAR(Lang.REMOVALS_BOSS_BAR),
    TOTEM_POP(Lang.REMOVALS_TOTEM_POP),
    GLOWING(Lang.REMOVALS_GLOWING),
    WITHER_HEARTS(Lang.REMOVALS_WITHER_HEARTS);

    final Translation displayName;

    RemovedVisualType(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
