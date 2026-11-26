package sanch3zss.raven.feature.module.movement;

import net.minecraft.world.effect.MobEffects;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.event.*;
import sanch3zss.raven.event.player.MovementUpdateEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

@Aliases(aliases = {"Auto Sprint", "Sprint", "Auto Run", "Always Sprint", "Automatic Sprint", "Toggle Sprint"})
public class SprintModule extends Module {
    Mc mc;

    public SprintModule() {
        super(ModuleTab.PLAYER, "Sprint");
        this.mc = Mc.INSTANCE;
        register(MovementUpdateEvent.class, event2 -> {
            if (!isState() || !this.mc.isWorldLoaded() || !event2.getDirectionalInput().isMoving()
                    || !hasEnoughFood() || hasBlindness()) return;

            if (event2.getSource() == MovementUpdateSource.INPUT
                    || event2.getSource() == MovementUpdateSource.MOVEMENT_TICK) {
                event2.setSprint(true);
            }
        }, EventPriority.LOW);
    }

    public boolean hasEnoughFood() {
        return this.mc.getPlayer().getFoodData().getFoodLevel() > 6;
    }

    public boolean hasBlindness() {
        return this.mc.getPlayer().hasEffect(MobEffects.BLINDNESS);
    }
}
