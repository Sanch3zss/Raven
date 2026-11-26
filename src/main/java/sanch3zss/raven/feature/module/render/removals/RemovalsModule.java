package sanch3zss.raven.feature.module.render.removals;

import net.minecraft.client.renderer.LevelRenderer;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.client.SoundPlayEvent;
import sanch3zss.raven.event.render.*;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.feature.world.*;
import sanch3zss.raven.event.render.RenderOverlayType;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"No Render", "Anti Render", "Removals", "Remove", "No Overlay", "Hide Overlay", "No Hurt Cam", "No Fire", "No Scoreboard", "Anti Hurt Cam"})
public class RemovalsModule extends Module {
    public final MultiSelectSetting<RemovedVisualType> removedVisuals;
    public final MultiSelectSetting<RemovedSoundCategory> removedSounds;
    public final NumberSetting soundVolume;
    public RemovalsModule() {
        super(ModuleTab.VISUALS, "Removals");
        this.removedVisuals = new MultiSelectSetting(Lang.REMOVALS_ELEMENTS).values(RemovedVisualType.class);
        this.removedSounds = new MultiSelectSetting(Lang.REMOVALS_ANNOYING_SOUNDS).values(RemovedSoundCategory.class);
        this.soundVolume = new NumberSetting(Lang.REMOVALS_SOUND_VOLUME).currentValue(0.0f).range(0.0f, 100.0f).step(1.0f).unit(SettingUnit.PERCENTS).visible(() -> {
            return Boolean.valueOf(!this.removedSounds.selectedValues().isEmpty());
        });
        addSettings(this.removedVisuals, this.removedSounds, this.soundVolume);
        register(RenderOverlayEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded() && isState()) {
                RenderOverlayType type = event.getType();
                if ((type.isCameraHurt() && removes(RemovedVisualType.CAMERA_HURT))
                        || (type.isFireOverlay() && removes(RemovedVisualType.FIRE_OVERLAY))
                        || (type.isLavaOverlay() && removes(RemovedVisualType.LAVA_OVERLAY))
                        || (type.isWaterOverlay() && removes(RemovedVisualType.WATER_OVERLAY))
                        || (type.isVignette() && removes(RemovedVisualType.VIGNETTE))
                        || (type.isScoreboard() && removes(RemovedVisualType.SCOREBOARD))
                        || (type.isBossBar() && removes(RemovedVisualType.BOSS_BAR))
                        || (type.isTotemPop() && removes(RemovedVisualType.TOTEM_POP))
                        || (type.isGlowing() && removes(RemovedVisualType.GLOWING))
                        || (type.isWitherHearts() && removes(RemovedVisualType.WITHER_HEARTS))) {
                    event.cancel();
                }
            }
        });
        register(SoundPlayEvent.class, event3 -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                String path = event3.getSoundInstance().getIdentifier().getPath();
                if (this.removedSounds.selectedValues().stream().anyMatch(value -> {
                    return value.matches(path);
                })) {
                    float currentValue = this.soundVolume.currentValue();
                    if (currentValue <= 0.0f) {
                        event3.cancel();
                    } else {
                        event3.setVolumeMultiplier(currentValue / 100.0f);
                    }
                }
            }
        });
    }

    public boolean removes(RemovedVisualType type) {
        return isState() && this.removedVisuals.isSelected(type);
    }

    @Override
    public void activate() {
        super.activate();
        reload();
    }

    @Override
    public void deactivate() {
        reload();
        super.deactivate();
    }

    public void reload() {
        if (Mc.INSTANCE.isWorldLoaded() && isState()) {
            net.minecraft.client.Minecraft.getInstance().levelExtractor.allChanged();
        }
    }
}
