package sanch3zss.raven.feature.module;

import sanch3zss.raven.ui.setting.Setting;

@FunctionalInterface
public interface ModuleSettingConsumer {
    void process(Module module, Setting setting);
}
