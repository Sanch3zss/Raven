package sanch3zss.raven.feature.module.player;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.player.DeathTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.NumberSetting;

@Aliases(aliases = {"Auto Respawn", "Automatic Respawn", "Fast Respawn", "Quick Respawn", "Auto Revive"})
public class AutoRespawnModule extends Module {
    public final NumberSetting respawnDelay;

    public AutoRespawnModule() {
        super(ModuleTab.PLAYER, "AutoRespawn");
        this.respawnDelay = new NumberSetting(Lang.AUTORESPAWN_DELAY).currentValue(20.0f).range(0.0f, 70.0f).step(1.0f).unit(SettingUnit.TICKS);
        addSettings(this.respawnDelay);
        register(DeathTickEvent.class, event -> {
            Mc mc;
            LocalPlayer player;
            if (!isState() || event.ticksSinceDeath() <= Math.round(this.respawnDelay.currentValue()) || (player = (mc = Mc.INSTANCE).getPlayer()) == null) {
                return;
            }
            player.respawn();
            mc.setCurrentScreen((Screen) null);
        });
    }
}
