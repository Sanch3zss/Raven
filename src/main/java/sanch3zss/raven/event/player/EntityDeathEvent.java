package sanch3zss.raven.event.player;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import sanch3zss.raven.event.Event;

public record EntityDeathEvent(LivingEntity entity, DamageSource source)
        implements Event {

    @Nullable
    public LivingEntity killerEntity() {
        return this.entity.getKillCredit();
    }
}
