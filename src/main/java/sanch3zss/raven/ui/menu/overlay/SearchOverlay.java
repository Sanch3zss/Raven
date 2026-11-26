package sanch3zss.raven.ui.menu.overlay;

import java.util.*;

import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.config.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.feature.module.ModuleCategory;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.TabsController;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.component.layout.ScrollArea;
import sanch3zss.raven.ui.hud.*;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.card.*;
import sanch3zss.raven.ui.menu.layout.MenuTabElement;
import sanch3zss.raven.ui.search.*;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.interpolation.Easings;

public class SearchOverlay extends WidgetContainer {
    public static final float smallGap = 2.0f;
    public static final float rowHeight = 26.0f;
    public static final float horizontalPadding = 10.0f;
    public static final float fontSize = 13.0f;
    public static final float groupSpacing = 12.0f;
    public static final float maxHeight = 250.0f;
    public static final float cornerRadius = 8.0f;
    public static final float epsilon = 0.01f;
    public static final int iconSize = 13;
    public static final int searchGlyphSize = 17;
    public static final float searchGlyphBox = 18.0f;
    public boolean opened;

    public final GlTexture searchIcon = new GlTexture(new ClasspathResource("/icons/menu/new/search.png")).setDimensions(iconSize, iconSize);
    public final GlTexture frameIcon = new GlTexture(new ClasspathResource("/icons/menu/new/frame.png")).setDimensions(12, 12);
    public final GlTexture multiEnumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/multienum.png")).setDimensions(12, 12);
    public final GlTexture sliderIcon = new GlTexture(new ClasspathResource("/icons/menu/new/slider.png")).setDimensions(12, 12);
    public final GlTexture enumIcon = new GlTexture(new ClasspathResource("/icons/menu/new/enum.png")).setDimensions(12, 12);
    public final GlTexture arrowIcon = new GlTexture(new ClasspathResource("/icons/menu/new/arrow_horizontal.png")).setDimensions(9, 8);
    public final GlTexture gamepadIcon = new GlTexture(new ClasspathResource("/icons/menu/new/gamepad.png")).setDimensions(12, 12);
    public final GlTexture textIcon = new GlTexture(new ClasspathResource("/icons/menu/new/text.png")).setDimensions(12, 12);
    public final GlTexture groupIcon = new GlTexture(new ClasspathResource("/icons/menu/new/group.png")).setDimensions(12, 12);
    public final GlTexture brushIcon = new GlTexture(new ClasspathResource("/icons/menu/new/brush.png")).setDimensions(12, 12);
    public final GlTexture clickIcon = new GlTexture(new ClasspathResource("/icons/menu/new/click.png")).setDimensions(12, 12);
    public boolean keyboardNavigation = false;
    public float lastMouseX = Float.NaN;
    public float lastMouseY = Float.NaN;
    public final MsdfFont font = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont boldFont = Fonts.INTER_EXTRA_BOLD.get();
    public final MsdfFont iconFont = Fonts.CLICK_GUI.get();
    public final MsdfFont categoryFont = Fonts.CATEGORIES.get();
    final TextInputField searchField = new TextInputField(this.font, iconSize);
    public final ToggleAnimator openAnimator = new ToggleAnimator(320, Easings.EASE_IN_OUT_CUBIC);
    final WidgetBounds searchBoxBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    final WidgetBounds resultsBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    final List<SearchMatch> matches = new ArrayList<>();
    final List<SearchResultRow> resultRows = new ArrayList<>();
    public final SearchNavigator navigator = new MenuSearchNavigator();
    final ScrollArea scrollArea = new ScrollArea();
    public int selectedIndex = -1;
    private float selectionY;
    private float selectionVisible;
    private long selectionUpdate;
    public final ToggleAnimator selectionAnimator = new ToggleAnimator(150, Easings.EASE_IN_OUT_CUBIC);
    public final List<ClickableBehavior> clickBehaviors = new ArrayList<>();

