package sanch3zss.raven.ui.menu.layout;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.client.Minecraft;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.storage.CustomThemeStore;
import sanch3zss.raven.client.config.storage.CustomThemeStore.SavedTheme;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.CursorMoveInput;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.input.MouseButtonInput;
import sanch3zss.raven.client.input.ScrollInput;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.feature.module.misc.InterfaceStyle;
import sanch3zss.raven.render.animation.AnimatedFloat;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.font.MsdfFont;
import sanch3zss.raven.render.theme.ColorMode;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.render.theme.ThemePalette;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.component.layout.AbstractTabLayout;
import sanch3zss.raven.ui.component.layout.FrameElementColumn;
import sanch3zss.raven.ui.component.layout.ScrollArea;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.header.ThemeColorSetting;
import sanch3zss.raven.ui.menu.window.MenuWindow;
import sanch3zss.raven.ui.setting.element.ColorSettingElement;
import sanch3zss.raven.ui.setting.element.ModeSettingElement;
import sanch3zss.raven.util.math.interpolation.Easings;

public final class ThemesLayout extends AbstractTabLayout {
    private static final String CODE_PREFIX = "RVN:";
    private static final float CARD_HEIGHT = 64.0f;
    private static final float CARD_GAP = 10.0f;
    private static final List<ThemeEntry> PRESETS = List.of(
            preset("Raven", 0xFF6E74E3, 0xFF8186EA),
            preset("Лаванда", 0xFFB59CFF, 0xFF7FD4FF),
            preset("Мята", 0xFF3DDC97, 0xFF7FE8FF),
            preset("Закат", 0xFFFF7A59, 0xFFFFC15E),
            preset("Сакура", 0xFFFF8FB8, 0xFFFFC1E3),
            preset("Океан", 0xFF2F80ED, 0xFF56CCF2),
            preset("Лайм", 0xFFB6FF1F, 0xFF3DFF9A),
            preset("Рубин", 0xFFE0245E, 0xFFFF6B6B),
            preset("Аметист", 0xFF8E54E9, 0xFF4776E6),
            preset("Золото", 0xFFF7B733, 0xFFFC4A1A),
            preset("Лёд", 0xFFA1C4FD, 0xFFC2E9FB),
            preset("Неон", 0xFF00F5A0, 0xFF00D9F5),
            new ThemeEntry("preset:Монохром", "Монохром", 0xFFE5E5EA, 0xFF8E8E93, ColorMode.STATIC, null));
    private static final String[] FILTERS = {"Все", "Встроенные", "Мои"};

    private final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont textFont = Fonts.INTER_MEDIUM.get();
    private final MsdfFont iconFont = Fonts.CLICK_GUI.get();
    private final CustomThemeStore store = new CustomThemeStore();
    private FrameElementColumn editor;
    private final TextInputField nameField = new TextInputField(this.textFont, 13).maxLength(24);
    private final ClickableBehavior saveButton = new ClickableBehavior().clickCallback(this::saveCurrent);
    private final ClickableBehavior randomButton = new ClickableBehavior().clickCallback(this::applyRandom);
    private final ClickableBehavior importButton = new ClickableBehavior().clickCallback(this::importFromClipboard);
    private final ClickableBehavior exportButton = new ClickableBehavior().clickCallback(this::exportToClipboard);
    private final ClickableBehavior[] filterButtons = new ClickableBehavior[FILTERS.length];
    private final ClickableBehavior[] styleButtons = new ClickableBehavior[InterfaceStyle.values().length];
    private float stylePosition;
    private final ClickableBehavior[] previewTabs = new ClickableBehavior[4];
    private final ToggleAnimator previewSwitch = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    private final ClickableBehavior previewSwitchButton = new ClickableBehavior()
            .clickCallback(() -> this.previewSwitch.state(!this.previewSwitch.state()));
    private final ClickableBehavior previewButton = new ClickableBehavior().clickCallback(() -> this.previewPress = 1.0f);
    private int previewTab = 1;
    private float previewTabPosition = 1.0f;
    private float previewSlider = 0.65f;
    private final AnimatedFloat previewSliderValue = new AnimatedFloat(180, Easings.EASE_OUT_CUBIC);
    private final ToggleAnimator previewSliderHover = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    private final ToggleAnimator previewSliderPress = new ToggleAnimator(220, Easings.EASE_OUT_CUBIC);
    private boolean previewDragging;
    private float previewPress;
    private float previewSliderX;
    private float previewSliderY;
    private float previewSliderWidth;
    private final Map<String, CardControls> cards = new HashMap<>();
    private final ScrollArea scrollArea = new ScrollArea();
    private int filter;
    private float filterPosition;
    private String status = "";
    private long statusTime;
    private long transitionStart;
    private int fromFirst;
    private int fromSecond;
    private int toFirst;
    private int toSecond;

