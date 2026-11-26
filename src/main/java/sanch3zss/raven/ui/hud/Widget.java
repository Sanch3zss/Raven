package sanch3zss.raven.ui.hud;


import sanch3zss.raven.render.engine.OverlayCommandQueue;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.LayoutCallback;import sanch3zss.raven.render.data.*;

public abstract class Widget implements LayoutCallback {
    public WidgetContainer parent;
    public WidgetBounds bounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public boolean visible = true;

    public void setPosition(float f, float f2) {
        this.bounds = this.bounds.withPosition(Math.round(f), Math.round(f2));
    }

    public void setSize(float f, float f2) {
        this.bounds = this.bounds.withSize(f, f2);
    }

    public float x() {
        return this.bounds.x();
    }

    public float y() {
        return this.bounds.y();
    }

    public float width() {
        return this.bounds.width();
    }

    public float height() {
        return this.bounds.height();
    }

    public void handleClose() {
    }

    public void onMenuDrag(boolean z) {
    }

    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return false;
    }

    public void animation(WeightedEngine weightedEngine) {
    }

    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
    }

    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
    }

    public boolean hitTest(PixelPoint point, LayoutScaleContext layoutScaleContext) {
        return this.bounds.containsPhysical(point.x(), point.y(), layoutScaleContext.scaleFactor());
    }

    public WidgetContainer parent() {
        return this.parent;
    }

    public Widget parent(WidgetContainer widgetContainer) {
        this.parent = widgetContainer;
        return this;
    }

    public void setParent(WidgetContainer widgetContainer) {
        this.parent = widgetContainer;
    }

    public boolean visible() {
        return this.visible;
    }

    public Widget visible(boolean z) {
        this.visible = z;
        return this;
    }
}
