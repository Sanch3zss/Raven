package sanch3zss.raven.ui.component.control;

import java.util.function.Supplier;

import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.AbstractWidget;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ToggleSwitch extends AbstractWidget {
    public final float switchWidth;
    public final float switchHeight;
    public final Supplier<Boolean> stateSupplier;
    public Runnable clickRunnable;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final ToggleAnimator toggleSwitchAnimation = new ToggleAnimator(210, Easings.EASE_IN_OUT_CUBIC);
    public boolean lastState = false;

    public ToggleSwitch(float f, float f2, Supplier<Boolean> supplier) {
        this.switchWidth = f;
        this.switchHeight = f2;
        this.stateSupplier = supplier;
        this.clickable.clickCallback(this::invert);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        boolean zBooleanValue = this.stateSupplier.get().booleanValue();
        if (zBooleanValue != this.lastState) {
            this.toggleSwitchAnimation.state(zBooleanValue);
            this.lastState = zBooleanValue;
        }
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        float progress = this.toggleSwitchAnimation.smoothAnimation();
        float hover = this.clickable.hoverAnimation().smoothAnimation();
        int track = colorStack.interpolate(colorStack.computeColor(0xFF787880, 0.32f + 0.06f * hover),
                colorStack.computeColor(palette.accent().argb()), progress);
        MenuStyle.drawSwitch(drawCtx, x(), y(), this.switchWidth, this.switchHeight, progress, track);
    }

    public void invert() {
        if (this.clickRunnable != null) {
            this.clickRunnable.run();
        }
    }

    public void setClickableArea(float f, float f2, float f3, float f4) {
        this.clickable.setDimensions(f, f2, f3, f4);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.switchWidth, this.switchHeight);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.toggleSwitchAnimation.animate(weightedEngine);
        this.clickable.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        if (!zHandleInput) {
            zHandleInput = this.clickable.handleInput(inputEventContext, z);
        }
        return zHandleInput;
    }

    public ToggleAnimator toggleSwitchAnimation() {
        return this.toggleSwitchAnimation;
    }

    public ToggleSwitch runnable(Runnable runnable) {
        this.clickRunnable = runnable;
        return this;
    }
}
