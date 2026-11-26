package sanch3zss.raven.render.theme;


import sanch3zss.raven.render.gl.StencilBufferUtil;
import net.minecraft.util.ARGB;
import sanch3zss.raven.util.math.MathUtil;

public final class ColorUtil {
    private static final long GRADIENT_PERIOD_NANOS = 6_000_000_000L;
    static float alphaMultiplier = 1.0f;
    private static volatile long gradientFrameTime = System.nanoTime();

    public static void beginGradientFrame(long nanoTime) {
        gradientFrameTime = nanoTime;
    }

    public static int applyOpacity(int i, float f) {
        return ARGB.color((int) ((alpha(i) * f) / 255.0f), red(i), green(i), blue(i));
    }

    public static int argb(int i, int i2, int i3, int i4) {
        return ARGB.color(i, i2, i3, i4);
    }

    public static int argb(float f, int i, int i2, int i3) {
        return ARGB.color(Math.round(MathUtil.clamp(f, 0.0f, 1.0f) * 255.0f), i, i2, i3);
    }

    public static int red(int i) {
        return (i >> 16) & StencilBufferUtil.STENCIL_MASK;
    }

    public static int green(int i) {
        return (i >> 8) & StencilBufferUtil.STENCIL_MASK;
    }

    public static int blue(int i) {
        return i & StencilBufferUtil.STENCIL_MASK;
    }

    public static int alpha(int i) {
        return (i >> 24) & StencilBufferUtil.STENCIL_MASK;
    }

    public static int interpolateColor(int i, int i2, float f) {
        float f2 = ((i >> 24) & StencilBufferUtil.STENCIL_MASK) / 255.0f;
        float f3 = ((i >> 16) & StencilBufferUtil.STENCIL_MASK) / 255.0f;
        float f4 = ((i >> 8) & StencilBufferUtil.STENCIL_MASK) / 255.0f;
        float f5 = (i & StencilBufferUtil.STENCIL_MASK) / 255.0f;
        return (((int) ((f2 + (f * ((((i2 >> 24) & StencilBufferUtil.STENCIL_MASK) / 255.0f) - f2))) * 255.0f)) << 24) | (((int) ((f3 + (f * ((((i2 >> 16) & StencilBufferUtil.STENCIL_MASK) / 255.0f) - f3))) * 255.0f)) << 16) | (((int) ((f4 + (f * ((((i2 >> 8) & StencilBufferUtil.STENCIL_MASK) / 255.0f) - f4))) * 255.0f)) << 8) | ((int) ((f5 + (f * (((i2 & StencilBufferUtil.STENCIL_MASK) / 255.0f) - f5))) * 255.0f));
    }

    public static int interpolateOklab(int first, int second, float amount) {
        double t = Math.max(0.0, Math.min(1.0, amount));
        if (first == second || t <= 0.0) {
            return first;
        }
        if (t >= 1.0) {
            return second;
        }
        double[] a = toOklab(first);
        double[] b = toOklab(second);
        double l = a[0] + (b[0] - a[0]) * t;
        double x = a[1] + (b[1] - a[1]) * t;
        double y = a[2] + (b[2] - a[2]) * t;
        double ll = Math.pow(l + 0.3963377774 * x + 0.2158037573 * y, 3.0);
        double mm = Math.pow(l - 0.1055613458 * x - 0.0638541728 * y, 3.0);
        double ss = Math.pow(l - 0.0894841775 * x - 1.2914855480 * y, 3.0);
        int red = toSrgb(4.0767416621 * ll - 3.3077115913 * mm + 0.2309699292 * ss);
        int green = toSrgb(-1.2684380046 * ll + 2.6097574011 * mm - 0.3413193965 * ss);
        int blue = toSrgb(-0.0041960863 * ll - 0.7034186147 * mm + 1.7076147010 * ss);
        int alpha = (int) Math.round(ColorUtil.alpha(first) + (ColorUtil.alpha(second) - ColorUtil.alpha(first)) * t);
        return argb(alpha, red, green, blue);
    }

    public static int gradientWave(int first, int second, float offset) {
        double phase = gradientPhaseRadians() + offset * Math.PI * 2.0;
        float amount = 0.5f - 0.5f * (float) Math.cos(phase);
        return interpolateOklab(first, second, amount);
    }

    public static float gradientPhaseRadians() {
        return (float) ((gradientFrameTime % GRADIENT_PERIOD_NANOS)
                / (double) GRADIENT_PERIOD_NANOS * Math.PI * 2.0);
    }

    private static double[] toOklab(int color) {
        double red = toLinear(red(color));
        double green = toLinear(green(color));
        double blue = toLinear(blue(color));
        double l = Math.cbrt(0.4122214708 * red + 0.5363325363 * green + 0.0514459929 * blue);
        double m = Math.cbrt(0.2119034982 * red + 0.6806995451 * green + 0.1073969566 * blue);
        double s = Math.cbrt(0.0883024619 * red + 0.2817188376 * green + 0.6299787005 * blue);
        return new double[] {
                0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s,
                1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s,
                0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        };
    }

    private static double toLinear(int channel) {
        double value = channel / 255.0;
        return value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
    }

    private static int toSrgb(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        double encoded = clamped <= 0.0031308 ? 12.92 * clamped : 1.055 * Math.pow(clamped, 1.0 / 2.4) - 0.055;
        return (int) Math.round(encoded * 255.0);
    }

    public static int replAlpha(int i, int i2) {
        return argb(i2, red(i), green(i), blue(i));
    }

    public static int replAlpha(int i, float f) {
        return argb(f, red(i), green(i), blue(i));
    }

    public static int multBright(int i, float f) {
        return argb(alpha(i), Math.min(StencilBufferUtil.STENCIL_MASK, Math.round(red(i) / f)), Math.min(StencilBufferUtil.STENCIL_MASK, Math.round(green(i) / f)), Math.min(StencilBufferUtil.STENCIL_MASK, Math.round(blue(i) / f)));
    }

    public ColorUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static float getAlphaMultiplier() {
        return alphaMultiplier;
    }
}
