package sanch3zss.raven.ui.menu.layout;

import hw.zako.alphamap.protocol.AtlasGeometry;
import hw.zako.alphamap.protocol.ServerMessage;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import org.joml.Vector3i;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.storage.Waypoint;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.server.holyworld.AlphaIslandAtlas;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.font.MsdfFont;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.component.layout.AbstractTabLayout;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.window.MenuWindow;

public final class WorldMapTabLayout extends AbstractTabLayout {
    private final AlphaIslandAtlas atlas = new AlphaIslandAtlas();
    private final MsdfFont textFont = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont inputFont = Fonts.INTER_MEDIUM.get();
    private final MsdfFont markerFont = Fonts.WAYPOINTS.get();
    private final MsdfFont mapIconFont = Fonts.MAP.get();
    private final TextInputField markerName = new TextInputField(this.inputFont, 13);
    private final GlTexture playerArrow = new GlTexture(new ClasspathResource("/textures/arrow.png"))
            .generateMipMap(true).minFilter(9987).magFilter(9729)
            .wrapX(33071).wrapY(33071).anisotropy(16.0f);
    private AtlasGeometry lastGeometry;
    private float zoom = 1.0f;
    private double focusX = 0.5;
    private double focusY = 0.5;
    private boolean dragging;
    private boolean moved;
    private float dragX;
    private float dragY;
    private int selectedX;
    private int selectedZ;
    private boolean selected;
    private Waypoint selectedWaypoint;
    private ServerMessage.Marker selectedMarker;
    private record MapTarget(ServerMessage.Marker marker, Waypoint waypoint, int x, int z) {
    }

    public WorldMapTabLayout() {
        this.markerName.maxLength = 32;
    }

    public void initializeAtlas() {
        this.atlas.initialize();
    }

    public void close() {
        this.dragging = false;
        this.markerName.focused(false);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        AtlasGeometry geometry = this.atlas.geometry();
        if (geometry != null && !geometry.equals(this.lastGeometry)) {
            this.lastGeometry = geometry;
            resetView();
            clearSelection();
        }
        if (geometry == null && this.lastGeometry != null) {
            this.lastGeometry = null;
            clearSelection();
        }
        if ((this.selectedMarker != null && !this.atlas.markers().contains(this.selectedMarker))
                || (this.selectedWaypoint != null
                && !Raven.INSTANCE.waysRepository().getWaypoints().contains(this.selectedWaypoint))) {
            clearSelection();
        }
        ColorStack colors = drawCtx.colorStack();
        colors.push();
        int text = colors.computeColor(drawCtx.theme().palette().text().tone(100).argb());
        int secondary = colors.computeColor(drawCtx.theme().palette().text().tone(100).argb(), 0.62f);
        int accent = colors.computeColor(drawCtx.theme().palette().accent().argb());
        drawCtx.text(this.textFont, "Карта острова", 14, originX() + 12.0f,
                originY() + 8.0f, text);

        float mapX = mapX();
        float mapY = mapY();
        float side = mapSide();
        MenuStyle.drawModulePanel(drawCtx, mapX, mapY, side, side, MenuStyle.SECTION_RADIUS);
        float panelX = panelX();
        float panelW = panelWidth();
        MenuStyle.drawModulePanel(drawCtx, panelX, mapY, panelW, side, MenuStyle.SECTION_RADIUS);

        if (geometry == null) {
            String status = this.atlas.status();
            float labelWidth = drawCtx.textWidthPhysical(this.textFont, status, 11);
            drawCtx.text(this.textFont, status, 11, mapX + (side - labelWidth) / 2.0f,
                    mapY + side / 2.0f - 6.0f, secondary);
        } else {
            drawMap(drawCtx, geometry, accent);
        }
        drawPanel(drawCtx, geometry, text, secondary);
        colors.pop();
    }

