package sanch3zss.raven.ui.menu.window;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import java.util.*;
import java.util.function.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.config.ConfigAutoSaveScheduler;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.feature.setting.BindMode;import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.IconLabelBadge;
import sanch3zss.raven.ui.component.layout.*;
import sanch3zss.raven.ui.hud.OverlayWidget;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.ui.setting.element.*;
import sanch3zss.raven.util.math.interpolation.Easings;

public class SettingsGroupWindow extends OverlayWidget {
    public final MsdfFont font;
    public final GlTexture propertyIcon;
    public final GlTexture groupIcon;
    public final IconLabelBadge settingsBadge;
    public final ToggleAnimator openAnimator;
    public final Translation title;

    public final FrameElementColumn settingsColumn;
    public final ScrollArea scrollArea;
    public final ScrollbarWidget scrollbar;
    public final WidgetBounds titleBounds;
    public final float headerHeight;
    public final ToggleAnimator hoverAnimator;
    private final boolean showHeader;
    private boolean hudBackground;

    public SettingsGroupWindow(Translation translation, List<Setting> list) {
        this(translation, list, true, true);
    }

    public SettingsGroupWindow(Translation translation, List<Setting> list, boolean z) {
        this(translation, list, z, true);
    }

    public SettingsGroupWindow(Translation translation, List<Setting> list, boolean z,
                               boolean showHeader) {
        this.font = Fonts.INTER_SEMIBOLD.get();
        this.propertyIcon = new GlTexture(new ClasspathResource("/icons/menu/new/property.png"));
        this.groupIcon = new GlTexture(new ClasspathResource("/icons/menu/new/group.png"));
        this.settingsBadge = new IconLabelBadge(this.propertyIcon, Translation.clearText("SETTINGS GROUP"));
        this.openAnimator = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);
        this.scrollArea = new ScrollArea();
        this.titleBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.hoverAnimator = new ToggleAnimator(1200, Easings.EASE_IN_OUT_CUBIC);
        setSize(280.0f, 36.0f);
        this.scrollbar = new ScrollbarWidget(
            this.scrollArea::scrollY,
            this::contentHeight,
            this.scrollArea::height,
            this.scrollArea::scrollTo,
            8.0f, 4.0f, 30.0f
        );
        this.settingsColumn = new FrameElementColumn(computeColumnWidth(), 10.0f);
        this.settingsColumn.clearElements();
        this.showHeader = showHeader;
        this.headerHeight = showHeader ? height() : 0.0f;
        this.title = translation;
        ExpandableSetting.inlineSmallGroups(list).forEach(value -> {
            if (value instanceof BooleanSetting booleanSetting) {
                Translation name = booleanSetting.getName();
                Translation description = booleanSetting.getDescription();
                CheckboxSettingElement checkboxSettingElement = new CheckboxSettingElement(name, description, booleanSetting::getKeyBind, z);
                checkboxSettingElement.toggledSupplier(booleanSetting::isValue);
                checkboxSettingElement.changeRunnable(wrapWithAutoSave(booleanSetting::switchValue));
                checkboxSettingElement.onChange(wrapConsumerWithAutoSave(booleanSetting::setKey));
                checkboxSettingElement.setupBindType(booleanSetting::getType, wrapConsumerWithAutoSave(booleanSetting::setType));
                checkboxSettingElement.visibleSupplier(booleanSetting.getVisible());
                this.settingsColumn.addFrameElement(checkboxSettingElement);
                return;
            }
            if (value instanceof NumberSetting numberSetting) {
                SliderSettingElement sliderSettingElement = new SliderSettingElement(
                    numberSetting.getName(),
                    numberSetting.getDescription(),
                    numberSetting.valueUnit(),
                    numberSetting.max(),
                    numberSetting.min(),
                    numberSetting.step()
                );
                sliderSettingElement.currentValueSupplier(numberSetting::currentValue);
                sliderSettingElement.onValueChanged(wrapConsumerWithAutoSave(numberSetting::setCurrentValue));
                sliderSettingElement.visibleSupplier(numberSetting.getVisible());
                this.settingsColumn.addFrameElement(sliderSettingElement);
                return;
            }
            if (value instanceof ModeSetting<?> modeSetting) {
                addModeSetting(modeSetting);
                return;
            }
            if (value instanceof EnumSetting<?> enumSetting) {
                addEnumSetting(enumSetting);
                return;
            }
            if (value instanceof MultiSelectSetting<?> multiSelectSetting) {
                addMultiSelectSetting(multiSelectSetting);
                return;
            }
            if (value instanceof TextFieldSetting textFieldSetting) {
                TextFieldSettingElement textFieldSettingElement = new TextFieldSettingElement(
                    textFieldSetting.getName(),
                    textFieldSetting.getDescription(),
                    textFieldSetting.getPlaceholder(),
                    textFieldSetting.isOnlyDigits(),
                    textFieldSetting.getMax(),
                    textFieldSetting.isPassword(),
                    textFieldSetting::getText
                );
                textFieldSettingElement.onChange(wrapConsumerWithAutoSave(textFieldSetting::setText));
                textFieldSettingElement.visibleSupplier(textFieldSetting.getVisible());
                this.settingsColumn.addFrameElement(textFieldSettingElement);
                return;
            }
            if (value instanceof ButtonSetting buttonSetting) {
                ButtonSettingElement buttonSettingElement = new ButtonSettingElement(
                    buttonSetting.getName(),
                    buttonSetting.getDescription(),
                    buttonSetting.getButtonName(),
                    wrapWithAutoSave(buttonSetting.getRunnable())
                );
                buttonSettingElement.visibleSupplier(buttonSetting.getVisible());
                this.settingsColumn.addFrameElement(buttonSettingElement);
                return;
            }
            if (value instanceof ColorSetting colorSetting) {
                ColorSettingElement colorSettingElement = new ColorSettingElement(
                    colorSetting.getName(),
                    colorSetting.getDescription(),
                    colorSetting::getColorWithoutAlpha,
                    colorSetting::getAlpha
                );
                colorSettingElement.gradient(colorSetting);
                colorSettingElement.visibleSupplier(colorSetting.getVisible());
                colorSettingElement.colorConsumer(colorSetting::setColor);
                colorSettingElement.alphaConsumer(colorSetting::setAlpha);
                colorSettingElement.commitRunnable(this::scheduleAutoSave);
                this.settingsColumn.addFrameElement(colorSettingElement);
                if (colorSetting.isGradientAvailable()) {
                    ModeSettingElement<ColorMode> colorModeElement = new ModeSettingElement<>(
                        Lang.COLOR_MODE, null, ColorMode.values()
                    );
                    colorModeElement.onChangeCallback(wrapConsumerWithAutoSave(colorSetting::setMode));
                    colorModeElement.currentValueSupplier(colorSetting::mode);
                    colorModeElement.visibleSupplier(colorSetting.getVisible());
                    this.settingsColumn.addFrameElement(colorModeElement);
                }
                return;
            }
            if (value instanceof OrderedEnumSetting<?> orderedEnumSetting) {
                addOrderedEnumSetting(orderedEnumSetting);
                return;
            }
            if (value instanceof ExpandableSetting expandableSetting) {
                GroupSettingElement groupSettingElement = new GroupSettingElement(
                    expandableSetting.getName(),
                    expandableSetting.getDescription(),
                    expandableSetting.getSubSettings(),
                    expandableSetting.getKeybinds()
                );
                groupSettingElement.navigationOnly(expandableSetting.isNavigationOnly());
                groupSettingElement.toggledSupplier(expandableSetting::isValue);
                groupSettingElement.changeRunnable(wrapWithAutoSave(expandableSetting::switchValue));
                groupSettingElement.visibleSupplier(expandableSetting.getVisible());
                groupSettingElement.onChange(wrapConsumerWithAutoSave(expandableSetting::setKey));
                this.settingsColumn.addFrameElement(groupSettingElement);
                return;
            }
            if (value instanceof KeybindSetting keybindSetting) {
                KeybindSettingElement keybindSettingElement = new KeybindSettingElement(
                    keybindSetting.getName(),
                    keybindSetting.getDescription(),
                    keybindSetting::getKeyBind
                );
                keybindSettingElement.visibleSupplier(keybindSetting.getVisible());
                keybindSettingElement.onChange(wrapConsumerWithAutoSave(keybindSetting::setKey));
                this.settingsColumn.addFrameElement(keybindSettingElement);
                return;
            }
            if (value instanceof SeparatorSetting) {
                this.settingsColumn.addFrameElement(new SeparatorElement());
            }
        });
        addChild(this.settingsColumn);
    }

    public void hudBackground(boolean enabled) {
        this.hudBackground = enabled;
    }

    public <E extends Enum<E>> void addModeSetting(ModeSetting<E> modeSetting) {
        ModeSettingElement modeSettingElement = new ModeSettingElement(modeSetting.getName(), modeSetting.getDescription(), modeSetting.options());
        modeSettingElement.onChangeCallback(wrapConsumerWithAutoSave(modeSetting::setCurrentValue));
        modeSettingElement.currentValueSupplier(modeSetting::currentValue);
        modeSettingElement.visibleSupplier(modeSetting.getVisible());
        this.settingsColumn.addFrameElement(modeSettingElement);
    }

    public <E extends Enum<E>> void addMultiSelectSetting(MultiSelectSetting<E> multiSelectSetting) {
        MultiSelectSettingElement multiSelectSettingElement = new MultiSelectSettingElement(
            multiSelectSetting.getName(),
            multiSelectSetting.getDescription(),
            multiSelectSetting.options(),
            multiSelectSetting::selectedValues,
            wrapConsumerWithAutoSave(multiSelectSetting::toggle)
        );
        multiSelectSettingElement.visibleSupplier(multiSelectSetting.getVisible());
        this.settingsColumn.addFrameElement(multiSelectSettingElement);
    }

    public <E extends Enum<E>> void addOrderedEnumSetting(OrderedEnumSetting<E> orderedEnumSetting) {
        OrderedListSettingElement<E> orderedListSettingElement = new OrderedListSettingElement<>(
            orderedEnumSetting::orderedValues,
            orderedEnumSetting::getSelected,
            orderedEnumSetting::optionName,
            wrapConsumerWithAutoSave(e -> {
                if (orderedEnumSetting.isSelected(e)) {
                    orderedEnumSetting.deselect(e);
                } else {
                    orderedEnumSetting.select(e);
                }
            }),
            (from, to) -> {
                orderedEnumSetting.move(from, to);
                scheduleAutoSave();
            }
        );
        orderedListSettingElement.visibleSupplier(orderedEnumSetting.getVisible());
        this.settingsColumn.addFrameElement(orderedListSettingElement);
    }

    public <E extends Enum<E>> void addEnumSetting(EnumSetting<E> enumSetting) {
        SoundModeSettingElement soundModeSettingElement = new SoundModeSettingElement(
            enumSetting.getName(),
            enumSetting.getDescription(),
            enumSetting.options(),
            enumSetting.soundAction()
        );
        soundModeSettingElement.onChangeCallback(wrapConsumerWithAutoSave(enumSetting::setCurrentValue));
        soundModeSettingElement.currentValueSupplier(enumSetting::currentValue);
        soundModeSettingElement.visibleSupplier(enumSetting.getVisible());
        this.settingsColumn.addFrameElement(soundModeSettingElement);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.openAnimator.isZero()) {
            return;
        }
        LayoutScaleContext layoutContext = drawCtx.layoutContext();
        layout(layoutContext);
        float maxY = Math.max(5.0f, layoutContext.logicalHeight() - totalHeight() - 5.0f);
        if (y() > maxY) {
            super.setPosition(x(), maxY);
            layout(layoutContext);
        }
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        colorStack.push();
        colorStack.alpha(this.openAnimator.smoothAnimation());
        float f = totalHeight();
        if (this.hudBackground) {
            float scale = layoutContext.scaleFactor();
            drawCtx.matrixStack().pushPose();
            drawCtx.matrixStack().scale(scale, scale, 1.0f);
            HudGlassPanel.draw(drawCtx.drawEngine(), drawCtx.matrixStack().last().pose(),
                    drawCtx.theme(), Raven.INSTANCE.widgetStack().hudBlurTexture(),
                    x(), y(), width(), f, 10.0f);
            drawCtx.matrixStack().popPose();
        } else {
            MenuStyle.drawSettingsPanel(drawCtx, x(), y(), width(), f, 10.0f);
        }
        if (this.showHeader) {
            float fX = x() + 12.0f;
            float fY = (y() + (this.headerHeight / 2.0f)) - (this.font.getHeight(13.0f) / 2.0f);
            float fTextWidthPhysical = drawCtx.textWidthPhysical(this.font, this.title.effective(), 13);
            float fX2 = x() + width() - 12.0f;
            float visibleTitleWidth = Math.max(0.0f, fX2 - fX);
            this.titleBounds.withPosition(fX, fY).withSize(Math.min(fTextWidthPhysical, visibleTitleWidth),
                    this.font.getHeight(13.0f));
            MenuStyle.drawScrollingText(drawCtx, this.font, this.title.effective(), 13,
                    fX, fY, visibleTitleWidth, this.hoverAnimator.smoothAnimation(),
                    colorStack.computeColor(palette.text().tone(100).argb()));
            drawCtx.fillRect(x() + 10.0f, y() + this.headerHeight - 1.0f, width() - 20.0f, 1.0f,
                    MenuStyle.outline(colorStack));
        } else {
            this.titleBounds.withSize(0.0f, 0.0f);
        }
        float contentTop = y() + this.headerHeight + (this.showHeader ? -1.0f : 6.0f);
        float contentViewportHeight = totalHeight() - this.headerHeight
                + (this.showHeader ? 2.0f : -12.0f);
        this.scrollArea.beginArea(drawCtx, x(), contentTop, width(), contentViewportHeight);
        this.settingsColumn.render(drawCtx);
        this.scrollArea.endArea(drawCtx, contentHeight() - (this.showHeader ? 0.0f : 12.0f));
        drawCtx.matrixStack().pushPose();
        drawCtx.matrixStack().translate(0.0f, -drawCtx.layoutContext().toPhysical(this.scrollArea.scrollY()), 0.0f);
        this.settingsColumn.renderOverlays(drawCtx);
        drawCtx.matrixStack().popPose();
        if (needsScrollbar()) {
            this.scrollbar.render(drawCtx);
        }
        colorStack.pop();
    }

    @Override
    public void setPosition(float f, float f2) {
        super.setPosition(f, f2);
        layout(new LayoutScaleContext(ScreenResolution.resolution(), Raven.INSTANCE.windowController().dpiScaleFactor()));
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.settingsColumn.setPosition(x() + 12.0f, y() + this.headerHeight + 10.0f);
        this.settingsColumn.setWidth(computeColumnWidth());
        this.groupIcon.setDimensions(14, 14);
        this.propertyIcon.setDimensions(10, 10);
        setSize(width(), this.headerHeight);
        this.scrollArea.layout(layoutScaleContext);
        this.scrollbar.layout(layoutScaleContext);
        this.scrollbar.setSize(4.0f, height() + contentHeight());
        float scrollbarY = this.showHeader
                ? this.titleBounds.y() + this.titleBounds.height() + 6.0f
                : y() + 8.0f;
        this.scrollbar.setPosition(((x() + width()) - 10.0f) - this.scrollbar.width(), scrollbarY);
        super.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (this.openAnimator.isZero()) {
            return false;
        }
        ColorPickerWindow picker = (ColorPickerWindow) Raven.INSTANCE.windowController()
                .getWindow(ColorPickerWindow.class);
        if (picker != null && picker.opened()
                && inputEventContext.inArea(picker.x(), picker.y(), picker.width(), picker.height())) {
            return false;
        }
        float fScrollY = this.scrollArea.scrollY();
        boolean zInArea = this.showHeader && inputEventContext.inArea(
                this.titleBounds.x(), this.titleBounds.y(), this.titleBounds.width(), this.titleBounds.height());
        this.hoverAnimator.state(zInArea && !z);
        if (super.handleInput(inputEventContext.withMouseOffset(0.0f, fScrollY), z)) {
            return true;
        }
        if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && !inputEventContext.inArea(x(), y(), width(), totalHeight())) {
            return true;
        }
        if ((inputEventContext.inputEvent() instanceof ScrollInput) && !inputEventContext.inArea(x(), y(), width(), totalHeight())) {
            closeWindow();
            return false;
        }
        InputEvent inputEvent = inputEventContext.inputEvent();
        if (inputEvent instanceof KeyInput keyInput) {
            if (keyInput.keyAction().press() && keyInput.keyCode() == 256) {
                closeWindow();
                return true;
            }
        }
        InputEvent inputEvent2 = inputEventContext.inputEvent();
        if ((inputEvent2 instanceof MouseButtonInput) && ((MouseButtonInput) inputEvent2).action().press()) {
            if (inputEventContext.inArea(x(), y(), width(), totalHeight())) {
                return true;
            }
            if (!z) {
                closeWindow();
                return true;
            }
        }
        if (needsScrollbar() && this.scrollbar.handleInput(inputEventContext, z)) {
            return true;
        }
        return this.scrollArea.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.openAnimator.animate(weightedEngine);
        this.scrollbar.animation(weightedEngine);
        this.hoverAnimator.animate(weightedEngine);
        this.scrollArea.animation(weightedEngine);
        this.settingsColumn.handleViewportVisibility(this.scrollArea.visibleTop(), this.scrollArea.visibleBottom(),
                this.settingsColumn.y());
        super.animation(weightedEngine);
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        super.collectBloomElements(renderCommandQueue);
    }

    public void openWindow() {
        if (isOpen()) {
            return;
        }
        this.openAnimator.state(true);
        layout(new LayoutScaleContext(ScreenResolution.resolution(), Raven.INSTANCE.windowController().dpiScaleFactor()));
    }

    public void closeWindow() {
        if (isOpen()) {
            this.openAnimator.state(false);
            super.handleClose();
        }
    }

    @Override
    public void closeOverlay() {
        closeWindow();
    }

    public void scheduleAutoSave() {
        ConfigAutoSaveScheduler.scheduleAutoSave();
    }

    public Runnable wrapWithAutoSave(Runnable runnable) {
        return () -> {
            if (runnable != null) {
                runnable.run();
            }
            scheduleAutoSave();
        };
    }

    public <T> Consumer<T> wrapConsumerWithAutoSave(Consumer<T> consumer) {
        return obj -> {
            consumer.accept(obj);
            scheduleAutoSave();
        };
    }

    public boolean needsScrollbar() {
        return contentHeight() > (totalHeight() - this.headerHeight) + 0.5f;
    }

    @Override
    public boolean isOpen() {
        return this.openAnimator.state() || !this.openAnimator.isZero();
    }

    @Override
    public boolean acceptsInput() {
        return this.openAnimator.state();
    }

    public float totalHeight() {
        return this.headerHeight + Math.min(contentHeight(), 400.0f);
    }

    public float computeColumnWidth() {
        return width() - (needsScrollbar() ? 38.0f : 28.0f);
    }

    public float contentHeight() {
        if (this.settingsColumn != null && this.settingsColumn.height() > 0.0f) {
            return 10.0f + this.settingsColumn.height() + 10.0f;
        }
        return 0.0f;
    }
}
