package sanch3zss.raven.ui.setting;

import java.util.*;
import java.util.function.Supplier;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.setting.BindMode;

public class ExpandableSetting extends Setting {
    public List<Setting> serializableChildren;
    public boolean value;
    public BindMode type;
    public final List<Integer> keyBind;
    private boolean navigationOnly;
    private boolean inlined;

    public ExpandableSetting(Translation translation2, Translation translation) {
        super(translation2, translation);
        this.serializableChildren = new ArrayList<>();
        this.type = BindMode.TOGGLE;
        this.keyBind = new ArrayList<>();
    }

    public ExpandableSetting(Translation translation2) {
        this(translation2, null);
    }

    public ExpandableSetting settings(Setting... arr) {
        this.serializableChildren.addAll(Arrays.asList(arr));
        return this;
    }

    public ExpandableSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public void switchValue() {
        if (!this.navigationOnly) {
            this.value = !this.value;
        }
    }

    public ExpandableSetting navigationOnly() {
        this.navigationOnly = true;
        this.value = true;
        return this;
    }

    public boolean isNavigationOnly() {
        return this.navigationOnly;
    }

    public static List<Setting> inlineSmallGroups(List<Setting> settings) {
        List<Setting> result = new ArrayList<>();
        for (Setting setting : settings) {
            if (setting instanceof ExpandableSetting group && group.isSmallGroup()) {
                result.addAll(group.inlineChildren());
            } else {
                result.add(setting);
            }
        }
        return result;
    }

    private boolean isSmallGroup() {
        if (!this.navigationOnly) {
            return false;
        }
        int shown = 0;
        for (Setting child : this.serializableChildren) {
            boolean hiddenField = child instanceof TextFieldSetting && child.getVisible() != null && !child.getVisible().get();
            if (!hiddenField) {
                shown++;
            }
        }
        return shown <= 4;
    }

    private List<Setting> inlineChildren() {
        if (!this.inlined) {
            this.inlined = true;
            Supplier<Boolean> groupVisible = this.visible;
            for (Setting child : this.serializableChildren) {
                Supplier<Boolean> childVisible = child.getVisible();
                child.setVisible(() -> (groupVisible == null || groupVisible.get())
                        && (childVisible == null || childVisible.get()));
            }
        }
        return this.serializableChildren;
    }

    public Setting getSubSetting(String str) {
        return this.serializableChildren.stream()
                .filter(s -> s.getName().original().equalsIgnoreCase(str))
                .findFirst()
                .orElse(null);
    }

    public List<Integer> getKeybinds() {
        return Collections.unmodifiableList(this.keyBind);
    }

    public int getKey() {
        if (this.keyBind.isEmpty()) {
            return -1;
        }
        return this.keyBind.getFirst();
    }

    public void setKey(int i) {
        setKey(i == -1 ? List.of() : List.of(i));
    }

    public void setKey(List<Integer> list) {
        this.keyBind.clear();
        if (list == null) {
            return;
        }
        list.stream()
                .filter(num -> num != -1)
                .limit(2)
                .forEach(this.keyBind::add);
    }

    @Override
    public Map<String, Object> toSerializedData() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", this.value);
        map.put("keys", new ArrayList<>(getKeybinds()));
        map.put("bindType", this.type.name());
        return map;
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("value");
        if (obj instanceof Boolean b) {
            setValue(b);
        }
        Object obj2 = map.get("keys");
        if (obj2 instanceof List<?> list) {
            setKey(list.stream().filter(Number.class::isInstance)
                    .map(Number.class::cast).map(Number::intValue).toList());
        } else {
            Object obj3 = map.get("key");
            if (obj3 instanceof Number n) {
                setKey(n.intValue());
            }
        }
        Object obj4 = map.get("bindType");
        if (obj4 instanceof String str) {
            setType(BindMode.valueOf(str));
        }
    }

    @Override
    public List<Setting> getSerializableChildren() {
        return this.serializableChildren;
    }

    public List<Setting> getSubSettings() {
        return this.serializableChildren;
    }

    public boolean isValue() {
        return this.value;
    }

    public BindMode getType() {
        return this.type;
    }

    public List<Integer> getKeyBind() {
        return this.keyBind;
    }

    public ExpandableSetting setSubSettings(List<Setting> list) {
        this.serializableChildren = list;
        return this;
    }

    public ExpandableSetting setValue(boolean z) {
        this.value = this.navigationOnly || z;
        return this;
    }

    public ExpandableSetting setType(BindMode bindMode) {
        this.type = bindMode;
        return this;
    }
}
