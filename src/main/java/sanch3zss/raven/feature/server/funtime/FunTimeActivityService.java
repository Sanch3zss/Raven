package sanch3zss.raven.feature.server.funtime;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.*;
import okhttp3.*;

import sanch3zss.raven.client.Raven;

public final class FunTimeActivityService {
    private static final String MINES_URL =
            "https://funtime.me/api/backend/api/v1/plugin/mines?server-types=all";
    private static final String EVENTS_URL =
            "https://funtime.me/api/backend/api/v1/events?event-type=all&server-type=";
    private static final long REFRESH_INTERVAL_MS = 15_000L;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(7L, TimeUnit.SECONDS)
            .readTimeout(12L, TimeUnit.SECONDS)
            .callTimeout(15L, TimeUnit.SECONDS)
            .build();
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Snapshot snapshot = Snapshot.empty();
    private volatile long lastRefreshAttempt;

    public Snapshot snapshot() {
        return this.snapshot;
    }

    public void refreshIfStale() {
        long now = System.currentTimeMillis();
        if (now - this.lastRefreshAttempt < REFRESH_INTERVAL_MS
                || !this.refreshing.compareAndSet(false, true)) {
            return;
        }
        this.lastRefreshAttempt = now;
        Snapshot current = this.snapshot;
        this.snapshot = current.withLoading(true);
        Raven.INSTANCE.executor().execute(() -> {
            try {
                List<Mine> mines = parseMines(fetch(MINES_URL));
                List<Event> events = parseEvents(fetch(EVENTS_URL));
                this.snapshot = new Snapshot(List.copyOf(mines), List.copyOf(events),
                        System.currentTimeMillis(), false, "");
            } catch (Throwable throwable) {
                String message = throwable.getMessage();
                if (message == null || message.isBlank()) {
                    message = throwable.getClass().getSimpleName();
                }
                this.snapshot = new Snapshot(current.mines(), current.events(), current.syncedAt(),
                        false, message);
            } finally {
                this.refreshing.set(false);
            }
        });
    }

