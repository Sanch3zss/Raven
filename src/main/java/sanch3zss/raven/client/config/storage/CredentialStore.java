package sanch3zss.raven.client.config.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.google.gson.*;

import sanch3zss.raven.client.config.CredentialKey;
import sanch3zss.raven.client.config.GamePaths;
import sanch3zss.raven.client.config.io.AtomicFileWriter;
import sanch3zss.raven.client.config.serialization.CredentialListTypeToken;
import sanch3zss.raven.client.security.AesCipherUtil;

public class CredentialStore {
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    static final Path legacyFile = GamePaths.gameDirectory().resolve("autoauth_credentials.txt");

    public final Path path;

    public Path path() {
        return this.path;
    }

    public Map<CredentialKey, String> loadCredentials() throws IOException {
        List<StoredCredential> list;
        if (!Files.exists(this.path)) {
            Map<CredentialKey, String> legacyCredentials = loadLegacyCredentials();
            if (!legacyCredentials.isEmpty()) {
                saveCredentials(legacyCredentials);
            }
            return legacyCredentials;
        }
        String string = Files.readString(this.path, StandardCharsets.UTF_8);
        if (!string.isBlank() && (list = (List) gson.fromJson(string, new CredentialListTypeToken(this).getType())) != null) {
            Map<CredentialKey, String> map = new HashMap<>();
            for (StoredCredential storedCredential : list) {
                if (storedCredential != null && storedCredential.serverAddress != null && storedCredential.username != null && storedCredential.password != null) {
                    map.put(new CredentialKey(storedCredential.serverAddress, storedCredential.username), AesCipherUtil.decrypt(storedCredential.password));
                }
            }
            return map;
        }
        return Map.of();
    }

    public void saveCredentials(Map<CredentialKey, String> map) throws IOException {
        List<StoredCredential> credentials = new ArrayList<>();
        for (Map.Entry<CredentialKey, String> entry : map.entrySet()) {
            CredentialKey key = entry.getKey();
            if (key != null && entry.getValue() != null) {
                credentials.add(new StoredCredential(key.serverAddress(), key.username(), AesCipherUtil.encrypt(entry.getValue())));
            }
        }
        AtomicFileWriter.writeBytes(this.path, gson.toJson(credentials).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    public Map<CredentialKey, String> loadLegacyCredentials() throws IOException {
        if (!Files.exists(legacyFile)) {
            return Map.of();
        }
        Map<CredentialKey, String> map = new HashMap<>();
        for (String line : Files.readAllLines(legacyFile, StandardCharsets.UTF_8)) {
            String[] parts = line.split(":", 3);
            if (parts.length >= 3) {
                map.put(new CredentialKey(parts[0], parts[1]), AesCipherUtil.decrypt(parts[2]));
            }
        }
        return map;
    }

    public CredentialStore(Path path) {
        this.path = path;
    }
}
