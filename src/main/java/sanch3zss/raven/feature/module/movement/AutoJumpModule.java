package sanch3zss.raven.feature.module.movement;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.event.EventPriority;
import sanch3zss.raven.event.player.MovementInputEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

@Aliases(aliases = {"Auto Jump", "Auto Hop", "Jump Assist", "Jump Bot", "Automatic Jump", "Movement Jump"})
public class AutoJumpModule extends Module {
    public AutoJumpModule() {
        super(ModuleTab.PLAYER, "AutoJump");
        register(MovementInputEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (isState() && mc.isWorldLoaded()) {
                if (mc.getPlayer().onGround() || (mc.getPlayer().isInWater() && !mc.getPlayer().isUnderWater())) {
                    event.setJumping(true);
                }
            }
        }, EventPriority.LOW);
    }
}
