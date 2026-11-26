package sanch3zss.raven.client.input;

import java.util.*;
import java.util.function.Consumer;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.audio.WavSoundPlayer;
import sanch3zss.raven.event.ClientListener;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.feature.module.*;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.*;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.ui.setting.*;

public class KeybindHandler implements ClientListener {
    public static final int codeBufferLength = 6;
    public static final String secretCode = "mxrbid";
    public final StringBuilder typedBuffer = new StringBuilder();
    public final Set<Integer> pressedKeys = new HashSet<>();

    public KeybindHandler() {
        Raven.INSTANCE.eventDispatcher().register(KeyInputEvent.class, this::onKeyInput);
        Raven.INSTANCE.eventDispatcher().register(MouseButtonEvent.class, this::onMouseButton);
    }

    public void onKeyInput(KeyInputEvent event) {
        if (Mc.INSTANCE.getWorld() == null) {
            return;
        }
        if (event.action() == KeyPressState.PRESS) {
            handleTypedChar((char) event.key());
            this.pressedKeys.add(event.key());
        }
        forEachModuleSetting((module, setting) -> {
            if (setting == null) {
                processModuleBind(event, module);
            } else if (setting instanceof BooleanSetting booleanSetting) {
                processBooleanSettingBind(event, booleanSetting);
            } else if (setting instanceof ExpandableSetting expandableSetting) {
                processExpandableSettingBind(event, expandableSetting);
                if (expandableSetting.isValue()) {
                    expandableSetting.getSubSettings().forEach(subSetting -> {
                        if (subSetting instanceof KeybindSetting keybindSetting) {
                            processKeybindSettingBind(event, keybindSetting);
                        }
                    });
                }
            } else if (setting instanceof KeybindSetting keybindSetting) {
                processKeybindSettingBind(event, keybindSetting);
            }
        });
        forEachWidgetKeybind(setting -> processKeybindSettingBind(event, setting));
        if (event.action() == KeyPressState.RELEASE) {
            this.pressedKeys.remove(event.key());
        }
    }

    public void onMouseButton(MouseButtonEvent event) {
        if (Mc.INSTANCE.getWorld() == null) {
            return;
        }
        if (event.action() == ButtonAction.PRESS) {
            this.pressedKeys.add(event.button());
        }
        forEachModuleSetting((module, setting) -> {
            if (setting == null) {
                processModuleBind(event, module);
            } else if (setting instanceof BooleanSetting booleanSetting) {
                processBooleanSettingBind(event, booleanSetting);
            } else if (setting instanceof ExpandableSetting expandableSetting) {
                processExpandableSettingBind(event, expandableSetting);
                if (expandableSetting.isValue()) {
                    expandableSetting.getSubSettings().forEach(subSetting -> {
                        if (subSetting instanceof KeybindSetting keybindSetting) {
                            processKeybindSettingBind(event, keybindSetting);
                        }
                    });
                }
            } else if (setting instanceof KeybindSetting keybindSetting) {
                processKeybindSettingBind(event, keybindSetting);
            }
        });
        forEachWidgetKeybind(setting -> processKeybindSettingBind(event, setting));
        if (event.action() == ButtonAction.RELEASE) {
            this.pressedKeys.remove(event.button());
        }
    }

    public void handleTypedChar(char c) {
        this.typedBuffer.append(c);
        if (this.typedBuffer.length() > codeBufferLength) {
            this.typedBuffer.delete(0, this.typedBuffer.length() - codeBufferLength);
        }
        if (secretCode.equalsIgnoreCase(this.typedBuffer.toString())) {
            WavSoundPlayer.INSTANCE.playSound(secretCode, 80.0f, false);
        }
    }

    public void forEachModuleSetting(ModuleSettingConsumer moduleSettingConsumer) {
        for (Module module : Raven.INSTANCE.moduleRepository().getModules()) {
            if (module.isBlocked()) {
                continue;
            }
            moduleSettingConsumer.process(module, null);
            if (module.isState()) {
                for (Setting setting : module.getSettings()) {
                    moduleSettingConsumer.process(module, setting);
                }
            }
        }
    }

