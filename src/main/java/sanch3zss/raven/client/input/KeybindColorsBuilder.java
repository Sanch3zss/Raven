package sanch3zss.raven.client.input;

import sanch3zss.raven.render.theme.KeybindColors;

public class KeybindColorsBuilder {
    private int outlineColor;
    private int backgroundColor;
    private int textColor;
    private int textEmptyColor;

    public KeybindColorsBuilder outline(int i) {
        this.outlineColor = i;
        return this;
    }

    public KeybindColorsBuilder background(int i) {
        this.backgroundColor = i;
        return this;
    }

    public KeybindColorsBuilder text(int i) {
        this.textColor = i;
        return this;
    }

    public KeybindColorsBuilder textEmpty(int i) {
        this.textEmptyColor = i;
        return this;
    }

    public KeybindColors build() {
        return new KeybindColors(this.outlineColor, this.backgroundColor, this.textColor, this.textEmptyColor);
    }
}
