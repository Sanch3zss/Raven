package sanch3zss.raven.ui.hud.impl;

import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import com.mojang.blaze3d.vertex.PoseStack;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BooleanSupplier;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.server.ServerUtil;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.interpolation.Easings;

public class WatermarkWidget extends Draggable {
    private static final String RAVEN_ICON = "a";
    float computedWidth;
    float computedHeight;
    final MsdfFont font;
    final MsdfFont clientIconFont;
    final MsdfFont watermarkIconFont;
    public final OrderedEnumSetting<HudInfoType> hudLines;
    public final BooleanSetting use12hFormat;
    private final Map<HudInfoType, AnimatedFloat> elementPositions = new EnumMap<>(HudInfoType.class);
    private final Map<HudInfoType, ToggleAnimator> elementVisibility = new EnumMap<>(HudInfoType.class);
    private final Set<HudInfoType> selectedElements = EnumSet.noneOf(HudInfoType.class);
    private final Set<HudInfoType> pendingAppearance = EnumSet.noneOf(HudInfoType.class);
    private final Map<HudInfoType, Long> appearanceAfter = new EnumMap<>(HudInfoType.class);
    private final AnimatedFloat widthAnimation = new AnimatedFloat(220, Easings.EASE_IN_OUT_CUBIC);
    private boolean positionsInitialized;

    public WatermarkWidget(BooleanSupplier booleanSupplier) {
        super("Watermark", booleanSupplier);
        this.font = Fonts.INTER_BOLD.get();
        this.clientIconFont = Fonts.CLIENT_ICON.get();
        this.watermarkIconFont = Fonts.WATERMARK.get();
        this.hudLines = new OrderedEnumSetting<HudInfoType>(Lang.WIDGET_WATERMARK_HUD_LINES)
                .options(HudInfoType.LATENCY, HudInfoType.FRAMERATE,
                        HudInfoType.CURRENT_TIME, HudInfoType.SERVER_NAME)
                .ordered(HudInfoType.LATENCY, HudInfoType.FRAMERATE,
                        HudInfoType.CURRENT_TIME, HudInfoType.SERVER_NAME)
                .selected(HudInfoType.LATENCY, HudInfoType.FRAMERATE);
        this.use12hFormat = new BooleanSetting(Lang.WIDGET_WATERMARK_USE_12H_FORMAT).visible(() -> {
            return this.hudLines.isSelected(HudInfoType.CURRENT_TIME);
        });
        for (HudInfoType type : HudInfoType.values()) {
            this.elementPositions.put(type, new AnimatedFloat(220, Easings.EASE_IN_OUT_CUBIC));
            this.elementVisibility.put(type,
                    new ToggleAnimator(160, Easings.EASE_IN_OUT_QUAD));
        }
        addSettings(this.use12hFormat, this.hudLines);
        this.x = 10.0f;
        this.y = 10.0f;
    }

    @Override
    public void layout(DragRenderContext dragRenderContext) {
        String lineText;
        if (isVisible()) {
            Mc mc = Mc.INSTANCE;
            float width = 8.0f + this.clientIconFont.getWidth(RAVEN_ICON, 14.0f)
                    + 3.0f
                    + this.font.getWidth("Raven", 11.0f) + 8.0f;
            Set<HudInfoType> activeElements = EnumSet.noneOf(HudInfoType.class);
            for (HudInfoType e : this.hudLines.orderedValues()) {
                if (e != HudInfoType.NICKNAME && this.hudLines.isSelected(e) && (lineText = textFor(e, mc)) != null && !lineText.isEmpty()) {
                    AnimatedFloat position = this.elementPositions.get(e);
                    ToggleAnimator visibility = this.elementVisibility.get(e);
                    boolean newlySelected = !this.selectedElements.contains(e);
                    if (this.positionsInitialized && !newlySelected) {
                        position.destination(width);
                    } else {
                        position.set(width);
                    }
                    if (!this.positionsInitialized) {
                        visibility.force(true);
                    } else if (newlySelected) {
                        visibility.force(false);
                        this.pendingAppearance.add(e);
                        this.appearanceAfter.put(e, System.currentTimeMillis() + 240L);
                    }
                    this.selectedElements.add(e);
                    activeElements.add(e);
                    width += this.watermarkIconFont.getWidth(iconFor(e), 12.0f) + 3.0f
                            + this.font.getWidth(lineText, fontSizeFor(e)) + 8.0f;
                }
            }
            for (HudInfoType type : HudInfoType.values()) {
                if (!activeElements.contains(type)) {
                    this.selectedElements.remove(type);
                    this.pendingAppearance.remove(type);
                    this.appearanceAfter.remove(type);
                    this.elementVisibility.get(type).state(false);
                }
            }
            float targetWidth = width;
            if (this.positionsInitialized) {
                this.widthAnimation.destination(targetWidth);
            } else {
                this.widthAnimation.set(targetWidth);
                this.positionsInitialized = true;
            }
            this.computedWidth = this.widthAnimation.animatedValue();
            this.computedHeight = 20.0f;
        }
    }