    private void drawMap(DrawCtx drawCtx, AtlasGeometry geometry, int accent) {
        float mapX = mapX();
        float mapY = mapY();
        float side = mapSide();
        float scale = scale(geometry);
        float left = tileLeft(geometry);
        float top = tileTop(geometry);
        float physicalX = drawCtx.layoutContext().toPhysical(mapX + 1.0f);
        float physicalY = drawCtx.layoutContext().toPhysical(mapY + 1.0f);
        float physicalSide = drawCtx.layoutContext().toPhysical(side - 2.0f);
        drawCtx.drawEngine().beginScissor(drawCtx.matrixStack().last().pose(),
                physicalX, physicalY, physicalSide, physicalSide);
        float tileSize = geometry.tilePixels() * scale;
        float pixelScale = drawCtx.layoutContext().scaleFactor();
        var layout = drawCtx.layoutContext();
        float innerLeft = mapX + 1.0f;
        float innerTop = mapY + 1.0f;
        float innerRight = mapX + side - 1.0f;
        float innerBottom = mapY + side - 1.0f;
        float radius = layout.toPhysical(MenuStyle.SECTION_RADIUS - 1.0f);
        int tint = drawCtx.colorStack().white();
        for (int z = 0; z < geometry.tilesPerSide(); z++) {
            for (int x = 0; x < geometry.tilesPerSide(); x++) {
                var name = this.atlas.texture(x, z);
                if (name == null) continue;
                int textureId = FramebufferUtil.getTextureId(
                        Minecraft.getInstance().getTextureManager().getTexture(name));
                if (textureId <= 0) continue;
                float tileX = layout.alignToScale(left + x * tileSize, pixelScale);
                float tileY = layout.alignToScale(top + z * tileSize, pixelScale);
                float tileRight = layout.alignToScale(left + (x + 1) * tileSize, pixelScale);
                float tileBottom = layout.alignToScale(top + (z + 1) * tileSize, pixelScale);
                if (tileRight <= innerLeft || tileX >= innerRight || tileBottom <= innerTop || tileY >= innerBottom) {
                    continue;
                }
                float tileWidth = tileRight - tileX;
                float tileHeight = tileBottom - tileY;
                drawCtx.drawEngine().beginScissor(drawCtx.matrixStack().last().pose(),
                        layout.toPhysical(tileX), layout.toPhysical(tileY),
                        layout.toPhysical(tileWidth), layout.toPhysical(tileHeight));
                drawCtx.drawEngine().roundedTexture(drawCtx.matrixStack().last().pose(),
                        layout.toPhysical(innerLeft), layout.toPhysical(innerTop),
                        layout.toPhysical(innerRight - innerLeft), layout.toPhysical(innerBottom - innerTop),
                        radius, radius, radius, radius,
                        (innerLeft - tileX) / tileWidth, (innerTop - tileY) / tileHeight,
                        (innerRight - tileX) / tileWidth, (innerBottom - tileY) / tileHeight,
                        drawCtx.drawEngine().bindTexture(textureId), tint, tint, tint, tint);
                drawCtx.drawEngine().endScissor();
            }
        }
        int white = drawCtx.colorStack().computeColor(0xFFFFFFFF);
        List<Waypoint> waypoints = Raven.INSTANCE.waysRepository().getWaypoints().stream()
                .filter(waypoint -> waypoint.vector() != null && waypoint.name() != null)
                .sorted(Comparator.comparing(Waypoint::name))
                .toList();
        boolean selectedMarkerDrawn = false;
        Set<Long> drawnPositions = new HashSet<>();
        for (ServerMessage.Marker marker : this.atlas.markers()) {
            if (drawnPositions.add(positionKey(marker.x(), marker.z()))) {
                drawMarker(drawCtx, geometry, marker.x(), marker.z(), markerLabel(marker),
                        white, accent);
                selectedMarkerDrawn |= this.selected && this.selectedX == marker.x()
                        && this.selectedZ == marker.z();
            }
        }
        for (Waypoint waypoint : waypoints) {
            if (drawnPositions.add(positionKey(waypoint.vector().x(), waypoint.vector().z()))) {
                drawMarker(drawCtx, geometry, waypoint.vector().x(),
                        waypoint.vector().z(), waypoint.name(), white, accent);
                selectedMarkerDrawn |= this.selected && this.selectedX == waypoint.vector().x()
                        && this.selectedZ == waypoint.vector().z();
            }
        }
        if (this.selected && !selectedMarkerDrawn) {
            drawMarker(drawCtx, geometry, this.selectedX, this.selectedZ, null, white, accent);
        }
        if (Minecraft.getInstance().player != null) {
            float playerX = screenX(geometry, Minecraft.getInstance().player.getX());
            float playerY = screenY(geometry, Minecraft.getInstance().player.getZ());
            float physicalPlayerX = drawCtx.layoutContext().toPhysical(playerX);
            float physicalPlayerY = drawCtx.layoutContext().toPhysical(playerY);
            drawCtx.matrixStack().pushPose();
            drawCtx.matrixStack().translate(physicalPlayerX, physicalPlayerY, 0.0f);
            drawCtx.matrixStack().mulPose(Axis.ZP.rotationDegrees(
                    Minecraft.getInstance().player.getYRot() + 90.0f));
            drawCtx.texture(this.playerArrow, -10.0f, -10.0f, 20.0f, 20.0f,
                    drawCtx.colorStack().computeColor(0xFF9B111E));
            drawCtx.matrixStack().popPose();
        }
        drawCtx.drawEngine().endScissor();
    }

