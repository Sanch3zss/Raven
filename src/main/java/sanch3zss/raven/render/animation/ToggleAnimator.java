package sanch3zss.raven.render.animation;

import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.*;

public class ToggleAnimator {
    public static final int defaultDuration = 220;
    public final EasingFunction easeIn;
    public final EasingFunction easeOut;
    public final float speedIn;
    public final float speedOut;

    public boolean state;
    public float animation;
    public float smoothAnimation;
    public float maxValueScale;

    public ToggleAnimator(EasingFunction easingFunction2) {
        this(defaultDuration, easingFunction2, easingFunction2);
    }

    public ToggleAnimator(int i, EasingFunction easingFunction2) {
        this(i, easingFunction2, easingFunction2);
    }

    public ToggleAnimator(int i, EasingFunction easingFunction2, EasingFunction easingFunction) {
        this(i, i, easingFunction2, easingFunction);
    }

    public ToggleAnimator(int inDuration, int outDuration, EasingFunction easeIn, EasingFunction easeOut) {
        this.maxValueScale = 1.0f;
        this.easeIn = easeIn;
        this.easeOut = easeOut;
        this.speedIn = 1000.0f / inDuration;
        this.speedOut = 1000.0f / outDuration;
    }

    public void animate(WeightedEngine weightedEngine) {
        float fWeight = weightedEngine.weight() * (this.state ? this.speedIn : this.speedOut);
        float fAnimationMultiplier = weightedEngine.engine().animationMultiplier();
        this.animation = MathUtil.clamp(this.animation + (this.state ? fWeight : -fWeight), 0.0f, 1.0f);
        float fEase = (this.state ? this.easeIn : this.easeOut).ease(this.state ? this.animation : 1.0f - this.animation);
        this.smoothAnimation = (this.state ? fEase : 1.0f - fEase) * fAnimationMultiplier * this.maxValueScale;
    }

    public void fill() {
        this.animation = 1.0f;
    }

    public void force(boolean z) {
        this.state = z;
        this.animation = z ? 1.0f : 0.0f;
        this.smoothAnimation = (z ? 1.0f : 0.0f) * this.maxValueScale;
    }

    public void invert() {
        this.state = !this.state;
    }

    public float interpolate(float f, float f2) {
        return f + ((f2 - f) * this.smoothAnimation);
    }

    public boolean finished() {
        return isZero() || isOne();
    }

    public boolean isZero() {
        return this.animation == 0.0f;
    }

    public boolean isOne() {
        return this.animation == 1.0f;
    }

    public static ToggleAnimator times(int i) {
        return times(i, Easings.EASE_IN_OUT_CUBIC);
    }

    public static ToggleAnimator times(int i, int i2) {
        return times(i, i2, Easings.EASE_IN_OUT_CUBIC);
    }

    public static ToggleAnimator times(int i, EasingFunction easingFunction2) {
        return new ToggleAnimator((defaultDuration / i) * i, easingFunction2).maxValue(i);
    }

    public static ToggleAnimator times(int i, int i2, EasingFunction easingFunction2) {
        return new ToggleAnimator(i2 * i, easingFunction2).maxValue(i);
    }

    public static ToggleAnimator times(int i, int i2, EasingFunction easingFunction2, EasingFunction easingFunction) {
        return new ToggleAnimator(i2 * i, easingFunction2, easingFunction).maxValue(i);
    }

    public ToggleAnimator state(boolean z) {
        this.state = z;
        return this;
    }

    public boolean state() {
        return this.state;
    }

    public ToggleAnimator animation(float f) {
        this.animation = f;
        return this;
    }

    public float animation() {
        return this.animation;
    }

    public float smoothAnimation() {
        return this.smoothAnimation;
    }

    public ToggleAnimator maxValue(float f) {
        this.maxValueScale = f;
        return this;
    }
}
