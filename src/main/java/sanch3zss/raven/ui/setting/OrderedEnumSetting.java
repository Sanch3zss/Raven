package sanch3zss.raven.ui.setting;

import java.util.*;
import java.util.function.Supplier;

import sanch3zss.raven.client.chat.*;

public class OrderedEnumSetting<E extends Enum<E>> extends Setting {
    public E[] options;
    public final Set<E> selected;
    public final List<E> order;

    public OrderedEnumSetting(Translation translation2) {
        super(translation2, null);
        this.selected = new HashSet<>();
        this.order = new ArrayList<>();
    }

    public OrderedEnumSetting(Translation translation2, Translation translation) {
        super(translation2, translation);
        this.selected = new HashSet<>();
        this.order = new ArrayList<>();
    }

    public OrderedEnumSetting<E> values(Class<E> cls) {
        E[] enumConstants = cls.getEnumConstants();
        if (enumConstants == null || enumConstants.length == 0) {
            throw new IllegalArgumentException("Enum " + cls.getSimpleName() + " пуст.");
        }
        this.options = enumConstants;
        if (this.order.isEmpty()) {
            this.order.addAll(Arrays.asList(enumConstants));
        }
        return this;
    }

    @SafeVarargs
    public final OrderedEnumSetting<E> selected(E... eArr) {
        for (E e : eArr) {
            validateValue(e);
            this.selected.add(e);
        }
        return this;
    }

    public Set<E> getSelected() {
        return this.selected;
    }

    @SafeVarargs
    public final OrderedEnumSetting<E> ordered(E... eArr) {
        this.order.clear();
        this.order.addAll(Arrays.asList(eArr));
        return this;
    }

    public OrderedEnumSetting<E> visible(Supplier<Boolean> supplier) {
        setVisible(supplier);
        return this;
    }

    public List<E> orderedValues() {
        return Collections.unmodifiableList(this.order);
    }

    public void move(int i, int i2) {
        if (i == i2) {
            return;
        }
        this.order.add(i2, this.order.remove(i));
    }

    public void select(E e) {
        validateValue(e);
        this.selected.add(e);
    }

    public void deselect(E e) {
        this.selected.remove(e);
    }

    public boolean isSelected(E e) {
        return this.selected.contains(e);
    }

    public Translation optionName(E e) {
        return e instanceof DisplayNamed displayNamed ? displayNamed.getDisplayName() : Translation.clearText(e.name());
    }

    public void validateValue(E e) {
        if (this.options == null) {
            throw new IllegalStateException("Сначала вызови values()!");
        }
        for (E e2 : this.options) {
            if (e2 == e) {
                return;
            }
        }
        throw new IllegalArgumentException("Недопустимое значение enum: " + e);
    }

    @Override
    public Map<String, Object> toSerializedData() {
        List<String> selectedList = this.selected.stream().map(Enum::name).toList();
        List<String> orderList = this.order.stream().map(Enum::name).toList();
        return Map.of("selected", selectedList, "order", orderList);
    }

    @Override
    public void loadSerializedData(Map<String, Object> map) {
        if (this.options == null || this.options.length == 0) {
            return;
        }
        Class<E> declaringClass = this.options[0].getDeclaringClass();
        Object obj = map.get("selected");
        if (obj instanceof List<?> list) {
            this.selected.clear();
            for (Object obj2 : list) {
                if (obj2 instanceof String str) {
                    try {
                        this.selected.add(Enum.valueOf(declaringClass, str));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        Object obj3 = map.get("order");
        if (obj3 instanceof List<?> list) {
            this.order.clear();
            for (Object obj4 : list) {
                if (obj4 instanceof String str) {
                    try {
                        this.order.add(Enum.valueOf(declaringClass, str));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            for (E option : this.options) {
                if (!this.order.contains(option)) {
                    this.order.add(option);
                }
            }
        }
    }

    public E[] options() {
        return this.options;
    }

    public List<E> order() {
        return this.order;
    }

    @SafeVarargs
    public final OrderedEnumSetting<E> options(E... eArr) {
        this.options = eArr;
        if (this.order.isEmpty()) {
            this.order.addAll(Arrays.asList(eArr));
        }
        return this;
    }
}
