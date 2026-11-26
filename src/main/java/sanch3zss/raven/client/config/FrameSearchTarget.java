package sanch3zss.raven.client.config;

import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.menu.layout.MenuTabElement;
import sanch3zss.raven.ui.search.SearchNavigator;

public final class FrameSearchTarget implements SearchNavTarget {
    public final AbstractFrame frameContainer;
    public final MenuTabElement tab;

    public FrameSearchTarget(AbstractFrame frame, MenuTabElement tabElement) {
        this.frameContainer = frame;
        this.tab = tabElement;
    }

    @Override
    public void navigate(SearchNavigator searchNavigator) {
        searchNavigator.focusFrame(this.frameContainer, this.tab);
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "frameContainer=" + this.frameContainer + ", " + "tab=" + this.tab + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.frameContainer, this.tab);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FrameSearchTarget)) return false;
        FrameSearchTarget o = (FrameSearchTarget) obj;
        return java.util.Objects.equals(this.frameContainer, o.frameContainer) && java.util.Objects.equals(this.tab, o.tab);
    }
public AbstractFrame frameContainer() {
        return this.frameContainer;
    }

    public MenuTabElement tab() {
        return this.tab;
    }
}
