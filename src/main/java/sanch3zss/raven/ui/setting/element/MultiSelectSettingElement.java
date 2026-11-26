package sanch3zss.raven.ui.setting.element;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.*;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.layout.*;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class MultiSelectSettingElement<T extends Enum<T>> extends ModuleFrame {
    public static final float MAX_DROPDOWN_HEIGHT = 250.0f;
    public static final int titleTextSize = 14;
    public static final int descTextSize = 13;
    public static final float DESC_GAP = 3.0f;
    public static final float BLOCK_SPACING = 12.0f;
    public static final float FIELD_HEIGHT = MenuStyle.DROPDOWN_FIELD_HEIGHT;
    public static final float OPTION_HEIGHT = MenuStyle.DROPDOWN_OPTION_HEIGHT;

    public final Translation name;
    public final Translation description;
    public final Supplier<Set<T>> selectedSupplier;
    public Supplier<Boolean> visibleCondition;
    public boolean expanded;
    public final ScrollbarWidget scrollbar;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    public final MsdfFont valueFont = Fonts.INTER_BOLD.get();
    public final MsdfFont iconFont = Fonts.CLICK_GUI.get();
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator expandAnimation = new ToggleAnimator(MenuStyle.MOTION_NORMAL_MS, Easings.EASE_IN_OUT_CUBIC);
    public final GlTexture enumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final GlTexture multiEnumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/multienum.png"));
    public final GlTexture checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final List<MultiSelectOption2<T>> options = new ArrayList<>();
    public final WidgetBounds boxBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public String wrappedDescription = null;
    public boolean descriptionDirty = true;
    public float cachedWidth = -1.0f;
    public String cachedDescription = null;
    public float descriptionHeight = 0.0f;
    public final ScrollArea scrollArea = new ScrollArea();
    public final WidgetBounds dropdownBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);

    public MultiSelectSettingElement(Translation translation2, Translation translation, T[] tArr, Supplier<Set<T>> supplier, Consumer<T> consumer) {
        this.scrollbar = new ScrollbarWidget(
            this.scrollArea::scrollY,
            this::computeContentHeight,
            this.scrollArea::height,
            this.scrollArea::scrollTo,
            4.0f, 4.0f, 30.0f
        );
        this.name = translation2;
        this.description = translation;
        this.selectedSupplier = supplier;
        for (T t : tArr) {
            this.options.add(new MultiSelectOption2<>(t));
        }
        Set<T> set = supplier.get();
        for (MultiSelectOption2<T> option : this.options) {
            option.toggleAnimation().force(set.contains(option.option()));
            option.clickableBehavior().clickCallback(() -> {
                boolean nextState = !supplier.get().contains(option.option());
                consumer.accept(option.option());
                option.toggleAnimation().state(nextState);
            });
        }
        this.clickBehavior.clickCallback(this::invert);    }

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
        updateDescription(Math.max(0.0f, f - boxWidth - 10.0f));
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        float fY = y();
        float fValue = this.highlightAnimation.value();
        MenuStyle.drawFadingText(drawCtx, this.titleFont, this.name.effective(), titleTextSize,
                x(), fY, Math.max(0.0f, boxX - x() - 8.0f),
                colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()),
                        colorStack.computeColor(palette.text().tone(100).argb()), fValue));
        if (this.wrappedDescription != null) {
            fY += this.titleFont.getHeight(14.0f) + DESC_GAP;
            drawCtx.text(this.descriptionFont, this.wrappedDescription, descTextSize, x(), fY, colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), fValue));
        }
        float height = y();
        float contentHeight = computeContentHeight();
        this.boxBounds.withPosition(boxX, height).withSize(boxWidth, FIELD_HEIGHT);
        MenuStyle.drawDropdownField(drawCtx, boxX, height, boxWidth,
                FIELD_HEIGHT, 4.0f, this.clickBehavior.hoverAnimation().smoothAnimation());
        MenuStyle.drawDropdownArrows(drawCtx, boxX, height, boxWidth, FIELD_HEIGHT);
        float fX = boxX + 7.0f;
        float fX2 = boxX + boxWidth - 12.0f;
        if (this.selectedSupplier == null) {
            return;
        }
        Set<T> set = this.selectedSupplier.get();
        String strEffective = set.isEmpty() ? Lang.NOTHING_SELECTED.effective() : this.options.stream()
                .map(MultiSelectOption2::option).filter(set::contains).map(r2 -> {
            return r2 instanceof DisplayNamed named ? named.getDisplayName().effective() : r2.name();
        }).collect(Collectors.joining(", "));
        float fWidth2 = fX;
        int color = colorStack.computeColor(palette.text().tone(100).argb());
        float f4 = fX2 - fWidth2;
        drawCtx.drawEngine().beginStencil();
        drawCtx.fillRoundedRect(fX, height, Math.max(0.0f, f4), FIELD_HEIGHT,
                2.0f, colorStack.white());
        drawCtx.drawEngine().prepareStencil(1);
        if (drawCtx.textWidthPhysical(this.valueFont, strEffective, 12) > f4) {
            int i = color & 16777215;
            String str = "";
            for (int length = strEffective.length(); length > 0; length--) {
                String strSubstring = strEffective.substring(0, length);
                if (drawCtx.textWidthPhysical(this.valueFont, strSubstring, 12) <= f4) {
                    str = strSubstring;
                    break;
                }
            }
            drawCtx.textWithHorizontalGradient(this.valueFont, str, 12, fWidth2,
                    height + (FIELD_HEIGHT - this.valueFont.getHeight(BLOCK_SPACING)) / 2.0f, color, i);
        } else {
            drawCtx.text(this.valueFont, strEffective, 12, fWidth2,
                    height + (FIELD_HEIGHT - this.valueFont.getHeight(BLOCK_SPACING)) / 2.0f, color);
        }
        drawCtx.drawEngine().endStencil();
        float f5 = height + FIELD_HEIGHT + DESC_GAP;
        if (this.options.isEmpty() || (!this.expanded && this.expandAnimation.isZero())) {
            this.dropdownBounds.withSize(0.0f, 0.0f);
        } else {
            float dropdownHeight = Math.min(contentHeight, MAX_DROPDOWN_HEIGHT);
            float screenHeight = sanch3zss.raven.render.engine.ScreenResolution.resolution().screenHeight()
                    / Math.max(Raven.INSTANCE.windowController().dpiScaleFactor(), 0.01f);
            float dropdownY = f5 + dropdownHeight > screenHeight - 5.0f
                    ? height - DESC_GAP - dropdownHeight : f5;
            this.dropdownBounds.withPosition(boxX, Math.max(5.0f, dropdownY)).withSize(boxWidth, dropdownHeight);
        }
        this.clickBehavior.setDimensions(boxX, height, boxWidth, FIELD_HEIGHT);
    }

    @Override
    public void drawOverlay(DrawCtx drawCtx) {
        if (this.options.isEmpty()) {
            return;
        }
        if (!this.expandAnimation.isZero() || this.expanded) {
            ThemePalette palette = drawCtx.theme().palette();
            ColorStack colorStack = drawCtx.drawEngine().colorStack();
            colorStack.push();
            colorStack.alpha(this.expandAnimation.smoothAnimation());
            float fY = this.dropdownBounds.y();
            float fHeight = this.dropdownBounds.height();
            float fWidth = this.dropdownBounds.width();
            MenuStyle.drawDropdownPanel(drawCtx, this.dropdownBounds.x(), fY,
                    fWidth, fHeight, MenuStyle.DROPDOWN_RADIUS);
            drawCtx.drawEngine().beginStencil();
            drawCtx.fillRoundedRect(this.dropdownBounds.x(), fY, fWidth, fHeight,
                    MenuStyle.DROPDOWN_RADIUS, colorStack.white());
            drawCtx.drawEngine().prepareStencil(1);
            this.scrollArea.beginArea(drawCtx, this.dropdownBounds.x(),
                    this.dropdownBounds.y() + MenuStyle.DROPDOWN_PADDING,
                    this.dropdownBounds.width(),
                    this.dropdownBounds.height() - MenuStyle.DROPDOWN_PADDING * 2.0f);
            float f = fY + MenuStyle.DROPDOWN_PADDING;
            float fVisibleTop = f + this.scrollArea.visibleTop();
            float fVisibleBottom = f + this.scrollArea.visibleBottom();
            Set<T> selectedValues = this.selectedSupplier != null
                    ? this.selectedSupplier.get() : Set.of();
            for (int i = 0; i < this.options.size(); i++) {
                MultiSelectOption2<T> option = this.options.get(i);
                ClickableBehavior clickableBehavior = option.clickableBehavior();
                float f2 = fY + MenuStyle.DROPDOWN_PADDING + (i * OPTION_HEIGHT);
                if (f2 + OPTION_HEIGHT < fVisibleTop || f2 > fVisibleBottom) {
                    clickableBehavior.setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
                } else {
                    clickableBehavior.setDimensions(this.dropdownBounds.x(), f2, fWidth, OPTION_HEIGHT);
                    ToggleAnimator animator = option.toggleAnimation();
                    MenuStyle.drawDropdownOption(drawCtx, this.dropdownBounds.x(), f2, fWidth, OPTION_HEIGHT,
                            clickableBehavior.hoverAnimation().smoothAnimation());
                    float selected = animator.smoothAnimation();
                    if (selected > 0.01f) {
                        MenuStyle.drawCheckmark(drawCtx, this.dropdownBounds.x() + 14.0f, f2 + OPTION_HEIGHT / 2.0f,
                                this.checkmarkIcon.width() * 0.72f,
                                colorStack.computeColor(palette.accent().argb(), selected));
                    }
                    int iInterpolate = colorStack.computeColor(palette.text().tone(100).argb());
                    T t = option.option();
                    if (t instanceof DisplayNamed named) {
                        float shift = 18.0f * selected;
                        MenuStyle.drawFadingText(drawCtx, this.valueFont,
                                named.getDisplayName().effective(), 12,
                                this.dropdownBounds.x() + 8.0f + shift,
                                f2 + (OPTION_HEIGHT - this.valueFont.getHeight(12.0f)) / 2.0f,
                                Math.max(0.0f, fWidth - 16.0f - shift - (needsScrollbar() ? 8.0f : 0.0f)),
                                iInterpolate);
                    }
                }
            }
            this.scrollArea.endArea(drawCtx,
                    computeContentHeight() - MenuStyle.DROPDOWN_PADDING * 2.0f);
            drawCtx.drawEngine().endStencil();
            if (needsScrollbar()) {
                this.scrollbar.render(drawCtx);
            }
            colorStack.pop();
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.enumIcon.setDimensions(12, 12);
        this.multiEnumIcon.setDimensions(15, 15);
        this.checkmarkIcon.setDimensions(titleTextSize, titleTextSize);
        this.scrollArea.layout(layoutScaleContext);
        if (needsScrollbar()) {
            this.scrollbar.layout(layoutScaleContext);
            this.scrollbar.setSize(4.0f, Math.min(computeContentHeight(), MAX_DROPDOWN_HEIGHT));
            this.scrollbar.setPosition(((this.dropdownBounds.x() + this.dropdownBounds.width())
                    - MenuStyle.DROPDOWN_PADDING * 2.0f) - this.scrollbar.width(),
                    this.dropdownBounds.y() + MenuStyle.DROPDOWN_PADDING);
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibleCondition != null) {
            visible(Boolean.TRUE.equals(this.visibleCondition.get()));
        }
        this.expandAnimation.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        if (needsScrollbar()) {
            this.scrollbar.animation(weightedEngine);
        }
        this.highlightAnimation.animate(weightedEngine);
        Set<T> setOf = this.selectedSupplier != null ? this.selectedSupplier.get() : Set.of();
        for (MultiSelectOption2<T> multiSelectOption2 : this.options) {
            multiSelectOption2.toggleAnimation().state(setOf.contains(multiSelectOption2.option()));
            multiSelectOption2.toggleAnimation().animate(weightedEngine);
            multiSelectOption2.clickableBehavior().animate(weightedEngine);
        }
        this.scrollArea.animation(weightedEngine);
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean handled = false;
        if (this.expanded || !this.expandAnimation.isZero()) {
            handled = this.scrollArea.handleInput(inputEventContext, z);
            if (needsScrollbar() && this.scrollbar.handleInput(inputEventContext, z)) {
                handled = true;
            }
            InputEventContext withMouseOffset = inputEventContext.withMouseOffset(0.0f, this.scrollArea.scrollY());
            for (int size = this.options.size() - 1; size >= 0; size--) {
                if (this.options.get(size).clickableBehavior().handleInput(withMouseOffset, z || handled)) {
                    handled = true;
                }
            }
            if (!z && !handled) {
                if ((inputEventContext.inputEvent() instanceof MouseButtonInput) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
                if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
            }
            if ((inputEventContext.inputEvent() instanceof ScrollInput) && !inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
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
        }
        if (this.clickBehavior.handleInput(inputEventContext, z || handled)) {
            handled = true;
        }
        return handled;
    }

    @Override
    public void handleClose() {
        this.scrollArea.scrollTo(0.0f);
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
            updateDescription(Math.max(0.0f, this.parent.width() - 110.0f));
        }
        float height = this.titleFont.getHeight(14.0f);
        if (this.description != null && this.wrappedDescription != null) {
            height += DESC_GAP + this.descriptionHeight;
        }
        return Math.max(height, FIELD_HEIGHT);
    }

    public float computeContentHeight() {
        if (this.expandAnimation.isZero() && !this.expanded) {
            return 0.0f;
        }
        return MenuStyle.dropdownContentHeight(this.options.size());
    }

    private float preferredDropdownWidth(DrawCtx drawCtx, float availableWidth) {
        float width = MenuStyle.DROPDOWN_MIN_WIDTH;
        for (MultiSelectOption2<T> option : this.options) {
            T value = option.option();
            String label = value instanceof DisplayNamed named
                    ? named.getDisplayName().effective() : value.name();
            width = Math.max(width, drawCtx.textWidthPhysical(this.valueFont, label, 12) + 32.0f);
        }
        return MenuStyle.settingControlWidth(width, availableWidth);
    }

    public void closeDropdown() {
        if (this.expanded || !this.expandAnimation.isZero()) {
            this.expanded = false;
            this.expandAnimation.state(false);
            this.scrollArea.scrollTo(0.0f);
        }
    }

    public boolean needsScrollbar() {
        return computeContentHeight() > MAX_DROPDOWN_HEIGHT;
    }

    public void invert() {
        this.expanded = !this.expanded;
        if (this.expanded && this.selectedSupplier != null) {
            Set<T> selected = this.selectedSupplier.get();
            for (MultiSelectOption2<T> option : this.options) {
                option.toggleAnimation().force(selected.contains(option.option()));
            }
        }
        this.expandAnimation.state(this.expanded);
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.cachedWidth = f;
            this.cachedDescription = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.cachedWidth || strEffective == null || !strEffective.equals(this.cachedDescription)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descriptionFont, descTextSize);
            this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descTextSize);
            this.cachedWidth = f;
            this.cachedDescription = strEffective;
            this.descriptionDirty = false;
        }
    }

    public Translation name() {
        return this.name;
    }

    public MultiSelectSettingElement<T> visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }
}