    private void drawMarker(DrawCtx drawCtx, AtlasGeometry geometry, int worldX,
                            int worldZ, String label, int white, int accent) {
        float x = screenX(geometry, worldX);
        float y = screenY(geometry, worldZ);
        if (x < mapX() - 10.0f || x > mapX() + mapSide() + 10.0f
                || y < mapY() - 10.0f || y > mapY() + mapSide() + 10.0f) return;
        int iconColor = this.selected && this.selectedX == worldX && this.selectedZ == worldZ
                ? accent : white;
        MenuStyle.drawCenteredIcon(drawCtx, this.markerFont, "a", 15,
                x - 8.0f, y - 8.0f, 16.0f, 16.0f, iconColor);
        if (label != null && !label.isBlank() && this.zoom >= 1.25f) {
            float width = drawCtx.textWidthPhysical(this.textFont, label, 9);
            drawCtx.text(this.textFont, label, 9, x - width / 2.0f, y + 9.0f, white);
        }
    }

    private static long positionKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private void drawPanel(DrawCtx drawCtx, AtlasGeometry geometry,
                           int text, int secondary) {
        float x = panelX() + 9.0f;
        float width = panelWidth() - 18.0f;
        float y = mapY() + 11.0f;
        drawCtx.text(this.textFont, "WorldMap", 12, x, y, text);
        drawCtx.text(this.textFont, "Масштаб " + Math.round(this.zoom * 100.0f) + "%",
                10, x, y + 24.0f, secondary);
        drawButton(drawCtx, x, y + 43.0f, 25.0f, "+", text);
        drawButton(drawCtx, x + 31.0f, y + 43.0f, 25.0f, "-", text);
        drawButton(drawCtx, x + 62.0f, y + 43.0f, width - 62.0f,
                "Центр", text);
        MenuStyle.drawDivider(drawCtx, x, y + 79.0f, width);
        if (geometry == null) {
            drawCtx.text(this.textFont, Minecraft.getInstance().getCurrentServer() == null
                    ? "Нет подключения" : "Ожидание карты", 10, x, y + 94.0f, secondary);
            return;
        }
        if (!this.selected) {
            drawCtx.text(this.textFont, "Нажми на карту,", 10, x, y + 96.0f, secondary);
            drawCtx.text(this.textFont, "чтобы поставить метку", 10, x, y + 109.0f, secondary);
            return;
        }
        String title = this.selectedMarker != null ? markerLabel(this.selectedMarker)
                : this.selectedWaypoint != null ? this.selectedWaypoint.name() : "Новая метка";
        drawCtx.text(this.textFont, title, 11, x, y + 98.0f, text);
        drawCtx.text(this.textFont, "X " + this.selectedX + "  Z " + this.selectedZ,
                10, x, y + 115.0f, secondary);
        int danger = this.selectedMarker == null ? 0 : dangerRating(this.selectedMarker.icon());
        float dangerOffset = danger == 0 ? 0.0f : 18.0f;
        if (danger > 0) {
            float dangerY = y + 132.0f;
            drawCtx.text(this.textFont, "Опасность:", 10, x, dangerY, text);
            float starX = x + drawCtx.textWidthPhysical(this.textFont, "Опасность:", 10) + 5.0f;
            for (int star = 0; star < 5; star++) {
                MenuStyle.drawCenteredIcon(drawCtx, this.mapIconFont, "c", 10,
                        starX + star * 11.0f, dangerY, 11.0f, this.textFont.getHeight(10.0f),
                        drawCtx.colorStack().computeColor(text, star < danger ? 1.0f : 0.28f));
            }
        }
        if (this.selectedWaypoint == null) {
            float fieldY = y + 137.0f + dangerOffset;
            MenuStyle.drawTextInput(drawCtx, this.markerName, this.inputFont, this.markerName.text(),
                    "Название метки", x, fieldY, width, 24.0f);
            drawButton(drawCtx, x, y + 170.0f + dangerOffset,
                    width, "Добавить метку", text);
        } else {
            drawButton(drawCtx, x, y + 137.0f + dangerOffset, width, "Удалить метку", text);
        }
    }

