package sanch3zss.raven.ui.menu;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.gl.FramebufferUtil;
import java.util.Map;
import org.joml.Vector4f;

import net.minecraft.client.gui.screens.ChatScreen;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.misc.InterfaceStyle;
import sanch3zss.raven.render.font.MsdfFont;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.input.TextInputField;

public final class MenuStyle {
    public static final float LARGE_WIDTH = 810.0f;
    public static final float LARGE_HEIGHT = 540.0f;

    public static final float HEADER_HEIGHT = 50.0f;
    public static final float SIDEBAR_WIDTH = 175.0f;
    public static final float WORKSPACE_X = SIDEBAR_WIDTH;
    public static final float WINDOW_RADIUS = 16.0f;
    public static final float CONTENT_GAP = 5.0f;
    public static final float TAB_HEIGHT = 30.0f;
    public static final float TAB_RADIUS = 8.0f;
    public static final float SECTION_RADIUS = 10.0f;
    public static final float CONTROL_RADIUS = 7.0f;
    public static final float SIDEBAR_CONTENT_TOP = 60.0f;
    public static final float SIDEBAR_CONTENT_BOTTOM = 10.0f;
    public static final float TAB_LABEL_HEIGHT = 15.0f;
    public static final float CONFIG_BUTTON_WIDTH = 115.0f;
    public static final float CONFIG_MENU_WIDTH = 220.0f;
    public static final float CONFIG_MENU_HEIGHT = 110.0f;
    public static final float SEARCH_CLOSED_WIDTH = 30.0f;
    public static final float SEARCH_OPEN_WIDTH = 220.0f;
    public static final float DROPDOWN_RADIUS = 6.0f;
    public static final float DROPDOWN_GAP = 2.0f;
    public static final float DROPDOWN_PADDING = 2.0f;
    public static final float DROPDOWN_FIELD_HEIGHT = 22.0f;
    public static final float DROPDOWN_OPTION_HEIGHT = 22.0f;
    public static final float DROPDOWN_MIN_WIDTH = 120.0f;
    public static final float DROPDOWN_MAX_WIDTH = 220.0f;
    public static final float SETTING_LABEL_MIN_WIDTH = 84.0f;
    public static final float SETTING_CONTROL_GAP = 8.0f;
    public static final int MOTION_FAST_MS = 70;
    public static final int MOTION_NORMAL_MS = 120;
    public static final int MOTION_SLOW_MS = 180;

    public static final int SURFACE_BASE_COLOR = 0xFF111217;

    private static ThemePalette lightSource;
    private static Theme lightTheme;

    private MenuStyle() {
    }

    public static boolean light() {
        return Raven.INSTANCE.interfaceStyle() == InterfaceStyle.LIGHT;
    }

    public static Theme menuTheme(Theme theme) {
        if (!light()) {
            return theme;
        }
        ThemePalette palette = theme.palette();
        if (lightTheme == null || lightSource != palette) {
            lightSource = palette;
            ThemePalette lightPalette = new ThemePalette(palette.accentStart(), palette.accentBright(), palette.favorite(),
                    ColorValue.fromHex("F2F2F7"),
                    new ColorToneScale(Map.ofEntries(Map.entry(900, ColorValue.fromHex("E5E5EA")), Map.entry(800, ColorValue.fromHex("D1D1D6")),
                            Map.entry(700, ColorValue.fromHex("C7C7CC")), Map.entry(600, ColorValue.fromHex("AEAEB2")),
                            Map.entry(500, ColorValue.fromHex("8A8A8E")), Map.entry(400, ColorValue.fromHex("6C6C70")),
                            Map.entry(300, ColorValue.fromHex("48484A")), Map.entry(200, ColorValue.fromHex("2C2C2E")),
                            Map.entry(100, ColorValue.fromHex("1C1C1E")), Map.entry(50, ColorValue.fromHex("000000")))),
                    palette.error(),
                    new ColorToneScale(Map.ofEntries(Map.entry(700, ColorValue.fromHex("E5E5EA")), Map.entry(600, ColorValue.fromHex("E9E9EE")),
                            Map.entry(500, ColorValue.fromHex("D8D8DC")), Map.entry(400, ColorValue.fromHex("D1D1D6")),
                            Map.entry(300, ColorValue.fromHex("C6C6C8")), Map.entry(50, ColorValue.fromHex("8E8E93")))),
                    new ColorToneScale(Map.ofEntries(Map.entry(900, ColorValue.fromHex("F2F2F7")), Map.entry(801, ColorValue.fromHex("F5F5F8")),
                            Map.entry(800, ColorValue.fromHex("FFFFFF")), Map.entry(700, ColorValue.fromHex("FAFAFC")),
                            Map.entry(600, ColorValue.fromHex("F2F2F7")), Map.entry(500, ColorValue.fromHex("E9E9EE")),
                            Map.entry(400, ColorValue.fromHex("E5E5EA")), Map.entry(300, ColorValue.fromHex("DCDCE0")),
                            Map.entry(200, ColorValue.fromHex("AEAEB2")))),
                    palette.colorMode);
            lightTheme = Theme.of(theme.id(), theme.name(), theme.owner(), theme.kind(), ThemeMode.LIGHT, lightPalette,
                    theme.createdAt(), theme.updatedAt());
        }
        return lightTheme;
    }

