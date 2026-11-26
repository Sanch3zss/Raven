package sanch3zss.raven.client.service;

import sanch3zss.raven.client.service.scheduling.ActionScheduler;

public final class ActionSequenceController {
    private static final ActionScheduler SCHEDULER = new ActionScheduler();

    public static void tick() {
        SCHEDULER.update().cleanupIfFinished();
    }

    public static void reset() {
        SCHEDULER.cleanup();
    }

    public static boolean isIdle() {
        return SCHEDULER.isFinished();
    }

    public static ActionScheduler sequence() {
        return SCHEDULER;
    }

    public static void submit(Runnable action) {
        if (isIdle()) {
            SCHEDULER.addTickStep(0, action::run);
        }
    }

    private ActionSequenceController() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