    private void drawButton(DrawCtx drawCtx, float x, float y, float width,
                            String label, int text) {
        MenuStyle.drawSettingButton(drawCtx, x, y, width, 24.0f, 3.0f, 0.0f);
        if (label.equals("+") || label.equals("-")) {
            drawCtx.fillRoundedRect(x + width / 2.0f - 5.0f, y + 11.0f,
                    10.0f, 2.0f, 1.0f, text);
            if (label.equals("+")) {
                drawCtx.fillRoundedRect(x + width / 2.0f - 1.0f, y + 7.0f,
                        2.0f, 10.0f, 1.0f, text);
            }
            return;
        }
        float labelWidth = drawCtx.textWidthPhysical(this.textFont, label, 10);
        drawCtx.text(this.textFont, label, 10, x + (width - labelWidth) / 2.0f,
                y + (24.0f - this.textFont.getHeight(10.0f)) / 2.0f, text);
    }

    @Override
    public void layout(LayoutScaleContext context) {
        float x = panelX() + 9.0f;
        float dangerOffset = this.selectedMarker != null
                && dangerRating(this.selectedMarker.icon()) > 0 ? 18.0f : 0.0f;
        this.markerName.listen(x, mapY() + 148.0f + dangerOffset,
                panelWidth() - 18.0f, 24.0f,
                panelWidth() - 30.0f, x + 6.0f, context.scaleFactor());
    }

