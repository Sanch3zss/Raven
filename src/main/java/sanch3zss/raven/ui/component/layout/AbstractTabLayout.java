package sanch3zss.raven.ui.component.layout;

import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.menu.layout.MenuTabElement;
import sanch3zss.raven.ui.menu.window.MenuWindow;

public abstract class AbstractTabLayout implements TabLayout {
    public MenuTabElement tab;

    @Override
    public void initialize(MenuTabElement tabElement) {
        this.tab = tabElement;
    }

    public float originX() {
        return this.tab.framesOriginX();
    }

    public float originY() {
        return this.tab.framesOriginY();
    }

    public float scrollY() {
        return this.tab.scrollingAreaComponent().scrollY();
    }

    public float visibleTop() {
        return this.tab.scrollingAreaComponent().visibleTop();
    }

    public float visibleBottom() {
        return this.tab.scrollingAreaComponent().visibleBottom();
    }

    @Override
    public float height() {
        return (MenuWindow.MENU_HEIGHT - MenuWindow.HEADER_HEIGHT) - 2.0f;
    }

    public boolean isFrameVisible(AbstractFrame frame, float f, float f2) {
        float fY = frame.y() - f2;
        return (fY + frame.height()) + frame.contentHeight() >= visibleTop() - f && fY <= visibleBottom() + f;
    }

    public static int indexOfMin(float[] fArr) {
        int i = 0;
        float f = fArr[0];
        for (int i2 = 1; i2 < fArr.length; i2++) {
            if (fArr[i2] < f) {
                f = fArr[i2];
                i = i2;
            }
        }
        return i;
    }

    public MenuTabElement getTab() {
        return this.tab;
    }
}
