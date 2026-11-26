package sanch3zss.raven.event;

public interface EventCallback<T extends Event> {
    void call(T t);
}
