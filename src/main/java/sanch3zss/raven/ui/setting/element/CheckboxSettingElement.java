package sanch3zss.raven.ui.setting.element;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import sanch3zss.raven.render.engine.ScreenResolution;
import java.util.List;
import java.util.function.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.feature.setting.BindMode;import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.button.IconButtonWidget;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.ui.menu.window.KeybindConfigWindow;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class CheckboxSettingElement extends ModuleFrame {
    public static final float descriptionGap = 3.0f;
    public static final int nameTextSize = 14;
    public static final int descriptionTextSize = 13;
    public final MsdfFont titleFont;
    public final MsdfFont descriptionFont;
    public final GlTexture checkmarkIcon;
    public final Translation name;
    public final Translation description;
    public final ToggleAnimator checkAnimator;
    public final ClickableBehavior clickBehavior;
    public final HighlightAnimation highlightAnimation;
    public Supplier<Boolean> toggledStateSupplier;
    public Supplier<Boolean> visibilitySupplier;
    public Runnable changeAction;

    public final IconButtonWidget keybindButton;
    public boolean showKeybindButton;
    public boolean lastToggledState;
    public String wrappedDescription;
    public boolean descriptionDirty;
    public float cachedWidth;
    public String cachedDescriptionText;
    public float descriptionHeight;
    public final WidgetBounds keybindButtonBounds;
    public final KeybindConfigWindow keybindWindow;
    public ScreenResolution resolution;
    public static final float windowGap = 8.0f;
    public static final float margin = 5.0f;

    public CheckboxSettingElement(Translation translation2, Translation translation, Supplier<List<Integer>> supplier) {
        this(translation2, translation, supplier, true);
    }

    public CheckboxSettingElement(Translation translation2, Translation translation, Supplier<List<Integer>> supplier, boolean z) {
        this.titleFont = Fonts.INTER_SEMIBOLD.get();
        this.descriptionFont = Fonts.INTER_MEDIUM.get();
        this.checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
        this.checkAnimator = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);
        this.clickBehavior = new ClickableBehavior();
        this.highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
        this.lastToggledState = false;
        this.wrappedDescription = null;
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescriptionText = null;
        this.descriptionHeight = 0.0f;
        this.keybindButtonBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.name = translation2;
        this.description = translation;
        this.showKeybindButton = z;
        this.keybindWindow = new KeybindConfigWindow(Lang.BINDING_MODULE, translation2.effective(), supplier);
        Raven.INSTANCE.menuWindow().priorityOverlayHandler().registerPopup(this.keybindWindow);
        this.clickBehavior.clickCallback(this::invert);
        this.keybindButton = new IconButtonWidget(Fonts.CATEGORIES2.get(), "b", 14, this::toggleKeybindWindow, 16.0f, 16.0f);
    }

    public void setupBindType(Supplier<BindMode> supplier, Consumer<BindMode> consumer) {
        if (this.keybindWindow != null) {
            this.keybindWindow.bindType(supplier, consumer);
        }
    }

    public void onChange(Consumer<List<Integer>> consumer) {
        this.keybindWindow.onChange(consumer);
    }

    public void toggleKeybindWindow() {
        float fDpiScaleFactor = Raven.INSTANCE.windowController().dpiScaleFactor();
        if (this.keybindWindow.isOpen() || this.resolution == null) {
            this.keybindWindow.closeWindow();
            return;
        }
        float fScreenWidth = this.resolution.screenWidth() / fDpiScaleFactor;
        float fScreenHeight = this.resolution.screenHeight() / fDpiScaleFactor;
        float fWidth = this.keybindWindow.width();
        float f = this.keybindWindow.totalHeight();
        float fX = this.keybindButtonBounds.x();
        float fY = this.keybindButtonBounds.y() - this.viewportTop;
        float fWidth2 = this.keybindButtonBounds.width();
        float fHeight = this.keybindButtonBounds.height();
        float f2 = fX + fWidth2 + windowGap;
        float f3 = (fX - windowGap) - fWidth;
        if (f2 + fWidth > fScreenWidth - margin && f3 >= margin) {
            f2 = f3;
        }
        float fMax = Math.max(margin, Math.min(f2, (fScreenWidth - fWidth) - margin));
        float f4 = (fY + (fHeight / 2.0f)) - (f / 2.0f);
        if (f4 + f > fScreenHeight - margin) {
            f4 = (fScreenHeight - f) - margin;
        }
        if (f4 < margin) {
            f4 = 5.0f;
        }
        this.keybindWindow.openWindow();
        this.keybindWindow.setPosition(fMax, f4);
        Raven.INSTANCE.menuWindow().priorityOverlayHandler().bringPopupToFront(this.keybindWindow);
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
        this.resolution = drawCtx.resolution();
        updateWrappedDescription((((f - 30.0f) - (this.showKeybindButton ? this.keybindButton.width() : 0.0f)) - 10.0f) - margin);
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        float fX = (x() + f) - 30.0f;
        float f3 = 18.0f / 2.0f;
        float fY = (y() + (height() / 2.0f)) - f3;
        float fY2 = y();
        float fValue = this.highlightAnimation.value();
        float titleRight = this.showKeybindButton
                ? (((fX - 7.0f) - this.keybindButton.width()) - 7.0f)
                : fX - 8.0f;
        MenuStyle.drawScrollingText(drawCtx, this.titleFont, this.name.effective(), nameTextSize,
                x(), fY2, Math.max(0.0f, titleRight - x()),
                this.clickBehavior.hoverAnimation().smoothAnimation(),
                colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()),
                        colorStack.computeColor(palette.text().tone(100).argb()), fValue));
        if (this.wrappedDescription != null) {
            drawCtx.text(this.descriptionFont, this.wrappedDescription, descriptionTextSize, x(), fY2 + this.titleFont.getHeight(14.0f) + descriptionGap, colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), fValue));
        }
        if (this.showKeybindButton) {
            float fX2 = (((x() + f) - 30.0f) - 7.0f) - this.keybindButton.width();
            float fHeight = (fY + f3) - (this.keybindButton.height() / 2.0f);
            this.keybindButton.setPosition(fX2, fHeight);
            this.keybindButton.setColor(palette.text().tone(100).argb());
            this.keybindButton.render(drawCtx);
            this.keybindButtonBounds.withPosition(fX2, fHeight).withSize(this.keybindButton.width(), this.keybindButton.height());
        } else {
            this.keybindButtonBounds.withSize(0.0f, 0.0f);
        }
        float progress = this.checkAnimator.smoothAnimation();
        float hover = this.clickBehavior.hoverAnimation().smoothAnimation();
        int track = colorStack.interpolate(colorStack.computeColor(0xFF787880, 0.32f + 0.06f * hover),
                colorStack.computeColor(palette.accent().argb()), progress);
        MenuStyle.drawSwitch(drawCtx, fX, fY, 30.0f, 18.0f, progress, track);
        this.clickBehavior.setDimensions(x(), y(), f, height());
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.keybindWindow.layout(layoutScaleContext);
        if (this.showKeybindButton) {
            this.keybindButton.layout(layoutScaleContext);
        }
        this.checkmarkIcon.setDimensions(nameTextSize, nameTextSize);
    }

    public float textHeight(String str, int i, int i2) {
        float height = this.titleFont.getHeight(i);
        if (this.description != null) {
            height = height + descriptionGap + this.descriptionFont.getHeightWithLineBreaks(str, i2);
        }
        return height;
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (this.showKeybindButton && this.keybindButton.handleInput(inputEventContext, z)) {
            return true;
        }
        return this.clickBehavior.handleInput(inputEventContext, z);
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.resolution != null) {
            updateWrappedDescription((((this.resolution.width() - 18.0f) - (this.showKeybindButton ? this.keybindButton.width() : 0.0f)) - 10.0f) - margin);
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += descriptionGap + this.descriptionHeight;
        }
        return Math.max(height, 18.0f);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        boolean zBooleanValue;
        if (this.toggledStateSupplier != null && (zBooleanValue = this.toggledStateSupplier.get().booleanValue()) != this.lastToggledState) {
            this.checkAnimator.state(zBooleanValue);
            this.lastToggledState = zBooleanValue;
        }
        if (this.showKeybindButton) {
            this.keybindButton.animation(weightedEngine);
        }
        this.keybindWindow.animation(weightedEngine);
        if (this.visibilitySupplier != null) {
            visible(this.visibilitySupplier.get().booleanValue());
        }
        this.highlightAnimation.animate(weightedEngine);
        this.checkAnimator.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public void handleClose() {
        this.keybindWindow.closeWindow();
        super.handleClose();
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        this.keybindWindow.collectBloomElements(renderCommandQueue);
        super.collectBloomElements(renderCommandQueue);
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

    public void invert() {
        if (this.changeAction != null) {
            this.changeAction.run();
        }
    }

    public Translation name() {
        return this.name;
    }

    public CheckboxSettingElement toggledSupplier(Supplier<Boolean> supplier) {
        this.toggledStateSupplier = supplier;
        return this;
    }

    public CheckboxSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }

    public CheckboxSettingElement changeRunnable(Runnable runnable) {
        this.changeAction = runnable;
        return this;
    }
}
