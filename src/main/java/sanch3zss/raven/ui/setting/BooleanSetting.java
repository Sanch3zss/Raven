package sanch3zss.raven.ui.setting;

import java.util.*;
import java.util.function.Supplier;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.setting.BindMode;

public class BooleanSetting extends Setting {
    public BindMode type;
    public boolean value;
    public final List<Integer> keyBinds;

    public List<Integer> getKeyBind() {
        return Collections.unmodifiableList(this.keyBinds);
    }

    public BooleanSetting(Translation translation2, Translation translation) {
        super(translation2, translation);
        this.type = BindMode.TOGGLE;
        this.keyBinds = new ArrayList<>();
    }

    public BooleanSetting(Translation translation2) {
        this(translation2, null);
    }

    public BooleanSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public void switchValue() {
        this.value = !this.value;
    }

    public int getKey() {
        if (this.keyBinds.isEmpty()) {
            return -1;
        }
        return this.keyBinds.getFirst();
    }

    public void setKey(int i) {
        setKey(i == -1 ? List.of() : List.of(i));
    }

    public void setKey(List<Integer> list) {
        this.keyBinds.clear();
        if (list == null) {
            return;
        }
        list.stream()
                .filter(num -> num != -1)
                .limit(2)
                .forEach(this.keyBinds::add);
    }

    @Override
    public Map<String, Object> toSerializedData() {
        return Map.of("value", this.value, "keys", new ArrayList<>(getKeyBind()), "bindType", this.type.name());
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("value");
        if (obj instanceof Boolean b) {
            setValue(b);
        }
        Object obj2 = map.get("keys");
        if (obj2 instanceof List) {
            setKey(((List<?>) obj2).stream().filter(Number.class::isInstance)
                    .map(Number.class::cast).map(Number::intValue).toList());
        } else {
            Object legacyKey = map.get("key");
            if (legacyKey instanceof Number n) {
                setKey(n.intValue());
            }
        }
        Object obj3 = map.get("bindType");
        if (obj3 instanceof String str) {
            setType(BindMode.valueOf(str));
        }
    }

    public BindMode getType() {
        return this.type;
    }

    public boolean isValue() {
        return this.value;
    }

    public void setType(BindMode bindMode) {
        this.type = bindMode;
    }

    public BooleanSetting setValue(boolean z) {
        this.value = z;
        return this;
    }
}
