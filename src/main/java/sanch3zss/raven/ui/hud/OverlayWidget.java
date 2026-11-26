package sanch3zss.raven.ui.hud;

public abstract class OverlayWidget extends WidgetContainer {
    public abstract boolean isOpen();

    public boolean acceptsInput() {
        return isOpen();
    }

    public void closeOverlay() {
        handleClose();
    }
}
