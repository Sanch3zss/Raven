package sanch3zss.raven.ui.menu.header;

import sanch3zss.raven.render.engine.DrawCtx;
import java.util.*;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.component.layout.ScrollArea;
import sanch3zss.raven.ui.hud.*;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.card.ConfigCard;
import sanch3zss.raven.ui.menu.layout.*;
import sanch3zss.raven.ui.menu.window.*;
import sanch3zss.raven.util.math.interpolation.Easings;

public class MenuHeaderContainer extends WidgetContainer {
    private final List<MenuTabElement> tabs = Raven.INSTANCE.tabsController().tabElements();
    private final List<MenuTabElement> mainTabs = Raven.INSTANCE.tabsController().mainTabElements();
    private final List<MenuTabElement> holyWorldTabs = Raven.INSTANCE.tabsController().holyWorldTabElements();
    private final List<MenuTabElement> otherTabs = Raven.INSTANCE.tabsController().otherTabElements();
    private final List<MenuTabElement> settingsTabs = Raven.INSTANCE.tabsController().settingsTabElements();
    private final MsdfFont textFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont titleFont = Fonts.INTER_BOLD.get();
    private final MsdfFont iconFont = Fonts.CLICK_GUI.get();
    private final MsdfFont clientIconFont = Fonts.CLIENT_ICON.get();
    private final ClickableBehavior search = new ClickableBehavior().clickCallback(() -> Raven.INSTANCE.menuWindow().searchContainer().invert());
    private final ToggleAnimator configMenuAnimation = new ToggleAnimator(280, Easings.EASE_IN_OUT_QUAD);
    private final ToggleAnimator alphaSmpAnimation = new ToggleAnimator(240, Easings.EASE_IN_OUT_CUBIC);
    private final ScrollArea sidebarScroll = new ScrollArea();
    private final ScrollArea configScroll = new ScrollArea();
    private final ClickableBehavior configs = new ClickableBehavior().clickCallback(this::toggleConfigMenu);
    private final ClickableBehavior createConfig = new ClickableBehavior().clickCallback(this::createConfig);
    private final TextInputField configNameField = new TextInputField(this.textFont, 13).maxLength(30);
    private final List<ConfigEntryControls> configEntries = new ArrayList<>();
    private boolean configMenuOpen;
    private final float[] selection = {Float.NaN, Float.NaN, Float.NaN, Float.NaN};
    private final float[] selectionVelocity = new float[4];
    private long selectionTime;

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colors = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        int primary = colors.computeColor(palette.text().tone(100).argb());
        int secondary = colors.computeColor(palette.text().tone(100).argb());
        int accent = colors.computeColor(palette.accent().argb());
        int labelColor = colors.computeColor(palette.text().tone(500).argb());

        int logoSize = 35;
        GlyphPlacement logo = centeredGlyph(drawCtx, this.clientIconFont, "a", logoSize,
                x(), y(), MenuStyle.SIDEBAR_WIDTH, MenuStyle.HEADER_HEIGHT);
        drawCtx.text(this.clientIconFont, "a", logoSize, logo.drawX(), logo.drawY(), accent);
        drawDivider(drawCtx, x() + 12.0f, y() + MenuStyle.HEADER_HEIGHT - 1.0f,
                MenuStyle.SIDEBAR_WIDTH - 24.0f, palette, colors);

