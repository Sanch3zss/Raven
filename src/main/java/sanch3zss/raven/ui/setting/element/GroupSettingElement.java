package sanch3zss.raven.ui.setting.element;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import sanch3zss.raven.render.engine.ScreenResolution;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.*;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.feature.setting.KeybindCaptureState;import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.ui.menu.window.SettingsGroupWindow;
import sanch3zss.raven.ui.setting.Setting;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class GroupSettingElement extends ModuleFrame {
    static final int titleTextSize = 14;
    static final int descTextSize = 13;
    static final int bodyTextSize = 12;
    static final float GAP = 3.0f;
    static final float PADDING = 10.0f;
    static final float BUTTON_HEIGHT = 25.0f;
    static final float MARGIN = 5.0f;

    public final Translation name;
    public final Translation description;
    public Supplier<Boolean> toggleSupplier;
    public Supplier<Boolean> visibilitySupplier;
    public Runnable changeCallback;
    public String wrappedDescription;
    public String cachedDescriptionSource;
    public float descriptionHeight;
    public ScreenResolution resolution;

    public final SettingsGroupWindow groupWindow;
    public final MsdfFont semiBoldFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont mediumFont = Fonts.INTER_MEDIUM.get();
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ClickableBehavior toggleClickable = new ClickableBehavior();
    public final ClickableBehavior keybindClickable = new ClickableBehavior();
    public final ClickableBehavior groupClickable = new ClickableBehavior();
    public final ToggleAnimator toggleAnimator = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);
    public final KeybindCaptureState keybindCapture = new KeybindCaptureState();
    public final WidgetBounds bindingBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds groupButtonBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public boolean lastToggleState = false;
    public float lastDescriptionWidth = -1.0f;
    public boolean descriptionDirty = true;
    private boolean navigationOnly;

    public GroupSettingElement(Translation translation2, Translation translation, List<Setting> list, List<Integer> list2) {
        this.name = translation2;
        this.description = translation;
        this.keybindCapture.addMutableKeys(list2);
        this.toggleClickable.clickCallback(this::invert);
        this.keybindClickable.clickCallback(this.keybindCapture::toggleCapture);
        this.groupWindow = new SettingsGroupWindow(translation2, list, true, false);
        this.groupClickable.clickCallback(this::toggleGroupWindow);
        Raven.INSTANCE.menuWindow().priorityOverlayHandler().registerPopup(this.groupWindow);
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.keybindCapture.onChange(consumer);
    }

    public void toggleGroupWindow() {
        float fDpiScaleFactor = Raven.INSTANCE.windowController().dpiScaleFactor();
        ScreenResolution res = this.resolution != null ? this.resolution : ScreenResolution.resolution();
        float fScreenWidth = res.screenWidth() / fDpiScaleFactor;
        float fScreenHeight = res.screenHeight() / fDpiScaleFactor;
        float fWidth = this.groupWindow.width();
        float f2 = this.groupWindow.totalHeight();
        float fX = this.groupButtonBounds.x() + this.groupButtonBounds.width() + 8.0f;
        float fX2 = (this.groupButtonBounds.x() - 8.0f) - fWidth;
        if (fX + fWidth > fScreenWidth - MARGIN && fX2 >= MARGIN) {
            fX = fX2;
        }
        float fMax = Math.max(MARGIN, Math.min(fX, (fScreenWidth - fWidth) - MARGIN));
        float visibleY = this.groupButtonBounds.y() - this.viewportTop;
        float fY = (visibleY + (this.groupButtonBounds.height() / 2.0f)) - (f2 / 2.0f);
        if (fY + f2 > fScreenHeight - MARGIN) {
            fY = (fScreenHeight - f2) - MARGIN;
        }
        if (fY < MARGIN) {
            fY = MARGIN;
        }
        if (this.groupWindow.isOpen()) {
            this.groupWindow.closeWindow();
        } else {
            this.groupWindow.setPosition(fMax, fY);
            this.groupWindow.openWindow();
            Raven.INSTANCE.menuWindow().priorityOverlayHandler().bringPopupToFront(this.groupWindow);
        }
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
    public void draw(DrawCtx drawCtx, float f2, float f3) {
        if (this.keybindCapture.captureKey()) {
            drawCtx.window().interceptKeyboard(true);
        }
        this.resolution = drawCtx.resolution();
        updateDescriptionLayout(f2);
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        float contentY = drawDescription(drawCtx, drawTitle(drawCtx, y())) + PADDING;
        if (this.navigationOnly) {
            this.groupButtonBounds.withPosition(x(), contentY).withSize(f2, BUTTON_HEIGHT);
            this.groupClickable.setDimensions(this.groupButtonBounds.x(), this.groupButtonBounds.y(),
                    this.groupButtonBounds.width(), this.groupButtonBounds.height());
            drawGroupButton(drawCtx);
            return;
        }
        float toggleGap = 8.0f;
        float toggleSize = 18.0f;
        float buttonsWidth = Math.max(80.0f, f2 - toggleSize - toggleGap);
        float buttonWidth = (buttonsWidth - 6.0f) / 2.0f;
        this.bindingBounds.withPosition(x(), contentY).withSize(buttonWidth, BUTTON_HEIGHT);
        this.keybindClickable.setDimensions(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height());
        MenuStyle.drawDropdownField(drawCtx, this.bindingBounds.x(), this.bindingBounds.y(),
                this.bindingBounds.width(), this.bindingBounds.height(), 4.0f,
                this.keybindClickable.hoverAnimation().smoothAnimation());
        String bindingText = this.keybindCapture.formatBindingText();
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.semiBoldFont, bindingText, bodyTextSize);
        float height = this.semiBoldFont.getHeight(12.0f);
        float fWidth2 = this.bindingBounds.width() - 16.0f;
        String displayText = bindingText;
        if (fTextWidthPhysical > fWidth2) {
            displayText = truncateText(drawCtx, bindingText, fWidth2);
        }
        float fX = this.bindingBounds.x() + ((this.bindingBounds.width()
                - drawCtx.textWidthPhysical(this.semiBoldFont, displayText, bodyTextSize)) / 2.0f);
        float fY = this.bindingBounds.y() + (this.bindingBounds.height() / 2.0f);
        int color = colorStack.computeColor(palette.text().tone(100).argb());
        drawCtx.text(this.semiBoldFont, displayText, bodyTextSize, fX,
                fY - (height / 2.0f), color);
        this.groupButtonBounds.withPosition(x() + this.bindingBounds.width() + 6.0f, contentY)
                .withSize(buttonWidth, BUTTON_HEIGHT);
        this.groupClickable.setDimensions(this.groupButtonBounds.x(), this.groupButtonBounds.y(), this.groupButtonBounds.width(), this.groupButtonBounds.height());
        drawGroupButton(drawCtx);
        drawToggle(drawCtx, contentY, f2);
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        this.groupWindow.collectBloomElements(renderCommandQueue);
        super.collectBloomElements(renderCommandQueue);
    }

    @Override
    public void drawOverlay(DrawCtx drawCtx) {
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.groupClickable.setDimensions(this.groupButtonBounds.x(), this.groupButtonBounds.y(), this.groupButtonBounds.width(), this.groupButtonBounds.height());
        if (!this.navigationOnly) {
            this.keybindClickable.setDimensions(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height());
        }
        this.groupWindow.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (this.navigationOnly) {
            boolean handled = this.groupClickable.handleInput(inputEventContext, z);
            return handled | super.handleInput(inputEventContext, handled);
        }
        boolean zHandleInput = this.keybindClickable.handleInput(inputEventContext, z);
        if (this.keybindCapture.captureKey()) {
            InputEvent inputEvent = inputEventContext.inputEvent();
            if (inputEvent instanceof MouseButtonInput mouseButtonInput) {
                if (mouseButtonInput.action().press() && mouseButtonInput.button() == 0 && !inputEventContext.inArea(this.bindingBounds.x(), this.bindingBounds.y(), this.bindingBounds.width(), this.bindingBounds.height())) {
                    this.keybindCapture.toggleCapture();
                    zHandleInput = true;
                }
            }
        }
        boolean zHandleInput2 = zHandleInput | this.groupClickable.handleInput(inputEventContext, zHandleInput);
        InputEvent inputEvent2 = inputEventContext.inputEvent();
        if (inputEvent2 instanceof KeyInput keyInput) {
            zHandleInput2 |= this.keybindCapture.handleKeyInput(keyInput) && !zHandleInput2;
        }
        InputEvent inputEvent3 = inputEventContext.inputEvent();
        if (inputEvent3 instanceof MouseButtonInput mouseButtonInput) {
            zHandleInput2 |= this.keybindCapture.handleMouseInput(mouseButtonInput) && !zHandleInput2;
        }
        boolean zHandleInput3 = zHandleInput2 | this.toggleClickable.handleInput(inputEventContext, zHandleInput2);
        return zHandleInput3 | super.handleInput(inputEventContext, zHandleInput3);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.toggleSupplier != null) {
            boolean toggled = Boolean.TRUE.equals(this.toggleSupplier.get());
            if (toggled != this.lastToggleState) {
                this.toggleAnimator.state(toggled);
                this.lastToggleState = toggled;
            }
        }
        if (this.visibilitySupplier != null) {
            visible(Boolean.TRUE.equals(this.visibilitySupplier.get()));
        }
        this.groupWindow.animation(weightedEngine);
        this.groupClickable.animate(weightedEngine);
        this.keybindClickable.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        this.toggleClickable.animate(weightedEngine);
        this.toggleAnimator.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {
        this.groupWindow.closeWindow();
        super.handleClose();
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.resolution != null) {
            updateDescriptionLayout(this.resolution.width());
        }
        float height = this.semiBoldFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += GAP + this.descriptionHeight;
        }
        return height + PADDING + BUTTON_HEIGHT;
    }

    public void invert() {
        if (this.changeCallback != null) {
            this.changeCallback.run();
        }
    }

    public String truncateText(DrawCtx drawCtx, String str, float f2) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        float fTextWidthPhysical = f2 - drawCtx.textWidthPhysical(this.semiBoldFont, "...", bodyTextSize);
        if (fTextWidthPhysical <= 0.0f) {
            return "...";
        }
        StringBuilder sb = new StringBuilder();
        for (int i2 = 0; i2 < str.length(); i2++) {
            if (drawCtx.textWidthPhysical(this.semiBoldFont, sb.toString() + str.charAt(i2), bodyTextSize) > fTextWidthPhysical) {
                return sb.toString() + "...";
            }
            sb.append(str.charAt(i2));
        }
        return str;
    }

    public float drawTitle(DrawCtx drawCtx, float f2) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        drawCtx.text(this.semiBoldFont, this.name.effective(), titleTextSize, x(), f2, colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.highlightAnimation.value()));
        return f2 + this.semiBoldFont.getHeight(14.0f);
    }

    public float drawDescription(DrawCtx drawCtx, float f2) {
        if (this.wrappedDescription == null) {
            return f2;
        }
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        int iInterpolate = colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.highlightAnimation.value());
        float f3 = f2 + GAP;
        drawCtx.text(this.mediumFont, this.wrappedDescription, descTextSize, x(), f3, iInterpolate);
        return f3 + this.descriptionHeight;
    }

    public void drawGroupButton(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        MenuStyle.drawDropdownField(drawCtx, this.groupButtonBounds.x(), this.groupButtonBounds.y(),
                this.groupButtonBounds.width(), this.groupButtonBounds.height(), 4.0f,
                this.groupClickable.hoverAnimation().smoothAnimation());
        String strEffective = Lang.SHOW_GROUP.effective();
        float fX = this.groupButtonBounds.x() + ((this.groupButtonBounds.width()
                - drawCtx.textWidthPhysical(this.semiBoldFont, strEffective, bodyTextSize)) / 2.0f);
        float fY = this.groupButtonBounds.y() + (this.groupButtonBounds.height() / 2.0f);
        int color = colorStack.computeColor(palette.text().tone(100).argb());
        drawCtx.text(this.semiBoldFont, strEffective, bodyTextSize, fX,
                fY - (this.semiBoldFont.getHeight(12.0f) / 2.0f), color);
    }

    public void drawToggle(DrawCtx drawCtx, float f2, float f3) {
        float scale = Math.max(drawCtx.layoutContext().scaleFactor(), 0.01f);
        float fX = drawCtx.layoutContext().alignToScale((x() + f3) - 18.0f);
        float f4 = (f2 + 12.5f) - (18.0f / 2.0f);
        float alignedY = drawCtx.layoutContext().alignToScale(f4);
        float size = Math.round(18.0f * scale) / scale;
        MenuStyle.drawCheckbox(drawCtx, fX, alignedY, size, this.toggleAnimator.smoothAnimation(),
                this.toggleClickable.hoverAnimation().smoothAnimation());
        this.toggleClickable.setDimensions(fX - 3.0f, alignedY - 3.0f, size + 6.0f, size + 6.0f);
    }

    public void resetDescriptionCache() {
        this.descriptionDirty = true;
        this.lastDescriptionWidth = -1.0f;
        this.cachedDescriptionSource = null;
    }

    public void updateDescriptionLayout(float f2) {
        if (this.description == null) {
            clearDescriptionLayout(f2);
            return;
        }
        String strEffective = this.description.effective();
        if (isDescriptionUpToDate(f2, strEffective)) {
            return;
        }
        computeDescriptionLayout(f2, strEffective == null ? "" : strEffective);
    }

    public void clearDescriptionLayout(float f2) {
        this.wrappedDescription = null;
        this.descriptionHeight = 0.0f;
        this.descriptionDirty = false;
        this.lastDescriptionWidth = f2;
        this.cachedDescriptionSource = null;
    }

    public boolean isDescriptionUpToDate(float f2, String str) {
        return !this.descriptionDirty && f2 == this.lastDescriptionWidth && str != null && str.equals(this.cachedDescriptionSource);
    }

    public void computeDescriptionLayout(float f2, String str) {
        this.wrappedDescription = StringUtil.formatTextToFitWidth(str, f2, this.mediumFont, descTextSize);
        this.descriptionHeight = this.mediumFont.getHeightWithLineBreaks(this.wrappedDescription, descTextSize);
        this.lastDescriptionWidth = f2;
        this.cachedDescriptionSource = str;
        this.descriptionDirty = false;
    }

    public Translation name() {
        return this.name;
    }

    public GroupSettingElement toggledSupplier(Supplier<Boolean> supplier) {
        this.toggleSupplier = supplier;
        return this;
    }

    public GroupSettingElement navigationOnly(boolean navigationOnly) {
        this.navigationOnly = navigationOnly;
        return this;
    }

    public GroupSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }

    public GroupSettingElement changeRunnable(Runnable runnable) {
        this.changeCallback = runnable;
        return this;
    }
}
