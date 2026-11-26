package sanch3zss.raven.client.service.scheduling;

import java.util.PriorityQueue;

import sanch3zss.raven.feature.module.Module;

public class TickScheduler<T> {
    public int tickCounter = 0;
    public final PriorityQueue<ScheduledTask<T>> activeTasks = new PriorityQueue<>((scheduledTask, value) -> {
        return Integer.compare(value.priority, scheduledTask.priority);
    });

    public void tick(int i) {
        this.tickCounter += i;
    }

    public void addTask(ScheduledTask<T> scheduledTask) {
        this.activeTasks.removeIf(value -> {
            return value.provider.equals(scheduledTask.provider);
        });
        ((ScheduledTask) scheduledTask).expiresIn += this.tickCounter;
        this.activeTasks.add(scheduledTask);
    }

    public T getValue() {
        while (!this.activeTasks.isEmpty() && this.activeTasks.peek() != null && (((ScheduledTask) this.activeTasks.peek()).expiresIn <= this.tickCounter || !((ScheduledTask) this.activeTasks.peek()).provider.isState())) {
            this.activeTasks.poll();
        }
        if (this.activeTasks.isEmpty() || this.activeTasks.peek() == null) {
            return null;
        }
        return ((ScheduledTask<T>) this.activeTasks.peek()).value;
    }

    public void cancel(Module module) {
        this.activeTasks.removeIf(scheduledTask -> {
            return scheduledTask.provider == module;
        });
    }
}
