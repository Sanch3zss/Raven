package sanch3zss.raven.ui.hud.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class CooldownsWidget extends Draggable {
    private final MsdfFont textFont = Fonts.ZENITH_MEDIUM.get();
    private final MsdfFont iconFont = Fonts.ETC.get();
    private final float widestTimeDigitWidth;
    private final Map<Identifier, CooldownEntry> entries = new LinkedHashMap();
    private final Map<Identifier, RecentItem> recentItems = new HashMap<>();
    private final AnimatedFloat visibilityAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_QUAD);
    private final AnimatedFloat leftWidthAnimation = new AnimatedFloat(260, Easings.EASE_IN_OUT_QUAD);
    private final AnimatedFloat rightWidthAnimation = new AnimatedFloat(260, Easings.EASE_IN_OUT_QUAD);
    private float width = 57.0f;
    private float height = 12.0f;
    private boolean hadVisibleRows;

    public CooldownsWidget(BooleanSupplier visibility) {
        super("Cooldowns", visibility);
        this.x = 337.0f;
        this.y = 49.0f;
        this.leftWidthAnimation.set(0.0f);
        this.rightWidthAnimation.set(0.0f);
        float digitWidth = 0.0f;
        for (char digit = '0'; digit <= '9'; digit++) {
            digitWidth = Math.max(digitWidth,
                    this.textFont.getWidth(Character.toString(digit), 7.5f));
        }
        this.widestTimeDigitWidth = digitWidth;
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

    private List<CooldownEntry> visibleEntries() {
        return this.entries.values().stream().filter(entry -> !entry.animation.isZero())
                .sorted(Comparator.<CooldownEntry>comparingDouble(entry ->
                                -nameWidth(entry))
                        .thenComparing(this::nameText, String.CASE_INSENSITIVE_ORDER))
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
        for (CooldownEntry entry : visibleEntries()) {
            float animation = Math.min(entry.animation.smoothAnimation(), 1.0f);
            String time = formatTime(remainingSeconds(entry));
            float timeWidth = inkWidth(time);
            if (entry.cooldown != null) {
                int durationTicks = Math.max(0, entry.cooldown.endTime - entry.cooldown.startTime);
                int digitCount = formatTime(durationTicks / 20.0f).length() - 1;
                timeWidth = Math.max(timeWidth, digitCount * this.widestTimeDigitWidth
                        + this.textFont.getWidth(":", 7.5f));
            }
            targetLeftWidth = Math.max(targetLeftWidth, 3.5f + 8.0f + 3.5f + 0.6f + 3.5f + timeWidth + 3.5f);
            targetRightWidth = Math.max(targetRightWidth, 3.5f + nameWidth(entry) + 3.5f);
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
        List<CooldownEntry> visible = visibleEntries();

        colors.push();
        colors.alpha(this.visibilityAnimation.animatedValue());
        drawHeader(context, this.x);
        float rowY = this.y + 12.0f + 2f;
        for (int index = 0; index < visible.size(); index++) {
            CooldownEntry entry = visible.get(index);
            float animation = Math.min(entry.animation.smoothAnimation(), 1.0f);
            if (index > 0) {
                rowY += 0.75f * animation;
            }
            float columnLeftWidth = this.leftWidthAnimation.animatedValue();
            String name = nameText(entry);
            float rowRightWidth = 3.5f + nameWidth(entry) + 3.5f;
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
            String time = formatTime(remainingSeconds(entry));
            float iconX = smallX + 3.5f;
            float dividerX = iconX + 8.0f + 3.5f;
            drawDivider(context, dividerX, rowY + 11.5f / 2.0f, palette);
            float scaleX = Math.abs(matrices.last().pose().m00());
            float timeX = snapToPixel(dividerX + 0.6f + 3.5f, matrices.last().pose().m30(), scaleX);
            float nameX = snapToPixel(largeX + 3.5f, matrices.last().pose().m30(), scaleX);
            drawEngine.msdfFont(matrices.last().pose(), this.textFont, time,
                    timeX, textY, 7.5f, 0.0f,
                    colors.computeColor(palette.text().tone(100).argb()));
            int nameColor = colors.computeColor(palette.text().tone(50).argb());
            drawEngine.msdfFont(matrices.last().pose(), this.textFont, name,
                    nameX, textY, 7.5f, 0.0f, nameColor);
            context.drawVanillaItem(entry.stack, iconX,
                    rowY + (11.5f - 8.0f) / 2.0f, 8.0f, false,
                    animation);
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
        List<CooldownEntry> visible = visibleEntries();
        float rowY = this.y + 12.0f + 2f;
        for (int index = 0; index < visible.size(); index++) {
            CooldownEntry entry = visible.get(index);
            float animation = Math.min(entry.animation.smoothAnimation(), 1.0f);
            if (index > 0) {
                rowY += 0.75f * animation;
            }
            float iconX = this.x + 3.5f;
            context.drawVanillaItem(entry.stack, iconX,
                    rowY + (11.5f - 8.0f) / 2.0f, 8.0f, false,
                    animation);
            rowY += 11.5f * animation;
        }
    }

    @Override
    public void update() {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (!isVisible() || player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            captureItemName(player.getInventory().getItem(slot), now, player.getCooldowns());
        }
        captureItemName(player.getInventory().getSelectedItem(), now, player.getCooldowns());
        this.recentItems.entrySet().removeIf(entry -> now - entry.getValue().seenAt() > 10_000L);
        Set<Identifier> active = new HashSet();
        ItemCooldowns manager = player.getCooldowns();
        for (Object rawEntry : manager.cooldowns.entrySet()) {
            Map.Entry<?, ?> mapEntry = (Map.Entry<?, ?>) rawEntry;
            if (!(mapEntry.getKey() instanceof Identifier id) || !(mapEntry.getValue() instanceof ItemCooldowns.CooldownInstance cooldown)) {
                continue;
            }
            RecentItem recentItem = this.recentItems.get(id);
            ItemStack stack = recentItem == null
                    ? BuiltInRegistries.ITEM.getValue(id).getDefaultInstance() : recentItem.stack();
            if (stack.is(Items.AIR) && !id.equals(BuiltInRegistries.ITEM.getKey(Items.AIR))) {
                continue;
            }
            CooldownEntry entry = this.entries.computeIfAbsent(id, ignored -> new CooldownEntry(stack));
            if (recentItem != null) {
                entry.stack = recentItem.stack();
                if (!Objects.equals(entry.customName, recentItem.customName())) {
                    entry.customName = recentItem.customName();
                }
            }
            entry.cooldown = cooldown;
            entry.animation.state(true);
            active.add(id);
        }
        this.entries.forEach((id, entry) -> entry.animation.state(active.contains(id)));
        this.visibilityAnimation.destination(1.0f);
    }

    @Override
    public void animate(WeightedEngine engine) {
        this.entries.values().forEach(entry -> entry.animation.animate(engine));
        this.entries.entrySet().removeIf(entry -> !entry.getValue().animation.state() && entry.getValue().animation.isZero());
        this.visibilityAnimation.animate(engine);
        this.leftWidthAnimation.animate(engine);
        this.rightWidthAnimation.animate(engine);
    }

    private void captureItemName(ItemStack stack, long now, ItemCooldowns cooldowns) {
        if (stack.isEmpty()) {
            return;
        }
        Component name = stack.getHoverName();
        boolean customPrefix = hasCustomPrefix(name);
        Identifier group = cooldowns.getCooldownGroup(stack);
        RecentItem previous = this.recentItems.get(group);
        if (previous != null && now - previous.seenAt() < 1000L
                && previous.stack().getItem() == stack.getItem()
                && Objects.equals(previous.stack().getHoverName(), name)) {
            return;
        }
        if (previous == null || customPrefix || previous.customName() == null) {
            this.recentItems.put(group, new RecentItem(stack.copy(), customPrefix ? name : null, now));
        }
    }

    private boolean hasCustomPrefix(Component name) {
        return name.visit((style, text) -> {
            for (int offset = 0; offset < text.length(); ) {
                int codePoint = text.codePointAt(offset);
                offset += Character.charCount(codePoint);
                int type = Character.getType(codePoint);
                if (Character.isWhitespace(codePoint) || type == Character.FORMAT
                        || codePoint == '[' || codePoint == '(' || codePoint == '{') {
                    continue;
                }
                return Optional.of(!style.getFont().equals(FontDescription.DEFAULT)
                        || type == Character.PRIVATE_USE || type == Character.OTHER_SYMBOL
                        || type == Character.MATH_SYMBOL || type == Character.MODIFIER_SYMBOL);
            }
            return Optional.empty();
        }, Style.EMPTY).orElse(false);
    }

    private String nameText(CooldownEntry entry) {
        String fallback = entry.stack.getItem().getName(entry.stack.getItem().getDefaultInstance()).getString();
        if (entry.customName == null) return fallback;
        String custom = cleanCustomName(entry.customName);
        return custom.isEmpty() ? fallback : custom;
    }

    private float nameWidth(CooldownEntry entry) {
        return inkWidth(nameText(entry));
    }

    private float headerWidth() {
        return 3.5f + 8.0f + 3.5f + 0.6f + 3.5f + inkWidth("Cooldowns") + 3.5f;
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

    private static String cleanCustomName(Component name) {
        String value = name.getString().strip();
        if (value.isEmpty()) return value;
        int opening = value.codePointAt(0);
        if (opening == '[' || opening == '(' || opening == '{') {
            int closing = value.indexOf(opening == '[' ? ']' : opening == '(' ? ')' : '}');
            if (closing > 0 && closing < value.length() - 1) {
                String marker = value.substring(1, closing);
                if (marker.codePoints().anyMatch(codePoint -> isStarGlyph(codePoint, Style.EMPTY))) {
                    return value.substring(closing + 1).stripLeading();
                }
            }
        }
        int first = value.codePointAt(0);
        if (isStarGlyph(first, Style.EMPTY)) {
            return value.substring(Character.charCount(first)).stripLeading();
        }
        return value;
    }

    private static boolean isStarGlyph(int codePoint, Style style) {
        int type = Character.getType(codePoint);
        return codePoint == '*' || codePoint == '★' || codePoint == '☆'
                || codePoint == '✦' || codePoint == '✧'
                || type == Character.PRIVATE_USE || type == Character.OTHER_SYMBOL
                || (type == Character.MATH_SYMBOL && !Character.isLetterOrDigit(codePoint))
                || (!style.getFont().equals(FontDescription.DEFAULT)
                    && !Character.isLetterOrDigit(codePoint) && !Character.isWhitespace(codePoint)
                    && codePoint != '[' && codePoint != ']' && codePoint != '(' && codePoint != ')');
    }

    private float remainingSeconds(CooldownEntry entry) {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null || entry.cooldown == null) {
            return 0.0f;
        }
        float tickDelta = Mc.INSTANCE.getMinecraft().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float progress = player.getCooldowns().getCooldownPercent(entry.stack, tickDelta);
        return Math.max(0.0f, progress * (entry.cooldown.endTime - entry.cooldown.startTime) / 20.0f);
    }

    private String formatTime(float seconds) {
        int rounded = Math.max(0, (int) Math.ceil(seconds));
        return String.format(Locale.ROOT, "%d:%02d", rounded / 60, rounded % 60);
    }

    private void drawHeader(DragRenderContext context, float headerX) {
        DrawEngine drawEngine = context.drawEngine();
        PoseStack matrices = context.matrixStack();
        ColorStack colors = drawEngine.colorStack();
        ThemePalette palette = context.theme().palette();
        drawGlassPanel(context, headerX, this.y, headerWidth(), 12.0f);
        float dividerX = headerX + 3.5f + 8.0f + 3.5f;
        drawCenteredIcon(context, "g", 8.5f, headerX + 3.5f, this.y,
                8.0f, 12.0f,
                colors.computeColor(palette.accent().argb()));
        drawDivider(context, dividerX, this.y + 12.0f / 2.0f, palette);
        float textY = this.y + (12.0f - this.textFont.getHeight(7.5f)) / 2.0f;
        float titleX = snapToPixel(dividerX + 0.6f + 3.5f, matrices.last().pose().m30(),
                Math.abs(matrices.last().pose().m00()));
        drawEngine.msdfFont(matrices.last().pose(), this.textFont, "Cooldowns",
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

    private static final class CooldownEntry {
        private ItemStack stack;
        private final ToggleAnimator animation = new ToggleAnimator(220, Easings.EASE_IN_OUT_QUAD);
        private ItemCooldowns.CooldownInstance cooldown;
        private Component customName;

        private CooldownEntry(ItemStack stack) {
            this.stack = stack;
        }
    }

    private record RecentItem(ItemStack stack, Component customName, long seenAt) {
    }

}
