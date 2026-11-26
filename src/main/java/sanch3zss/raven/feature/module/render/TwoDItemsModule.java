package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;

public class TwoDItemsModule extends Module {
    public final BooleanSetting affect3DModels = new BooleanSetting(Translation.clearText("Затрагивать 3D-модели")).setValue(true);
    public final BooleanSetting renderSides = new BooleanSetting(Translation.clearText("Показывать боковые грани")).setValue(false);
    public final BooleanSetting castShadows = new BooleanSetting(Translation.clearText("Тени предметов")).setValue(true);

    public TwoDItemsModule() {
        super(ModuleTab.VISUALS, "2DItems");
        addSettings(affect3DModels, renderSides, castShadows);
    }
}
