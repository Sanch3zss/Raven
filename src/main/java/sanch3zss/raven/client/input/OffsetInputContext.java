package sanch3zss.raven.client.input;

import sanch3zss.raven.render.data.PixelPoint;

public final class OffsetInputContext extends InputEventContext {
    public final InputEventContext delegate;
    public final float offsetX;
    public final float offsetY;

    public OffsetInputContext(InputEventContext inputEventContext, float f, float f2) {
        super(inputEventContext.inputEvent(), inputEventContext.currentMousePosition(), inputEventContext.scaleFactor());
        this.delegate = inputEventContext;
        this.offsetX = f;
        this.offsetY = f2;
    }

    @Override
    public PixelPoint currentMousePosition() {
        return this.delegate.currentMousePosition();
    }

    @Override
    public PixelPoint logicalMousePosition() {
        PixelPoint logicalMousePosition = this.delegate.logicalMousePosition();
        return new PixelPoint(logicalMousePosition.x() + this.offsetX, logicalMousePosition.y() + this.offsetY);
    }

    @Override
    public boolean inArea(float f, float f2, float f3, float f4) {
        PixelPoint logicalMousePosition = logicalMousePosition();
        return ((float) logicalMousePosition.x()) >= f && ((float) logicalMousePosition.y()) >= f2 && ((float) logicalMousePosition.x()) <= f + f3 && ((float) logicalMousePosition.y()) <= f2 + f4;
    }
}
