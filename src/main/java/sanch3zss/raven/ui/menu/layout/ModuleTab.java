package sanch3zss.raven.ui.menu.layout;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;

public enum ModuleTab {
    PLAYER(Translation.clearText("Player"), "player"),
    VISUALS(Translation.clearText("Visuals"), "visuals"),
    MISC(Translation.clearText("Misc"), "misc"),
    INTERFACE(Translation.clearText("Interface"), "interface"),
    ALPHA_PLAYER(Translation.clearText("Player"), "alphaplayer");

    public final Translation name;
    public final String iconName;

    public Translation getName() {
        return this.name;
    }

    public String getIconName() {
        return this.iconName;
    }

    ModuleTab(Translation translation, String str) {
        this.name = translation;
        this.iconName = str;
    }
}
