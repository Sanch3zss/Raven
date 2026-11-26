package sanch3zss.raven.ui.component.layout;

import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;

public class SeparatorElement extends ModuleFrame {
    @Override
    public void highlight() {
    }

    @Override
    public void clearHighlight() {
    }

    @Override
    public void draw(DrawCtx drawCtx, float f, float f2) {
        drawCtx.hairline(x(), y(), f,drawCtx.drawEngine().colorStack().computeColor(drawCtx.theme().palette().surfaceOutline().tone(600).argb()));
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
    }

    @Override
    public float height() {
        return 1.0f;
    }
}
