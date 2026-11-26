package sanch3zss.raven.event.client;

import net.minecraft.client.gui.screens.Screen;
import sanch3zss.raven.event.CancellableEvent;

public class ScreenOpenEvent extends CancellableEvent {
    public final Screen screen;

    public Screen screen() {
        return this.screen;
    }

    public ScreenOpenEvent(Screen screen) {
        this.screen = screen;
    }
}
