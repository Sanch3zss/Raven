package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.ColorSetting;

public class CustomHitBoxModule extends Module {
    public final ColorSetting color;
    public final BooleanSetting fill;
    public final BooleanSetting direction;
    public final BooleanSetting eyeLine;

    public CustomHitBoxModule() {
        super(ModuleTab.VISUALS, "CustomHitBox");
        this.color = new ColorSetting(Translation.clearText("Цвет"))
                .value(0xFF8FCBFF).staticOnly();
        this.fill = new BooleanSetting(Translation.clearText("Заливка"));
        this.fill.setValue(true);
        this.direction = new BooleanSetting(Translation.clearText("Стрелка направления"));
        this.direction.setValue(true);
        this.eyeLine = new BooleanSetting(Translation.clearText("Линия глаз"));
        this.eyeLine.setValue(true);
        addSettings(this.color, this.fill, this.direction, this.eyeLine);
    }

    public static CustomHitBoxModule active() {
        Raven raven = Raven.INSTANCE;
        return raven == null || raven.moduleRepository() == null ? null
                : raven.moduleRepository().find(CustomHitBoxModule.class)
                .filter(Module::isState).orElse(null);
    }

    public int outlineColor() {
        int selected = this.color.getFirstColor();
        return (selected & 0xFF000000)
                | ((int) (((selected >> 16) & 0xFF) * 0.82f) << 16)
                | ((int) (((selected >> 8) & 0xFF) * 0.82f) << 8)
                | (int) ((selected & 0xFF) * 0.82f);
    }

    public int fillColor() {
        int selected = this.color.getFirstColor();
        int alpha = (int) (((selected >>> 24) & 0xFF) * 0.62f);
        return (alpha << 24) | (selected & 0xFFFFFF);
    }
}
