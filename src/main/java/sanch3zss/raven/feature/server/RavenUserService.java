package sanch3zss.raven.feature.server;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import okhttp3.*;

import sanch3zss.raven.client.Raven;

public final class RavenUserService {
    private static final String PROJECT_URL = "https://raqkmlmbkayvhbxypxau.supabase.co";
    private static final String PUBLISHABLE_KEY = "sb_publishable_kuFdD7qhMUZ73Dp2t9sVpQ_3NNQWVAS";
    private static final MediaType JSON = MediaType.get("application/json");
    private static final long REFRESH_INTERVAL_SECONDS = 60L;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5L, TimeUnit.SECONDS)
            .readTimeout(8L, TimeUnit.SECONDS)
            .callTimeout(10L, TimeUnit.SECONDS)
            .build();
    private final AtomicBoolean refreshing = new AtomicBoolean();
    private volatile Set<String> usernames = Set.of();

    public void init() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.getUser() != null) {
                String username = client.getUser().getName();
                Raven.INSTANCE.executor().execute(() -> {
                    call("raven_heartbeat", username);
                    refreshNow();
                });
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (client.getUser() != null) {
                String username = client.getUser().getName();
                Raven.INSTANCE.executor().execute(() -> call("raven_leave", username));
            }
        });
        refresh();
        Raven.INSTANCE.executor().scheduleWithFixedDelay(this::refresh,
                REFRESH_INTERVAL_SECONDS, REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public boolean contains(String username) {
        return username != null && this.usernames.contains(normalize(username));
    }

    private void call(String function, String username) {
        String normalized = normalize(username);
        if (!valid(normalized)) {
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("p_username", normalized);
        Request request = request("/rest/v1/rpc/" + function)
                .post(RequestBody.create(payload.toString(), JSON))
                .build();
        try (Response response = this.client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Raven.LOGGER.debug("Raven {} returned HTTP {}", function, response.code());
            }
        } catch (IOException exception) {
            Raven.LOGGER.debug("Unable to call {}", function, exception);
        }
    }

    private void refresh() {
        if (!this.refreshing.compareAndSet(false, true)) {
            return;
        }
        Raven.INSTANCE.executor().execute(() -> {
            try {
                Minecraft client = Minecraft.getInstance();
                if (client.getConnection() != null && client.getUser() != null) {
                    call("raven_heartbeat", client.getUser().getName());
                }
                refreshNow();
            } finally {
                this.refreshing.set(false);
            }
        });
    }

    private void refreshNow() {
        Request request = request("/rest/v1/raven_online_users?select=username")
                .get()
                .build();
        try (Response response = this.client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                Raven.LOGGER.debug("Raven user list returned HTTP {}", response.code());
                return;
            }
            JsonElement root = JsonParser.parseString(response.body().string());
            if (!root.isJsonArray()) {
                return;
            }
            Set<String> loaded = new HashSet<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonElement username = element.getAsJsonObject().get("username");
                if (username == null || !username.isJsonPrimitive()) {
                    continue;
                }
                String normalized = normalize(username.getAsString());
                if (valid(normalized)) {
                    loaded.add(normalized);
                }
            }
            this.usernames = Set.copyOf(loaded);
        } catch (IOException | RuntimeException exception) {
            Raven.LOGGER.debug("Unable to refresh Raven users", exception);
        }
    }

    private Request.Builder request(String path) {
        return new Request.Builder()
                .url(PROJECT_URL + path)
                .header("apikey", PUBLISHABLE_KEY)
                .header("Authorization", "Bearer " + PUBLISHABLE_KEY)
                .header("Accept", "application/json");
    }

    private static boolean valid(String username) {
        return username.matches("[a-z0-9_]{3,16}");
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
