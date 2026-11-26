package sanch3zss.raven.feature.module.render;

import net.minecraft.world.entity.Entity;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.event.player.AttackEntityEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.ui.setting.NumberSetting;

@Aliases(aliases = {"HitParticles", "Hit Particles"})
public class HitParticlesModule extends ParticlesModule {
    public final NumberSetting hitAmount;
    public final NumberSetting spread;

    public HitParticlesModule() {
        super("HitParticles", false);
        this.hitAmount = new NumberSetting(Translation.clearText("Добавлять частиц за удар"))
                .range(5.0f, 150.0f).currentValue(40.0f).step(5.0f).unit(SettingUnit.UNITS);
        this.spread = new NumberSetting(Translation.clearText("Макс. разброс от удара"))
                .range(0.5f, 12.0f).currentValue(2.0f).step(0.5f).unit(SettingUnit.BLOCKS);
        addOmniSettings(this.hitAmount, this.spread);
        register(AttackEntityEvent.class, event -> {
            Entity target = event.attacker();
            if (isState() && target != null) {
                spawn(target, Math.round(this.hitAmount.currentValue()), this.spread.currentValue());
            }
        });
    }
}
