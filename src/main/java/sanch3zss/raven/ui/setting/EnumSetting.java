package sanch3zss.raven.ui.setting;

import java.lang.Enum;
import java.util.Map;
import java.util.function.*;

import sanch3zss.raven.client.chat.*;

public class EnumSetting<T extends Enum<T>> extends Setting {
    public T[] options;

    public T currentValue;
    public Consumer<T> soundAction;

    public EnumSetting(Translation translation2, Translation translation) {
        super(translation2, translation);
    }

    public EnumSetting(Translation translation2) {
        this(translation2, null);
    }

    public EnumSetting<T> values(Class<T> cls) {
        T[] enumConstants = cls.getEnumConstants();
        if (enumConstants == null || enumConstants.length == 0) {
            throw new IllegalArgumentException("Enum class " + cls.getSimpleName() + " has no constants.");
        }
        this.options = enumConstants;
        this.currentValue = enumConstants[0];
        return this;
    }

    public EnumSetting<T> visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public EnumSetting<T> select(T t) {
        if (!isValidOption(t)) {
            throw new IllegalArgumentException("Invalid enum value: " + String.valueOf(t));
        }
        this.currentValue = t;
        return this;
    }

    public void setCurrentValue(T t) {
        if (!isValidOption(t)) {
            throw new IllegalArgumentException("Invalid enum value: " + String.valueOf(t));
        }
        this.currentValue = t;
    }

    public void playSoundForOption(T t) {
        if (this.soundAction != null) {
            this.soundAction.accept(t);
        }
    }

    public int selectedIndex() {
        for (int i = 0; i < this.options.length; i++) {
            if (this.options[i] == this.currentValue) {
                return i;
            }
        }
        throw new IllegalStateException("Current value is not in options list.");
    }

    public boolean isSelected(T t) {
        return this.currentValue == t;
    }

    public boolean isValidOption(T t) {
        for (T t2 : this.options) {
            if (t2 == t) {
                return true;
            }
        }
        return false;
    }

    public Translation optionName(T t) {
        if (t instanceof DisplayNamed) {
            return ((DisplayNamed) t).getDisplayName();
        }
        throw new IllegalArgumentException(String.valueOf(t) + " called without displayable");
    }

    @Override
    public Map<String, Object> toSerializedData() {
        return Map.of("value", this.currentValue.name());
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("value");
        if (obj instanceof String) {
            try {
                select(Enum.valueOf(this.currentValue.getDeclaringClass(), (String) obj));
            } catch (IllegalArgumentException e) {
            }
        }
    }

    public T[] options() {
        return this.options;
    }

    public T currentValue() {
        return this.currentValue;
    }

    public Consumer<T> soundAction() {
        return this.soundAction;
    }

    public EnumSetting<T> options(T[] tArr) {
        this.options = tArr;
        return this;
    }

    public EnumSetting<T> currentValue(T t) {
        this.currentValue = t;
        return this;
    }

    public EnumSetting<T> soundAction(Consumer<T> consumer) {
        this.soundAction = consumer;
        return this;
    }
}
