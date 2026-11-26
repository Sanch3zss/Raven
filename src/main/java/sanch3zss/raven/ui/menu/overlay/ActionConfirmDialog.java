package sanch3zss.raven.ui.menu.overlay;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.ui.dialog.ActionDialogData;
import java.util.Objects;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.button.LoadingButtonWidget;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.control.IconLabelBadge;
import org.joml.Vector4f;
import sanch3zss.raven.ui.component.layout.FrameElementColumn;
import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.window.MenuWindow;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ActionConfirmDialog extends AbstractFrame {
    public final MsdfFont semiBoldFont;
    public final MsdfFont mediumFont;
    public final GlTexture errorIcon;
    public final GlTexture progressIcon;
    public static final Translation confirmLabelText = Lang.ACTION_CONFIRM;
    public static final String clickIconPath = "/icons/menu/new/click.png";
    public static final float defaultWidth = 320.0f;
    public static final float contentPadding = 12.0f;
    public static final float boxCornerRadius = 10.0f;
    public static final float elementSpacing = 8.0f;
    public static final float boxOutlineThickness = 2.5f;
    public static final float windowCornerRadius = 8.0f;
    public static final float sectionSpacing = 16.0f;
    public static final int animationDurationMs = 220;
    public static final float backgroundAlpha = 0.8f;
    public String errorMessage;
    public final ToggleAnimator errorAnimator;

    public final WidgetBounds errorBounds;
    public final WidgetBounds dialogBounds;
    public final ToggleAnimator openAnimator;
    public final LoadingButtonWidget confirmButton;
    private final ClickableBehavior alertCancel = new ClickableBehavior().clickCallback(this::close);
    private final ClickableBehavior alertConfirm = new ClickableBehavior().clickCallback(this::confirmFromAlert);
    public IconLabelBadge iconBadge;

    public ActionDialogData dialogData;
    int layoutFramesRemaining;
    public boolean opened;

    public ActionConfirmDialog() {
        super(new FrameElementColumn(296.0f, contentPadding));
        this.semiBoldFont = Fonts.INTER_SEMIBOLD.get();
        this.mediumFont = Fonts.INTER_MEDIUM.get();
        this.errorIcon = new GlTexture(new ClasspathResource("/icons/menu/new/error.png"));
        this.progressIcon = new GlTexture(new ClasspathResource("/icons/menu/new/progress.png"));
        this.errorMessage = null;
        this.errorAnimator = new ToggleAnimator(animationDurationMs, Easings.EASE_IN_OUT_CUBIC);
        this.errorBounds = new WidgetBounds(0.0f, 0.0f, defaultWidth, 0.0f);
        this.dialogBounds = new WidgetBounds(0.0f, 0.0f, defaultWidth, 0.0f);
        this.openAnimator = new ToggleAnimator(animationDurationMs, Easings.EASE_IN_OUT_CUBIC);
        this.layoutFramesRemaining = 2;
        setSize(MenuWindow.MENU_WIDTH, MenuWindow.MENU_HEIGHT);
        this.confirmButton = new LoadingButtonWidget(new GlTexture(new ClasspathResource(clickIconPath)), 12, confirmLabelText, 296, 25, this::confirm, 8.0f, true);
        this.confirmButton.loadingVisuals(this.progressIcon, Lang.ACTION_LOADING);
        addChild(this.confirmButton);
    }

    public void setLoading(boolean z, String str) {
        if (str != null && !str.isBlank()) {
            this.confirmButton.loadingVisuals(this.progressIcon, Translation.clearText(str));
        }
        this.confirmButton.loading(z);
    }

    public void showError(String str) {
        this.errorMessage = str;
        this.errorAnimator.state(true);
    }

    public void hideError() {
        this.errorAnimator.state(false);
        if (this.errorAnimator.isZero()) {
            this.errorMessage = null;
        }
    }

    public void open(ActionDialogData actionDialogData) {
        Objects.requireNonNull(actionDialogData, "ActionData cannot be null");
        this.dialogData = actionDialogData;
        this.iconBadge = new IconLabelBadge(actionDialogData.icon(), actionDialogData.placeholderText());
        if (actionDialogData.needsChangePlaceholderColor()) {
            this.iconBadge.setBaseColor(Integer.valueOf(actionDialogData.placeHolderColor()));
        }
        this.confirmButton.primary(new GlTexture(new ClasspathResource(clickIconPath)), 12, actionDialogData.confirmLabel() != null ? actionDialogData.confirmLabel() : confirmLabelText);
        this.confirmButton.loading(false);
        this.confirmButton.enabled(actionDialogData.enableConfirmButton() != null ? actionDialogData.enableConfirmButton() : () -> {
            return true;
        });
        this.frameElementsContainer.clearElements();
        if (actionDialogData.contentConfigurator() != null) {
            actionDialogData.contentConfigurator().accept(this.frameElementsContainer);
        }
        this.layoutFramesRemaining = 2;
        this.opened = !this.opened;
        this.openAnimator.state(this.opened);
    }

    public void close() {
        this.opened = false;
        this.openAnimator.state(false);
        hideError();
        setLoading(false, "");
    }

    public boolean isOpen() {
        return this.opened || !this.openAnimator.isZero();
    }

    public float computeContentHeight() {
        float fHeight = 16.0f;
        if (this.iconBadge != null) {
            fHeight = sectionSpacing + this.iconBadge.height() + 8.0f;
        }
        float height = fHeight + this.semiBoldFont.getHeight(sectionSpacing);
        if (this.dialogData.description() != null) {
            height += 8.0f + this.mediumFont.getHeightWithLineBreaks(this.dialogData.description().effective(), 12);
        }
        float fHeight2 = height + sectionSpacing;
        if (this.frameElementsContainer.height() > 0.0f) {
            fHeight2 = fHeight2 + this.frameElementsContainer.height() + sectionSpacing;
        }
        return fHeight2 + 44.0f;
    }

    private void drawAlertText(DrawCtx drawCtx, MsdfFont font, String text, int size, float textY, int color) {
        float lineY = textY;
        for (String line : text.split("\n")) {
            float lineWidth = drawCtx.textWidthPhysical(font, line, size);
            drawCtx.text(font, line, size, this.dialogBounds.x() + (this.dialogBounds.width() - lineWidth) / 2.0f,
                    lineY, color);
            lineY += font.getHeight(size);
        }
    }

    private void drawAlertButtons(DrawCtx drawCtx, ColorStack colorStack, ThemePalette palette) {
        float buttonsY = this.dialogBounds.y() + this.dialogBounds.height() - 44.0f;
        float half = this.dialogBounds.width() / 2.0f;
        int separator = MenuStyle.outline(colorStack);
        drawCtx.hairline(this.dialogBounds.x(), buttonsY, this.dialogBounds.width(), separator);
        drawCtx.verticalHairline(this.dialogBounds.x() + half, buttonsY, 44.0f, separator);
        int accent = colorStack.computeColor(palette.accent().argb());
        boolean enabled = this.dialogData.enableConfirmButton() == null || this.dialogData.enableConfirmButton().getAsBoolean();
        String cancel = Lang.CANCEL.effective();
        String confirm = this.confirmButton.loading() ? Lang.ACTION_LOADING.effective()
                : (this.dialogData.confirmLabel() != null ? this.dialogData.confirmLabel() : confirmLabelText).effective();
        float cancelHover = this.alertCancel.hoverAnimation().smoothAnimation();
        float confirmHover = this.alertConfirm.hoverAnimation().smoothAnimation();
        if (cancelHover > 0.005f) {
            drawCtx.fillRoundedRect(this.dialogBounds.x(), buttonsY, half, 44.0f, new Vector4f(0.0f, 0.0f, 0.0f, 14.0f),
                    MenuStyle.hoverTint(colorStack, 0.08f * cancelHover));
        }
        if (confirmHover > 0.005f && enabled) {
            drawCtx.fillRoundedRect(this.dialogBounds.x() + half, buttonsY, half, 44.0f, new Vector4f(0.0f, 14.0f, 0.0f, 0.0f),
                    MenuStyle.hoverTint(colorStack, 0.08f * confirmHover));
        }
        float textY = buttonsY + (44.0f - this.semiBoldFont.getHeight(13.0f)) / 2.0f;
        drawCtx.text(this.mediumFont, cancel, 13,
                this.dialogBounds.x() + (half - drawCtx.textWidthPhysical(this.mediumFont, cancel, 13)) / 2.0f, textY, accent);
        drawCtx.text(this.semiBoldFont, confirm, 13,
                this.dialogBounds.x() + half + (half - drawCtx.textWidthPhysical(this.semiBoldFont, confirm, 13)) / 2.0f,
                textY, enabled ? accent : colorStack.computeColor(palette.text().tone(500).argb()));
        this.alertCancel.setDimensions(this.dialogBounds.x(), buttonsY, half, 44.0f);
        this.alertConfirm.setDimensions(this.dialogBounds.x() + half, buttonsY, half, 44.0f);
    }

    public float computeErrorHeight() {
        float heightWithLineBreaks = 24.0f;
        if (this.errorMessage != null) {
            heightWithLineBreaks = 24.0f + this.semiBoldFont.getHeightWithLineBreaks(this.errorMessage, 12);
        }
        return heightWithLineBreaks;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.openAnimator.isZero() || this.dialogData == null) {
            return;
        }
        if (this.layoutFramesRemaining > 0) {
            this.layoutFramesRemaining--;
            this.dialogBounds.withSize(this.dialogData.width() > 0.0f ? this.dialogData.width() : defaultWidth, computeContentHeight());
        }
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        colorStack.push();
        try {
            colorStack.alpha(this.openAnimator.smoothAnimation());
            drawCtx.roundedBlur(sanch3zss.raven.render.gl.FramebufferUtil.getColorAttachmentId(Raven.INSTANCE.windowController().menuSceneBlur().getBlurFramebuffer()), x(), y(), width(), height(), 8.0f, colorStack.white());
            drawCtx.fillRoundedRect(x() - 1.0f, y(), width() + 2.0f, height() + 1.0f, 8.0f, colorStack.computeColor(palette.surfaceBackground().tone(900).argb(), backgroundAlpha));
            colorStack.push();
            colorStack.alpha(this.errorAnimator.smoothAnimation());
            drawCtx.fillOutlinedRoundedRect(this.errorBounds.x(), this.errorBounds.y(), this.errorBounds.width(), this.errorBounds.height(), boxCornerRadius, boxOutlineThickness, colorStack.computeColor(15620209, 0.3f), colorStack.computeColor(15620209, 0.07f));
            drawCtx.textureVerticalC(this.errorIcon, this.errorBounds.x() + contentPadding, this.errorBounds.y() + (this.errorBounds.height() / 2.0f), this.errorIcon.width(), this.errorIcon.height(), colorStack.computeColor(palette.error().tone(500).argb()));
            if (this.errorMessage != null) {
                drawCtx.text(this.semiBoldFont, this.errorMessage, 12, this.errorBounds.x() + contentPadding + this.errorIcon.width() + 8.0f, (this.errorBounds.y() + (this.errorBounds.height() / 2.0f)) - (this.semiBoldFont.getHeightWithLineBreaks(this.errorMessage, 12) / 2.0f), colorStack.computeColor(palette.error().tone(500).argb()));
            }
            colorStack.pop();
            MenuStyle.drawDialog(drawCtx, this.dialogBounds.x(), this.dialogBounds.y(), this.dialogBounds.width(), this.dialogBounds.height(), boxCornerRadius);
            float fY = this.dialogBounds.y() + sectionSpacing;
            if (this.iconBadge != null) {
                this.iconBadge.render(drawCtx);
                fY = this.iconBadge.y() + this.iconBadge.height() + 8.0f;
            }
            drawAlertText(drawCtx, this.semiBoldFont, this.dialogData.title().effective(), 16, fY, colorStack.computeColor(palette.text().tone(100).argb()));
            float height = fY + this.semiBoldFont.getHeight(sectionSpacing) + 8.0f;
            if (this.dialogData.description() != null) {
                drawAlertText(drawCtx, this.mediumFont, this.dialogData.description().effective(), 12, height, colorStack.computeColor(palette.text().tone(300).argb()));
            }
            if (this.frameElementsContainer.height() > 0.0f) {
                renderFrameElements(drawCtx);
            }
            drawAlertButtons(drawCtx, colorStack, palette);
        } finally {
            colorStack.pop();
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        if (this.dialogData == null) {
            return;
        }
        this.errorIcon.setDimensions(15, 15);
        this.errorBounds.withSize(this.dialogData.width() > 0.0f ? this.dialogData.width() : defaultWidth, computeErrorHeight());
        float fX = x() + (width() / 2.0f);
        float fY = y() + (height() / 2.0f);
        float fHeight = this.errorBounds.height() * this.errorAnimator.smoothAnimation();
        float fSmoothAnimation = 6.0f * this.errorAnimator.smoothAnimation();
        float fHeight2 = fY - (((fHeight + fSmoothAnimation) + this.dialogBounds.height()) / 2.0f);
        if (this.errorMessage != null && !this.errorAnimator.isZero()) {
            this.errorBounds.withPosition(fX - (this.errorBounds.width() / 2.0f), fHeight2);
        }
        this.dialogBounds.withPosition(fX - (this.dialogBounds.width() / 2.0f), fHeight2 + fHeight + fSmoothAnimation);
        float fY2 = this.dialogBounds.y() + sectionSpacing;
        if (this.iconBadge != null) {
            this.iconBadge.setPosition(this.dialogBounds.x() + contentPadding, fY2);
            this.iconBadge.layout(layoutScaleContext);
            fY2 = this.iconBadge.y() + this.iconBadge.height() + 8.0f;
        }
        float height = fY2 + this.semiBoldFont.getHeight(sectionSpacing);
        if (this.dialogData.description() != null) {
            height += 8.0f + this.mediumFont.getHeightWithLineBreaks(this.dialogData.description().effective(), 12);
        }
        float fHeight3 = height + sectionSpacing;
        if (this.frameElementsContainer.height() > 0.0f) {
            this.frameElementsContainer.setPosition(this.dialogBounds.x() + contentPadding, fHeight3);
            fHeight3 += this.frameElementsContainer.height() + sectionSpacing;
        }
        this.confirmButton.setPosition(this.dialogBounds.x() + contentPadding, fHeight3);
        this.confirmButton.layout(layoutScaleContext);
        super.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.openAnimator.animate(weightedEngine);
        this.confirmButton.animation(weightedEngine);
        this.alertCancel.animate(weightedEngine);
        this.alertConfirm.animate(weightedEngine);
        this.errorAnimator.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (this.openAnimator.isZero() || this.dialogData == null) {
            return false;
        }
        if (!z) {
            InputEvent inputEvent = inputEventContext.inputEvent();
            if (inputEvent instanceof KeyInput) {
                KeyInput keyInput2 = (KeyInput) inputEvent;
                if (keyInput2.keyAction().press() && (keyInput2.keyCode() == 257 || keyInput2.keyCode() == 335)) {
                    if ((this.dialogData.enableConfirmButton() == null || this.dialogData.enableConfirmButton().getAsBoolean()) && !this.confirmButton.loading() && this.dialogData.onConfirm() != null) {
                        confirm();
                        return true;
                    }
                }
            }
        }
        boolean buttonsHandled = this.alertCancel.handleInput(inputEventContext, z)
                | this.alertConfirm.handleInput(inputEventContext, z);
        if (!(inputEventContext.inputEvent() instanceof MouseButtonInput)) {
            buttonsHandled = false;
        }
        if (((inputEventContext.inputEvent() instanceof ScrollInput) && !z) || buttonsHandled || super.handleInput(inputEventContext, z)) {
            return true;
        }
        if (z) {
            return false;
        }
        InputEvent inputEvent2 = inputEventContext.inputEvent();
        if (inputEvent2 instanceof KeyInput) {
            KeyInput keyInput = (KeyInput) inputEvent2;
            if ((keyInput.keyAction().press() || keyInput.keyAction().repeat()) && keyInput.keyCode() == 256) {
                close();
                return true;
            }
        }
        InputEvent inputEvent3 = inputEventContext.inputEvent();
        if (inputEvent3 instanceof MouseButtonInput) {
            MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent3;
            if (mouseButtonInput.action().press() && mouseButtonInput.button() == 0) {
                if (inputEventContext.inArea(this.dialogBounds.x(), this.dialogBounds.y(), this.dialogBounds.width(), this.dialogBounds.height())) {
                    return true;
                }
                close();
                return true;
            }
        }
        return inputEventContext.inputEvent() instanceof CursorMoveInput;
    }

    private void confirmFromAlert() {
        boolean enabled = this.dialogData == null || this.dialogData.enableConfirmButton() == null
                || this.dialogData.enableConfirmButton().getAsBoolean();
        if (enabled && !this.confirmButton.loading()) {
            confirm();
        }
    }

    public void confirm() {
        if (this.dialogData == null || this.dialogData.onConfirm() == null) {
            return;
        }
        this.dialogData.onConfirm().run();
    }

    @Override
    public float contentHeight() {
        return this.dialogBounds.height();
    }

    public boolean isOpened() {
        return this.opened;
    }
}