    public static int surfaceColor(Theme theme, int dark) {
        return dark;
    }

    public static int control(ColorStack colors) {
        return fill(colors, 0.0f);
    }

    public static int controlHover(ColorStack colors) {
        return fill(colors, 1.0f);
    }

    public static int outline(ColorStack colors) {
        return light() ? colors.computeColor(0xFF3C3C43, 0.18f) : colors.computeColor(0xFF545458, 0.6f);
    }

    public static int hoverTint(ColorStack colors, float alpha) {
        return light() ? colors.computeColor(0xFF000000, alpha * 0.7f) : colors.computeColor(0xFFFFFFFF, alpha);
    }

    public static int primaryText(DrawCtx drawCtx) {
        return drawCtx.colorStack().computeColor(drawCtx.theme().palette().text().tone(100).argb());
    }

    private static int fill(ColorStack colors, float hover) {
        return colors.computeColor(0xFF787880, (light() ? 0.14f : 0.30f) + 0.06f * hover);
    }

    private static int highlight(ColorStack colors, float amount) {
        return light() ? colors.computeColor(0xFF000000, 0.05f * amount) : colors.computeColor(0xFFFFFFFF, 0.07f * amount);
    }

    public static void drawRing(DrawCtx drawCtx, float x, float y, float width, float height, float radius,
                                float thickness, int color) {
        drawCtx.fillOutlinedRoundedRect(x, y, width, height, radius, thickness, color, color & 0x00FFFFFF);
    }

    private static int cell(ColorStack colors) {
        return colors.computeColor(light() ? 0xFFFFFFFF : 0xFF2C2C2E);
    }

    public static void drawRootGlass(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        ColorStack colors = drawCtx.colorStack();
        drawSoftShadow(drawCtx, x, y + 10.0f, width, height, radius, 26.0f,
                colors.computeColor(0xFF000000, light() ? 0.22f : 0.45f));
        drawMaterial(drawCtx, x, y, width, height, new Vector4f(radius), light() ? 0xFFF2F2F7 : 0xFF1C1C1E, 0.94f,
                sceneBlurTexture());
        drawRing(drawCtx, x, y, width, height, radius, 0.8f,
                colors.computeColor(light() ? 0xFF000000 : 0xFFFFFFFF, light() ? 0.08f : 0.1f));
    }

    public static void drawSidebarPanel(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        ColorStack colors = drawCtx.colorStack();
        drawCtx.fillRoundedRect(x, y, width, height, new Vector4f(0.0f, 0.0f, radius, radius),
                light() ? colors.computeColor(0xFFFFFFFF, 0.6f) : colors.computeColor(0xFF000000, 0.18f));
        drawCtx.verticalHairline(x + width - 1.0f, y, height, outline(colors));
    }

    public static void drawSidebarTab(DrawCtx drawCtx, float x, float y, float width, float height,
                                      float hover, float active) {
        ColorStack colors = drawCtx.colorStack();
        if (hover > 0.005f && active < 0.995f) {
            drawCtx.fillRoundedRect(x, y, width, height, TAB_RADIUS, highlight(colors, hover * (1.0f - active)));
        }
        if (active > 0.005f) {
            drawCtx.fillRoundedRect(x, y, width, height, TAB_RADIUS,
                    colors.computeColor(drawCtx.theme().palette().accent().argb(), active));
        }
    }

