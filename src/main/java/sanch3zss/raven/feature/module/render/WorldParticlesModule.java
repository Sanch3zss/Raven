package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.ui.setting.NumberSetting;

@Aliases(aliases = {"WorldParticles", "World Particles", "Particles", "Stars"})
public class WorldParticlesModule extends ParticlesModule {
    public final NumberSetting spawnCount;
    public final NumberSetting spread;

    public WorldParticlesModule() {
        super("WorldParticles", true);
        this.spawnCount = new NumberSetting(Translation.clearText("Добавлять за тик по миру"))
                .range(1.0f, 15.0f).currentValue(4.0f).step(1.0f).unit(SettingUnit.UNITS);
        this.spread = new NumberSetting(Translation.clearText("Макс. разброс по миру"))
                .range(1.0f, 24.0f).currentValue(10.0f).step(1.0f).unit(SettingUnit.BLOCKS);
        addOmniSettings(this.spawnCount, this.spread);
        register(ClientTickEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                spawn(Mc.INSTANCE.getPlayer(), Math.round(this.spawnCount.currentValue()),
                        this.spread.currentValue());
            }
        });
    }
}