    @Override
    public void animation(WeightedEngine engine) {
        this.markerName.animate(engine);
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        if (consumed) return false;
        var event = context.inputEvent();
        float mouseX = context.logicalMousePosition().x();
        float mouseY = context.logicalMousePosition().y();
        boolean inMap = inMap(mouseX, mouseY);
        if (this.selected && this.selectedWaypoint == null
                && (this.selectedMarker == null || !hasWaypointAt(this.selectedX, this.selectedZ))
                && this.markerName.handleInput(context, false)) return true;
        if (event instanceof ScrollInput scroll && inMap && this.atlas.geometry() != null) {
            zoomAt(scroll.deltaY() > 0.0 ? 1.25f : 0.8f, mouseX, mouseY);
            return true;
        }
        if (event instanceof CursorMoveInput && this.dragging) {
            float dx = mouseX - this.dragX;
            float dy = mouseY - this.dragY;
            if (Math.abs(dx) + Math.abs(dy) > 1.0f) this.moved = true;
            if (this.moved) {
                this.focusX = clampFocus(this.focusX - dx / (mapSide() * this.zoom));
                this.focusY = clampFocus(this.focusY - dy / (mapSide() * this.zoom));
            }
            this.dragX = mouseX;
            this.dragY = mouseY;
            return true;
        }
        if (!(event instanceof MouseButtonInput mouse) || mouse.button() != 0) return false;
        if (mouse.action().release()) {
            if (!this.dragging) return false;
            this.dragging = false;
            if (!this.moved && inMap) selectAt(mouseX, mouseY);
            return true;
        }
        if (!mouse.action().press()) return false;
        if (inMap && this.atlas.geometry() != null) {
            this.dragging = true;
            this.moved = false;
            this.dragX = mouseX;
            this.dragY = mouseY;
            return true;
        }
        float x = panelX() + 9.0f;
        float y = mapY() + 11.0f;
        float width = panelWidth() - 18.0f;
        if (inside(mouseX, mouseY, x, y + 43.0f, 25.0f, 24.0f)) {
            zoomAt(1.25f, mapX() + mapSide() / 2.0f, mapY() + mapSide() / 2.0f);
            return true;
        }
        if (inside(mouseX, mouseY, x + 31.0f, y + 43.0f, 25.0f, 24.0f)) {
            zoomAt(0.8f, mapX() + mapSide() / 2.0f, mapY() + mapSide() / 2.0f);
            return true;
        }
        if (inside(mouseX, mouseY, x + 62.0f, y + 43.0f, width - 62.0f, 24.0f)) {
            resetView();
            return true;
        }
        float dangerOffset = this.selectedMarker != null
                && dangerRating(this.selectedMarker.icon()) > 0 ? 18.0f : 0.0f;
        if (this.selected && inside(mouseX, mouseY, x,
                y + (this.selectedWaypoint == null ? 170.0f : 137.0f) + dangerOffset,
                width, 24.0f)) {
            if (this.selectedWaypoint == null) addWaypoint();
            else {
                Raven.INSTANCE.waysRepository().remove(this.selectedWaypoint.name());
                clearSelection();
            }
            return true;
        }
        return false;
    }

