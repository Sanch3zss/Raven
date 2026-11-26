package sanch3zss.raven.ui.component.control;

import java.util.function.*;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.util.math.interpolation.Easings;

public class DualToggleElement extends ModuleFrame {
    static final int LABEL_SIZE = 14;
    static final int BUTTON_TEXT_SIZE = 9;
    static final float TOGGLE_HEIGHT = 22.0f;
    public final Translation labelText;
    public final Translation leftText;
    public final Translation rightText;
    public Consumer<Boolean> onToggleCallback;
    public Supplier<Boolean> visibleSupplier;
    public final MsdfFont labelFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont buttonFont = Fonts.INTER_EXTRA_BOLD.get();
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final ToggleAnimator toggleAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    public boolean leftActive = false;

    public DualToggleElement(Translation translation3, Translation translation2, Translation translation) {
        this.labelText = translation3;
        this.leftText = translation2;
        this.rightText = translation;
        this.clickBehavior.clickCallback(this::toggle);
    }

    @Override
    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public void clearHighlight() {
        this.highlightAnimation.clearHighlight();
    }

    @Override
    public void draw(DrawCtx drawCtx, float f, float f2) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        float fX = (x() + f) - 110.0f;
        float fHeight = height() / 2.0f;
        float fY = y();
        drawCtx.text(this.labelFont, this.labelText.effective(), LABEL_SIZE, x(), (y() + fHeight) - (this.labelFont.getHeight(14.0f) / 2.0f), colorStack.computeColor(palette.text().tone(100).argb()));
        MenuStyle.drawSegmentedControl(drawCtx, this.buttonFont, BUTTON_TEXT_SIZE,
                new String[]{this.leftText.effective(), this.rightText.effective()},
                fX, fY, 110.0f, TOGGLE_HEIGHT, 1.0f - this.toggleAnimation.smoothAnimation(),
                this.clickBehavior.hoverAnimation().smoothAnimation());
        this.clickBehavior.setDimensions(fX, fY, 110.0f, TOGGLE_HEIGHT);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickBehavior.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickBehavior.animate(weightedEngine);
        this.toggleAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public float height() {
        return Math.max(this.labelFont.getHeight(14.0f), TOGGLE_HEIGHT);
    }

    public void toggle() {
        this.leftActive = !this.leftActive;
        this.toggleAnimation.state(this.leftActive);
        if (this.onToggleCallback != null) {
            this.onToggleCallback.accept(Boolean.valueOf(this.leftActive));
        }
    }

    public void setState(boolean z) {
        this.leftActive = z;
        this.toggleAnimation.state(z);
    }

    public boolean isLeftActive() {
        return this.leftActive;
    }

    public void setOnToggleCallback(Consumer<Boolean> consumer) {
        this.onToggleCallback = consumer;
    }

    public void setVisibleSupplier(Supplier<Boolean> supplier) {
        this.visibleSupplier = supplier;
    }
}
