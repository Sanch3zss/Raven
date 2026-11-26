package sanch3zss.raven.client.config;

import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class GamePaths {
    private GamePaths() {
    }

    public static Path gameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static Path configDirectory() {
        return gameDirectory().resolve("raven").resolve("config");
    }
}
