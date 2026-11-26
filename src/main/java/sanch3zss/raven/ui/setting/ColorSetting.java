package sanch3zss.raven.ui.setting;


import sanch3zss.raven.render.gl.StencilBufferUtil;
import sanch3zss.raven.render.theme.ColorMode;
import sanch3zss.raven.render.theme.ColorUtil;
import java.awt.Color;
import java.util.Map;
import java.util.function.Supplier;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
public class ColorSetting extends Setting {
    public float hue;
    public float saturation;
    public float brightness;
    public float alpha;
    private int secondColor;
    private ColorMode mode = ColorMode.GRADIENT;
    private final BooleanSetting themeSync;
    private boolean themeSyncAvailable = true;
    private boolean gradientAvailable = true;
    private Supplier<Boolean> baseVisibility;

    public ColorSetting(Translation translation2, Translation translation) {
        super(translation2, translation);
        this.hue = 0.0f;
        this.saturation = 1.0f;
        this.brightness = 1.0f;
        this.alpha = 1.0f;
        this.themeSync = new BooleanSetting(Lang.THEME_SYNC)
                .visible(this::baseVisible);
        setVisible(() -> baseVisible() && !this.themeSync.isValue());
        setColor(-1);
        setSecondColor(0xFF9BB7FF);
    }

    public ColorSetting(Translation translation2) {
        this(translation2, null);
    }

    public ColorSetting value(int i) {
        setColor(i);
        float[] hsb = Color.RGBtoHSB(ColorUtil.red(i), ColorUtil.green(i), ColorUtil.blue(i), null);
        int companion = Color.HSBtoRGB((hsb[0] + 0.09f) % 1.0f,
                Math.max(0.35f, Math.min(0.88f, hsb[1] * 0.9f)),
                Math.max(0.72f, hsb[2]));
        setSecondColor((companion & 0xFFFFFF) | (getFirstColor() & 0xFF000000));
        return this;
    }

    public ColorSetting visible(Supplier<Boolean> supplier) {
        this.baseVisibility = supplier;
        return this;
    }

    private boolean baseVisible() {
        return this.baseVisibility == null || Boolean.TRUE.equals(this.baseVisibility.get());
    }

    public int getColor() {
        return getAnimatedGradientColor(0.0f);
    }

    public int getAnimatedGradientColor(float offset) {
        return effectiveMode() == ColorMode.STATIC ? getFirstColor()
                : ColorUtil.gradientWave(getFirstColor(), getSecondColor(), offset);
    }

    public int getFirstColor() {
        return (getColorWithoutAlpha() & 0xFFFFFF) | (Math.round(this.alpha * 255.0f) << 24);
    }

    public int getSecondColor() {
        if (this.themeSync.isValue()) {
            int bright = Raven.INSTANCE.theme().palette().accentBright().argb();
            float themeAlpha = (bright >>> 24) == 0 ? 1.0f : (bright >>> 24) / 255.0f;
            return (bright & 0xFFFFFF) | (Math.round(this.alpha * themeAlpha * 255.0f) << 24);
        }
        return this.secondColor;
    }

    public int getSecondColorWithoutAlpha() {
        return getSecondColor() & 0xFFFFFF;
    }

    public float getSecondAlpha() {
        return ColorUtil.alpha(getSecondColor()) / 255.0f;
    }

    public int getGradientColor(float position) {
        return effectiveMode() == ColorMode.STATIC ? getFirstColor()
                : ColorUtil.interpolateOklab(getFirstColor(), getSecondColor(), position);
    }

    public int getGradientEndColor() {
        return effectiveMode() == ColorMode.STATIC ? getFirstColor() : getSecondColor();
    }

    private ColorMode effectiveMode() {
        return this.themeSync.isValue() ? Raven.INSTANCE.theme().palette().colorMode : this.mode;
    }

    public ColorMode mode() {
        return this.mode;
    }

    public void setMode(ColorMode mode) {
        this.mode = this.gradientAvailable ? mode : ColorMode.STATIC;
    }

    public void setSecondColor(int color) {
        this.secondColor = color;
    }

    public void setSecondAlpha(float value) {
        this.secondColor = (this.secondColor & 0xFFFFFF)
                | (Math.round(Math.max(0.0f, Math.min(1.0f, value)) * 255.0f) << 24);
    }

    public int getRed() {
        return (getColorWithoutAlpha() >> 16) & StencilBufferUtil.STENCIL_MASK;
    }

    public int getGreen() {
        return (getColorWithoutAlpha() >> 8) & StencilBufferUtil.STENCIL_MASK;
    }

    public int getBlue() {
        return getColorWithoutAlpha() & StencilBufferUtil.STENCIL_MASK;
    }

    public int getColorWithoutAlpha() {
        if (this.themeSync.isValue()) {
            return Raven.INSTANCE.theme().palette().accentStart().argb();
        }
        return Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
    }

    public BooleanSetting themeSyncSetting() {
        return this.themeSync;
    }

    public ColorSetting withoutThemeSync() {
        this.themeSync.setValue(false);
        this.themeSyncAvailable = false;
        return this;
    }

    public ColorSetting staticOnly() {
        withoutThemeSync();
        this.gradientAvailable = false;
        this.mode = ColorMode.STATIC;
        return this;
    }

    public boolean isGradientAvailable() {
        return this.gradientAvailable;
    }

    public boolean isThemeSyncAvailable() {
        return this.themeSyncAvailable;
    }

    public ColorSetting setColor(int i) {
        float[] fArrRGBtoHSB = Color.RGBtoHSB(ColorUtil.red(i), ColorUtil.green(i), ColorUtil.blue(i), (float[]) null);
        this.hue = fArrRGBtoHSB[0];
        this.saturation = fArrRGBtoHSB[1];
        this.brightness = fArrRGBtoHSB[2];
        int iAlpha = ColorUtil.alpha(i);
        if (iAlpha == 0 && (i >>> 24) == 0) {
            this.alpha = 1.0f;
        } else {
            this.alpha = iAlpha / 255.0f;
        }
        return this;
    }

    @Override
    public Map<String, Object> toSerializedData() {
        return Map.of("color", Integer.valueOf(getFirstColor()), "secondColor", Integer.valueOf(this.secondColor),
                "mode", this.mode.name(), "themeSync", this.themeSyncAvailable && this.themeSync.isValue());
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("color");
        if (obj instanceof Number) {
            setColor(((Number) obj).intValue());
            Object second = map.get("secondColor");
            setSecondColor(second instanceof Number ? ((Number) second).intValue() : ((Number) obj).intValue());
        }
        Object selectedMode = map.get("mode");
        if (selectedMode instanceof String value) {
            try {
                setMode(ColorMode.valueOf(value));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (map.get("themeSync") instanceof Boolean sync && this.themeSyncAvailable) {
            this.themeSync.setValue(sync);
        }
    }

    public float getHue() {
        return this.hue;
    }

    public float getSaturation() {
        return this.saturation;
    }

    public float getBrightness() {
        return this.brightness;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public void setHue(float f) {
        this.hue = f;
    }

    public void setSaturation(float f) {
        this.saturation = f;
    }

    public void setBrightness(float f) {
        this.brightness = f;
    }

    public void setAlpha(float f) {
        this.alpha = f;
    }
}
