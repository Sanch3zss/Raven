package sanch3zss.raven.ui.hud.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.misc.ClickGuiModule;
import sanch3zss.raven.ui.hud.impl.HotkeyEntry;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import sanch3zss.raven.render.animation.AnimatedFloat;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.interpolation.Easings;

public class HotkeysWidget extends Draggable {
    private final MsdfFont textFont = Fonts.ZENITH_MEDIUM.get();
    private final MsdfFont iconFont = Fonts.ETC.get();
    private final AnimatedFloat visibilityAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_QUAD);
    private final AnimatedFloat leftWidthAnimation = new AnimatedFloat(260, Easings.EASE_IN_OUT_QUAD);
    private final AnimatedFloat rightWidthAnimation = new AnimatedFloat(260, Easings.EASE_IN_OUT_QUAD);
    private final List<HotkeyEntry> entries = new ArrayList();
    private final Map<String, HotkeyEntry> entryMap = new HashMap();
    private float width = 51.85f;
    private float height = 12.5f;
    private boolean hadVisibleRows;

    public HotkeysWidget(BooleanSupplier visibility) {
        super("Hotkeys", visibility);
        this.x = 10.0f;
        this.y = 49.0f;
        this.leftWidthAnimation.set(0.0f);
        this.rightWidthAnimation.set(0.0f);
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    @Override
    protected float baseScale() {
        return 1.75f;
    }

    public boolean isRendering() {
        return !this.visibilityAnimation.isZero();
    }

    @Override
    public boolean click(MouseButtonInput2 input, boolean hovered) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput input, boolean hovered) {
        return hovered && !input.intercepted();
    }

    @Override
    public void layout(DragRenderContext context) {
        if (!isVisible()) {
            return;
        }
        float targetLeftWidth = 0.0f;
        float targetRightWidth = 0.0f;
        float targetHeight = 12.5f;
        float iconWidth = glyphVisibleWidth("a", 7.5f);
        boolean first = true;
        for (HotkeyEntry entry : this.entries) {
            float animation = Math.min(entry.anim.smoothAnimation(), 1.0f);
            if (animation <= 0.001f) {
                continue;
            }
            String key = KeyboardUtil.formatCompactCombination(entry.keys);
            targetLeftWidth = Math.max(targetLeftWidth, 3.5f + iconWidth + 3.5f + 0.6f + 3.5f + inkWidth(key) + 3.5f);
            targetRightWidth = Math.max(targetRightWidth, 3.5f + inkWidth(entry.name) + 3.5f);
            targetHeight += (first ? 2f : 0.75f) * animation;
            targetHeight += 11.5f * animation;
            first = false;
        }
        float headerWidth = headerWidth();
        if (targetLeftWidth > 0.0f) {
            targetRightWidth = Math.max(targetRightWidth, headerWidth - 2.0f - targetLeftWidth);
        }
        boolean hasVisibleRows = targetRightWidth > 0.0f;
        if (hasVisibleRows && !this.hadVisibleRows) {
            this.leftWidthAnimation.set(targetLeftWidth);
            this.rightWidthAnimation.set(targetRightWidth);
        } else {
            this.leftWidthAnimation.destination(targetLeftWidth);
            this.rightWidthAnimation.destination(targetRightWidth);
        }
        this.hadVisibleRows = hasVisibleRows;
        float contentWidth = this.leftWidthAnimation.animatedValue() + 2.0f
                + this.rightWidthAnimation.animatedValue();
        this.width = Math.max(headerWidth, contentWidth);
        this.height = targetHeight;
    }

    @Override
    public void render(DragRenderContext context) {
        if (!isVisible() || this.visibilityAnimation.isZero()) {
            return;
        }
        DrawEngine drawEngine = context.drawEngine();
        PoseStack matrices = context.matrixStack();
        ColorStack colors = drawEngine.colorStack();
        ThemePalette palette = context.theme().palette();
        colors.push();
        colors.alpha(this.visibilityAnimation.animatedValue());
        drawHeader(context, this.x);

        float rowY = this.y + 12.5f + 2f;
        boolean first = true;
        for (HotkeyEntry entry : this.entries) {
            float animation = Math.min(entry.anim.smoothAnimation(), 1.0f);
            if (animation <= 0.001f) {
                continue;
            }
            if (!first) {
                rowY += 0.75f * animation;
            }
            float columnLeftWidth = this.leftWidthAnimation.animatedValue();
            float rowRightWidth = 3.5f + inkWidth(entry.name) + 3.5f;
            float smallX = this.x;
            float largeX = smallX + columnLeftWidth + 2.0f;
            colors.push();
            colors.alpha(animation);
            drawInfoCell(context, smallX, rowY, columnLeftWidth);
            drawInfoCell(context, largeX, rowY, rowRightWidth);
            colors.pop();
            colors.push();
            colors.alpha(animation * animation);
            float textY = rowY + (11.5f - this.textFont.getHeight(7.5f)) / 2.0f;
            String key = KeyboardUtil.formatCompactCombination(entry.keys);
            float stateIconWidth = glyphVisibleWidth("a", 7.5f);
            float stateIconX = smallX + 3.5f;
            float dividerX = stateIconX + stateIconWidth + 3.5f;
            drawCenteredIcon(context, "a", 7.5f,
                    stateIconX, rowY, stateIconWidth, 11.5f,
                    colors.computeColor(palette.text().tone(50).argb()));
            drawDivider(context, dividerX, rowY + 11.5f / 2.0f, palette);
            float keyStart = dividerX + 0.6f + 3.5f;
            float keyEnd = smallX + columnLeftWidth - 3.5f;
            float keyX = keyStart + (keyEnd - keyStart - inkWidth(key)) / 2.0f;
            float scaleX = Math.abs(matrices.last().pose().m00());
            keyX = snapToPixel(keyX, matrices.last().pose().m30(), scaleX);
            float nameX = snapToPixel(largeX + 3.5f, matrices.last().pose().m30(), scaleX);
            drawEngine.msdfFont(matrices.last().pose(), this.textFont, key,
                    keyX, textY, 7.5f, 0.0f,
                    colors.computeColor(palette.text().tone(100).argb()));
            drawEngine.msdfFont(matrices.last().pose(), this.textFont, entry.name,
                    nameX, textY, 7.5f, 0.0f,
                    colors.computeColor(palette.text().tone(50).argb()));
            colors.pop();
            rowY += 11.5f * animation;
            first = false;
        }
        colors.pop();
    }

    @Override
    public void animate(WeightedEngine engine) {
        if (Mc.INSTANCE.getPlayer() == null) {
            return;
        }
        this.entries.forEach(entry -> entry.anim.animate(engine));
        this.visibilityAnimation.animate(engine);
        this.leftWidthAnimation.animate(engine);
        this.rightWidthAnimation.animate(engine);
    }

    @Override
    public void update() {
        if (!isVisible()) {
            return;
        }
        Set<String> activeKeys = new HashSet();
        for (Module module : Raven.INSTANCE.moduleRepository().getModules()) {
            if (module.isBlocked() || module instanceof ClickGuiModule) {
                continue;
            }
            if (module.hasKeyBind()) {
                updateEntry(activeKeys, "m:" + module.getName(),
                        () -> new HotkeyEntry(module, module.getName(), module.getKeyBind()),
                        module.getKeyBind(), module.isState());
            }
            if (!module.isState()) {
                continue;
            }
            for (Setting setting : module.getSettings()) {
                if (setting instanceof BooleanSetting booleanSetting && booleanSetting.getKey() != -1) {
                    String name = booleanSetting.getName().effective();
                    updateEntry(activeKeys, "s:" + module.getName() + ":" + name,
                            () -> new HotkeyEntry(module, name, booleanSetting.getKeyBind()), booleanSetting.getKeyBind(), booleanSetting.isValue());
                }
                if (setting instanceof ExpandableSetting expandableSetting) {
                    String groupName = expandableSetting.getName().effective();
                    if (expandableSetting.getKey() != -1) {
                        updateEntry(activeKeys, "s:" + module.getName() + ":" + groupName,
                                () -> new HotkeyEntry(module, groupName, expandableSetting.getKeyBind()), expandableSetting.getKeyBind(), expandableSetting.isValue());
                    }
                    for (Setting child : expandableSetting.getSubSettings()) {
                        if (child instanceof BooleanSetting booleanSetting && booleanSetting.getKey() != -1) {
                            String name = groupName + "/" + booleanSetting.getName().effective();
                            updateEntry(activeKeys, "s:" + module.getName() + ":" + name,
                                    () -> new HotkeyEntry(module, name, booleanSetting.getKeyBind()), booleanSetting.getKeyBind(), booleanSetting.isValue());
                        }
                    }
                }
            }
        }
        this.entryMap.keySet().retainAll(activeKeys);
        this.entries.clear();
        Stream<HotkeyEntry> sorted = this.entryMap.values().stream().sorted(
                Comparator.<HotkeyEntry>comparingDouble(entry -> -this.textFont.getWidth(entry.name, 7.5f))
                        .thenComparing(entry -> entry.name, String.CASE_INSENSITIVE_ORDER));
        List<HotkeyEntry> target = this.entries;
        Objects.requireNonNull(target);
        sorted.forEach(target::add);
        this.visibilityAnimation.destination(1.0f);
    }

    private void updateEntry(Set<String> keys, String id, Supplier<HotkeyEntry> supplier, List<Integer> binding, boolean active) {
        keys.add(id);
        HotkeyEntry entry = this.entryMap.computeIfAbsent(id, ignored -> supplier.get());
        entry.keys.clear();
        if (binding != null) {
            entry.keys.addAll(binding);
        }
        entry.anim.state(active);
    }

    private void drawHeader(DragRenderContext context, float headerX) {
        DrawEngine drawEngine = context.drawEngine();
        PoseStack matrices = context.matrixStack();
        ColorStack colors = drawEngine.colorStack();
        ThemePalette palette = context.theme().palette();
        drawGlassPanel(context, headerX, this.y, headerWidth(), 12.5f);
        float slotWidth = glyphVisibleWidth("a", 7.5f);
        float dividerX = headerX + 3.5f + slotWidth + 3.5f;
        drawCenteredIcon(context, "f", 8f, headerX + 3.5f, this.y,
                slotWidth, 12.5f,
                colors.computeColor(palette.accent().argb()));
        drawDivider(context, dividerX, this.y + 12.5f / 2.0f, palette);
        float textY = this.y + (12.5f - this.textFont.getHeight(7.5f)) / 2.0f;
        float titleX = snapToPixel(dividerX + 0.6f + 3.5f, matrices.last().pose().m30(),
                Math.abs(matrices.last().pose().m00()));
        drawEngine.msdfFont(matrices.last().pose(), this.textFont, "Hotkeys",
                titleX, textY, 7.5f, 0.0f,
                colors.computeColor(palette.text().tone(50).argb()));
    }

    private float headerWidth() {
        return 3.5f + glyphVisibleWidth("a", 7.5f) + 3.5f + 0.6f + 3.5f + inkWidth("Hotkeys") + 3.5f;
    }

    private float inkWidth(String text) {
        if (text.isEmpty()) {
            return 0.0f;
        }
        float width = this.textFont.getWidth(text, 7.5f);
        int last = this.textFont.glyphIndex(text.codePointBefore(text.length()));
        if (last < 0) {
            return width;
        }
        return width - (this.textFont.advance[last] - this.textFont.planeWidth[last]) * 7.5f;
    }

    private float glyphVisibleWidth(String glyph, float size) {
        int glyphIndex = this.iconFont.glyphIndex(glyph.codePointAt(0));
        return glyphIndex < 0 ? 0.0f : this.iconFont.planeWidth[glyphIndex] * size;
    }

    private void drawCenteredIcon(DragRenderContext context, String glyph, float size,
                                  float boxX, float boxY, float boxWidth, float boxHeight, int color) {
        int glyphIndex = this.iconFont.glyphIndex(glyph.codePointAt(0));
        if (glyphIndex < 0) {
            return;
        }
        float visibleWidth = this.iconFont.planeWidth[glyphIndex] * size;
        float visibleHeight = this.iconFont.planeHeight[glyphIndex] * size;
        float ascent = this.iconFont.metrics().ascent() != 0.0f
                ? this.iconFont.metrics().ascent()
                : this.iconFont.metrics().lineHeight();
        float drawX = boxX + (boxWidth - visibleWidth) / 2.0f;
        float drawY = boxY + (boxHeight - visibleHeight) / 2.0f
                - (ascent - this.iconFont.bearingY[glyphIndex]) * size;
        context.drawEngine().msdfFont(context.matrixStack().last().pose(),
                this.iconFont, glyph, drawX, drawY, size, 0.0f, color);
    }

    private void drawDivider(DragRenderContext context, float dividerX, float centerY,
                             ThemePalette palette) {
        DrawEngine drawEngine = context.drawEngine();
        PoseStack matrices = context.matrixStack();
        float scaleX = Math.abs(matrices.last().pose().m00());
        float scaleY = Math.abs(matrices.last().pose().m11());
        float x = snapToPixel(dividerX, matrices.last().pose().m30(), scaleX);
        float physicalHeight = Math.max(1.0f, Math.round(4.0f * scaleY));
        float y = snapCenteredToPixel(centerY, physicalHeight,
                matrices.last().pose().m31(), scaleY);
        float width = Math.max(1.0f, Math.round(0.6f * scaleX)) / scaleX;
        float height = physicalHeight / scaleY;
        drawEngine.roundedRectangle(matrices.last().pose(), x, y, width, height,
                width / 2.0f,
                drawEngine.colorStack().computeColor(palette.text().tone(100).argb(), 0.38f));
    }

    private static float snapToPixel(float value, float translation, float scale) {
        return scale <= 0.0f ? value : (Math.round(value * scale + translation) - translation) / scale;
    }

    private static float snapCenteredToPixel(float center, float physicalHeight,
                                             float translation, float scale) {
        if (scale <= 0.0f) return center - 4.0f / 2.0f;
        float topPixel = Math.round(center * scale + translation - physicalHeight / 2.0f);
        return (topPixel - translation) / scale;
    }

    private void drawInfoCell(DragRenderContext context, float cellX, float rowY, float cellWidth) {
        HudGlassPanel.drawInfoCell(context.drawEngine(), context.matrixStack().last().pose(),
                Raven.INSTANCE.widgetStack().hudBlurTexture(),
                cellX, rowY, cellWidth, 11.5f, 4.0f);
    }

    private void drawGlassPanel(DragRenderContext context, float x, float y, float width, float height) {
        DrawEngine drawEngine = context.drawEngine();
        PoseStack matrices = context.matrixStack();
        ColorStack colors = drawEngine.colorStack();
        int blurTexture = Raven.INSTANCE.widgetStack().hudBlurTexture();
        int tint = mixRgb(sanch3zss.raven.ui.menu.MenuStyle.surfaceColor(context.theme(), 0xFF111217), context.theme().palette().accentStart().argb(), 0.075f);
        HudGlassPanel.drawShadows(drawEngine, matrices.last().pose(), x, y, width, height,
                4.0f, 25.5f,
                colors.computeColor(tint, 1.5f));
        if (blurTexture != 0) {
            drawEngine.roundedGlassPanel(matrices.last().pose(), x, y, width, height,
                    4.0f, 0.0f, 0.8f,
                    colors.computeColor(tint, 1.0f),
                    0, blurTexture);
        } else {
            drawEngine.roundedRectangle(matrices.last().pose(), x, y, width, height,
                    4.0f, colors.computeColor(tint, 0.96f));
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
