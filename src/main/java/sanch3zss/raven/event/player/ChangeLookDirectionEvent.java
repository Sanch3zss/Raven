package sanch3zss.raven.event.player;

import sanch3zss.raven.event.CancellableEvent;

public class ChangeLookDirectionEvent extends CancellableEvent {
    private final double deltaX;
    private final double deltaY;

    public ChangeLookDirectionEvent(double deltaX, double deltaY) {
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public double deltaX() {
        return this.deltaX;
    }

    public double deltaY() {
        return this.deltaY;
    }
}