    private void selectAt(float mouseX, float mouseY) {
        AtlasGeometry geometry = this.atlas.geometry();
        if (geometry == null) return;
        List<MapTarget> matches = new ArrayList<>();
        for (ServerMessage.Marker marker : this.atlas.markers()) {
            double dx = screenX(geometry, marker.x()) - mouseX;
            double dy = screenY(geometry, marker.z()) - mouseY;
            if (dx * dx + dy * dy < 100.0) {
                matches.add(new MapTarget(marker, null, marker.x(), marker.z()));
            }
        }
        for (Waypoint waypoint : Raven.INSTANCE.waysRepository().getWaypoints().stream()
                .filter(waypoint -> waypoint.name() != null && waypoint.vector() != null)
                .sorted(Comparator.comparing(Waypoint::name)).toList()) {
            double dx = screenX(geometry, waypoint.vector().x()) - mouseX;
            double dy = screenY(geometry, waypoint.vector().z()) - mouseY;
            if (dx * dx + dy * dy < 100.0) {
                matches.add(new MapTarget(null, waypoint,
                        waypoint.vector().x(), waypoint.vector().z()));
            }
        }
        if (matches.isEmpty()) {
            int x = Math.round((float) (geometry.originX()
                    + (mouseX - tileLeft(geometry)) / scale(geometry) * geometry.blocksPerPixel()));
            int z = Math.round((float) (geometry.originZ()
                    + (mouseY - tileTop(geometry)) / scale(geometry) * geometry.blocksPerPixel()));
            matches.add(new MapTarget(null, null, x, z));
        } else {
            matches.sort(Comparator.comparingDouble(target -> {
                double dx = screenX(geometry, target.x()) - mouseX;
                double dy = screenY(geometry, target.z()) - mouseY;
                return dx * dx + dy * dy;
            }));
        }
        MapTarget nearest = matches.getFirst();
        ServerMessage.Marker dungeon = this.atlas.markers().stream()
                .filter(marker -> Math.abs(marker.x() - nearest.x()) <= 24
                        && Math.abs(marker.z() - nearest.z()) <= 24)
                .findFirst().orElse(null);
        int targetX = dungeon == null ? nearest.x() : dungeon.x();
        int targetZ = dungeon == null ? nearest.z() : dungeon.z();
        Waypoint waypoint = Raven.INSTANCE.waysRepository().getWaypoints().stream()
                .filter(point -> point.vector() != null
                        && Math.abs(point.vector().x() - targetX) <= (dungeon == null ? 0 : 24)
                        && Math.abs(point.vector().z() - targetZ) <= (dungeon == null ? 0 : 24))
                .findFirst().orElse(null);
        applySelection(new MapTarget(dungeon, waypoint, targetX, targetZ));
    }

    private void applySelection(MapTarget target) {
        this.selectedMarker = target.marker();
        this.selectedWaypoint = target.waypoint();
        this.selectedX = target.x();
        this.selectedZ = target.z();
        this.markerName.clearText();
        if (this.selectedMarker != null) {
            this.markerName.append(markerLabel(this.selectedMarker));
            this.markerName.cursorIndex = this.markerName.text().length();
        }
        this.markerName.focused(false);
        this.selected = true;
    }

    private void addWaypoint() {
        if (hasWaypointAt(this.selectedX, this.selectedZ)) return;
        String base = this.markerName.text().trim();
        if (base.isEmpty()) base = "Метка " + this.selectedX + " " + this.selectedZ;
        String name = base;
        int suffix = 2;
        while (Raven.INSTANCE.waysRepository().hasWaypoint(name)) {
            name = base + " " + suffix++;
        }
        int y = Minecraft.getInstance().player == null ? 64
                : Minecraft.getInstance().player.getBlockY();
        AtlasGeometry geometry = this.atlas.geometry();
        Waypoint.AlphaBinding binding = geometry == null ? null : AlphaIslandAtlas.bind(geometry,
                this.selectedMarker == null ? null : this.selectedMarker.icon(),
                this.selectedMarker == null ? null : this.selectedMarker.label());
        Raven.INSTANCE.waysRepository().add(new Waypoint(name,
                new Vector3i(this.selectedX, y, this.selectedZ), "a", base, binding));
        clearSelection();
    }

    private void clearSelection() {
        this.selected = false;
        this.selectedWaypoint = null;
        this.selectedMarker = null;
        this.markerName.clearText();
        this.markerName.focused(false);
    }

    private void zoomAt(float factor, float mouseX, float mouseY) {
        AtlasGeometry geometry = this.atlas.geometry();
        if (geometry == null) return;
        double offsetX = (mouseX - mapX() - mapSide() / 2.0) / mapSide();
        double offsetY = (mouseY - mapY() - mapSide() / 2.0) / mapSide();
        double anchorX = this.focusX + offsetX / this.zoom;
        double anchorY = this.focusY + offsetY / this.zoom;
        float maximum = (float) Math.clamp(geometry.visiblePixels() * 2.0 / mapSide(), 1.0, 32.0);
        this.zoom = Math.clamp(this.zoom * factor, 1.0f, maximum);
        this.focusX = clampFocus(anchorX - offsetX / this.zoom);
        this.focusY = clampFocus(anchorY - offsetY / this.zoom);
    }

