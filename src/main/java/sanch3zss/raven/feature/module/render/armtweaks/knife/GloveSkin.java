package sanch3zss.raven.feature.module.render.armtweaks.knife;

import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;

public enum GloveSkin implements DisplayNamed {
    SPORTY_LIGHT_BLUE(GloveType.SPORTY, "Сверхпроводник"),
    SPORTY_PURPLE(GloveType.SPORTY, "Ящик Пандоры"),
    SPORTY_HUNTER(GloveType.SPORTY, "Крупная добыча"),
    SPORTY_SLINGSHOT(GloveType.SPORTY, "Мясник"),
    SPORTY_BLUE_PINK(GloveType.SPORTY, "Порок"),
    SPORTY_JAGUAR(GloveType.SPORTY, "Ноктс"),
    SPORTY_POISON_FROG_BLUE_WHITE(GloveType.SPORTY, "Амфибия"),
    SPORTY_POISON_FROG_RED_GREEN(GloveType.SPORTY, "Окисление бронзы"),
    SPORTY_BLACK_WEBBING_YELLOW(GloveType.SPORTY, "Омега"),
    SPORTY_HOUNDSTOOTH_RED(GloveType.SPORTY, "Кровавый шемах"),
    SPORTY_GREEN(GloveType.SPORTY, "Живая изгородь"),
    SPORTY_MILITARY(GloveType.SPORTY, "Засуха"),
    SLICK_SNAKESKIN_YELLOW(GloveType.SLICK, "Гремучая змея"),
    SLICK_JAGUAR_YELLOW(GloveType.SLICK, "Королева ягуаров"),
    SLICK_SNAKESKIN_WHITE(GloveType.SLICK, "Королевский змей"),
    SLICK_PLAID_PURPLE(GloveType.SLICK, "Имперская клетка"),
    SLICK_RED(GloveType.SLICK, "Багряный узор"),
    SLICK_BLACK(GloveType.SLICK, "Лунный узор"),
    SLICK_REZAN(GloveType.SLICK, "Резан Красный"),
    SLICK_JAGUAR_WHITE(GloveType.SLICK, "Снежный барс"),
    SLICK_STITCHED_BLACK_WHITE(GloveType.SLICK, "Чёрный галстук"),
    SLICK_STITCHED_BLACK_ORANGE(GloveType.SLICK, "Обгон"),
    SLICK_STITCHED_GREEN_GREY(GloveType.SLICK, "Гоночный зелёный"),
    SLICK_MILITARY(GloveType.SLICK, "Конвой"),
    SPECIALIST_FADE(GloveType.SPECIALIST, "Градиент"),
    SPECIALIST_MARBLE_FADE(GloveType.SPECIALIST, "Мраморный градиент"),
    SPECIALIST_EMERALD_WEB(GloveType.SPECIALIST, "Изумрудная сеть"),
    SPECIALIST_KIMONO_DIAMONDS_RED(GloveType.SPECIALIST, "Кровавое кимоно"),
    SPECIALIST_WEBS_RED(GloveType.SPECIALIST, "Кровавая паутина"),
    SPECIALIST_TIGER_ORANGE(GloveType.SPECIALIST, "Удар тигра"),
    SPECIALIST_FBI(GloveType.SPECIALIST, "Полевой агент"),
    SPECIALIST_ORANGE_WHITE(GloveType.SPECIALIST, "Основа"),
    SPECIALIST_RICKSAW_CAMO(GloveType.SPECIALIST, "Капитан 3-го ранга"),
    SPECIALIST_WINTERHEX(GloveType.SPECIALIST, "Покоритель гор"),
    SPECIALIST_FOREST_BROWN(GloveType.SPECIALIST, "Дробь"),
    SPECIALIST_DDPAT_GREEN_CAMO(GloveType.SPECIALIST, "Пиксельный камуфляж «Лес»"),
    MOTORCYCLE_SMOKE(GloveType.MOTORCYCLE, "Бросаю дымовую"),
    MOTORCYCLE_CHOCO_BOOM(GloveType.MOTORCYCLE, "БУХ!"),
    MOTORCYCLE_MONO_BOOM(GloveType.MOTORCYCLE, "Бах!"),
    MOTORCYCLE_TRIANGLE_BLUE(GloveType.MOTORCYCLE, "Мятная прохлада"),
    MOTORCYCLE_MINT_TRIANGLE(GloveType.MOTORCYCLE, "Мята"),
    MOTORCYCLE_TRIGRID_BLUE(GloveType.MOTORCYCLE, "Полигон"),
    MOTORCYCLE_YELLOW_CAMO(GloveType.MOTORCYCLE, "Транспорт"),
    MOTORCYCLE_BASIC_BLACK(GloveType.MOTORCYCLE, "Затмение"),
    MOTORCYCLE_CARBONFIBER_RED(GloveType.MOTORCYCLE, "Кровяное давление"),
    MOTORCYCLE_CHECKER_FLAG_BLUE_GREEN(GloveType.MOTORCYCLE, "Финишная линия"),
    MOTORCYCLE_COMMANDO_KSK(GloveType.MOTORCYCLE, "Третья рота коммандо"),
    MOTORCYCLE_BASIC_GREEN_ORANGE(GloveType.MOTORCYCLE, "Черепаха"),
    HANDWRAP_RED_SLAUGHTER(GloveType.HANDWRAP, "Бойня"),
    HANDWRAP_LEATHERY_CAUTION(GloveType.HANDWRAP, "ОСТОРОЖНО!"),
    HANDWRAP_LEATHERY_FABRIC_BLUE_SKULLS(GloveType.HANDWRAP, "Синие черепа"),
    HANDWRAP_LEATHERY_FABRIC_GEOMETRIC_BLUE(GloveType.HANDWRAP, "Оттиск"),
    HANDWRAP_LEATHERY_SNAKESKIN_ORANGE(GloveType.HANDWRAP, "Удав"),
    HANDWRAP_FABRIC_HOUNDSTOOTH_ORANGE(GloveType.HANDWRAP, "Пустынный шемах"),
    HANDWRAP_LEATHERY_FABRIC_GIRAFFE(GloveType.HANDWRAP, "Жираф"),
    HANDWRAP_LEATHERY_DUCTTAPE(GloveType.HANDWRAP, "Скотч"),
    HANDWRAP_LEATHERY_FABRIC_GREEN_CAMO(GloveType.HANDWRAP, "Лес"),
    HANDWRAP_FABRIC_ORANGE_CAMO(GloveType.HANDWRAP, "Пустошь"),
    HANDWRAP_CAMO_GREY(GloveType.HANDWRAP, "Пиксельный камуфляж «Хвоя»"),
    HANDWRAP_LEATHERY(GloveType.HANDWRAP, "Кожа"),
    BLOODHOUND_SNAKESKIN_BRASS(GloveType.BLOODHOUND, "Укус змеи"),
    BLOODHOUND_METALLIC(GloveType.BLOODHOUND, "Бронза"),
    BLOODHOUND_BLACK_SILVER(GloveType.BLOODHOUND, "Зола"),
    BLOODHOUND_GUERRILLA(GloveType.BLOODHOUND, "Диверсант"),
    HYDRA_CASE_HARDENED(GloveType.HYDRA, "Поверхностная закалка"),
    HYDRA_BLACK_GREEN(GloveType.HYDRA, "Изумруд"),
    HYDRA_SNAKESKIN_BRASS(GloveType.HYDRA, "Гремучая змея"),
    HYDRA_GREEN_LEATHER_MESH_BRASS(GloveType.HYDRA, "Мангры"),
    BROKEN_FANG_METALIC_GREEN(GloveType.BROKEN_FANG, "Нефрит"),
    BROKEN_FANG_FLORAL(GloveType.BROKEN_FANG, "Остриё иглы"),
    BROKEN_FANG_SNAKESKIN_BLACK(GloveType.BROKEN_FANG, "Без тормозов"),
    BROKEN_FANG_POISON_FROG_BLACK_YELLOW(GloveType.BROKEN_FANG, "Жёлтые полосы");

    final GloveType type;
    private final Translation displayName;

    GloveSkin(GloveType type, String name) {
        this.type = type;
        this.displayName = Translation.clearText(name);
    }

    @Override
    public Translation getDisplayName() {
        return displayName;
    }
}
