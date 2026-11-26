package sanch3zss.raven.ui.hud;

import sanch3zss.raven.client.input.*;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.util.math.MathUtil;

public class DraggableContainer extends WidgetContainer {
    public boolean dragging;
    public float dragOffsetX;
    public float dragOffsetY;
    public float logicalWidth;
    public float logicalHeight;
    private Widget target;

    public DraggableContainer target(Widget target) {
        this.target = target;
        return this;
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        InputEvent inputEvent = inputEventContext.inputEvent();
        if (this.target == null || z && !this.dragging) {
            return false;
        }
        if (inputEvent instanceof CursorMoveInput) {
            if (!this.dragging) {
                return inputEventContext.inArea(x(), y(), width(), height());
            }
            float fX = inputEventContext.logicalMousePosition().x() - this.dragOffsetX;
            float fY = inputEventContext.logicalMousePosition().y() - this.dragOffsetY;
            this.target.setPosition(clampX(fX), clampY(fY));
            return true;
        }
        if (inputEvent instanceof MouseButtonInput mouseButtonInput && mouseButtonInput.button() == 0) {
            if (mouseButtonInput.action().press() && !z && inputEventContext.inArea(x(), y(), width(), height())) {
                this.dragging = true;
                this.dragOffsetX = inputEventContext.logicalMousePosition().x() - this.target.x();
                this.dragOffsetY = inputEventContext.logicalMousePosition().y() - this.target.y();
                return true;
            }
            if (mouseButtonInput.action().release() && this.dragging) {
                this.dragging = false;
                return true;
            }
        }
        return super.handleInput(inputEventContext, z);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.logicalWidth = layoutScaleContext.logicalWidth();
        this.logicalHeight = layoutScaleContext.logicalHeight();
        if (this.target != null && !this.dragging) {
            this.target.setPosition(clampX(this.target.x()), clampY(this.target.y()));
        }
        super.layout(layoutScaleContext);
    }

    private float clampX(float x) {
        return this.logicalWidth > this.target.width() ? MathUtil.clamp(x, 0.0f, this.logicalWidth - this.target.width()) : x;
    }

    private float clampY(float y) {
        return this.logicalHeight > this.target.height() ? MathUtil.clamp(y, 0.0f, this.logicalHeight - this.target.height()) : y;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public void setDragging(boolean z) {
        this.dragging = z;
    }
}
