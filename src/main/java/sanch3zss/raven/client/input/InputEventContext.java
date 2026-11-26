package sanch3zss.raven.client.input;

import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.render.data.PixelPoint;

public class InputEventContext {
    public final InputEvent inputEvent;
    public final float scaleFactor;
    public PixelPoint currentMousePosition;

    public InputEventContext(InputEvent event, PixelPoint point, float f) {
        this.inputEvent = event;
        this.scaleFactor = f;
        this.currentMousePosition = point;
        if (event instanceof CursorMoveInput cursorMoveInput) {
            this.currentMousePosition = cursorMoveInput.mousePosition();
        }
    }

    public PixelPoint currentMousePosition() {
        return this.currentMousePosition;
    }

    public InputEventContext withMouseOffset(float f, float f2) {
        return new OffsetInputContext(this, f, f2);
    }

    public PixelPoint logicalMousePosition() {
        return toLogical(this.currentMousePosition, this.scaleFactor);
    }

    public boolean inArea(float f, float f2, float f3, float f4) {
        PixelPoint logicalMousePosition = logicalMousePosition();
        return ((float) logicalMousePosition.x()) >= f && ((float) logicalMousePosition.y()) >= f2 && ((float) logicalMousePosition.x()) <= f + f3 && ((float) logicalMousePosition.y()) <= f2 + f4;
    }

    public boolean inArea(PixelPoint point, float f, float f2, float f3, float f4) {
        PixelPoint logical = toLogical(point, this.scaleFactor);
        return ((float) logical.x()) >= f && ((float) logical.y()) >= f2 && ((float) logical.x()) <= f + f3 && ((float) logical.y()) <= f2 + f4;
    }

    public boolean inPhysicalArea(PixelPoint point, float f, float f2, float f3, float f4) {
        return ((float) point.x()) >= f && ((float) point.y()) >= f2 && ((float) point.x()) <= f + f3 && ((float) point.y()) <= f2 + f4;
    }

    public PixelPoint toLogical(PixelPoint point, float f) {
        return (f <= 0.0f || f == 1.0f) ? point : new PixelPoint(point.x() / f, point.y() / f);
    }

    public InputEvent inputEvent() {
        return this.inputEvent;
    }

    public float scaleFactor() {
        return this.scaleFactor;
    }
}
