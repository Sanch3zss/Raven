package sanch3zss.raven.ui.menu.layout;

import java.util.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.module.*;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.component.layout.*;
import sanch3zss.raven.ui.hud.AbstractFrame;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.card.ModuleCard;
import sanch3zss.raven.ui.menu.window.MenuWindow;

public class ModuleGridLayout extends AbstractTabLayout {
    public static final float CULLING_PADDING = 50.0f;
    public static final float SCROLLBAR_PADDING = 16.0f;
    public static final float PADDING_X = 16.0f;
    public static final float COLUMN_GAP = MenuStyle.CONTENT_GAP;
    public static final float FRAME_GAP = MenuStyle.CONTENT_GAP;
    ScrollbarWidget scrollbar;
    private final MsdfFont sectionFont = Fonts.INTER_MEDIUM.get();
    private final List<SectionBounds> sections = new ArrayList<>();

    @Override
    public void initialize(MenuTabElement tabElement) {
        super.initialize(tabElement);
        ScrollArea scrollingArea = tabElement.scrollingAreaComponent();
        this.scrollbar = new ScrollbarWidget(
            scrollingArea::scrollY,
            this::getContentHeight,
            () -> MenuWindow.MENU_HEIGHT - MenuWindow.HEADER_HEIGHT,
            scrollingArea::scrollTo,
            16.0f, 3.0f, 24.0f
        );
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        colorStack.push();
        this.scrollbar.render(drawCtx);
        this.tab.scrollingAreaComponent().beginArea(drawCtx, originX(), originY(), width(), height());
        renderSections(drawCtx);
        renderFrames(drawCtx);
        this.tab.scrollingAreaComponent().endArea(drawCtx, getContentHeight());
        colorStack.pop();
    }

    private void renderSections(DrawCtx drawCtx) {
        ColorStack colors = drawCtx.colorStack();
        int labelColor = colors.computeColor(drawCtx.theme().palette().text().tone(500).argb());
        float labelInset = 14.0f;
        for (SectionBounds section : this.sections) {
            MenuStyle.drawSectionPanel(drawCtx, section.x(), section.panelY(), section.width(),
                    section.height(), MenuStyle.SECTION_RADIUS);
            drawCtx.text(this.sectionFont, section.title(), 12, section.x() + labelInset,
                    section.titleY() + (19.0f - this.sectionFont.getHeight(12.0f)) / 2.0f,
                    labelColor);
        }
    }

