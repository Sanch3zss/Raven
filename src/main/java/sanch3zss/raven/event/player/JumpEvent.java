package sanch3zss.raven.event.player;

import sanch3zss.raven.event.Event;

public class JumpEvent implements Event {
    public float yaw;

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float f) {
        this.yaw = f;
    }

    public JumpEvent(float f) {
        this.yaw = f;
    }
}