    @Override
    public void render(DragRenderContext dragRenderContext) {
        if (isVisible()) {
            Mc mc = Mc.INSTANCE;
            DrawEngine drawEngine = dragRenderContext.drawEngine();
            ColorStack colorStack = drawEngine.colorStack();
            PoseStack matrixStack = dragRenderContext.matrixStack();
            ThemePalette palette = dragRenderContext.theme().palette();
            float f = this.y + (this.computedHeight / 2.0f);
            drawGlassPanel(dragRenderContext, this.x, this.y, this.computedWidth, this.computedHeight);
            float iconX = this.x + 8.0f;
            drawEngine.msdfFont(matrixStack.last().pose(), this.clientIconFont, RAVEN_ICON,
                    iconX, centeredGlyphY(this.clientIconFont, RAVEN_ICON, 14.0f,
                            this.y, this.computedHeight),
                    14.0f, 0.05f, colorStack.computeColor(palette.accent().argb()));
            float f2 = iconX + this.clientIconFont.getWidth(RAVEN_ICON, 14.0f) + 3.0f;
            String clientName = "Raven";
            drawEngine.msdfFont(matrixStack.last().pose(), this.font, clientName, f2, f - (this.font.getHeight(11.0f) / 2.0f), 11.0f, 0.05f, colorStack.computeColor(palette.text().tone(300).argb()));
            for (HudInfoType e : this.hudLines.orderedValues()) {
                if (e != HudInfoType.NICKNAME && this.hudLines.isSelected(e)) {
                    String icon = iconFor(e);
                    String lineText = textFor(e, mc);
                    if (lineText != null && !lineText.isEmpty()) {
                        colorStack.push();
                        colorStack.alpha(this.elementVisibility.get(e).smoothAnimation());
                        float width = this.x + this.elementPositions.get(e).animatedValue();
                        drawEngine.msdfFont(matrixStack.last().pose(), this.watermarkIconFont, icon,
                                width, centeredGlyphY(this.watermarkIconFont, icon, 12.0f,
                                        this.y, this.computedHeight), 12.0f, 0.05f,
                                colorStack.computeColor(palette.accent().argb()));
                        float f3 = width + this.watermarkIconFont.getWidth(icon, 12.0f) + 3.0f;
                        int fontSize = fontSizeFor(e);
                        drawEngine.msdfFont(matrixStack.last().pose(), this.font, lineText, f3, f - (this.font.getHeight(fontSize) / 2.0f), fontSize, 0.05f, colorStack.computeColor(palette.text().tone(300).argb()));
                        colorStack.pop();
                    }
                }
            }
        }
    }

    private static float centeredGlyphY(MsdfFont font, String glyph, float size,
                                        float top, float height) {
        int glyphIndex = font.glyphIndex(glyph.codePointAt(0));
        if (glyphIndex < 0) {
            return top + (height - font.getHeight(size)) / 2.0f;
        }
        float visibleHeight = font.planeHeight[glyphIndex] * size;
        float ascent = font.metrics().ascent() != 0.0f
                ? font.metrics().ascent() : font.metrics().lineHeight();
        float visibleTopOffset = (ascent - font.bearingY[glyphIndex]) * size;
        return top + (height - visibleHeight) / 2.0f - visibleTopOffset;
    }