    public ThemesLayout() {
        for (int i = 0; i < FILTERS.length; i++) {
            int index = i;
            this.filterButtons[i] = new ClickableBehavior().clickCallback(() -> this.filter = index);
        }
        for (int i = 0; i < this.previewTabs.length; i++) {
            int index = i;
            this.previewTabs[i] = new ClickableBehavior().clickCallback(() -> this.previewTab = index);
        }
        for (int i = 0; i < this.styleButtons.length; i++) {
            InterfaceStyle style = InterfaceStyle.values()[i];
            this.styleButtons[i] = new ClickableBehavior().clickCallback(() -> {
                Raven.INSTANCE.interfaceStyle(style);
                Raven.INSTANCE.configManager().saveLocalConfig();
            });
        }
        this.previewSwitch.force(true);
        this.previewSliderValue.set(this.previewSlider);
    }

    private FrameElementColumn editor() {
        if (this.editor == null) {
            ThemeColorSetting themeColor = new ThemeColorSetting();
            ColorSettingElement colorElement = new ColorSettingElement(themeColor.getName(), null,
                    themeColor::firstColor, () -> 1.0f);
            colorElement.gradient(themeColor);
            colorElement.colorConsumer(themeColor::setColor);
            colorElement.commitRunnable(() -> Raven.INSTANCE.configManager().saveLocalConfig());
            ModeSettingElement<ColorMode> modeElement = new ModeSettingElement<>(
                    Translation.clearText("Тип цвета"), null, ColorMode.values());
            modeElement.currentValueSupplier(themeColor::mode);
            modeElement.onChangeCallback(mode -> {
                themeColor.setMode(mode);
                Raven.INSTANCE.configManager().saveLocalConfig();
            });
            this.editor = new FrameElementColumn(360.0f, 8.0f);
            this.editor.addFrameElement(colorElement);
            this.editor.addFrameElement(modeElement);
        }
        return this.editor;
    }

