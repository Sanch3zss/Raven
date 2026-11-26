package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.render.AspectRatioEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.render.AspectRatioPreset;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"Aspect Ratio", "Screen Ratio", "Custom Aspect", "Resolution Modifier", "Ratio Adjuster", "Display Aspect", "Resolution Adjustment"})
public class AspectRatioModule extends Module {
    public final ModeSetting<AspectRatioPreset> resolutionPreset;
    public final NumberSetting customResolution;

    public AspectRatioModule() {
        super(ModuleTab.VISUALS, "AspectRatio");
        this.resolutionPreset = new ModeSetting(Lang.ASPECTRATIO_RESOLUTION).values(AspectRatioPreset.class);
        this.customResolution = (NumberSetting) new NumberSetting(Lang.ASPECTRATIO_RESOLUTION_VALUE).range(0.7f, 1.5f).currentValue(0.7f).step(0.05f).setVisible(() -> {
            return Boolean.valueOf(this.resolutionPreset.isSelected(AspectRatioPreset.CUSTOM));
        });
        addSettings(this.resolutionPreset, this.customResolution);
        register(AspectRatioEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded() && isState()) {
                if (this.resolutionPreset.isSelected(AspectRatioPreset.CUSTOM)) {
                    event.setAspectRatio(this.customResolution.currentValue());
                    return;
                }
                String[] strArrSplit = ((AspectRatioPreset) this.resolutionPreset.currentValue()).getDisplayName().effective().split(":");
                event.setAspectRatio(Float.parseFloat(strArrSplit[0]) / Float.parseFloat(strArrSplit[1]));
            }
        });
    }
}
