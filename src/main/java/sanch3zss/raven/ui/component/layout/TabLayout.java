package sanch3zss.raven.ui.component.layout;

import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.ui.menu.layout.MenuTabElement;

public interface TabLayout {
    default void initialize(MenuTabElement tabElement) {
    }

    void render(DrawCtx drawCtx);

    default void renderOverlays(DrawCtx drawCtx) {
    }

    void layout(LayoutScaleContext layoutScaleContext);

    void animation(WeightedEngine weightedEngine);

    boolean handleInput(InputEventContext inputEventContext, boolean z);

    float getContentHeight();

    void positionFrames();

    float width();

    float height();
}
