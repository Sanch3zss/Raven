package sanch3zss.raven.feature.module;

import java.util.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.ConfigApplyContext;
import sanch3zss.raven.event.*;
import sanch3zss.raven.event.client.ModuleStateEvent;
import sanch3zss.raven.feature.setting.BindMode;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.SettingHolder;

public class Module extends SettingHolder {
    public static final int KEY_UNBOUND = -1;
    public final ModuleTab moduleTab;

    public ModuleCategory category;
    public final String visibleName;
    public final String name;
    public BindMode type;
    public final List<Integer> boundKeys;
    public boolean state;

    public Module(ModuleTab tab, String str) {
        this(tab, null, str);
    }

    public Module(ModuleTab tab, ModuleCategory category, String str) {
        this.type = BindMode.TOGGLE;
        this.boundKeys = new ArrayList<>();
        this.moduleTab = tab;
        this.visibleName = str;
        this.name = str;
        if (category != null) {
            setCategory(category);
        }
    }

    public void switchState() {
        setState(!this.state);
    }

    public void setState(boolean z) {
        if (z && isBlocked()) {
            Raven.INSTANCE.featureControl().rememberRequestedActivation(this);
            return;
        }
        if (z != this.state) {
            this.state = z;
            applyState();
        }
    }

    public Module setStateSilent(boolean z) {
        if (z && isBlocked()) {
            Raven.INSTANCE.featureControl().rememberRequestedActivation(this);
            return this;
        }
        if (z != this.state) {
            this.state = z;
        }
        return this;
    }

    public void setCategory(ModuleCategory category) {
        if (category != null && !category.supports(this.moduleTab)) {
            throw new IllegalArgumentException("Category %s is not supported for tab %s".formatted(category.name(), this.moduleTab.name()));
        }
        this.category = category;
    }

    public void setBind(int i, BindMode bindMode) {
        setBind(i == -1 ? List.of() : List.of(i), bindMode);
    }

    public void setBind(List<Integer> list, BindMode bindMode) {
        setKey(list);
        this.type = bindMode;
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

    public List<Integer> getKeyBind() {
        return Collections.unmodifiableList(this.boundKeys);
    }

    public boolean hasKeyBind() {
        return !this.boundKeys.isEmpty();
    }

    public int getKey() {
        if (this.boundKeys.isEmpty()) {
            return -1;
        }
        return this.boundKeys.getFirst();
    }

    public void applyState() {
        if (this.state) {
            activate();
        } else {
            deactivate();
        }
    }

    public void activate() {
        if (!ConfigApplyContext.isApplying()) {
            Raven.INSTANCE.eventDispatcher().dispatch(new ModuleStateEvent(this, true));
        }
    }

    public void deactivate() {
        if (!ConfigApplyContext.isApplying()) {
            Raven.INSTANCE.eventDispatcher().dispatch(new ModuleStateEvent(this, false));
        }
    }

    public final <T extends Event> void register(Class<T> cls, EventCallback<T> eventCallback) {
        Raven.INSTANCE.eventDispatcher().register(cls, eventCallback);
    }

    public final <T extends Event> void register(Class<T> cls, EventCallback<T> eventCallback, EventPriority eventPriority) {
        Raven.INSTANCE.eventDispatcher().register(cls, eventCallback, eventPriority);
    }

    public ModuleTab getModuleTab() {
        return this.moduleTab;
    }

    public ModuleCategory getCategory() {
        return this.category;
    }

    public String getVisibleName() {
        return this.visibleName;
    }

    public String getName() {
        return this.name;
    }

    public BindMode getType() {
        return this.type;
    }

    public boolean isState() {
        return this.state;
    }

    public boolean isBlocked() {
        return Raven.INSTANCE != null && Raven.INSTANCE.featureControl() != null
                && Raven.INSTANCE.featureControl().isBlocked(this);
    }

    public void setType(BindMode bindMode) {
        this.type = bindMode;
    }
}
