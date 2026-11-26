package sanch3zss.raven.ui.hud;

import sanch3zss.raven.event.WindowVisibilityListener;
import sanch3zss.raven.render.data.LayoutScaleContext;

public abstract class AbstractWindow extends WidgetContainer {
    public WindowVisibilityListener windowVisibilityListener;

    public void visibilityListener(WindowVisibilityListener windowVisibilityListener) {
        this.windowVisibilityListener = windowVisibilityListener;
    }

    public void onVisibilityChanged(boolean z) {
        if (this.windowVisibilityListener != null) {
            this.windowVisibilityListener.onWindowVisibilityChanged(this, z);
        }
    }

    public void open() {
        visible(true);
        onVisibilityChanged(true);
    }

    public void close() {
        visible(false);
        onVisibilityChanged(false);
        handleClose();
    }

    public void toggle() {
        if (this.visible) {
            close();
        } else {
            open();
        }
    }

    public AbstractWindow(float f, float f2) {
        setSize(f, f2);
    }

    public void center(LayoutScaleContext layoutScaleContext) {
        setPosition(layoutScaleContext.alignToScale((layoutScaleContext.logicalWidth() - width()) / 2.0f), layoutScaleContext.alignToScale((layoutScaleContext.logicalHeight() - height()) / 2.0f));
    }
}
