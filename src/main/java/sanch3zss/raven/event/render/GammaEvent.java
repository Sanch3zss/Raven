package sanch3zss.raven.event.render;

import sanch3zss.raven.event.CancellableEvent;

public class GammaEvent extends CancellableEvent {
    public double gamma;

    public double getGamma() {
        return this.gamma;
    }

    public void setGamma(double d) {
        this.gamma = d;
    }
}