    private void forEachWidgetKeybind(Consumer<KeybindSetting> consumer) {
        for (Draggable widget : Raven.INSTANCE.widgetStack().widgets()) {
            if (!widget.isVisible()) {
                continue;
            }
            for (Setting setting : widget.getSettings()) {
                if (setting instanceof KeybindSetting keybindSetting) {
                    consumer.accept(keybindSetting);
                }
            }
        }
    }

    public void processModuleBind(Object obj, Module module) {
        if (module.getKeyBind().isEmpty()) {
            return;
        }
        if (obj instanceof KeyInputEvent event2) {
            if (matchesKeyBind(event2, module.getKeyBind())) {
                BindMode type = module.getType();
                boolean z = event2.action() == KeyPressState.PRESS;
                Objects.requireNonNull(module);
                applyBindAction(type, z, module::switchState);
                return;
            }
        }
        if (obj instanceof MouseButtonEvent event) {
            if (matchesMouseBind(event, module.getKeyBind())) {
                BindMode type2 = module.getType();
                boolean z2 = event.action() == ButtonAction.PRESS;
                Objects.requireNonNull(module);
                applyBindAction(type2, z2, module::switchState);
            }
        }
    }

    public void processBooleanSettingBind(Object obj, BooleanSetting booleanSetting) {
        if (booleanSetting.getKeyBind().isEmpty()) {
            return;
        }
        if (obj instanceof KeyInputEvent event2) {
            if (matchesKeyBind(event2, booleanSetting.getKeyBind())) {
                BindMode type = booleanSetting.getType();
                boolean z = event2.action() == KeyPressState.PRESS;
                Objects.requireNonNull(booleanSetting);
                applyBindAction(type, z, booleanSetting::switchValue);
                return;
            }
        }
        if (obj instanceof MouseButtonEvent event) {
            if (matchesMouseBind(event, booleanSetting.getKeyBind())) {
                BindMode type2 = booleanSetting.getType();
                boolean z2 = event.action() == ButtonAction.PRESS;
                Objects.requireNonNull(booleanSetting);
                applyBindAction(type2, z2, booleanSetting::switchValue);
            }
        }
    }

    public void processExpandableSettingBind(Object obj, ExpandableSetting expandableSetting) {
        if (expandableSetting.getKeybinds().isEmpty()) {
            return;
        }
        Runnable runnable = () -> {
            expandableSetting.setValue(!expandableSetting.isValue());
        };
        if (obj instanceof KeyInputEvent event2) {
            if (matchesKeyBind(event2, expandableSetting.getKeybinds())) {
                applyBindAction(expandableSetting.getType(), event2.action() == KeyPressState.PRESS, runnable);
                return;
            }
        }
        if (obj instanceof MouseButtonEvent event) {
            if (matchesMouseBind(event, expandableSetting.getKeybinds())) {
                applyBindAction(expandableSetting.getType(), event.action() == ButtonAction.PRESS, runnable);
            }
        }
    }

    public void processKeybindSettingBind(Object obj, KeybindSetting keybindSetting) {
        if (keybindSetting.getKeyBind().isEmpty()) {
            return;
        }
        if (obj instanceof KeyInputEvent event2) {
            if (matchesKeyBind(event2, keybindSetting.getKeyBind())) {
                KeyAction keyAction2 = event2.action() == KeyPressState.PRESS ? KeyAction.PRESS : KeyAction.RELEASE;
                applyBindAction(keybindSetting.getType(), event2.action() == KeyPressState.PRESS, () -> {
                    keybindSetting.getConsumer().accept(keyAction2);
                });
                return;
            }
        }
        if (obj instanceof MouseButtonEvent event) {
            if (matchesMouseBind(event, keybindSetting.getKeyBind())) {
                KeyAction keyAction = event.action() == ButtonAction.PRESS ? KeyAction.PRESS : KeyAction.RELEASE;
                applyBindAction(keybindSetting.getType(), event.action() == ButtonAction.PRESS, () -> {
                    keybindSetting.getConsumer().accept(keyAction);
                });
            }
        }
    }

