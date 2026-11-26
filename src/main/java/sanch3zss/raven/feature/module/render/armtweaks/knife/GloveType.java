package sanch3zss.raven.feature.module.render.armtweaks.knife;

import java.util.Arrays;
import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;

public enum GloveType implements DisplayNamed {
    DEFAULT("Стандартные", null),
    SPORTY("Спортивные перчатки", "sporty"),
    SLICK("Водительские перчатки", "slick"),
    SPECIALIST("Перчатки спецназа", "specialist"),
    MOTORCYCLE("Мотоциклетные перчатки", "motorcycle"),
    HANDWRAP("Обмотки рук", "handwrap"),
    BLOODHOUND("Перчатки «Бладхаунд»", "bloodhound"),
    HYDRA("Перчатки «Гидра»", "hydra"),
    BROKEN_FANG("Перчатки «Сломанный клык»", "brokenfang");

    final String model;
    private final Translation displayName;

    GloveType(String name, String model) {
        this.displayName = Translation.clearText(name);
        this.model = model;
    }

    public GloveSkin[] skins() {
        return Arrays.stream(GloveSkin.values()).filter(skin -> skin.type == this).toArray(GloveSkin[]::new);
    }

    @Override
    public Translation getDisplayName() {
        return displayName;
    }
}
