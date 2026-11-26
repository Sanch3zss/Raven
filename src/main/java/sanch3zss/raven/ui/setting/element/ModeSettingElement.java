package sanch3zss.raven.ui.setting.element;

import java.util.*;
import java.util.function.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.feature.setting.ModeOption;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.layout.ScrollArea;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ModeSettingElement<T extends Enum<T>> extends ModuleFrame {
    static final int titleTextSize = 14;
    static final int descTextSize = 13;
    static final float descGap = 3.0f;
    static final float blockSpacing = 12.0f;
    static final float rowHeight = MenuStyle.DROPDOWN_FIELD_HEIGHT;
    static final float OPTION_HEIGHT = MenuStyle.DROPDOWN_OPTION_HEIGHT;
    static final float FIELD_RADIUS = 4.0f;
    static final float HALF = 2.0f;

    public final Translation name;
    public final Translation description;
    public boolean expanded;
    public String wrappedDescription;
    public String cachedDescription;
    public float descriptionHeight;
    public Supplier<T> valueSupplier;
    public Consumer<T> changeCallback;
    public Supplier<Boolean> visibleCondition;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont valueFont = Fonts.INTER_BOLD.get();
    public final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    public final MsdfFont iconFont = Fonts.CLICK_GUI.get();
    public final GlTexture enumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final GlTexture checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
    public final ToggleAnimator expandAnimation = new ToggleAnimator(MenuStyle.MOTION_NORMAL_MS, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator textHoverAnimation = new ToggleAnimator(1200, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator valueHoverAnimation = new ToggleAnimator(1200, Easings.EASE_IN_OUT_CUBIC);
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final List<ModeOption<T>> options = new ArrayList<>();
    public final WidgetBounds dropdownBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final ScrollArea dropdownScroll = new ScrollArea();
    public final WidgetBounds boxBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds textBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public float cachedWidth = -1.0f;
    public boolean descriptionDirty = true;

    public ModeSettingElement(Translation translation2, Translation translation, T[] tArr) {
        this.name = translation2;
        this.description = translation;
        for (T t : tArr) {
            this.options.add(new ModeOption<>(t));
        }
        setupOptionCallbacks();
        this.clickBehavior.clickCallback(this::toggleExpanded);    }

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
        float boxWidth = preferredDropdownWidth(drawCtx, f);
        float boxX = x() + f - boxWidth;
        float textWidth = Math.max(0.0f, boxX - x() - 8.0f);
        updateDescription(textWidth);
        float textBottom = drawDescription(drawCtx, drawTitle(drawCtx, y(), textWidth), textWidth);
        this.textBounds.withPosition(x(), y()).withSize(textWidth, Math.max(0.0f, textBottom - y()));
        float boxY = y();
        this.boxBounds.withPosition(boxX, boxY).withSize(boxWidth, rowHeight);
        drawBox(drawCtx, boxY, boxX, boxWidth);
        drawBoxContent(drawCtx, boxY, boxX, boxWidth);
        updateBounds(boxY, boxX, boxWidth);
    }

    @Override
    public void drawOverlay(DrawCtx drawCtx) {
        if (this.options.isEmpty()) {
            return;
        }
        if (!this.expandAnimation.isZero() || this.expanded) {
            ColorStack colorStack = drawCtx.drawEngine().colorStack();
            colorStack.push();
            colorStack.alpha(this.expandAnimation.smoothAnimation());
            drawDropdownBackground(drawCtx);
            this.dropdownScroll.beginArea(drawCtx, this.dropdownBounds.x(), this.dropdownBounds.y(),
                    this.dropdownBounds.width(), this.dropdownBounds.height());
            drawDropdownOptions(drawCtx);
            this.dropdownScroll.endArea(drawCtx, rawDropdownHeight());
            colorStack.pop();
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.enumIcon.setDimensions(12, 12);
        this.checkmarkIcon.setDimensions(titleTextSize, titleTextSize);
        this.dropdownScroll.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibleCondition != null) {
            visible(Boolean.TRUE.equals(this.visibleCondition.get()));
        }
        this.expandAnimation.animate(weightedEngine);
        this.textHoverAnimation.animate(weightedEngine);
        this.valueHoverAnimation.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        this.dropdownScroll.animation(weightedEngine);
        for (ModeOption<T> modeOption : this.options) {
            modeOption.clickableBehavior().animate(weightedEngine);
            if (this.valueSupplier != null) {
                modeOption.currentOptionAnimation.state(modeOption.option == this.valueSupplier.get());
                modeOption.currentOptionAnimation.animate(weightedEngine);
            }
        }
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        this.textHoverAnimation.state(inputEventContext.inArea(this.textBounds.x(), this.textBounds.y(),
                this.textBounds.width(), this.textBounds.height()) && !z);
        this.valueHoverAnimation.state(inputEventContext.inArea(this.boxBounds.x(), this.boxBounds.y(),
                this.boxBounds.width(), this.boxBounds.height()) && !z);
        boolean handled = false;
        if (this.expanded || !this.expandAnimation.isZero()) {
            handled = handleDropdownInput(inputEventContext, z);
        }
        if (this.clickBehavior.handleInput(inputEventContext, z || handled)) {
            handled = true;
        }
        return handled;
    }

    @Override
    public void handleClose() {
        closeDropdown();        super.handleClose();
    }

    @Override
    public void handleViewportVisibility(float f, float f2, float f3) {
        if (this.expanded || !this.expandAnimation.isZero()) {
            float fY = y() - f3;
            if (fY + height() < f || fY > f2) {
                closeDropdown();
            }
        }
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.parent != null) {
            updateDescription(Math.max(0.0f, this.parent.width() - MenuStyle.DROPDOWN_MIN_WIDTH - 10.0f));
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += descGap + this.descriptionHeight;
        }
        return Math.max(height, rowHeight);
    }

    public void setupOptionCallbacks() {
        for (ModeOption<T> modeOption : this.options) {
            modeOption.clickableBehavior().clickCallback(() -> {
                if (this.expanded) {
                    toggleExpanded();
                }
                if (this.changeCallback != null) {
                    this.changeCallback.accept(modeOption.option);
                }
            });
        }
    }

    public float drawTitle(DrawCtx drawCtx, float f, float maxWidth) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        MenuStyle.drawScrollingText(drawCtx, this.titleFont, this.name.effective(), titleTextSize,
                x(), f, maxWidth, this.textHoverAnimation.smoothAnimation(),
                colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()),
                        colorStack.computeColor(palette.text().tone(100).argb()),
                        this.highlightAnimation.value()));
        return f + this.titleFont.getHeight(14.0f);
    }

    public float drawDescription(DrawCtx drawCtx, float f, float maxWidth) {
        if (this.wrappedDescription == null) {
            return f;
        }
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        int iInterpolate = colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.highlightAnimation.value());
        float f2 = f + descGap;
        MenuStyle.drawScrollingText(drawCtx, this.descriptionFont, this.wrappedDescription,
                descTextSize, x(), f2, maxWidth, this.textHoverAnimation.smoothAnimation(), iInterpolate);
        return f2 + this.descriptionHeight;
    }

    public void drawBox(DrawCtx drawCtx, float f, float boxX, float boxWidth) {
        MenuStyle.drawDropdownField(drawCtx, boxX, f, boxWidth, rowHeight, FIELD_RADIUS,
                this.clickBehavior.hoverAnimation().smoothAnimation());
        MenuStyle.drawDropdownArrows(drawCtx, boxX, f, boxWidth, rowHeight);
    }

    public void drawBoxContent(DrawCtx drawCtx, float f, float boxX, float boxWidth) {
        drawValueText(drawCtx, f, boxX, boxWidth, boxX + 7.0f);
    }

    public void drawValueText(DrawCtx drawCtx, float f, float boxX, float boxWidth, float f3) {
        if (this.valueSupplier == null) {
            return;
        }
        T t = this.valueSupplier.get();
        if (t instanceof DisplayNamed displayNamed) {
            int color = drawCtx.drawEngine().colorStack().computeColor(drawCtx.theme().palette().text().tone(100).argb());
            String strEffective = displayNamed.getDisplayName().effective();
            float fX = boxX + boxWidth - 12.0f - f3;
            float fRound = f + (rowHeight - this.valueFont.getHeight(blockSpacing)) / HALF;
            MenuStyle.drawScrollingText(drawCtx, this.valueFont, strEffective, 12,
                    f3, fRound, fX, this.valueHoverAnimation.smoothAnimation(), color);
        }
    }

    public void drawDropdownBackground(DrawCtx drawCtx) {
        MenuStyle.drawDropdownPanel(drawCtx, this.dropdownBounds.x(), this.dropdownBounds.y(),
                this.dropdownBounds.width(), this.dropdownBounds.height(), MenuStyle.DROPDOWN_RADIUS);
    }

    public void drawDropdownOptions(DrawCtx drawCtx) {
        for (int i = 0; i < this.options.size(); i++) {
            ModeOption<T> modeOption = this.options.get(i);
            float fY = this.dropdownBounds.y() + MenuStyle.DROPDOWN_PADDING + (i * OPTION_HEIGHT);
            modeOption.clickableBehavior.setDimensions(this.dropdownBounds.x(), fY, this.dropdownBounds.width(), OPTION_HEIGHT);
            drawOption(drawCtx, modeOption, fY);
        }
    }

    public void drawOption(DrawCtx drawCtx, ModeOption<T> modeOption, float f) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        float fX = this.dropdownBounds.x() + MenuStyle.DROPDOWN_PADDING;
        float fWidth = this.dropdownBounds.width() - MenuStyle.DROPDOWN_PADDING * 2.0f;
        MenuStyle.drawDropdownOption(drawCtx, fX, f, fWidth, OPTION_HEIGHT,
                modeOption.clickableBehavior().hoverAnimation().smoothAnimation());
        float selected = modeOption.currentOptionAnimation().smoothAnimation();
        if (selected > 0.01f) {
            MenuStyle.drawCheckmark(drawCtx, fX + 12.0f, f + OPTION_HEIGHT / 2.0f, this.checkmarkIcon.width() * 0.72f,
                    colorStack.computeColor(palette.accent().argb(), selected));
        }
        T t = modeOption.option;
        if (t instanceof DisplayNamed displayNamed) {
            float shift = 18.0f * selected;
            float textX = fX + 6.0f + shift;
            float textWidth = fWidth - 12.0f - shift;
            MenuStyle.drawScrollingText(drawCtx, this.valueFont,
                    displayNamed.getDisplayName().effective(), 12, textX,
                    f + (OPTION_HEIGHT - this.valueFont.getHeight(blockSpacing)) / HALF, textWidth,
                    modeOption.clickableBehavior().hoverAnimation().smoothAnimation(),
                    colorStack.computeColor(palette.text().tone(100).argb()));
        }
    }

    public void updateBounds(float f, float boxX, float boxWidth) {
        float f3 = f + rowHeight + descGap;
        float dropdownHeight = computeDropdownHeight();
        if (this.options.isEmpty() || (!this.expanded && this.expandAnimation.isZero())) {
            this.dropdownBounds.withSize(0.0f, 0.0f);
        } else {
            float screenHeight = ScreenResolution.resolution().screenHeight()
                    / Math.max(Raven.INSTANCE.windowController().dpiScaleFactor(), 0.01f);
            float dropdownY = f3 + dropdownHeight > screenHeight - 5.0f ? f - descGap - dropdownHeight : f3;
            this.dropdownBounds.withPosition(boxX, Math.max(5.0f, dropdownY)).withSize(boxWidth, dropdownHeight);
        }
        this.clickBehavior.setDimensions(boxX, f, boxWidth, rowHeight);
    }

    public float computeDropdownHeight() {
        if (this.expandAnimation.isZero() && !this.expanded) {
            return 0.0f;
        }
        return MenuStyle.dropdownHeight(this.options.size());
    }

    public float rawDropdownHeight() {
        return MenuStyle.dropdownContentHeight(this.options.size());
    }

    private float preferredDropdownWidth(DrawCtx drawCtx, float availableWidth) {
        float width = MenuStyle.DROPDOWN_MIN_WIDTH;
        for (ModeOption<T> option : this.options) {
            String label = option.option instanceof DisplayNamed named
                    ? named.getDisplayName().effective() : option.option.name();
            width = Math.max(width, drawCtx.textWidthPhysical(this.valueFont, label, 12) + 32.0f);
        }
        return MenuStyle.settingControlWidth(width, availableWidth);
    }

    public boolean handleDropdownInput(InputEventContext inputEventContext, boolean z) {
        boolean handled = false;
        InputEventContext scrolledContext = inputEventContext.withMouseOffset(0.0f, this.dropdownScroll.scrollY());
        for (int size = this.options.size() - 1; size >= 0; size--) {
            if (this.options.get(size).clickableBehavior().handleInput(scrolledContext, z || handled)) {
                handled = true;
            }
        }
        if (!z && !handled && (((inputEventContext.inputEvent() instanceof MouseButtonInput) || (inputEventContext.inputEvent() instanceof CursorMoveInput)) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height()))) {
            return true;
        }
        if (inputEventContext.inputEvent() instanceof ScrollInput) {
            if (this.dropdownScroll.handleInput(inputEventContext, z || handled)) {
                return true;
            }
            closeDropdown();
            return true;
        }
        if (!z && (inputEventContext.inputEvent() instanceof MouseButtonInput)) {
            boolean zInArea = inputEventContext.inArea(this.clickBehavior.ownerX(), this.clickBehavior.ownerY(), this.clickBehavior.ownerW(), this.clickBehavior.ownerH());
            boolean zInArea2 = inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height());
            if (!zInArea && !zInArea2) {
                closeDropdown();
                return true;
            }
        }
        return handled;
    }

    public void toggleExpanded() {
        this.expanded = !this.expanded;
        if (this.expanded && this.valueSupplier != null) {
            T current = this.valueSupplier.get();
            for (ModeOption<T> option : this.options) {
                option.currentOptionAnimation().force(option.option == current);
            }
        }
        this.expandAnimation.state(this.expanded);
    }

    public void closeDropdown() {
        if (this.expanded || !this.expandAnimation.isZero()) {
            this.expanded = false;
            this.expandAnimation.state(false);
            this.dropdownScroll.scrollTo(0.0f);
        }
    }

    public void invalidateLayout() {
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescription = null;
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            clearDescription(f);
            return;
        }
        String strEffective = this.description.effective();
        if (isDescriptionCached(f, strEffective)) {
            return;
        }
        wrapDescription(f, strEffective == null ? "" : strEffective);
    }

    public void clearDescription(float f) {
        this.wrappedDescription = null;
        this.descriptionHeight = 0.0f;
        this.descriptionDirty = false;
        this.cachedWidth = f;
        this.cachedDescription = null;
    }

    public boolean isDescriptionCached(float f, String str) {
        return !this.descriptionDirty && f == this.cachedWidth && str != null && str.equals(this.cachedDescription);
    }

    public void wrapDescription(float f, String str) {
        this.wrappedDescription = str;
        this.descriptionHeight = str.isEmpty() ? 0.0f : this.descriptionFont.getHeight(descTextSize);
        this.cachedWidth = f;
        this.cachedDescription = str;
        this.descriptionDirty = false;
    }

    public Translation name() {
        return this.name;
    }

    public ModeSettingElement<T> currentValueSupplier(Supplier<T> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public ModeSettingElement<T> onChangeCallback(Consumer<T> consumer) {
        this.changeCallback = consumer;
        return this;
    }

    public ModeSettingElement<T> visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }
}