    public void renderFrames(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame frame2 = this.tab.frames().get(size);
            if (frame2.visible() && isFrameVisible(frame2, CULLING_PADDING, originY())) {
                float dimmingForFrame = this.tab.dimmingManager().getDimmingForFrame(frame2);
                if (dimmingForFrame > 0.01f) {
                    colorStack.push();
                    colorStack.alpha(1.0f - (dimmingForFrame * 0.5f));
                }
                frame2.render(drawCtx);
                if (dimmingForFrame > 0.01f) {
                    colorStack.pop();
                }
            }
        }
    }

    @Override
    public void renderOverlays(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        this.tab.scrollingAreaComponent().scrollY();
        colorStack.push();
        drawCtx.matrixStack().pushPose();
        drawCtx.matrixStack().translate(0.0f, -drawCtx.layoutContext().toPhysical(this.tab.scrollingAreaComponent().scrollY()), 0.0f);
        for (int size = this.tab.frames().size() - 1; size >= 0; size--) {
            AbstractFrame frame2 = this.tab.frames().get(size);
            if (frame2.visible() && isFrameVisible(frame2, CULLING_PADDING, originY())) {
                float dimmingForFrame = this.tab.dimmingManager().getDimmingForFrame(frame2);
                if (dimmingForFrame > 0.01f) {
                    colorStack.push();
                    colorStack.alpha(1.0f - (dimmingForFrame * 0.5f));
                }
                frame2.renderOverlays(drawCtx);
                if (dimmingForFrame > 0.01f) {
                    colorStack.pop();
                }
            }
        }
        drawCtx.matrixStack().popPose();
        colorStack.pop();
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.tab.frames().forEach(frame2 -> frame2.layout(layoutScaleContext));
        layoutScrollbar(layoutScaleContext);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.scrollbar.animation(weightedEngine);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = z | this.scrollbar.handleInput(inputEventContext, z);
        boolean z2 = ((((float) inputEventContext.logicalMousePosition().y()) > this.tab.framesOriginY() ? 1 : (((float) inputEventContext.logicalMousePosition().y()) == this.tab.framesOriginY() ? 0 : -1)) < 0) && ((inputEventContext.inputEvent() instanceof MouseButtonInput) || (inputEventContext.inputEvent() instanceof CursorMoveInput));
        InputEventContext withMouseOffset = inputEventContext.withMouseOffset(0.0f, this.tab.scrollingAreaComponent().scrollY());
        if (!zHandleInput && !z2) {
            for (AbstractFrame frame : this.tab.frames()) {
                if (frame.visible() && isFrameVisible(frame, CULLING_PADDING, originY())
                        && frame instanceof ModuleCard moduleCard
                        && moduleCard.handleOpenDropdownInput(withMouseOffset)) {
                    return true;
                }
            }
        }
        for (AbstractFrame frame2 : this.tab.frames()) {
            if (frame2.visible() && isFrameVisible(frame2, CULLING_PADDING, originY())) {
                if (frame2.handleInput(withMouseOffset, z2 || zHandleInput) && !z2) {
                    zHandleInput = true;
                }
            }
        }
        return zHandleInput | this.tab.scrollingAreaComponent().handleInput(inputEventContext, zHandleInput);
    }

    @Override
    public float getContentHeight() {
        float f = 0.0f;
        for (AbstractFrame frame2 : this.tab.frames()) {
            if (frame2.visible()) {
                float fY = (frame2.y() - this.tab.framesOriginY()) + frame2.height() + frame2.contentHeight();
                if (fY > f) {
                    f = fY;
                }
            }
        }
        return f + 10.0f;
    }

    @Override
    public void positionFrames() {
        List<AbstractFrame> listFrames = this.tab.frames();
        this.sections.clear();
        Map<String, List<ModuleCard>> grouped = new LinkedHashMap<>();
        Map<String, String> titles = new LinkedHashMap<>();
        initializeGroups(grouped, titles);
        for (AbstractFrame frame : listFrames) {
            if (frame.visible() && frame instanceof ModuleCard card) {
                String key = groupKey(card);
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(card);
                titles.putIfAbsent(key, card.module().getCategory() == null
                        ? this.tab.title() : card.module().getCategory().displayName());
            }
        }
        Comparator<ModuleCard> cardOrder = Comparator.comparing(ModuleCard::name, String.CASE_INSENSITIVE_ORDER);
        grouped.values().forEach(cards -> cards.sort(cardOrder));

        float cardWidth = listFrames.stream()
                .filter(AbstractFrame::visible)
                .filter(ModuleCard.class::isInstance)
                .map(AbstractFrame::width)
                .findFirst()
                .orElse(0.0f);
        int columns = cardWidth > 0.0f
                ? Math.max(1, (int) Math.floor((((width() - 20.0f) - this.scrollbar.width()) + COLUMN_GAP)
                / (cardWidth + COLUMN_GAP)))
                : 1;
        float[] columnHeights = new float[columns];

        for (Map.Entry<String, List<ModuleCard>> entry : grouped.entrySet()) {
            List<ModuleCard> cards = entry.getValue();
            if (cards.isEmpty()) {
                continue;
            }
            int column = preferredInterfaceColumn(entry.getKey(), columns);
            if (column < 0) {
                column = indexOfMin(columnHeights);
            }
            float sectionX = originX() + 10.0f + column * (cardWidth + COLUMN_GAP);
            float titleY = originY() + 10.0f + columnHeights[column];
            float panelY = titleY + 19.0f;
            float rowY = panelY + 0.0f;
            for (int cardIndex = 0; cardIndex < cards.size(); cardIndex++) {
                ModuleCard card = cards.get(cardIndex);
                card.sectionEdges(cardIndex == 0, cardIndex == cards.size() - 1);
                card.targetPosition(sectionX, rowY, !this.tab.forceImmediateFramePositioning());
                rowY += card.height() + card.contentHeight();
            }
            float panelHeight = Math.max(1.0f, rowY - panelY + 0.0f);
            String title = titles.getOrDefault(entry.getKey(), this.tab.title());
            this.sections.add(new SectionBounds(title, sectionX, titleY, panelY, cardWidth, panelHeight));
            columnHeights[column] += 19.0f + panelHeight + 14.0f;
        }

        for (AbstractFrame frame : listFrames) {
            if (!frame.visible() || frame instanceof ModuleCard) {
                continue;
            }
            int column = indexOfMin(columnHeights);
            float frameX = originX() + 10.0f + column * (cardWidth + COLUMN_GAP);
            float frameY = originY() + 10.0f + columnHeights[column];
            frame.setPosition(frameX, frameY);
            columnHeights[column] += frame.height() + frame.contentHeight() + FRAME_GAP;
        }
    }

    private void initializeGroups(Map<String, List<ModuleCard>> grouped, Map<String, String> titles) {
        if (this.tab.moduleTab == ModuleTab.INTERFACE) {
            addGroup(grouped, titles, "status", "Статус");
            addGroup(grouped, titles, "system", "Система");
            addGroup(grouped, titles, "inventory", "Инвентарь");
            addGroup(grouped, titles, "binds", "Клавиши");
            addGroup(grouped, titles, "misc", "Прочее");
            return;
        }
        for (ModuleCategory category : ModuleCategory.categoriesForTab(this.tab.moduleTab)) {
            String key = category.name();
            addGroup(grouped, titles, key, category.displayName());
        }
    }

    private static void addGroup(Map<String, List<ModuleCard>> grouped, Map<String, String> titles,
                                 String key, String title) {
        grouped.put(key, new ArrayList<>());
        titles.put(key, title);
    }

    private String groupKey(ModuleCard card) {
        if (this.tab.moduleTab != ModuleTab.INTERFACE) {
            ModuleCategory category = card.module().getCategory();
            return category == null ? "uncategorized" : category.name();
        }
        return switch (card.name()) {
            case "Cooldowns", "PotionList", "Target HUD", "Coords" -> "status";
            case "Notifications", "Watermark" -> "system";
            case "Inventory HUD", "Equipment HUD" -> "inventory";
            case "Hotkeys" -> "binds";
            default -> "misc";
        };
    }

    private int preferredInterfaceColumn(String key, int columns) {
        if (this.tab.moduleTab != ModuleTab.INTERFACE || columns < 2) {
            return -1;
        }
        return switch (key) {
            case "status", "inventory", "misc" -> 0;
            case "system", "binds" -> 1;
            default -> -1;
        };
    }

    @Override
    public float width() {
        return MenuWindow.MENU_WIDTH - MenuStyle.WORKSPACE_X;
    }

    public void layoutScrollbar(LayoutScaleContext layoutScaleContext) {
        float f = MenuWindow.MENU_HEIGHT - MenuWindow.HEADER_HEIGHT;
        this.scrollbar.setPosition(((originX() + width()) - SCROLLBAR_PADDING) - this.scrollbar.width(), originY());
        this.scrollbar.setSize(this.scrollbar.width(), f);
        this.scrollbar.layout(layoutScaleContext);
    }

    private record SectionBounds(String title, float x, float titleY, float panelY, float width,
                                 float height) {
    }
}