    public static void drawOptionsIcon(DrawCtx drawCtx, MsdfFont iconFont, float x, float y, float height, int color) {
        drawCtx.text(iconFont, "g", 17, x, y + (height - iconFont.getHeight(17.0f)) / 2.0f + 2.0f, color);
    }

    public static void drawModulePanel(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        drawCtx.fillRoundedRect(x, y, width, height, radius, cell(drawCtx.colorStack()));
    }

    public static void drawSectionPanel(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        drawModulePanel(drawCtx, x, y, width, height, radius);
    }

    public static void drawCard(DrawCtx drawCtx, float x, float y, float width, float height, float radius, float hover) {
        drawModulePanel(drawCtx, x, y, width, height, radius);
        if (hover > 0.005f) {
            drawCtx.fillRoundedRect(x, y, width, height, radius, highlight(drawCtx.colorStack(), hover));
        }
    }

    public static void drawModuleRow(DrawCtx drawCtx, float x, float y, float width, float height,
                                     float hover, boolean first, boolean last) {
        drawListRow(drawCtx, x, y, width, height, hover, first, last);
        if (!first) {
            drawCtx.hairline(x, y, width, outline(drawCtx.colorStack()));
        }
    }

    public static void drawListRow(DrawCtx drawCtx, float x, float y, float width, float height,
                                   float hover, boolean first, boolean last) {
        if (hover <= 0.005f) {
            return;
        }
        Vector4f radii = new Vector4f(first ? SECTION_RADIUS : 0.0f, last ? SECTION_RADIUS : 0.0f,
                first ? SECTION_RADIUS : 0.0f, last ? SECTION_RADIUS : 0.0f);
        drawCtx.fillRoundedRect(x, y, width, height, radii, highlight(drawCtx.colorStack(), hover));
    }

    public static void drawChevron(DrawCtx drawCtx, float centerX, float centerY, float height,
                                   float thickness, int color) {
        float width = height / 2.0f + thickness;
        float boxHeight = height + thickness;
        var layout = drawCtx.layoutContext();
        drawCtx.drawEngine().chevron(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(centerX - width / 2.0f), layout.toPhysical(centerY - boxHeight / 2.0f),
                layout.toPhysical(width), layout.toPhysical(boxHeight), layout.toPhysical(thickness), color);
    }

    public static void drawPopup(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        drawFloatingPanel(drawCtx, x, y, width, height, radius);
    }

    public static void drawSettingsPanel(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        drawFloatingPanel(drawCtx, x, y, width, height, radius);
    }

    public static void drawDropdownPanel(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        drawFloatingPanel(drawCtx, x, y, width, height, radius);
    }

    public static void drawDialog(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        drawFloatingPanel(drawCtx, x, y, width, height, radius);
    }

    public static void drawTooltip(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        ColorStack colors = drawCtx.colorStack();
        drawSoftShadow(drawCtx, x, y + 3.0f, width, height, radius, 10.0f,
                colors.computeColor(0xFF000000, light() ? 0.14f : 0.35f));
        drawMaterial(drawCtx, x, y, width, height, new Vector4f(radius), light() ? 0xFFFFFFFF : 0xFF2C2C2E, 0.9f,
                blurTexture());
    }

    private static void drawFloatingPanel(DrawCtx drawCtx, float x, float y, float width, float height, float radius) {
        ColorStack colors = drawCtx.colorStack();
        radius = Math.min(SECTION_RADIUS, height / 2.0f);
        drawSoftShadow(drawCtx, x, y + 6.0f, width, height, radius, 16.0f,
                colors.computeColor(0xFF000000, light() ? 0.16f : 0.4f));
        drawMaterial(drawCtx, x, y, width, height, new Vector4f(radius), light() ? 0xFFFFFFFF : 0xFF2C2C2E, 0.9f,
                blurTexture());
        drawRing(drawCtx, x, y, width, height, radius, 0.8f,
                colors.computeColor(light() ? 0xFF000000 : 0xFFFFFFFF, light() ? 0.06f : 0.1f));
    }

    private static void drawMaterial(DrawCtx drawCtx, float x, float y, float width, float height, Vector4f radii,
                                     int tint, float strength, int blurTexture) {
        ColorStack colors = drawCtx.colorStack();
        if (blurTexture == 0 || radii.x != radii.z) {
            drawCtx.fillRoundedRect(x, y, width, height, radii, colors.computeColor(tint));
            return;
        }
        var layout = drawCtx.layoutContext();
        drawCtx.drawEngine().roundedGlassPanel(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(x), layout.toPhysical(y), layout.toPhysical(width), layout.toPhysical(height),
                layout.toPhysical(radii.x), 0.0f, strength, colors.computeColor(tint), 0, blurTexture);
    }