    private void drawGlassPanel(DragRenderContext context, float x, float y, float width, float height) {
        DrawEngine drawEngine = context.drawEngine();
        ColorStack colors = drawEngine.colorStack();
        int blurTexture = Raven.INSTANCE.widgetStack().hudBlurTexture();
        int tint = mixRgb(sanch3zss.raven.ui.menu.MenuStyle.surfaceColor(context.theme(), 0xFF111217), context.theme().palette().accentStart().argb(), 0.075f);
        HudGlassPanel.drawShadows(drawEngine, context.matrixStack().last().pose(), x, y,
                width, height, 6.0f, 25.5f,
                colors.computeColor(tint, 1.5f));
        if (blurTexture != 0) {
            drawEngine.roundedGlassPanel(context.matrixStack().last().pose(), x, y, width, height,
                    6.0f, 0.0f, 0.8f,
                    colors.computeColor(tint, 1.0f),
                    0, blurTexture);
        } else {
            drawEngine.roundedRectangle(context.matrixStack().last().pose(), x, y, width, height,
                    6.0f, colors.computeColor(tint, 0.96f));
        }
    }

    private static int mixRgb(int base, int tint, float amount) {
        float inverse = 1.0f - amount;
        int red = Math.round(((base >> 16) & 0xFF) * inverse + ((tint >> 16) & 0xFF) * amount);
        int green = Math.round(((base >> 8) & 0xFF) * inverse + ((tint >> 8) & 0xFF) * amount);
        int blue = Math.round((base & 0xFF) * inverse + (tint & 0xFF) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @Override
    public boolean click(MouseButtonInput2 mouseButtonInput2, boolean z) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput mouseMoveInput, boolean z) {
        return z && !mouseMoveInput.intercepted();
    }

    @Override
    public void animate(WeightedEngine weightedEngine) {
        this.widthAnimation.animate(weightedEngine);
        this.elementPositions.values().forEach(position -> position.animate(weightedEngine));
        long now = System.currentTimeMillis();
        for (HudInfoType type : EnumSet.copyOf(this.pendingAppearance)) {
            if (now >= this.appearanceAfter.getOrDefault(type, 0L)) {
                this.elementVisibility.get(type).state(true);
                this.pendingAppearance.remove(type);
                this.appearanceAfter.remove(type);
            }
        }
        this.elementVisibility.values().forEach(visibility -> visibility.animate(weightedEngine));
    }

    @Override
    public void update() {
    }

    public String iconFor(HudInfoType hudInfoType) {
        return switch (hudInfoType) {
            case NICKNAME -> null;
            case LATENCY -> "d";
            case FRAMERATE -> "a";
            case CURRENT_TIME -> "b";
            case SERVER_NAME -> "c";
        };
    }

    public int fontSizeFor(HudInfoType hudInfoType) {
        return hudInfoType == HudInfoType.NICKNAME ? 11 : 10;
    }

    public String textFor(HudInfoType hudInfoType, Mc mc) {
        return switch (hudInfoType) {
            case NICKNAME -> "Raven";
            case LATENCY -> getPing() + "ms";
            case FRAMERATE -> mc.getCurrentFps() + " FPS";
            case CURRENT_TIME -> formatTime();
            case SERVER_NAME -> serverAddress();
        };
    }

    public String formatTime() {
        return LocalTime.now().format(this.use12hFormat.isValue() ? DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH) : DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String serverAddress() {
        String serverIp = ServerUtil.getServerIp();
        return serverIp == null ? "" : serverIp.toLowerCase();
    }

    public int getPing() {
        if (Mc.INSTANCE.isWorldLoaded()) {
            return Mc.INSTANCE.getNetworkHandler().getOnlinePlayers().stream()
                    .filter(entry -> entry.getProfile().id().equals(Mc.INSTANCE.getPlayer().getUUID()))
                    .findFirst()
                    .map(net.minecraft.client.multiplayer.PlayerInfo::getLatency)
                    .orElse(0);
        }
        return 0;
    }

    @Override
    public float width() {
        return this.computedWidth;
    }

    @Override
    public float height() {
        return this.computedHeight;
    }
}
