package sanch3zss.raven.client.config;

public final class ConfigApplyContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private ConfigApplyContext() {
    }

    public static void begin() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void end() {
        int depth = DEPTH.get() - 1;
        if (depth <= 0) {
            DEPTH.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    public static boolean isApplying() {
        return DEPTH.get() > 0;
    }
}
