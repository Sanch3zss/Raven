package sanch3zss.raven.ui.search;

import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.menu.layout.MenuTabElement;
import sanch3zss.raven.ui.setting.Setting;

public interface SearchNavigator {
    void focusFrame(AbstractFrame frame, MenuTabElement tabElement);

    void focusSetting(MenuTabElement tabElement, AbstractFrame frame, Setting setting);
}
