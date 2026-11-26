package sanch3zss.raven.client.service.scheduling;

import java.util.*;
import java.util.function.BooleanSupplier;

public final class ActionScheduler {
    private final List<TickAction> actions = new ArrayList<>();
    private int currentActionIndex;
    private int currentTick;

    public ActionScheduler addTickStep(int tick, Runnable action) {
        return addTickStep(tick, action, () -> true);
    }

    public ActionScheduler addTickStep(int tick, Runnable action, BooleanSupplier condition) {
        this.actions.add(new TickAction(tick, action, condition));
        this.actions.sort(Comparator.comparingInt(TickAction::tick));
        return this;
    }

    public ActionScheduler update() {
        if (isFinished()) {
            return this;
        }

        TickAction action = this.actions.get(this.currentActionIndex);
        if (action.tick() <= this.currentTick && action.condition().getAsBoolean()) {
            action.action().run();
            this.currentActionIndex++;
        }
        this.currentTick++;
        return this;
    }

    public ActionScheduler cleanupIfFinished() {
        if (isFinished()) {
            cleanup();
        }
        return this;
    }

    public ActionScheduler cleanup() {
        this.actions.clear();
        this.currentActionIndex = 0;
        this.currentTick = 0;
        return this;
    }

    public boolean isFinished() {
        return this.currentActionIndex >= this.actions.size();
    }

    private record TickAction(int tick, Runnable action, BooleanSupplier condition) {
    }
}
