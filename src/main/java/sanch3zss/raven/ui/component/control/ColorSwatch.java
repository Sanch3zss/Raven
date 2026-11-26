package sanch3zss.raven.ui.component.control;

import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ColorSwatch {
    public final int color;
    public final boolean system;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final ToggleAnimator selectAnim = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);

    public int color() {
        return this.color;
    }

    public boolean system() {
        return this.system;
    }

    public ClickableBehavior clickable() {
        return this.clickable;
    }

    public ToggleAnimator selectAnim() {
        return this.selectAnim;
    }

    public ColorSwatch(int i, boolean z) {
        this.color = i;
        this.system = z;
    }
}
