package sanch3zss.raven.feature.setting;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import sanch3zss.raven.client.input.*;
import sanch3zss.raven.feature.player.MovementInputHelper;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.util.math.interpolation.Easings;

public class KeybindCaptureState {
    static final int maxKeys = 2;
    public final Set<Integer> pressedKeys = new LinkedHashSet<>();
    public final Set<Integer> mutableKeys = new LinkedHashSet<>();
    public final Set<Integer> immutableKeys = new LinkedHashSet<>();
    public boolean captureKey = false;
    public final ToggleAnimator conflictAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public Consumer<List<Integer>> changeCallback;

    public void toggleCapture() {
        this.captureKey = !this.captureKey;
    }

    public boolean handleKeyInput(KeyInput keyInput) {
        if (!this.captureKey) {
            return false;
        }
        if (keyInput.keyAction().press()) {
            return handleKeyPress(keyInput.keyCode());
        }
        if (keyInput.keyAction().release()) {
            return handleKeyRelease();
        }
        return false;
    }

    public boolean handleMouseInput(MouseButtonInput mouseButtonInput) {
        if (!this.captureKey || mouseButtonInput.button() == 0 || mouseButtonInput.button() == 1) {
            return false;
        }
        if (mouseButtonInput.action().press()) {
            return handleMousePress(mouseButtonInput.button());
        }
        if (mouseButtonInput.action().release()) {
            return handleMouseRelease();
        }
        return false;
    }

    boolean handleKeyPress(int i) {
        if (i == 256 || i == 261) {
            clearBinding();
            this.captureKey = false;
            return true;
        }
        if (i != -1) {
            this.pressedKeys.add(i);
        }
        if (this.pressedKeys.size() < 2) {
            return true;
        }
        commitKeys();
        this.captureKey = false;
        return true;
    }

    boolean handleKeyRelease() {
        if (!this.pressedKeys.isEmpty()) {
            commitKeys();
        }
        this.captureKey = false;
        return true;
    }

    boolean handleMousePress(int i) {
        if (i >= 0 && i < 8) {
            this.pressedKeys.add(i);
        }
        if (this.pressedKeys.size() < 2) {
            return true;
        }
        commitKeys();
        this.captureKey = false;
        return true;
    }

    boolean handleMouseRelease() {
        if (!this.pressedKeys.isEmpty()) {
            commitKeys();
        }
        this.captureKey = false;
        return true;
    }

    void clearBinding() {
        this.pressedKeys.clear();
        this.mutableKeys.clear();
        if (this.changeCallback != null) {
            this.changeCallback.accept(List.of());
        }
        MovementInputHelper.refreshPressedKeys();
    }

    void commitKeys() {
        List<Integer> listCopyOf = List.copyOf(this.pressedKeys);
        this.mutableKeys.clear();
        this.mutableKeys.addAll(limitKeys(listCopyOf));
        if (this.changeCallback != null) {
            this.changeCallback.accept(List.copyOf(this.mutableKeys));
        }
        MovementInputHelper.refreshPressedKeys();
        this.pressedKeys.clear();
    }

    public KeybindCaptureState addMutableKeys(List<Integer> list) {
        this.mutableKeys.clear();
        if (list != null) {
            list.stream().filter(num -> num != -1).forEach(this.mutableKeys::add);
        }
        return this;
    }

    public void syncKeys(List<Integer> list) {
        if (this.mutableKeys.equals(new LinkedHashSet<>(list))) {
            return;
        }
        addMutableKeys(list);
    }

    public KeybindCaptureState addImmutableKeys(List<Integer> list) {
        this.immutableKeys.clear();
        if (list != null) {
            list.stream().filter(num -> num != -1).forEach(this.immutableKeys::add);
        }
        return this;
    }

    public String formatBindingText() {
        List<String> parts = new ArrayList<>();
        if (hasKeys(this.mutableKeys)) {
            parts.add(this.captureKey ? "..." : formatKeys(this.mutableKeys));
        }
        if (hasKeys(this.immutableKeys)) {
            parts.add(formatKeys(this.immutableKeys));
        }
        if (parts.isEmpty()) {
            return this.captureKey ? "..." : "None";
        }
        return String.join(" or ", parts);
    }

    String formatKeys(Set<Integer> set) {
        return set.stream().map(KeyboardUtil::keyToString).collect(Collectors.joining(" + "));
    }

    public boolean hasAnyKeys() {
        return hasKeys(this.mutableKeys) || hasKeys(this.immutableKeys);
    }

    boolean hasKeys(Set<Integer> set) {
        return set != null && !set.isEmpty();
    }

    List<Integer> limitKeys(List<Integer> list) {
        return list.stream().limit(2L).toList();
    }

    public Set<Integer> pressedKeys() {
        return this.pressedKeys;
    }

    public Set<Integer> mutableKeys() {
        return this.mutableKeys;
    }

    public Set<Integer> immutableKeys() {
        return this.immutableKeys;
    }

    public boolean captureKey() {
        return this.captureKey;
    }

    public ToggleAnimator conflictAnimation() {
        return this.conflictAnimation;
    }

    public KeybindCaptureState onChange(Consumer<List<Integer>> consumer) {
        this.changeCallback = consumer;
        return this;
    }
}
