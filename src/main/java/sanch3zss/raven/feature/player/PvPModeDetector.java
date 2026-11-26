package sanch3zss.raven.feature.player;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.util.math.interpolation.Stopwatch;

public class PvPModeDetector {
    public static final Stopwatch pvpStopwatch = new Stopwatch();

    public static boolean isPvPMode() {
        return !pvpStopwatch.hasElapsed(500L);
    }

    public static void tick() {
        if (hasPvpBossBar()) {
            pvpStopwatch.reset();
        }
    }

    public static boolean hasPvpBossBar() {
        return Mc.INSTANCE.getInGameHud().getBossOverlay().events.values().stream().map(clientBossBar -> {
            return clientBossBar.getName().getString().toLowerCase();
        }).anyMatch(str -> {
            return str.contains("pvp") || str.contains("пвп");
        });
    }
}
