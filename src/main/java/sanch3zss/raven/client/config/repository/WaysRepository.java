package sanch3zss.raven.client.config.repository;

import java.io.IOException;
import java.util.*;

import org.joml.Vector3i;
import org.slf4j.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.ConfigErrorNotice;
import sanch3zss.raven.client.config.storage.Waypoint;
import sanch3zss.raven.client.chat.ChatUtil;

public class WaysRepository {
    public final Set<Waypoint> waypoints = new HashSet<>();
    static final Logger logger = LoggerFactory.getLogger(WaysRepository.class);

    public void add(String str, Vector3i vector3i) {
        add(str, vector3i, null);
    }

    public void add(String str, Vector3i vector3i, String icon) {
        add(str, vector3i, icon, null);
    }

    public void add(String str, Vector3i vector3i, String icon, String label) {
        add(new Waypoint(str, vector3i, icon, label));
    }

    public void add(Waypoint waypoint) {
        this.waypoints.add(waypoint);
        try {
            Raven.INSTANCE.configManager().saveWays();
        } catch (IOException e) {
            logger.error("Failed to persist ways after add(name={})", waypoint.name(), e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("сохранении точки"));
        }
    }

    public void replace(Map<Waypoint, Waypoint> changes) {
        if (changes.isEmpty()) {
            return;
        }
        this.waypoints.removeAll(changes.keySet());
        this.waypoints.addAll(changes.values());
        try {
            Raven.INSTANCE.configManager().saveWays();
        } catch (IOException e) {
            logger.error("Failed to persist ways after relocation", e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("переносе точек"));
        }
    }

    public boolean remove(String str) {
        if (!this.waypoints.removeIf(waypoint -> waypoint.name().equalsIgnoreCase(str))) {
            return false;
        }
        try {
            Raven.INSTANCE.configManager().saveWays();
            return true;
        } catch (IOException e) {
            logger.error("Failed to persist ways after remove(name={})", str, e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("удалении точки"));
            return true;
        }
    }

    public boolean isEmpty() {
        return this.waypoints.isEmpty();
    }

    public boolean hasWaypoint(String str) {
        for (Waypoint waypoint : this.waypoints) {
            if (waypoint.name().equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    public void clearList() {
        if (this.waypoints.isEmpty()) {
            return;
        }
        this.waypoints.clear();
        try {
            Raven.INSTANCE.configManager().saveWays();
        } catch (IOException e) {
            logger.error("Failed to persist ways after clear()", e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("очистке путей"));
        }
    }

    public Set<Waypoint> getWaypoints() {
        return this.waypoints;
    }
}
