package sanch3zss.raven.mixin.client.render.entity;

import java.util.UUID;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import sanch3zss.raven.feature.module.render.KillEffectRenderState;
import sanch3zss.raven.feature.module.render.hitcolor.HitColorRenderState;
import sanch3zss.raven.feature.module.render.party.PartyRenderState;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerRenderState;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements PartyRenderState, LeaveTrackerRenderState, HitColorRenderState,
        KillEffectRenderState {
    @Unique private UUID raven$partyEntityId;
    @Unique private boolean raven$killEffectHidden;

    @Override
    public boolean raven$killEffectHidden() {
        return this.raven$killEffectHidden;
    }

    @Override
    public void raven$killEffectHidden(boolean hidden) {
        this.raven$killEffectHidden = hidden;
    }
    @Unique private boolean raven$leaveTrackerGhost;
    @Unique private long raven$leftAtNanos;
    @Unique private float raven$hurtProgress;

    @Override
    public float raven$hurtProgress() {
        return this.raven$hurtProgress;
    }

    @Override
    public void raven$hurtProgress(float progress) {
        this.raven$hurtProgress = progress;
    }

    @Override
    public UUID raven$partyEntityId() {
        return this.raven$partyEntityId;
    }

    @Override
    public void raven$partyEntityId(UUID id) {
        this.raven$partyEntityId = id;
    }

    @Override
    public boolean raven$isLeaveTrackerGhost() {
        return this.raven$leaveTrackerGhost;
    }

    @Override
    public void raven$setLeaveTrackerGhost(boolean value) {
        this.raven$leaveTrackerGhost = value;
    }

    @Override
    public long raven$leftAtNanos() {
        return this.raven$leftAtNanos;
    }

    @Override
    public void raven$setLeftAtNanos(long value) {
        this.raven$leftAtNanos = value;
    }
}