        float sidebarTop = y() + MenuStyle.SIDEBAR_CONTENT_TOP;
        float sidebarHeight = MenuWindow.MENU_HEIGHT - MenuStyle.SIDEBAR_CONTENT_TOP - MenuStyle.SIDEBAR_CONTENT_BOTTOM;
        float headerGap = MenuStyle.SIDEBAR_CONTENT_TOP - MenuStyle.HEADER_HEIGHT;
        this.sidebarScroll.beginArea(drawCtx, x() + 5.0f, sidebarTop - headerGap,
                MenuStyle.SIDEBAR_WIDTH - 10.0f, sidebarHeight + headerGap);
        drawSelection(drawCtx);
        float modulesLabelY = sidebarTop;
        drawSidebarLabel(drawCtx, "Main", modulesLabelY, labelColor);
        for (MenuTabElement tab : this.mainTabs) {
            tab.drawExpanded(drawCtx);
        }
        float holyWorldLabelY = this.holyWorldTabs.getFirst().y()
                - MenuStyle.TAB_LABEL_HEIGHT - MenuStyle.CONTENT_GAP;
        drawSidebarLabel(drawCtx, "HolyWorld", holyWorldLabelY, labelColor);
        this.holyWorldTabs.getFirst().drawExpanded(drawCtx);
        if (alphaSmpChildrenHeight() * this.alphaSmpAnimation.smoothAnimation() > 0.01f) {
            beginChildrenScissor(drawCtx);
            colors.push();
            colors.alpha(this.alphaSmpAnimation.smoothAnimation());
            for (int i = 1; i < this.holyWorldTabs.size(); i++) {
                this.holyWorldTabs.get(i).drawExpanded(drawCtx);
            }
            colors.pop();
            drawCtx.drawEngine().endScissor();
        }
        if (!this.otherTabs.isEmpty()) {
            float otherLabelY = this.otherTabs.getFirst().y()
                    - MenuStyle.TAB_LABEL_HEIGHT - MenuStyle.CONTENT_GAP;
            drawSidebarLabel(drawCtx, "FunTime", otherLabelY, labelColor);
            for (MenuTabElement tab : this.otherTabs) {
                tab.drawExpanded(drawCtx);
            }
        }
        float settingsLabelY = this.settingsTabs.getFirst().y()
                - MenuStyle.TAB_LABEL_HEIGHT - MenuStyle.CONTENT_GAP;
        drawSidebarLabel(drawCtx, "Settings", settingsLabelY, labelColor);
        for (MenuTabElement tab : this.settingsTabs) {
            tab.drawExpanded(drawCtx);
        }
        this.sidebarScroll.endArea(drawCtx, sidebarContentHeight() + headerGap);
        drawScrollHint(drawCtx, x() + 5.0f, sidebarTop, MenuStyle.SIDEBAR_WIDTH - 10.0f, sidebarHeight);

        float workspaceX = x() + MenuStyle.WORKSPACE_X;
        float searchX = x() + MenuWindow.MENU_WIDTH - 40.0f;
        drawHeaderButton(drawCtx, searchX, y() + 10.0f, 30.0f, "b", null, this.search, primary, accent);
        String configName = Raven.INSTANCE.configManager().activeConfigName();
        float configWidth = Math.max(MenuStyle.CONFIG_BUTTON_WIDTH,
                drawCtx.textWidthPhysical(this.textFont, configName, 12) + 75.0f);
        drawConfigButton(drawCtx, workspaceX + 10.0f, y() + 10.0f, configWidth, configName, secondary, accent);