    private static ThemeEntry preset(String name, int first, int second) {
        return new ThemeEntry("preset:" + name, name, first, second, ColorMode.GRADIENT, null);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colors = drawCtx.colorStack();
        colors.push();
        int text = colors.computeColor(drawCtx.theme().palette().text().tone(100).argb());
        int secondary = colors.computeColor(drawCtx.theme().palette().text().tone(100).argb(), 0.55f);
        drawCtx.text(this.titleFont, "Темы", 14, originX() + 12.0f, originY() + 8.0f, text);
        float styleX = originX() + 64.0f;
        float styleSegment = 68.0f;
        String[] styleLabels = new String[this.styleButtons.length];
        float styleHover = 0.0f;
        for (int i = 0; i < this.styleButtons.length; i++) {
            styleLabels[i] = InterfaceStyle.values()[i].getDisplayName().effective();
            styleHover = Math.max(styleHover, this.styleButtons[i].hoverAnimation().smoothAnimation());
            this.styleButtons[i].setDimensions(styleX + i * styleSegment, originY() + 5.0f, styleSegment, 24.0f);
        }
        MenuStyle.drawSegmentedControl(drawCtx, this.titleFont, 11, styleLabels, styleX, originY() + 5.0f,
                styleSegment * styleLabels.length, 24.0f, this.stylePosition, styleHover);
        float buttonsRight = originX() + width() - 12.0f;
        drawButton(drawCtx, this.exportButton, "Экспорт", buttonsRight - 80.0f, originY() + 5.0f, 80.0f);
        drawButton(drawCtx, this.importButton, "Импорт", buttonsRight - 166.0f, originY() + 5.0f, 80.0f);
        drawButton(drawCtx, this.randomButton, "Случайная", buttonsRight - 272.0f, originY() + 5.0f, 100.0f);
        float statusAlpha = 1.0f - Math.max(0.0f, (System.currentTimeMillis() - this.statusTime - 2000L) / 500.0f);
        if (!this.status.isEmpty() && statusAlpha > 0.0f) {
            float statusLeft = styleX + styleSegment * styleLabels.length + 12.0f;
            float statusRight = buttonsRight - 282.0f;
            float statusWidth = drawCtx.textWidthPhysical(this.textFont, this.status, 11);
            MenuStyle.drawFadingText(drawCtx, this.textFont, this.status, 11,
                    Math.max(statusLeft, statusRight - statusWidth),
                    originY() + 5.0f + (24.0f - this.textFont.getHeight(11.0f)) / 2.0f,
                    Math.max(0.0f, statusRight - statusLeft),
                    colors.computeColor(drawCtx.theme().palette().text().tone(100).argb(), 0.7f * statusAlpha));
        }

        float cardX = originX() + 12.0f;
        float cardY = originY() + 36.0f;
        float cardWidth = width() - 24.0f;
        MenuStyle.drawModulePanel(drawCtx, cardX, cardY, cardWidth, 124.0f, MenuStyle.SECTION_RADIUS);
        drawPreview(drawCtx, cardX + 10.0f, cardY + 10.0f, 210.0f, 104.0f);
        editor().render(drawCtx);
        float fieldWidth = cardWidth - 242.0f - 140.0f;
        MenuStyle.drawTextInput(drawCtx, this.nameField, this.textFont, this.nameField.text(), "Название своей темы",
                cardX + 232.0f, cardY + 90.0f, fieldWidth, 24.0f);
        drawButton(drawCtx, this.saveButton, "Сохранить как свою", cardX + cardWidth - 142.0f, cardY + 90.0f, 132.0f);

        float filterY = cardY + 134.0f;
        float segmentWidth = 100.0f;
        float filterHover = 0.0f;
        for (int i = 0; i < FILTERS.length; i++) {
            filterHover = Math.max(filterHover, this.filterButtons[i].hoverAnimation().smoothAnimation());
            this.filterButtons[i].setDimensions(cardX + i * segmentWidth, filterY, segmentWidth, 24.0f);
        }
        MenuStyle.drawSegmentedControl(drawCtx, this.titleFont, 11, FILTERS, cardX, filterY,
                segmentWidth * FILTERS.length, 24.0f, this.filterPosition, filterHover);

        List<ThemeEntry> entries = visibleEntries();
        syncCards(entries);
        float gridY = gridY();
        float columnWidth = (cardWidth - CARD_GAP * 2.0f) / 3.0f;
        if (entries.isEmpty()) {
            String hint = "Своих тем пока нет — сохрани текущую или импортируй код";
            float hintWidth = drawCtx.textWidthPhysical(this.textFont, hint, 12);
            drawCtx.text(this.textFont, hint, 12, cardX + (cardWidth - hintWidth) / 2.0f, gridY + 40.0f, secondary);
        }
        this.scrollArea.beginArea(drawCtx, cardX, gridY, cardWidth, gridHeight());
        for (int i = 0; i < entries.size(); i++) {
            ThemeEntry entry = entries.get(i);
            float x = cardX + (i % 3) * (columnWidth + CARD_GAP);
            float y = gridY + (i / 3) * (CARD_HEIGHT + CARD_GAP);
            drawThemeCard(drawCtx, entry, this.cards.get(entry.id()), x, y, columnWidth);
        }
        this.scrollArea.endArea(drawCtx, ((entries.size() + 2) / 3) * (CARD_HEIGHT + CARD_GAP));
        colors.pop();
    }

