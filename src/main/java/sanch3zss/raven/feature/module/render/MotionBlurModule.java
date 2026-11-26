package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.NumberSetting;

public class MotionBlurModule extends Module {
    public final NumberSetting strength;

    public MotionBlurModule() {
        super(ModuleTab.VISUALS, "MotionBlur");
        strength = new NumberSetting(Translation.clearText("Сила"))
                .range(0.25f, 1.0f).currentValue(1.0f).step(0.25f)
                .unit(SettingUnit.UNITS);
        addSettings(strength);
    }

    @Override
    public void activate() {
        strength.setCurrentValue(Math.max(strength.min(), Math.min(strength.max(), strength.currentValue())));
        super.activate();
    }
}
