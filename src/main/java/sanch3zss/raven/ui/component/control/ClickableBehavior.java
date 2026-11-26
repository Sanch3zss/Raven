package sanch3zss.raven.ui.component.control;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.ScissorBounds;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ClickableBehavior {
    public static final int hoverDuration = 200;
    public final ToggleAnimator hoverAnimation = new ToggleAnimator(hoverDuration, Easings.EASE_IN_OUT_CUBIC);
    public Runnable clickAction;
    public Runnable rightClickAction;
    public Runnable middleClickAction;
    public float ownerX;
    public float ownerY;
    public float ownerW;
    public float ownerH;

    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        float f = this.ownerX;
        float f2 = this.ownerY;
        float f3 = this.ownerW;
        float f4 = this.ownerH;
        if (inputEventContext.inputEvent() instanceof CursorMoveInput) {
            boolean z2 = inputEventContext.inArea(f, f2, f3, f4) && !z;
            this.hoverAnimation.state(z2);
            return z2;
        }
        InputEvent inputEvent = inputEventContext.inputEvent();
        if (!(inputEvent instanceof MouseButtonInput)) {
            return false;
        }
        MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent;
        MouseButtonAction action = mouseButtonInput.action();
        boolean zInArea = inputEventContext.inArea(f, f2, f3, f4);
        if (mouseButtonInput.button() == 0 && this.clickAction != null && action.press() && zInArea && !z) {
            this.clickAction.run();
            return true;
        }
        if (mouseButtonInput.button() == 2 && this.middleClickAction != null
                && action.press() && zInArea && !z) {
            this.middleClickAction.run();
            return true;
        }
        if (mouseButtonInput.button() != 1 || this.rightClickAction == null || !action.press() || !zInArea || z) {
            return false;
        }
        this.rightClickAction.run();
        return true;
    }

    public void determineFromLastDrawEngineOperation(DrawCtx drawCtx) {
        DrawEngine drawEngine = drawCtx.drawEngine();
        ScissorBounds lastBounds = drawEngine.lastBounds();
        float fScaleFactor = drawCtx.layoutContext().scaleFactor();
        float fTransformX = drawEngine.transformX(drawCtx.matrixStack(), lastBounds.x);
        float fTransformY = drawEngine.transformY(drawCtx.matrixStack(), lastBounds.y);
        if (fScaleFactor <= 0.0f || fScaleFactor == 1.0f) {
            this.ownerX = fTransformX;
            this.ownerY = fTransformY;
            this.ownerW = lastBounds.w;
            this.ownerH = lastBounds.h;
            return;
        }
        this.ownerX = fTransformX / fScaleFactor;
        this.ownerY = fTransformY / fScaleFactor;
        this.ownerW = lastBounds.w / fScaleFactor;
        this.ownerH = lastBounds.h / fScaleFactor;
    }

    public void setDimensions(float f, float f2, float f3, float f4) {
        this.ownerX = f;
        this.ownerY = f2;
        this.ownerW = f3;
        this.ownerH = f4;
    }

    public void animate(WeightedEngine weightedEngine) {
        this.hoverAnimation.animate(weightedEngine);
    }

    public ToggleAnimator hoverAnimation() {
        return this.hoverAnimation;
    }

    public ClickableBehavior clickCallback(Runnable runnable) {
        this.clickAction = runnable;
        return this;
    }

    public ClickableBehavior rightClickCallback(Runnable runnable) {
        this.rightClickAction = runnable;
        return this;
    }

    public ClickableBehavior middleClickCallback(Runnable runnable) {
        this.middleClickAction = runnable;
        return this;
    }

    public float ownerX() {
        return this.ownerX;
    }

    public float ownerY() {
        return this.ownerY;
    }

    public float ownerW() {
        return this.ownerW;
    }

    public float ownerH() {
        return this.ownerH;
    }
}
