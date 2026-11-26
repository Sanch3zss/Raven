package sanch3zss.raven.ui.menu.layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import sanch3zss.raven.client.input.CursorMoveInput;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.input.MouseButtonInput;
import sanch3zss.raven.client.input.ScrollInput;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.font.MsdfFont;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.component.layout.AbstractTabLayout;
import sanch3zss.raven.ui.component.layout.ScrollArea;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.window.MenuWindow;
import sanch3zss.raven.util.math.interpolation.Easings;

public abstract class ListPageLayout<T> extends AbstractTabLayout {
    protected static final float ROW_HEIGHT = 34.0f;
    protected final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    protected final MsdfFont textFont = Fonts.INTER_MEDIUM.get();
    protected final MsdfFont iconFont = Fonts.CLICK_GUI.get();
    protected final TextInputField searchField = new TextInputField(this.textFont, 13).maxLength(32);
    private final ScrollArea scrollArea = new ScrollArea();
    private final Map<String, Row> rows = new HashMap<>();
    private final String title;
    private final String emptyText;
    private List<T> visibleEntries = List.of();

    protected ListPageLayout(String title, String emptyText) {
        this.title = title;
        this.emptyText = emptyText;
    }

    protected abstract List<T> entries();

    protected abstract String key(T entry);

    protected String searchText(T entry) {
        return "";
    }

    protected boolean searchable() {
        return true;
    }

    protected abstract float composerHeight();

    protected abstract void drawComposer(DrawCtx drawCtx, float x, float y, float width);

    protected abstract boolean handleComposerInput(InputEventContext context, boolean consumed);

    protected abstract void setupRow(String key, Row row);

    protected abstract void drawRow(DrawCtx drawCtx, T entry, Row row, float x, float y, float width);

    protected void layoutComposer(LayoutScaleContext context, float x, float y, float width) {
    }

    protected void animateComposer(WeightedEngine engine) {
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colors = drawCtx.colorStack();
        colors.push();
        drawCtx.text(this.titleFont, this.title, 14, originX() + 12.0f, originY() + 8.0f, textColor(drawCtx));
        if (searchable()) {
            MenuStyle.drawTextInput(drawCtx, this.searchField, this.textFont, this.searchField.text(), "Поиск",
                    searchX(), originY() + 5.0f, 170.0f, 22.0f);
        }

        MenuStyle.drawModulePanel(drawCtx, panelX(), composerY(), panelWidth(), composerHeight(), MenuStyle.SECTION_RADIUS);
        drawComposer(drawCtx, panelX() + 10.0f, composerY(), panelWidth() - 20.0f);

        MenuStyle.drawModulePanel(drawCtx, panelX(), listY(), panelWidth(), listHeight(), MenuStyle.SECTION_RADIUS);
        this.visibleEntries = filteredEntries();
        syncRows();
        if (this.visibleEntries.isEmpty()) {
            float textWidth = drawCtx.textWidthPhysical(this.textFont, this.emptyText, 12);
            drawCtx.text(this.textFont, this.emptyText, 12, panelX() + (panelWidth() - textWidth) / 2.0f,
                    listY() + listHeight() / 2.0f - this.textFont.getHeight(12.0f) / 2.0f, secondaryColor(drawCtx));
        }
        this.scrollArea.beginArea(drawCtx, panelX(), listY() + 4.0f, panelWidth(), listHeight() - 8.0f);
        float rowX = panelX() + 6.0f;
        float rowWidth = panelWidth() - 12.0f;
        for (int i = 0; i < this.visibleEntries.size(); i++) {
            T entry = this.visibleEntries.get(i);
            Row row = this.rows.get(key(entry));
            float rowY = listY() + 4.0f + i * ROW_HEIGHT;
            float hover = row.hover.hoverAnimation().smoothAnimation();
            if (hover > 0.005f) {
                drawCtx.fillRoundedRect(rowX, rowY + 1.0f, rowWidth, ROW_HEIGHT - 2.0f, 8.0f,
                        MenuStyle.hoverTint(colors, 0.05f * hover));
            }
            row.hover.setDimensions(rowX, rowY, rowWidth, ROW_HEIGHT);
            drawRow(drawCtx, entry, row, rowX + 8.0f, rowY, rowWidth - 16.0f);
        }
        this.scrollArea.endArea(drawCtx, this.visibleEntries.size() * ROW_HEIGHT);
        colors.pop();
    }

    protected void drawIconButton(DrawCtx drawCtx, ClickableBehavior button, String glyph, float x, float y,
                                  int hoverColor) {
        ColorStack colors = drawCtx.colorStack();
        float hover = button.hoverAnimation().smoothAnimation();
        MenuStyle.drawCenteredIcon(drawCtx, this.iconFont, glyph, 14, x, y, 24.0f, 24.0f,
                colors.interpolate(secondaryColor(drawCtx), hoverColor, hover));
        button.setDimensions(x, y, 24.0f, 24.0f);
    }

    protected void drawTextButton(DrawCtx drawCtx, ClickableBehavior button, String label, float x, float y,
                                  float width) {
        MenuStyle.drawSettingButton(drawCtx, x, y, width, 24.0f, 5.0f, button.hoverAnimation().smoothAnimation());
        float labelWidth = drawCtx.textWidthPhysical(this.titleFont, label, 11);
        drawCtx.text(this.titleFont, label, 11, x + (width - labelWidth) / 2.0f,
                y + (24.0f - this.titleFont.getHeight(11.0f)) / 2.0f, textColor(drawCtx));
        button.setDimensions(x, y, width, 24.0f);
    }

