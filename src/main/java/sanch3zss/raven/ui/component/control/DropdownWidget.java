package sanch3zss.raven.ui.component.control;

import java.util.*;
import java.util.function.Consumer;

import org.joml.Quaternionf;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.*;
import sanch3zss.raven.feature.setting.DropdownOption;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.AbstractWidget;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.util.math.interpolation.Easings;

public class DropdownWidget<T> extends AbstractWidget {
    public boolean initialized;
    public final MsdfFont labelFont;
    public final GlTexture iconTexture;
    public final float cornerRadius;
    public final float padding;
    public final float verticalPadding;
    public final int iconSize;
    public float panelWidth;
    public Consumer<T> onSelectCallback;
    public T selectedValue;
    public boolean open;
    public static final float OPTION_HEIGHT = MenuStyle.DROPDOWN_OPTION_HEIGHT;
    public static final float OPTION_PADDING = MenuStyle.DROPDOWN_PADDING;
    public static final float TWO = 2.0f;
    public final AnimatedFloat widthAnimation = new AnimatedFloat(220, Easings.LINEAR);
    public final AnimatedFloat heightAnimation = new AnimatedFloat(220, Easings.LINEAR);
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final List<DropdownOptionItem<T>> optionItems = new ArrayList();
    public final MsdfFont optionFont = Fonts.INTER_BOLD.get();
    public final WidgetBounds headerBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds panelBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public Translation currentLabel = Translation.clearText("");
    public int labelSize = 12;
    public final ToggleAnimator openAnimation = new ToggleAnimator(sanch3zss.raven.ui.menu.MenuStyle.MOTION_NORMAL_MS, Easings.EASE_IN_OUT_CUBIC);

    public DropdownWidget(MsdfFont msdfFont, GlTexture texture, float f, float f2, float f3, int i) {
        this.labelFont = msdfFont;
        this.iconTexture = texture;
        this.cornerRadius = f;
        this.padding = f2;
        this.verticalPadding = f3;
        this.iconSize = i;
        this.clickBehavior.clickCallback(this::toggle);
    }

    public void onSelect(Consumer<T> consumer) {
        this.onSelectCallback = consumer;
    }

    public void setOptions(List<DropdownOption<T>> list, T t) {
        this.optionItems.clear();
        for (DropdownOption<T> dropdownOption : list) {
            DropdownOptionItem<T> dropdownOptionItem = new DropdownOptionItem<>(dropdownOption);
            dropdownOptionItem.clickableBehavior().clickCallback(() -> {
                applySelection(dropdownOption.value(), true);
            });
            this.optionItems.add(dropdownOptionItem);
        }
        applySelection(t, false);
    }

    public void select(T t) {
        applySelection(t, false);
    }

    public void label(Translation translation, int i) {
        this.currentLabel = translation;
        this.labelSize = i;
    }

    public void toggle() {
        this.open = !this.open;
        if (this.open) {
            for (DropdownOptionItem<T> option : this.optionItems) {
                option.currentOptionAnimation().force(Objects.equals(option.option().value(), this.selectedValue));
            }
        }
        this.openAnimation.state(this.open);
    }

