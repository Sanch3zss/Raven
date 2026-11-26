package sanch3zss.raven.ui.setting.element;

import java.util.List;
import java.util.function.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.feature.setting.KeybindCaptureState;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class KeybindSettingElement extends ModuleFrame {
    static final int nameFontSize = 14;
    static final int descriptionFontSize = 13;
    static final int bindingFontSize = 12;
    static final float GAP = 3.0f;
    static final float PADDING = 10.0f;
    static final float BUTTON_HEIGHT = 18.0f;
    static final float ICON_SIZE = 8.0f;

    final Translation name;
    final Translation description;
    Supplier<Boolean> visibilitySupplier;
    String wrappedDescription;
    String cachedDescriptionSource;
    float descriptionHeight;
    public final Supplier<List<Integer>> keysSupplier;
    final MsdfFont semiBoldFont = Fonts.INTER_SEMIBOLD.get();
    final MsdfFont mediumFont = Fonts.INTER_MEDIUM.get();
    final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    final ClickableBehavior clickableBehavior = new ClickableBehavior();
    final WidgetBounds bindingBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    final KeybindCaptureState captureState = new KeybindCaptureState();
    final ToggleAnimator activeToggleAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    final AnimatedFloat widthAnimator = new AnimatedFloat(200, Easings.EASE_IN_OUT_CUBIC);
    boolean widthInitialized = false;
    float scaleFactor = 1.0f;
    float cachedLayoutWidth = -1.0f;
    boolean descriptionDirty = true;

    public KeybindSettingElement(Translation translation2, Translation translation, Supplier<List<Integer>> supplier) {
        this.name = translation2;
        this.description = translation;
        this.keysSupplier = supplier;
        this.clickableBehavior.clickCallback(this.captureState::toggleCapture);    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.captureState.onChange(consumer);
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
        if (this.captureState.captureKey()) {
            drawCtx.window().interceptKeyboard(true);
        }
        updateDescriptionLayout(f);
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        float fScaleFactor = drawCtx.layoutContext().scaleFactor();
        float nameWidth = measureTextWidth(this.name.effective(), nameFontSize, fScaleFactor);
        float f3 = f / 2.0f;
        boolean z = nameWidth >= (f - f3) + PADDING || this.wrappedDescription != null;
        float fY = y();
        if (z) {
            fY = drawDescriptionText(drawCtx, drawNameLabel(drawCtx, fY));
        }
        String bindingText = this.captureState.formatBindingText();
        float textWidth = measureTextWidth(bindingText, bindingFontSize, fScaleFactor);
        float height = this.semiBoldFont.getHeight(12.0f);
        int iInterpolate2 = colorStack.interpolate(sanch3zss.raven.ui.menu.MenuStyle.control(colorStack), sanch3zss.raven.ui.menu.MenuStyle.controlHover(colorStack), this.clickableBehavior.hoverAnimation());
        float f4 = 0.0f;
        float fWidth = 0.0f;
        float fWidth2 = z ? (((this.bindingBounds.width() - 20.0f) - fWidth) - f4) - 60.0f : (f - f3) - 30.0f;
        String displayText = bindingText;
        if (textWidth > fWidth2) {
            displayText = truncateWithEllipsis(bindingText, fWidth2, fScaleFactor);
        }
        float displayTextWidth = measureTextWidth(displayText, bindingFontSize, fScaleFactor);
        float fTargetWidth = Math.max(45.0f, displayTextWidth + 14.0f);
        if (!this.widthInitialized) {
            this.widthAnimator.set(fTargetWidth);
            this.widthInitialized = true;
        }
        this.widthAnimator.destination(fTargetWidth);
        float f5 = fWidth + f4 + displayTextWidth;
        int iInterpolate3 = colorStack.interpolate(colorStack.computeColor(palette.text().tone(500).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.activeToggleAnimator.smoothAnimation());
        if (z) {
            this.bindingBounds.withPosition(x(), fY + PADDING).withSize(f, BUTTON_HEIGHT);
            float fY2 = this.bindingBounds.y() + (this.bindingBounds.height() / 2.0f);
            float fX = this.bindingBounds.x() + ((this.bindingBounds.width() - f5) / 2.0f);
            sanch3zss.raven.ui.menu.MenuStyle.drawSettingButton(drawCtx,
                    this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(),
                    this.bindingBounds.height(), 4.0f, this.clickableBehavior.hoverAnimation().smoothAnimation());
            drawCtx.text(this.semiBoldFont, displayText, bindingFontSize, fX + fWidth + f4, fY2 - (height / 2.0f), iInterpolate3);
        } else {
            float fAnimatedValue = Math.max(45.0f, this.widthAnimator.animatedValue());
            this.bindingBounds.withPosition((x() + f) - fAnimatedValue, y()).withSize(fAnimatedValue, BUTTON_HEIGHT);
            float fY2 = this.bindingBounds.y() + (this.bindingBounds.height() / 2.0f);
            sanch3zss.raven.ui.menu.MenuStyle.drawSettingButton(drawCtx,
                    this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(),
                    this.bindingBounds.height(), 4.0f, this.clickableBehavior.hoverAnimation().smoothAnimation());
            float f6 = this.bindingBounds.x() + (this.bindingBounds.width() - displayTextWidth) / 2.0f;
            if (f6 + displayTextWidth > (this.bindingBounds.x() + fAnimatedValue) - 5.0f) {
                drawCtx.drawEngine().beginStencil();
                drawCtx.fillRoundedRect(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height(), 4.0f, iInterpolate2);
                drawCtx.drawEngine().prepareStencil(1);
                drawCtx.text(this.semiBoldFont, displayText, bindingFontSize, f6, fY2 - (height / 2.0f), iInterpolate3);
                drawCtx.drawEngine().endStencil();
            } else {
                drawCtx.text(this.semiBoldFont, displayText, bindingFontSize, f6, fY2 - (height / 2.0f), iInterpolate3);
            }
            drawCtx.text(this.semiBoldFont, this.name.effective(), nameFontSize, x(), fY2 - (this.semiBoldFont.getHeight(14.0f) / 2.0f), colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.highlightAnimation.value()));
        }
        this.clickableBehavior.setDimensions(x(), y(), f, height());
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        String bindingText = this.captureState.formatBindingText();
        this.scaleFactor = layoutScaleContext.scaleFactor();
        float textWidth = measureTextWidth(bindingText, bindingFontSize, this.scaleFactor);
        float parentW = this.parent != null ? this.parent.width() : 200.0f;
        float fWidth = (parentW - (parentW / 2.0f)) - 30.0f;
        String displayText = bindingText;
        if (textWidth > fWidth) {
            displayText = truncateWithEllipsis(bindingText, fWidth, this.scaleFactor);
        }
        float displayTextWidth = measureTextWidth(displayText, bindingFontSize, this.scaleFactor);
        float fWidth2 = Math.max(45.0f, displayTextWidth + 14.0f);
        if (!this.widthInitialized) {
            this.widthAnimator.set(fWidth2);
            this.widthInitialized = true;
        }
        this.widthAnimator.destination(fWidth2);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = super.handleInput(inputEventContext, z);
        boolean zHandleInput2 = zHandleInput | this.clickableBehavior.handleInput(inputEventContext, zHandleInput);
        if (!z) {
            if (this.captureState.captureKey()) {
                InputEvent inputEvent = inputEventContext.inputEvent();
                if (inputEvent instanceof MouseButtonInput mouseButtonInput) {
                    if (mouseButtonInput.action().press() && mouseButtonInput.button() == 0 && !inputEventContext.inArea(this.clickableBehavior.ownerX(), this.clickableBehavior.ownerY(), this.clickableBehavior.ownerW(), this.clickableBehavior.ownerH())) {
                        this.captureState.toggleCapture();
                        zHandleInput2 = true;
                    }
                }
            }
            if (!zHandleInput2) {
                InputEvent inputEvent2 = inputEventContext.inputEvent();
                if (inputEvent2 instanceof KeyInput keyInput) {
                    zHandleInput2 = this.captureState.handleKeyInput(keyInput);
                }
                if (inputEvent2 instanceof MouseButtonInput mouseButtonInput) {
                    zHandleInput2 = this.captureState.handleMouseInput(mouseButtonInput);
                }
            }
            zHandleInput2 |= this.captureState.captureKey() && !zHandleInput2;
        }
        return zHandleInput2;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(Boolean.TRUE.equals(this.visibilitySupplier.get()));
        }
        if (this.keysSupplier != null) {
            this.captureState.syncKeys(this.keysSupplier.get());
        }
        this.activeToggleAnimator.state(this.captureState.hasAnyKeys());
        this.activeToggleAnimator.animate(weightedEngine);
        this.widthAnimator.animate(weightedEngine);
        this.clickableBehavior.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {        super.handleClose();
    }

    @Override
    public float height() {
        float fMax;
        float parentW = this.parent != null ? this.parent.width() : 200.0f;
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescriptionLayout(parentW);
        }
        float nameWidth = measureTextWidth(this.name.effective(), nameFontSize, this.scaleFactor);
        float fWidth = (parentW - (parentW / 2.0f)) + PADDING;
        if (this.wrappedDescription != null || nameWidth >= fWidth) {
            float height = 0.0f + this.semiBoldFont.getHeight(14.0f);
            if (this.description != null && this.wrappedDescription != null) {
                height += GAP + this.descriptionHeight;
            }
            fMax = height + 35.0f;
        } else {
            fMax = Math.max(this.semiBoldFont.getHeight(14.0f), BUTTON_HEIGHT);
        }
        return fMax;
    }

    public String truncateWithEllipsis(String str, float f, float f2) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        float remaining = f - measureTextWidth("...", bindingFontSize, f2);
        if (remaining <= 0.0f) {
            return "...";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            if (measureTextWidth(sb.toString() + str.charAt(i), bindingFontSize, f2) > remaining) {
                return sb.toString() + "...";
            }
            sb.append(str.charAt(i));
        }
        return str;
    }

    public float measureTextWidth(String str, int i, float f) {
        return this.semiBoldFont.getWidth(str, Math.max(1, Math.round(i * f))) / f;
    }

    public float drawNameLabel(DrawCtx drawCtx, float f) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        drawCtx.text(this.semiBoldFont, this.name.effective(), nameFontSize, x(), f, colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.highlightAnimation.value()));
        return f + this.semiBoldFont.getHeight(14.0f);
    }

    public float drawDescriptionText(DrawCtx drawCtx, float f) {
        if (this.wrappedDescription == null) {
            return f;
        }
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        int iInterpolate = colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.highlightAnimation.value());
        float f2 = f + GAP;
        drawCtx.text(this.mediumFont, this.wrappedDescription, descriptionFontSize, x(), f2, iInterpolate);
        return f2 + this.descriptionHeight;
    }

    public void invalidateDescriptionCache() {
        this.descriptionDirty = true;
        this.cachedLayoutWidth = -1.0f;
        this.cachedDescriptionSource = null;
    }

    public void updateDescriptionLayout(float f) {
        if (this.description == null) {
            clearDescription(f);
            return;
        }
        String strEffective = this.description.effective();
        if (isDescriptionCacheValid(f, strEffective)) {
            return;
        }
        formatDescription(f, strEffective == null ? "" : strEffective);
    }

    public void clearDescription(float f) {
        this.wrappedDescription = null;
        this.descriptionHeight = 0.0f;
        this.descriptionDirty = false;
        this.cachedLayoutWidth = f;
        this.cachedDescriptionSource = null;
    }

    public boolean isDescriptionCacheValid(float f, String str) {
        return !this.descriptionDirty && f == this.cachedLayoutWidth && str != null && str.equals(this.cachedDescriptionSource);
    }

    public void formatDescription(float f, String str) {
        this.wrappedDescription = StringUtil.formatTextToFitWidth(str, f, this.mediumFont, descriptionFontSize);
        this.descriptionHeight = this.mediumFont.getHeightWithLineBreaks(this.wrappedDescription, descriptionFontSize);
        this.cachedLayoutWidth = f;
        this.cachedDescriptionSource = str;
        this.descriptionDirty = false;
    }

    public Translation name() {
        return this.name;
    }

    public KeybindSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }
}
