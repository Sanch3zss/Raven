package sanch3zss.raven.feature.module.render.chinahat;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.ColorSetting;

public class ChinaHatModule extends Module {
    public final ColorSetting color;

    public ChinaHatModule() {
        super(ModuleTab.VISUALS, "ChinaHat");
        this.color = new ColorSetting(Translation.clearText("Цвет"))
                .value(Raven.INSTANCE.theme().palette().accentStart().argb());
        addSettings(this.color);
    }
}