    public void applySelection(T t, boolean z) {
        if (this.optionItems.isEmpty()) {
            return;
        }
        DropdownOptionItem<T> orElse = this.optionItems.stream().filter(dropdownOptionItem -> {
            return Objects.equals(dropdownOptionItem.option().value(), t);
        }).findFirst().orElse((DropdownOptionItem) this.optionItems.getFirst());
        this.selectedValue = orElse.option().value();
        this.currentLabel = orElse.option().label();
        if (!z || this.onSelectCallback == null) {
            return;
        }
        this.onSelectCallback.accept(orElse.option().value());
        this.open = false;
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
        int color = colorStack.computeColor(palette.text().tone(100).argb());
        int iInterpolate2 = color;
        sanch3zss.raven.ui.menu.MenuStyle.drawDropdownField(drawCtx, x(), y(), fWidth, fHeight,
                this.cornerRadius, hoverAnimation.smoothAnimation());
        sanch3zss.raven.ui.menu.MenuStyle.drawDropdownArrows(drawCtx, x(), y(), fWidth, fHeight);
        float fX = x() + 7.0f;
        float fY = y() + (fHeight / TWO);
        String label = truncateText(drawCtx, this.currentLabel.effective(), Math.max(0.0f, fWidth - 14.0f));
        drawCtx.text(this.labelFont, label, this.labelSize, fX,
                y() + (fHeight - this.labelFont.getHeight(this.labelSize)) / TWO, iInterpolate2);
        if (this.optionItems.isEmpty() || (!this.open && this.openAnimation.isZero())) {
            this.panelBounds.withSize(0.0f, 0.0f);
            return;
        }
        float fY2 = y() + fHeight + MenuStyle.DROPDOWN_GAP;
        float screenWidth = drawCtx.layoutContext().logicalWidth();
        float panelX = Math.max(5.0f, Math.min(x(), screenWidth - this.panelWidth - 5.0f));
        this.panelBounds.withPosition(panelX, fY2).withSize(this.panelWidth, size);
        colorStack.push();
        colorStack.alpha(fSmoothAnimation);
        MenuStyle.drawDropdownPanel(drawCtx, panelX, fY2, this.panelWidth, size,
                MenuStyle.DROPDOWN_RADIUS);
        for (int i = 0; i < this.optionItems.size(); i++) {
            DropdownOptionItem<T> dropdownOptionItem = this.optionItems.get(i);
            float f4 = fY2 + OPTION_PADDING + (i * OPTION_HEIGHT);
            int iInterpolate3 = colorStack.computeColor(palette.text().tone(100).argb());
            float fX3 = panelX + MenuStyle.DROPDOWN_PADDING;
            float f5 = this.panelWidth - MenuStyle.DROPDOWN_PADDING * 2.0f;
            MenuStyle.drawDropdownOption(drawCtx, fX3, f4, f5, OPTION_HEIGHT,
                    dropdownOptionItem.clickableBehavior().hoverAnimation().smoothAnimation());
            boolean selected = Objects.equals(dropdownOptionItem.option().value(), this.selectedValue);
            if (selected) {
                MenuStyle.drawCheckmark(drawCtx, fX3 + 12.0f, f4 + OPTION_HEIGHT / 2.0f, 10.0f,
                        colorStack.computeColor(palette.accent().argb()));
            }
            float shift = selected ? 18.0f : 0.0f;
            String optionLabel = truncateText(drawCtx, dropdownOptionItem.option().label().effective(),
                    Math.max(0.0f, this.panelWidth - 16.0f - shift));
            drawCtx.text(this.optionFont, optionLabel, this.labelSize, fX3 + 6.0f + shift,
                    f4 + (OPTION_HEIGHT - this.optionFont.getHeight(this.labelSize)) / TWO, iInterpolate3);
            dropdownOptionItem.clickableBehavior().setDimensions(panelX, f4,
                    this.panelWidth, OPTION_HEIGHT);
        }
        colorStack.pop();
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean z2 = false;
        if (this.open && !z && (inputEventContext.inputEvent() instanceof MouseButtonInput)) {
            boolean zInArea = inputEventContext.inArea(this.headerBounds.x(), this.headerBounds.y(), this.headerBounds.width(), this.headerBounds.height());
            boolean zInArea2 = inputEventContext.inArea(this.panelBounds.x(), this.panelBounds.y(), this.panelBounds.width(), this.panelBounds.height());
            if (!zInArea && !zInArea2) {
                close();
                return true;
            }
        }
        if (this.open) {
            for (int size = this.optionItems.size() - 1; size >= 0; size--) {
                if (this.optionItems.get(size).clickableBehavior().handleInput(inputEventContext, z || z2)) {
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
        setSize(computePanelWidth(layoutScaleContext), MenuStyle.DROPDOWN_FIELD_HEIGHT);
        this.initialized = true;
        this.panelWidth = width();
        this.headerBounds.withPosition(x(), y()).withSize(width(), height());
        this.clickBehavior.setDimensions(x(), y(), width(), height());
        if (this.optionItems.isEmpty()) {
            return;
        }
        float fY = y() + height() + MenuStyle.DROPDOWN_GAP;
        for (int i = 0; i < this.optionItems.size(); i++) {
            this.optionItems.get(i).clickableBehavior().setDimensions(x(), fY + OPTION_PADDING + (i * OPTION_HEIGHT), this.panelWidth, OPTION_HEIGHT);
        }
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
        for (DropdownOptionItem<T> dropdownOptionItem : this.optionItems) {
            dropdownOptionItem.clickableBehavior().animate(weightedEngine);
            if (this.selectedValue != null) {
                dropdownOptionItem.currentOptionAnimation.state(Objects.equals(dropdownOptionItem.option().value(), this.selectedValue));
                dropdownOptionItem.currentOptionAnimation.animate(weightedEngine);
            }
        }
    }

    public float computePanelWidth(LayoutScaleContext layoutScaleContext) {
        float result = MenuStyle.DROPDOWN_MIN_WIDTH;
        for (DropdownOptionItem<T> option : this.optionItems) {
            result = Math.max(result, layoutScaleContext.textWidthPhysical(this.optionFont,
                    option.option().label().effective(), this.labelSize) + 32.0f);
        }
        return Math.min(MenuStyle.DROPDOWN_MAX_WIDTH, result);
    }

    private String truncateText(DrawCtx drawCtx, String text, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return "";
        }
        if (drawCtx.textWidthPhysical(this.labelFont, text, this.labelSize) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int end = text.length();
        while (end > 0 && drawCtx.textWidthPhysical(this.labelFont,
                text.substring(0, end) + suffix, this.labelSize) > maxWidth) {
            end--;
        }
        return end == 0 ? "" : text.substring(0, end) + suffix;
    }

    public List<DropdownOption<T>> options() {
        return this.optionItems.stream().map(DropdownOptionItem::option).toList();
    }

    public boolean isOpen() {
        return this.open;
    }
}
