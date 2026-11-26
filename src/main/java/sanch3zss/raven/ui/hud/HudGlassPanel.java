package sanch3zss.raven.ui.hud;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.render.engine.DrawEngine;
import org.joml.Matrix4f;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.render.theme.Theme;
import sanch3zss.raven.ui.menu.MenuStyle;

public final class HudGlassPanel {
    private HudGlassPanel() {
    }

    public static void drawShadows(DrawEngine drawEngine, Matrix4f matrix, float x, float y,
                                   float width, float height, float cornerRadius,
                                   float farRadius, int color) {
        drawEngine.roundedShadow(matrix, x, y, width, height, cornerRadius,
                farRadius * 0.35f, color);
        int nearAlpha = Math.round((color >>> 24) * 0.62f);
        int nearColor = (nearAlpha << 24) | (color & 0x00FFFFFF);
        drawEngine.roundedShadow(matrix, x, y, width, height, cornerRadius,
                Math.min(4.5f, farRadius * 0.18f), nearColor);
    }

    public static void drawInfoCell(DrawEngine drawEngine, Matrix4f matrix, int blurTexture,
                                    float x, float y, float width, float height, float cornerRadius) {
        if (width <= 0.01f) return;
        ColorStack colors = drawEngine.colorStack();
        drawEngine.squircleCorners(false);
        if (blurTexture != 0) {
            Theme theme = Raven.INSTANCE.theme();
            int tint = mixRgb(MenuStyle.surfaceColor(theme, 0xFF111217),
                    theme.palette().accentStart().argb(), 0.05f);
            drawEngine.roundedGlassPanel(matrix, x, y, width, height, cornerRadius, 0.0f, 0.28f,
                    colors.computeColor(tint, 1.0f), 0, blurTexture);
        }
        drawEngine.roundedRectangle(matrix, x, y, width, height, cornerRadius,
                colors.computeColor(0xFFFFFFFF, 0.08f));
        drawEngine.squircleCorners(true);
    }

    public static void draw(DrawEngine drawEngine, Matrix4f matrix, Theme theme,
                            int blurTexture, float x, float y, float width, float height,
                            float cornerRadius) {
        ColorStack colors = drawEngine.colorStack();
        int tint = mixRgb(MenuStyle.surfaceColor(theme, 0xFF111217),
                theme.palette().accentStart().argb(), 0.075f);
        drawShadows(drawEngine, matrix, x, y, width, height, cornerRadius,
                25.5f, colors.computeColor(tint, 1.5f));
        if (blurTexture != 0) {
            drawEngine.roundedGlassPanel(matrix, x, y, width, height,
                    cornerRadius, 0.0f, 0.80f,
                    colors.computeColor(tint, 1.0f),
                    0, blurTexture);
        } else {
            drawEngine.roundedRectangle(matrix, x, y, width, height,
                    cornerRadius, colors.computeColor(tint, 0.96f));
        }
    }

    private static int mixRgb(int base, int tint, float amount) {
        float inverse = 1.0f - amount;
        int red = Math.round(((base >> 16) & 0xFF) * inverse + ((tint >> 16) & 0xFF) * amount);
        int green = Math.round(((base >> 8) & 0xFF) * inverse + ((tint >> 8) & 0xFF) * amount);
        int blue = Math.round((base & 0xFF) * inverse + (tint & 0xFF) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
