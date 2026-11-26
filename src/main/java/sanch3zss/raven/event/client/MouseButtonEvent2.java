package sanch3zss.raven.event.client;

import sanch3zss.raven.event.Event;
import sanch3zss.raven.feature.setting.ButtonAction;

public final class MouseButtonEvent2 implements Event {
    public final ButtonAction action;
    public final int button;

    public MouseButtonEvent2(ButtonAction buttonAction, int i) {
        this.action = buttonAction;
        this.button = i;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "action=" + this.action + ", " + "button=" + this.button + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.action, this.button);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MouseButtonEvent2)) return false;
        MouseButtonEvent2 o = (MouseButtonEvent2) obj;
        return java.util.Objects.equals(this.action, o.action) && java.util.Objects.equals(this.button, o.button);
    }
public ButtonAction action() {
        return this.action;
    }

    public int button() {
        return this.button;
    }
}
