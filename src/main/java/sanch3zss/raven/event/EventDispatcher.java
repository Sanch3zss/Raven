package sanch3zss.raven.event;

import java.util.*;

import sanch3zss.raven.client.Raven;

public class EventDispatcher {
    public final Map<Class<? extends Event>, List<RegisteredListener<?>>> listeners = new HashMap<>();

    public <T extends Event> void dispatch(T event) {
        List<RegisteredListener<?>> list = this.listeners.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }
        int n = list.size();
        for (int i = 0; i < n; i++) {
            RegisteredListener<?> registeredListener = list.get(i);
            EventCallback eventCallback = registeredListener.callback();
            try {
                eventCallback.call(event);
            } catch (Throwable th) {
                Raven.LOGGER.error("EventDispatcher: exception in callback {} (priority={}) for event {}", eventCallback.getClass().getName(), registeredListener.priority(), event.getClass().getName(), th);
            }
            if (event instanceof CancellableEvent && ((CancellableEvent) event).isStopProgression()) {
                break;
            }
        }
    }

    public <T extends Event> void register(Class<T> cls, EventCallback<T> eventCallback) {
        register(cls, eventCallback, EventPriority.NORMAL);
    }

    public <T extends Event> void register(Class<T> cls, EventCallback<T> eventCallback, EventPriority eventPriority) {
        List<RegisteredListener<?>> listComputeIfAbsent = this.listeners.computeIfAbsent(cls, k -> new ArrayList<>());
        listComputeIfAbsent.add(new RegisteredListener<>(eventCallback, eventPriority));
        listComputeIfAbsent.sort(Comparator.comparing(RegisteredListener::priority));
    }

    public <T extends Event> void unregister(Class<T> cls, EventCallback<T> eventCallback) {
        List<RegisteredListener<?>> list = this.listeners.get(cls);
        if (list != null) {
            list.removeIf(registeredListener -> registeredListener.callback() == eventCallback);
        }
    }
}
