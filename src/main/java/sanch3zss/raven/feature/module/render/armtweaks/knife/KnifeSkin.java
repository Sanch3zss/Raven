package sanch3zss.raven.feature.module.render.armtweaks.knife;

import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;

public enum KnifeSkin implements DisplayNamed {
    DEFAULT("Обычный"),
    DOPPLER_P1("Волны (Фаза 1)"),
    DOPPLER_P2("Волны (Фаза 2)"),
    DOPPLER_P3("Волны (Фаза 3)"),
    DOPPLER("Волны (Фаза 4)"),
    DOPPLER_RUBY("Волны (Рубин)"),
    DOPPLER_SAPPHIRE("Волны (Сапфир)"),
    DOPPLER_BLACK_PEARL("Волны (Чёрная жемчужина)"),
    GAMMA_P1("Гамма-волны (Фаза 1)"),
    GAMMA_P2("Гамма-волны (Фаза 2)"),
    GAMMA_P3("Гамма-волны (Фаза 3)"),
    GAMMA_P4("Гамма-волны (Фаза 4)"),
    GAMMA_EMERALD("Гамма-волны (Изумруд)"),
    GRADIENT("Градиент"),
    MARBLE_FADE("Мраморный градиент"),
    TIGER_TOOTH("Зуб тигра"),
    DAMASCUS("Дамасская сталь"),
    SLAUGHTER("Убийство"),
    CASE_HARDENED("Поверхностная закалка"),
    CRIMSON_WEB("Кровавая паутина"),
    LORE("Легенды"),
    AUTOTRONIC("Автотроника"),
    BLACK_LAMINATE("Черный глянец"),
    ULTRAVIOLET("Ультрафиолет"),
    FREEHAND("Ручная роспись"),
    BRIGHT_WATER("Чистая вода"),
    BLUE_STEEL("Вороненая сталь"),
    NIGHT("Ночь"),
    RUST_COAT("Пыльник"),
    BOREAL_FOREST("Северный лес"),
    STAINED("Патина"),
    FOREST_DDPAT("Пиксельный камуфляж «Лес»"),
    SCORCHED("Сажа"),
    SAFARI_MESH("Африканская сетка"),
    NIGHT_STRIPE("Ночная полоса"),
    URBAN_MASKED("Городская маскировка");

    private final Translation displayName;

    KnifeSkin(String name) {
        this.displayName = Translation.clearText(name);
    }

    @Override
    public Translation getDisplayName() {
        return displayName;
    }
}
