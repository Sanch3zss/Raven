package sanch3zss.raven.ui.setting;

import java.util.*;
import java.util.function.*;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.KeyAction;
import sanch3zss.raven.feature.setting.BindMode;

public class KeybindSetting extends Setting {
    public BindMode type;
    public final List<Integer> boundKeys;
    public Consumer<KeyAction> consumer;

    public KeybindSetting(Translation translation2, Translation translation) {
        super(translation2, translation);
        this.type = BindMode.TOGGLE;
        this.boundKeys = new ArrayList<>();
        this.consumer = action -> {};
    }

    public KeybindSetting(Translation translation2) {
        this(translation2, null);
    }

    public KeybindSetting visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public KeybindSetting consumer(Consumer<KeyAction> consumer) {
        this.consumer = consumer;
        return this;
    }

    public KeybindSetting consumer(BindMode bindMode, Consumer<KeyAction> consumer) {
        this.type = bindMode;
        this.consumer = consumer;
        return this;
    }

    public List<Integer> getKeyBind() {
        return Collections.unmodifiableList(this.boundKeys);
    }

    public int getKey() {
        if (this.boundKeys.isEmpty()) {
            return -1;
        }
        return this.boundKeys.getFirst();
    }

    public void setKey(int i) {
        setKey(i == -1 ? List.of() : List.of(i));
    }

    public void setKey(List<Integer> list) {
        this.boundKeys.clear();
        if (list == null) {
            return;
        }
        list.stream()
                .filter(num -> num != -1)
                .limit(2)
                .forEach(this.boundKeys::add);
    }

    @Override
    public Map<String, Object> toSerializedData() {
        return Map.of("keys", new ArrayList<>(getKeyBind()), "bindType", this.type.name());
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("keys");
        if (obj instanceof List<?> list) {
            setKey(list.stream().filter(Number.class::isInstance)
                    .map(Number.class::cast).map(Number::intValue).toList());
        } else {
            Object obj2 = map.get("key");
            if (obj2 instanceof Number n) {
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

    public void setType(BindMode bindMode) {
        this.type = bindMode;
    }

    public Consumer<KeyAction> getConsumer() {
        return this.consumer;
    }
}
