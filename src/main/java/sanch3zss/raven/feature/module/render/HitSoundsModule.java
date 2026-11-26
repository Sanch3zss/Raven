package sanch3zss.raven.feature.module.render;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.audio.WavSoundPlayer;
import sanch3zss.raven.client.audio.CustomSoundPicker;
import sanch3zss.raven.client.audio.SoundSelectionMode;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.client.SoundPlayEvent;
import sanch3zss.raven.event.player.AttackEntityEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.render.HitSoundType;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"Hit Sounds", "Custom Hit Sounds", "Attack Sounds", "Damage Sounds", "Hit Markers"})
public class HitSoundsModule extends Module {
    private static final long ATTACK_SOUND_WINDOW_NANOS = 1_000_000_000L;
    private static final long SAME_ATTACK_NANOS = 150_000_000L;
    private static final double ATTACK_SOUND_RADIUS_SQUARED = 16.0d;
    EnumSetting<HitSoundType> soundTypeSetting;
    ModeSetting<SoundSelectionMode> selectionMode;
    TextFieldSetting customSound;
    NumberSetting volumeSetting;
    BooleanSetting disableVanillaSound;
    private final Deque<Long> pendingAttacks = new ArrayDeque<>();
    private long lastConfirmedAttack;

    public HitSoundsModule() {
        super(ModuleTab.MISC, "HitSounds");
        this.selectionMode = new ModeSetting<SoundSelectionMode>(Translation.clearText("Тип Звуков"))
                .values(SoundSelectionMode.class);
        this.soundTypeSetting = new EnumSetting<HitSoundType>(Lang.HITSOUNDS_TYPE)
                .values(HitSoundType.class).visible(() -> this.selectionMode.isSelected(SoundSelectionMode.CLIENT));
        this.customSound = new TextFieldSetting(Translation.clearText("Путь к звуку удара"))
                .setText("").visible(() -> false);
        ButtonSetting chooseSound = new ButtonSetting(Translation.clearText("Звук удара"))
                .setButtonName(Translation.clearText("Выбрать звук"))
                .setRunnable(() -> CustomSoundPicker.choose(path -> {
                    this.customSound.setText(path);
                    WavSoundPlayer.INSTANCE.playCustomSound(path, this.volumeSetting.currentValue());
                }))
                .visible(() -> this.selectionMode.isSelected(SoundSelectionMode.CUSTOM));
        ButtonSetting resetSound = new ButtonSetting(Translation.clearText(""))
                .setButtonName(Translation.clearText("Сбросить"))
                .setRunnable(() -> this.customSound.setText(""))
                .visible(() -> this.selectionMode.isSelected(SoundSelectionMode.CUSTOM));
        this.volumeSetting = new NumberSetting(Lang.HITSOUNDS_VOLUME).currentValue(70.0f).range(1.0f, 100.0f).step(1.0f).unit(SettingUnit.PERCENTS);
        this.disableVanillaSound = new BooleanSetting(Lang.HITSOUNDS_DISABLE_VANILLA,
                Lang.HITSOUNDS_DISABLE_VANILLA_DESC);
        ExpandableSetting custom = new ExpandableSetting(Translation.clearText("Свой звук"))
                .navigationOnly().setSubSettings(List.of(chooseSound, resetSound, this.customSound))
                .visible(() -> this.selectionMode.isSelected(SoundSelectionMode.CUSTOM));
        addSettings(this.selectionMode, this.soundTypeSetting, this.volumeSetting,
                this.disableVanillaSound, custom);
        this.soundTypeSetting.soundAction(this::previewSound);
        register(AttackEntityEvent.class, event -> {
            if (!isState() || !Mc.INSTANCE.isWorldLoaded()) return;
            if (event.attacker.getId() < 0) {
                playHitSound();
                return;
            }
            long now = System.nanoTime();
            discardExpiredAttacks(now);
            this.pendingAttacks.addLast(now);
        });
        register(SoundPlayEvent.class, event -> {
            if (!isState() || !Mc.INSTANCE.isWorldLoaded()) return;
            SoundInstance sound = event.getSoundInstance();
            String path = sound.getIdentifier().getPath();
            if (sound.getSource() != SoundSource.PLAYERS
                    || !sound.getIdentifier().getNamespace().equals("minecraft")
                    || !path.startsWith("entity.player.attack.")
                    || Mc.INSTANCE.getPlayer().distanceToSqr(sound.getX(), sound.getY(), sound.getZ())
                    > ATTACK_SOUND_RADIUS_SQUARED) {
                return;
            }
            long now = System.nanoTime();
            discardExpiredAttacks(now);
            if (!this.pendingAttacks.isEmpty()) {
                this.pendingAttacks.removeFirst();
                this.lastConfirmedAttack = now;
                if (!path.endsWith("nodamage")) playHitSound();
            } else if (now - this.lastConfirmedAttack > SAME_ATTACK_NANOS) {
                return;
            }
            if (this.disableVanillaSound.isValue()) event.cancel();
        });
    }

    private void discardExpiredAttacks(long now) {
        while (!this.pendingAttacks.isEmpty()
                && now - this.pendingAttacks.getFirst() > ATTACK_SOUND_WINDOW_NANOS) {
            this.pendingAttacks.removeFirst();
        }
    }

    @Override
    public void deactivate() {
        this.pendingAttacks.clear();
        super.deactivate();
    }

    public void previewSound(HitSoundType hitSoundType) {
        WavSoundPlayer.INSTANCE.playSound("/" + getSoundPath(hitSoundType), this.volumeSetting.currentValue(), false);
    }

    public void playHitSound() {
        if (this.selectionMode.isSelected(SoundSelectionMode.CUSTOM)) {
            WavSoundPlayer.INSTANCE.playCustomSound(this.customSound.getText(),
                    this.volumeSetting.currentValue());
            return;
        }
        WavSoundPlayer.INSTANCE.playSound("/" + getSoundPath((HitSoundType) this.soundTypeSetting.currentValue()), this.volumeSetting.currentValue(), false);
    }

    public String getSoundPath(HitSoundType hitSoundType) {
        int i = 0;
        HitSoundType[] arrValues = HitSoundType.values();
        int length = arrValues.length;
        for (int i2 = 0; i2 < length && arrValues[i2] != hitSoundType; i2++) {
            i++;
        }
        return "hitsounds/hit" + (i + 1);
    }
}
