package sanch3zss.raven.event.render;

import net.minecraft.world.InteractionHand;
import sanch3zss.raven.event.CancellableEvent;

public class HandSwingEvent extends CancellableEvent {
    private final InteractionHand hand;
    public float swingSpeed;

    public HandSwingEvent(InteractionHand hand) {
        this.hand = hand;
    }

    public InteractionHand hand() {
        return this.hand;
    }

    public float swingSpeed() {
        return this.swingSpeed;
    }

    public HandSwingEvent swingSpeed(float f) {
        this.swingSpeed = f;
        return this;
    }
}