    @Override
    public void renderOverlays(DrawCtx drawCtx) {
        drawCtx.colorStack().push();
        editor().renderOverlays(drawCtx);
        drawCtx.colorStack().pop();
    }

    private void drawPreview(DrawCtx drawCtx, float x, float y, float width, float height) {
        ColorStack colors = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        int accent = colors.computeColor(palette.accent().argb());
        MenuStyle.drawWorkspacePanel(drawCtx, x, y, width, height, 10.0f);
        int textTone = palette.text().tone(100).argb();
        drawCtx.fillRoundedRect(x + 6.0f, y + 6.0f, 58.0f, height - 12.0f, 6.0f, MenuStyle.hoverTint(colors, 0.04f));
        drawCtx.fillRoundedRect(x + 10.0f, y + 9.0f + this.previewTabPosition * 20.0f, 50.0f, 16.0f, 4.0f, accent);
        for (int i = 0; i < this.previewTabs.length; i++) {
            float pillY = y + 12.0f + i * 20.0f;
            float selected = Math.max(0.0f, 1.0f - Math.abs(this.previewTabPosition - i));
            float hover = this.previewTabs[i].hoverAnimation().smoothAnimation();
            int foreground = colors.interpolate(colors.computeColor(palette.accent().argb()), colors.computeColor(0xFFFFFFFF), selected);
            drawCtx.fillRoundedRect(x + 14.0f, pillY + 1.0f, 8.0f, 8.0f, 3.0f, foreground);
            drawCtx.fillRoundedRect(x + 26.0f, pillY + 3.0f, 28.0f, 4.0f, 2.0f,
                    colors.interpolate(colors.computeColor(textTone, 0.45f + 0.2f * hover), colors.computeColor(0xFFFFFFFF, 0.9f), selected));
            this.previewTabs[i].setDimensions(x + 10.0f, pillY - 3.0f, 50.0f, 16.0f);
        }
        float contentX = x + 74.0f;
        float contentWidth = width - 84.0f;
        int text = colors.computeColor(textTone);
        float switchProgress = this.previewSwitch.smoothAnimation();
        float switchHover = this.previewSwitchButton.hoverAnimation().smoothAnimation();
        float switchX = contentX + contentWidth - 30.0f;
        drawCtx.text(this.titleFont, "Переключатель", 12, contentX,
                y + 8.0f + (18.0f - this.titleFont.getHeight(12.0f)) / 2.0f, text);
        MenuStyle.drawSwitch(drawCtx, switchX, y + 8.0f, 30.0f, 18.0f, switchProgress,
                colors.interpolate(colors.computeColor(0xFF787880, 0.32f + 0.06f * switchHover), accent, switchProgress));
        this.previewSwitchButton.setDimensions(contentX, y + 8.0f, contentWidth, 18.0f);

        float sliderRowY = y + 32.0f;
        float sliderHover = this.previewSliderHover.smoothAnimation();
        String value = Math.round(this.previewSlider * 100.0f) + " %";
        float valueTextWidth = drawCtx.textWidthPhysical(this.titleFont, value, 12);
        float valueWidth = Math.max(46.0f, valueTextWidth + 16.0f);
        drawCtx.text(this.titleFont, "Слайдер", 12, contentX,
                sliderRowY + (18.0f - this.titleFont.getHeight(12.0f)) / 2.0f, text);
        MenuStyle.drawControlField(drawCtx, contentX + contentWidth - valueWidth, sliderRowY, valueWidth, 18.0f, 4.0f,
                sliderHover);
        drawCtx.text(this.titleFont, value, 12, contentX + contentWidth - valueWidth + (valueWidth - valueTextWidth) / 2.0f,
                sliderRowY + (18.0f - this.titleFont.getHeight(12.0f)) / 2.0f, accent);
        this.previewSliderX = contentX;
        this.previewSliderY = sliderRowY + 24.0f;
        this.previewSliderWidth = contentWidth;
        MenuStyle.drawSlider(drawCtx, contentX, this.previewSliderY, contentWidth, 4.0f,
                this.previewSliderValue.animatedValue(), sliderHover, this.previewSliderPress.smoothAnimation(), accent);

        float buttonY = y + height - 28.0f;
        float buttonHover = this.previewButton.hoverAnimation().smoothAnimation();
        MenuStyle.drawSettingButton(drawCtx, contentX, buttonY, contentWidth, 22.0f, 6.0f,
                Math.min(1.0f, buttonHover + this.previewPress));
        String label = "Кнопка";
        float labelWidth = drawCtx.textWidthPhysical(this.titleFont, label, 11);
        drawCtx.text(this.titleFont, label, 11, contentX + (contentWidth - labelWidth) / 2.0f,
                buttonY + (22.0f - this.titleFont.getHeight(11.0f)) / 2.0f, text);
        this.previewButton.setDimensions(contentX, buttonY, contentWidth, 22.0f);
    }

