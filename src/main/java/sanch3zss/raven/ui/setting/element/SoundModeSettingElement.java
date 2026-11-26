package sanch3zss.raven.ui.setting.element;

import java.util.*;
import java.util.function.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.feature.setting.SoundModeOption;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.layout.ScrollArea;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class SoundModeSettingElement<T extends Enum<T>> extends ModuleFrame {
    public static final int titleFontSize = 14;
    public static final int descFontSize = 13;
    public static final float DESC_GAP = 3.0f;
    public static final float BLOCK_SPACING = 12.0f;
    public static final float FIELD_HEIGHT = MenuStyle.DROPDOWN_FIELD_HEIGHT;
    public static final float OPTION_HEIGHT = MenuStyle.DROPDOWN_OPTION_HEIGHT;
    public static final float RADIUS = 4.0f;
    public static final float HALF = 2.0f;
    public static final float POPUP_Z = 250.0f;

    public final Translation name;
    public final Translation description;
    public boolean expanded;
    public Supplier<T> valueSupplier;
    public Consumer<T> changeConsumer;
    public Supplier<Boolean> visibleCondition;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont valueFont = Fonts.INTER_BOLD.get();
    public final MsdfFont descFont = Fonts.INTER_MEDIUM.get();
    public final MsdfFont iconFont = Fonts.CLICK_GUI.get();
    public final ToggleAnimator dropdownAnimator = new ToggleAnimator(MenuStyle.MOTION_NORMAL_MS, Easings.EASE_IN_OUT_CUBIC);
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final GlTexture enumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png"));
    public final GlTexture checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
    public final GlTexture audioIcon = new GlTexture(new ClasspathResource("/icons/menu/new/audio.png"));
    public final ClickableBehavior clickable = new ClickableBehavior();
    public final List<SoundModeOption<T>> options = new ArrayList<>();
    public final WidgetBounds dropdownBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final ScrollArea dropdownScroll = new ScrollArea();
    public String wrappedDescription = null;
    public boolean descriptionDirty = true;
    public float lastLayoutWidth = -1.0f;
    public String lastDescription = null;
    public float descriptionHeight = 0.0f;


    public SoundModeSettingElement(Translation translation2, Translation translation, T[] tArr, Consumer<T> consumer) {
        this.name = translation2;
        this.description = translation;
        for (T t : tArr) {
            this.options.add(new SoundModeOption<>(t));
        }
        for (SoundModeOption<T> soundModeOption : this.options) {
            soundModeOption.clickableBehavior().clickCallback(() -> {
                if (this.expanded) {
                    invert();
                }
                if (this.changeConsumer != null) {
                    this.changeConsumer.accept(soundModeOption.option);
                }
            });
            soundModeOption.soundClickableBehavior.clickCallback(() -> {
                if (consumer != null) {
                    consumer.accept(soundModeOption.option);
                }
            });
        }
        this.clickable.clickCallback(this::invert);    }

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
        MenuStyle.drawFadingText(drawCtx, this.titleFont, this.name.effective(), titleFontSize,
                x(), fY, Math.max(0.0f, boxX - x() - 8.0f),
                colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()),
                        colorStack.computeColor(palette.text().tone(100).argb()), fValue));
        if (this.wrappedDescription != null) {
            fY += this.titleFont.getHeight(14.0f) + DESC_GAP;
            drawCtx.text(this.descFont, this.wrappedDescription, descFontSize, x(), fY, colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), fValue));
        }
        float height = y();
        MenuStyle.drawDropdownField(drawCtx, boxX, height, boxWidth, FIELD_HEIGHT, RADIUS,
                this.clickable.hoverAnimation().smoothAnimation());
        MenuStyle.drawDropdownArrows(drawCtx, boxX, height, boxWidth, FIELD_HEIGHT);
        float fX = boxX + 7.0f;
        float fX2 = boxX + boxWidth - 12.0f;
        if (this.valueSupplier == null) {
            return;
        }
        T t = this.valueSupplier.get();
        if (t instanceof DisplayNamed displayNamed) {
            float fWidth2 = fX;
            int color = colorStack.computeColor(palette.text().tone(100).argb());
            String strEffective = displayNamed.getDisplayName().effective();
            float f4 = fX2 - fWidth2;
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
                        height + (FIELD_HEIGHT - this.valueFont.getHeight(BLOCK_SPACING)) / HALF, color, i);
            } else {
                drawCtx.text(this.valueFont, strEffective, 12, fWidth2,
                        height + (FIELD_HEIGHT - this.valueFont.getHeight(BLOCK_SPACING)) / HALF, color);
            }
        }
        float f5 = height + FIELD_HEIGHT + DESC_GAP;
        float dropdownHeight = computeDropdownHeight();
        if (this.options.isEmpty() || (!this.expanded && this.dropdownAnimator.isZero())) {
            this.dropdownBounds.withSize(0.0f, 0.0f);
        } else {
            float screenHeight = sanch3zss.raven.render.engine.ScreenResolution.resolution().screenHeight()
                    / Math.max(Raven.INSTANCE.windowController().dpiScaleFactor(), 0.01f);
            float dropdownY = f5 + dropdownHeight > screenHeight - 5.0f ? height - DESC_GAP - dropdownHeight : f5;
            this.dropdownBounds.withPosition(boxX, Math.max(5.0f, dropdownY)).withSize(boxWidth, dropdownHeight);
        }
        this.clickable.setDimensions(boxX, height, boxWidth, FIELD_HEIGHT);
    }

    @Override
    public void drawOverlay(DrawCtx drawCtx) {
        if (this.options.isEmpty()) {
            return;
        }
        if (!this.dropdownAnimator.isZero() || this.expanded) {
            ThemePalette palette = drawCtx.theme().palette();
            ColorStack colorStack = drawCtx.drawEngine().colorStack();
            colorStack.push();
            colorStack.alpha(this.dropdownAnimator.smoothAnimation());
            drawCtx.matrixStack().pushPose();
            drawCtx.matrixStack().translate(0.0f, 0.0f, POPUP_Z);
            float fHeight = this.dropdownBounds.height();
            float fY = this.dropdownBounds.y();
            float fWidth = this.dropdownBounds.width();
            MenuStyle.drawDropdownPanel(drawCtx, this.dropdownBounds.x(), fY,
                    fWidth, fHeight, MenuStyle.DROPDOWN_RADIUS);
            this.dropdownScroll.beginArea(drawCtx, this.dropdownBounds.x(), fY, fWidth, fHeight);
            T t = this.valueSupplier == null ? null : this.valueSupplier.get();
            for (int i = 0; i < this.options.size(); i++) {
                SoundModeOption<T> soundModeOption = this.options.get(i);
                float f = fY + MenuStyle.DROPDOWN_PADDING + (i * OPTION_HEIGHT);
                soundModeOption.clickableBehavior.setDimensions(this.dropdownBounds.x(), f, fWidth, OPTION_HEIGHT);
                boolean z = t != null && soundModeOption.option == t;
                float fX = this.dropdownBounds.x() + MenuStyle.DROPDOWN_PADDING;
                float f2 = fWidth - MenuStyle.DROPDOWN_PADDING * 2.0f;
                MenuStyle.drawDropdownOption(drawCtx, fX, f, f2, OPTION_HEIGHT,
                        soundModeOption.clickableBehavior().hoverAnimation().smoothAnimation());
                float f3 = ((fX + f2) - 5.0f) - 14.0f;
                int color = colorStack.computeColor(palette.text().tone(100).argb());
                if (z) {
                    MenuStyle.drawCheckmark(drawCtx, fX + 12.0f, f + OPTION_HEIGHT / 2.0f,
                            this.checkmarkIcon.width() * 0.72f, colorStack.computeColor(palette.accent().argb()));
                }
                soundModeOption.soundClickableBehavior.setDimensions(f3 - 4.0f, ((f + OPTION_HEIGHT / 2.0f) - (14.0f / HALF)) - 4.0f, 14.0f + (4.0f * HALF), 14.0f + (4.0f * HALF));
                drawCtx.textureVerticalC(this.audioIcon, f3, f + OPTION_HEIGHT / 2.0f, this.checkmarkIcon.width(), this.checkmarkIcon.height(), colorStack.interpolate(color, colorStack.computeColor(palette.text().tone(!z ? 300 : 50).argb()), soundModeOption.soundClickableBehavior.hoverAnimation()));
                T t2 = soundModeOption.option;
                if (t2 instanceof DisplayNamed displayNamed) {
                    drawCtx.text(this.valueFont, displayNamed.getDisplayName().effective(), 12,
                            fX + 6.0f + (z ? 18.0f : 0.0f),
                            f + (OPTION_HEIGHT - this.valueFont.getHeight(BLOCK_SPACING)) / HALF,
                            colorStack.computeColor(palette.text().tone(100).argb()));
                }
            }
            this.dropdownScroll.endArea(drawCtx, rawDropdownHeight());
            drawCtx.matrixStack().popPose();
            colorStack.pop();
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.enumIcon.setDimensions(12, 12);
        this.checkmarkIcon.setDimensions(titleFontSize, titleFontSize);
        this.dropdownScroll.layout(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibleCondition != null) {
            visible(Boolean.TRUE.equals(this.visibleCondition.get()));
        }
        this.dropdownAnimator.animate(weightedEngine);
        this.clickable.animate(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        this.dropdownScroll.animation(weightedEngine);
        for (SoundModeOption<T> soundModeOption : this.options) {
            soundModeOption.clickableBehavior().animate(weightedEngine);
            soundModeOption.soundClickableBehavior.animate(weightedEngine);
            if (this.valueSupplier != null) {
                soundModeOption.currentOptionAnimation.state(soundModeOption.option == this.valueSupplier.get());
                soundModeOption.currentOptionAnimation.animate(weightedEngine);
            }
        }
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = false;
        if (this.expanded || !this.dropdownAnimator.isZero()) {
            InputEventContext scrolledContext = inputEventContext.withMouseOffset(0.0f, this.dropdownScroll.scrollY());
            for (int size = this.options.size() - 1; size >= 0; size--) {
                SoundModeOption<T> soundModeOption = this.options.get(size);
                boolean zHandleInput2 = zHandleInput | soundModeOption.soundClickableBehavior.handleInput(scrolledContext, z || zHandleInput);
                zHandleInput = zHandleInput2 | soundModeOption.clickableBehavior.handleInput(scrolledContext, z || zHandleInput2);
            }
            if (!z && !zHandleInput) {
                if ((inputEventContext.inputEvent() instanceof MouseButtonInput) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
                if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height())) {
                    return true;
                }
            }
            if (inputEventContext.inputEvent() instanceof ScrollInput) {
                if (this.dropdownScroll.handleInput(inputEventContext, z || zHandleInput)) {
                    return true;
                }
                closeDropdown();
                return true;
            }
            if (!z && (inputEventContext.inputEvent() instanceof MouseButtonInput)) {
                boolean zInArea = inputEventContext.inArea(this.clickable.ownerX(), this.clickable.ownerY(), this.clickable.ownerW(), this.clickable.ownerH());
                boolean zInArea2 = inputEventContext.inArea(this.dropdownBounds.x(), this.dropdownBounds.y(), this.dropdownBounds.width(), this.dropdownBounds.height());
                if (!zInArea && !zInArea2) {
                    closeDropdown();
                    return true;
                }
            }
        }
        if (this.clickable.handleInput(inputEventContext, z || zHandleInput)) {
            zHandleInput = true;
        }
        return zHandleInput;
    }

    @Override
    public void handleClose() {
        closeDropdown();        super.handleClose();
    }

    @Override
    public void handleViewportVisibility(float f, float f2, float f3) {
        if (this.expanded || !this.dropdownAnimator.isZero()) {
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

    public float computeDropdownHeight() {
        if (this.dropdownAnimator.isZero() && !this.expanded) {
            return 0.0f;
        }
        return MenuStyle.dropdownHeight(this.options.size());
    }

    public float rawDropdownHeight() {
        return MenuStyle.dropdownContentHeight(this.options.size());
    }

    private float preferredDropdownWidth(DrawCtx drawCtx, float availableWidth) {
        float width = MenuStyle.DROPDOWN_MIN_WIDTH;
        for (SoundModeOption<T> option : this.options) {
            String label = option.option instanceof DisplayNamed named
                    ? named.getDisplayName().effective() : option.option.name();
            width = Math.max(width, drawCtx.textWidthPhysical(this.valueFont, label, 12) + 50.0f);
        }
        return MenuStyle.settingControlWidth(width, availableWidth);
    }

    public void closeDropdown() {
        if (this.expanded || !this.dropdownAnimator.isZero()) {
            this.expanded = false;
            this.dropdownAnimator.state(false);
            this.dropdownScroll.scrollTo(0.0f);
        }
    }

    public void invert() {
        this.expanded = !this.expanded;
        this.dropdownAnimator.state(this.expanded);
    }

    public void updateDescription(float f) {
        if (this.description == null) {
            this.wrappedDescription = null;
            this.descriptionHeight = 0.0f;
            this.descriptionDirty = false;
            this.lastLayoutWidth = f;
            this.lastDescription = null;
            return;
        }
        String strEffective = this.description.effective();
        if (this.descriptionDirty || f != this.lastLayoutWidth || strEffective == null || !strEffective.equals(this.lastDescription)) {
            if (strEffective == null) {
                strEffective = "";
            }
            this.wrappedDescription = StringUtil.formatTextToFitWidth(strEffective, f, this.descFont, descFontSize);
            this.descriptionHeight = this.descFont.getHeightWithLineBreaks(this.wrappedDescription, descFontSize);
            this.lastLayoutWidth = f;
            this.lastDescription = strEffective;
            this.descriptionDirty = false;
        }
    }

    public Translation name() {
        return this.name;
    }

    public SoundModeSettingElement<T> currentValueSupplier(Supplier<T> supplier) {
        this.valueSupplier = supplier;
        return this;
    }

    public SoundModeSettingElement<T> onChangeCallback(Consumer<T> consumer) {
        this.changeConsumer = consumer;
        return this;
    }

    public SoundModeSettingElement<T> visibleSupplier(Supplier<Boolean> supplier) {
        this.visibleCondition = supplier;
        return this;
    }
}
