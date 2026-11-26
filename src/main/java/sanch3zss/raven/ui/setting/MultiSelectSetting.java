package sanch3zss.raven.ui.setting;

import java.util.*;
import java.util.function.Supplier;

import sanch3zss.raven.client.chat.*;

public class MultiSelectSetting<T extends Enum<T>> extends Setting {
    public Set<T> selectedValues;
    public T[] options;

    public MultiSelectSetting(Translation translation2, Translation translation) {
        super(translation2, translation);
        this.selectedValues = new LinkedHashSet<>();
    }

    public MultiSelectSetting(Translation translation2) {
        this(translation2, null);
    }

    public MultiSelectSetting<T> values(Class<T> cls) {
        T[] enumConstants = cls.getEnumConstants();
        if (enumConstants == null || enumConstants.length == 0) {
            throw new IllegalArgumentException("Enum class " + cls.getSimpleName() + " has no constants.");
        }
        this.options = enumConstants;
        return this;
    }

    public MultiSelectSetting<T> visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    @SafeVarargs
    public final MultiSelectSetting<T> select(T... items) {
        for (T t : items) {
            select(t);
        }
        return this;
    }

    public MultiSelectSetting<T> toggle(T t) {
        if (isSelected(t)) {
            deselect(t);
        } else {
            select(t);
        }
        return this;
    }

    public MultiSelectSetting<T> select(T t) {
        if (!isValidOption(t)) {
            throw new IllegalArgumentException("Invalid enum value: " + t);
        }
        this.selectedValues.add(t);
        return this;
    }

    @SafeVarargs
    public final MultiSelectSetting<T> deselect(T... items) {
        for (T t : items) {
            this.selectedValues.remove(t);
        }
        return this;
    }

    public MultiSelectSetting<T> deselect(T t) {
        this.selectedValues.remove(t);
        return this;
    }

    public boolean isSelected(T t) {
        return this.selectedValues.contains(t);
    }

    public Translation optionName(T t) {
        if (t instanceof DisplayNamed displayNamed) {
            return displayNamed.getDisplayName();
        }
        throw new IllegalArgumentException(t + " called without displayable");
    }

    public boolean isValidOption(T t) {
        return Set.of(this.options).contains(t);
    }

    @Override
    public Map<String, Object> toSerializedData() {
        List<String> list = this.selectedValues.stream().map(Enum::name).toList();
        return Map.of("selected", list);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void loadSerializedData(Map<String, Object> map) {
        Object obj = map.get("selected");
        if (obj instanceof List<?> list) {
            this.selectedValues.clear();
            if (this.options != null && this.options.length > 0) {
                Class<T> declaringClass = this.options[0].getDeclaringClass();
                for (Object obj2 : list) {
                    if (obj2 instanceof String str) {
                        try {
                            this.selectedValues.add(Enum.valueOf(declaringClass, str));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            }
        }
    }

    public Set<T> selectedValues() {
        return this.selectedValues;
    }

    public T[] options() {
        return this.options;
    }

    public MultiSelectSetting<T> selectedValues(Set<T> set) {
        this.selectedValues = set;
        return this;
    }

    public MultiSelectSetting<T> options(T[] tArr) {
        this.options = tArr;
        return this;
    }
}
