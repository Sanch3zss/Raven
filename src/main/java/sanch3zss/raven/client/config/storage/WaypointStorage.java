package sanch3zss.raven.client.config.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.google.gson.*;

import sanch3zss.raven.client.config.io.AtomicFileWriter;
import sanch3zss.raven.client.config.repository.WaysRepository;

public class WaypointStorage {
    private static final List<LegacyEvent> LEGACY_EVENTS = List.of(
            new LegacyEvent("beacon", "Маяк-убийца", "Killer Beacon", "d"),
            new LegacyEvent("myst_beacon", "Загадочный маяк", "Mysterious Beacon", "d"),
            new LegacyEvent("altarundead", "Алтарь нежити", "Undead Altar", "e"),
            new LegacyEvent("airdrop", "Аирдроп", "Air Drop", "g"),
            new LegacyEvent("vulkan", "Вулкан", "Volcano", "h"),
            new LegacyEvent("meteor_rain", "Метеоритный дождь", "Meteor Shower", "b"),
            new LegacyEvent("hellm", "Адская резня", "Infernal Slaughter", "f"),
            new LegacyEvent("geyser", "Гейзер", "Geyser", "a"),
            new LegacyEvent("deathchest", "Сундук смерти", "Death Chest", "c"));
    public final Path path;
    public final WaysRepository repository;
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void save() throws IOException {
        AtomicFileWriter.writeBytes(this.path, gson.toJson(this.repository.getWaypoints()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    public void load() throws IOException {
        if (Files.exists(this.path, new LinkOption[0])) {
            Waypoint[] arr = (Waypoint[]) gson.fromJson(Files.readString(this.path, StandardCharsets.UTF_8), Waypoint[].class);
            Set<Waypoint> waypoints = this.repository.getWaypoints();
            waypoints.clear();
            for (Waypoint waypoint : arr) {
                waypoints.add(migrateLegacyEvent(waypoint));
            }
        }
    }

    private static Waypoint migrateLegacyEvent(Waypoint waypoint) {
        if (waypoint.icon() != null || waypoint.label() != null) {
            return waypoint;
        }
        for (LegacyEvent event : LEGACY_EVENTS) {
            for (String label : List.of(event.russian(), event.english())) {
                String prefix = label.replace(' ', '_') + "-";
                if (!waypoint.name().startsWith(prefix)) {
                    continue;
                }
                String server = waypoint.name().substring(prefix.length());
                if (server.startsWith("Анархия") || server.startsWith("Anarchy")) {
                    return new Waypoint(event.id() + "-" + server, waypoint.vector(),
                            event.glyph(), label);
                }
            }
        }
        return waypoint;
    }

    private record LegacyEvent(String id, String russian, String english, String glyph) {
    }

    public WaypointStorage(Path path, WaysRepository waysRepo) {
        this.path = path;
        this.repository = waysRepo;
    }
}
