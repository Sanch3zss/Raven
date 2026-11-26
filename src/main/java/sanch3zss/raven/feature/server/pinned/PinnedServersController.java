package sanch3zss.raven.feature.server.pinned;

import java.util.*;

import sanch3zss.raven.client.service.scheduling.PinnedServerEntry;

public class PinnedServersController {
    public final List<PinnedServerEntry> favorites = new ArrayList<>();

    public void init() {
    }

    public boolean isPinned(String str) {
        String targetAddress = normalizeAddress(str);
        return this.favorites.stream().anyMatch(entry -> normalizeAddress(entry.address()).equals(targetAddress));
    }

    public void addFavorite(PinnedServerEntry pinnedServerEntry) {
        this.favorites.add(pinnedServerEntry);
    }

    public static String normalizeAddress(String str) {
        if (str == null) {
            return "";
        }
        String strTrim = str.trim();
        return !strTrim.contains(":") ? strTrim + ":25565" : strTrim;
    }

    public List<PinnedServerEntry> favorites() {
        return List.copyOf(this.favorites);
    }
}
