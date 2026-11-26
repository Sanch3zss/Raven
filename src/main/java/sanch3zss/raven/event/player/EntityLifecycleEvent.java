package sanch3zss.raven.event.player;

import net.minecraft.world.entity.Entity;
import sanch3zss.raven.event.Event;
import sanch3zss.raven.feature.entity.EntityLifecycleAction;

public final class EntityLifecycleEvent implements Event {
    public final Entity entity;
    public final EntityLifecycleAction type;

    public EntityLifecycleEvent(Entity entity, EntityLifecycleAction entityLifecycleAction) {
        this.entity = entity;
        this.type = entityLifecycleAction;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "entity=" + this.entity + ", " + "type=" + this.type + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.entity, this.type);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof EntityLifecycleEvent)) return false;
        EntityLifecycleEvent o = (EntityLifecycleEvent) obj;
        return java.util.Objects.equals(this.entity, o.entity) && java.util.Objects.equals(this.type, o.type);
    }
public Entity entity() {
        return this.entity;
    }

    public EntityLifecycleAction type() {
        return this.type;
    }
}
