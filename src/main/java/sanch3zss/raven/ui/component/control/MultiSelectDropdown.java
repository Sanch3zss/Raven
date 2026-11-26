package sanch3zss.raven.ui.component.control;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.joml.Quaternionf;

import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.*;
import sanch3zss.raven.feature.setting.MultiSelectOption;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.AbstractWidget;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.util.math.interpolation.Easings;

public class MultiSelectDropdown<T> extends AbstractWidget {
    public boolean initialized;
    public final float dropdownWidth;
    public final MsdfFont font;
    public final GlTexture icon;
    public final float cornerRadius;
    public final float paddingX;
    public final float paddingY;
    public final int iconSize;
    public Consumer<Set<T>> selectCallback;
    public boolean open;
    public static final float optionHeight = MenuStyle.DROPDOWN_OPTION_HEIGHT;
    public static final float optionPadding = MenuStyle.DROPDOWN_PADDING;
    public static final float rowSpacing = 2.0f;
    public final AnimatedFloat widthAnimation = new AnimatedFloat(220, Easings.LINEAR);
    public final AnimatedFloat heightAnimation = new AnimatedFloat(220, Easings.LINEAR);
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final List<MultiSelectOptionItem<T>> optionItems = new ArrayList();
    public final MsdfFont optionFont = Fonts.INTER_BOLD.get();