    public static void drawKeycap(DrawCtx drawCtx, float x, float y, float width, float height, float hover,
                                  boolean capturing) {
        ColorStack colors = drawCtx.colorStack();
        float radius = Math.min(5.0f, height / 2.0f);
        drawCtx.fillRoundedRect(x, y + 1.2f, width, height, radius,
                colors.computeColor(0xFF000000, light() ? 0.14f : 0.45f));
        drawCtx.fillRoundedRect(x, y, width, height, radius, colors.computeColor(light() ? 0xFFFFFFFF : 0xFF48484A));
        if (hover > 0.005f) {
            drawCtx.fillRoundedRect(x, y, width, height, radius, highlight(colors, hover));
        }
        if (capturing) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 180.0);
            drawRing(drawCtx, x - 1.0f, y - 1.0f, width + 2.0f, height + 2.0f, radius + 1.0f, 1.5f,
                    colors.computeColor(drawCtx.theme().palette().accent().argb(), 0.45f + 0.5f * pulse));
        }
    }

    public static void drawColorWell(DrawCtx drawCtx, float x, float y, float size, int color, float hover) {
        ColorStack colors = drawCtx.colorStack();
        var layout = drawCtx.layoutContext();
        drawCtx.drawEngine().colorWell(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(x), layout.toPhysical(y), layout.toPhysical(size),
                layout.toPhysical(2.5f), layout.toPhysical(2.0f - 0.75f * hover),
                colors.computeColor(color | 0xFF000000), colors.computeColor(0xFFFFFFFF));
    }

    public static void drawDragHandle(DrawCtx drawCtx, float x, float centerY, int color) {
        for (int i = -1; i <= 1; i++) {
            drawCtx.fillRoundedRect(x, centerY + i * 3.5f - 0.6f, 10.0f, 1.2f, 0.6f, color);
        }
    }

    private static void drawControlFill(DrawCtx drawCtx, float x, float y, float width, float height, float hover) {
        drawCtx.fillRoundedRect(x, y, width, height, Math.min(height / 2.0f, CONTROL_RADIUS),
                fill(drawCtx.colorStack(), hover));
    }

    public static void drawDropdownField(DrawCtx drawCtx, float x, float y, float width, float height,
                                         float radius, float hover) {
        drawControlFill(drawCtx, x, y, width, height, hover);
    }

    public static void drawControlField(DrawCtx drawCtx, float x, float y, float width, float height,
                                        float radius, float hover) {
        drawControlFill(drawCtx, x, y, width, height, hover);
    }

    public static void drawSettingButton(DrawCtx drawCtx, float x, float y, float width, float height,
                                         float radius, float hover) {
        drawControlFill(drawCtx, x, y, width, height, hover);
    }

    public static void drawDropdownArrows(DrawCtx drawCtx, float x, float y, float width, float height) {
        int color = drawCtx.colorStack().computeColor(drawCtx.theme().palette().accent().argb());
        var layout = drawCtx.layoutContext();
        float arrowX = x + width - 13.0f;
        float centerY = y + height / 2.0f;
        drawCtx.drawEngine().chevron(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(arrowX), layout.toPhysical(centerY - 4.5f),
                layout.toPhysical(6.5f), layout.toPhysical(3.5f), layout.toPhysical(1.4f), 1, color);
        drawCtx.drawEngine().chevron(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(arrowX), layout.toPhysical(centerY + 1.0f),
                layout.toPhysical(6.5f), layout.toPhysical(3.5f), layout.toPhysical(1.4f), 2, color);
    }

    public static void drawCheckmark(DrawCtx drawCtx, float centerX, float centerY, float size, int color) {
        var layout = drawCtx.layoutContext();
        float height = size * 0.78f;
        drawCtx.drawEngine().chevron(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(centerX - size / 2.0f), layout.toPhysical(centerY - height / 2.0f),
                layout.toPhysical(size), layout.toPhysical(height), layout.toPhysical(Math.max(1.6f, size * 0.17f)), 3, color);
    }

    public static void drawCheckbox(DrawCtx drawCtx, float x, float y, float size, float progress, float hover) {
        ColorStack colors = drawCtx.colorStack();
        drawCtx.drawEngine().squircleCorners(false);
        if (progress < 0.99f) {
            drawRing(drawCtx, x, y, size, size, size / 2.0f, 1.4f,
                    colors.computeColor(light() ? 0xFFC7C7CC : 0xFF8E8E93, (1.0f - progress) * (0.8f + 0.2f * hover)));
        }
        if (progress > 0.01f) {
            drawCtx.fillRoundedRect(x, y, size, size, size / 2.0f,
                    colors.computeColor(drawCtx.theme().palette().accent().argb(), progress));
        }
        drawCtx.drawEngine().squircleCorners(true);
        if (progress > 0.01f) {
            drawCheckmark(drawCtx, x + size / 2.0f, y + size / 2.0f + 0.3f, size * 0.5f,
                    colors.computeColor(0xFFFFFFFF, progress));
        }
    }

    public static void drawTextField(DrawCtx drawCtx, float x, float y, float width, float height,
                                     float radius, float hover, float focus) {
        ColorStack colors = drawCtx.colorStack();
        float fieldRadius = Math.min(height / 2.0f, CONTROL_RADIUS);
        drawCtx.fillRoundedRect(x, y, width, height, fieldRadius, fill(colors, Math.max(hover, focus * 0.5f)));
        if (focus > 0.001f) {
            drawRing(drawCtx, x - 1.0f, y - 1.0f, width + 2.0f, height + 2.0f, fieldRadius + 1.0f, 1.5f,
                    colors.computeColor(drawCtx.theme().palette().accentStart().argb(), 0.7f * focus));
        }
    }

    public static void drawTextInput(DrawCtx drawCtx, TextInputField field, MsdfFont font, String value,
                                     String placeholder, float x, float y, float width, float height) {
        drawTextInput(drawCtx, field, font, 13, value, placeholder, x, y, width, height, 6.0f);
    }

    public static void drawTextInput(DrawCtx drawCtx, TextInputField field, MsdfFont font, int fontSize, String value,
                                     String placeholder, float x, float y, float width, float height, float inset) {
        ColorStack colors = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        drawTextField(drawCtx, x, y, width, height, CONTROL_RADIUS, field.hoverAnimation().smoothAnimation(),
                field.focusAnimation.smoothAnimation());
        if (field.focused()) {
            drawCtx.window().interceptKeyboard(true);
        }
        boolean showPlaceholder = value.isEmpty() && !field.focused();
        float textX = x + inset - field.viewportOffset();
        float textHeight = font.getHeight(fontSize);
        float textY = y + height / 2.0f - textHeight / 2.0f;
        int textColor = colors.computeColor(palette.text().tone(100).argb());
        drawCtx.drawEngine().beginStencil();
        drawCtx.fillRoundedRect(x + inset - 2.0f, y, width - inset - 2.0f, height, 4.0f, colors.white());
        drawCtx.drawEngine().prepareStencil(1);
        if (field.hasSelection()) {
            float selectionStart = textX + drawCtx.textWidthPhysical(font, value.substring(0, field.selMin()), fontSize);
            float selectionEnd = textX + drawCtx.textWidthPhysical(font, value.substring(0, field.selMax()), fontSize);
            drawCtx.fillRoundedRect(selectionStart, textY - 1.0f, selectionEnd + 1.0f - selectionStart, textHeight + 2.0f,
                    3.0f, colors.computeColor(palette.accent().argb(), 128));
        }
        drawCtx.text(font, showPlaceholder ? placeholder : value, fontSize, textX, textY,
                showPlaceholder ? colors.computeColor(palette.text().tone(500).argb()) : textColor);
        if (field.isCursorVisible() && !field.hasSelection()) {
            float cursorX = textX + drawCtx.textWidthPhysical(font, value.substring(0, field.cursorIndex()), fontSize);
            drawCtx.fillRect(cursorX, textY, 1.0f, textHeight, textColor);
        }
        drawCtx.drawEngine().endStencil();
    }

    public static void drawSegmentedControl(DrawCtx drawCtx, MsdfFont font, int textSize, String[] labels,
                                            float x, float y, float width, float height,
                                            float selection, float hover) {
        ColorStack colors = drawCtx.colorStack();
        float radius = Math.min(height / 2.0f, CONTROL_RADIUS);
        drawCtx.fillRoundedRect(x, y, width, height, radius, fill(colors, hover));
        float segmentWidth = width / labels.length;
        float thumbX = x + 2.0f + segmentWidth * selection;
        float thumbWidth = segmentWidth - 4.0f;
        float thumbHeight = height - 4.0f;
        float thumbRadius = radius - 2.0f;
        drawSoftShadow(drawCtx, thumbX, y + 2.5f, thumbWidth, thumbHeight, thumbRadius, 2.5f,
                colors.computeColor(0xFF000000, light() ? 0.12f : 0.3f));
        drawCtx.fillRoundedRect(thumbX, y + 2.0f, thumbWidth, thumbHeight, thumbRadius,
                colors.computeColor(light() ? 0xFFFFFFFF : 0xFF636366));
        int textColor = colors.computeColor(drawCtx.theme().palette().text().tone(100).argb());
        float textY = y + (height - font.getHeight(textSize)) / 2.0f;
        for (int i = 0; i < labels.length; i++) {
            float textWidth = drawCtx.textWidthPhysical(font, labels[i], textSize);
            drawCtx.text(font, labels[i], textSize, x + i * segmentWidth + (segmentWidth - textWidth) / 2.0f,
                    textY, textColor);
        }
    }

    public static void drawSwitch(DrawCtx drawCtx, float x, float y, float width, float height,
                                  float progress, int track) {
        var layout = drawCtx.layoutContext();
        ColorStack colors = drawCtx.colorStack();
        float scale = Math.max(layout.scaleFactor(), 0.01f);
        float trackX = layout.alignToScale(x);
        float trackY = layout.alignToScale(y);
        float trackWidth = Math.round(width * scale) / scale;
        float trackHeight = Math.round(height * scale) / scale;
        drawCtx.drawEngine().squircleCorners(false);
        drawCtx.fillRoundedRect(trackX, trackY, trackWidth, trackHeight, trackHeight / 2.0f, track);
        float inset = Math.max(1.0f, Math.round(1.5f * scale)) / scale;
        float knob = trackHeight - inset * 2.0f;
        float knobWidth = knob + knob * 0.4f * (float) Math.sin(progress * Math.PI);
        float knobX = trackX + inset + (trackWidth - inset * 2.0f - knobWidth) * progress;
        float knobY = trackY + inset;
        drawCtx.fillRoundedRect(knobX - 0.5f, knobY, knobWidth + 1.0f, knob + 1.0f, (knob + 1.0f) / 2.0f,
                colors.computeColor(0xFF000000, 0.14f));
        drawCtx.fillRoundedRect(knobX, knobY, knobWidth, knob, knob / 2.0f, colors.computeColor(0xFFFFFFFF));
        drawCtx.drawEngine().squircleCorners(true);
    }

    public static void drawSlider(DrawCtx drawCtx, float x, float y, float width, float height,
                                  float progress, float hover, float press, int fillColor) {
        ColorStack colorStack = drawCtx.colorStack();
        int track = colorStack.interpolate(hoverTint(colorStack, 0.06f), hoverTint(colorStack, 0.085f), hover);
        drawCtx.drawEngine().squircleCorners(false);
        drawCtx.fillRoundedRect(x, y, width, height, new Vector4f(2.0f, 2.0f, 2.0f, 2.0f), track);
        var layout = drawCtx.layoutContext();
        float scale = Math.max(layout.scaleFactor(), 0.01f);
        float knobWidth = Math.round((13.0f + 7.0f * press) * scale) / scale;
        float knobHeight = Math.round(13.0f * scale) / scale;
        float knobX = Math.max(x + knobWidth / 2.0f, Math.min(x + width - knobWidth / 2.0f, x + progress * width));
        float fillWidth = knobX - x;
        if (fillWidth > 0.0f) {
            drawCtx.fillRoundedRect(x, y, fillWidth, height, new Vector4f(2.0f, 2.0f, 2.0f, 2.0f), fillColor);
        }
        float knobY = layout.alignToScale(y + height / 2.0f - knobHeight / 2.0f);
        float left = layout.alignToScale(knobX - knobWidth / 2.0f);
        drawCtx.fillRoundedRect(left, knobY, knobWidth, knobHeight, knobHeight / 2.0f,
                colorStack.computeColor(0xFFFFFFFF, 0.95f + 0.05f * hover));
        if (light()) {
            drawRing(drawCtx, left, knobY, knobWidth, knobHeight, knobHeight / 2.0f, 0.6f,
                    colorStack.computeColor(0xFF000000, 0.12f));
        }
        drawCtx.drawEngine().squircleCorners(true);
    }

    public static void drawSoftShadow(DrawCtx drawCtx, float x, float y, float width, float height,
                                      float radius, float softness, int color) {
        var layout = drawCtx.layoutContext();
        drawCtx.drawEngine().roundedShadow(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(x), layout.toPhysical(y), layout.toPhysical(width), layout.toPhysical(height),
                layout.toPhysical(radius), layout.toPhysical(softness), color);
    }

    public static void drawDropdownOption(DrawCtx drawCtx, float x, float y, float width,
                                          float height, float hover) {
        if (hover > 0.005f) {
            drawCtx.fillRoundedRect(x + 2.0f, y + 1.0f, width - 4.0f, height - 2.0f, DROPDOWN_RADIUS,
                    highlight(drawCtx.colorStack(), hover * 1.4f));
        }
    }

    public static float dropdownHeight(int optionCount) {
        return Math.min(250.0f, DROPDOWN_PADDING * 2.0f
                + Math.max(0, optionCount) * DROPDOWN_OPTION_HEIGHT);
    }

    public static float dropdownContentHeight(int optionCount) {
        return DROPDOWN_PADDING * 2.0f
                + Math.max(0, optionCount) * DROPDOWN_OPTION_HEIGHT;
    }

    public static float dropdownWidth(DrawCtx drawCtx, MsdfFont font, int textSize,
                                      float minimum, float maximum, Iterable<String> labels,
                                      float horizontalSpace) {
        float result = minimum;
        for (String label : labels) {
            if (label != null) {
                result = Math.max(result,
                        drawCtx.textWidthPhysical(font, label, textSize) + horizontalSpace);
            }
        }
        return Math.min(Math.max(minimum, maximum), result);
    }

    public static float settingControlWidth(float preferred, float available) {
        if (available <= 0.0f) {
            return 0.0f;
        }
        float minimum = Math.min(DROPDOWN_MIN_WIDTH, available);
        float maximum = Math.min(DROPDOWN_MAX_WIDTH,
                Math.max(minimum, available - SETTING_LABEL_MIN_WIDTH - SETTING_CONTROL_GAP));
        return Math.max(minimum, Math.min(preferred, maximum));
    }

    public static void drawFadingText(DrawCtx drawCtx, MsdfFont font, String text, int size,
                                      float x, float y, float maxWidth, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return;
        }
        if (drawCtx.textWidthPhysical(font, text, size) <= maxWidth) {
            drawCtx.text(font, text, size, x, y, color);
            return;
        }

        int visibleLength = text.length();
        while (visibleLength > 0
                && drawCtx.textWidthPhysical(font, text.substring(0, visibleLength), size) > maxWidth) {
            visibleLength--;
        }
        if (visibleLength == 0) {
            return;
        }

        float fadeWidth = Math.min(12.0f, maxWidth * 0.35f);
        int fadeStart = visibleLength;
        while (fadeStart > 0 && drawCtx.textWidthPhysical(font,
                text.substring(fadeStart, visibleLength), size) < fadeWidth) {
            fadeStart--;
        }
        String solid = text.substring(0, fadeStart);
        String fading = text.substring(fadeStart, visibleLength);
        drawCtx.text(font, solid, size, x, y, color);
        float fadeX = x + drawCtx.textWidthPhysical(font, solid, size);
        drawCtx.textWithHorizontalGradient(font, fading, size, fadeX, y, color,
                color & 0x00FFFFFF);
    }

    public static void drawScrollingText(DrawCtx drawCtx, MsdfFont font, String text, int size,
                                         float x, float y, float maxWidth, float progress, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return;
        }
        float textWidth = drawCtx.textWidthPhysical(font, text, size);
        if (textWidth <= maxWidth) {
            drawCtx.text(font, text, size, x, y, color);
            return;
        }
        float clampedProgress = Math.max(0.0f, Math.min(1.0f, progress));
        float offset = (textWidth - maxWidth) * clampedProgress;
        float scale = drawCtx.layoutContext().scaleFactor();
        drawCtx.drawEngine().beginScissor(drawCtx.matrixStack().last().pose(),
                drawCtx.layoutContext().toPhysical(x),
                drawCtx.layoutContext().toPhysical(y - 1.0f),
                drawCtx.layoutContext().toPhysical(maxWidth),
                drawCtx.layoutContext().toPhysical(font.getHeight(Math.max(1.0f, size * scale)) / scale + 2.0f));
        if (clampedProgress < 0.999f) {
            drawFadingText(drawCtx, font, text, size, x, y, maxWidth,
                    multiplyAlpha(color, 1.0f - clampedProgress));
        }
        if (clampedProgress > 0.001f) {
            drawCtx.text(font, text, size, x - offset, y,
                    multiplyAlpha(color, clampedProgress));
        }
        drawCtx.drawEngine().endScissor();
    }

    private static int multiplyAlpha(int color, float multiplier) {
        int alpha = (color >>> 24) & 0xFF;
        int scaledAlpha = Math.round(alpha * Math.max(0.0f, Math.min(1.0f, multiplier)));
        return (color & 0x00FFFFFF) | (scaledAlpha << 24);
    }

    public static void drawDivider(DrawCtx drawCtx, float x, float y, float width) {
        drawCtx.hairline(x, y, width, outline(drawCtx.colorStack()));
    }

    public static void drawWorkspacePanel(DrawCtx drawCtx, float x, float y, float width,
                                          float height, float radius) {
        drawCtx.fillRoundedRect(x, y, width, height, radius, hoverTint(drawCtx.colorStack(), 0.04f));
    }

    static int blurTexture() {
        if (Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen) {
            return Raven.INSTANCE.widgetStack().hudBlurTexture();
        }
        if (FramebufferUtil.hasValidColorAttachment(
                Raven.INSTANCE.windowController().menuUiBlur().getBlurFramebuffer())) {
            return FramebufferUtil.getColorAttachmentId(
                    Raven.INSTANCE.windowController().menuUiBlur().getBlurFramebuffer());
        }
        return sceneBlurTexture();
    }

    static int sceneBlurTexture() {
        if (Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen) {
            return Raven.INSTANCE.widgetStack().hudBlurTexture();
        }
        int blurTexture = 0;
        if (FramebufferUtil.hasValidColorAttachment(
                Raven.INSTANCE.windowController().menuSceneBlur().getBlurFramebuffer())) {
            blurTexture = FramebufferUtil.getColorAttachmentId(
                    Raven.INSTANCE.windowController().menuSceneBlur().getBlurFramebuffer());
        }
        if (blurTexture == 0) {
            blurTexture = Raven.INSTANCE.widgetStack().hudBlurTexture();
        }
        return blurTexture;
    }

    public static String tabGlyph(String legacyName) {
        return switch (legacyName) {
            case "mines" -> "a";
            case "player" -> "b";
            case "misc" -> "c";
            case "visuals" -> "d";
            case "interface" -> "e";
            case "events" -> "f";
            default -> "a";
        };
    }

    public static int fittedGlyphSize(MsdfFont font, String glyph, float visibleSize) {
        int glyphIndex = glyph == null || glyph.isEmpty() ? -1 : font.glyphIndex(glyph.codePointAt(0));
        if (glyphIndex < 0) {
            return Math.round(visibleSize);
        }
        float extent = Math.max(font.planeWidth[glyphIndex], font.planeHeight[glyphIndex]);
        return Math.round(visibleSize / Math.max(extent, 0.01f));
    }

    public static void drawCenteredIcon(DrawCtx drawCtx, MsdfFont font, String glyph, int size,
                                        float x, float y, float width, float height, int color) {
        int glyphIndex = glyph == null || glyph.isEmpty() ? -1 : font.glyphIndex(glyph.codePointAt(0));
        if (glyphIndex < 0) {
            float drawX = x + (width - drawCtx.textWidthPhysical(font, glyph, size)) / 2.0f;
            float drawY = y + (height - font.getHeight(size)) / 2.0f;
            drawCtx.text(font, glyph, size, drawX, drawY, color);
            return;
        }

        float visibleWidth = font.planeWidth[glyphIndex] * size;
        float visibleHeight = font.planeHeight[glyphIndex] * size;
        float ascent = font.metrics().ascent() != 0.0f
                ? font.metrics().ascent() : font.metrics().lineHeight();
        float visibleTopOffset = (ascent - font.bearingY[glyphIndex]) * size;
        float drawX = x + (width - visibleWidth) / 2.0f;
        float drawY = y + (height - visibleHeight) / 2.0f - visibleTopOffset;
        drawCtx.text(font, glyph, size, drawX, drawY, color);
    }

}
