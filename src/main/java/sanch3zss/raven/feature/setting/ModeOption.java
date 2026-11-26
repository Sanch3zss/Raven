package sanch3zss.raven.feature.setting;

import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ModeOption<T> {
    public final T option;
    public final ClickableBehavior clickableBehavior = new ClickableBehavior();
    public final ToggleAnimator currentOptionAnimation = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);

    public T option() {
        return this.option;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator currentOptionAnimation() {
        return this.currentOptionAnimation;
    }

    public ModeOption(T t) {
        this.option = t;
    }
}
