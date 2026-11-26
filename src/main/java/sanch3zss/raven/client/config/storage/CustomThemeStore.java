package sanch3zss.raven.client.config.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.io.AtomicFileWriter;

public final class CustomThemeStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomThemeStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final List<SavedTheme> themes = new ArrayList<>();
    private boolean loaded;

    public List<SavedTheme> themes() {
        if (!this.loaded) {
            this.loaded = true;
            load();
        }
        return this.themes;
    }

    public void add(SavedTheme theme) {
        themes().removeIf(saved -> saved.name.equalsIgnoreCase(theme.name));
        this.themes.add(theme);
        save();
    }

    public void remove(SavedTheme theme) {
        if (themes().remove(theme)) {
            save();
        }
    }

    private Path path() {
        return Raven.INSTANCE.configManager().configDir().resolve("themes.json");
    }

    private void load() {
        try {
            Path path = path();
            if (!Files.exists(path)) {
                return;
            }
            List<SavedTheme> saved = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8),
                    new TypeToken<List<SavedTheme>>() {}.getType());
            if (saved != null) {
                saved.removeIf(theme -> theme == null || theme.name == null);
                this.themes.addAll(saved);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load custom themes", e);
        }
    }

    private void save() {
        try {
            AtomicFileWriter.writeBytes(path(), GSON.toJson(this.themes).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            LOGGER.error("Failed to save custom themes", e);
        }
    }

    public static final class SavedTheme {
        public final String name;
        public final int first;
        public final int second;
        public final String mode;

        public SavedTheme(String name, int first, int second, String mode) {
            this.name = name;
            this.first = first;
            this.second = second;
            this.mode = mode;
        }
    }
}
