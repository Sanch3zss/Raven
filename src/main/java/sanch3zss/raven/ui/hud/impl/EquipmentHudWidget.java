package sanch3zss.raven.ui.hud.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.util.math.interpolation.Easings;

public class EquipmentHudWidget extends Draggable {
    private final MsdfFont textFont = Fonts.ZENITH_MEDIUM.get();
    private final MsdfFont iconFont = Fonts.ETC.get();
    private static final List<EquipmentSlot> EQUIPMENT_ORDER = List.of(
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
    private final Map<EquipmentSlot, EquipmentEntry> entries = new EnumMap<>(EquipmentSlot.class);
    private final AnimatedFloat widthAnimation = new AnimatedFloat(260, Easings.EASE_IN_OUT_QUAD);
    private float width = 43.0f;
    private float height = 12.5f;

    public EquipmentHudWidget(BooleanSupplier visibility) {
        super("EquipmentHud", visibility);
        this.x = 36.0f;
        this.y = 180.0f;
        this.widthAnimation.set(43.0f);
        for (EquipmentSlot slot : EQUIPMENT_ORDER) {
            this.entries.put(slot, new EquipmentEntry());
        }
    }

    @Override
    public void layout(DragRenderContext context) {
        syncEquipment();
        List<EquipmentEntry> equipment = visibleEquipment();
        float nextWidth = 3.5f + 8.0f + 3.5f;
        float targetHeight = 12.5f;
        boolean first = true;
        for (EquipmentEntry entry : equipment) {
            float animation = Math.min(entry.animation.smoothAnimation(), 1.0f);
            if (animation <= 0.001f) {
                continue;
            }
            String value = equipmentValue(entry.stack);
            if (!value.isEmpty()) {
                nextWidth = Math.max(nextWidth, 3.5f + 8.0f + 3.5f + 0.6f + 3.5f + inkWidth(value) + 3.5f);
            }
            targetHeight += (first ? 2.0f : 0.75f) * animation;
            targetHeight += 11.5f * animation;
            first = false;
        }
        this.widthAnimation.destination(nextWidth);
        this.width = Math.max(headerWidth(), this.widthAnimation.animatedValue());
        this.height = targetHeight;
    }

    @Override
    public void render(DragRenderContext context) {
        if (!isVisible()) {
            return;
        }
        DrawEngine drawEngine = context.drawEngine();
        PoseStack matrices = context.matrixStack();
        ColorStack colors = drawEngine.colorStack();
        ThemePalette palette = context.theme().palette();
        drawHeader(context, this.x);
        List<EquipmentEntry> equipment = visibleEquipment();
        float rowY = this.y + 12.5f + 2.0f;
        boolean first = true;
        for (EquipmentEntry entry : equipment) {
            float animation = Math.min(entry.animation.smoothAnimation(), 1.0f);
            if (animation <= 0.001f) {
                continue;
            }
            if (!first) {
                rowY += 0.75f * animation;
            }
            ItemStack stack = entry.stack;
            String value = equipmentValue(stack);
            float rowWidth = value.isEmpty() ? 3.5f + 8.0f + 3.5f : this.widthAnimation.animatedValue();
            float rowX = this.x;
            colors.push();
            colors.alpha(animation);
            drawInfoCell(context, rowX, rowY, rowWidth);
            colors.pop();
            float itemX = rowX + 3.5f;
            colors.push();
            colors.alpha(animation * animation);
            context.drawVanillaItem(stack, itemX,
                    rowY + (11.5f - 8.0f) / 2.0f, 8.0f, false, animation);
            if (!value.isEmpty()) {
                float dividerX = itemX + 8.0f + 3.5f;
                drawDivider(context, dividerX, rowY + 11.5f / 2.0f, palette);
                float valueStart = dividerX + 0.6f + 3.5f;
                float valueEnd = rowX + rowWidth - 3.5f;
                float textX = snapToPixel(valueStart + (valueEnd - valueStart - inkWidth(value)) / 2.0f,
                        matrices.last().pose().m30(), Math.abs(matrices.last().pose().m00()));
                float textY = rowY + (11.5f - this.textFont.getHeight(7.5f)) / 2.0f;
                drawEngine.msdfFont(matrices.last().pose(), this.textFont, value,
                        textX, textY, 7.5f, 0.0f,
                        colors.computeColor(palette.text().tone(50).argb()));
            }
            colors.pop();
            rowY += 11.5f * animation;
            first = false;
        }
    }

    @Override
    protected void collectItems(DragRenderContext context) {
        if (!isVisible()) {
            return;
        }
        float rowY = this.y + 12.5f + 2.0f;
        boolean first = true;
        for (EquipmentEntry entry : visibleEquipment()) {
            float animation = Math.min(entry.animation.smoothAnimation(), 1.0f);
            if (animation <= 0.001f) {
                continue;
            }
            if (!first) {
                rowY += 0.75f * animation;
            }
            context.drawVanillaItem(entry.stack, this.x + 3.5f,
                    rowY + (11.5f - 8.0f) / 2.0f, 8.0f, false, animation);
            rowY += 11.5f * animation;
            first = false;
        }
    }

    private void drawHeader(DragRenderContext context, float headerX) {
        DrawEngine drawEngine = context.drawEngine();
        PoseStack matrices = context.matrixStack();
        ColorStack colors = drawEngine.colorStack();
        ThemePalette palette = context.theme().palette();
        drawGlassPanel(context, headerX, this.y, headerWidth(), 12.5f);
        float dividerX = headerX + 3.5f + 8.0f + 3.5f;
        drawCenteredIcon(context, "b", headerX + 3.5f, this.y, 8.0f, 12.5f,
                colors.computeColor(palette.accent().argb()));
        drawDivider(context, dividerX, this.y + 12.5f / 2.0f, palette);
        float titleX = snapToPixel(dividerX + 0.6f + 3.5f, matrices.last().pose().m30(),
                Math.abs(matrices.last().pose().m00()));
        float titleY = this.y + (12.5f - this.textFont.getHeight(7.5f)) / 2.0f;
        drawEngine.msdfFont(matrices.last().pose(), this.textFont, "Equipment",
                titleX, titleY, 7.5f, 0.0f,
                colors.computeColor(palette.text().tone(50).argb()));
    }

    private float headerWidth() {
        return 3.5f + 8.0f + 3.5f + 0.6f + 3.5f + inkWidth("Equipment") + 3.5f;
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

    private List<EquipmentEntry> visibleEquipment() {
        List<EquipmentEntry> result = new ArrayList<>(EQUIPMENT_ORDER.size());
        for (EquipmentSlot slot : EQUIPMENT_ORDER) {
            EquipmentEntry entry = this.entries.get(slot);
            if (entry != null && !entry.stack.isEmpty()
                    && (entry.animation.state() || !entry.animation.isZero())) {
                result.add(entry);
            }
        }
        return result;
    }

    private void syncEquipment() {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null) {
            this.entries.values().forEach(entry -> entry.animation.state(false));
            return;
        }
        for (EquipmentSlot slot : EQUIPMENT_ORDER) {
            EquipmentEntry entry = this.entries.get(slot);
            ItemStack current = player.getItemBySlot(slot);
            boolean present = current != null && !current.isEmpty();
            if (present) {
                entry.stack = current.copy();
            }
            entry.animation.state(present);
        }
    }

    private static String equipmentValue(ItemStack stack) {
        if (stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
            return Math.round(remaining * 100.0f / stack.getMaxDamage()) + "%";
        }
        return stack.getCount() > 1 ? "x" + stack.getCount() : "";
    }

    private void drawCenteredIcon(DragRenderContext context, String glyph, float boxX,
                                  float boxY, float boxWidth, float boxHeight, int color) {
        int glyphIndex = this.iconFont.glyphIndex(glyph.codePointAt(0));
        if (glyphIndex < 0) {
            return;
        }
        float visibleWidth = this.iconFont.planeWidth[glyphIndex] * 10.5f;
        float visibleHeight = this.iconFont.planeHeight[glyphIndex] * 10.5f;
        float ascent = this.iconFont.metrics().ascent() != 0.0f
                ? this.iconFont.metrics().ascent() : this.iconFont.metrics().lineHeight();
        float drawX = boxX + (boxWidth - visibleWidth) / 2.0f;
        float drawY = boxY + (boxHeight - visibleHeight) / 2.0f
                - (ascent - this.iconFont.bearingY[glyphIndex]) * 10.5f;
        context.drawEngine().msdfFont(context.matrixStack().last().pose(),
                this.iconFont, glyph, drawX, drawY, 10.5f, 0.0f, color);
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
        ColorStack colors = drawEngine.colorStack();
        int blurTexture = Raven.INSTANCE.widgetStack().hudBlurTexture();
        int tint = mixRgb(sanch3zss.raven.ui.menu.MenuStyle.surfaceColor(context.theme(), 0xFF111217), context.theme().palette().accentStart().argb(), 0.075f);
        HudGlassPanel.drawShadows(drawEngine, context.matrixStack().last().pose(), x, y,
                width, height, 4.0f, 25.5f,
                colors.computeColor(tint, 1.5f));
        if (blurTexture != 0) {
            drawEngine.roundedGlassPanel(context.matrixStack().last().pose(), x, y,
                    width, height, 4.0f, 0.0f, 0.8f,
                    colors.computeColor(tint, 1.0f),
                    0, blurTexture);
        } else {
            drawEngine.roundedRectangle(context.matrixStack().last().pose(),
                    x, y, width, height, 4.0f, colors.computeColor(tint, 0.96f));
        }
    }

    private static int mixRgb(int base, int tint, float amount) {
        float inverse = 1.0f - amount;
        int red = Math.round(((base >> 16) & 0xFF) * inverse + ((tint >> 16) & 0xFF) * amount);
        int green = Math.round(((base >> 8) & 0xFF) * inverse + ((tint >> 8) & 0xFF) * amount);
        int blue = Math.round((base & 0xFF) * inverse + (tint & 0xFF) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    public boolean isRendering() {
        return isVisible();
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
    public void animate(WeightedEngine engine) {
        syncEquipment();
        this.entries.values().forEach(entry -> entry.animation.animate(engine));
        this.widthAnimation.animate(engine);
    }

    @Override
    public void update() {
        syncEquipment();
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

    private static final class EquipmentEntry {
        private ItemStack stack = ItemStack.EMPTY;
        private final ToggleAnimator animation = new ToggleAnimator(220, Easings.EASE_IN_OUT_QUAD);
    }
}
