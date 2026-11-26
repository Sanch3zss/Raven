package sanch3zss.raven.ui.hud.impl;

import java.util.*;

import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.util.math.interpolation.Easings;

public class HotkeyEntry {
    public final String name;
    public final String moduleName;
    public final List<Integer> keys;
    public final ToggleAnimator anim;

    public HotkeyEntry(String str, List<Integer> list) {
        this.keys = new ArrayList();
        this.anim = new ToggleAnimator(220, Easings.EASE_IN_OUT_QUAD);
        this.moduleName = null;
        this.name = str;
        if (list != null) {
            this.keys.addAll(list);
        }
    }

    public HotkeyEntry(Module module, String str, List<Integer> list) {
        this.keys = new ArrayList();
        this.anim = new ToggleAnimator(220, Easings.EASE_IN_OUT_QUAD);
        this.moduleName = module != null ? module.getName() : null;
        this.name = str;
        if (list != null) {
            this.keys.addAll(list);
        }
    }

    public boolean hasModule() {
        return this.moduleName != null;
    }

    public String name() {
        return this.name;
    }

    public String moduleName() {
        return this.moduleName;
    }

    public List<Integer> keys() {
        return this.keys;
    }

    public ToggleAnimator anim() {
        return this.anim;
    }
}
