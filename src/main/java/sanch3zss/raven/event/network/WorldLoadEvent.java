package sanch3zss.raven.event.network;

import net.minecraft.world.level.Level;
import sanch3zss.raven.event.Event;

public final class WorldLoadEvent implements Event {
    public final Level world;

    public WorldLoadEvent(Level world) {
        this.world = world;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "world=" + this.world + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.world);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WorldLoadEvent)) return false;
        WorldLoadEvent o = (WorldLoadEvent) obj;
        return java.util.Objects.equals(this.world, o.world);
    }
public Level world() {
        return this.world;
    }
}
