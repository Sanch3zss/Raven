package sanch3zss.raven.util.math.interpolation;

import sanch3zss.raven.util.math.MathUtil;

public final class Easings {
    public static final EasingFunction LINEAR = f -> {
        return f;
    };
    public static final EasingFunction EASE_IN_QUAD = f -> {
        return f * f;
    };
    public static final EasingFunction EASE_OUT_QUAD = f -> {
        return 1.0f - ((1.0f - f) * (1.0f - f));
    };
    public static final EasingFunction EASE_IN_OUT_QUAD = f -> {
        return f < 0.5f ? 2.0f * f * f : 1.0f - (MathUtil.fastPow(((-2.0f) * f) + 2.0f, 2.0f) / 2.0f);
    };
    public static final EasingFunction EASE_IN_CUBIC = f -> {
        return f * f * f;
    };
    public static final EasingFunction EASE_OUT_CUBIC = f -> {
        return 1.0f - MathUtil.fastPow(1.0f - f, 3.0f);
    };
    public static final EasingFunction EASE_IN_OUT_CUBIC = f -> {
        return f < 0.5f ? 4.0f * f * f * f : 1.0f - (MathUtil.fastPow(((-2.0f) * f) + 2.0f, 3.0f) / 2.0f);
    };
    public static final EasingFunction EASE_IN_QUART = f -> {
        return f * f * f * f;
    };
    public static final EasingFunction EASE_OUT_QUART = f -> {
        return 1.0f - MathUtil.fastPow(1.0f - f, 4.0f);
    };
    public static final EasingFunction EASE_IN_OUT_QUART = f -> {
        return f < 0.5f ? 8.0f * MathUtil.fastPow(f, 4.0f) : 1.0f - (MathUtil.fastPow(((-2.0f) * f) + 2.0f, 4.0f) / 2.0f);
    };
    public static final EasingFunction EASE_IN_QUINT = f -> {
        return MathUtil.fastPow(f, 5.0f);
    };
    public static final EasingFunction EASE_OUT_QUINT = f -> {
        return 1.0f - MathUtil.fastPow(1.0f - f, 5.0f);
    };
    public static final EasingFunction EASE_IN_OUT_QUINT = f -> {
        return f < 0.5f ? 16.0f * MathUtil.fastPow(f, 5.0f) : 1.0f - (MathUtil.fastPow(((-2.0f) * f) + 2.0f, 5.0f) / 2.0f);
    };
    public static final EasingFunction CIRC_OUT = cubicBezier(0.0f, 0.55f, 0.45f, 1.0f);

    public static EasingFunction cubicBezier(float x1, float y1, float x2, float y2) {
        return progress -> {
            if (progress <= 0.0f || progress >= 1.0f) {
                return progress;
            }
            float parameter = progress;
            for (int i = 0; i < 8; i++) {
                float value = bezier(parameter, x1, x2) - progress;
                if (Math.abs(value) < 1.0E-5f) {
                    break;
                }
                float derivative = bezierDerivative(parameter, x1, x2);
                if (Math.abs(derivative) < 1.0E-6f) {
                    break;
                }
                parameter = MathUtil.clamp(parameter - value / derivative, 0.0f, 1.0f);
            }
            return bezier(parameter, y1, y2);
        };
    }

    private static float bezier(float value, float first, float second) {
        float inverse = 1.0f - value;
        return 3.0f * inverse * inverse * value * first
                + 3.0f * inverse * value * value * second + value * value * value;
    }

    private static float bezierDerivative(float value, float first, float second) {
        float inverse = 1.0f - value;
        return 3.0f * inverse * inverse * first
                + 6.0f * inverse * value * (second - first)
                + 3.0f * value * value * (1.0f - second);
    }

    public Easings() {
    }
}