    private String fetch(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json, text/plain, */*")
                .header("Accept-Language", "ru,en;q=0.9")
                .header("Referer", "https://funtime.me/")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 Chrome/131.0 Safari/537.36")
                .build();
        try (Response response = this.client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("FunTime HTTP " + response.code());
            }
            return response.body().string();
        }
    }

    private static List<Mine> parseMines(String payload) {
        JsonObject root = JsonParser.parseString(payload).getAsJsonObject();
        JsonObject servers = object(root, "servers");
        List<Mine> result = new ArrayList<>();
        if (servers == null) {
            return result;
        }
        for (var serverEntry : servers.entrySet()) {
            if (!serverEntry.getValue().isJsonArray()) {
                continue;
            }
            for (JsonElement element : serverEntry.getValue().getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject mine = element.getAsJsonObject();
                String server = text(mine, "server-ru-name",
                        formatServerName(serverEntry.getKey()));
                result.add(new Mine(server, text(mine, "mine-name", "mine"),
                        Rarity.from(text(mine, "mine-rarity", "default")),
                        Rarity.from(text(mine, "next-mine-rarity", "default")),
                        normalizedSeconds(mine, "reset-seconds-left")));
            }
        }
        return result;
    }

    private static List<Event> parseEvents(String payload) {
        JsonElement root = JsonParser.parseString(payload);
        JsonArray servers = null;
        if (root.isJsonObject()) {
            JsonElement response = root.getAsJsonObject().get("response");
            if (response != null && response.isJsonArray()) {
                servers = response.getAsJsonArray();
            }
        } else if (root.isJsonArray()) {
            servers = root.getAsJsonArray();
        }
        List<Event> result = new ArrayList<>();
        if (servers == null) {
            return result;
        }
        for (JsonElement serverElement : servers) {
            if (!serverElement.isJsonObject()) {
                continue;
            }
            JsonObject serverObject = serverElement.getAsJsonObject();
            String server = text(serverObject, "server",
                    text(serverObject, "server_name", ""));
            JsonArray events = array(serverObject, "events");
            if (events == null) {
                continue;
            }
            for (JsonElement eventElement : events) {
                if (!eventElement.isJsonObject()) {
                    continue;
                }
                JsonObject event = eventElement.getAsJsonObject();
                String id = text(event, "id", "");
                if (id.isBlank()) {
                    continue;
                }
                JsonObject location = object(event, "location-event");
                Integer x = location == null ? null : nullableInteger(location, "x");
                Integer y = location == null ? null : nullableInteger(location, "y");
                Integer z = location == null ? null : nullableInteger(location, "z");
                String loot = text(event, "loot", "");
                if ("null".equalsIgnoreCase(loot)) {
                    loot = "";
                }
                result.add(new Event(formatServerName(server), id,
                        text(event, "phase", ""), loot,
                        normalizedSeconds(event, "time-seconds-left"), x, y, z));
            }
        }
        return result;
    }

    private static JsonObject object(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : null;
    }

    private static String text(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return fallback;
        }
        try {
            return value.getAsString();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int normalizedSeconds(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return 0;
        }
        String raw;
        try {
            raw = value.getAsString().trim();
            double numeric = Double.parseDouble(raw);
            double seconds;
            if (numeric > 1_000_000_000_000.0d) {
                seconds = (numeric - System.currentTimeMillis()) / 1000.0d;
            } else if (numeric > 1_000_000_000.0d) {
                seconds = numeric - System.currentTimeMillis() / 1000.0d;
            } else {
                seconds = numeric;
            }
            return (int) Math.max(0.0d, Math.floor(seconds));
        } catch (NumberFormatException ignored) {
            try {
                long millis = Instant.parse(value.getAsString()).toEpochMilli()
                        - System.currentTimeMillis();
                return (int) Math.max(0L, millis / 1000L);
            } catch (DateTimeParseException | UnsupportedOperationException ignoredDate) {
                return 0;
            }
        }
    }

    private static Integer nullableInteger(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()) {
            return null;
        }
        try {
            return value.getAsInt();
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String formatServerName(String server) {
        if (server == null || server.isBlank()) {
            return "FunTime";
        }
        String lower = server.toLowerCase(Locale.ROOT);
        if (lower.startsWith("anarchy")) {
            return "Анархия-" + server.substring("anarchy".length());
        }
        return server;
    }

    public enum Rarity {
        DEFAULT,
        MYTHICAL,
        LEGENDARY;

        public static Rarity from(String value) {
            if (value == null) {
                return DEFAULT;
            }
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.contains("legend")) {
                return LEGENDARY;
            }
            if (normalized.contains("myth")) {
                return MYTHICAL;
            }
            return DEFAULT;
        }
    }

    public record Mine(String server, String name, Rarity rarity, Rarity nextRarity,
                       int secondsLeft) {
    }

    public record Event(String server, String id, String phase, String loot, int secondsLeft,
                        Integer x, Integer y, Integer z) {
        public boolean hasLocation() {
            return this.x != null && this.y != null && this.z != null;
        }
    }

    public record Snapshot(List<Mine> mines, List<Event> events, long syncedAt,
                           boolean loading, String error) {
        private static Snapshot empty() {
            return new Snapshot(List.of(), List.of(), 0L, false, "");
        }

        private Snapshot withLoading(boolean value) {
            return new Snapshot(this.mines, this.events, this.syncedAt, value, this.error);
        }

        public int remainingSeconds(int sourceSeconds) {
            if (this.syncedAt <= 0L) {
                return Math.max(0, sourceSeconds);
            }
            long elapsed = Math.max(0L, (System.currentTimeMillis() - this.syncedAt) / 1000L);
            return (int) Math.max(0L, sourceSeconds - elapsed);
        }
    }
}
