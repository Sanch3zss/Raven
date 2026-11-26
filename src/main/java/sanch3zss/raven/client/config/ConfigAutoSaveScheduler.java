package sanch3zss.raven.client.config;

import java.util.concurrent.*;

import sanch3zss.raven.client.Raven;

public final class ConfigAutoSaveScheduler {
    public static final long autoSaveDelayMs = 1200;
    public static final Object lock = new Object();
    public static ScheduledFuture<?> pendingSave;

    public ConfigAutoSaveScheduler() {
    }

    public static void scheduleAutoSave() {
        if (Raven.INSTANCE == null || Raven.INSTANCE.configManager() == null) {
            return;
        }
        synchronized (lock) {
            if (pendingSave != null && !pendingSave.isDone()) {
                pendingSave.cancel(false);
            }
            pendingSave = Raven.INSTANCE.executor().schedule(() -> {
                try {
                    Raven.INSTANCE.configManager().saveLocalConfig();
                } catch (Exception th) {
                    Raven.LOGGER.error("Failed to auto-save local config", th);
                }
            }, autoSaveDelayMs, TimeUnit.MILLISECONDS);
        }
    }
}

