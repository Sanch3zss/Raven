package sanch3zss.raven.client.config;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.io.AtomicFileWriter;
import sanch3zss.raven.client.config.repository.*;
import sanch3zss.raven.client.config.storage.*;

public class ConfigManager {
    public final Path configDir;
    private final Path namedConfigsDir;
    private volatile String activeConfigId = "";
    private volatile String activeConfigName = "Не выбран";
    public final MacroStorage macrosConfig;

    public final FriendStore friendsConfig;

    public final WaypointStorage waysConfig;

    public final CredentialStore autoAuthConfig;

    public final LocalPreferencesFile menuStateConfig;

    public ScheduledExecutorService executor;

    public ConfigManager(Path path, WaysRepository waysRepo, MacroRepository macroRepo) throws IOException {
        this.configDir = path;
        Files.createDirectories(path, new FileAttribute[0]);
        this.namedConfigsDir = path.resolve("configs");
        Files.createDirectories(this.namedConfigsDir);
        restoreActiveSelection();
        this.macrosConfig = new MacroStorage(path.resolve("macros.json"), macroRepo);
        this.friendsConfig = new FriendStore(path.resolve("friends.json"));
        this.waysConfig = new WaypointStorage(path.resolve("ways.json"), waysRepo);
        this.menuStateConfig = new LocalPreferencesFile(path.resolve("menu-state.json"));
        this.autoAuthConfig = new CredentialStore(path.resolve("auto-auth.json"));
    }

    public void saveMacros() throws IOException {
        this.macrosConfig.save();
    }

    public void loadMacros() throws IOException {
        this.macrosConfig.load();
    }

    public void saveWays() throws IOException {
        this.waysConfig.save();
    }

    public void loadWays() throws IOException {
        this.waysConfig.load();
    }

    public void saveFriends() throws IOException {
        this.friendsConfig.save();
    }

    public void loadFriends() throws IOException {
        this.friendsConfig.load();
    }

    public void saveMenuState() throws IOException {
        this.menuStateConfig.save();
    }

    public void loadMenuState() {
        this.menuStateConfig.load();
    }

