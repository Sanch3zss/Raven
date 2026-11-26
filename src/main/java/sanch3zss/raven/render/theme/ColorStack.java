package sanch3zss.raven.render.theme;

import java.util.concurrent.ThreadLocalRandom;

import sanch3zss.raven.render.gl.StencilBufferUtil;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.util.math.MathUtil;

public class ColorStack {
    public static final int stackCapacity = 128;
    public final float[] alphaStack = new float[stackCapacity];

    public int stackPointer;

    public ColorStack() {
        float[] fArr = this.alphaStack;
        this.stackPointer = 0;
        fArr[0] = 1.0f;
    }

    public void begin() {
        float[] fArr = this.alphaStack;
        this.stackPointer = 0;
        fArr[0] = 1.0f;
    }

    public void end() {
        if (this.stackPointer > 0) {
            overflow();
        }
    }

    public void push() {
        int i = this.stackPointer + 1;
        if (i >= stackCapacity) {
            overflow();
        }
        this.alphaStack[i] = this.alphaStack[this.stackPointer];
        this.stackPointer = i;
    }

    public void pop() {
        if (this.stackPointer == 0) {
            underflow();
        }
        this.stackPointer--;
    }

    public void alpha(float f) {
        float[] fArr = this.alphaStack;
        int i = this.stackPointer;
        fArr[i] = fArr[i] * f;
    }

    public void alpha(int i) {
        alpha(i / 255.0f);
    }

    public void alphaAnimation(ToggleAnimator animator3) {
        alpha(animator3.smoothAnimation());
    }

    public float alphaMultiplier() {
        return this.alphaStack[this.stackPointer];
    }

    public int brightenedInterpolatedColor(int i, float f, ToggleAnimator animator3, ToggleAnimator animator2) {
        return interpolate(i, brighten(i, f), animator3, animator2);
    }

    public int darkenedInterpolatedColor(int i, float f, ToggleAnimator animator3, ToggleAnimator animator2) {
        return interpolate(i, darken(i, f), animator3, animator2);
    }

    public int darkenedInterpolatedColor(int i, float f, ToggleAnimator animator3) {
        return interpolate(i, autoBrightenDarken(i, f), animator3);
    }

    public int brightenedInterpolatedColor(int i, float f, ToggleAnimator animator3) {
        return interpolate(i, brighten(i, f), animator3);
    }

    public int interpolate(int i, int i2, ToggleAnimator animator3) {
        float fSmoothAnimation = animator3.smoothAnimation();
        int i3 = (i >> 16) & StencilBufferUtil.STENCIL_MASK;
        int i4 = (i >> 8) & StencilBufferUtil.STENCIL_MASK;
        int i5 = i & StencilBufferUtil.STENCIL_MASK;
        int i6 = (i >> 24) & StencilBufferUtil.STENCIL_MASK;
        int i7 = (i2 >> 16) & StencilBufferUtil.STENCIL_MASK;
        int i8 = (i2 >> 8) & StencilBufferUtil.STENCIL_MASK;
        int i9 = i2 & StencilBufferUtil.STENCIL_MASK;
        int i10 = (i2 >> 24) & StencilBufferUtil.STENCIL_MASK;
        return computeColor(MathUtil.interpolate(i6, i10, fSmoothAnimation), MathUtil.interpolate(i3, i7, fSmoothAnimation), MathUtil.interpolate(i4, i8, fSmoothAnimation), MathUtil.interpolate(i5, i9, fSmoothAnimation));
    }

    public int interpolate(int i, int i2, float f) {
        int i3 = (i >> 16) & StencilBufferUtil.STENCIL_MASK;
        int i4 = (i >> 8) & StencilBufferUtil.STENCIL_MASK;
        int i5 = i & StencilBufferUtil.STENCIL_MASK;
        int i6 = (i >> 24) & StencilBufferUtil.STENCIL_MASK;
        int i7 = (i2 >> 16) & StencilBufferUtil.STENCIL_MASK;
        int i8 = (i2 >> 8) & StencilBufferUtil.STENCIL_MASK;
        int i9 = i2 & StencilBufferUtil.STENCIL_MASK;
        int i10 = (i2 >> 24) & StencilBufferUtil.STENCIL_MASK;
        return computeColor(MathUtil.interpolate(i6, i10, f), MathUtil.interpolate(i3, i7, f), MathUtil.interpolate(i4, i8, f), MathUtil.interpolate(i5, i9, f));
    }