    private boolean handlePreviewInput(InputEventContext context, boolean consumed) {
        boolean handled = consumed;
        for (ClickableBehavior tab : this.previewTabs) {
            handled |= tab.handleInput(context, handled);
        }
        handled |= this.previewSwitchButton.handleInput(context, handled);
        handled |= this.previewButton.handleInput(context, handled);
        InputEvent event = context.inputEvent();
        boolean overSlider = context.inArea(this.previewSliderX, this.previewSliderY - 6.0f, this.previewSliderWidth, 16.0f);
        if (event instanceof CursorMoveInput) {
            this.previewSliderHover.state((overSlider && !handled) || this.previewDragging);
        }
        if (event instanceof MouseButtonInput mouse && mouse.button() == 0) {
            if (mouse.action().press() && overSlider && !handled) {
                this.previewDragging = true;
            } else if (mouse.action().release() && this.previewDragging) {
                this.previewDragging = false;
                return true;
            }
        }
        if (this.previewDragging && (event instanceof CursorMoveInput || event instanceof MouseButtonInput)) {
            float position = (context.logicalMousePosition().x() - this.previewSliderX) / Math.max(1.0f, this.previewSliderWidth);
            this.previewSlider = Math.round(Math.max(0.0f, Math.min(1.0f, position)) * 100.0f) / 100.0f;
            return true;
        }
        return handled;
    }

    private void drawThemeCard(DrawCtx drawCtx, ThemeEntry entry, CardControls controls, float x, float y, float width) {
        ColorStack colors = drawCtx.colorStack();
        float hover = controls.select.hoverAnimation().smoothAnimation();
        boolean active = isActive(entry);
        controls.active.state(active);
        float selected = controls.active.smoothAnimation();
        MenuStyle.drawCard(drawCtx, x, y, width, CARD_HEIGHT, 10.0f, hover);
        if (selected > 0.01f) {
            drawCtx.fillOutlinedRoundedRect(x, y, width, CARD_HEIGHT, 10.0f, 1.5f,
                    colors.computeColor(drawCtx.theme().palette().accent().argb(), selected), colors.computeColor(0, 0.0f));
        }
        int first = colors.computeColor(entry.first());
        int second = colors.computeColor(entry.mode() == ColorMode.STATIC ? entry.first() : entry.second());
        drawCtx.fillGradientRoundedRect(x + 8.0f, y + 8.0f, width - 16.0f, 24.0f, 7, first, second, second, first);
        int text = colors.computeColor(drawCtx.theme().palette().text().tone(100).argb());
        float nameLimit = width - 20.0f - (entry.saved() != null ? 26.0f : 0.0f) - (active ? 18.0f : 0.0f);
        MenuStyle.drawFadingText(drawCtx, this.titleFont, entry.name(), 12, x + 10.0f,
                y + 40.0f, nameLimit, text);
        float iconX = x + width - 10.0f;
        float nameCenterY = y + 40.0f + this.titleFont.getHeight(12.0f) / 2.0f;
        if (entry.saved() != null) {
            iconX -= 20.0f;
            float deleteHover = controls.delete.hoverAnimation().smoothAnimation();
            MenuStyle.drawCenteredIcon(drawCtx, this.iconFont, "d", 12, iconX, nameCenterY - 10.0f, 20.0f, 20.0f,
                    colors.interpolate(colors.computeColor(text, 0.45f),
                            colors.computeColor(drawCtx.theme().palette().error().tone(500).argb()), deleteHover));
            controls.delete.setDimensions(iconX, nameCenterY - 10.0f, 20.0f, 20.0f);
        }
        if (selected > 0.01f) {
            MenuStyle.drawCheckmark(drawCtx, iconX - 10.0f, nameCenterY, 9.0f + selected,
                    colors.computeColor(drawCtx.theme().palette().accent().argb(), selected));
        }
        controls.select.setDimensions(x, y, width, CARD_HEIGHT);
    }