    private double clampFocus(double value) {
        double half = 0.5 / this.zoom;
        return Math.clamp(value, half, 1.0 - half);
    }

    private void resetView() {
        this.zoom = 1.0f;
        this.focusX = 0.5;
        this.focusY = 0.5;
    }

    private float scale(AtlasGeometry geometry) {
        return (float) (mapSide() * this.zoom / geometry.visiblePixels());
    }

    private float tileLeft(AtlasGeometry geometry) {
        return (float) (mapX() + mapSide() / 2.0
                - (geometry.visibleOriginPixel() + this.focusX * geometry.visiblePixels())
                * scale(geometry));
    }

    private float tileTop(AtlasGeometry geometry) {
        return (float) (mapY() + mapSide() / 2.0
                - (geometry.visibleOriginPixel() + this.focusY * geometry.visiblePixels())
                * scale(geometry));
    }

    private float screenX(AtlasGeometry geometry, double worldX) {
        return tileLeft(geometry) + (float) (geometry.pixelX(worldX) * scale(geometry));
    }

    private float screenY(AtlasGeometry geometry, double worldZ) {
        return tileTop(geometry) + (float) (geometry.pixelZ(worldZ) * scale(geometry));
    }

    private float mapX() {
        return originX() + 12.0f;
    }

    private float mapY() {
        return originY() + 38.0f;
    }

    private float mapSide() {
        return Math.min(height() - 38.0f - 12.0f, width() - 190.0f);
    }

    private float panelX() {
        return mapX() + mapSide() + 9.0f;
    }

    private float panelWidth() {
        return originX() + width() - 12.0f - panelX();
    }

    private boolean inMap(float x, float y) {
        return inside(x, y, mapX(), mapY(), mapSide(), mapSide());
    }

    private static boolean inside(float x, float y, float left, float top,
                                  float width, float height) {
        return x >= left && x <= left + width && y >= top && y <= top + height;
    }

    private static String markerLabel(ServerMessage.Marker marker) {
        if (marker.label() != null && !marker.label().isBlank()) return marker.label();
        if (marker.icon() == null) return "Точка";
        return switch (marker.icon().toLowerCase(Locale.ROOT)) {
            case "bed" -> "Кровать";
            case "mineshaft" -> "Заброшенная шахта";
            case "stronghold" -> "Крепость Энда";
            case "copper" -> "Медный данж";
            case "bastion" -> "Бастион";
            case "airland" -> "Парящий остров";
            case "beacon" -> "Маяк";
            case "mill" -> "Мельница";
            case "quarry_sulfur" -> "Серный карьер";
            case "quarry_ore" -> "Рудный карьер";
            case "promzone" -> "Промзона";
            case "village" -> "Деревня";
            case "cave_evil" -> "Злая пещера";
            case "cave_good" -> "Добрая пещера";
            default -> marker.icon();
        };
    }

    private static int dangerRating(String icon) {
        if (icon == null) return 0;
        return switch (icon.toLowerCase(Locale.ROOT)) {
            case "village", "mill", "beacon" -> 1;
            case "promzone", "airland" -> 2;
            case "quarry_sulfur", "quarry_ore", "cave_good" -> 3;
            case "cave_evil", "mineshaft" -> 4;
            case "copper", "bastion", "stronghold" -> 5;
            default -> 0;
        };
    }

    private boolean hasWaypointAt(int x, int z) {
        int radius = this.selectedMarker == null ? 0 : 24;
        return Raven.INSTANCE.waysRepository().getWaypoints().stream()
                .anyMatch(waypoint -> waypoint.vector() != null
                        && Math.abs(waypoint.vector().x() - x) <= radius
                        && Math.abs(waypoint.vector().z() - z) <= radius);
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
}
