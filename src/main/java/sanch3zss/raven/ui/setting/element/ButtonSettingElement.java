package sanch3zss.raven.ui.setting.element;

import java.util.function.Supplier;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.HighlightAnimation;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ButtonSettingElement extends ModuleFrame {
    public static final int nameTextSize = 14;
    public static final int descriptionTextSize = 13;
    public static final int buttonTextSize = 12;
    public static final float descriptionGap = 3.0f;
    public static final float horizontalPadding = 10.0f;
    public static final float buttonHeight = MenuStyle.DROPDOWN_FIELD_HEIGHT;
    public static final float buttonWidth = 101.0f;
    public final Translation name;
    public final Translation description;
    public final Translation buttonLabel;
    public Supplier<Boolean> visibilitySupplier;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    public final GlTexture clickIcon = new GlTexture(new ClasspathResource("/icons/menu/new/click.png"));
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final WidgetBounds buttonBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public float scaleFactor = 1.0f;
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public String wrappedDescription = null;
    public boolean descriptionDirty = true;
    public float cachedWidth = -1.0f;
    public String cachedDescriptionText = null;
    public float descriptionHeight = 0.0f;
    public ButtonSettingElement(Translation translation3, Translation translation2, Translation translation, Runnable runnable) {
        this.name = translation3;
        this.description = translation2;
        this.buttonLabel = translation;
        this.clickBehavior.clickCallback(runnable);
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
        updateWrappedDescription(f);
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        float fY = y();
        float fValue = this.highlightAnimation.value();
        int iInterpolate = colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), fValue);
        int color = colorStack.computeColor(palette.surfaceBackground().tone(600).argb());
        int color2 = colorStack.computeColor(palette.surfaceOutline().tone(400).argb());
        int color3 = colorStack.computeColor(palette.text().tone(100).argb());
        int iInterpolate2 = colorStack.interpolate(color, colorStack.computeColor(palette.surfaceBackground().tone(500).argb()), this.clickBehavior.hoverAnimation());
        int iInterpolate3 = colorStack.interpolate(color2, colorStack.computeColor(palette.surfaceOutline().tone(300).argb()), this.clickBehavior.hoverAnimation());
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.titleFont, this.name.effective(), nameTextSize);
        float height = this.titleFont.getHeight(12.0f);
        float fTextWidthPhysical2 = drawCtx.textWidthPhysical(this.titleFont, this.buttonLabel.effective(), buttonTextSize);
        float buttonFieldWidth = Math.min(f, Math.max(buttonWidth, fTextWidthPhysical2 + 16.0f));
        float f3 = (f - buttonFieldWidth) - horizontalPadding;
        if (this.name.effective().isEmpty()) {
            this.buttonBounds.withPosition(x(), y()).withSize(f, buttonHeight);
            MenuStyle.drawSettingButton(drawCtx, x(), y(), f, buttonHeight, 4.0f,
                    this.clickBehavior.hoverAnimation().smoothAnimation());
            drawCtx.text(this.titleFont, this.buttonLabel.effective(), buttonTextSize,
                    x() + (f - fTextWidthPhysical2) / 2.0f, y() + (buttonHeight - height) / 2.0f, color3);
        } else if (this.wrappedDescription != null || fTextWidthPhysical >= f3) {
            drawCtx.text(this.titleFont, this.name.effective(), nameTextSize, x(), fY, iInterpolate);
            if (this.wrappedDescription != null) {
                fY += this.titleFont.getHeight(14.0f) + descriptionGap;
                drawCtx.text(this.descriptionFont, this.wrappedDescription, descriptionTextSize, x(), fY, colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), fValue));
            }
            this.buttonBounds.withPosition(x(), fY + (this.description != null ? this.descriptionHeight : this.titleFont.getHeight(14.0f)) + horizontalPadding).withSize(f, buttonHeight);
            float fY2 = this.buttonBounds.y() + (this.buttonBounds.height() / 2.0f);
            MenuStyle.drawSettingButton(drawCtx, this.buttonBounds.x(), this.buttonBounds.y(),
                    this.buttonBounds.width(), this.buttonBounds.height(), 4.0f,
                    this.clickBehavior.hoverAnimation().smoothAnimation());
            drawCtx.text(this.titleFont, this.buttonLabel.effective(), buttonTextSize, (this.buttonBounds.x() + (this.buttonBounds.width() / 2.0f)) - (fTextWidthPhysical2 / 2.0f), fY2 - (height / 2.0f), color3);
        } else {
            this.buttonBounds.withPosition((x() + f) - buttonFieldWidth, y()).withSize(buttonFieldWidth, buttonHeight);
            float fY3 = this.buttonBounds.y() + (this.buttonBounds.height() / 2.0f);
            drawCtx.text(this.titleFont, this.name.effective(), nameTextSize, x(), fY3 - (this.titleFont.getHeight(14.0f) / 2.0f), iInterpolate);
            MenuStyle.drawSettingButton(drawCtx, this.buttonBounds.x(), this.buttonBounds.y(),
                    this.buttonBounds.width(), this.buttonBounds.height(), 4.0f,
                    this.clickBehavior.hoverAnimation().smoothAnimation());
            drawCtx.text(this.titleFont, this.buttonLabel.effective(), buttonTextSize,
                    this.buttonBounds.x() +(this.buttonBounds.width() - fTextWidthPhysical2) / 2.0f,
                    fY3 - (height / 2.0f), color3);
        }
        this.clickBehavior.setDimensions(this.buttonBounds.x(), this.buttonBounds.y(),
                this.buttonBounds.width(), this.buttonBounds.height());
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.clickIcon.setDimensions(buttonTextSize, buttonTextSize);
        this.scaleFactor = layoutScaleContext.scaleFactor();
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickBehavior.handleInput(inputEventContext, z);
    }

    @Override
    public void handleClose() {
        super.handleClose();
    }

    @Override
    public float height() {
        float fMax;
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateWrappedDescription(this.parent.width() - 18.0f);
        }
        if (this.name.effective().isEmpty()) {
            return buttonHeight;
        }
        float nameWidth = measureTextWidth(this.name.effective(), nameTextSize);
        float fieldWidth = Math.max(buttonWidth,
                measureTextWidth(this.buttonLabel.effective(), buttonTextSize) + 16.0f);
        float fWidth = (this.parent.width() - fieldWidth) - horizontalPadding;
        if ((this.description == null || this.wrappedDescription == null) && nameWidth < fWidth) {
            fMax = Math.max(this.titleFont.getHeight(14.0f), buttonHeight);
        } else {
            float height = 0.0f + this.titleFont.getHeight(14.0f);
            if (this.description != null && this.wrappedDescription != null) {
                height += descriptionGap + this.descriptionHeight;
            }
            fMax = height + horizontalPadding + buttonHeight;
        }
        return fMax;
    }

    public float measureTextWidth(String str, int i) {
        if (this.scaleFactor <= 0.0f || this.scaleFactor == 1.0f) {
            return this.titleFont.getWidth(str, i);
        }
        return this.titleFont.getWidth(str, Math.max(1.0f, i * this.scaleFactor)) / this.scaleFactor;
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(Boolean.TRUE.equals(this.visibilitySupplier.get()));
        }
        this.highlightAnimation.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void updateWrappedDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.cachedWidth = f;
            this.cachedDescriptionText = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.cachedWidth || strEffective == null || !strEffective.equals(this.cachedDescriptionText)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descriptionFont, descriptionTextSize);
            this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descriptionTextSize);
            this.cachedWidth = f;
            this.cachedDescriptionText = strEffective;
            this.descriptionDirty = false;
        }
    }

    public Translation name() {
        return this.name;
    }

    public ButtonSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }
}
