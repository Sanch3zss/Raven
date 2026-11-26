package sanch3zss.raven.feature.module.misc;

import java.util.List;
import sanch3zss.raven.client.audio.WavSoundPlayer;
import sanch3zss.raven.client.audio.CustomSoundPicker;
import sanch3zss.raven.client.audio.SoundSelectionMode;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.client.ModuleStateEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.feature.module.misc.ClientSoundType;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"Client Sounds", "Module Sounds", "Sound Effects", "Sounds", "Toggle Sounds"})
public class SoundsModule extends Module {
    public final EnumSetting<ClientSoundType> soundTypeSetting;
    public final ModeSetting<SoundSelectionMode> selectionMode;
    public final TextFieldSetting enableSound;
    public final TextFieldSetting disableSound;
    public final NumberSetting volumeSetting;
    public boolean playEnableSound;

    public SoundsModule() {
        super(ModuleTab.MISC, "Sounds");
        this.selectionMode = new ModeSetting<SoundSelectionMode>(Translation.clearText("Тип Звуков"))
                .values(SoundSelectionMode.class);
        this.soundTypeSetting = new EnumSetting<ClientSoundType>(Lang.CLIENTSOUNDS_TYPE)
                .values(ClientSoundType.class).visible(() -> this.selectionMode.isSelected(SoundSelectionMode.CLIENT));
        this.enableSound = new TextFieldSetting(Translation.clearText("Путь к звуку включения"))
                .setText("").visible(() -> false);
        this.disableSound = new TextFieldSetting(Translation.clearText("Путь к звуку выключения"))
                .setText("").visible(() -> false);
        this.volumeSetting = new NumberSetting(Lang.CLIENTSOUNDS_VOLUME).currentValue(70.0f).range(1.0f, 100.0f).step(1.0f).unit(SettingUnit.PERCENTS);
        ButtonSetting chooseEnable = new ButtonSetting(Translation.clearText("Звук включения"))
                .setButtonName(Translation.clearText("Выбрать звук для вкл. модуля"))
                .setRunnable(() -> CustomSoundPicker.choose(path -> {
                    this.enableSound.setText(path);
                    WavSoundPlayer.INSTANCE.playCustomSound(path, this.volumeSetting.currentValue());
                }))
                .visible(() -> this.selectionMode.isSelected(SoundSelectionMode.CUSTOM));
        ButtonSetting chooseDisable = new ButtonSetting(Translation.clearText("Звук выключения"))
                .setButtonName(Translation.clearText("Выбрать звук для выкл. модуля"))
                .setRunnable(() -> CustomSoundPicker.choose(path -> {
                    this.disableSound.setText(path);
                    WavSoundPlayer.INSTANCE.playCustomSound(path, this.volumeSetting.currentValue());
                }))
                .visible(() -> this.selectionMode.isSelected(SoundSelectionMode.CUSTOM));
        ButtonSetting resetSounds = new ButtonSetting(Translation.clearText(""))
                .setButtonName(Translation.clearText("Сбросить"))
                .setRunnable(() -> {
                    this.enableSound.setText("");
                    this.disableSound.setText("");
                }).visible(() -> this.selectionMode.isSelected(SoundSelectionMode.CUSTOM));
        this.playEnableSound = true;
        ExpandableSetting custom = new ExpandableSetting(Translation.clearText("Свои звуки"))
                .navigationOnly().setSubSettings(List.of(chooseEnable, chooseDisable,
                        resetSounds, this.enableSound, this.disableSound))
                .visible(() -> this.selectionMode.isSelected(SoundSelectionMode.CUSTOM));
        addSettings(this.selectionMode, this.soundTypeSetting, this.volumeSetting, custom);
        this.soundTypeSetting.soundAction(this::playPreviewSound);
        register(ModuleStateEvent.class, event -> {
            if (isState()) {
                if (this.selectionMode.isSelected(SoundSelectionMode.CUSTOM)) {
                    WavSoundPlayer.INSTANCE.playCustomSound(event.moduleState()
                            ? this.enableSound.getText() : this.disableSound.getText(),
                            this.volumeSetting.currentValue());
                } else {
                    WavSoundPlayer.INSTANCE.playSound(event.moduleState() ? "module_enable_" + (this.soundTypeSetting.selectedIndex() + 1) : "module_disable_" + (this.soundTypeSetting.selectedIndex() + 1), this.volumeSetting.currentValue(), false);
                }
            }
        });
    }

    public void playPreviewSound(ClientSoundType clientSoundType) {
        int soundIndex = indexOf(clientSoundType);
        WavSoundPlayer.INSTANCE.playSound(this.playEnableSound ? "module_enable_" + (soundIndex + 1) : "module_disable_" + (soundIndex + 1), this.volumeSetting.currentValue(), false);
        this.playEnableSound = !this.playEnableSound;
    }

    public int indexOf(ClientSoundType clientSoundType) {
        ClientSoundType[] arrValues = ClientSoundType.values();
        for (int i = 0; i < arrValues.length; i++) {
            if (arrValues[i] == clientSoundType) {
                return i;
            }
        }
        return 0;
    }
}
