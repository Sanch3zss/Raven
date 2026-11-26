package sanch3zss.raven.feature.setting;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;

public enum SettingUnit {
    UNITS(Lang.UNIT_UNITS),
    BLOCKS(Lang.UNIT_BLOCKS),
    TICKS(Lang.UNIT_TICKS),
    MILLISECONDS(Lang.UNIT_MILLISECONDS),
    SECONDS(Lang.UNIT_SECONDS),
    HITPOINTS(Lang.UNIT_HITPOINTS),
    PERCENTS(Lang.UNIT_PERCENTS),
    DEGREES(Lang.UNIT_DEGREES),
    PIXELS(Lang.UNIT_PIXELS);

    public final Translation unitName;

    public String format(float f) {
        return this.unitName.effective();
    }

    public Translation getUnitName() {
        return this.unitName;
    }

    SettingUnit(Translation translation) {
        this.unitName = translation;
    }
}
