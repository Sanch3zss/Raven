package sanch3zss.raven.render.engine;

import java.util.*;
import java.util.function.Consumer;

import sanch3zss.raven.ui.hud.WidgetParent;

public class OverlayCommandQueue {
    public final List<Consumer<DrawCtx>> commands = new ArrayList();

    public void record(Consumer<DrawCtx> consumer) {
        if (consumer == null) {
            return;
        }
        this.commands.add(consumer);
    }

    public void recordElement(WidgetParent widgetParent) {
        if (widgetParent == null) {
            return;
        }
        Objects.requireNonNull(widgetParent);
        record(widgetParent::render);
    }

    public void renderRecorded(DrawCtx drawCtx) {
        Iterator<Consumer<DrawCtx>> it = this.commands.iterator();
        while (it.hasNext()) {
            it.next().accept(drawCtx);
        }
        this.commands.clear();
    }

    public void clear() {
        this.commands.clear();
    }

    public boolean isEmpty() {
        return this.commands.isEmpty();
    }
}
