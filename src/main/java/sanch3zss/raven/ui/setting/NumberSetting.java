package sanch3zss.raven.ui.setting;

import java.util.Map;
import java.util.function.Supplier;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.setting.SettingUnit;

public class NumberSetting extends Setting {
    public float currentValue;
    public float min;
    public float max;
    public float step;
    public SettingUnit valueUnit;

    public NumberSetting(Translation translation2, Translation translation) {
        super(translation2, translation);
        this.step = 0.05f;
        this.valueUnit = SettingUnit.UNITS;
    }

    public NumberSetting(Translation translation2) {
        this(translation2, null);
    }

    public NumberSetting range(float f, float f2) {
        this.min = f;
        this.max = f2;
        return this;
    }

    public NumberSetting unit(SettingUnit settingUnit) {
        this.valueUnit = settingUnit;
        return this;
    }

    public NumberSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public void setCurrentValue(float f) {
        this.currentValue = f;
    }

    @Override
    public Map<String, Object> toSerializedData() {
        return Map.of("value", Float.valueOf(this.currentValue));
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("value");
        if (obj instanceof Number) {
            currentValue(((Number) obj).floatValue());
        }
    }

    public float currentValue() {
        return this.currentValue;
    }

    public float min() {
        return this.min;
    }

    public float max() {
        return this.max;
    }

    public float step() {
        return this.step;
    }

    public SettingUnit valueUnit() {
        return this.valueUnit;
    }

    public NumberSetting currentValue(float f) {
        this.currentValue = f;
        return this;
    }

    public NumberSetting min(float f) {
        this.min = f;
        return this;
    }

    public NumberSetting max(float f) {
        this.max = f;
        return this;
    }

    public NumberSetting step(float f) {
        this.step = f;
        return this;
    }

    public NumberSetting valueUnit(SettingUnit settingUnit) {
        this.valueUnit = settingUnit;
        return this;
    }
}
