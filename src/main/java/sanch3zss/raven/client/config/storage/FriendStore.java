package sanch3zss.raven.client.config.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import com.google.gson.*;

import sanch3zss.raven.client.config.io.AtomicFileWriter;
import sanch3zss.raven.feature.friend.FriendManager;

public class FriendStore {
    public final Path filePath;
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public void save() throws IOException {
        AtomicFileWriter.writeBytes(this.filePath, gson.toJson(FriendManager.entries()).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    public void load() throws IOException {
        if (Files.exists(this.filePath)) {
            JsonElement root = JsonParser.parseString(Files.readString(this.filePath, StandardCharsets.UTF_8));
            FriendManager.clear();
            if (root == null || !root.isJsonArray()) {
                return;
            }
            for (JsonElement element : root.getAsJsonArray()) {
                if (element.isJsonPrimitive()) {
                    FriendManager.load(element.getAsString(), null);
                } else if (element.isJsonObject() && element.getAsJsonObject().has("name")) {
                    JsonObject object = element.getAsJsonObject();
                    FriendManager.load(object.get("name").getAsString(),
                            object.has("alias") ? object.get("alias").getAsString() : null);
                }
            }
        }
    }

    public FriendStore(Path path) {
        this.filePath = path;
    }
}
