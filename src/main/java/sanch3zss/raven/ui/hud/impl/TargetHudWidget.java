package sanch3zss.raven.ui.hud.impl;

import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import sanch3zss.raven.render.gl.FramebufferUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL33;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.media.RemoteImageLoader;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.texture.ItemSpriteManager;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.feature.module.misc.NameProtectModule;
import sanch3zss.raven.feature.module.player.FakePlayerModule;
import sanch3zss.raven.feature.server.ScoreboardHelper;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.AnimatedTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.ui.setting.TextFieldSetting;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class TargetHudWidget extends Draggable {
    private final AnimatedFloat fadeAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_QUAD);
    private final AnimatedFloat healthAnimation = new AnimatedFloat(500, Easings.EASE_OUT_QUAD);
    private final AnimatedFloat absorptionAnimation = new AnimatedFloat(500, Easings.EASE_OUT_QUAD);
    private final ToggleAnimator nameScrollAnimation = new ToggleAnimator(1200, Easings.EASE_IN_OUT_CUBIC);
    private final MsdfFont textFont = Fonts.ZENITH_SEMIBOLD.get();
    private final MsdfFont iconFont = Fonts.NUR_ALPHA.get();
    private final TextFieldSetting customImageLink = new TextFieldSetting(Translation.clearText("Ссылка на GIF"))
            .setPlaceholder(Translation.clearText("https://..."))
            .setMax(2048);
    private LivingEntity target;
    private long lastTargetSeenAt;
    private String observedImageLink = "";
    private String requestedImageLink = "";
    private long imageLinkChangedAt;
    private long customImageStartedAt;
    private volatile AnimatedTexture pendingCustomImage;
    private AnimatedTexture customImage;

    public TargetHudWidget(BooleanSupplier visibility) {
        super("TargetHud", visibility);
        this.x = 604.0f;
        this.y = 469.0f;
        addSettings(this.customImageLink);
    }

    @Override
    public void layout(DragRenderContext context) {
    }

    @Override
    public void render(DragRenderContext context) {
        acceptPendingCustomImage();
        if (!isVisible() || this.fadeAnimation.isZero() || this.target == null) {
            return;
        }
        float health = targetHealth(this.target);
        float maxHealth = getEffectiveMaxHealth(this.target);
        float absorption = Math.max(0.0f, this.target.getAbsorptionAmount());
        float barCapacity = Math.max(maxHealth, health + absorption);
        float healthRatio = barCapacity > 0.0f ? MathUtil.clamp(health / barCapacity, 0.0f, 1.0f) : 0.0f;
        float absorptionRatio = barCapacity > 0.0f ? MathUtil.clamp(absorption / barCapacity, 0.0f, 1.0f) : 0.0f;
        this.healthAnimation.destination(healthRatio);
        this.absorptionAnimation.destination(absorptionRatio);

        ColorStack colors = context.drawEngine().colorStack();
        colors.push();
        colors.alpha(this.fadeAnimation.animatedValue());
        renderFirst(context, health, maxHealth, absorption);
        colors.pop();
    }

    private void renderFirst(DragRenderContext context, float health, float maxHealth, float absorption) {
        DrawEngine drawEngine = context.drawEngine();
        Matrix4f matrix = context.matrixStack().last().pose();
        ColorStack colors = drawEngine.colorStack();
        ThemePalette palette = context.theme().palette();
        float avatarX = this.x + 3.4f;
        float avatarY = this.y + 3.4f;
        float contentX = this.x + 34.0f;
        drawGlassPanel(context, this.x, this.y, 91.0f, 34.0f, 5.0f);
        renderTargetFace(drawEngine, matrix, colors, palette, avatarX, avatarY,
                27.0f, 5.0f);
        boolean unknownHealth = health > this.target.getMaxHealth() + 20.0f;
        String healthText = unknownHealth ? "Unknown HP" : String.format(Locale.ROOT, "%.1f HP",
                Math.round((health + absorption) * 2.0f) / 2.0f);
        renderFadingName(drawEngine, matrix, getDisplayName(this.target), contentX, this.y + 5.0f,
                Math.max(0.0f, this.x + 91.0f - 4.0f - contentX),
                colors.computeColor(palette.text().tone(50).argb()));
        drawEngine.msdfFont(matrix, this.textFont, healthText, contentX, this.y + 14.5f,
                7.0f, 0.0f, colors.computeColor(palette.accent().argb()));
        float barX = this.x + 34.0f;
        float barY = this.y + 24.0f;
        float barWidth = 52.2f;
        float healthWidth = barWidth * this.healthAnimation.animatedValue();
        float absorptionWidth = shouldShowAbsorption(this.target)
                ? Math.min(Math.max(0.0f, barWidth - healthWidth),
                barWidth * this.absorptionAnimation.animatedValue()) : 0.0f;
        drawBarBackground(context, barX, barY, barWidth, 6.0f, 2.35f);
        if (absorptionWidth > 0.0f) {
            float filledWidth = healthWidth + absorptionWidth;
            float transitionHalf = Math.min(2.5f, Math.min(healthWidth, absorptionWidth));
            drawEngine.roundedHorizontalGradient(matrix, barX, barY, filledWidth, 6.0f,
                    2.35f, colors.horizontalGradient(palette.accent().argb(),
                            ThemePalette.golden.argb(), ThemePalette.darkGolden.argb(),
                            (healthWidth - transitionHalf) / filledWidth,
                            (healthWidth + transitionHalf) / filledWidth));
        } else {
            drawEngine.roundedHorizontalGradient(matrix, barX, barY, healthWidth, 6.0f,
                    2.35f, colors.horizontalGradient(palette.gradientAccent(0.0f),
                            palette.gradientAccent(0.5f), palette.gradientAccent(0.5f), 0.0f, 1.0f));
        }
    }

    private void renderTargetFace(DrawEngine drawEngine, Matrix4f matrix, ColorStack colors,
                                  ThemePalette palette, float x, float y, float size, float radius) {
        if (this.customImage != null) {
            renderCustomImage(drawEngine, matrix, colors, x, y, size, size, radius);
        } else if (this.target instanceof Player) {
            renderAvatar(drawEngine, matrix, colors, x, y, size, size, radius);
        } else {
            drawEngine.msdfFont(matrix, this.iconFont, "N", x - 2.0f, y, size + 1.0f,
                    0.0f, colors.computeColor(palette.accent().argb()));
        }
    }

    @Override
    protected void collectItems(DragRenderContext context) {
        if (!isVisible() || this.fadeAnimation.isZero() || this.target == null) {
            return;
        }
        float alpha = this.fadeAnimation.animatedValue();

        List<ItemStack> equipment = getEquipment(this.target).stream()
                .filter(stack -> !stack.isEmpty()).toList();
        float itemX = this.x + 91.0f - equipment.size() * 9.1f;
        for (ItemStack stack : equipment) {
            context.drawVanillaItem(stack, itemX, this.y - 9.1f,
                    0.275f * 32.0f, false, alpha);
            itemX += 9.1f;
        }
    }

    private void drawBarBackground(DragRenderContext context, float x, float y, float width,
                                   float height, float radius) {
        context.drawEngine().roundedRectangle(context.matrixStack().last().pose(),
                x, y, width, height, radius,
                context.drawEngine().colorStack().computeColor(10.0f / 255.0f, 255, 255, 255));
    }

    private void drawGlassPanel(DragRenderContext context, float x, float y, float width, float height,
                                float cornerRadius) {
        HudGlassPanel.draw(context.drawEngine(), context.matrixStack().last().pose(),
                context.theme(), Raven.INSTANCE.widgetStack().hudBlurTexture(),
                x, y, width, height, cornerRadius);
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
        this.healthAnimation.animate(engine);
        this.absorptionAnimation.animate(engine);
        this.fadeAnimation.animate(engine);
        this.nameScrollAnimation.state(this.cursorHovered).animate(engine);
    }

    @Override
    public void update() {
        if (isVisible()) {
            updateCustomImage();
            updateTarget();
        }
    }

    private void updateCustomImage() {
        String current = this.customImageLink.getText() == null ? "" : this.customImageLink.getText().trim();
        if (!current.equals(this.observedImageLink)) {
            this.observedImageLink = current;
            this.imageLinkChangedAt = System.currentTimeMillis();
            if (current.isEmpty()) {
                this.requestedImageLink = "";
                clearCustomImage();
            }
        }
        if (!current.isEmpty() && !current.equals(this.requestedImageLink)
                && System.currentTimeMillis() - this.imageLinkChangedAt >= 500L) {
            requestCustomImage(current);
        }
    }

    private void requestCustomImage(String url) {
        this.requestedImageLink = url;
        RemoteImageLoader.load(url).whenComplete((loaded, error) -> {
            if (!url.equals(this.observedImageLink)) {
                if (loaded != null) loaded.close();
                return;
            }
            if (error != null) {
                Raven.LOGGER.warn("Failed to load TargetHud image from {}: {}", url, error.getMessage());
                return;
            }
            this.pendingCustomImage = loaded;
        });
    }

    private void acceptPendingCustomImage() {
        AnimatedTexture loaded = this.pendingCustomImage;
        if (loaded == null) {
            return;
        }
        this.pendingCustomImage = null;
        clearCustomImage();
        this.customImage = loaded;
        this.customImageStartedAt = System.currentTimeMillis();
    }

    private void clearCustomImage() {
        if (this.customImage != null) {
            this.customImage.close();
            this.customImage = null;
        }
    }

    public boolean isRendering() {
        return !this.fadeAnimation.isZero() && this.target != null;
    }

    private void updateTarget() {
        LivingEntity nextTarget = findTarget();
        if (nextTarget == null) {
            if (isValidTarget(this.target)
                    && System.currentTimeMillis() - this.lastTargetSeenAt <= 350L) {
                this.fadeAnimation.destination(1.0f);
                return;
            }
            if (this.target != null) {
                this.fadeAnimation.destination(0.0f);
            }
            if (this.fadeAnimation.isZero()) {
                clearTarget();
            }
            return;
        }
        this.lastTargetSeenAt = System.currentTimeMillis();
        if (!Objects.equals(this.target, nextTarget)) {
            this.target = nextTarget;
            float health = targetHealth(nextTarget);
            float maxHealth = getEffectiveMaxHealth(nextTarget);
            float absorption = Math.max(0.0f, nextTarget.getAbsorptionAmount());
            float barCapacity = Math.max(maxHealth, health + absorption);
            this.healthAnimation.set(MathUtil.clamp(barCapacity > 0.0f ? health / barCapacity : 0.0f, 0.0f, 1.0f));
            this.absorptionAnimation.set(MathUtil.clamp(barCapacity > 0.0f ? absorption / barCapacity : 0.0f, 0.0f, 1.0f));
        }
        this.fadeAnimation.destination(1.0f);
    }

    private void renderAvatar(DrawEngine drawEngine, Matrix4f matrix, ColorStack colors, float x, float y,
                              float width, float height, float radius) {
        try {
            Mc mc = Mc.INSTANCE;
            if (!(this.target instanceof AbstractClientPlayer playerTarget)) {
                return;
            }
            AbstractTexture texture = mc.getTextureManager().getTexture(playerTarget.getSkin().body().texturePath());
            if (texture == null) {
                return;
            }
            int textureId = FramebufferUtil.getTextureId(texture);
            if (textureId <= 0) {
                return;
            }
            applyNearestFiltering(textureId);
            int color = colors.computeColor(0xFFFFFFFF);
            drawEngine.skinFace(matrix, x, y, width, height, radius,
                    ItemSpriteManager.INSTANCE.defaultFaceUv.u0(), ItemSpriteManager.INSTANCE.defaultFaceUv.v0(),
                    ItemSpriteManager.INSTANCE.defaultFaceUv.u1(), ItemSpriteManager.INSTANCE.defaultFaceUv.v1(),
                    0.625f - ItemSpriteManager.INSTANCE.defaultFaceUv.u0(), drawEngine.bindTexture(textureId), color);
        } catch (Throwable ignored) {
        }
    }

    private void renderCustomImage(DrawEngine drawEngine, Matrix4f matrix, ColorStack colors,
                                   float x, float y, float width, float height, float radius) {
        try {
            int textureId = this.customImage.texture(System.currentTimeMillis() - this.customImageStartedAt);
            float imageAspect = this.customImage.width() / (float) this.customImage.height();
            float boxAspect = width / height;
            float u0 = 0.0f;
            float v0 = 0.0f;
            float u1 = 1.0f;
            float v1 = 1.0f;
            if (imageAspect > boxAspect) {
                float visible = boxAspect / imageAspect;
                u0 = (1.0f - visible) / 2.0f;
                u1 = 1.0f - u0;
            } else if (imageAspect < boxAspect) {
                float visible = imageAspect / boxAspect;
                v0 = (1.0f - visible) / 2.0f;
                v1 = 1.0f - v0;
            }
            int color = colors.computeColor(0xFFFFFFFF);
            drawEngine.roundedTexture(matrix, x, y, width, height,
                    radius, radius, radius, radius, u0, v0, u1, v1,
                    drawEngine.bindTexture(textureId), color, color, color, color);
        } catch (Throwable ignored) {
            clearCustomImage();
        }
    }

    private static void applyNearestFiltering(int textureId) {
        int previousActiveTexture = GL33.glGetInteger(GL33.GL_ACTIVE_TEXTURE);
        GL33.glActiveTexture(GL33.GL_TEXTURE0);
        int previousTexture = GL33.glGetInteger(GL33.GL_TEXTURE_BINDING_2D);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, textureId);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, previousTexture);
        GL33.glActiveTexture(previousActiveTexture);
    }

    private LivingEntity findTarget() {
        Mc mc = Mc.INSTANCE;
        LivingEntity fakePlayer = Raven.INSTANCE.moduleRepository()
                .get(FakePlayerModule.class).lookedAtFakePlayer();
        if (isValidTarget(fakePlayer)) {
            return fakePlayer;
        }
        if (mc.getMinecraft().crosshairPickEntity instanceof LivingEntity targeted && isValidTarget(targeted)) {
            return targeted;
        }
        LocalPlayer self = mc.getPlayer();
        if (!(mc.getCurrentScreen() instanceof ChatScreen) || self == null || !self.isAlive()
                || getDisplayName(self).isEmpty()) {
            return null;
        }
        return self;
    }

    @Override
    public float width() {
        return 91.0f;
    }

    @Override
    public float height() {
        return 34.0f;
    }

    @Override
    protected float baseScale() {
        return 1.75f;
    }

    private static float getEffectiveMaxHealth(LivingEntity entity) {
        float maxHealth = entity.getMaxHealth();
        float health = targetHealth(entity);
        return health > maxHealth + 20.0f ? maxHealth : Math.max(maxHealth, health);
    }

    private static float targetHealth(LivingEntity entity) {
        if (entity == Raven.INSTANCE.moduleRepository().get(FakePlayerModule.class).getFakePlayer()) {
            return entity.getHealth();
        }
        return ScoreboardHelper.INSTANCE.getHealthBelowName(entity);
    }

    private static boolean shouldShowAbsorption(LivingEntity entity) {
        return entity != null && entity.getAbsorptionAmount() > 0.0f;
    }

    private static boolean isValidTarget(LivingEntity entity) {
        return entity != null && entity.isAlive() && !isInvisible(entity);
    }

    private static boolean isInvisible(LivingEntity entity) {
        return entity.isInvisible() || entity.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
    }

    private void clearTarget() {
        this.target = null;
        this.lastTargetSeenAt = 0L;
        this.fadeAnimation.set(0.0f);
        this.healthAnimation.set(0.0f);
        this.absorptionAnimation.set(0.0f);
    }

    private static List<ItemStack> getEquipment(LivingEntity entity) {
        if (entity == null) {
            return List.of();
        }
        return List.of(
                entity.getMainHandItem(),
                entity.getOffhandItem(),
                entity.getItemBySlot(EquipmentSlot.HEAD),
                entity.getItemBySlot(EquipmentSlot.CHEST),
                entity.getItemBySlot(EquipmentSlot.LEGS),
                entity.getItemBySlot(EquipmentSlot.FEET));
    }

    private void renderFadingName(DrawEngine drawEngine, Matrix4f matrix, String text, float x, float y,
                                  float maxWidth, int color) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0f) {
            return;
        }
        if (this.textFont.getWidth(text, 7.0f) <= maxWidth) {
            drawEngine.msdfFont(matrix, this.textFont, text, x, y, 7.0f, 0.0f, color);
            return;
        }

        float scrollProgress = this.nameScrollAnimation.smoothAnimation();
        if (scrollProgress > 0.001f) {
            float offset = (this.textFont.getWidth(text, 7.0f) - maxWidth) * scrollProgress;
            drawEngine.beginScissor(matrix, x, y - 1.0f, maxWidth,
                    this.textFont.getHeight(7.0f) + 2.0f);
            drawEngine.msdfFont(matrix, this.textFont, text, x - offset, y,
                    7.0f, 0.0f, color);
            drawEngine.endScissor();
            return;
        }

        int visibleLength = text.length();
        while (visibleLength > 0 && this.textFont.getWidth(text.substring(0, visibleLength), 7.0f) > maxWidth) {
            visibleLength--;
        }
        if (visibleLength == 0) {
            return;
        }

        int fadeStart = visibleLength;
        while (fadeStart > 0
                && this.textFont.getWidth(text.substring(fadeStart, visibleLength), 7.0f) < 10.0f) {
            fadeStart--;
        }
        String solidPart = text.substring(0, fadeStart);
        String fadingPart = text.substring(fadeStart, visibleLength);
        float fadeX = x + this.textFont.getWidth(solidPart, 7.0f);
        drawEngine.msdfFont(matrix, this.textFont, solidPart, x, y, 7.0f, 0.0f, color);
        drawEngine.msdfFontHorizontalGradient(matrix, this.textFont, fadingPart, fadeX, y, 7.0f,
                color, color & 0x00FFFFFF);
    }

    private String getDisplayName(LivingEntity entity) {
        if (entity == null) {
            return "";
        }
        String name = FriendManager.applyAliases(StringUtil.stripColor(entity.getName().getString()));
        NameProtectModule nameProtect = Raven.INSTANCE.moduleRepository().get(NameProtectModule.class);
        return nameProtect != null && nameProtect.isState() ? nameProtect.replace(name) : name;
    }

}
