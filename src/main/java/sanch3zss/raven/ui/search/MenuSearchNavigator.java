package sanch3zss.raven.ui.search;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.ui.TabsController;
import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.menu.card.ModuleCard;
import sanch3zss.raven.ui.menu.layout.MenuTabElement;
import sanch3zss.raven.ui.setting.Setting;

public final class MenuSearchNavigator implements SearchNavigator {
    @Override
    public void focusFrame(AbstractFrame frame, MenuTabElement tabElement) {
        TabsController tabsController = Raven.INSTANCE.tabsController();
        if (tabsController.current() != tabElement) {
            tabsController.open(tabElement);
            tabElement.markFramesDirty();
        }
        ensureCategoryVisible(tabsController, tabElement, frame);
        tabElement.focusFrame(frame);
    }

    @Override
    public void focusSetting(MenuTabElement tabElement, AbstractFrame frame, Setting setting) {
        TabsController tabsController = Raven.INSTANCE.tabsController();
        if (tabsController.current() != tabElement) {
            tabsController.open(tabElement);
            tabElement.markFramesDirty();
        }
        ensureCategoryVisible(tabsController, tabElement, frame);
        tabElement.focusSetting(frame, setting);
    }

    public void ensureCategoryVisible(TabsController tabsController2, MenuTabElement tabElement, AbstractFrame frame) {
        if (frame instanceof ModuleCard) {
            tabsController2.ensureCategoryVisible(tabElement, ((ModuleCard) frame).module().getCategory());
        }
    }
}
