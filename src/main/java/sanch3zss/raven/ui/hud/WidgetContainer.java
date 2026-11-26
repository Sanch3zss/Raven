package sanch3zss.raven.ui.hud;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.OverlayCommandQueue;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import java.util.*;

import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.LayoutCallback;import sanch3zss.raven.render.data.LayoutScaleContext;

public abstract class WidgetContainer extends AbstractWidget {
    public final List<Widget> childWidgets = new ArrayList<>();

    public List<Widget> children() {
        return Collections.unmodifiableList(this.childWidgets);
    }

    public void addChild(Widget widget) {
        if (widget == null) {
            return;
        }
        if (widget.parent() != null) {
            widget.parent().removeChild(widget);
        }
        widget.parent(this);
        this.childWidgets.add(widget);
    }

    @Override
    public void onMenuDrag(boolean z) {
        for (Widget widget : this.childWidgets) {
            widget.onMenuDrag(z);
        }
    }

    public void addChild(int i, Widget widget) {
        if (widget == null) {
            return;
        }
        if (widget.parent() != null) {
            widget.parent().removeChild(widget);
        }
        widget.parent(this);
        this.childWidgets.add(i, widget);
    }

    public void removeChild(Widget widget) {
        if (this.childWidgets.remove(widget)) {
            widget.parent(null);
        }
    }

    public void clearChildren() {
        for (Widget widget : this.childWidgets) {
            widget.parent(null);
        }
        this.childWidgets.clear();
    }

    public void center(float f, float f2) {
        setPosition((f - width()) / 2.0f, (f2 - height()) / 2.0f);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        for (Widget widget : this.childWidgets) {
            widget.animation(weightedEngine);
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        for (Widget widget : this.childWidgets) {
            widget.layout(layoutScaleContext);
        }
    }

    @Override
    public void render(DrawCtx drawCtx) {
        for (LayoutCallback layoutCallback : this.childWidgets) {
            if (layoutCallback instanceof WidgetParent widgetParent) {
                widgetParent.render(drawCtx);
            }
        }
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (!visible()) {
            return z;
        }
        boolean zHandleInput = z;
        for (int size = this.childWidgets.size() - 1; size >= 0; size--) {
            Widget widget = this.childWidgets.get(size);
            if (widget.visible()) {
                zHandleInput |= widget.handleInput(inputEventContext, zHandleInput);
            }
        }
        return zHandleInput | super.handleInput(inputEventContext, zHandleInput);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        for (Widget widget : this.childWidgets) {
            widget.collectBlurElements(overlayCommandQueue);
        }
    }

    @Override
    public void handleClose() {
        for (Widget widget : this.childWidgets) {
            widget.handleClose();
        }
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        for (Widget widget : this.childWidgets) {
            widget.collectBloomElements(renderCommandQueue);
        }
    }
}