    public void saveLocalConfig() {
        try {
            byte[] data = new ConfigSerializer().serialize(Raven.INSTANCE.moduleRepository(), Raven.INSTANCE.widgetStack());
            AtomicFileWriter.writeBytes(this.configDir.resolve("local-config.json"), data,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            if (!this.activeConfigId.isEmpty()
                    && !this.menuStateConfig.isAutoSaveDisabled(this.activeConfigId)) {
                writeSnapshot(namedPath(this.activeConfigId), this.activeConfigName);
            }
        } catch (Exception e) {
            Raven.LOGGER.error("Failed to save local config", e);
        }
    }

    public void loadLocalConfig() {
        try {
            Path path = this.configDir.resolve("local-config.json");
            if (!Files.exists(path)) {
                return;
            }
            ConfigSerializer serializer = new ConfigSerializer();
            ConfigFile bundle = serializer.deserialize(Files.readAllBytes(path));
            serializer.applyConfig(new LoadedConfig(bundle));
            Raven.LOGGER.info("Loaded local config ({} modules)", bundle.modules() == null ? 0 : bundle.modules().size());
        } catch (Exception e) {
            Raven.LOGGER.error("Failed to load local config", e);
        }
    }

    public List<NamedConfigInfo> listNamedConfigs() {
        List<NamedConfigInfo> result = new ArrayList<>();
        try (var files = Files.list(this.namedConfigsDir)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    ConfigFile config = new ConfigSerializer().deserialize(Files.readAllBytes(path));
                    String fileName = path.getFileName().toString();
                    String id = fileName.substring(0, fileName.length() - 5);
                    String name = normalizeName(config.displayName(), "Config");
                    result.add(new NamedConfigInfo(id, name, Files.getLastModifiedTime(path).toMillis()));
                } catch (Exception e) {
                    Raven.LOGGER.warn("Skipping invalid named config {}", path, e);
                }
            });
        } catch (IOException e) {
            Raven.LOGGER.error("Failed to list named configs", e);
        }
        result.sort(Comparator.comparingLong(NamedConfigInfo::lastModified).reversed());
        return result;
    }

    public String createNamedConfig(String requestedName) {
        String name = normalizeName(requestedName, "");
        if (name.isEmpty() || hasConfigNamed(name, null)) {
            return null;
        }
        String id = UUID.randomUUID().toString();
        if (!writeSnapshot(namedPath(id), name)) {
            return null;
        }
        this.activeConfigId = id;
        this.activeConfigName = name;
        writeActiveSelection();
        saveLocalConfig();
        return id;
    }

    public boolean loadNamedConfig(String id) {
        if ("default".equals(id)) {
            ConfigFile defaults = Raven.INSTANCE.defaultConfigFile();
            if (defaults == null) {
                return false;
            }
            new ConfigSerializer().applyConfig(new LoadedConfig(defaults));
            this.activeConfigId = "";
            this.activeConfigName = "Не выбран";
            writeActiveSelection();
            saveLocalConfig();
            return true;
        }
        try {
            Path path = namedPath(id);
            ConfigFile config = new ConfigSerializer().deserialize(Files.readAllBytes(path));
            new ConfigSerializer().applyConfig(new LoadedConfig(config));
            this.activeConfigId = id;
            this.activeConfigName = normalizeName(config.displayName(), "Config");
            writeActiveSelection();
            saveLocalConfig();
            return true;
        } catch (Exception e) {
            Raven.LOGGER.error("Failed to load named config {}", id, e);
            return false;
        }
    }

    public boolean saveNamedConfig(String id) {
        if ("default".equals(id)) {
            saveLocalConfig();
            return true;
        }
        NamedConfigInfo info = findNamedConfig(id);
        return info != null && writeSnapshot(namedPath(id), info.name());
    }

    public boolean deleteNamedConfig(String id) {
        if ("default".equals(id)) {
            return false;
        }
        try {
            boolean deleted = Files.deleteIfExists(namedPath(id));
            if (deleted && id.equals(this.activeConfigId)) {
                this.activeConfigId = "";
                this.activeConfigName = "Не выбран";
                writeActiveSelection();
            }
            return deleted;
        } catch (IOException | IllegalArgumentException e) {
            Raven.LOGGER.error("Failed to delete named config {}", id, e);
            return false;
        }
    }

    public boolean renameNamedConfig(String id, String requestedName) {
        String name = normalizeName(requestedName, "");
        if ("default".equals(id) || name.isEmpty() || hasConfigNamed(name, id)) {
            return false;
        }
        try {
            Path path = namedPath(id);
            ConfigSerializer serializer = new ConfigSerializer();
            ConfigFile config = serializer.deserialize(Files.readAllBytes(path)).displayName(name);
            AtomicFileWriter.writeBytes(path, serializer.serialize(config), StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            if (id.equals(this.activeConfigId)) {
                this.activeConfigName = name;
                writeActiveSelection();
            }
            return true;
        } catch (Exception e) {
            Raven.LOGGER.error("Failed to rename named config {}", id, e);
            return false;
        }
    }

    private boolean writeSnapshot(Path path, String name) {
        try {
            ConfigSerializer serializer = new ConfigSerializer();
            ConfigFile config = serializer.buildConfigFile(Raven.INSTANCE.moduleRepository(), Raven.INSTANCE.widgetStack())
                    .displayName(name);
            AtomicFileWriter.writeBytes(path, serializer.serialize(config), StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            return true;
        } catch (Exception e) {
            Raven.LOGGER.error("Failed to save named config {}", path, e);
            return false;
        }
    }

    private NamedConfigInfo findNamedConfig(String id) {
        return listNamedConfigs().stream().filter(info -> info.id().equals(id)).findFirst().orElse(null);
    }

    private boolean hasConfigNamed(String name, String exceptId) {
        return listNamedConfigs().stream().anyMatch(info -> !info.id().equals(exceptId)
                && info.name().equalsIgnoreCase(name));
    }

    private Path namedPath(String id) {
        if (id == null || !id.matches("[0-9a-fA-F-]{36}")) {
            throw new IllegalArgumentException("Invalid config id");
        }
        return this.namedConfigsDir.resolve(id.toLowerCase(Locale.ROOT) + ".json");
    }

    private void restoreActiveSelection() {
        Path marker = this.configDir.resolve("active-config.txt");
        try {
            if (!Files.exists(marker)) {
                return;
            }
            String id = Files.readString(marker).trim();
            if (id.isEmpty()) {
                return;
            }
            Path path = namedPath(id);
            if (!Files.exists(path)) {
                return;
            }
            ConfigFile config = new ConfigSerializer().deserialize(Files.readAllBytes(path));
            this.activeConfigId = id;
            this.activeConfigName = normalizeName(config.displayName(), "Config");
        } catch (Exception e) {
            Raven.LOGGER.warn("Failed to restore active config selection", e);
        }
    }

    private void writeActiveSelection() {
        try {
            AtomicFileWriter.writeBytes(this.configDir.resolve("active-config.txt"),
                    this.activeConfigId.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
        } catch (IOException e) {
            Raven.LOGGER.warn("Failed to persist active config selection", e);
        }
    }

    private static String normalizeName(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String name = value.trim();
        return name.isEmpty() ? fallback : name.substring(0, Math.min(name.length(), 50));
    }

    public String activeConfigId() {
        return this.activeConfigId;
    }

    public String activeConfigName() {
        return this.activeConfigName;
    }

    public record NamedConfigInfo(String id, String name, long lastModified) {
    }

    public void saveAutoAuth(Map<CredentialKey, String> map) throws IOException {
        this.autoAuthConfig.saveCredentials(map);
    }

    public Map<CredentialKey, String> loadAutoAuth() throws IOException {
        return this.autoAuthConfig.loadCredentials();
    }

    public Path configDir() {
        return this.configDir;
    }

    public MacroStorage macrosConfig() {
        return this.macrosConfig;
    }

    public FriendStore friendsConfig() {
        return this.friendsConfig;
    }

    public WaypointStorage waysConfig() {
        return this.waysConfig;
    }

    public CredentialStore autoAuthConfig() {
        return this.autoAuthConfig;
    }

    public ScheduledExecutorService executor() {
        return this.executor;
    }

    public LocalPreferencesFile menuStateConfig() {
        return this.menuStateConfig;
    }
}
