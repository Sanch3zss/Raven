package sanch3zss.raven.ui.menu.layout;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.joml.Vector3i;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.storage.Waypoint;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.font.MsdfFont;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.menu.MenuStyle;

public final class WaypointsLayout extends ListPageLayout<Waypoint> {
    private final MsdfFont markerFont = Fonts.WAYPOINTS.get();
    private final TextInputField nameField = new TextInputField(this.textFont, 13).maxLength(32);
    private final ClickableBehavior addButton = new ClickableBehavior().clickCallback(this::addHere);
    private final ClickableBehavior sortButton = new ClickableBehavior().clickCallback(() -> this.sortByName = !this.sortByName);
    private boolean sortByName;

    public WaypointsLayout() {
        super("Метки", "Пока нет меток, добавь первую");
    }

    @Override
    protected List<Waypoint> entries() {
        Comparator<Waypoint> order = this.sortByName || Minecraft.getInstance().player == null
                ? Comparator.comparing(waypoint -> waypoint.name().toLowerCase(Locale.ROOT))
                : Comparator.comparingDouble(this::distance);
        return Raven.INSTANCE.waysRepository().getWaypoints().stream()
                .filter(waypoint -> waypoint.name() != null && waypoint.vector() != null)
                .sorted(order).toList();
    }

    @Override
    protected String key(Waypoint entry) {
        return entry.name();
    }

    @Override
    protected String searchText(Waypoint entry) {
        return entry.name();
    }

    @Override
    protected float composerHeight() {
        return 40.0f;
    }

    @Override
    protected void drawComposer(DrawCtx drawCtx, float x, float y, float width) {
        MenuStyle.drawTextInput(drawCtx, this.nameField, this.textFont, this.nameField.text(), "Название метки",
                x, y + 8.0f, 200.0f, 24.0f);
        drawTextButton(drawCtx, this.addButton, "Добавить здесь", x + 208.0f, y + 8.0f, 120.0f);
        drawTextButton(drawCtx, this.sortButton, this.sortByName ? "Сортировка: по имени" : "Сортировка: по расстоянию",
                x + width - 180.0f, y + 8.0f, 180.0f);
    }

    @Override
    protected void layoutComposer(LayoutScaleContext context, float x, float y, float width) {
        this.nameField.listen(x, y + 8.0f, 200.0f, 24.0f, 188.0f, x + 6.0f, context.scaleFactor());
    }

    @Override
    protected void animateComposer(WeightedEngine engine) {
        this.nameField.animate(engine);
        this.addButton.animate(engine);
        this.sortButton.animate(engine);
    }

    @Override
    protected boolean handleComposerInput(InputEventContext context, boolean consumed) {
        boolean handled = consumed;
        handled |= this.nameField.handleInput(context, handled);
        handled |= this.addButton.handleInput(context, handled);
        handled |= this.sortButton.handleInput(context, handled);
        return handled;
    }

    @Override
    protected void setupRow(String key, Row row) {
        row.primary.clickCallback(() -> Raven.INSTANCE.waysRepository().remove(key));
    }

    @Override
    protected void drawRow(DrawCtx drawCtx, Waypoint entry, Row row, float x, float y, float width) {
        float centerY = y + ROW_HEIGHT / 2.0f;
        MenuStyle.drawCenteredIcon(drawCtx, this.markerFont, "a", 15, x, centerY - 9.0f, 18.0f, 18.0f,
                accentColor(drawCtx));
        drawFittedText(drawCtx, this.titleFont, entry.name(), 12, x + 28.0f, centerY, 220.0f, textColor(drawCtx));
        Vector3i position = entry.vector();
        String coords = "X " + position.x() + "   Y " + position.y() + "   Z " + position.z();
        drawFittedText(drawCtx, this.textFont, coords, 11, x + 260.0f, centerY, 170.0f, secondaryColor(drawCtx));
        float deleteX = x + width - 24.0f;
        if (Minecraft.getInstance().player != null) {
            String distance = Math.round(distance(entry)) + " м";
            float distanceWidth = drawCtx.textWidthPhysical(this.textFont, distance, 11);
            drawCtx.text(this.textFont, distance, 11, deleteX - 12.0f - distanceWidth,
                    textY(this.textFont, 11, centerY), secondaryColor(drawCtx));
        }
        drawIconButton(drawCtx, row.primary, "d", deleteX, centerY - 12.0f, dangerColor(drawCtx));
    }

    private double distance(Waypoint waypoint) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0.0;
        }
        Vector3i position = waypoint.vector();
        return Math.sqrt(player.distanceToSqr(position.x() + 0.5, position.y(), position.z() + 0.5));
    }

    private void addHere() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        String base = this.nameField.text().trim();
        if (base.isEmpty()) {
            base = "Метка " + player.getBlockX() + " " + player.getBlockZ();
        }
        String name = base;
        int suffix = 2;
        while (Raven.INSTANCE.waysRepository().hasWaypoint(name)) {
            name = base + " " + suffix++;
        }
        Raven.INSTANCE.waysRepository().add(name, new Vector3i(player.getBlockX(), player.getBlockY(), player.getBlockZ()));
        this.nameField.clearText();
    }

    @Override
    public void close() {
        super.close();
        this.nameField.focused(false);
    }
}
