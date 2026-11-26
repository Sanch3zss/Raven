package sanch3zss.raven.feature.module.render.armtweaks.knife;

import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;

public enum CsAgent implements DisplayNamed {
    DEFAULT("Стандартные руки", null),
    CTM_SAS("Стандартный спецназ", "ctm_sas"),
    CTM_ST6_VARIANTM("«Дважды» Маккой | USAF TACP", "ctm_st6_variantm"),
    CTM_ST6_VARIANTL("«Дважды» Маккой | Кавалерия TACP", "ctm_st6_variantl"),
    CTM_ST6_VARIANTJ("«Черничный» Бакшот | NSWC SEAL", "ctm_st6_variantj"),
    CTM_ST6_VARIANTG("Бакшот | NSWC SEAL", "ctm_st6_variantg"),
    CTM_GENDARMERIE_VARIANTA("Второй лейтенант медслужбы | Национальная жандармерия", "ctm_gendarmerie_varianta"),
    CTM_SWAT_VARIANTG("Джон «Ван Лечитт» Каск | SWAT", "ctm_swat_variantg"),
    CTM_GENDARMERIE_VARIANTD("Кадет | Национальная жандармерия", "ctm_gendarmerie_variantd"),
    CTM_ST6_VARIANTI("Капитан 3-го ранга Риксоу | NSWC SEAL", "ctm_st6_varianti"),
    CTM_DIVER_VARIANTA("Капитан Давида «Окуляры» Фернандес | Боевые пловцы SEAL", "ctm_diver_varianta"),
    CTM_DIVER_VARIANTB("Капитан Фрэнк «Мокрые носки» Бару | Боевые пловцы SEAL", "ctm_diver_variantb"),
    CTM_GENDARMERIE_VARIANTB("Капитан химзащиты | Национальная жандармерия", "ctm_gendarmerie_variantb"),
    CTM_GENDARMERIE_VARIANTC("Коммандант Рушар | Национальная жандармерия", "ctm_gendarmerie_variantc"),
    CTM_SWAT_VARIANTE("Коммандер Мэй «Льдина» Джемисон | SWAT", "ctm_swat_variante"),
    CTM_SWAT_VARIANTK("Лейтенант «Лесник» Фарлоу | SWAT", "ctm_swat_variantk"),
    CTM_DIVER_VARIANTC("Лейтенант Рекс Крайки | Боевые пловцы SEAL", "ctm_diver_variantc"),
    CTM_FBI_VARIANTH("Майкл Сайферс | ФБР: снайпер", "ctm_fbi_varianth"),
    CTM_FBI_VARIANTG("Маркус Делроу | ФБР: антитеррор", "ctm_fbi_variantg"),
    CTM_FBI_VARIANTF("Оперативник | ФБР: SWAT", "ctm_fbi_variantf"),
    CTM_FBI_VARIANTB("Особый агент Ава | ФБР", "ctm_fbi_variantb"),
    CTM_GENDARMERIE_VARIANTE("Офицер Жак Белтрам | Национальная жандармерия", "ctm_gendarmerie_variante"),
    CTM_SAS_VARIANTF("Офицер отряда B | SAS", "ctm_sas_variantf"),
    CTM_SAS_VARIANTG("Офицер отряда D | NZSAS", "ctm_sas_variantg"),
    CTM_SWAT_VARIANTI("Сержант Бомбсон | SWAT", "ctm_swat_varianti"),
    CTM_ST6_VARIANTE("Солдат SEAL Team 6 | NSWC SEAL", "ctm_st6_variante"),
    CTM_SWAT_VARIANTH("Специалист по биозащите | SWAT", "ctm_swat_varianth"),
    CTM_SWAT_VARIANTJ("Специалист по химзащите | SWAT", "ctm_swat_variantj"),
    CTM_ST6_VARIANTN("Старший лейтенант | Первый батальон Бразилии", "ctm_st6_variantn"),
    CTM_SWAT_VARIANTF("Старший лейтенант Фарлоу | SWAT", "ctm_swat_variantf"),
    CTM_ST6_VARIANTK("Третья рота коммандо | KSK", "ctm_st6_variantk"),
    TM_PHOENIX("Стандартный террорист", "tm_phoenix"),
    TM_BALKAN_VARIANTH("«Доктор» Романов | Сабля", "tm_balkan_varianth"),
    TM_PROFESSIONAL_VARJ("Беглянка Салли | Профессионалы", "tm_professional_varj"),
    TM_JUNGLE_RAIDER_VARIANTE("Вайпа, сестра революции | Партизаны", "tm_jungle_raider_variante"),
    TM_PROFESSIONAL_VARG("Взломщица Вольцманн | Профессионалы", "tm_professional_varg"),
    TM_PHOENIX_VARIANTF("Головорез | Феникс", "tm_phoenix_variantf"),
    TM_LEET_VARIANTG("Диверсант | Элитный отряд", "tm_leet_variantg"),
    TM_BALKAN_VARIANTL("Драгомир | Пехота «Сабли»", "tm_balkan_variantl"),
    TM_BALKAN_VARIANTF("Драгомир | Сабля", "tm_balkan_variantf"),
    TM_PROFESSIONAL_VARH("Крошка Кев | Профессионалы", "tm_professional_varh"),
    TM_JUNGLE_RAIDER_VARIANTB2("Крэссуотер «Поджарый» | Партизаны", "tm_jungle_raider_variantb2"),
    TM_JUNGLE_RAIDER_VARIANTB("Крэссуотер Забытый | Партизаны", "tm_jungle_raider_variantb"),
    TM_BALKAN_VARIANTI("Максимус | Сабля", "tm_balkan_varianti"),
    TM_PHOENIX_VARIANTG("Мясник | Феникс", "tm_phoenix_variantg"),
    TM_PROFESSIONAL_VARI("Номер К | Профессионалы", "tm_professional_vari"),
    TM_LEET_VARIANTH("Осирис | Элитный отряд", "tm_leet_varianth"),
    TM_JUNGLE_RAIDER_VARIANTF2("Охотник | Партизаны", "tm_jungle_raider_variantf2"),
    TM_JUNGLE_RAIDER_VARIANTF("Охотник-агрессор | Партизаны", "tm_jungle_raider_variantf"),
    TM_JUNGLE_RAIDER_VARIANTC("Переросток Арно | Партизаны", "tm_jungle_raider_variantc"),
    TM_JUNGLE_RAIDER_VARIANTD("Полковник Мангос Дабиси | Партизаны", "tm_jungle_raider_variantd"),
    TM_LEET_VARIANTI("Профессор Шахмат | Элитный отряд", "tm_leet_varianti"),
    TM_BALKAN_VARIANTG("Резан Готовый | Сабля", "tm_balkan_variantg"),
    TM_BALKAN_VARIANTK("Резан Красный | Сабля", "tm_balkan_variantk"),
    TM_PHOENIX_VARIANTH("Солдат | Феникс", "tm_phoenix_varianth"),
    TM_PROFESSIONAL_VARF("Сэр «Чёртов Майами» Дэррил | Профессионалы", "tm_professional_varf"),
    TM_PROFESSIONAL_VARF4("Сэр «Чёртов болтун» Дэррил | Профессионалы", "tm_professional_varf4"),
    TM_PROFESSIONAL_VARF1("Сэр «Чёртов тихоня» Дэррил | Профессионалы", "tm_professional_varf1"),
    TM_PROFESSIONAL_VARF2("Сэр «Чёртов череп» Дэррил | Профессионалы", "tm_professional_varf2"),
    TM_PROFESSIONAL_VARF3("Сэр «Чёртова знать» Дэррил | Профессионалы", "tm_professional_varf3"),
    TM_PROFESSIONAL_VARF5("Сэр «Чёртовы ремни» Дэррил | Профессионалы", "tm_professional_varf5"),
    TM_LEET_VARIANTJ("Тропический диверсант | Элитный отряд", "tm_leet_variantj"),
    TM_PHOENIX_VARIANTI("Уличный солдат | Феникс", "tm_phoenix_varianti"),
    TM_BALKAN_VARIANTJ("Черноволк | Сабля", "tm_balkan_variantj"),
    TM_LEET_VARIANTF("Элитный мистер Мохлик | Элитный отряд", "tm_leet_variantf"),
    TM_JUNGLE_RAIDER_VARIANTA("Элитный охотник Солман | Партизаны", "tm_jungle_raider_varianta");

    public final String model;
    private final Translation displayName;

    CsAgent(String name, String model) {
        this.displayName = Translation.clearText(name);
        this.model = model;
    }

    @Override
    public Translation getDisplayName() {
        return displayName;
    }
}
