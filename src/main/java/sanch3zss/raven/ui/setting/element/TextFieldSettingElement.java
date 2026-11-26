package sanch3zss.raven.ui.setting.element;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.DrawEngine;
import java.util.function.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.render.animation.HighlightAnimation;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class TextFieldSettingElement extends ModuleFrame {
    public static final int nameFontSize = 14;
    public static final int descriptionFontSize = 13;
    public static final int inputFontSize = 13;
    public static final float DESC_GAP = 3.0f;
    public static final float PADDING = 10.0f;
    public static final float FIELD_HEIGHT = 18.0f;
    public static final float COMPACT_WIDTH = 100.0f;

    public final Translation name;
    public final Translation placeholder;
    public final Translation description;
    public final boolean masked;
    public final Supplier<String> valueSupplier;
    public Consumer<String> changeCallback;
    public Supplier<Boolean> visibilitySupplier;
    public final MsdfFont nameFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont textFont = Fonts.INTER_MEDIUM.get();
    public final GlTexture pencilTexture = new GlTexture(new ClasspathResource("/icons/menu/new/pencil.png"));
    public final TextInputField inputField = new TextInputField(this.textFont, 13);
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final WidgetBounds inputBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public float scaleFactor = 1.0f;
    public String wrappedDescription = null;
    public boolean descriptionDirty = true;
    public float lastWrapWidth = -1.0f;
    public String lastDescription = null;
    public float descriptionHeight = 0.0f;


    public TextFieldSettingElement(Translation translation3, Translation translation2, Translation translation, boolean z, int i, boolean z2, Supplier<String> supplier) {
        this.name = translation3;
        this.placeholder = translation;
        this.description = translation2;
        this.masked = z2;
        this.valueSupplier = supplier;
        if (supplier != null) {
            this.inputField.setTextSilently(supplier.get());
        }
        this.inputField.numbersOnly(z);
        if (i != -1) {
            this.inputField.maxLength(i);
        }
        this.inputField.changeText(str2 -> {
            if (this.changeCallback != null) {
                this.changeCallback.accept(str2);
            }
        });    }

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
        if (!this.inputField.focused() && this.valueSupplier != null) {
            String value = this.valueSupplier.get();
            if (!this.inputField.text().equals(value == null ? "" : value)) {
                this.inputField.setTextSilently(value);
            }
        }
        updateDescription(f);
        ThemePalette palette = drawCtx.theme().palette();
        DrawEngine drawEngine = drawCtx.drawEngine();
        ColorStack colorStack = drawEngine.colorStack();
        float fY = y();
        float fValue = this.highlightAnimation.value();
        int iInterpolate = colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), fValue);
        String strText = (this.inputField.focused() || !this.masked) ? this.inputField.text() : "*".repeat(this.inputField.text().length());
        updateInputBounds(f);
        if (this.wrappedDescription != null) {
            drawCtx.text(this.nameFont, this.name.effective(), nameFontSize, x(), fY, iInterpolate);
            fY += this.nameFont.getHeight(14.0f) + DESC_GAP;
            drawCtx.text(this.textFont, this.wrappedDescription, 13, x(), fY, colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), fValue));
        } else {
            MenuStyle.drawFadingText(drawCtx, this.nameFont, this.name.effective(), nameFontSize, x(),
                    (this.inputBounds.y() + (this.inputBounds.height() / 2.0f)) - (this.nameFont.getHeight(14.0f) / 2.0f),
                    f - COMPACT_WIDTH - PADDING, iInterpolate);
        }
        this.inputField.listen(this.inputBounds.x(), this.inputBounds.y(), this.inputBounds.width(), this.inputBounds.height(), this.inputBounds.width() - 12.0f, this.inputBounds.x() + 6.0f, drawCtx.layoutContext().scaleFactor());
        MenuStyle.drawTextInput(drawCtx, this.inputField, this.textFont, strText,
                this.placeholder != null ? this.placeholder.effective() : "",
                this.inputBounds.x(), this.inputBounds.y(), this.inputBounds.width(), this.inputBounds.height());
    }

    public void updateInputBounds(float width) {
        if (this.wrappedDescription != null) {
            float fY = y() + this.nameFont.getHeight(14.0f) + DESC_GAP + this.descriptionHeight;
            this.inputBounds.withPosition(x(), fY + PADDING).withSize(width, FIELD_HEIGHT);
        } else {
            this.inputBounds.withPosition((x() + width) - COMPACT_WIDTH, y()).withSize(COMPACT_WIDTH, FIELD_HEIGHT);
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.pencilTexture.setDimensions(12, 12);
        this.scaleFactor = layoutScaleContext.scaleFactor();
        float parentW = this.parent != null ? this.parent.width() : 200.0f;
        if (this.description != null && this.parent != null) {
            updateDescription(parentW - 18.0f);
        }
        updateInputBounds(parentW);
        this.inputField.listen(this.inputBounds.x(), this.inputBounds.y(), this.inputBounds.width(), this.inputBounds.height(), this.inputBounds.width() - 12.0f, this.inputBounds.x() + 6.0f, this.scaleFactor);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.inputField.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(Boolean.TRUE.equals(this.visibilitySupplier.get()));
        }
        this.inputField.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public float height() {
        float fMax;
        float parentW = this.parent != null ? this.parent.width() : 200.0f;
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescription(parentW - 18.0f);
        }
        if (this.description == null || this.wrappedDescription == null) {
            fMax = Math.max(this.nameFont.getHeight(14.0f), FIELD_HEIGHT);
        } else {
            fMax = this.nameFont.getHeight(14.0f) + DESC_GAP + this.descriptionHeight + 35.0f;
        }
        return fMax;
    }

    @Override
    public void handleClose() {
        this.inputField.focused(false);        super.handleClose();
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.lastWrapWidth = f;
            this.lastDescription = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.lastWrapWidth || strEffective == null || !strEffective.equals(this.lastDescription)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.textFont, 13);
            this.descriptionHeight = this.textFont.getHeightWithLineBreaks(this.wrappedDescription, 13);
            this.lastWrapWidth = f;
            this.lastDescription = strEffective;
            this.descriptionDirty = false;
        }
    }

    public void focusAtEnd() {
        this.inputField.focusAtEnd();
    }

    public Translation name() {
        return this.name;
    }

    public TextFieldSettingElement onChange(Consumer<String> consumer) {
        this.changeCallback = consumer;
        return this;
    }

    public TextFieldSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }
}
