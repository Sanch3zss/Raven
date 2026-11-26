package sanch3zss.raven.ui.setting;

import java.util.*;
import java.util.function.Supplier;

import sanch3zss.raven.client.chat.Translation;

public class Setting {
    public final Translation name;
    public final Translation description;
    public Supplier<Boolean> visible;

    public Setting(Translation translation2) {
        this(translation2, null);
    }

    public Setting setVisible(Supplier<Boolean> supplier) {
        this.visible = supplier;
        return this;
    }

    public Map<String, Object> toSerializedData() {
        return Map.of();
    }

    public void loadSerializedData(Map<String, Object> map) {
    }

    public List<Setting> getSerializableChildren() {
        return List.of();
    }

    public Translation getName() {
        return this.name;
    }

    public Translation getDescription() {
        return this.description;
    }

    public Supplier<Boolean> getVisible() {
        return this.visible;
    }

    public Setting(Translation translation2, Translation translation) {
        this.name = translation2;
        this.description = translation;
    }
}