    public HorizontalGradient horizontalGradient(int startColor, int endTopColor, int endBottomColor,
                                                 float transitionStart, float transitionEnd) {
        float start = MathUtil.clamp(transitionStart, 0.0f, 1.0f);
        float end = MathUtil.clamp(Math.max(transitionEnd, start + 0.001f), 0.0f, 1.0f);
        return new HorizontalGradient(computeColor(startColor), computeColor(endTopColor),
                computeColor(endBottomColor), start, end);
    }

    public record HorizontalGradient(int startColor, int endTopColor, int endBottomColor,
                                     float transitionStart, float transitionEnd) {
    }

    public int interpolate(int i, int i2, ToggleAnimator animator3, ToggleAnimator animator2) {
        float fSmoothAnimation = (animator3.smoothAnimation() + animator2.smoothAnimation()) / 2.0f;
        int i3 = (i >> 16) & StencilBufferUtil.STENCIL_MASK;
        int i4 = (i >> 8) & StencilBufferUtil.STENCIL_MASK;
        int i5 = i & StencilBufferUtil.STENCIL_MASK;
        int i6 = (i >> 24) & StencilBufferUtil.STENCIL_MASK;
        int i7 = (i2 >> 16) & StencilBufferUtil.STENCIL_MASK;
        int i8 = (i2 >> 8) & StencilBufferUtil.STENCIL_MASK;
        int i9 = i2 & StencilBufferUtil.STENCIL_MASK;
        int i10 = (i2 >> 24) & StencilBufferUtil.STENCIL_MASK;
        return computeColor(MathUtil.interpolate(i6, i10, fSmoothAnimation), MathUtil.interpolate(i3, i7, fSmoothAnimation), MathUtil.interpolate(i4, i8, fSmoothAnimation), MathUtil.interpolate(i5, i9, fSmoothAnimation));
    }

    public int computeColor(int i, int i2, int i3, int i4) {
        return argb(adjustAlpha(i), i2, i3, i4);
    }

    public int computeColor(float f, int i, int i2, int i3) {
        return argb((int) (MathUtil.clamp(f, 0.0f, 1.0f) * alphaMultiplier() * 255.0f), i, i2, i3);
    }

    public int computeColor(int i, int i2, int i3) {
        return computeColor(StencilBufferUtil.STENCIL_MASK, i, i2, i3);
    }

    public int computeColor(int i, int i2) {
        return argb((int) MathUtil.clamp(i2 * alphaMultiplier(), 0.0f, 255.0f), (i >> 16) & StencilBufferUtil.STENCIL_MASK, (i >> 8) & StencilBufferUtil.STENCIL_MASK, i & StencilBufferUtil.STENCIL_MASK);
    }

    public int computeColor(int i, float f) {
        return argb((int) (MathUtil.clamp(f, 0.0f, 1.0f) * alphaMultiplier() * 255.0f), (i >> 16) & StencilBufferUtil.STENCIL_MASK, (i >> 8) & StencilBufferUtil.STENCIL_MASK, i & StencilBufferUtil.STENCIL_MASK);
    }

    public int toggleInterpolateColor(int i, int i2, float f, ToggleAnimator animator3, ToggleAnimator animator2, ToggleAnimator animator) {
        return interpolate(brightenedInterpolatedColor(i, f, animator3, animator2), brightenedInterpolatedColor(i2, f, animator3, animator2), animator);
    }

    public int toggleDarkenedInterpolateColor(int i, int i2, float f, ToggleAnimator animator3, ToggleAnimator animator2, ToggleAnimator animator) {
        return interpolate(brightenedInterpolatedColor(i, f, animator3, animator2), darkenedInterpolatedColor(i2, f * 3.0f, animator3, animator2), animator);
    }

    public int toggleDarkenedInterpolateColor(int i, int i2, float f, ToggleAnimator animator3, ToggleAnimator animator2) {
        return interpolate(brightenedInterpolatedColor(i, f, animator3), darkenedInterpolatedColor(i2, f, animator3), animator2);
    }

    public int toggleInterpolateColor(int i, int i2, float f, ToggleAnimator animator3, ToggleAnimator animator2) {
        return interpolate(brightenedInterpolatedColor(i, f, animator3), darkenedInterpolatedColor(i2, f, animator3), animator2);
    }

    public int computeColor(int i) {
        return computeColor(i, StencilBufferUtil.STENCIL_MASK);
    }

