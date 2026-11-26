package sanch3zss.raven.event.player;

import sanch3zss.raven.event.Event;

public class MovementYawEvent implements Event {
    public float yaw;

    public float getYaw() {
        return this.yaw;
    }

    public void setYaw(float f) {
        this.yaw = f;
    }

    public MovementYawEvent(float f) {
        this.yaw = f;
    }
}
