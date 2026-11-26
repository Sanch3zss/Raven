package sanch3zss.raven.event.render;

import sanch3zss.raven.event.CancellableEvent;
import sanch3zss.raven.event.render.RenderOverlayType;

public class RenderOverlayEvent extends CancellableEvent {
    public final RenderOverlayType type;

    public RenderOverlayType getType() {
        return this.type;
    }

    public RenderOverlayEvent(RenderOverlayType renderOverlayType) {
        this.type = renderOverlayType;
    }
}