    protected void drawFittedText(DrawCtx drawCtx, MsdfFont font, String text, int size, float x, float centerY,
                                  float maxWidth, int color) {
        MenuStyle.drawFadingText(drawCtx, font, text, size, x, textY(font, size, centerY), maxWidth, color);
    }

    protected float textY(MsdfFont font, int size, float centerY) {
        int glyph = font.glyphIndex('H');
        if (glyph < 0) {
            return centerY - font.getHeight(size) / 2.0f;
        }
        float ascent = font.metrics().ascent() != 0.0f ? font.metrics().ascent() : font.metrics().lineHeight();
        float capTop = (ascent - font.bearingY[glyph]) * size;
        return centerY - capTop - font.planeHeight[glyph] * size / 2.0f;
    }

    protected int textColor(DrawCtx drawCtx) {
        return drawCtx.colorStack().computeColor(drawCtx.theme().palette().text().tone(100).argb());
    }

    protected int secondaryColor(DrawCtx drawCtx) {
        return drawCtx.colorStack().computeColor(drawCtx.theme().palette().text().tone(100).argb(), 0.55f);
    }

    protected int accentColor(DrawCtx drawCtx) {
        return drawCtx.colorStack().computeColor(drawCtx.theme().palette().accent().argb());
    }

    protected int dangerColor(DrawCtx drawCtx) {
        return drawCtx.colorStack().computeColor(drawCtx.theme().palette().error().tone(500).argb());
    }

    private List<T> filteredEntries() {
        String query = this.searchField.text().trim().toLowerCase(Locale.ROOT);
        List<T> result = new ArrayList<>();
        for (T entry : entries()) {
            if (query.isEmpty() || searchText(entry).toLowerCase(Locale.ROOT).contains(query)) {
                result.add(entry);
            }
        }
        return result;
    }

    private void syncRows() {
        List<String> keys = new ArrayList<>();
        for (T entry : this.visibleEntries) {
            String key = key(entry);
            keys.add(key);
            this.rows.computeIfAbsent(key, k -> {
                Row row = new Row();
                setupRow(k, row);
                return row;
            });
        }
        this.rows.keySet().retainAll(keys);
    }

    @Override
    public void layout(LayoutScaleContext context) {
        this.searchField.listen(searchX(), originY() + 5.0f, 170.0f, 22.0f, 158.0f, searchX() + 6.0f,
                context.scaleFactor());
        layoutComposer(context, panelX() + 10.0f, composerY(), panelWidth() - 20.0f);
        this.scrollArea.layout(context);
    }

    @Override
    public void animation(WeightedEngine engine) {
        this.searchField.animate(engine);
        this.scrollArea.animation(engine);
        animateComposer(engine);
        for (Row row : this.rows.values()) {
            row.hover.animate(engine);
            row.primary.animate(engine);
            row.secondary.animate(engine);
            row.extra.animate(engine);
            row.toggle.animate(engine);
            row.toggleAnimation.animate(engine);
            if (row.field != null) {
                row.field.animate(engine);
            }
        }
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        boolean handled = consumed;
        if (searchable()) {
            handled |= this.searchField.handleInput(context, handled);
        }
        handled |= handleComposerInput(context, handled);
        InputEvent event = context.inputEvent();
        boolean pointer = event instanceof MouseButtonInput || event instanceof CursorMoveInput
                || event instanceof ScrollInput;
        boolean outsideList = pointer && !context.inArea(panelX(), listY() + 4.0f, panelWidth(), listHeight() - 8.0f);
        InputEventContext rowContext = context.withMouseOffset(0.0f, this.scrollArea.scrollY());
        for (Row row : new ArrayList<>(this.rows.values())) {
            if (row.field != null) {
                handled |= row.field.handleInput(rowContext, handled || outsideList);
            }
            handled |= row.primary.handleInput(rowContext, handled || outsideList);
            handled |= row.secondary.handleInput(rowContext, handled || outsideList);
            handled |= row.extra.handleInput(rowContext, handled || outsideList);
            handled |= row.toggle.handleInput(rowContext, handled || outsideList);
            row.hover.handleInput(rowContext, handled || outsideList);
        }
        if (!outsideList) {
            handled |= this.scrollArea.handleInput(context, handled);
        }
        return handled;
    }

    public void close() {
        this.searchField.focused(false);
        for (Row row : this.rows.values()) {
            if (row.field != null) {
                row.field.focused(false);
            }
        }
    }

    protected float panelX() {
        return originX() + 12.0f;
    }

    protected float panelWidth() {
        return width() - 24.0f;
    }

    private float searchX() {
        return originX() + width() - 12.0f - 170.0f;
    }

    private float composerY() {
        return originY() + 36.0f;
    }

    private float listY() {
        return composerY() + composerHeight() + 9.0f;
    }

    private float listHeight() {
        return originY() + height() - 12.0f - listY();
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

    public static final class Row {
        public final ClickableBehavior hover = new ClickableBehavior();
        public final ClickableBehavior primary = new ClickableBehavior();
        public final ClickableBehavior secondary = new ClickableBehavior();
        public final ClickableBehavior extra = new ClickableBehavior();
        public final ClickableBehavior toggle = new ClickableBehavior();
        public final ToggleAnimator toggleAnimation = new ToggleAnimator(180, Easings.EASE_IN_OUT_CUBIC);
        public TextInputField field;
    }
}
