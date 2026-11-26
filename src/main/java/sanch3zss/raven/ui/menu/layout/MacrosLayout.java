package sanch3zss.raven.ui.menu.layout;

import java.util.List;
import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.input.KeyInput;
import sanch3zss.raven.client.input.KeyboardUtil;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.macro.MacroEntry;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.menu.MenuStyle;

public final class MacrosLayout extends ListPageLayout<MacroEntry> {
    private final TextInputField nameField = new TextInputField(this.textFont, 13).maxLength(24);
    private final TextInputField contentField = new TextInputField(this.textFont, 13).maxLength(256);
    private final ClickableBehavior keyButton = new ClickableBehavior().clickCallback(() -> toggleCapture(""));
    private final ClickableBehavior addButton = new ClickableBehavior().clickCallback(this::addMacro);
    private int composerKey = GLFW.GLFW_KEY_UNKNOWN;
    private String captureFor;

    public MacrosLayout() {
        super("Макросы", "Пока нет макросов, создай первый");
    }

    @Override
    protected List<MacroEntry> entries() {
        return Raven.INSTANCE.macroRepository().getMacroList();
    }

    @Override
    protected String key(MacroEntry entry) {
        return entry.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected String searchText(MacroEntry entry) {
        return entry.name() + " " + entry.content();
    }

    @Override
    protected float composerHeight() {
        return 40.0f;
    }

    @Override
    protected void drawComposer(DrawCtx drawCtx, float x, float y, float width) {
        if (this.captureFor != null) {
            drawCtx.window().interceptKeyboard(true);
        }
        MenuStyle.drawTextInput(drawCtx, this.nameField, this.textFont, this.nameField.text(), "Название",
                x, y + 8.0f, 140.0f, 24.0f);
        float contentWidth = width - 140.0f - 110.0f - 90.0f - 24.0f;
        MenuStyle.drawTextInput(drawCtx, this.contentField, this.textFont, this.contentField.text(),
                "Текст или /команда", x + 148.0f, y + 8.0f, contentWidth, 24.0f);
        drawKeyChip(drawCtx, this.keyButton, "".equals(this.captureFor), this.composerKey,
                x + 156.0f + contentWidth, y + 8.0f, 110.0f);
        drawTextButton(drawCtx, this.addButton, "Добавить", x + width - 90.0f, y + 8.0f, 90.0f);
    }

    @Override
    protected void layoutComposer(LayoutScaleContext context, float x, float y, float width) {
        float contentWidth = width - 140.0f - 110.0f - 90.0f - 24.0f;
        this.nameField.listen(x, y + 8.0f, 140.0f, 24.0f, 128.0f, x + 6.0f, context.scaleFactor());
        this.contentField.listen(x + 148.0f, y + 8.0f, contentWidth, 24.0f, contentWidth - 12.0f, x + 154.0f,
                context.scaleFactor());
    }

    @Override
    protected void animateComposer(WeightedEngine engine) {
        this.nameField.animate(engine);
        this.contentField.animate(engine);
        this.keyButton.animate(engine);
        this.addButton.animate(engine);
    }

    @Override
    protected boolean handleComposerInput(InputEventContext context, boolean consumed) {
        if (this.captureFor != null && context.inputEvent() instanceof KeyInput key && key.keyAction().press()) {
            if (key.keyCode() != GLFW.GLFW_KEY_ESCAPE) {
                if (this.captureFor.isEmpty()) {
                    this.composerKey = key.keyCode();
                } else {
                    Raven.INSTANCE.macroRepository().setKey(this.captureFor, key.keyCode());
                }
            }
            this.captureFor = null;
            return true;
        }
        boolean handled = consumed;
        handled |= this.nameField.handleInput(context, handled);
        handled |= this.contentField.handleInput(context, handled);
        handled |= this.keyButton.handleInput(context, handled);
        handled |= this.addButton.handleInput(context, handled);
        return handled;
    }

    @Override
    protected void setupRow(String key, Row row) {
        row.primary.clickCallback(() -> Raven.INSTANCE.macroRepository().deleteMacro(key));
        row.extra.clickCallback(() -> toggleCapture(key));
        row.toggle.clickCallback(() -> entries().stream().filter(entry -> key(entry).equals(key)).findFirst()
                .ifPresent(entry -> Raven.INSTANCE.macroRepository().setEnabled(entry.name(), !entry.enabled())));
        entries().stream().filter(entry -> key(entry).equals(key)).findFirst()
                .ifPresent(entry -> row.toggleAnimation.force(entry.enabled()));
    }

    @Override
    protected void drawRow(DrawCtx drawCtx, MacroEntry entry, Row row, float x, float y, float width) {
        float centerY = y + ROW_HEIGHT / 2.0f;
        row.toggleAnimation.state(entry.enabled());
        MenuStyle.drawCheckbox(drawCtx, x, centerY - 8.0f, 16.0f, row.toggleAnimation.smoothAnimation(),
                row.toggle.hoverAnimation().smoothAnimation());
        row.toggle.setDimensions(x - 4.0f, centerY - 12.0f, 24.0f, 24.0f);
        int nameColor = drawCtx.colorStack().interpolate(secondaryColor(drawCtx), textColor(drawCtx),
                row.toggleAnimation.smoothAnimation());
        drawFittedText(drawCtx, this.titleFont, entry.name(), 12, x + 26.0f, centerY, 124.0f, nameColor);
        float deleteX = x + width - 24.0f;
        float chipX = deleteX - 10.0f - 110.0f;
        drawFittedText(drawCtx, this.textFont, entry.content(), 11, x + 160.0f, centerY,
                chipX - 12.0f - (x + 160.0f), secondaryColor(drawCtx));
        drawKeyChip(drawCtx, row.extra, key(entry).equals(this.captureFor), entry.key(), chipX, centerY - 12.0f, 110.0f);
        drawIconButton(drawCtx, row.primary, "d", deleteX, centerY - 12.0f, dangerColor(drawCtx));
    }

    private void drawKeyChip(DrawCtx drawCtx, ClickableBehavior button, boolean capturing, int key,
                             float x, float y, float width) {
        MenuStyle.drawSettingButton(drawCtx, x, y, width, 24.0f, 5.0f, button.hoverAnimation().smoothAnimation());
        String label = capturing ? "Нажми клавишу..."
                : key == GLFW.GLFW_KEY_UNKNOWN ? "Без клавиши" : KeyboardUtil.keyToString(key).toUpperCase(Locale.ROOT);
        float labelWidth = drawCtx.textWidthPhysical(this.titleFont, label, 11);
        drawCtx.text(this.titleFont, label, 11, x + (width - labelWidth) / 2.0f,
                y + (24.0f - this.titleFont.getHeight(11.0f)) / 2.0f,
                capturing ? accentColor(drawCtx) : textColor(drawCtx));
        button.setDimensions(x, y, width, 24.0f);
    }

    private void toggleCapture(String target) {
        this.captureFor = target.equals(this.captureFor) ? null : target;
    }

    private void addMacro() {
        String name = this.nameField.text().trim();
        String content = this.contentField.text().trim();
        if (name.isEmpty()) {
            this.nameField.focused(true);
            return;
        }
        if (content.isEmpty()) {
            this.contentField.focused(true);
            return;
        }
        if (Raven.INSTANCE.macroRepository().hasMacro(name)) {
            return;
        }
        Raven.INSTANCE.macroRepository().addMacro(name, content, this.composerKey);
        this.nameField.clearText();
        this.contentField.clearText();
        this.composerKey = GLFW.GLFW_KEY_UNKNOWN;
    }

    @Override
    public void close() {
        super.close();
        this.nameField.focused(false);
        this.contentField.focused(false);
        this.captureFor = null;
    }
}
