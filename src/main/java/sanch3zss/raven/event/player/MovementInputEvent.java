package sanch3zss.raven.event.player;

import sanch3zss.raven.event.CancellableEvent;
import sanch3zss.raven.util.math.interpolation.DirectionalInput;

public class MovementInputEvent extends CancellableEvent {
    public DirectionalInput input;
    public boolean jumping;
    public boolean sneaking;
    public boolean sprinting;

    public DirectionalInput getInput() {
        return this.input;
    }

    public boolean isJumping() {
        return this.jumping;
    }

    public boolean isSneaking() {
        return this.sneaking;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public void setInput(DirectionalInput directionalInput) {
        this.input = directionalInput;
    }

    public void setJumping(boolean z) {
        this.jumping = z;
    }

    public void setSneaking(boolean z) {
        this.sneaking = z;
    }

    public void setSprinting(boolean z) {
        this.sprinting = z;
    }

    public MovementInputEvent(DirectionalInput directionalInput, boolean z, boolean z2, boolean z3) {
        this.input = directionalInput;
        this.jumping = z;
        this.sneaking = z2;
        this.sprinting = z3;
    }
}
