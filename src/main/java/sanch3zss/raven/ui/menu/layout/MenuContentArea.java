package sanch3zss.raven.ui.menu.layout;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.OverlayCommandQueue;

import org.joml.Vector4f;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.hud.WidgetContainer;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.window.MenuWindow;
import sanch3zss.raven.util.math.interpolation.Easings;

public class MenuContentArea extends WidgetContainer {
    private static final float FADE_OUT_PART = 0.3f;
    public final ToggleAnimator tabTransition = new ToggleAnimator(300, Easings.LINEAR);
    private MenuTabElement displayedTab;
    private MenuTabElement outgoingTab;

    @Override
    public void render(DrawCtx drawCtx) {
        syncTabs();
        renderTransition(drawCtx, false);
        super.render(drawCtx);
    }

    public void renderOverlays(DrawCtx drawCtx) {
        renderTransition(drawCtx, true);
    }

    private void renderTransition(DrawCtx drawCtx, boolean overlays) {
        float progress = this.tabTransition.smoothAnimation();
        if (this.outgoingTab != null && progress < FADE_OUT_PART) {
            float fadeOut = 1.0f - Easings.EASE_OUT_QUAD.ease(progress / FADE_OUT_PART);
            drawTab(drawCtx, this.outgoingTab, fadeOut, 1.0f, overlays);
        }
        float fadeIn = Easings.EASE_OUT_CUBIC.ease(
                Math.max(0.0f, (progress - FADE_OUT_PART) / (1.0f - FADE_OUT_PART)));
        if (this.outgoingTab == null) {
            fadeIn = 1.0f;
        }
        if (fadeIn > 0.0f) {
            drawTab(drawCtx, this.displayedTab, fadeIn, 0.96f + 0.04f * fadeIn, overlays);
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        syncTabs();
        this.tabTransition.animate(weightedEngine);
        if (this.tabTransition.isOne()) {
            this.outgoingTab = null;
        }
        super.animation(weightedEngine);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(MenuWindow.MENU_WIDTH - MenuStyle.WORKSPACE_X, MenuWindow.MENU_HEIGHT - MenuWindow.HEADER_HEIGHT);
        syncTabs();
        this.displayedTab.layout(layoutScaleContext, x(), y());
        if (this.outgoingTab != null) {
            this.outgoingTab.layout(layoutScaleContext, x(), y());
        }
        super.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = z | super.handleInput(inputEventContext, z);
        return zHandleInput | Raven.INSTANCE.tabsController().current().handleInputFrames(inputEventContext, zHandleInput);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        syncTabs();
        overlayCommandQueue.record(this::renderTransitionForBlur);
        if (this.outgoingTab != null) {
            this.outgoingTab.frames().forEach(frame -> frame.collectBlurElements(overlayCommandQueue));
        }
        this.displayedTab.frames().forEach(frame -> frame.collectBlurElements(overlayCommandQueue));
        super.collectBlurElements(overlayCommandQueue);
    }

    private void syncTabs() {
        MenuTabElement current = Raven.INSTANCE.tabsController().current();
        if (this.displayedTab == null) {
            this.displayedTab = current;
            this.tabTransition.force(true);
            return;
        }
        if (this.displayedTab == current) {
            return;
        }
        this.outgoingTab = this.displayedTab;
        this.displayedTab = current;
        this.tabTransition.force(false);
        this.tabTransition.state(true);
    }

    private void drawTab(DrawCtx drawCtx, MenuTabElement tab, float alpha, float scale, boolean overlays) {
        if (tab == null) {
            return;
        }
        drawCtx.colorStack().push();
        drawCtx.colorStack().alpha(alpha);
        drawCtx.matrixStack().pushPose();
        if (scale < 1.0f) {
            float centerX = drawCtx.layoutContext().toPhysical(x() + width() / 2.0f);
            float centerY = drawCtx.layoutContext().toPhysical(y() + height() / 2.0f);
            drawCtx.matrixStack().translate(centerX, centerY, 0.0f);
            drawCtx.matrixStack().scale(scale, scale, 1.0f);
            drawCtx.matrixStack().translate(-centerX, -centerY, 0.0f);
        }
        if (overlays) {
            tab.renderFrameOverlays(drawCtx);
        } else {
            tab.drawFrames(drawCtx);
        }
        drawCtx.matrixStack().popPose();
        drawCtx.colorStack().pop();
    }

    private void renderTransitionForBlur(DrawCtx drawCtx) {
        renderTransition(drawCtx, false);
    }
}
