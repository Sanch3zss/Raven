package sanch3zss.raven.event.player;

import sanch3zss.raven.event.*;

public class PlayerTickEvent extends CancellableEvent {
    public final TickStage stage;

    public boolean isPost() {
        return this.stage == TickStage.POST;
    }

    public boolean isPre() {
        return this.stage == TickStage.PRE;
    }

    public PlayerTickEvent(TickStage tickStage) {
        this.stage = tickStage;
    }

    public TickStage getStage() {
        return this.stage;
    }
}
