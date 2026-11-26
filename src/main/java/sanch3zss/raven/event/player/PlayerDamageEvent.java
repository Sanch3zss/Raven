package sanch3zss.raven.event.player;

import net.minecraft.world.entity.player.Player;
import sanch3zss.raven.event.Event;

public record PlayerDamageEvent(Player target, Player attacker) implements Event {
}