    public SearchOverlay() {
        addChild(this.scrollArea);
        this.searchField.changeText(str -> {
            this.matches.clear();
            String strReplaceAll = str.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
            boolean zIsBlank = strReplaceAll.isBlank();
            TabsController tabsController = Raven.INSTANCE.tabsController();
            if (!zIsBlank) {
                for (MenuTabElement tabElement : tabsController.tabElements()) {
                    for (AbstractFrame frame2 : tabElement.frames()) {
                        if (frame2 instanceof ModuleCard moduleCard && !moduleCard.module().isBlocked()) {
                            String strReplaceAll2 = moduleCard.name().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
                            MatchMode matchMode = null;
                            if (strReplaceAll2.equals(strReplaceAll)) {
                                matchMode = MatchMode.EXACT;
                            } else if (strReplaceAll2.startsWith(strReplaceAll)) {
                                matchMode = MatchMode.STARTS_WITH;
                            } else if (strReplaceAll2.contains(strReplaceAll)) {
                                matchMode = MatchMode.CONTAINS;
                            }
                            if (matchMode == null && Arrays.stream(moduleCard.aliases()).anyMatch(alias -> {
                                return alias.replaceAll("\\s+", "").toLowerCase(Locale.ROOT).contains(strReplaceAll);
                            })) {
                                matchMode = MatchMode.ALIAS;
                            }
                            if (matchMode != null) {
                                this.matches.add(new SearchMatch(new FrameSearchTarget(moduleCard, tabElement), matchMode));
                            }
                        }
                    }
                }
                Collections.sort(this.matches);
            }
            rebuildResultRows();
            this.scrollArea.scrollTo(0.0f);
            this.selectedIndex = this.resultRows.isEmpty() ? -1 : 0;
        });
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.openAnimator.isZero()) {
            return;
        }
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        if (this.searchField.focused()) {
            drawCtx.window().interceptKeyboard(true);
        }
        colorStack.push();
        float openProgress = this.openAnimator.smoothAnimation();
        colorStack.alpha(openProgress);
        float panelHeight = this.searchBoxBounds.height() + Math.min(computeContentHeight(), maxHeight);
        MenuStyle.drawPopup(drawCtx, this.searchBoxBounds.x(), this.searchBoxBounds.y(),
                this.searchBoxBounds.width(), panelHeight, cornerRadius);
        renderSearchBox(drawCtx, palette, colorStack);
        if (!this.resultRows.isEmpty() && openProgress > 0.25f) {
            colorStack.push();
            colorStack.alpha(Math.min(1.0f, (openProgress - 0.25f) / 0.75f));
            renderResults(drawCtx, palette, colorStack);
            colorStack.pop();
        }
        super.render(drawCtx);
        colorStack.pop();
    }

    @Nullable
    public static MatchMode matchText(String str, String str2, String str3) {
        String lowerCase = str.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        String lowerCase2 = str2.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        MatchMode matchMode = null;
        if (lowerCase.equals(str3)) {
            matchMode = MatchMode.EXACT;
        } else if (lowerCase.startsWith(str3)) {
            matchMode = MatchMode.STARTS_WITH;
        } else if (lowerCase.contains(str3) || lowerCase2.contains(str3)) {
            matchMode = MatchMode.CONTAINS;
        }
        return matchMode;
    }

    public void renderSearchBox(DrawCtx drawCtx, ThemePalette palette2, ColorStack colorStack2) {
        if (!this.resultRows.isEmpty()) {
            drawCtx.hairline(this.searchBoxBounds.x() + horizontalPadding,
                    this.searchBoxBounds.y() + this.searchBoxBounds.height() - 1.0f,
                    this.searchBoxBounds.width() - horizontalPadding * 2.0f,
                    MenuStyle.outline(colorStack2));
        }
        float fX = this.searchBoxBounds.x() + horizontalPadding;
        int color = colorStack2.computeColor(palette2.text().tone(100).argb());
        String strText = this.searchField.text();
        MenuStyle.drawCenteredIcon(drawCtx, this.iconFont, "b", searchGlyphSize,
                fX, this.searchBoxBounds.y(), searchGlyphBox, this.searchBoxBounds.height(), color);
        float fWidth = fX + searchGlyphBox + 6.0f - this.searchField.viewportOffset();
        float height = this.font.getHeight(fontSize);
        float fY = (this.searchBoxBounds.y() + (this.searchBoxBounds.height() / smallGap)) - Math.round(height / smallGap);
        float textClipX = fX + searchGlyphBox + 6.0f;
        float textClipWidth = Math.max(0.0f, this.searchBoxBounds.width() - 40.0f);
        if (textClipWidth < 1.0f) {
            return;
        }
        drawCtx.drawEngine().beginStencil();
        drawCtx.fillRect(textClipX, this.searchBoxBounds.y(), textClipWidth, this.searchBoxBounds.height(), colorStack2.white());
        drawCtx.drawEngine().prepareStencil(1);
        if (this.searchField.hasSelection()) {
            int iSelMin = this.searchField.selMin();
            int iSelMax = this.searchField.selMax();
            float fTextWidthPhysical = fWidth + drawCtx.textWidthPhysical(this.font, strText.substring(0, iSelMin), iconSize);
            drawCtx.fillRoundedRect(fTextWidthPhysical, fY - 1.0f, ((fWidth + drawCtx.textWidthPhysical(this.font, strText.substring(0, iSelMax), iconSize)) + 1.0f) - fTextWidthPhysical, height + smallGap, 4.0f, colorStack2.computeColor(palette2.accent().argb(), 128));
        }
        if (!strText.isEmpty() && this.searchField.focused()) {
            color = colorStack2.computeColor(palette2.text().tone(100).argb());
        }
        drawCtx.text(this.font, (!strText.isEmpty() || this.searchField.focused()) ? strText : Lang.SEARCH.effective(), iconSize, fWidth, fY, color);
        if (this.searchField.isCursorVisible() && !this.searchField.hasSelection()) {
            drawCtx.fillRect(fWidth + drawCtx.textWidthPhysical(this.font, strText.substring(0, this.searchField.cursorIndex()), iconSize), fY, 1.0f, height, MenuStyle.primaryText(drawCtx));
        }
        drawCtx.drawEngine().endStencil();
    }

    public void renderResults(DrawCtx drawCtx, ThemePalette palette2, ColorStack colorStack2) {
        float contentHeight = getContentHeight();
        this.scrollArea.beginArea(drawCtx, this.resultsBounds.x(), this.resultsBounds.y(), this.resultsBounds.width(), this.resultsBounds.height());
        float fY = this.resultsBounds.y();
        float fX = this.resultsBounds.x() + horizontalPadding;
        float fWidth = this.resultsBounds.width() - horizontalPadding * 2.0f;
        long now = System.nanoTime();
        float frame = this.selectionUpdate == 0L ? 1.0f : Math.min(1.0f, (now - this.selectionUpdate) * 1.0e-9f * 16.0f);
        this.selectionUpdate = now;
        float targetY = fY + this.selectedIndex * (rowHeight + smallGap);
        this.selectionY = this.selectionVisible <= 0.01f ? targetY : this.selectionY + (targetY - this.selectionY) * frame;
        boolean showSelection = this.keyboardNavigation && this.selectedIndex >= 0 && this.selectedIndex < this.resultRows.size();
        this.selectionVisible += ((showSelection ? 1.0f : 0.0f) - this.selectionVisible) * frame;
        if (this.selectionVisible > 0.01f) {
            float topRadius = 6.0f * Math.clamp((this.selectionY - fY) / rowHeight, 0.0f, 1.0f);
            drawCtx.fillRoundedRect(fX, this.selectionY, fWidth, rowHeight,
                    new Vector4f(topRadius, 6.0f, topRadius, 6.0f),
                    MenuStyle.hoverTint(colorStack2, 0.1f * this.selectionVisible));
        }
        for (int i = 0; i < this.resultRows.size(); i++) {
            SearchResultRow searchResultRow = this.resultRows.get(i);
            float rowHover = i < this.clickBehaviors.size()
                    ? this.clickBehaviors.get(i).hoverAnimation().smoothAnimation() : 0.0f;
            MenuStyle.drawDropdownOption(drawCtx, fX - 2.0f, fY - 1.0f, fWidth + 4.0f, rowHeight + 2.0f, rowHover);
            float nameX = fX + cornerRadius;
            String visibleName = truncateText(drawCtx, getResultName(searchResultRow),
                    Math.max(0.0f, fX + fWidth - cornerRadius - nameX));
            drawCtx.text(this.font, visibleName, iconSize, nameX,
                    fY + (rowHeight - this.font.getHeight(fontSize)) / 2.0f,
                    colorStack2.computeColor(palette2.text().tone(100).argb()));
            fY += rowHeight + smallGap;
        }
        this.scrollArea.endArea(drawCtx, contentHeight);
    }

    public String getResultName(SearchResultRow searchResultRow2) {
        SearchNavTarget searchTarget = searchResultRow2.searchResult().searchTarget();
        if (searchTarget instanceof SettingSearchTarget settingTarget) {
            return settingTarget.setting().getName().effective();
        }
        if (searchTarget instanceof FrameSearchTarget frameTarget) {
            AbstractFrame frameContainer = frameTarget.frameContainer();
            return frameContainer instanceof ModuleCard moduleCard ? moduleCard.name() : "";
        }
        return "";
    }

    public void renderResultPath(DrawCtx drawCtx, ThemePalette palette2, ColorStack colorStack2, SearchResultRow searchResultRow2, float f, float f2) {
        SearchNavTarget searchTarget = searchResultRow2.searchResult().searchTarget();
        int color = colorStack2.computeColor(palette2.text().tone(100).argb());
        int color2 = colorStack2.computeColor(palette2.text().tone(100).argb());
        if (searchTarget instanceof SettingSearchTarget settingSearchTarget) {
            String tabGlyph = MenuStyle.tabGlyph(settingSearchTarget.tab().iconName());
            drawCtx.text(this.categoryFont, tabGlyph, 11, f, f2 - (this.categoryFont.getHeight(11.0f) / smallGap), color2);
            float fWidth = f + 14.0f;
            String strTitle = settingSearchTarget.tab().title();
            drawCtx.text(this.font, strTitle, 11, fWidth, f2 - (this.font.getHeight(11.0f) / smallGap), color);
            float fTextWidthPhysical = fWidth + drawCtx.textWidthPhysical(this.font, strTitle, 11) + smallGap;
            drawCtx.textureVerticalC(this.arrowIcon, fTextWidthPhysical, f2 + 1.0f, this.arrowIcon.width(), this.arrowIcon.height(), color2);
            float fWidth2 = fTextWidthPhysical + this.arrowIcon.width() + smallGap;
            drawCtx.textureVerticalC(this.frameIcon, fWidth2, f2, 11, 11, color2);
            float f3 = fWidth2 + 14.0f;
            AbstractFrame frame = settingSearchTarget.frame();
            if (frame instanceof ModuleCard moduleCard) {
                drawCtx.text(this.font, moduleCard.name(), 11, f3, f2 - (this.font.getHeight(11.0f) / smallGap), color);
            }
            return;
        }
        if (searchTarget instanceof FrameSearchTarget frameSearchTarget) {
            AbstractFrame frameContainer = frameSearchTarget.frameContainer();
            MenuTabElement tab = frameSearchTarget.tab();
            String tabGlyph = MenuStyle.tabGlyph(tab.iconName());
            drawCtx.text(this.categoryFont, tabGlyph, 11, f, f2 - (this.categoryFont.getHeight(11.0f) / smallGap), color2);
            float fWidth3 = f + 14.0f;
            String strTitle2 = tab.title();
            drawCtx.text(this.font, strTitle2, 11, fWidth3, f2 - (this.font.getHeight(11.0f) / smallGap), color);
            float fTextWidthPhysical2 = fWidth3 + drawCtx.textWidthPhysical(this.font, strTitle2, 11) + smallGap;
            drawCtx.textureVerticalC(this.arrowIcon, fTextWidthPhysical2, f2 + 1.0f, this.arrowIcon.width(), this.arrowIcon.height(), color2);
            float fWidth4 = fTextWidthPhysical2 + this.arrowIcon.width() + smallGap;
            if (frameContainer instanceof ModuleCard moduleCard) {
                ModuleCategory category = moduleCard.module().getCategory();
                GlTexture icon3 = category != null ? category.icon() : this.frameIcon;
                icon3.setDimensions(11, 11);
                drawCtx.textureVerticalC(icon3, fWidth4, f2, 11, 11, color2);
                drawCtx.text(this.font, category != null ? category.displayName() : "Без категории", 11, fWidth4 + 14.0f, f2 - (this.font.getHeight(11.0f) / smallGap), color);
            }
        }
    }

    public float measureResultPath(DrawCtx drawCtx, SearchResultRow searchResultRow2) {
        SearchNavTarget searchTarget = searchResultRow2.searchResult().searchTarget();
        float fTextWidthPhysical = 0.0f;
        if (searchTarget instanceof SettingSearchTarget settingSearchTarget) {
            fTextWidthPhysical = 14.0f + drawCtx.textWidthPhysical(this.font, settingSearchTarget.tab().title(), 11) + smallGap + this.arrowIcon.width() + smallGap + 14.0f;
            AbstractFrame frame = settingSearchTarget.frame();
            if (frame instanceof ModuleCard moduleCard) {
                fTextWidthPhysical += drawCtx.textWidthPhysical(this.font, moduleCard.name(), 11);
            }
        } else if (searchTarget instanceof FrameSearchTarget frameSearchTarget) {
            AbstractFrame frameContainer = frameSearchTarget.frameContainer();
            fTextWidthPhysical = 14.0f + drawCtx.textWidthPhysical(this.font, frameSearchTarget.tab().title(), 11) + smallGap + this.arrowIcon.width() + smallGap;
            if (frameContainer instanceof ModuleCard moduleCard) {
                ModuleCategory category = moduleCard.module().getCategory();
                fTextWidthPhysical = fTextWidthPhysical + 14.0f + drawCtx.textWidthPhysical(this.font, category != null ? category.displayName() : "Без категории", 11);
            }
        }
        return fTextWidthPhysical;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        if (this.openAnimator.isZero()) {
            return;
        }
        float searchWidth = MenuStyle.SEARCH_CLOSED_WIDTH
                + (MenuStyle.SEARCH_OPEN_WIDTH - MenuStyle.SEARCH_CLOSED_WIDTH) * this.openAnimator.smoothAnimation();
        this.searchBoxBounds.withPosition(x() + width() - 10.0f - searchWidth, y() + 10.0f)
                .withSize(searchWidth, 30.0f);
        this.resultsBounds.withPosition(this.searchBoxBounds.x(), this.searchBoxBounds.y() + this.searchBoxBounds.height()).withSize(this.searchBoxBounds.width(), Math.min(computeContentHeight(), maxHeight));
        this.searchIcon.setDimensions(iconSize, iconSize);
        this.frameIcon.setDimensions(12, 12);
        this.multiEnumIcon.setDimensions(12, 12);
        this.sliderIcon.setDimensions(12, 12);
        this.enumIcon.setDimensions(12, 12);
        this.gamepadIcon.setDimensions(12, 12);
        this.groupIcon.setDimensions(12, 12);
        this.arrowIcon.setDimensions(9, 8);
        this.clickIcon.setDimensions(12, 12);
        this.brushIcon.setDimensions(12, 12);
        this.textIcon.setDimensions(12, 12);
        this.searchField.listen(this.searchBoxBounds.x(), this.searchBoxBounds.y(), this.searchBoxBounds.width(), this.searchBoxBounds.height(), Math.max(1.0f, this.searchBoxBounds.width() - 47.0f), this.searchBoxBounds.x() + horizontalPadding + searchGlyphBox + 6.0f, layoutScaleContext.scaleFactor());
        rebuildClickBehaviors();
        super.layout(layoutScaleContext);
    }

    public void rebuildClickBehaviors() {
        this.clickBehaviors.clear();
        float fY = this.resultsBounds.y();
        float fX = this.resultsBounds.x() + horizontalPadding;
        float fWidth = this.resultsBounds.width() - horizontalPadding * 2.0f;
        for (int i = 0; i < this.resultRows.size(); i++) {
            ClickableBehavior clickableBehavior = new ClickableBehavior();
            int resultIndex = i;
            clickableBehavior.clickCallback(() -> selectResult(resultIndex));
            clickableBehavior.setDimensions(fX, fY, fWidth, rowHeight);
            this.clickBehaviors.add(clickableBehavior);
            fY += rowHeight + smallGap;
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.openAnimator.animate(weightedEngine);
        this.selectionAnimator.animate(weightedEngine);
        this.searchField.animate(weightedEngine);
        for (ClickableBehavior b : this.clickBehaviors) {
            b.animate(weightedEngine);
        }
        super.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        InputEventContext withMouseOffset = inputEventContext.withMouseOffset(0.0f, this.scrollArea.scrollY());
        if (this.openAnimator.isZero() && !this.opened) {
            return false;
        }
        if (super.handleInput(inputEventContext, z)) {
            return true;
        }
        if ((inputEventContext.inputEvent() instanceof ScrollInput) && !z) {
            return true;
        }
        if (!z) {
            InputEvent inputEvent = withMouseOffset.inputEvent();
            if (inputEvent instanceof MouseButtonInput mouse && mouse.action().press() && mouse.button() == 0
                    && withMouseOffset.inArea(this.searchBoxBounds.x(), this.searchBoxBounds.y(),
                    MenuStyle.SEARCH_CLOSED_WIDTH, this.searchBoxBounds.height())) {
                close();
                return true;
            }
            if (inputEvent instanceof KeyInput keyInput) {
                if (keyInput.keyAction().press() || keyInput.keyAction().repeat()) {
                    switch (keyInput.keyCode()) {
                        case 256:
                            close();
                            return true;
                        case 257:
                            if (this.selectedIndex >= 0 && this.selectedIndex < this.resultRows.size()) {
                                selectResult(this.selectedIndex);
                                return true;
                            }
                            break;
                        case 264:
                            if (!this.resultRows.isEmpty()) {
                                this.keyboardNavigation = true;
                                this.selectedIndex = Math.min(this.selectedIndex + 1, this.resultRows.size() - 1);
                                scrollToSelected();
                                return true;
                            }
                            break;
                        case 265:
                            if (!this.resultRows.isEmpty()) {
                                this.keyboardNavigation = true;
                                this.selectedIndex = Math.max(this.selectedIndex - 1, 0);
                                scrollToSelected();
                                return true;
                            }
                            break;
                    }
                }
            }
            InputEvent inputEvent2 = withMouseOffset.inputEvent();
            if (inputEvent2 instanceof CursorMoveInput cursorMoveInput) {
                PixelPoint mousePosition = cursorMoveInput.mousePosition();
                float fX = mousePosition.x();
                float fY = mousePosition.y();
                boolean z2 = Float.isNaN(this.lastMouseX) || Math.abs(fX - this.lastMouseX) > epsilon || Math.abs(fY - this.lastMouseY) > epsilon;
                this.lastMouseX = fX;
                this.lastMouseY = fY;
                if (z2) {
                    this.keyboardNavigation = false;
                    for (int i = 0; i < this.clickBehaviors.size(); i++) {
                        ClickableBehavior clickableBehavior = this.clickBehaviors.get(i);
                        if (withMouseOffset.inArea(clickableBehavior.ownerX(), clickableBehavior.ownerY(), clickableBehavior.ownerW(), clickableBehavior.ownerH())) {
                            this.selectedIndex = i;
                        }
                    }
                }
            }
        }
        for (ClickableBehavior b : this.clickBehaviors) {
            if (b.handleInput(withMouseOffset, z)) {
                return true;
            }
        }
        if (this.searchField.handleInput(withMouseOffset, z)) {
            return true;
        }
        InputEvent inputEvent3 = withMouseOffset.inputEvent();
        if (!(inputEvent3 instanceof MouseButtonInput mouseButtonInput)) {
            return false;
        }
        if (!mouseButtonInput.action().press() || mouseButtonInput.button() != 0) {
            return false;
        }
        if (withMouseOffset.inArea(this.searchBoxBounds.x(), this.searchBoxBounds.y(), this.searchBoxBounds.width(), this.searchBoxBounds.height() + Math.min(computeContentHeight(), maxHeight))) {
            return true;
        }
        close();
        this.searchField.focused(false);
        return true;
    }

    public void scrollToSelected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.resultRows.size()) {
            return;
        }
        float rowY = getRowY(this.selectedIndex);
        float fScrollY = this.scrollArea.scrollY();
        float fY = this.resultsBounds.y() + cornerRadius;
        float fY2 = (this.resultsBounds.y() + this.resultsBounds.height()) - cornerRadius;
        float f = rowY - fScrollY;
        float f2 = f + rowHeight;
        if (f < fY) {
            this.scrollArea.scrollTo((rowY - this.resultsBounds.y()) - cornerRadius);
        } else if (f2 > fY2) {
            this.scrollArea.scrollTo(fScrollY + (f2 - fY2));
        }
    }

    public float getRowY(int i) {
        return this.resultsBounds.y() + i * (rowHeight + smallGap);
    }

    public void selectResult(int i) {
        SearchResultRow row = getRow(i);
        if (row == null) {
            return;
        }
        SearchNavTarget searchTarget = row.searchResult().searchTarget();
        if (searchTarget instanceof SettingSearchTarget settingSearchTarget) {
            this.navigator.focusSetting(settingSearchTarget.tab(), settingSearchTarget.frame(), settingSearchTarget.setting());
        } else if (searchTarget instanceof FrameSearchTarget frameSearchTarget) {
            this.navigator.focusFrame(frameSearchTarget.frameContainer(), frameSearchTarget.tab());
        }
        close();
    }

    public boolean isFocused() {
        return this.searchField.focused();
    }

    public SearchResultRow getRow(int i) {
        return i >= 0 && i < this.resultRows.size() ? this.resultRows.get(i) : null;
    }

    public void open() {
        this.opened = true;
        visible(true);
        this.openAnimator.state(true);
        this.searchField.focused(true);
        if (this.resultRows.isEmpty()) {
            return;
        }
        this.selectedIndex = 0;
    }

    public void close() {
        this.opened = false;
        this.openAnimator.state(false);
        this.searchField.focused(false);
        this.selectedIndex = -1;
        this.searchField.clearText();
    }

    public void invert() {
        this.opened = !this.opened;
        this.openAnimator.state(this.opened);
        this.searchField.focused(this.opened);
        if (!this.opened) {
            this.searchField.clearText();
        }
        if (this.opened && !this.resultRows.isEmpty()) {
            this.selectedIndex = 0;
        } else {
            if (this.opened) {
                return;
            }
            this.selectedIndex = -1;
        }
    }

    public void rebuildResultRows() {
        this.resultRows.clear();
        for (SearchMatch searchMatch : this.matches) {
            if (searchMatch.searchTarget() instanceof FrameSearchTarget) {
                this.resultRows.add(new SearchResultRow(searchMatch, Translation.clearText(""),
                        createTypeBadge(searchMatch)));
            }
        }
    }

    public SearchTypeBadge createTypeBadge(SearchMatch searchMatch) {
        SearchNavTarget searchTarget = searchMatch.searchTarget();
        if (!(searchTarget instanceof SettingSearchTarget settingTarget)) {
            return new SearchTypeBadge(null, this.frameIcon);
        }
        Setting setting = settingTarget.setting();
        if (setting instanceof BooleanSetting) return new SearchTypeBadge("CHECKBOX", this.multiEnumIcon);
        if (setting instanceof NumberSetting) return new SearchTypeBadge("SLIDER", this.sliderIcon);
        if (setting instanceof ExpandableSetting) return new SearchTypeBadge("GROUP", this.groupIcon);
        if (setting instanceof KeybindSetting) return new SearchTypeBadge("KEYBIND", this.gamepadIcon);
        if (setting instanceof MultiSelectSetting) return new SearchTypeBadge("MULTI COMBOBOX", this.enumIcon);
        if (setting instanceof TextFieldSetting) return new SearchTypeBadge("TEXTFIELD", this.textIcon);
        if (setting instanceof ModeSetting) return new SearchTypeBadge("COMBOBOX", this.enumIcon);
        if (setting instanceof ButtonSetting) return new SearchTypeBadge("BUTTON", this.clickIcon);
        if (setting instanceof ColorSetting) return new SearchTypeBadge("COLOR", this.brushIcon);
        return new SearchTypeBadge(null, this.frameIcon);
    }

    public float computeContentHeight() {
        if (this.resultRows.isEmpty()) {
            return 0.0f;
        }
        return smallGap * 2.0f + this.resultRows.size() * rowHeight
                + Math.max(0, this.resultRows.size() - 1) * smallGap;
    }

    public float getContentHeight() {
        return computeContentHeight();
    }

    private String truncateText(DrawCtx drawCtx, String text, float maxWidth) {
        if (maxWidth <= 0.0f || text.isEmpty()) {
            return "";
        }
        if (drawCtx.textWidthPhysical(this.font, text, iconSize) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        float suffixWidth = drawCtx.textWidthPhysical(this.font, suffix, iconSize);
        if (suffixWidth > maxWidth) {
            return "";
        }
        int end = text.length();
        while (end > 0 && drawCtx.textWidthPhysical(this.font, text.substring(0, end), iconSize) + suffixWidth > maxWidth) {
            end--;
        }
        return text.substring(0, end) + suffix;
    }

    public boolean isOpened() {
        return this.opened;
    }
}
