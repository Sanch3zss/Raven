package sanch3zss.raven.feature.module.render.enchantcolor;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.ColorSetting;
import sanch3zss.raven.ui.setting.ModeSetting;

public class CustomEnchantColorModule extends Module {
    public final ModeSetting<GlintStyle> style;
    public final ColorSetting color;

    public CustomEnchantColorModule() {
        super(ModuleTab.VISUALS, "CustomEnchantColor");
        this.style = new ModeSetting<GlintStyle>(Translation.clearText("Вид блеска")).values(GlintStyle.class);
        this.color = new ColorSetting(Translation.clearText("Цвет блеска"))
                .value(0xFFA985FF);
        this.color.setSecondColor(0xFF69D9FF);
        addSettings(this.style, this.color);
    }
}
