package sanch3zss.raven.ui.component.layout;

import java.util.function.*;

import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.AbstractWidget;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ScrollbarWidget extends AbstractWidget {
    public final Supplier<Float> scrollOffsetSupplier;
    public final Supplier<Float> contentSizeSupplier;
    public final Supplier<Float> viewportSizeSupplier;
    public final Consumer<Float> scrollConsumer;
    public final float padding;
    public final float thickness;
    public final float minThumbSize;
    public boolean dragging;
    public float dragOffset;
    public long lastScrollTime;
    public final ToggleAnimator visibilityAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator hoverAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public final WidgetBounds trackBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds thumbBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);

    public ScrollbarWidget(Supplier<Float> supplier, Supplier<Float> supplier2, Supplier<Float> supplier3, Consumer<Float> consumer, float f, float f2, float f3) {
        this.scrollOffsetSupplier = supplier;
        this.contentSizeSupplier = supplier2;
        this.viewportSizeSupplier = supplier3;
        this.scrollConsumer = consumer;
        this.padding = f;
        this.thickness = f2;
        this.minThumbSize = f3;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        float viewportSize = floatOrZero(this.viewportSizeSupplier.get());
        setSize(this.thickness, viewportSize);
        float fX = x();
        float fY = y() + this.padding;
        this.trackBounds.withPosition(fX, fY).withSize(this.thickness, Math.max(0.0f, viewportSize - (this.padding * 2.0f)));
        updateThumbBounds();
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.colorStack();
        int accent = colorStack.computeColor(palette.accent().argb());
        int idle = colorStack.interpolate(colorStack.computeColor(palette.surfaceBackground().tone(300).argb()), accent, 0.45f);
        int hovered = colorStack.interpolate(colorStack.computeColor(palette.surfaceBackground().tone(200).argb()), accent, 0.65f);
        int scrolling = colorStack.interpolate(colorStack.computeColor(palette.surfaceBackground().tone(200).argb()), accent, 0.80f);
        int thumb = colorStack.interpolate(colorStack.interpolate(idle, hovered, this.hoverAnimator, this.hoverAnimator), scrolling, this.visibilityAnimator, this.visibilityAnimator);
        int end = colorStack.interpolate(thumb, colorStack.computeColor(palette.gradientEnd().argb()), 0.45f);
        drawCtx.fillGradientRoundedRect(this.thumbBounds.x(), this.thumbBounds.y(),
                this.thumbBounds.width(), this.thumbBounds.height(), 2,
                thumb, thumb, end, end);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.hoverAnimator.animate(weightedEngine);
        this.visibilityAnimator.animate(weightedEngine);
        if (!this.dragging && this.lastScrollTime > 0 && System.currentTimeMillis() - this.lastScrollTime > 1000) {
            this.visibilityAnimator.state(false);
            this.lastScrollTime = 0L;
        }
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (z) {
            return false;
        }
        InputEvent inputEvent = inputEventContext.inputEvent();
        if (inputEvent instanceof CursorMoveInput) {
            this.hoverAnimator.state(!this.dragging && inputEventContext.inArea(this.thumbBounds.x(), this.thumbBounds.y(), this.thumbBounds.width(), this.thumbBounds.height()));
            if (!this.dragging) {
                return false;
            }
            dragTo(inputEventContext.logicalMousePosition().y());
            return true;
        }
        if (inputEvent instanceof ScrollInput) {
            this.visibilityAnimator.state(true);
            this.lastScrollTime = System.currentTimeMillis();
            return false;
        }
        if (!(inputEvent instanceof MouseButtonInput)) {
            return false;
        }
        MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent;
        if (mouseButtonInput.button() != 0) {
            return false;
        }
        if (floatOrZero(this.contentSizeSupplier.get()) <= floatOrZero(this.viewportSizeSupplier.get()) + 0.5f) {
            this.dragging = false;
            this.visibilityAnimator.state(false);
            return false;
        }
        MouseButtonAction action = mouseButtonInput.action();
        boolean zInArea = inputEventContext.inArea(this.thumbBounds.x(), this.thumbBounds.y(), this.thumbBounds.width(), this.thumbBounds.height());
        boolean zInArea2 = inputEventContext.inArea(this.trackBounds.x(), this.trackBounds.y(), this.trackBounds.width(), this.trackBounds.height());
        if (action.press()) {
            if (zInArea) {
                this.dragging = true;
                this.visibilityAnimator.state(true);
                this.lastScrollTime = 0L;
                this.dragOffset = inputEventContext.logicalMousePosition().y() - this.thumbBounds.y();
                return true;
            }
            if (zInArea2) {
                applyScrollPosition(inputEventContext.logicalMousePosition().y() - (this.thumbBounds.height() / 2.0f));
                return true;
            }
        }
        if (!action.release() || !this.dragging) {
            return false;
        }
        this.dragging = false;
        this.visibilityAnimator.state(false);
        return true;
    }

    public void updateThumbBounds() {
        float contentSize = floatOrZero(this.contentSizeSupplier.get());
        float viewportSize = floatOrZero(this.viewportSizeSupplier.get());
        float fHeight = this.trackBounds.height();
        if (fHeight <= 0.0f) {
            this.thumbBounds.withPosition(this.trackBounds.x(), this.trackBounds.y()).withSize(this.trackBounds.width(), 0.0f);
            return;
        }
        if (contentSize <= viewportSize + 0.5f) {
            this.thumbBounds.withPosition(this.trackBounds.x(), this.trackBounds.y()).withSize(this.trackBounds.width(), fHeight);
            return;
        }
        float fMin = Math.min(Math.max(this.minThumbSize, (fHeight * viewportSize) / contentSize), fHeight);
        float fClamp = MathUtil.clamp(floatOrZero(this.scrollOffsetSupplier.get()) / Math.max(0.001f, contentSize - viewportSize), 0.0f, 1.0f);
        this.thumbBounds.withPosition(this.trackBounds.x(), this.trackBounds.y() + ((fHeight - fMin) * fClamp)).withSize(this.trackBounds.width(), fMin);
    }

    public void dragTo(double d) {
        applyScrollPosition(((float) d) - this.dragOffset);
    }

    public void applyScrollPosition(float f) {
        float contentSize = floatOrZero(this.contentSizeSupplier.get());
        float viewportSize = floatOrZero(this.viewportSizeSupplier.get());
        float fHeight = this.trackBounds.height();
        if (contentSize <= viewportSize + 0.5f || fHeight <= 0.0f) {
            return;
        }
        float fMax = Math.max(0.001f, fHeight - this.thumbBounds.height());
        this.scrollConsumer.accept(((MathUtil.clamp(f, this.trackBounds.y(), this.trackBounds.y() + fMax) - this.trackBounds.y()) / fMax) * Math.max(0.0f, contentSize - viewportSize));
        updateThumbBounds();
    }

    public float floatOrZero(Float f) {
        if (f == null) {
            return 0.0f;
        }
        return f.floatValue();
    }
}
