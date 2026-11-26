package sanch3zss.raven.ui.hud.impl;

import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.*;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.inventory.*;
import sanch3zss.raven.feature.inventory.InventoryUtil;
import sanch3zss.raven.render.animation.AnimatedFloat;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.util.math.interpolation.Easings;

public class PotionListWidget extends Draggable {
    private final MsdfFont textFont = Fonts.ZENITH_MEDIUM.get();
    private final MsdfFont iconFont = Fonts.ETC.get();
    private final Map<PotionKey, PotionEntry> entries = new HashMap();
    private final AnimatedFloat visibilityAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_QUAD);
    private final AnimatedFloat leftWidthAnimation = new AnimatedFloat(260, Easings.EASE_IN_OUT_QUAD);
    private final AnimatedFloat rightWidthAnimation = new AnimatedFloat(260, Easings.EASE_IN_OUT_QUAD);
    private float width = 65.5f;
    private float height = 12.0f;
    private int frameCounter;
    private boolean hadVisibleRows;

    public PotionListWidget(BooleanSupplier visibility) {
        super("PotionList", visibility);
        this.x = 119.0f;
        this.y = 49.0f;
        this.leftWidthAnimation.set(0.0f);
        this.rightWidthAnimation.set(0.0f);
    }

    @Override
    public void update() {
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

    private List<PotionEntry> visibleEntries() {
        return this.entries.values().stream()
                .filter(entry -> !entry.animator.isZero() && entry.effect != null)
                .sorted(Comparator.<PotionEntry>comparingDouble(entry ->
                                -this.textFont.getWidth(entry.getDisplayName(), 7.5f))
                        .thenComparing(PotionEntry::getDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public void layout(DragRenderContext context) {
        if (!isVisible()) {
            return;
        }
        float targetLeftWidth = 0.0f;
        float targetRightWidth = 0.0f;
        float targetHeight = 12.0f;
        boolean first = true;
        for (PotionEntry entry : visibleEntries()) {
            float animation = Math.min(entry.animator.smoothAnimation(), 1.0f);
            targetLeftWidth = Math.max(targetLeftWidth, timeCellWidth(entry));
            targetRightWidth = Math.max(targetRightWidth, 3.5f + inkWidth(entry.getDisplayName()) + 3.5f);
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
        this.width = Math.max(headerWidth, this.leftWidthAnimation.animatedValue() + 2.0f
                + this.rightWidthAnimation.animatedValue());
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
        List<PotionEntry> visible = visibleEntries();

        colors.push();
        colors.alpha(this.visibilityAnimation.animatedValue());
        drawHeader(context, this.x);
        float rowY = this.y + 12.0f + 2f;
        for (int index = 0; index < visible.size(); index++) {
            PotionEntry entry = visible.get(index);
            float animation = Math.min(entry.animator.smoothAnimation(), 1.0f);
            if (index > 0) {
                rowY += 0.75f * animation;
            }
            float columnLeftWidth = this.leftWidthAnimation.animatedValue();
            String displayName = entry.getDisplayName();
            float rowRightWidth = 3.5f + inkWidth(displayName) + 3.5f;
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
            String duration = entry.infinite ? "--:--" : entry.durationText;
            float dividerX = smallX + 3.5f + 8.0f + 3.5f;
            drawDivider(context, dividerX, rowY + 11.5f / 2.0f, palette);
            float durationStart = dividerX + 0.6f + 3.5f;
            float durationEnd = smallX + columnLeftWidth - 3.5f;
            float scaleX = Math.abs(matrices.last().pose().m00());
            float durationX = snapToPixel(durationStart + (durationEnd - durationStart - inkWidth(duration)) / 2.0f,
                    matrices.last().pose().m30(), scaleX);
            float nameX = snapToPixel(largeX + 3.5f, matrices.last().pose().m30(), scaleX);
            drawEngine.msdfFont(matrices.last().pose(), this.textFont, duration,
                    durationX, textY, 7.5f, 0.0f,
                    colors.computeColor(palette.text().tone(100).argb()));
            drawEngine.msdfFont(matrices.last().pose(), this.textFont, displayName,
                    nameX, textY, 7.5f, 0.0f,
                    colors.computeColor(palette.text().tone(50).argb()));
            colors.pop();
            rowY += 11.5f * animation;
        }
        colors.pop();
    }

    @Override
    protected void collectItems(DragRenderContext context) {
        if (!isVisible() || this.visibilityAnimation.isZero()) {
            return;
        }
        List<PotionEntry> visible = visibleEntries();
        float rowY = this.y + 12.0f + 2f;
        for (int index = 0; index < visible.size(); index++) {
            PotionEntry entry = visible.get(index);
            float animation = Math.min(entry.animator.smoothAnimation(), 1.0f);
            if (index > 0) {
                rowY += 0.75f * animation;
            }
            context.drawVanillaSprite(net.minecraft.client.gui.Hud.getMobEffectSprite(entry.effect),
                    this.x + 3.5f,
                    rowY + (11.5f - 8.0f) / 2.0f,
                    8.0f,
                    this.visibilityAnimation.animatedValue() * animation * animation);
            rowY += 11.5f * animation;
        }
    }

    @Override
    public void animate(WeightedEngine engine) {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null) {
            return;
        }
        this.frameCounter++;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            PotionKey key = new PotionKey(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()), effect.getAmplifier());
            PotionEntry entry = this.entries.computeIfAbsent(key, ignored -> new PotionEntry());
            entry.lastUpdateTick = this.frameCounter;
            boolean active = effect.isInfiniteDuration() || effect.getDuration() > 0;
            entry.animator.state(active).animate(engine);
            if (active) {
                entry.hasBeenActive = true;
            }
            entry.effect = effect.getEffect();
            entry.harmful = effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
            entry.amplifier = effect.getAmplifier();
            entry.translationKey = effect.getDescriptionId();
            entry.infinite = effect.isInfiniteDuration();
            entry.duration = effect.getDuration();
            entry.durationText = InventoryUtil.INSTANCE.getPotionDuration(effect);
            if (!entry.infinite && entry.duration > entry.previousDuration + 5) {
                entry.expiryNotified = false;
            }
            if (!entry.infinite && isBuffEffect(entry.effect) && !entry.expiryNotified && entry.previousDuration > 20 && entry.duration <= 20) {
                Raven.INSTANCE.notificationRepository().post(NotificationType.INFO, buildExpiryMessage(entry), 2500L);
                entry.expiryNotified = true;
            }
            entry.previousDuration = entry.duration;
            if (entry.infinite) {
                entry.maxDuration = -1;
            } else if (entry.maxDuration <= 0 || entry.duration > entry.maxDuration) {
                entry.maxDuration = entry.duration;
            }
        }
        for (PotionEntry entry : this.entries.values()) {
            if (entry.lastUpdateTick != this.frameCounter) {
                entry.animator.state(false).animate(engine);
                entry.infinite = false;
            }
        }
        this.entries.entrySet().removeIf(entry -> this.frameCounter - entry.getValue().lastUpdateTick > 40 && entry.getValue().animator.isZero());
        this.visibilityAnimation.destination(1.0f).animate(engine);
        this.leftWidthAnimation.animate(engine);
        this.rightWidthAnimation.animate(engine);
    }

    private boolean isBuffEffect(Holder<MobEffect> effect) {
        Identifier id;
        return effect != null
                && (id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value())) != null
                && (id.equals(BuiltInRegistries.MOB_EFFECT.getKey(MobEffects.STRENGTH.value()))
                || id.equals(BuiltInRegistries.MOB_EFFECT.getKey(MobEffects.SPEED.value())));
    }

    private Component buildExpiryMessage(PotionEntry entry) {
        String name = I18n.get(entry.translationKey, new Object[0]) + (entry.amplifier > 0 ? " " + (entry.amplifier + 1) : "");
        return Component.literal("Заканчивается эффект: " + name);
    }

    private float timeCellWidth(PotionEntry entry) {
        String duration = entry.infinite ? "--:--" : entry.durationText;
        return 3.5f + 8.0f + 3.5f + 0.6f + 3.5f + inkWidth(duration) + 3.5f;
    }

    private float headerWidth() {
        return 3.5f + 8.0f + 3.5f + 0.6f + 3.5f + inkWidth("Active Potions") + 3.5f;
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

    private void drawHeader(DragRenderContext context, float headerX) {
        DrawEngine drawEngine = context.drawEngine();
        PoseStack matrices = context.matrixStack();
        ColorStack colors = drawEngine.colorStack();
        ThemePalette palette = context.theme().palette();
        drawGlassPanel(context, headerX, this.y, headerWidth(), 12.0f);
        float dividerX = headerX + 3.5f + 8.0f + 3.5f;
        drawCenteredIcon(context, "e", 8.5f, headerX + 3.5f, this.y,
                8.0f, 12.0f,
                colors.computeColor(palette.accent().argb()));
        drawDivider(context, dividerX, this.y + 12.0f / 2.0f, palette);
        float textY = this.y + (12.0f - this.textFont.getHeight(7.5f)) / 2.0f;
        float titleX = snapToPixel(dividerX + 0.6f + 3.5f, matrices.last().pose().m30(),
                Math.abs(matrices.last().pose().m00()));
        drawEngine.msdfFont(matrices.last().pose(), this.textFont, "Active Potions",
                titleX, textY, 7.5f, 0.0f,
                colors.computeColor(palette.text().tone(50).argb()));
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
