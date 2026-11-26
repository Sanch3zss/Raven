package sanch3zss.raven.ui.menu.header;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.render.theme.ColorMode;
import sanch3zss.raven.ui.setting.ColorSetting;

public final class ThemeColorSetting extends ColorSetting {
    private boolean bound;

    public ThemeColorSetting() {
        super(Translation.clearText("Цвета темы"));
        withoutThemeSync();
        this.bound = true;
    }

    public int firstColor() {
        return Raven.INSTANCE.theme().palette().accentStart().argb();
    }

    @Override
    public int getSecondColor() {
        int color = Raven.INSTANCE.theme().palette().accentBright().argb();
        return (color >>> 24) == 0 ? color | 0xFF000000 : color;
    }

    @Override
    public ColorMode mode() {
        return Raven.INSTANCE.theme().palette().colorMode;
    }

    @Override
    public void setMode(ColorMode mode) {
        if (this.bound) {
            Raven.INSTANCE.updateThemeColorMode(mode);
        }
    }

    @Override
    public void setSecondColor(int color) {
        if (this.bound) {
            Raven.INSTANCE.updateThemeAccentBright(color);
        }
    }

    @Override
    public void setSecondAlpha(float value) {
        if (this.bound) {
            int alpha = Math.round(Math.max(0.01f, Math.min(1.0f, value)) * 255.0f);
            Raven.INSTANCE.updateThemeAccentBright((getSecondColor() & 0xFFFFFF) | (alpha << 24));
        }
    }

    @Override
    public ColorSetting setColor(int color) {
        super.setColor(color);
        if (this.bound) {
            Raven.INSTANCE.updateThemeAccent(color);
        }
        return this;
    }
}
