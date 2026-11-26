package sanch3zss.raven.feature.module.player;

import net.minecraft.client.CameraType;
import net.minecraft.client.Options;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.KeyPressState;
import sanch3zss.raven.client.input.KeyboardUtil;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.ButtonAction;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.KeybindSetting;

@Aliases(aliases = {"Free Look", "Free View", "360 Look", "Look Around", "Third-Person View",
        "Advanced Camera", "Free Roam", "Dynamic View", "Camera Control"})
public class FreeLookModule extends Module {
    public final KeybindSetting keybind;
    private boolean active;
    private CameraType previousPerspective;

    public FreeLookModule() {
        super(ModuleTab.PLAYER, "FreeLook");
        this.keybind = new KeybindSetting(Lang.PLAYER_FREELOOK_KEY);
        addSettings(this.keybind);

        register(KeyInputEvent.class, event -> {
            if (!isState() || !Mc.INSTANCE.isWorldLoaded() || event.key() != this.keybind.getKey()) {
                return;
            }
            if (event.action() == KeyPressState.PRESS) {
                startFreeLook();
            } else if (event.action() == KeyPressState.RELEASE) {
                stopFreeLook();
            }
        });
        register(MouseButtonEvent.class, event -> {
            if (!isState() || !Mc.INSTANCE.isWorldLoaded() || event.button() != this.keybind.getKey()) {
                return;
            }
            if (event.action() == ButtonAction.PRESS) {
                startFreeLook();
            } else if (event.action() == ButtonAction.RELEASE) {
                stopFreeLook();
            }
        });
        register(ClientTickEvent.class, event -> {
            int key = this.keybind.getKey();
            if (!isState() || !Mc.INSTANCE.isWorldLoaded() || key == KEY_UNBOUND) {
                stopFreeLook();
                return;
            }
            if (KeyboardUtil.isKeyPressed(KeyboardUtil.getKeyType(key), key)) {
                startFreeLook();
            } else {
                stopFreeLook();
            }
        });
    }

    private void startFreeLook() {
        if (this.active || Mc.INSTANCE.getPlayer() == null) {
            return;
        }
        Options options = Mc.INSTANCE.getGameOptions();
        this.previousPerspective = options.getCameraType();
        if (this.previousPerspective.isFirstPerson()) {
            options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        this.active = true;
    }

    public boolean isFreeLooking() {
        return this.active && isState();
    }

    private void stopFreeLook() {
        if (!this.active) {
            return;
        }
        this.active = false;
        if (this.previousPerspective != null) {
            Mc.INSTANCE.getGameOptions().setCameraType(this.previousPerspective);
            this.previousPerspective = null;
        }
    }

    @Override
    public void deactivate() {
        stopFreeLook();
        super.deactivate();
    }
}