    private void drawButton(DrawCtx drawCtx, ClickableBehavior button, String label, float x, float y, float width) {
        MenuStyle.drawSettingButton(drawCtx, x, y, width, 24.0f, 5.0f, button.hoverAnimation().smoothAnimation());
        float labelWidth = drawCtx.textWidthPhysical(this.titleFont, label, 11);
        drawCtx.text(this.titleFont, label, 11, x + (width - labelWidth) / 2.0f,
                y + (24.0f - this.titleFont.getHeight(11.0f)) / 2.0f,
                drawCtx.colorStack().computeColor(drawCtx.theme().palette().text().tone(100).argb()));
        button.setDimensions(x, y, width, 24.0f);
    }

    private List<ThemeEntry> visibleEntries() {
        return entries(this.filter);
    }

    private List<ThemeEntry> entries(int filter) {
        List<ThemeEntry> result = new ArrayList<>();
        if (filter != 2) {
            result.addAll(PRESETS);
        }
        if (filter != 1) {
            for (SavedTheme saved : this.store.themes()) {
                result.add(new ThemeEntry("custom:" + saved.name, saved.name, saved.first, saved.second,
                        parseMode(saved.mode), saved));
            }
        }
        return result;
    }

    private void syncCards(List<ThemeEntry> entries) {
        List<String> ids = new ArrayList<>();
        for (ThemeEntry entry : entries) {
            ids.add(entry.id());
            this.cards.computeIfAbsent(entry.id(), id -> {
                CardControls controls = new CardControls(
                        new ClickableBehavior().clickCallback(() -> apply(entry)),
                        new ClickableBehavior().clickCallback(() -> {
                            this.store.remove(entry.saved());
                            showStatus("Тема «" + entry.name() + "» удалена");
                        }),
                        new ToggleAnimator(260, Easings.EASE_IN_OUT_CUBIC));
                controls.active.force(isActive(entry));
                return controls;
            });
        }
        this.cards.keySet().retainAll(ids);
    }

    private boolean isActive(ThemeEntry entry) {
        ThemePalette palette = Raven.INSTANCE.theme().palette();
        int first = this.transitionStart != 0L ? this.toFirst : palette.accentStart().argb();
        int second = this.transitionStart != 0L ? this.toSecond : gradientEnd(palette);
        if (palette.colorMode != entry.mode() || (first & 0xFFFFFF) != (entry.first() & 0xFFFFFF)) {
            return false;
        }
        return entry.mode() == ColorMode.STATIC || (second & 0xFFFFFF) == (entry.second() & 0xFFFFFF);
    }

    private String activeThemeName() {
        for (ThemeEntry entry : entries(0)) {
            if (isActive(entry)) {
                return entry.name();
            }
        }
        return "Тема";
    }

    private static int gradientEnd(ThemePalette palette) {
        return palette.accentBright().argb() | 0xFF000000;
    }

    private void apply(ThemeEntry entry) {
        ThemePalette palette = Raven.INSTANCE.theme().palette();
        this.fromFirst = palette.accentStart().argb() | 0xFF000000;
        this.fromSecond = gradientEnd(palette);
        this.toFirst = entry.first() | 0xFF000000;
        this.toSecond = entry.second() | 0xFF000000;
        this.transitionStart = System.nanoTime();
        Raven.INSTANCE.updateThemeColorMode(entry.mode());
    }

