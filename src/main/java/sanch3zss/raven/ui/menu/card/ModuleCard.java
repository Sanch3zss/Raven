package sanch3zss.raven.ui.menu.card;

import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.OverlayCommandQueue;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import sanch3zss.raven.render.engine.ScreenResolution;
import java.io.IOException;
import java.util.*;
import java.util.function.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.config.ConfigAutoSaveScheduler;
import sanch3zss.raven.client.i18n.*;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.input.MouseButtonInput;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.BindMode;import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.*;
import sanch3zss.raven.ui.component.input.KeybindField;
import sanch3zss.raven.ui.component.layout.*;
import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.window.*;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.ui.setting.element.*;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ModuleCard extends AbstractFrame {
    private boolean firstInSection;
    private boolean lastInSection;
    public final GlTexture frameIcon;
    public final MsdfFont font;
    public final MsdfFont descriptionFont;
    public final MsdfFont iconFont;
    public final ClickableBehavior optionsBehavior;
    public final boolean complexSettings;
    public final SettingsGroupWindow settingsWindow;
    static final float height = 44.0f;
    public final Module module;
    public final String[] aliases;
    public final String name;
    public final ToggleSwitch toggleSwitch;

    public final KeybindField keybindField;
    static final float columnWidth = 200.0f;
    static final float padding = 14.0f;
    public final ToggleAnimator nameHoverAnimation;
    public final ToggleAnimator descriptionHoverAnimation;
    public String wrappedModuleDescription;
    public String cachedModuleDescription;
    public float cachedDescriptionWidth = -1.0f;
    public float moduleDescriptionHeight;
    public float nameX;
    public float nameY;
    public float nameWidth;
    public float nameHeight;
    public float nameVisibleWidth;
    public final AnimatedFloat xAnimation;
    public final AnimatedFloat yAnimation;

    public final KeybindConfigWindow keybindWindow;
    public final WidgetBounds keybindBounds;
    public float scrollOffset;

    public final HighlightAnimation highlightAnimation;

    public ScreenResolution resolution;
    public boolean positionInitialized;
    public static final float gap = 8.0f;
    public static final float screenMargin = 5.0f;
    public boolean dragging;

    public ModuleCard(Module module, String[] strArr) {
        super(new FrameElementColumn(columnWidth, MenuStyle.CONTENT_GAP));
        this.module = module;
        this.complexSettings = !module.getSettings().isEmpty();
        this.settingsWindow = this.complexSettings
                ? new SettingsGroupWindow(Translation.clearText(module.getVisibleName()), module.getSettings(), true, false)
                : null;
        this.frameIcon = new GlTexture(new ClasspathResource("/icons/menu/new/frame.png"));
        this.font = Fonts.INTER_SEMIBOLD.get();
        this.descriptionFont = Fonts.INTER_MEDIUM.get();
        this.iconFont = Fonts.CLICK_GUI.get();
        this.optionsBehavior = new ClickableBehavior().clickCallback(this::toggleSettingsWindow);
        this.nameHoverAnimation = new ToggleAnimator(1200, Easings.EASE_IN_OUT_CUBIC);
        this.descriptionHoverAnimation = new ToggleAnimator(180, Easings.EASE_IN_OUT_CUBIC);
        this.xAnimation = new AnimatedFloat(120, Easings.EASE_OUT_CUBIC);
        this.yAnimation = new AnimatedFloat(120, Easings.EASE_OUT_CUBIC);
        this.keybindBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
        this.aliases = strArr;
        this.name = module.getName();
        Objects.requireNonNull(module);
        this.toggleSwitch = new ToggleSwitch(30.0f, 18.0f, module::isState);
        ToggleSwitch toggleSwitch = this.toggleSwitch;
        Objects.requireNonNull(module);
        toggleSwitch.runnable(wrapWithAutoSave(module::switchState));
        this.toggleSwitch.clickable.middleClickCallback(this::toggleKeybindWindow);
        if (this.complexSettings) {
            this.toggleSwitch.clickable.rightClickCallback(this::toggleSettingsWindow);
        }
        Translation translation = Lang.BINDING_MODULE;
        String name = module.getName();
        Objects.requireNonNull(module);
        this.keybindWindow = new KeybindConfigWindow(translation, name, module::getKeyBind);
        KeybindConfigWindow keybindConfigWindow2 = this.keybindWindow;
        Objects.requireNonNull(module);
        Supplier<BindMode> supplier = module::getType;
        Objects.requireNonNull(module);
        keybindConfigWindow2.bindType(supplier, wrapConsumerWithAutoSave(module::setType));
        KeybindConfigWindow keybindConfigWindow = this.keybindWindow;
        Objects.requireNonNull(module);
        keybindConfigWindow.onChange(wrapConsumerWithAutoSave(module::setKey));
        Raven.INSTANCE.menuWindow().priorityOverlayHandler().registerPopup(this.keybindWindow);
        if (this.settingsWindow != null) {
            Raven.INSTANCE.menuWindow().priorityOverlayHandler().registerPopup(this.settingsWindow);
        }
        this.keybindField = new KeybindField(Fonts.INTER_SEMIBOLD.get(), () -> {
            toggleKeybindWindow();
        }, gap, 4.0f, 6.0f, 11).addMutableKeys(module.getKeyBind());
        KeybindField keybindField = this.keybindField;
        Objects.requireNonNull(module);
        keybindField.onChange(wrapConsumerWithAutoSave(module::setKey));
        module.getSettings().forEach(value -> {
            if (this.complexSettings) {
                return;
            }
            if (value instanceof BooleanSetting) {
                BooleanSetting booleanSetting = (BooleanSetting) value;
                Translation name2 = booleanSetting.getName();
                Translation description = booleanSetting.getDescription();
                Objects.requireNonNull(booleanSetting);
                CheckboxSettingElement checkboxSettingElement = new CheckboxSettingElement(name2, description, booleanSetting::getKeyBind);
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.toggledSupplier(booleanSetting::isValue);
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.changeRunnable(wrapWithAutoSave(booleanSetting::switchValue));
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.onChange(wrapConsumerWithAutoSave(booleanSetting::setKey));
                Objects.requireNonNull(booleanSetting);
                checkboxSettingElement.setupBindType(booleanSetting::getType, wrapConsumerWithAutoSave(booleanSetting::setType));
                checkboxSettingElement.visibleSupplier(booleanSetting.getVisible());
                this.frameElementsContainer.addFrameElement(checkboxSettingElement);
                return;
            }
            if (value instanceof NumberSetting numberSetting) {
                SliderSettingElement sliderSettingElement = new SliderSettingElement(numberSetting.getName(), numberSetting.getDescription(), numberSetting.valueUnit(), numberSetting.max(), numberSetting.min(), numberSetting.step());
                sliderSettingElement.currentValueSupplier(numberSetting::currentValue);
                sliderSettingElement.onValueChanged(wrapConsumerWithAutoSave(numberSetting::setCurrentValue));
                sliderSettingElement.visibleSupplier(numberSetting.getVisible());
                this.frameElementsContainer.addFrameElement(sliderSettingElement);
                return;
            }
            if (value instanceof ModeSetting modeSetting) {
                addModeElement(modeSetting);
                return;
            }
            if (value instanceof EnumSetting enumSetting) {
                addSoundModeElement(enumSetting);
                return;
            }
            if (value instanceof MultiSelectSetting multiSelectSetting) {
                addMultiSelectElement(multiSelectSetting);
                return;
            }
            if (value instanceof TextFieldSetting textFieldSetting) {
                TextFieldSettingElement textFieldSettingElement = new TextFieldSettingElement(textFieldSetting.getName(), textFieldSetting.getDescription(), textFieldSetting.getPlaceholder(), textFieldSetting.isOnlyDigits(), textFieldSetting.getMax(), textFieldSetting.isPassword(), textFieldSetting::getText);
                textFieldSettingElement.onChange(wrapConsumerWithAutoSave(textFieldSetting::setText));
                textFieldSettingElement.visibleSupplier(textFieldSetting.getVisible());
                this.frameElementsContainer.addFrameElement(textFieldSettingElement);
                return;
            }
            if (value instanceof ButtonSetting buttonSetting) {
                ButtonSettingElement buttonSettingElement = new ButtonSettingElement(buttonSetting.getName(), buttonSetting.getDescription(), buttonSetting.getButtonName(), wrapWithAutoSave(buttonSetting.getRunnable()));
                buttonSettingElement.visibleSupplier(buttonSetting.getVisible());
                this.frameElementsContainer.addFrameElement(buttonSettingElement);
                return;
            }
            if (value instanceof OrderedEnumSetting orderedEnumSetting) {
                addOrderedListElement(orderedEnumSetting);
                return;
            }
            if (value instanceof ColorSetting colorSetting) {
                ColorSettingElement colorSettingElement = new ColorSettingElement(colorSetting.getName(), colorSetting.getDescription(), colorSetting::getColorWithoutAlpha, colorSetting::getAlpha);
                colorSettingElement.gradient(colorSetting);
                colorSettingElement.visibleSupplier(colorSetting.getVisible());
                colorSettingElement.colorConsumer(colorSetting::setColor);
                colorSettingElement.alphaConsumer(colorSetting::setAlpha);
                colorSettingElement.commitRunnable(this::scheduleAutoSave);
                this.frameElementsContainer.addFrameElement(colorSettingElement);
                if (colorSetting.isGradientAvailable()) {
                    ModeSettingElement<ColorMode> colorModeElement = new ModeSettingElement<>(
                            Lang.COLOR_MODE, null, ColorMode.values());
                    colorModeElement.onChangeCallback(wrapConsumerWithAutoSave(colorSetting::setMode));
                    colorModeElement.currentValueSupplier(colorSetting::mode);
                    colorModeElement.visibleSupplier(colorSetting.getVisible());
                    this.frameElementsContainer.addFrameElement(colorModeElement);
                }
                return;
            }
            if (value instanceof ExpandableSetting expandableSetting) {
                GroupSettingElement groupSettingElement = new GroupSettingElement(expandableSetting.getName(), expandableSetting.getDescription(), expandableSetting.getSubSettings(), expandableSetting.getKeybinds());
                groupSettingElement.navigationOnly(expandableSetting.isNavigationOnly());
                groupSettingElement.toggledSupplier(expandableSetting::isValue);
                Objects.requireNonNull(expandableSetting);
                groupSettingElement.changeRunnable(wrapWithAutoSave(expandableSetting::switchValue));
                groupSettingElement.visibleSupplier(expandableSetting.getVisible());
                Objects.requireNonNull(expandableSetting);
                groupSettingElement.onChange(wrapConsumerWithAutoSave(expandableSetting::setKey));
                this.frameElementsContainer.addFrameElement(groupSettingElement);
                return;
            }
            if (!(value instanceof KeybindSetting)) {
                if (value instanceof SeparatorSetting) {
                    this.frameElementsContainer.addFrameElement(new SeparatorElement());
                    return;
                }
                return;
            }
            KeybindSetting keybindSetting = (KeybindSetting) value;
            Translation name5 = keybindSetting.getName();
            Translation description4 = keybindSetting.getDescription();
            Objects.requireNonNull(keybindSetting);
            KeybindSettingElement keybindSettingElement = new KeybindSettingElement(name5, description4, keybindSetting::getKeyBind);
            keybindSettingElement.visibleSupplier(keybindSetting.getVisible());
            Objects.requireNonNull(keybindSetting);
            keybindSettingElement.onChange(wrapConsumerWithAutoSave(keybindSetting::setKey));
            this.frameElementsContainer.addFrameElement(keybindSettingElement);
        });
        addChild(this.toggleSwitch);
    }

    public <E extends Enum<E>> void addOrderedListElement(OrderedEnumSetting<E> orderedEnumSetting) {
        Objects.requireNonNull(orderedEnumSetting);
        Supplier<List<E>> supplier = orderedEnumSetting::orderedValues;
        Objects.requireNonNull(orderedEnumSetting);
        Supplier<Set<E>> supplier2 = orderedEnumSetting::getSelected;
        Objects.requireNonNull(orderedEnumSetting);
        OrderedListSettingElement<E> orderedListSettingElement = new OrderedListSettingElement<>(supplier, supplier2, orderedEnumSetting::optionName, wrapConsumerWithAutoSave(e -> {
            if (orderedEnumSetting.isSelected(e)) {
                orderedEnumSetting.deselect(e);
            } else {
                orderedEnumSetting.select(e);
            }
        }), (num, num2) -> {
            orderedEnumSetting.move(num.intValue(), num2.intValue());
            scheduleAutoSave();
        });
        orderedListSettingElement.visibleSupplier(orderedEnumSetting.getVisible());
        this.frameElementsContainer.addFrameElement(orderedListSettingElement);
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
        float fX = this.keybindBounds.x();
        float fY = this.keybindBounds.y() - this.scrollOffset;
        float fWidth2 = this.keybindBounds.width();
        float fHeight = this.keybindBounds.height();
        float f2 = fX + fWidth2 + gap;
        float f3 = (fX - gap) - fWidth;
        if (f2 + fWidth > fScreenWidth - screenMargin && f3 >= screenMargin) {
            f2 = f3;
        }
        float fMax = Math.max(screenMargin, Math.min(f2, (fScreenWidth - fWidth) - screenMargin));
        float f4 = (fY + (fHeight / 2.0f)) - (f / 2.0f);
        if (f4 + f > fScreenHeight - screenMargin) {
            f4 = (fScreenHeight - f) - screenMargin;
        }
        if (f4 < screenMargin) {
            f4 = 5.0f;
        }
        this.keybindWindow.openWindow();
        this.keybindWindow.setPosition(fMax, f4);
        Raven.INSTANCE.menuWindow().priorityOverlayHandler().bringPopupToFront(this.keybindWindow);
    }

    public void toggleSettingsWindow() {
        if (this.settingsWindow == null || this.resolution == null) {
            return;
        }
        if (this.settingsWindow.isOpen()) {
            this.settingsWindow.closeWindow();
            return;
        }
        float dpi = Math.max(Raven.INSTANCE.windowController().dpiScaleFactor(), 0.01f);
        float screenWidth = this.resolution.screenWidth() / dpi;
        float screenHeight = this.resolution.screenHeight() / dpi;
        float popupWidth = this.settingsWindow.width();
        float popupHeight = this.settingsWindow.totalHeight();
        Raven.INSTANCE.menuWindow().priorityOverlayHandler().closePopupsExcept(this.settingsWindow);
        float visibleY = this.optionsBehavior.ownerY() - this.scrollOffset;
        float popupX = this.optionsBehavior.ownerX() + this.optionsBehavior.ownerW() + gap;
        if (popupX + popupWidth > screenWidth - screenMargin) {
            popupX = this.optionsBehavior.ownerX() - gap - popupWidth;
        }
        popupX = Math.max(screenMargin, Math.min(popupX, screenWidth - popupWidth - screenMargin));
        float popupY = Math.max(screenMargin, Math.min(visibleY - 8.0f,
                screenHeight - popupHeight - screenMargin));
        this.settingsWindow.setPosition(popupX, popupY);
        this.settingsWindow.openWindow();
        Raven.INSTANCE.menuWindow().priorityOverlayHandler().bringPopupToFront(this.settingsWindow);
    }

    public <E extends Enum<E>> void addModeElement(ModeSetting<E> modeSetting) {
        ModeSettingElement modeSettingElement = new ModeSettingElement(modeSetting.getName(), modeSetting.getDescription(), modeSetting.options());
        Objects.requireNonNull(modeSetting);
        modeSettingElement.onChangeCallback(wrapConsumerWithAutoSave(modeSetting::setCurrentValue));
        Objects.requireNonNull(modeSetting);
        modeSettingElement.currentValueSupplier(modeSetting::currentValue);
        modeSettingElement.visibleSupplier(modeSetting.getVisible());
        this.frameElementsContainer.addFrameElement(modeSettingElement);
    }

    public <E extends Enum<E>> void addMultiSelectElement(MultiSelectSetting<E> multiSelectSetting) {
        Translation name = multiSelectSetting.getName();
        Translation description = multiSelectSetting.getDescription();
        Enum[] enumArrOptions = multiSelectSetting.options();
        Objects.requireNonNull(multiSelectSetting);
        Supplier supplier = multiSelectSetting::selectedValues;
        Objects.requireNonNull(multiSelectSetting);
        MultiSelectSettingElement multiSelectSettingElement = new MultiSelectSettingElement(name, description, enumArrOptions, supplier, wrapConsumerWithAutoSave(multiSelectSetting::toggle));
        multiSelectSettingElement.visibleSupplier(multiSelectSetting.getVisible());
        this.frameElementsContainer.addFrameElement(multiSelectSettingElement);
    }

    public <E extends Enum<E>> void addSoundModeElement(EnumSetting<E> enumSetting) {
        SoundModeSettingElement soundModeSettingElement = new SoundModeSettingElement(enumSetting.getName(), enumSetting.getDescription(), enumSetting.options(), enumSetting.soundAction());
        Objects.requireNonNull(enumSetting);
        soundModeSettingElement.onChangeCallback(wrapConsumerWithAutoSave(enumSetting::setCurrentValue));
        Objects.requireNonNull(enumSetting);
        soundModeSettingElement.currentValueSupplier(enumSetting::currentValue);
        soundModeSettingElement.visibleSupplier(enumSetting.getVisible());
        this.frameElementsContainer.addFrameElement(soundModeSettingElement);
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

    public List<Setting> settings() {
        return this.module.getSettings();
    }

    @Override
    public void updateViewportVisibility(float f, float f2, float f3) {
        this.scrollOffset = f;
        super.updateViewportVisibility(f, f2, f3);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.module.isBlocked()) {
            renderBlocked(drawCtx);
            return;
        }
        this.resolution = drawCtx.resolution();
        updateModuleDescription(drawCtx);
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        MenuStyle.drawModuleRow(drawCtx, x(), y(), width(), height(),
                this.descriptionHoverAnimation.smoothAnimation(),
                this.firstInSection, this.lastInSection);
        int iInterpolate = colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.accent().argb()), this.toggleSwitch.toggleSwitchAnimation());
        int color = colorStack.computeColor(palette.text().tone(100).argb());
        this.nameX = x() + padding;
        this.nameY = (y() + (height() / 2.0f)) - (this.font.getHeight(12.0f) / 2.0f);
        this.nameWidth = drawCtx.textWidthPhysical(this.font, this.name, 12);
        this.nameHeight = this.font.getHeight(12.0f);
        float fX = this.toggleSwitch.x() - (this.complexSettings ? 29.0f : 7.0f);
        this.nameVisibleWidth = Math.max(0.0f, fX - this.nameX);
        MenuStyle.drawScrollingText(drawCtx, this.font, this.name, 12,
                this.nameX, this.nameY, this.nameVisibleWidth,
                this.nameHoverAnimation.smoothAnimation(), color);
        renderFrameElements(drawCtx);
        if (this.complexSettings) {
            int optionsColor = colorStack.interpolate(colorStack.computeColor(palette.accent().argb(), 0.72f),
                    colorStack.computeColor(palette.gradientEnd().argb()), this.optionsBehavior.hoverAnimation());
            float optionsX = this.toggleSwitch.x() - 24.0f;
            MenuStyle.drawOptionsIcon(drawCtx, this.iconFont, optionsX, y(), height(), optionsColor);
            this.optionsBehavior.setDimensions(optionsX - 2.0f, y() + (height() - 18.0f) / 2.0f, 20.0f, 18.0f);
        } else {
            this.optionsBehavior.setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
        }
        this.toggleSwitch.render(drawCtx);
        this.toggleSwitch.setClickableArea(x(), y(), width(), height());
    }

    private void renderBlocked(DrawCtx drawCtx) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        colorStack.push();
        colorStack.alpha(0.5f);
        MenuStyle.drawModuleRow(drawCtx, x(), y(), width(), height(), 0.0f,
                this.firstInSection, this.lastInSection);
        String badge = "Недоступно";
        float badgeWidth = drawCtx.textWidthPhysical(this.font, badge, 10);
        float badgeHeight = 16.0f;
        float badgeX = x() + width() - padding - badgeWidth;
        float badgeY = y() + (height() - badgeHeight) / 2.0f;
        this.nameX = x() + padding;
        this.nameY = (y() + (height() / 2.0f)) - (this.font.getHeight(12.0f) / 2.0f);
        this.nameWidth = drawCtx.textWidthPhysical(this.font, this.name, 12);
        this.nameHeight = this.font.getHeight(12.0f);
        this.nameVisibleWidth = Math.max(0.0f, badgeX - 8.0f - this.nameX);
        MenuStyle.drawScrollingText(drawCtx, this.font, this.name, 12, this.nameX, this.nameY, this.nameVisibleWidth,
                0.0f, colorStack.computeColor(palette.text().tone(400).argb()));
        drawCtx.hairline(this.nameX, this.nameY + this.nameHeight / 2.0f, Math.min(this.nameWidth, this.nameVisibleWidth),
                colorStack.computeColor(palette.text().tone(400).argb(), 0.7f));
        drawCtx.text(this.font, badge, 10, badgeX,
                badgeY + (badgeHeight - this.font.getHeight(10.0f)) / 2.0f,
                colorStack.computeColor(palette.text().tone(300).argb()));
        colorStack.pop();
        this.toggleSwitch.setClickableArea(0.0f, 0.0f, 0.0f, 0.0f);
        this.optionsBehavior.setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
    }

    @Override
    public void onMenuDrag(boolean z) {
        this.dragging = z;
        if (z && this.positionInitialized) {
            this.xAnimation.set(x());
            this.yAnimation.set(y());
            this.xAnimation.destination(x());
            this.yAnimation.destination(y());
        }
    }

    @Override
    public void renderOverlays(DrawCtx drawCtx) {
        renderFrameOverlays(drawCtx);
        renderModuleDescription(drawCtx, drawCtx.colorStack(), drawCtx.theme().palette());
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.keybindWindow.layout(layoutScaleContext);
        if (this.settingsWindow != null) {
            this.settingsWindow.layout(layoutScaleContext);
        }
        this.keybindField.layout(layoutScaleContext);
        this.frameElementsContainer.setWidth(width() - (padding * 2.0f));
        this.frameIcon.setDimensions(14, 14);
        this.toggleSwitch.setPosition(((x() + width()) - padding) - this.toggleSwitch.width(), (y() + (height() / 2.0f)) - (this.toggleSwitch.height() / 2.0f));
        this.keybindBounds.withPosition(x(), y()).withSize(width(), height());
        this.frameElementsContainer.setPosition(x() + padding,
                y() + height() + 10.0f);
        super.layout(layoutScaleContext);
    }

    public void targetPosition(float f, float f2, boolean z) {
        if (!this.positionInitialized || !z || this.dragging) {
            snapTo(f, f2);
        } else {
            this.xAnimation.destination(f);
            this.yAnimation.destination(f2);
        }
        this.positionInitialized = true;
    }

    public void snapTo(float f, float f2) {
        setPosition(f, f2);
        this.xAnimation.set(f);
        this.yAnimation.set(f2);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        MenuWindow menu = Raven.INSTANCE.menuWindow();
        boolean insideMenu = menu != null && menu.menuOpen
                && inputEventContext.inArea(menu.x() + MenuStyle.WORKSPACE_X,
                menu.y() + MenuStyle.HEADER_HEIGHT + this.scrollOffset,
                menu.width() - MenuStyle.WORKSPACE_X,
                menu.height() - MenuStyle.HEADER_HEIGHT);
        this.nameHoverAnimation.state(insideMenu && inputEventContext.inArea(this.nameX, this.nameY,
                Math.min(this.nameWidth, this.nameVisibleWidth), this.nameHeight) && !z);
        this.descriptionHoverAnimation.state(insideMenu
                && inputEventContext.inArea(x(), y(), width(), height()) && !z);
        if (this.module.isBlocked()) {
            return z || inputEventContext.inputEvent() instanceof MouseButtonInput
                    && insideMenu && inputEventContext.inArea(x(), y(), width(), height());
        }
        boolean handled = this.complexSettings && this.optionsBehavior.handleInput(inputEventContext, z);
        return super.handleInput(inputEventContext, z || handled);
    }

    public boolean handleOpenDropdownInput(InputEventContext inputEventContext) {
        return this.frameElementsContainer.handleOpenDropdownInput(inputEventContext);
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue) {
        super.collectBlurElements(overlayCommandQueue);
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        this.keybindWindow.collectBloomElements(renderCommandQueue);
        if (this.settingsWindow != null) {
            this.settingsWindow.collectBloomElements(renderCommandQueue);
        }
        super.collectBloomElements(renderCommandQueue);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.positionInitialized && !this.dragging) {
            this.xAnimation.animate(weightedEngine);
            this.yAnimation.animate(weightedEngine);
            setPosition(this.xAnimation.animatedValue(), this.yAnimation.animatedValue());
        }
        this.nameHoverAnimation.animate(weightedEngine);
        this.descriptionHoverAnimation.animate(weightedEngine);
        this.optionsBehavior.animate(weightedEngine);
        this.keybindWindow.animation(weightedEngine);
        if (this.settingsWindow != null) {
            this.settingsWindow.animation(weightedEngine);
        }
        this.keybindField.syncKeys(this.module.getKeyBind());
        this.highlightAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public void handleClose() {
        this.keybindWindow.closeWindow();
        if (this.settingsWindow != null) {
            this.settingsWindow.closeWindow();
        }
        super.handleClose();
    }

    public void hideForServerPolicy() {
        handleClose();
        this.keybindWindow.openAnimation.force(false);
        if (this.settingsWindow != null) {
            this.settingsWindow.openAnimator.force(false);
        }
    }

    @Override
    public float width() {
        float workspaceWidth = MenuWindow.MENU_WIDTH - MenuStyle.WORKSPACE_X;
        return ((workspaceWidth - 20.0f - 20.0f) - MenuStyle.CONTENT_GAP) / 2.0f;
    }

    @Override
    public float height() {
        return height;
    }

    @Override
    public float contentHeight() {
        if (this.frameElementsContainer.height() <= 0.0f) {
            return 0.0f;
        }
        return 10.0f + this.frameElementsContainer.height() + 9.5f;
    }

    private void renderModuleDescription(DrawCtx drawCtx, ColorStack colors, ThemePalette palette) {
        float progress = this.descriptionHoverAnimation.smoothAnimation();
        if (progress <= 0.01f || this.wrappedModuleDescription == null) {
            return;
        }
        float titleHeight = this.font.getHeight(12.0f);
        float tooltipHeight = 10.0f * 2.0f + titleHeight + 5.0f
                + this.moduleDescriptionHeight;
        float screenWidth = drawCtx.layoutContext().logicalWidth();
        float screenHeight = drawCtx.layoutContext().logicalHeight();
        float workspaceCenter = Raven.INSTANCE.menuWindow().x() + MenuStyle.WORKSPACE_X
                + (MenuWindow.MENU_WIDTH - MenuStyle.WORKSPACE_X) / 2.0f;
        float tooltipX = x() >= workspaceCenter
                ? x() - 220.0f - 6.0f
                : x() + width() + 6.0f;
        if (tooltipX < screenMargin || tooltipX + 220.0f > screenWidth - screenMargin) {
            tooltipX = x() >= workspaceCenter
                    ? x() + width() + 6.0f
                    : x() - 220.0f - 6.0f;
        }
        tooltipX = Math.max(screenMargin, Math.min(tooltipX, screenWidth - 220.0f - screenMargin));
        float visibleY = y() - this.scrollOffset;
        float tooltipVisibleY = Math.max(screenMargin,
                Math.min(visibleY, screenHeight - tooltipHeight - screenMargin));
        float tooltipY = tooltipVisibleY + this.scrollOffset;
        colors.push();
        colors.alpha(progress);
        MenuStyle.drawTooltip(drawCtx, tooltipX, tooltipY, 220.0f, tooltipHeight, 8.0f);
        drawCtx.text(this.font, this.name, 12, tooltipX + 10.0f,
                tooltipY + 10.0f,
                colors.computeColor(palette.text().tone(100).argb()));
        drawCtx.text(this.descriptionFont, this.wrappedModuleDescription, 11,
                tooltipX + 10.0f,
                tooltipY + 10.0f + titleHeight + 5.0f,
                colors.computeColor(palette.text().tone(100).argb()));
        colors.pop();
    }

    private void updateModuleDescription(DrawCtx drawCtx) {
        String description = moduleDescription();
        float availableWidth = 220.0f - 10.0f * 2.0f;
        if (description.equals(this.cachedModuleDescription)
                && Math.abs(availableWidth - this.cachedDescriptionWidth) < 0.01f) {
            return;
        }
        this.wrappedModuleDescription = StringUtil.formatTextToFitWidth(
                description, availableWidth, this.descriptionFont, 11);
        this.moduleDescriptionHeight = this.descriptionFont.getHeightWithLineBreaks(
                this.wrappedModuleDescription, 11);
        this.cachedModuleDescription = description;
        this.cachedDescriptionWidth = availableWidth;
    }

    private String moduleDescription() {
        String known = knownModuleDescription();
        if (known != null) {
            return known;
        }
        return switch (this.module.getModuleTab()) {
            case INTERFACE -> "Отображает и настраивает элемент интерфейса «" + this.name + "».";
            case VISUALS -> "Настраивает визуальный эффект «" + this.name + "».";
            case PLAYER -> "Настраивает функцию игрока «" + this.name + "».";
            case ALPHA_PLAYER -> "Функция игрока для AlphaSmp.";
            case MISC -> "Настраивает вспомогательную функцию «" + this.name + "».";
            case null -> "Настройки модуля.";
        };
    }

    private String knownModuleDescription() {
        return switch (this.name) {
            case "Target ESP" -> "Показывает выбранный эффект вокруг текущей цели.";
            case "SeeInvisibles" -> "Показывает круг у ног невидимых игроков.";
            case "HitColor" -> "Меняет красный цвет сущности при ударе на свой.";
            case "ProjectilePrediction" -> "Показывает траекторию и место падения снаряда.";
            case "Ambience" -> "Изменяет время суток и включает ночное зрение.";
            case "Removals" -> "Скрывает выбранные экранные и мировые эффекты.";
            case "PopTweaks" -> "Меняет анимацию срабатывания тотема.";
            case "HitParticles" -> "Создаёт частицы при ударе по сущности.";
            case "WorldParticles" -> "Создаёт частицы вокруг игрока в мире.";
            case "KillEffects" -> "Воспроизводит выбранный визуальный эффект после убийства.";
            case "JumpCircle" -> "Рисует круг при прыжке или приземлении.";
            case "CustomTags" -> "Настраивает ванильные теги игроков без отображения сквозь блоки.";
            case "DiscordRpc" -> "Показывает в профиле Discord, что ты играешь с RavenVisuals.";
            case "Crosshair" -> "Заменяет стандартный прицел на настраиваемый.";
            case "ChinaHat" -> "Рисует шляпу на голове игрока.";
            case "CustomEnchantColor" -> "Меняет цвет блеска зачарованных предметов и брони.";
            case "CustomHitBox" -> "Показывает настраиваемые хитбоксы сущностей вместо ванильных.";
            case "AspectRatio" -> "Позволяет изменить соотношение сторон изображения.";
            case "ArmTweaks" -> "Меняет положение руки и анимацию удара.";
            case "2DItems" -> "Рисует лежащие предметы плоскими спрайтами.";
            case "Trails" -> "Оставляет след за движущимся игроком.";
            case "ClientUsers" -> "Добавляет значок Raven к никам пользователей клиента.";
            case "MotionBlur" -> "Добавляет размытие изображения при движении камеры.";
            case "LeaveTracker" -> "Оставляет полупрозрачную модель на месте выхода игрока.";
            case "PartyHelper" -> "Показывает стрелки к участникам Party и метку выбранной цели.";
            case "Pets" -> "Добавляет локального питомца: муху или ворону с выбором окраса и глаз.";
            case "HitSounds" -> "Воспроизводит выбранный звук при попадании.";
            case "FreeLook" -> "Позволяет осматриваться без изменения направления движения.";
            case "DeathCoordinates" -> "Сохраняет и показывает координаты последней смерти.";
            case "ItemScroller" -> "Упрощает быстрое перемещение предметов колёсиком мыши.";
            case "AutoRespawn" -> "Автоматически возвращает игрока после смерти.";
            case "AutoLeave" -> "Выходит с сервера или в хаб, когда игрок подходит слишком близко.";
            case "FakePlayer" -> "Создаёт локального игрока для проверки визуальных эффектов.";
            case "Sprint" -> "Управляет автоматическим бегом и его условиями.";
            case "AutoJump" -> "Автоматически выполняет прыжок при движении.";
            case "AutoAuth" -> "Автоматизирует авторизацию на сохранённых серверах.";
            case "ExpTweaks" -> "Бросает пузырьки опыта по бинду и убирает задержку между бросками.";
            case "BetterChat" -> "Добавляет клиентские улучшения игрового чата.";
            case "NameProtect" -> "Заменяет выбранные имена в интерфейсе клиента.";
            case "ClickFriend" -> "Позволяет быстро добавлять игрока в список друзей.";
            case "ClanInvest" -> "Отслеживает валюту и вкладывает заданную долю в клан.";
            case "PvP Safe" -> "Блокирует опасные команды, например /spawn и /hub.";
            case "AntiRelog" -> "Показывает 30-секундный таймер PvP после обмена ударами.";
            case "AutoSwap" -> "По клавише меняет местами выбранные предметы.";
            case "ElytraSwap" -> "По клавише меняет нагрудник и элитры.";
            case "LockSlot" -> "Защищает выбранные слоты от выбрасывания предметов.";
            case "FT Helper" -> "Показывает зоны особых предметов и добавляет метки событий FunTime.";
            case "HW Helper" -> "Показывает зону действия трапок HolyWorld.";
            case "MineHelper" -> "Показывает время до шахты и помогает с инвентарём.";
            case "NbtGetter" -> "Копирует NBT выбранного предмета по клавише.";
            case "TP Accept" -> "Упрощает принятие входящих запросов телепортации.";
            case "TapeMouse" -> "Удерживает выбранную кнопку мыши программно.";
            case "Sounds" -> "Настраивает дополнительные звуки интерфейса клиента.";
            case "Watermark" -> "Показывает информацию клиента и управляет размером HUD.";
            case "MediaPlayer" -> "Показывает текущий трек в HUD.";
            case "Gif" -> "Показывает выбранную GIF-анимацию в HUD.";
            case "PotionList" -> "Показывает активные эффекты и оставшееся время.";
            case "Target HUD" -> "Показывает информацию о сущности под прицелом.";
            case "Hotkeys" -> "Показывает активные модули и назначенные клавиши.";
            case "Cooldowns" -> "Показывает активные перезарядки предметов.";
            case "Notifications" -> "Показывает уведомления клиента поверх игры.";
            case "Coords" -> "Показывает текущие координаты и скорость игрока.";
            case "Inventory HUD" -> "Показывает содержимое основного инвентаря.";
            case "Equipment HUD" -> "Показывает экипировку и оставшуюся прочность.";
            default -> null;
        };
    }

    public Module module() {
        return this.module;
    }

    public void sectionEdges(boolean first, boolean last) {
        this.firstInSection = first;
        this.lastInSection = last;
    }

    public String[] aliases() {
        return this.aliases;
    }

    public String name() {
        return this.name;
    }

    public HighlightAnimation highlightAnimation() {
        return this.highlightAnimation;
    }
}
