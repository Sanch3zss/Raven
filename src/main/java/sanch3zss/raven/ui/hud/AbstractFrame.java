package sanch3zss.raven.ui.hud;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.OverlayCommandQueue;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.ui.component.layout.FrameElementColumn;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;

public abstract class AbstractFrame extends WidgetContainer {
    public final FrameElementColumn frameElementsContainer;
    public final ElementDimmingController elementDimmingManager = new ElementDimmingController();

    public AbstractFrame(FrameElementColumn frameElementColumn) {
        this.frameElementsContainer = frameElementColumn;
        addChild(frameElementColumn);
    }

    public void renderFrameElements(DrawCtx drawCtx) {
        this.frameElementsContainer.render(drawCtx);
    }

    public void renderFrameOverlays(DrawCtx drawCtx) {
        this.frameElementsContainer.renderOverlays(drawCtx);
    }

    public void updateViewportVisibility(float f, float f2, float f3) {
        this.frameElementsContainer.handleViewportVisibility(f, f2, f3);
    }

    public void focusElement(ModuleFrame moduleFrame) {
        if (moduleFrame == null) {
            return;
        }
        this.elementDimmingManager.focusElement(moduleFrame);
        moduleFrame.highlight();
    }

    public void clearElementFocus() {
        this.elementDimmingManager.clearFocus();
    }

    public abstract float contentHeight();

    @Override
    public abstract void render(DrawCtx drawCtx);

    public void renderOverlays(DrawCtx drawCtx) {
        renderFrameOverlays(drawCtx);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return super.handleInput(inputEventContext, z);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.elementDimmingManager.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        super.collectBlurElements(overlayCommandQueue);
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        super.collectBloomElements(renderCommandQueue);
    }

    @Override
    public void handleClose() {
        clearElementFocus();
        super.handleClose();
    }

    @Override
    public void onMenuDrag(boolean z) {
        super.onMenuDrag(z);
    }

    public FrameElementColumn frameElementsContainer() {
        return this.frameElementsContainer;
    }

    public ElementDimmingController elementDimmingManager() {
        return this.elementDimmingManager;
    }
}
