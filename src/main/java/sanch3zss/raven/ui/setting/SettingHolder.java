package sanch3zss.raven.ui.setting;

import java.util.*;

import com.google.common.collect.Lists;

public class SettingHolder {
    public final List<Setting> settings = Lists.newArrayList();

    public void addSettings(Setting... arr) {
        for (Setting setting : arr) {
            this.settings.add(setting);
            if (setting instanceof ColorSetting colorSetting && colorSetting.isThemeSyncAvailable()) {
                this.settings.add(colorSetting.themeSyncSetting());
            }
        }
    }

    public Setting get(String str) {
        return this.settings.stream().filter(value -> {
            return value.getName().effective().equalsIgnoreCase(str);
        }).findFirst().orElse(null);
    }

    public List<Setting> getSettings() {
        return this.settings;
    }
}
