package sanch3zss.raven.ui.menu.layout;

import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.ui.hud.Widget;

public abstract class ModuleFrame extends Widget {
    public float viewportTop;
    public float viewportBottom;
    public float viewportOffset;

    public abstract void highlight();

    public abstract void clearHighlight();

    public abstract void draw(DrawCtx drawCtx, float f, float f2);

    public void drawOverlay(DrawCtx drawCtx) {
    }

    public void handleViewportVisibility(float f, float f2, float f3) {
        this.viewportTop = f;
        this.viewportBottom = f2;
        this.viewportOffset = f3;
    }

    @Override
    public float width() {
        return this.bounds.width();
    }
}
