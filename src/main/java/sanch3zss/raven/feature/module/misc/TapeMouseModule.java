package sanch3zss.raven.feature.module.misc;

import java.util.concurrent.TimeUnit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.misc.TapeMouseHand;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.interpolation.Stopwatch;

@Aliases(aliases = {"Tape Mouse", "Auto Click", "Click Assist", "Clicker", "Mouse Automation", "Auto Tap", "Hand Clicker", "Auto Hand Use"})
public class TapeMouseModule extends Module {
    public final ModeSetting<TapeMouseHand> handMode;
    public final NumberSetting delay;
    public final Stopwatch stopwatch;
    public boolean usePressed;

    public TapeMouseModule() {
        super(ModuleTab.MISC, "TapeMouse");
        this.handMode = new ModeSetting(Lang.TAPEMOUSE_HANDMODE).values(TapeMouseHand.class);
        this.delay = new NumberSetting(Lang.TAPEMOUSE_DELAY).currentValue(1.0f).range(0.0f, 30.0f).step(0.5f).unit(SettingUnit.SECONDS);
        this.stopwatch = new Stopwatch(false);
        this.usePressed = false;
        addSettings(this.handMode, this.delay);
        register(PlayerTickEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (isState() && mc.isWorldLoaded() && event.isPre()) {
                Options gameOptions = mc.getGameOptions();
                Minecraft minecraftClient = Minecraft.getInstance();
                if (this.handMode.isSelected(TapeMouseHand.LEFT)) {
                    if (this.stopwatch.hasElapsed(Math.round(this.delay.currentValue() * 1000.0f), TimeUnit.MILLISECONDS)) {
                        minecraftClient.startAttack();
                        this.stopwatch.reset();
                    }
                } else if (this.stopwatch.hasElapsed(Math.round(this.delay.currentValue() * 1000.0f), TimeUnit.MILLISECONDS)) {
                    gameOptions.keyUse.setDown(true);
                    this.usePressed = true;
                    this.stopwatch.reset();
                }
                if (this.usePressed) {
                    gameOptions.keyUse.setDown(false);
                    this.usePressed = false;
                }
            }
        });
    }

    @Override
    public void deactivate() {
        Mc mc = Mc.INSTANCE;
        if (mc.isWorldLoaded() && this.usePressed) {
            mc.getGameOptions().keyUse.setDown(false);
            this.usePressed = false;
        }
        super.deactivate();
    }
}
