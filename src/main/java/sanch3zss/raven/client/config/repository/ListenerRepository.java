package sanch3zss.raven.client.config.repository;


import sanch3zss.raven.render.engine.FragmentShaderBuilder;
import sanch3zss.raven.render.gl.MatrixCaptureHandler;
import java.util.*;

import sanch3zss.raven.client.input.KeybindHandler;
import sanch3zss.raven.client.service.CoreTickHandler;
import sanch3zss.raven.event.ClientListener;
import sanch3zss.raven.feature.inventory.SlotSyncHandler;public class ListenerRepository {
    public final List<ClientListener> listeners = new ArrayList();

    public ListenerRepository() {
        setup();
    }

    public void setup() {
        registerListeners(new SlotSyncHandler(), new KeybindHandler(), new FragmentShaderBuilder(), new MatrixCaptureHandler(), new CoreTickHandler());
    }

    public void registerListeners(ClientListener... arr) {
        this.listeners.addAll(List.of(arr));
    }

    public List<ClientListener> getListeners() {
        return this.listeners;
    }
}
