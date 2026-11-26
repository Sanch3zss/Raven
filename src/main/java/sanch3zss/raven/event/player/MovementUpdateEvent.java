package sanch3zss.raven.event.player;

import sanch3zss.raven.event.*;
import sanch3zss.raven.util.math.interpolation.DirectionalInput;

public class MovementUpdateEvent extends CancellableEvent {
    DirectionalInput directionalInput;
    boolean sprint;
    MovementUpdateSource source;

    public MovementUpdateEvent(DirectionalInput directionalInput, boolean z, MovementUpdateSource movementUpdateSource) {
        this.directionalInput = directionalInput;
        this.sprint = z;
        this.source = movementUpdateSource;
    }

    public DirectionalInput getDirectionalInput() {
        return this.directionalInput;
    }

    public boolean isSprint() {
        return this.sprint;
    }

    public MovementUpdateSource getSource() {
        return this.source;
    }

    public void setSprint(boolean z) {
        this.sprint = z;
    }
}
