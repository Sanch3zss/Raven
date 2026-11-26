package sanch3zss.raven.client.service.scheduling;

import java.util.function.Supplier;

import sanch3zss.raven.feature.setting.MultiSelectOption;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.ui.component.control.*;
import sanch3zss.raven.util.math.interpolation.Easings;

public class MultiSelectOptionItem<T> {
    final MultiSelectOption<T> option;
    final ClickableBehavior clickableBehavior = new ClickableBehavior();
    public final ToggleAnimator currentOptionAnimation = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);

    final ToggleSwitch toggleSwitch;

    public MultiSelectOptionItem(MultiSelectOption<T> multiSelectOption, Supplier<Boolean> supplier) {
        this.option = multiSelectOption;
        this.toggleSwitch = new ToggleSwitch(22.0f, 15.0f, supplier);
    }

    public MultiSelectOption<T> option() {
        return this.option;
    }

    public ClickableBehavior clickableBehavior() {
        return this.clickableBehavior;
    }

    public ToggleAnimator currentOptionAnimation() {
        return this.currentOptionAnimation;
    }

    public ToggleSwitch toggleSwitch() {
        return this.toggleSwitch;
    }
}
