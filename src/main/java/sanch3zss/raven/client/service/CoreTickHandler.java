package sanch3zss.raven.client.service;

import sanch3zss.raven.client.*;
import sanch3zss.raven.event.*;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.feature.player.PvPModeDetector;
import sanch3zss.raven.feature.server.ServerUtil;

public class CoreTickHandler implements ClientListener {
    public CoreTickHandler() {
        Raven.INSTANCE.eventDispatcher().register(PlayerTickEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded() && event.isPre()) {
                ActionSequenceController.tick();
                PvPModeDetector.tick();
                ServerUtil.tick();
            }
        }, EventPriority.LOW);
    }
}
