package sanch3zss.raven.feature.module.render.enchantcolor;

import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;

public enum GlintStyle implements DisplayNamed {
    VANILLA("Обычный"),
    PIXEL("Пиксельный"),
    SHADER("Шейдерный");

    private final Translation displayName;

    GlintStyle(String name) {
        this.displayName = Translation.clearText(name);
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }
}
