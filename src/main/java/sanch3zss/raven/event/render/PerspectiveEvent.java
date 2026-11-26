package sanch3zss.raven.event.render;

import net.minecraft.client.CameraType;
import sanch3zss.raven.event.Event;

public class PerspectiveEvent implements Event {
    public CameraType perspective;

    public PerspectiveEvent perspective(CameraType perspective) {
        this.perspective = perspective;
        return this;
    }

    public CameraType perspective() {
        return this.perspective;
    }

    public PerspectiveEvent(CameraType perspective) {
        this.perspective = perspective;
    }
}
