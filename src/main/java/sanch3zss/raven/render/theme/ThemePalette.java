package sanch3zss.raven.render.theme;

import sanch3zss.raven.render.theme.ColorMode;
import sanch3zss.raven.render.theme.ColorUtil;

public final class ThemePalette {
    public final ColorValue accent;
    public final ColorValue accentBright;
    public final ColorValue favorite;
    public final ColorValue frameBackground;

    public final ColorToneScale text;

    public final ColorToneScale error;

    public final ColorToneScale surfaceOutline;

    public final ColorToneScale surfaceBackground;
    public final ColorMode colorMode;
    public static ColorValue white = ColorValue.fromHex("FFFFFF");
    public static ColorValue golden = ColorValue.fromHex("FDC95A");
    public static ColorValue darkGolden = ColorValue.fromHex("E9B444");
    public static ColorValue darkOrange = ColorValue.fromHex("DA944F");
    public static ColorValue deepGreen = ColorValue.fromHex("4CA765");
    public static ColorValue darkGreen = ColorValue.fromHex("49A153");
    public static ColorValue red = ColorValue.fromHex("FF4C4F");
    public static ColorValue darkRed = ColorValue.fromHex("C83D3F");
    public static ColorValue violet = ColorValue.fromHex("7267FF");
    public static ColorValue lightOrange = ColorValue.fromHex("E87421");
    public static ColorValue blue = ColorValue.fromHex("5E84FF");
    public static ColorValue mistyBlue = ColorValue.fromHex("7986CB");
    public static ColorValue mint = ColorValue.fromHex("81C784");
    public static ColorValue grey = ColorValue.fromHex("868791");

    public ThemePalette(ColorValue colorValue4, ColorValue colorValue3, ColorValue colorValue2, ColorValue colorValue, ColorToneScale toneScale4, ColorToneScale toneScale3, ColorToneScale toneScale2, ColorToneScale toneScale) {
        this(colorValue4, colorValue3, colorValue2, colorValue, toneScale4, toneScale3,
                toneScale2, toneScale, ColorMode.GRADIENT);
    }

    public ThemePalette(ColorValue colorValue4, ColorValue colorValue3, ColorValue colorValue2, ColorValue colorValue, ColorToneScale toneScale4, ColorToneScale toneScale3, ColorToneScale toneScale2, ColorToneScale toneScale, ColorMode colorMode) {
        this.accent = colorValue4;
        this.accentBright = colorValue3;
        this.favorite = colorValue2;
        this.frameBackground = colorValue;
        this.text = toneScale4;
        this.error = toneScale3;
        this.surfaceOutline = toneScale2;
        this.surfaceBackground = toneScale;
        this.colorMode = colorMode;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "accent=" + this.accent + ", " + "accentBright=" + this.accentBright + ", " + "favorite=" + this.favorite + ", " + "frameBackground=" + this.frameBackground + ", " + "text=" + this.text + ", " + "error=" + this.error + ", " + "surfaceOutline=" + this.surfaceOutline + ", " + "surfaceBackground=" + this.surfaceBackground + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.accent, this.accentBright, this.favorite, this.frameBackground, this.text, this.error, this.surfaceOutline, this.surfaceBackground, this.colorMode);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ThemePalette)) return false;
        ThemePalette o = (ThemePalette) obj;
        return java.util.Objects.equals(this.accent, o.accent) && java.util.Objects.equals(this.accentBright, o.accentBright) && java.util.Objects.equals(this.favorite, o.favorite) && java.util.Objects.equals(this.frameBackground, o.frameBackground) && java.util.Objects.equals(this.text, o.text) && java.util.Objects.equals(this.error, o.error) && java.util.Objects.equals(this.surfaceOutline, o.surfaceOutline) && java.util.Objects.equals(this.surfaceBackground, o.surfaceBackground) && this.colorMode == o.colorMode;
    }
public ColorValue accent() {
        return this.colorMode == ColorMode.GRADIENT
                ? new ColorValue(gradientAccent(0.0f)) : this.accent;
    }

    public int gradientAccent(float offset) {
        return this.colorMode == ColorMode.GRADIENT
                ? ColorUtil.gradientWave(this.accent.argb(), this.accentBright.argb(), offset)
                : this.accent.argb();
    }

    public ColorValue gradientEnd() {
        return this.colorMode == ColorMode.GRADIENT ? this.accentBright : this.accent;
    }

    public ThemePalette withColorMode(ColorMode mode) {
        return new ThemePalette(this.accent, this.accentBright, this.favorite, this.frameBackground,
                this.text, this.error, this.surfaceOutline, this.surfaceBackground, mode);
    }

    public ColorValue accentStart() {
        return this.accent;
    }

    public ColorValue accentBright() {
        return this.accentBright;
    }

    public ColorValue favorite() {
        return this.favorite;
    }

    public ColorValue frameBackground() {
        return this.frameBackground;
    }

    public ColorToneScale text() {
        return this.text;
    }

    public ColorToneScale error() {
        return this.error;
    }

    public ColorToneScale surfaceOutline() {
        return this.surfaceOutline;
    }

    public ColorToneScale surfaceBackground() {
        return this.surfaceBackground;
    }
}