    public int brighten(int i, float f) {
        int i2 = (i >> 24) & StencilBufferUtil.STENCIL_MASK;
        int i3 = (i >> 16) & StencilBufferUtil.STENCIL_MASK;
        int i4 = (i >> 8) & StencilBufferUtil.STENCIL_MASK;
        int i5 = i & StencilBufferUtil.STENCIL_MASK;
        return computeColor(i2, Math.min((int) (i3 + ((StencilBufferUtil.STENCIL_MASK - i3) * f)), StencilBufferUtil.STENCIL_MASK), Math.min((int) (i4 + ((StencilBufferUtil.STENCIL_MASK - i4) * f)), StencilBufferUtil.STENCIL_MASK), Math.min((int) (i5 + ((StencilBufferUtil.STENCIL_MASK - i5) * f)), StencilBufferUtil.STENCIL_MASK));
    }

    public int autoBrightenDarken(int i, float f) {
        return (((0.2126f * ((float) ((i >> 16) & StencilBufferUtil.STENCIL_MASK))) + (0.7152f * ((float) ((i >> 8) & StencilBufferUtil.STENCIL_MASK)))) + (0.0722f * ((float) (i & StencilBufferUtil.STENCIL_MASK)))) / 255.0f > 0.4f ? darken(i, f) : brighten(i, f);
    }

    public int darken(int i, float f) {
        return computeColor((i >> 24) & StencilBufferUtil.STENCIL_MASK, Math.max((int) (((i >> 16) & StencilBufferUtil.STENCIL_MASK) * (1.0f - f)), 0), Math.max((int) (((i >> 8) & StencilBufferUtil.STENCIL_MASK) * (1.0f - f)), 0), Math.max((int) ((i & StencilBufferUtil.STENCIL_MASK) * (1.0f - f)), 0));
    }

    public int transparent() {
        return computeColor(0, 0, 0, 0);
    }

    public int white() {
        return computeColor(16777215);
    }

    public int black() {
        return computeColor(0);
    }

    public int darkAlpha(int i) {
        return adjustAlpha(i) << 24;
    }

    public int adjustAlpha(int i) {
        return MathUtil.clamp(Math.round(i * this.alphaStack[this.stackPointer]), 0, StencilBufferUtil.STENCIL_MASK);
    }

    public int randomColor() {
        return computeColor(StencilBufferUtil.STENCIL_MASK, ThreadLocalRandom.current().nextInt(0, StencilBufferUtil.STENCIL_MASK), ThreadLocalRandom.current().nextInt(0, StencilBufferUtil.STENCIL_MASK), ThreadLocalRandom.current().nextInt(0, StencilBufferUtil.STENCIL_MASK));
    }

    public int getHSBColor(float f, float f2, float f3) {
        return getHSBColor(f, f2, f3, 1.0f);
    }

    public int getHSBColor(float f, float f2, float f3, float f4) {
        return computeColor(HSBtoRGB(f, f2, f3), f4);
    }

    public int HSBtoRGB(float f, float f2, float f3) {
        float fFloor = f - ((float) Math.floor(f));
        float f4 = 0.0f;
        float f5 = 0.0f;
        float f6 = 0.0f;
        if (f2 != 0.0f) {
            float fFloor2 = (fFloor - ((float) Math.floor(fFloor))) * 6.0f;
            int i = (int) fFloor2;
            float f7 = fFloor2 - i;
            float f8 = f3 * (1.0f - f2);
            float f9 = f3 * (1.0f - (f2 * f7));
            float f10 = f3 * (1.0f - (f2 * (1.0f - f7)));
            switch (i % 6) {
                case 0:
                    f4 = f3;
                    f5 = f10;
                    f6 = f8;
                    break;
                case 1:
                    f4 = f9;
                    f5 = f3;
                    f6 = f8;
                    break;
                case 2:
                    f4 = f8;
                    f5 = f3;
                    f6 = f10;
                    break;
                case 3:
                    f4 = f8;
                    f5 = f9;
                    f6 = f3;
                    break;
                case 4:
                    f4 = f10;
                    f5 = f8;
                    f6 = f3;
                    break;
                case 5:
                    f4 = f3;
                    f5 = f8;
                    f6 = f9;
                    break;
            }
        } else {
            f6 = f3;
            f5 = f3;
            f4 = f3;
        }
        return argb(adjustAlpha(StencilBufferUtil.STENCIL_MASK), MathUtil.clamp((int) (f4 * 255.0f), 0, StencilBufferUtil.STENCIL_MASK), MathUtil.clamp((int) (f5 * 255.0f), 0, StencilBufferUtil.STENCIL_MASK), MathUtil.clamp((int) (f6 * 255.0f), 0, StencilBufferUtil.STENCIL_MASK));
    }

    public int argb(int i, int i2, int i3, int i4) {
        return (i << 24) | (i2 << 16) | (i3 << 8) | i4;
    }

    public void overflow() {
        throw new IllegalStateException("Stack overflow");
    }

    public void underflow() {
        throw new IllegalStateException("Stack underflow");
    }
}
