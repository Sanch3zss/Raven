package sanch3zss.raven.feature.waypoint;


import sanch3zss.raven.render.engine.DrawEngine;
import com.mojang.blaze3d.platform.Window;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.storage.Waypoint;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.render.theme.ThemePalette;

public final class WaypointRenderer {
    private static WaypointRenderer instance;
    private List<Marker> markers = List.of();

    private record Marker(String text, String icon, float x, float y) {
    }

    public WaypointRenderer() {
        instance = this;
        Raven.INSTANCE.eventDispatcher().register(WorldRenderEvent.class, this::prepare);
    }

    public static boolean hasMarkers() {
        return instance != null && !instance.markers.isEmpty();
    }

    public static void renderPrepared() {
        if (hasMarkers()) {
            instance.drawPrepared();
        }
    }

    private void prepare(WorldRenderEvent event) {
        this.markers = List.of();
        Mc mc = Mc.INSTANCE;
        if (!mc.isWorldLoaded() || Raven.INSTANCE.waysRepository().isEmpty()) {
            return;
        }

        Window window = mc.getWindow();
        int screenWidth = window.getWidth();
        int screenHeight = window.getHeight();
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        Matrix4f viewProjection = new Matrix4f(event.projectionMatrix()).mul(event.modelViewMatrix());
        Vec3 camera = mc.getCamera().position();
        List<Marker> markers = new ArrayList<>();
        for (Waypoint waypoint : Raven.INSTANCE.waysRepository().getWaypoints()) {
            Vector3i position = waypoint.vector();
            if (position == null || waypoint.name() == null || waypoint.name().isBlank()) {
                continue;
            }
            Vec3 worldPosition = new Vec3(position.x() + 0.5, position.y() + 1.0, position.z() + 0.5);
            Vec3 relative = worldPosition.subtract(camera);
            Vector4f clip = viewProjection.transform(new Vector4f(
                    (float) relative.x, (float) relative.y, (float) relative.z, 1.0f));
            if (clip.w <= 0.001f || !Float.isFinite(clip.w)) {
                continue;
            }
            float x = (clip.x / clip.w + 1.0f) * 0.5f * screenWidth;
            float y = (1.0f - clip.y / clip.w) * 0.5f * screenHeight;
            if (!Float.isFinite(x) || !Float.isFinite(y)) {
                continue;
            }
            int distance = (int) Math.round(Math.sqrt(mc.getPlayer().distanceToSqr(worldPosition)));
            String label = waypoint.label() == null || waypoint.label().isBlank()
                    ? waypoint.name() : waypoint.label();
            String icon = waypoint.icon() == null || waypoint.icon().isBlank() ? "a" : waypoint.icon();
            markers.add(new Marker(label + " - " + distance + "м", icon, x, y));
        }

        this.markers = markers;
    }

    private void drawPrepared() {
        DrawEngine draw = Raven.INSTANCE.drawEngine();
        ColorStack colors = draw.colorStack();
        ThemePalette palette = Raven.INSTANCE.theme().palette();
        draw.begin();
        int textColor = colors.computeColor(palette.text().tone(100).argb());
        Matrix4f pose = new Matrix4f();

        for (Marker marker : this.markers) {
            var iconFont = Fonts.WAYPOINTS.get();
            int glyph = marker.icon().codePointAt(0);
            float iconSize = 40.5f;
            draw.msdfFont(pose, iconFont, marker.icon(),
                    iconFont.centeredGlyphX(glyph, iconSize, marker.x() - iconSize / 2.0f, iconSize),
                    iconFont.centeredGlyphY(glyph, iconSize, marker.y() - 45.0f, iconSize),
                    iconSize, 0.0f, textColor);
            float textWidth = Fonts.INTER_BOLD.get().getWidth(marker.text(), 12.0f);
            draw.msdfFont(pose, Fonts.INTER_BOLD.get(), marker.text(),
                    marker.x() - textWidth / 2.0f, marker.y() - 8.0f,
                    12.0f, 0.0f, textColor);
        }
        draw.end();
    }
}