    public final WidgetBounds headerBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);

    public final WidgetBounds dropdownBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public String text = "";
    public int textSize = 12;

    public final Set<T> selectedValues = new HashSet();
    public final ToggleAnimator openAnimation = new ToggleAnimator(sanch3zss.raven.ui.menu.MenuStyle.MOTION_NORMAL_MS, Easings.EASE_IN_OUT_CUBIC);

    public MultiSelectDropdown(MsdfFont msdfFont, GlTexture texture, float f, float f2, float f3, int i, float f4) {
        this.font = msdfFont;
        this.icon = texture;
        this.cornerRadius = f;
        this.paddingX = f2;
        this.paddingY = f3;
        this.iconSize = i;
        this.dropdownWidth = f4;
        this.clickBehavior.clickCallback(this::toggle);
    }

    public void onSelect(Consumer<Set<T>> consumer) {
        this.selectCallback = consumer;
    }

    public void setOptions(List<MultiSelectOption<T>> list, Set<T> set) {
        this.optionItems.clear();
        this.selectedValues.clear();
        for (MultiSelectOption<T> multiSelectOption : list) {
            MultiSelectOptionItem<T> multiSelectOptionItem = new MultiSelectOptionItem<>(multiSelectOption,
                    () -> this.selectedValues.contains(multiSelectOption.value()));
            multiSelectOptionItem.clickableBehavior().clickCallback(() -> toggleSelection(multiSelectOptionItem.option().value(), true));
            multiSelectOptionItem.toggleSwitch().runnable(() -> toggleSelection(multiSelectOptionItem.option().value(), true));
            this.optionItems.add(multiSelectOptionItem);
        }
        if (set == null || set.isEmpty()) {
            this.selectedValues.addAll(list.stream().limit(1).map(MultiSelectOption::value).toList());
        } else {
            this.selectedValues.addAll(set);
        }
        ensureSelection();
        updateText();
    }

    public void label(String str, int i) {
        this.text = str;
        this.textSize = i;
    }

    public void toggle() {
        this.open = !this.open;
        if (this.open) {
            for (MultiSelectOptionItem<T> option : this.optionItems) {
                option.currentOptionAnimation.force(this.selectedValues.contains(option.option().value()));
            }
        }
    }

    public void toggleSelection(T t, boolean z) {
        if (this.optionItems.isEmpty()) {
            return;
        }
        if (!this.selectedValues.contains(t)) {
            this.selectedValues.add(t);
        } else if (this.selectedValues.size() > 1) {
            this.selectedValues.remove(t);
        }
        ensureSelection();
        updateText();
        if (!z || this.selectCallback == null) {
            return;
        }
        this.selectCallback.accept(Set.copyOf(this.selectedValues));
    }

    public void ensureSelection() {
        if (!this.selectedValues.isEmpty() || this.optionItems.isEmpty()) {
            return;
        }
        this.selectedValues.add(((MultiSelectOptionItem<T>) this.optionItems.getFirst()).option().value());
    }

    public void updateText() {
        this.text = (String) this.optionItems.stream().filter(multiSelectOptionItem -> {
            return this.selectedValues.contains(multiSelectOptionItem.option().value());
        }).map(value -> {
            return value.option().label();
        }).collect(Collectors.joining(", "));
        if (!this.text.isBlank() || this.optionItems.isEmpty()) {
            return;
        }
        this.text = ((MultiSelectOptionItem) this.optionItems.getFirst()).option().label();
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        float fWidth = width();
        float fHeight = height();
        float fSmoothAnimation = this.openAnimation.smoothAnimation();
        float size = MenuStyle.dropdownHeight(this.optionItems.size());
        ToggleAnimator hoverAnimation = this.clickBehavior.hoverAnimation();
        ToggleAnimator animator = this.openAnimation;
        int iInterpolate2 = colorStack.computeColor(palette.text().tone(100).argb());
        sanch3zss.raven.ui.menu.MenuStyle.drawDropdownField(drawCtx, x(), y(), fWidth, fHeight,
                this.cornerRadius, hoverAnimation.smoothAnimation());
        sanch3zss.raven.ui.menu.MenuStyle.drawDropdownArrows(drawCtx, x(), y(), fWidth, fHeight);
        float fX = x() + 7.0f;
        float fY = y() + (fHeight / rowSpacing);
        float fX2 = fX;
        float fY2 = (y() + (fHeight / rowSpacing)) - (this.font.getHeight(this.textSize) / rowSpacing);
        float iconSpace = this.icon == null ? 0.0f : this.iconSize + 10.0f;
        drawCtx.text(this.font, truncateText(drawCtx, this.font, this.text, this.textSize,
                Math.max(0.0f, fWidth - 14.0f - iconSpace)), this.textSize, fX2, fY2, iInterpolate2);
        if (this.icon != null) {
            drawCtx.textureVerticalC(this.icon, x() + fWidth - this.paddingX - this.iconSize,
                    y() + fHeight / rowSpacing, this.iconSize, this.iconSize, iInterpolate2);
        }
        if (this.optionItems.isEmpty() || (!this.open && this.openAnimation.isZero())) {
            this.dropdownBounds.withSize(0.0f, 0.0f);
            return;
        }
        float panelWidth = width();
        float fY3 = y() + fHeight + MenuStyle.DROPDOWN_GAP;
        float panelX = Math.max(5.0f,
                Math.min(x(), drawCtx.layoutContext().logicalWidth() - panelWidth - 5.0f));
        this.dropdownBounds.withPosition(panelX, fY3).withSize(panelWidth, size);
        colorStack.push();
        colorStack.alpha(fSmoothAnimation);
        MenuStyle.drawDropdownPanel(drawCtx, panelX, fY3, panelWidth, size,
                MenuStyle.DROPDOWN_RADIUS);
        for (int i = 0; i < this.optionItems.size(); i++) {
            MultiSelectOptionItem<T> multiSelectOptionItem = this.optionItems.get(i);
            float f4 = fY3 + optionPadding + (i * optionHeight);
            int iInterpolate3 = colorStack.computeColor(palette.text().tone(100).argb());
            float fX4 = panelX + MenuStyle.DROPDOWN_PADDING;
            float selected = multiSelectOptionItem.currentOptionAnimation().smoothAnimation();
            if (selected > 0.01f) {
                MenuStyle.drawCheckmark(drawCtx, fX4 + 12.0f, f4 + optionHeight / 2.0f, 10.0f,
                        colorStack.computeColor(palette.accent().argb(), selected));
            }
            drawCtx.text(this.optionFont, multiSelectOptionItem.option().label(), this.textSize, fX4 + 6.0f + 18.0f * selected, f4 + (optionHeight - this.optionFont.getHeight(this.textSize)) / rowSpacing, iInterpolate3);
            multiSelectOptionItem.clickableBehavior().setDimensions(panelX, f4,
                    panelWidth, optionHeight);
        }
        colorStack.pop();
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = false;
        if (this.open && !z && (inputEventContext.inputEvent() instanceof MouseButtonInput)) {
            boolean zInArea = inputEventContext.inArea(this.headerBounds.x(), this.headerBounds.y(), this.headerBounds.width(), this.headerBounds.height());
            boolean zInArea2 = inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height());
            if (!zInArea && !zInArea2) {
                close();
                return true;
            }
        }
        if (this.open) {
            for (int size = this.optionItems.size() - 1; size >= 0; size--) {
                MultiSelectOptionItem<T> multiSelectOptionItem = this.optionItems.get(size);
                if (multiSelectOptionItem.clickableBehavior().handleInput(inputEventContext, z || z2)) {
                    z2 = true;
                }
            }
        }
        if (this.clickBehavior.handleInput(inputEventContext, z || z2)) {
            z2 = true;
        }
        return z2;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(adaptiveWidth(layoutScaleContext), MenuStyle.DROPDOWN_FIELD_HEIGHT);
        this.initialized = true;
        this.headerBounds.withPosition(x(), y()).withSize(width(), height());
        this.clickBehavior.setDimensions(x(), y(), width(), height());
        if (this.icon != null) {
            this.icon.setDimensions(this.iconSize, this.iconSize);
        }
        if (this.optionItems.isEmpty()) {
            return;
        }
        float fY = y() + height() + MenuStyle.DROPDOWN_GAP;
        for (int i = 0; i < this.optionItems.size(); i++) {
            MultiSelectOptionItem<T> multiSelectOptionItem = this.optionItems.get(i);
            float f2 = fY + optionPadding + (i * optionHeight);
            multiSelectOptionItem.clickableBehavior().setDimensions(x(), f2, width(), optionHeight);
        }
    }

    public String truncateText(DrawCtx drawCtx, MsdfFont msdfFont, String str, int i, float f) {
        if (str == null || str.isEmpty()) {
            return "";
        }
        if (drawCtx.textWidthPhysical(msdfFont, str, i) <= f) {
            return str;
        }
        if (drawCtx.textWidthPhysical(msdfFont, ".", i) > f) {
            return "";
        }
        int i2 = 0;
        int length = str.length();
        while (i2 < length) {
            int i3 = ((i2 + length) + 1) >>> 1;
            if (drawCtx.textWidthPhysical(msdfFont, str.substring(0, i3) + ".", i) <= f) {
                i2 = i3;
            } else {
                length = i3 - 1;
            }
        }
        return str.substring(0, i2) + ".";
    }

    @Override
    public void handleClose() {
        close();
        super.handleClose();
    }

    public void close() {
        if (this.open || !this.openAnimation.isZero()) {
            this.open = false;
            this.openAnimation.state(false);
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickBehavior.animate(weightedEngine);
        this.openAnimation.state(this.open).animate(weightedEngine);
        this.widthAnimation.animate(weightedEngine);
        this.heightAnimation.animate(weightedEngine);
        for (MultiSelectOptionItem<T> multiSelectOptionItem : this.optionItems) {
            multiSelectOptionItem.clickableBehavior().animate(weightedEngine);
            multiSelectOptionItem.currentOptionAnimation.state(this.selectedValues.contains(multiSelectOptionItem.option().value()));
            multiSelectOptionItem.currentOptionAnimation.animate(weightedEngine);
        }
    }

    public List<MultiSelectOption<T>> options() {
        return this.optionItems.stream().map(MultiSelectOptionItem::option).toList();
    }

    private float adaptiveWidth(LayoutScaleContext layoutScaleContext) {
        float result = Math.max(MenuStyle.DROPDOWN_MIN_WIDTH, this.dropdownWidth);
        for (MultiSelectOptionItem<T> option : this.optionItems) {
            result = Math.max(result, layoutScaleContext.textWidthPhysical(this.optionFont,
                    option.option().label(), this.textSize) + 32.0f);
        }
        return Math.min(MenuStyle.DROPDOWN_MAX_WIDTH, result);
    }

    public boolean isOpen() {
        return this.open;
    }
}
