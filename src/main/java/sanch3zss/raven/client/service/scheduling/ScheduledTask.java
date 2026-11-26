package sanch3zss.raven.client.service.scheduling;

import sanch3zss.raven.feature.module.Module;

public class ScheduledTask<T> {
    public int expiresIn;
    public final int priority;
    public final Module provider;

    public final T value;

    public int getExpiresIn() {
        return this.expiresIn;
    }

    public int getPriority() {
        return this.priority;
    }

    public Module getProvider() {
        return this.provider;
    }

    public T getValue() {
        return this.value;
    }

    public void setExpiresIn(int i) {
        this.expiresIn = i;
    }

    public ScheduledTask(int i, int i2, Module module, T t) {
        this.expiresIn = i;
        this.priority = i2;
        this.provider = module;
        this.value = t;
    }
}