        drawDivider(drawCtx, workspaceX + 12.0f, y() + MenuStyle.HEADER_HEIGHT - 1.0f,
                MenuWindow.MENU_WIDTH - MenuStyle.WORKSPACE_X - 24.0f, palette, colors);
    }

    public void renderMenus(DrawCtx drawCtx) {
        ColorStack colors = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        int primary = colors.computeColor(palette.text().tone(100).argb());
        int secondary = colors.computeColor(palette.text().tone(100).argb());
        int accent = colors.computeColor(palette.accent().argb());
        drawConfigMenu(drawCtx, x() + MenuStyle.WORKSPACE_X + 10.0f,
                y() + MenuStyle.HEADER_HEIGHT - 4.0f,
                primary, secondary, accent);
    }

    private void drawSelection(DrawCtx drawCtx) {
        MenuTabElement current = Raven.INSTANCE.tabsController().current();
        if (current == null || !this.tabs.contains(current)) {
            return;
        }
        boolean nested = this.holyWorldTabs.indexOf(current) > 0;
        if (nested && this.alphaSmpAnimation.isZero()) {
            return;
        }
        float[] target = {current.x() + 1.0f - x(), current.y() - y(), current.expandedWidth() - 2.0f, current.expandedHeight()};
        long now = System.nanoTime();
        float delta = this.selectionTime == 0L ? 1.0f : Math.min(0.05f, (now - this.selectionTime) / 1.0E9f);
        this.selectionTime = now;
        for (int i = 0; i < target.length; i++) {
            if (Float.isNaN(this.selection[i]) || delta >= 1.0f) {
                this.selection[i] = target[i];
                this.selectionVelocity[i] = 0.0f;
                continue;
            }
            float acceleration = 320.0f * (target[i] - this.selection[i]) - 24.0f * this.selectionVelocity[i];
            this.selectionVelocity[i] += acceleration * delta;
            this.selection[i] += this.selectionVelocity[i] * delta;
        }
        if (!nested) {
            MenuStyle.drawSidebarTab(drawCtx, x() + this.selection[0], y() + this.selection[1], this.selection[2],
                    this.selection[3], 0.0f, 1.0f);
            return;
        }
        beginChildrenScissor(drawCtx);
        drawCtx.colorStack().push();
        drawCtx.colorStack().alpha(this.alphaSmpAnimation.smoothAnimation());
        MenuStyle.drawSidebarTab(drawCtx, x() + this.selection[0], y() + this.selection[1], this.selection[2],
                this.selection[3], 0.0f, 1.0f);
        drawCtx.colorStack().pop();
        drawCtx.drawEngine().endScissor();
    }

    private void beginChildrenScissor(DrawCtx drawCtx) {
        MenuTabElement alphaSmp = this.holyWorldTabs.getFirst();
        float childrenTop = alphaSmp.y() + alphaSmp.expandedHeight() + MenuStyle.CONTENT_GAP;
        drawCtx.drawEngine().beginScissor(drawCtx.matrixStack().last().pose(),
                drawCtx.layoutContext().toPhysical(x() + 5.0f),
                drawCtx.layoutContext().toPhysical(childrenTop),
                drawCtx.layoutContext().toPhysical(MenuStyle.SIDEBAR_WIDTH - 10.0f),
                drawCtx.layoutContext().toPhysical(alphaSmpChildrenHeight() * this.alphaSmpAnimation.smoothAnimation()));
    }

    private void drawSidebarLabel(DrawCtx drawCtx, String label, float labelY, int color) {
        drawCtx.text(this.textFont, label, 11, x() + 16.0f,
                centeredTextY(this.textFont, 11, labelY, MenuStyle.TAB_LABEL_HEIGHT), color);
    }

    private void drawDivider(DrawCtx drawCtx, float lineX, float lineY, float lineWidth,
                             ThemePalette palette, ColorStack colors) {
        drawCtx.hairline(lineX, lineY, lineWidth, MenuStyle.outline(colors));
    }

    private void drawConfigButton(DrawCtx drawCtx, float buttonX, float buttonY, float buttonWidth,
                                  String configName, int idleColor, int accentColor) {
        ColorStack colors = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        int foreground = colors.interpolate(idleColor, accentColor,
                Math.max(this.configs.hoverAnimation().smoothAnimation(), this.configMenuAnimation.smoothAnimation()));
        MenuStyle.drawModulePanel(drawCtx, buttonX, buttonY, buttonWidth, 30.0f, MenuStyle.SECTION_RADIUS);
        drawCtx.fillRoundedRect(buttonX, buttonY, buttonWidth, 30.0f, MenuStyle.SECTION_RADIUS,
                MenuStyle.hoverTint(colors, 0.07f * this.configs.hoverAnimation().smoothAnimation()));
        drawCtx.text(this.iconFont, "h", 20,
                centeredTextX(drawCtx, this.iconFont, "h", 20, buttonX, 30.0f),
                centeredTextY(this.iconFont, 20, buttonY, 35.0f), foreground);
        drawCtx.verticalHairline(buttonX + 30.0f, buttonY + 1.0f, 28.0f, MenuStyle.outline(colors));
        float textWidth = drawCtx.textWidthPhysical(this.textFont, configName, 12);
        drawCtx.text(this.textFont, configName, 12, buttonX + 31.0f + (buttonWidth - 31.0f - textWidth) / 2.0f,
                centeredTextY(this.textFont, 12, buttonY, 30.0f) - 0.5f, idleColor);
        this.configs.setDimensions(buttonX, buttonY, buttonWidth, 30.0f);
    }

    private void drawConfigMenu(DrawCtx drawCtx, float menuX, float menuY,
                                int primary, int secondary, int accent) {
        if (this.configMenuAnimation.isZero()) {
            return;
        }
        ColorStack colors = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        colors.push();
        colors.alpha(this.configMenuAnimation.smoothAnimation());
        MenuStyle.drawPopup(drawCtx, menuX, menuY, MenuStyle.CONFIG_MENU_WIDTH,
                configMenuHeight(), MenuStyle.SECTION_RADIUS);
        drawCtx.text(this.textFont, "Конфиг", 12, menuX + 10.0f,
                centeredTextY(this.textFont, 12, menuY + 6.0f, 18.0f) - 0.5f,
                colors.computeColor(primary));
        float inputX = menuX + 67.0f;
        float inputWidth = 120.0f;
        float addX = menuX + MenuStyle.CONFIG_MENU_WIDTH - 27.0f;
        float inputY = menuY + 4.0f;
        float inputHeight = 22.0f;
        MenuStyle.drawTextInput(drawCtx, this.configNameField, this.textFont, this.configNameField.text(),
                "Имя конфига ...", inputX, inputY, inputWidth, inputHeight);
        float addHover = this.createConfig.hoverAnimation().smoothAnimation();
        MenuStyle.drawCenteredIcon(drawCtx, this.iconFont, "f", 13,
                addX, menuY + 6.0f, 20.0f, 18.0f,
                colors.interpolate(colors.computeColor(palette.accent().argb(), 0.68f), accent, addHover));
        this.createConfig.setDimensions(addX, menuY + 6.0f, 20.0f, 18.0f);
        MenuStyle.drawDivider(drawCtx, menuX + 8.0f, menuY + 30.0f,
                MenuStyle.CONFIG_MENU_WIDTH - 16.0f);
        List<ConfigCard> cards = configCards();
        syncConfigEntries(cards);
        float listY = menuY + 35.0f;
        float listHeight = configMenuHeight() - 39.0f;
        this.configScroll.beginArea(drawCtx, menuX + 4.0f, listY,
                MenuStyle.CONFIG_MENU_WIDTH - 8.0f, listHeight);
        for (int i = 0; i < cards.size(); i++) {
            ConfigCard card = cards.get(i);
            ConfigEntryControls controls = this.configEntries.get(i);
            float rowY = listY + i * 32.0f;
            float loadHover = controls.load.hoverAnimation().smoothAnimation();
            float deleteHover = controls.delete.hoverAnimation().smoothAnimation();
            float rowX = menuX + 5.0f;
            float rowWidth = MenuStyle.CONFIG_MENU_WIDTH - 10.0f;
            drawCtx.fillRoundedRect(rowX, rowY, rowWidth, 28.0f, 6.0f,
                    MenuStyle.hoverTint(colors, 0.06f));
            float loadX = rowX + rowWidth - 22.0f;
            float deleteX = loadX - 20.0f;
            float nameX = rowX + 8.0f;
            String visibleName = truncateToWidth(drawCtx, card.configContext().name(),
                    Math.max(10.0f, deleteX - nameX - 5.0f), 11);
            drawCtx.text(this.textFont, visibleName, 11, nameX,
                     centeredTextY(this.textFont, 11, rowY, 28.0f),
                     colors.computeColor(primary));
            MenuStyle.drawCenteredIcon(drawCtx, this.iconFont, "d", 13,
                    deleteX, rowY + 4.0f, 20.0f, 20.0f,
                    colors.interpolate(secondary, colors.computeColor(palette.error().tone(500).argb()), deleteHover));
            MenuStyle.drawCenteredIcon(drawCtx, this.iconFont, "e", 13,
                    loadX, rowY + 4.0f, 20.0f, 20.0f,
                    colors.interpolate(secondary, accent, loadHover));
            controls.row.setDimensions(rowX, rowY, rowWidth, 28.0f);
            controls.load.setDimensions(loadX, rowY, 20.0f, 28.0f);
            controls.delete.setDimensions(deleteX, rowY, 20.0f, 28.0f);
        }
        this.configScroll.endArea(drawCtx, cards.size() * 32.0f);
        colors.pop();
    }

    public boolean hasOpenGlassOverlay() {
        return !this.configMenuAnimation.isZero();
    }

    private void toggleConfigMenu() {
        this.configMenuOpen = !this.configMenuOpen;
        this.configMenuAnimation.state(this.configMenuOpen);
        if (!this.configMenuOpen) {
            this.configNameField.focused(false);
        }
    }

    private void createConfig() {
        String name = this.configNameField.text().trim();
        if (name.isEmpty()) {
            this.configNameField.focused(true);
            return;
        }
        ConfigTabLayout layout = configLayout();
        if (layout != null && layout.createConfig(name)) {
            this.configNameField.clearText();
        }
    }

    private ConfigTabLayout configLayout() {
        return Raven.INSTANCE.tabsController().config().renderStrategy() instanceof ConfigTabLayout layout
                ? layout : null;
    }

    private List<ConfigCard> configCards() {
        Raven.INSTANCE.tabsController().config().applyPendingFramesUpdate();
        List<ConfigCard> result = new ArrayList<>();
        for (AbstractFrame frame : Raven.INSTANCE.tabsController().config().frames()) {
            if (frame instanceof ConfigCard card) {
                result.add(card);
            }
        }
        return result;
    }

    private void syncConfigEntries(List<ConfigCard> cards) {
        boolean same = this.configEntries.size() == cards.size();
        if (same) {
            for (int i = 0; i < cards.size(); i++) {
                if (!this.configEntries.get(i).id.equals(cards.get(i).configContext().cloudId())) {
                    same = false;
                    break;
                }
            }
        }
        if (same) {
            return;
        }
        this.configEntries.clear();
        for (ConfigCard card : cards) {
            String id = card.configContext().cloudId();
            ClickableBehavior load = new ClickableBehavior().clickCallback(() -> {
                ConfigTabLayout layout = configLayout();
                if (layout != null) {
                    layout.applyConfigById(id);
                }
            });
            ClickableBehavior delete = new ClickableBehavior().clickCallback(() -> {
                ConfigTabLayout layout = configLayout();
                if (layout != null) {
                    layout.deleteConfig(id);
                }
            });
            ClickableBehavior row = new ClickableBehavior().clickCallback(load.clickAction);
            this.configEntries.add(new ConfigEntryControls(id, row, load, delete));
        }
    }

    private void drawHeaderButton(DrawCtx drawCtx, float buttonX, float buttonY, float buttonWidth,
                                  String glyph, String label, ClickableBehavior clickable,
                                  int idleColor, int accentColor) {
        ColorStack colors = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        float hover = clickable.hoverAnimation().smoothAnimation();
        int foreground = colors.interpolate(idleColor, accentColor, hover);
        if (label != null) {
            int background = colors.interpolate(
                    colors.computeColor(0.25f, 13, 17, 22),
                    colors.computeColor(0.45f, 13, 17, 22), hover);
            drawCtx.fillRoundedRect(buttonX, buttonY, buttonWidth, 30.0f, 4.0f, background);
        }
        float iconX = buttonX + 10.0f;
        if (label == null) {
            drawCtx.fillRoundedRect(buttonX, buttonY, buttonWidth, 30.0f, MenuStyle.SECTION_RADIUS,
                    colors.interpolate(MenuStyle.control(colors), MenuStyle.controlHover(colors), hover));
        }
        if (label == null) {
            MenuStyle.drawCenteredIcon(drawCtx, this.iconFont, glyph, 17,
                    buttonX, buttonY, buttonWidth, 30.0f, foreground);
        } else {
            drawCtx.text(this.iconFont, glyph, 16, iconX,
                    centeredTextY(this.iconFont, 16, buttonY, 30.0f), foreground);
        }
        if (label != null) {
            drawCtx.text(this.textFont, label, 12, iconX + 24.0f, centeredTextY(this.textFont, 12, buttonY, 30.0f),
                    colors.computeColor(palette.text().tone(100).argb()));
        }
        clickable.setDimensions(buttonX, buttonY, buttonWidth, 30.0f);
    }

    private static float centeredTextY(MsdfFont font, float size, float top, float height) {
        return top + (height - font.getHeight(size)) / 2.0f;
    }

    private static float centeredTextX(DrawCtx drawCtx, MsdfFont font, String text, int size,
                                       float left, float width) {
        return left + (width - drawCtx.textWidthPhysical(font, text, size)) / 2.0f;
    }

    private static GlyphPlacement centeredGlyph(DrawCtx drawCtx, MsdfFont font, String glyph,
                                                 int size, float x, float y,
                                                 float width, float height) {
        int glyphIndex = glyph == null || glyph.isEmpty()
                ? -1 : font.glyphIndex(glyph.codePointAt(0));
        if (glyphIndex < 0) {
            float glyphWidth = drawCtx.textWidthPhysical(font, glyph, size);
            float glyphHeight = font.getHeight(size);
            float drawX = x + (width - glyphWidth) / 2.0f;
            float drawY = y + (height - glyphHeight) / 2.0f;
            return new GlyphPlacement(drawX, drawY, drawX, drawY, glyphWidth, glyphHeight);
        }

        float visibleWidth = font.planeWidth[glyphIndex] * size;
        float visibleHeight = font.planeHeight[glyphIndex] * size;
        float ascent = font.metrics().ascent() != 0.0f
                ? font.metrics().ascent() : font.metrics().lineHeight();
        float visibleTopOffset = (ascent - font.bearingY[glyphIndex]) * size;
        float visibleX = x + (width - visibleWidth) / 2.0f;
        float visibleY = y + (height - visibleHeight) / 2.0f;
        return new GlyphPlacement(visibleX, visibleY - visibleTopOffset,
                visibleX, visibleY, visibleWidth, visibleHeight);
    }

    private record GlyphPlacement(float drawX, float drawY, float visibleX, float visibleY,
                                  float visibleWidth, float visibleHeight) {
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(MenuWindow.MENU_WIDTH, MenuWindow.MENU_HEIGHT);
        float tabY = y() + MenuStyle.SIDEBAR_CONTENT_TOP + MenuStyle.TAB_LABEL_HEIGHT + MenuStyle.CONTENT_GAP;
        for (MenuTabElement tab : this.mainTabs) {
            tab.layout(layoutScaleContext);
            tab.setPosition(x() + 10.0f, tabY);
            tabY += MenuStyle.TAB_HEIGHT + MenuStyle.CONTENT_GAP;
        }
        tabY += MenuStyle.TAB_LABEL_HEIGHT + MenuStyle.CONTENT_GAP;
        MenuTabElement alphaSmp = this.holyWorldTabs.getFirst();
        alphaSmp.layout(layoutScaleContext);
        alphaSmp.setPosition(x() + 10.0f, tabY);
        tabY += alphaSmp.expandedHeight() + MenuStyle.CONTENT_GAP;
        float childY = tabY;
        for (int i = 1; i < this.holyWorldTabs.size(); i++) {
            MenuTabElement tab = this.holyWorldTabs.get(i);
            tab.layout(layoutScaleContext);
            tab.setPosition(x() + 21.0f, childY);
            childY += tab.expandedHeight() + MenuStyle.CONTENT_GAP;
        }
        tabY += alphaSmpChildrenHeight() * this.alphaSmpAnimation.smoothAnimation();
        tabY += MenuStyle.TAB_LABEL_HEIGHT + MenuStyle.CONTENT_GAP;
        for (MenuTabElement tab : this.otherTabs) {
            tab.layout(layoutScaleContext);
            tab.setPosition(x() + 10.0f, tabY);
            tabY += MenuStyle.TAB_HEIGHT + MenuStyle.CONTENT_GAP;
        }
        tabY += MenuStyle.TAB_LABEL_HEIGHT + MenuStyle.CONTENT_GAP;
        for (MenuTabElement tab : this.settingsTabs) {
            tab.layout(layoutScaleContext);
            tab.setPosition(x() + 10.0f, tabY);
            tabY += MenuStyle.TAB_HEIGHT + MenuStyle.CONTENT_GAP;
        }
        float menuX = x() + MenuStyle.WORKSPACE_X + 10.0f;
        float inputX = menuX + 67.0f;
        float inputY = y() + MenuStyle.HEADER_HEIGHT;
        this.configNameField.listen(inputX, inputY, 120.0f, 22.0f,
                108.0f, inputX + 6.0f, layoutScaleContext.scaleFactor());
        this.sidebarScroll.layout(layoutScaleContext);
        this.configScroll.layout(layoutScaleContext);
        super.layout(layoutScaleContext);
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        boolean handled = consumed;
        InputEvent event = context.inputEvent();
        boolean pointerEvent = event instanceof MouseButtonInput
                || event instanceof CursorMoveInput || event instanceof ScrollInput;
        float configMenuX = x() + MenuStyle.WORKSPACE_X + 10.0f;
        float configMenuY = y() + MenuStyle.HEADER_HEIGHT - 4.0f;
        boolean insideConfigSurface = !this.configMenuAnimation.isZero()
                && context.inArea(configMenuX, configMenuY,
                MenuStyle.CONFIG_MENU_WIDTH, configMenuHeight());
        boolean dismissedPopup = false;
        if (this.configMenuOpen && event instanceof MouseButtonInput mouse
                && mouse.action().press() && mouse.button() == 0) {
            boolean insideMenu = context.inArea(configMenuX, configMenuY,
                    MenuStyle.CONFIG_MENU_WIDTH, configMenuHeight());
            boolean insideButton = context.inArea(this.configs.ownerX(), this.configs.ownerY(),
                    this.configs.ownerW(), this.configs.ownerH());
            if (!insideMenu && !insideButton) {
                this.configMenuOpen = false;
                this.configMenuAnimation.state(false);
                this.configNameField.focused(false);
                dismissedPopup = true;
            }
        }
        handled |= dismissedPopup;
        handled |= this.search.handleInput(context, handled);
        handled |= this.configs.handleInput(context, handled);
        if (!this.configMenuAnimation.isZero()) {
            handled |= this.createConfig.handleInput(context, handled);
            InputEventContext configContext = context.withMouseOffset(0.0f, this.configScroll.scrollY());
            for (int i = this.configEntries.size() - 1; i >= 0; i--) {
                ConfigEntryControls entry = this.configEntries.get(i);
                handled |= entry.delete.handleInput(configContext, handled);
                handled |= entry.load.handleInput(configContext, handled);
                handled |= entry.row.handleInput(configContext, handled);
            }
            handled |= this.configNameField.handleInput(context, handled);
            handled |= this.configScroll.handleInput(context, handled);
        }
        if (pointerEvent && insideConfigSurface) {
            handled = true;
        }
        InputEventContext sidebarContext = context.withMouseOffset(0.0f, this.sidebarScroll.scrollY());
        for (int i = this.tabs.size() - 1; i >= 0; i--) {
            MenuTabElement tab = this.tabs.get(i);
            if (!this.alphaSmpAnimation.isOne()
                    && (tab == Raven.INSTANCE.tabsController().worldMap
                    || tab == Raven.INSTANCE.tabsController().alphaPlayer)) {
                continue;
            }
            handled |= tab.handleInput(sidebarContext, handled);
        }
        handled |= this.sidebarScroll.handleInput(context, handled);
        return handled;
    }

    @Override
    public void animation(WeightedEngine engine) {
        this.search.animate(engine);
        this.configs.animate(engine);
        this.createConfig.animate(engine);
        this.configNameField.animate(engine);
        for (ConfigEntryControls entry : this.configEntries) {
            entry.row.animate(engine);
            entry.load.animate(engine);
            entry.delete.animate(engine);
        }
        this.configMenuAnimation.animate(engine);
        this.alphaSmpAnimation.state(Raven.INSTANCE.tabsController().alphaSmpExpanded());
        this.alphaSmpAnimation.animate(engine);
        this.sidebarScroll.animation(engine);
        this.configScroll.animation(engine);
        for (MenuTabElement tab : this.tabs) {
            tab.animation(engine);
        }
        super.animation(engine);
    }

    @Override
    public void onMenuDrag(boolean dragging) {
        this.tabs.forEach(tab -> tab.onMenuDrag(dragging));
        super.onMenuDrag(dragging);
    }

    @Override
    public void handleClose() {
        this.sidebarScroll.scrollTo(0.0f);
        Raven.INSTANCE.tabsController().worldMapLayout.close();
        Raven.INSTANCE.tabsController().closeSettingsPages();
        this.configScroll.scrollTo(0.0f);
        this.configMenuOpen = false;
        this.configMenuAnimation.state(false);
        this.configNameField.focused(false);
        this.tabs.forEach(MenuTabElement::handleClose);
        super.handleClose();
    }

    private void drawScrollHint(DrawCtx drawCtx, float x, float top, float width, float height) {
        float remaining = sidebarContentHeight() - height - this.sidebarScroll.scrollY();
        float visibility = Math.max(0.0f, Math.min(1.0f, remaining / 24.0f));
        if (visibility <= 0.01f) {
            return;
        }
        ColorStack colors = drawCtx.colorStack();
        float bottom = top + height;
        float bob = (float) Math.sin(System.currentTimeMillis() / 320.0) * 1.5f;
        var layout = drawCtx.layoutContext();
        drawCtx.drawEngine().chevron(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(x + width / 2.0f - 5.0f), layout.toPhysical(bottom - 12.0f + bob),
                layout.toPhysical(10.0f), layout.toPhysical(5.0f), layout.toPhysical(1.5f), 2,
                colors.computeColor(drawCtx.theme().palette().text().tone(100).argb(), 0.6f * visibility));
    }

    private float sidebarContentHeight() {
        MenuTabElement last = this.tabs.getLast();
        return Math.max(0.0f, last.y() + MenuStyle.TAB_HEIGHT - (y() + MenuStyle.SIDEBAR_CONTENT_TOP));
    }

    private float alphaSmpChildrenHeight() {
        float height = 0.0f;
        for (int i = 1; i < this.holyWorldTabs.size(); i++) {
            height += this.holyWorldTabs.get(i).expandedHeight() + MenuStyle.CONTENT_GAP;
        }
        return height;
    }

    private float configMenuHeight() {
        return Math.max(MenuStyle.CONFIG_MENU_HEIGHT,
                Math.min(250.0f, 39.0f + configCards().size() * 32.0f));
    }

    private String truncateToWidth(DrawCtx drawCtx, String text, float maxWidth, int size) {
        if (drawCtx.textWidthPhysical(this.textFont, text, size) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        for (int end = text.length(); end > 0; end--) {
            String candidate = text.substring(0, end) + suffix;
            if (drawCtx.textWidthPhysical(this.textFont, candidate, size) <= maxWidth) {
                return candidate;
            }
        }
        return "";
    }

    private static final class ConfigEntryControls {
        private final String id;
        private final ClickableBehavior row;
        private final ClickableBehavior load;
        private final ClickableBehavior delete;

        private ConfigEntryControls(String id, ClickableBehavior row, ClickableBehavior load,
                                    ClickableBehavior delete) {
            this.id = id;
            this.row = row;
            this.load = load;
            this.delete = delete;
        }
    }

}