    private void applyRandom() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        float hue = random.nextFloat();
        float shift = (0.06f + random.nextFloat() * 0.12f) * (random.nextBoolean() ? 1.0f : -1.0f);
        int first = Color.HSBtoRGB(hue, 0.55f + random.nextFloat() * 0.3f, 0.85f + random.nextFloat() * 0.15f);
        int second = Color.HSBtoRGB((hue + shift + 1.0f) % 1.0f, 0.45f + random.nextFloat() * 0.3f, 0.9f + random.nextFloat() * 0.1f);
        apply(new ThemeEntry("random", "Случайная", first, second, ColorMode.GRADIENT, null));
        showStatus("Нравится? Сохрани как свою");
    }

    private void saveCurrent() {
        ThemePalette palette = Raven.INSTANCE.theme().palette();
        String name = this.nameField.text().trim();
        if (name.isEmpty()) {
            name = "Моя тема " + (this.store.themes().size() + 1);
        }
        this.store.add(new SavedTheme(name, palette.accentStart().argb() | 0xFF000000, gradientEnd(palette),
                palette.colorMode.name()));
        this.nameField.clearText();
        this.nameField.focused(false);
        showStatus("Тема «" + name + "» сохранена");
    }

    private void exportToClipboard() {
        ThemePalette palette = Raven.INSTANCE.theme().palette();
        String name = this.nameField.text().trim().isEmpty() ? activeThemeName() : this.nameField.text().trim();
        String payload = name.replace("|", "") + "|" + String.format("%08X", palette.accentStart().argb() | 0xFF000000)
                + "|" + String.format("%08X", gradientEnd(palette)) + "|" + palette.colorMode.name();
        Minecraft.getInstance().keyboardHandler.setClipboard(CODE_PREFIX
                + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)));
        showStatus("Код темы скопирован");
    }

    private void importFromClipboard() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard().trim();
        if (!clipboard.startsWith(CODE_PREFIX)) {
            showStatus("В буфере нет кода темы");
            return;
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(clipboard.substring(CODE_PREFIX.length())),
                    StandardCharsets.UTF_8);
            String[] parts = payload.split("\\|");
            SavedTheme theme = new SavedTheme(parts[0], Integer.parseUnsignedInt(parts[1], 16),
                    Integer.parseUnsignedInt(parts[2], 16), parseMode(parts[3]).name());
            this.store.add(theme);
            apply(new ThemeEntry("custom:" + theme.name, theme.name, theme.first, theme.second,
                    parseMode(theme.mode), theme));
            showStatus("Тема «" + theme.name + "» добавлена");
        } catch (RuntimeException e) {
            showStatus("Код темы повреждён");
        }
    }

    private static ColorMode parseMode(String mode) {
        try {
            return ColorMode.valueOf(mode.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return ColorMode.GRADIENT;
        }
    }

    private void showStatus(String text) {
        this.status = text;
        this.statusTime = System.currentTimeMillis();
    }

    private float gridY() {
        return originY() + 36.0f + 124.0f + 44.0f;
    }

    private float gridHeight() {
        return originY() + height() - 12.0f - gridY();
    }

    @Override
    public void layout(LayoutScaleContext context) {
        float cardX = originX() + 12.0f;
        float cardY = originY() + 36.0f;
        float cardWidth = width() - 24.0f;
        editor().setWidth(cardWidth - 242.0f);
        editor().setPosition(cardX + 232.0f, cardY + 8.0f);
        editor().layout(context);
        float fieldWidth = cardWidth - 242.0f - 140.0f;
        this.nameField.listen(cardX + 232.0f, cardY + 90.0f, fieldWidth, 24.0f, fieldWidth - 12.0f,
                cardX + 238.0f, context.scaleFactor());
        this.scrollArea.layout(context);
    }

    @Override
    public void animation(WeightedEngine engine) {
        editor().animation(engine);
        this.nameField.animate(engine);
        this.saveButton.animate(engine);
        this.randomButton.animate(engine);
        this.importButton.animate(engine);
        this.exportButton.animate(engine);
        for (ClickableBehavior button : this.filterButtons) {
            button.animate(engine);
        }
        for (ClickableBehavior tab : this.previewTabs) {
            tab.animate(engine);
        }
        this.previewSwitch.animate(engine);
        this.previewSwitchButton.animate(engine);
        this.previewButton.animate(engine);
        this.previewSliderValue.destination(this.previewSlider);
        this.previewSliderValue.animate(engine);
        this.previewSliderHover.animate(engine);
        this.previewSliderPress.state(this.previewDragging);
        this.previewSliderPress.animate(engine);
        this.previewTabPosition += (this.previewTab - this.previewTabPosition) * 0.25f;
        this.previewPress *= 0.85f;
        for (CardControls controls : this.cards.values()) {
            controls.select.animate(engine);
            controls.delete.animate(engine);
            controls.active.animate(engine);
        }
        this.scrollArea.animation(engine);
        this.filterPosition += (this.filter - this.filterPosition) * 0.25f;
        for (ClickableBehavior button : this.styleButtons) {
            button.animate(engine);
        }
        this.stylePosition += (Raven.INSTANCE.interfaceStyle().ordinal() - this.stylePosition) * 0.25f;
        if (this.transitionStart != 0L) {
            float progress = Math.min(1.0f, (System.nanoTime() - this.transitionStart) / 380_000_000.0f);
            float eased = progress * progress * (3.0f - 2.0f * progress);
            Raven.INSTANCE.updateThemeGradient(ColorUtil.interpolateOklab(this.fromFirst, this.toFirst, eased),
                    ColorUtil.interpolateOklab(this.fromSecond, this.toSecond, eased));
            if (progress >= 1.0f) {
                this.transitionStart = 0L;
                Raven.INSTANCE.configManager().saveLocalConfig();
            }
        }
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        boolean handled = consumed;
        handled |= editor().handleInput(context, handled);
        handled |= handlePreviewInput(context, handled);
        handled |= this.nameField.handleInput(context, handled);
        handled |= this.saveButton.handleInput(context, handled);
        handled |= this.randomButton.handleInput(context, handled);
        handled |= this.importButton.handleInput(context, handled);
        handled |= this.exportButton.handleInput(context, handled);
        for (ClickableBehavior button : this.filterButtons) {
            handled |= button.handleInput(context, handled);
        }
        for (ClickableBehavior button : this.styleButtons) {
            handled |= button.handleInput(context, handled);
        }
        InputEvent event = context.inputEvent();
        boolean pointer = event instanceof MouseButtonInput || event instanceof CursorMoveInput
                || event instanceof ScrollInput;
        boolean outsideGrid = pointer && !context.inArea(originX() + 12.0f, gridY(), width() - 24.0f, gridHeight());
        InputEventContext gridContext = context.withMouseOffset(0.0f, this.scrollArea.scrollY());
        for (CardControls controls : new ArrayList<>(this.cards.values())) {
            handled |= controls.delete.handleInput(gridContext, handled || outsideGrid);
            handled |= controls.select.handleInput(gridContext, handled || outsideGrid);
        }
        if (!outsideGrid) {
            handled |= this.scrollArea.handleInput(context, handled);
        }
        return handled;
    }

    public void close() {
        this.nameField.focused(false);
        if (this.editor != null) {
            this.editor.handleClose();
        }
    }

    @Override
    public float getContentHeight() {
        return height();
    }

    @Override
    public void positionFrames() {
    }

    @Override
    public float width() {
        return MenuWindow.MENU_WIDTH - MenuStyle.WORKSPACE_X;
    }

    private record ThemeEntry(String id, String name, int first, int second, ColorMode mode, SavedTheme saved) {
    }

    private record CardControls(ClickableBehavior select, ClickableBehavior delete, ToggleAnimator active) {
    }
}
