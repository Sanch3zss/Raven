package sanch3zss.raven.ui.component.layout;

import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.WidgetBounds;
import sanch3zss.raven.ui.component.control.ToggleSwitch;

public class OrderedListRow<E> {
    public final E value;
    public final ToggleSwitch toggleSwitch;
    public final ToggleAnimator toggleAnimation;
    public final WidgetBounds rowRect = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds grabRect = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);

    public E value() {
        return this.value;
    }

    public ToggleSwitch toggleSwitch() {
        return this.toggleSwitch;
    }

    public ToggleAnimator toggleAnimation() {
        return this.toggleAnimation;
    }

    public WidgetBounds rowRect() {
        return this.rowRect;
    }

    public WidgetBounds grabRect() {
        return this.grabRect;
    }

    public OrderedListRow(E e, ToggleSwitch toggleSwitch, ToggleAnimator animator) {
        this.value = e;
        this.toggleSwitch = toggleSwitch;
        this.toggleAnimation = animator;
    }
}
