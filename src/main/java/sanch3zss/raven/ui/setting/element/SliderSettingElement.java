package sanch3zss.raven.ui.setting.element;

import java.util.function.*;


import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class SliderSettingElement extends ModuleFrame {
    public static final int titleFontSize = 14;
    public static final int descFontSize = 13;

    public final Translation name;
    public final Translation description;
    public final SettingUnit unit;
    public final float max;
    public final float min;
    public final float step;
    public float currentValue;
    public Supplier<Float> valueSupplier;
    public Supplier<Boolean> visibleCondition;
    public Consumer<Float> changeConsumer;
    public float lastSyncedValue;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descFont = Fonts.INTER_MEDIUM.get();
    public final AnimatedFloat valueAnimation = new AnimatedFloat(180, Easings.EASE_OUT_CUBIC);
    public final ToggleAnimator pressAnimator = new ToggleAnimator(220, Easings.EASE_OUT_CUBIC);
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public String wrappedDescription = null;
    public boolean dragging = false;
    public final WidgetBounds trackBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds interactionBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final ToggleAnimator hoverAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public boolean descriptionDirty = true;
    public float lastLayoutWidth = -1.0f;
    public String lastDescription = null;
    public float descriptionHeight = 0.0f;

    public SliderSettingElement(Translation translation2, Translation translation, SettingUnit settingUnit, float max, float min, float step) {
        this.name = translation2;
        this.description = translation;
        this.unit = settingUnit;
        this.max = max;
        this.min = min;
        this.step = step;    }

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
        float fY = y();
        String str = (this.currentValue % 1.0f == 0.0f ? String.valueOf((int) this.currentValue) : String.format("%.2f", this.currentValue)) + " " + this.unit.format(this.currentValue);
        String strEffective = this.name.effective();
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.titleFont, str, 12);
        float valueWidth = Math.max(46.0f, fTextWidthPhysical + 16.0f);
        float valueX = x() + f - valueWidth;
        updateDescription(f);
        float titleRight = valueX - 8.0f;
        int color = colorStack.computeColor(palette.accent().argb());
        float fValue = this.highlightAnimation.value();
        MenuStyle.drawFadingText(drawCtx, this.titleFont, strEffective,
                titleFontSize, x(), fY, Math.max(0.0f, titleRight - x()),
                colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()),
                        colorStack.computeColor(palette.text().tone(100).argb()), fValue));
        int controlColor = MenuStyle.control(colorStack);
        MenuStyle.drawControlField(drawCtx, valueX, fY, valueWidth, 18.0f, 4.0f,
                this.hoverAnimator.smoothAnimation());
        drawCtx.text(this.titleFont, str, 12,
                valueX + (valueWidth - fTextWidthPhysical) / 2.0f,
                fY + (18.0f - this.titleFont.getHeight(12.0f)) / 2.0f, color);
        if (this.wrappedDescription != null) {
            fY += this.titleFont.getHeight(14.0f) + 3.0f;
            drawCtx.text(this.descFont, this.wrappedDescription, descFontSize, x(), fY,
                    colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()),
                            colorStack.computeColor(palette.text().tone(100).argb()), fValue));
        }
        float trackY = y() + 18.0f
                + (this.wrappedDescription == null ? 0.0f : 3.0f + this.descriptionHeight) + 6.0f;
        this.trackBounds.withPosition(x(), trackY).withSize(f, 4.0f);
        this.interactionBounds.withPosition(x(), trackY - 6.0f).withSize(f, 16.0f);
        float fClamp = MathUtil.clamp((this.valueAnimation.animatedValue() - this.min) / (this.max - this.min), 0.0f, 1.0f);
        MenuStyle.drawSlider(drawCtx, this.trackBounds.x(), this.trackBounds.y(), this.trackBounds.width(),
                this.trackBounds.height(), fClamp, this.hoverAnimator.smoothAnimation(),
                this.pressAnimator.smoothAnimation(), color);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (super.handleInput(inputEventContext, z)) {
            return true;
        }
        if (z) {
            return false;
        }
        InputEvent inputEvent = inputEventContext.inputEvent();
        if (!(inputEvent instanceof MouseButtonInput)) {
            if (!(inputEventContext.inputEvent() instanceof CursorMoveInput)) {
                return false;
            }
            if (this.dragging) {
                float newValue = roundToStep(positionToValue(this.min, this.max,
                        this.trackBounds.x(), Math.max(1.0f, this.trackBounds.width()),
                        inputEventContext.logicalMousePosition().x()), this.step);
                if (newValue != this.currentValue) {
                    this.currentValue = newValue;
                    if (this.changeConsumer != null) {
                        this.changeConsumer.accept(this.currentValue);
                    }
                }
            }
            boolean z2 = inputEventContext.inArea(this.interactionBounds.x(), this.interactionBounds.y(), this.interactionBounds.width(), this.interactionBounds.height()) || this.dragging;
            this.hoverAnimator.state(z2);
            return z2;
        }
        MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent;
        if (mouseButtonInput.button() != 0) {
            return false;
        }
        MouseButtonAction action = mouseButtonInput.action();
        if (!action.press() || !inputEventContext.inArea(this.interactionBounds.x(), this.interactionBounds.y(), this.interactionBounds.width(), this.interactionBounds.height())) {
            if (!action.release() || !this.dragging) {
                return false;
            }
            this.dragging = false;
            return true;
        }
        this.dragging = true;
        float newValue = roundToStep(positionToValue(this.min, this.max,
                this.trackBounds.x(), Math.max(1.0f, this.trackBounds.width()),
                inputEventContext.logicalMousePosition().x()), this.step);
        if (newValue == this.currentValue) {
            return true;
        }
        this.currentValue = newValue;
        if (this.changeConsumer == null) {
            return true;
        }
        this.changeConsumer.accept(this.currentValue);
        return true;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.valueSupplier != null) {
            Float f = this.valueSupplier.get();
            if (f != null && Math.abs(f - this.lastSyncedValue) > 1.0E-6f) {
                this.currentValue = f;
                this.lastSyncedValue = f;
            }
        }
        if (this.visibleCondition != null) {
            visible(Boolean.TRUE.equals(this.visibleCondition.get()));
        }
        this.highlightAnimation.animate(weightedEngine);
        this.valueAnimation.destination(this.currentValue);
        this.valueAnimation.animate(weightedEngine);
        this.hoverAnimator.animate(weightedEngine);
        this.pressAnimator.state(this.dragging);
        this.pressAnimator.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {
        this.dragging = false;        super.handleClose();
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescription(Math.max(0.0f, this.parent.width() - 150.0f));
        }
        float height = 18.0f;
        if (this.description != null && this.wrappedDescription != null) {
            height += 3.0f + this.descriptionHeight;
        }
        return height + 11.0f;
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.lastLayoutWidth = f;
            this.lastDescription = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.lastLayoutWidth || strEffective == null || !strEffective.equals(this.lastDescription)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descFont, descFontSize);
            this.descriptionHeight = this.descFont.getHeightWithLineBreaks(this.wrappedDescription, descFontSize);
            this.lastLayoutWidth = f;
            this.lastDescription = strEffective;
            this.descriptionDirty = false;
        }
    }

    public float positionToValue(float minVal, float maxVal, float trackStartX, float trackWidth, double mouseX) {
        return (MathUtil.clamp((float) ((mouseX - trackStartX) / trackWidth), 0.0f, 1.0f) * (maxVal - minVal)) + minVal;
    }

    public float roundToStep(float val, float stepVal) {
        return MathUtil.clamp(Math.round(val / stepVal) * stepVal, this.min, this.max);
    }

    public Translation name() {
        return this.name;
    }

    public SliderSettingElement currentValueSupplier(Supplier<Float> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public SliderSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }

    public SliderSettingElement onValueChanged(Consumer<Float> consumer) {
        this.changeConsumer = consumer;
        return this;
    }
}