    public void applyBindAction(BindMode bindMode, boolean z, Runnable runnable) {
        if (bindMode == BindMode.TOGGLE && z) {
            runnable.run();
        } else if (bindMode == BindMode.HOLD) {
            runnable.run();
        }
    }

    public boolean matchesKeyBind(KeyInputEvent event2, List<Integer> list) {
        if ((event2.action() == KeyPressState.PRESS || event2.action() == KeyPressState.RELEASE) && event2.key() == list.getLast()) {
            if (isCombinationPressed(event2.action() == KeyPressState.RELEASE, list)) {
                return true;
            }
        }
        return false;
    }

    public boolean matchesMouseBind(MouseButtonEvent event, List<Integer> list) {
        if ((event.action() == ButtonAction.PRESS || event.action() == ButtonAction.RELEASE) && event.button() == list.getLast()) {
            if (isCombinationPressed(event.action() == ButtonAction.RELEASE, list)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCombinationPressed(boolean isRelease, List<Integer> list) {
        Set<Integer> currentPressed = new HashSet<>(this.pressedKeys);
        if (isRelease) {
            currentPressed.add(list.getLast());
        }
        Set<Integer> allBoundKeys = collectAllBoundKeys();
        Set<Integer> matchingKeys = new HashSet<>();
        for (int key : currentPressed) {
            if (allBoundKeys.contains(key)) {
                matchingKeys.add(key);
            }
        }
        return matchingKeys.containsAll(list) && !isOverriddenByLongerBind(matchingKeys, list, list.getLast());
    }

    public boolean isOverriddenByLongerBind(Set<Integer> activeKeys, List<Integer> bind, int lastKey) {
        Set<Integer> bindSet = new HashSet<>(bind);
        return collectAllBinds().stream()
                .filter(otherBind -> !new HashSet<>(otherBind).equals(bindSet))
                .filter(otherBind -> !otherBind.isEmpty() && otherBind.getLast() == lastKey)
                .anyMatch(otherBind -> activeKeys.equals(new HashSet<>(otherBind)) && otherBind.size() > bind.size());
    }

    public Set<List<Integer>> collectAllBinds() {
        Set<List<Integer>> binds = new HashSet<>();
        forEachModuleSetting((module, setting) -> {
            if (setting == null) {
                addBindList(binds, module.getKeyBind());
            } else if (setting instanceof BooleanSetting booleanSetting) {
                addBindList(binds, booleanSetting.getKeyBind());
            } else if (setting instanceof ExpandableSetting expandableSetting) {
                addBindList(binds, expandableSetting.getKeybinds());
                expandableSetting.getSubSettings().forEach(subSetting -> {
                    if (subSetting instanceof KeybindSetting keybindSetting) {
                        addBindList(binds, keybindSetting.getKeyBind());
                    }
                });
            } else if (setting instanceof KeybindSetting keybindSetting) {
                addBindList(binds, keybindSetting.getKeyBind());
            }
        });
        forEachWidgetKeybind(setting -> addBindList(binds, setting.getKeyBind()));
        return binds;
    }

    public Set<Integer> collectAllBoundKeys() {
        Set<Integer> keys = new HashSet<>();
        forEachModuleSetting((module, setting) -> {
            if (setting == null) {
                addBindKeys(keys, module.getKeyBind());
            } else if (setting instanceof BooleanSetting booleanSetting) {
                addBindKeys(keys, booleanSetting.getKeyBind());
            } else if (setting instanceof ExpandableSetting expandableSetting) {
                addBindKeys(keys, expandableSetting.getKeybinds());
                expandableSetting.getSubSettings().forEach(subSetting -> {
                    if (subSetting instanceof KeybindSetting keybindSetting) {
                        addBindKeys(keys, keybindSetting.getKeyBind());
                    }
                });
            } else if (setting instanceof KeybindSetting keybindSetting) {
                addBindKeys(keys, keybindSetting.getKeyBind());
            }
        });
        forEachWidgetKeybind(setting -> addBindKeys(keys, setting.getKeyBind()));
        return keys;
    }

    public void addBindList(Set<List<Integer>> set, List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        set.add(list);
    }

    public void addBindKeys(Set<Integer> set, List<Integer> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        set.addAll(list);
    }
}
