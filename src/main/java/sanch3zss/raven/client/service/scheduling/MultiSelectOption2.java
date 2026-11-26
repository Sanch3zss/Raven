package sanch3zss.raven.client.service.scheduling;

import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.util.math.interpolation.Easings;

public class MultiSelectOption2<T> {
    public final T option;
    public final ClickableBehavior clickableBehavior = new ClickableBehavior();
    public final ToggleAnimator toggleAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);

    public MultiSelectOption2(T t) {
        this.option = t;
    }

    public T option() {
        return this.option;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator toggleAnimation() {
        return this.toggleAnimation;
    }
}
