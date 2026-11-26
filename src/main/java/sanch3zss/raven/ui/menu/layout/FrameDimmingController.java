package sanch3zss.raven.ui.menu.layout;

import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.animation.AnimatedFloat;
import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.util.math.interpolation.Easings;

public class FrameDimmingController {
    public final AnimatedFloat dimAnimation = new AnimatedFloat(400, Easings.EASE_IN_OUT_CUBIC);
    public AbstractFrame focusedFrame = null;
    public long focusStartTime = -1;
    public static final long FOCUS_DURATION = 2000;

    public void focusFrame(AbstractFrame frame) {
        if (frame == null) {
            return;
        }
        this.focusedFrame = frame;
        this.focusStartTime = System.currentTimeMillis();
        this.dimAnimation.destination(1.0f);
    }

    public void clearFocus() {
        this.focusStartTime = -1L;
        this.dimAnimation.destination(0.0f);
    }

    public void animate(WeightedEngine weightedEngine) {
        if (this.focusedFrame != null && this.focusStartTime > 0 && System.currentTimeMillis() - this.focusStartTime >= FOCUS_DURATION) {
            clearFocus();
        }
        this.dimAnimation.animate(weightedEngine);
        if (this.focusedFrame != null && this.focusStartTime < 0
                && this.dimAnimation.destination() == 0.0f
                && this.dimAnimation.isAtDestination()) {
            this.focusedFrame = null;
        }
    }

    public float getDimmingForFrame(AbstractFrame frame) {
        if (this.focusedFrame == null || frame == this.focusedFrame) {
            return 0.0f;
        }
        return this.dimAnimation.animatedValue();
    }

    public boolean isDimming() {
        return this.dimAnimation.animatedValue() > 0.01f;
    }

    public AbstractFrame focusedFrame() {
        return this.focusedFrame;
    }
}
