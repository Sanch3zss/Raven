package sanch3zss.raven.client.config.repository;

import java.util.*;

import sanch3zss.raven.feature.inventory.DropAllHandler;

public class HandlerRepository {
    public final List<ClientHandler> listeners = new ArrayList();
    public DropAllHandler containerHandler;

    public HandlerRepository() {
        setup();
    }

    public void setup() {
        DropAllHandler dropAllHandler = new DropAllHandler();
        this.containerHandler = dropAllHandler;
        registerHandlers(dropAllHandler);
    }

    public void registerHandlers(ClientHandler... arr) {
        this.listeners.addAll(List.of(arr));
    }

    public List<ClientHandler> getListeners() {
        return this.listeners;
    }

    public DropAllHandler getContainerHandler() {
        return this.containerHandler;
    }
}
