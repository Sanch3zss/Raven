package sanch3zss.raven.feature.module.render.targetesp;


import sanch3zss.raven.feature.module.render.GhostType;
import sanch3zss.raven.render.shape.ShapeRenderer;
import sanch3zss.raven.render.theme.ColorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.player.FakePlayerModule;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.*;

public class TargetESPModule extends Module {
    public static final Identifier markerTexture = Identifier.fromNamespaceAndPath("raven", "textures/marker_union.png");
    public static final Identifier soulsTexture = Identifier.fromNamespaceAndPath("raven", "textures/glow.png");
    public final NumberSetting rotationSpeed;
    public final NumberSetting ghostsSpeed;
    public ModeSetting<TargetEspMode> mode;
    public final ModeSetting<GhostType> ghostType;
    public final ColorSetting color;
    public final BooleanSetting distortion;
    public final DeltaTimeTracker deltaTimeTracker;
    public final AnimationStack2 animationStack;
    public final ToggleAnimator toggleAnimator;
    public LivingEntity target;
    public float crystalRotation;
    public float prevCrystalRotation;
    public float crystalProgress;
    public float prevCrystalProgress;
    public float soulsRotation;
    public float prevSoulsRotation;
    public float soulsProgress;
    public float prevSoulsProgress;
    public float soulsExpandOffset;
    public boolean soulsActive;
    private float markerRotation = 1.0f;
    private float previousMarkerRotation;
    private float markerSpeed = 1.0f;
    private float markerProgress;
    private float previousMarkerProgress;
    private boolean markerReverse;
    private boolean markerActive;
    public TargetESPModule() {
        super(ModuleTab.VISUALS, "Target ESP");
        this.mode = new ModeSetting(Lang.TARGETESP_MODE).values(TargetEspMode.class);
        this.ghostType = new ModeSetting<GhostType>(Translation.clearText("Тип призраков"))
                .values(GhostType.class)
                .visible(() -> this.mode.currentValue() == TargetEspMode.GHOSTS);
        this.rotationSpeed = new NumberSetting(Lang.TARGETESP_CUBE_SPEED)
                .range(0.4f, 2.0f).currentValue(1.0f).step(0.1f)
                .visible(() -> this.mode.currentValue() == TargetEspMode.CUBE);
        this.ghostsSpeed = new NumberSetting(Lang.TARGETESP_GHOSTS_SPEED)
                .range(8.0f, 16.0f).currentValue(8.0f).step(0.1f)
                .visible(() -> this.mode.currentValue() == TargetEspMode.GHOSTS
                        && this.ghostType.currentValue() != GhostType.SECOND);
        this.color = new ColorSetting(Lang.TARGETESP_COLOR);
        this.distortion = new BooleanSetting(Lang.TARGETESP_DISTORTION)
                .setValue(true)
                .visible(() -> this.mode.currentValue() == TargetEspMode.CRYSTALS);
        this.deltaTimeTracker = new DeltaTimeTracker();
        this.animationStack = new AnimationStack2();
        this.toggleAnimator = new ToggleAnimator(700, Easings.EASE_IN_OUT_CUBIC);
        ExpandableSetting effect = new ExpandableSetting(Translation.clearText("Настройки эффекта"))
                .navigationOnly().setSubSettings(List.of(this.ghostType, this.distortion,
                        this.rotationSpeed, this.ghostsSpeed));
        addSettings(this.mode, this.color, effect);
        register(PlayerTickEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded() && event.isPre()) {
                if (this.target != null && !isPresentTarget(this.target)) {
                    this.target = null;
                }
                LivingEntity livingEntityTarget = Mc.INSTANCE.getMinecraft().crosshairPickEntity instanceof LivingEntity le
                        && isTrackableTarget(le) ? le : null;
                LivingEntity fakeTarget = Raven.INSTANCE.moduleRepository()
                        .get(FakePlayerModule.class).lookedAtFakePlayer();
                if (isTrackableTarget(fakeTarget)) {
                    livingEntityTarget = fakeTarget;
                }
                if (livingEntityTarget != null && livingEntityTarget != this.target) {
                    this.target = livingEntityTarget;
                }
                boolean keepTarget = isState() && isTrackableTarget(this.target);
                this.toggleAnimator.state(keepTarget);
                this.markerActive = keepTarget;
                this.previousMarkerProgress = this.markerProgress;
                this.markerProgress = keepTarget
                        ? Math.min(this.markerProgress + 0.14f, 1.0f)
                        : Math.max(this.markerProgress - 0.09f, 0.0f);
                this.previousMarkerRotation = this.markerRotation;
                if (keepTarget) {
                    if (this.markerSpeed > 25.0f) this.markerReverse = true;
                    if (this.markerSpeed < -25.0f) this.markerReverse = false;
                } else {
                    if (this.markerSpeed > 0.0f && !this.markerReverse) this.markerReverse = true;
                    else if (this.markerSpeed < 0.0f && this.markerReverse) this.markerReverse = false;
                }
                this.markerSpeed += this.markerReverse ? -0.5f : 0.5f;
                this.markerRotation += this.markerSpeed * this.rotationSpeed.currentValue();
                if (!keepTarget && this.toggleAnimator.isZero()
                        && this.markerProgress <= 0.0f && this.soulsProgress <= 0.0f
                        && this.crystalProgress <= 0.0f) {
                    this.target = null;
                }
                WeightedEngine weightedEngine = new WeightedEngine(this.deltaTimeTracker.elapsedUnit(), this.animationStack);
                this.animationStack.begin();
                this.toggleAnimator.animate(weightedEngine);
                this.animationStack.end();
                this.prevCrystalRotation = this.crystalRotation;
                this.crystalRotation -= 3.4f;
                if (this.crystalRotation < -20000.0f) {
                    this.crystalRotation += 20000.0f;
                    this.prevCrystalRotation += 20000.0f;
                }
                this.prevCrystalProgress = this.crystalProgress;
                this.crystalProgress = keepTarget
                        ? Math.min(this.crystalProgress + 0.11f, 1.0f)
                        : Math.max(this.crystalProgress - 0.24f, 0.0f);
                this.prevSoulsRotation = this.soulsRotation;
                this.soulsRotation += this.ghostsSpeed.currentValue();
                this.prevSoulsProgress = this.soulsProgress;
                this.soulsActive = keepTarget;
                this.soulsProgress = this.soulsActive
                        ? Math.min(this.soulsProgress + 0.07f, 1.0f)
                        : Math.max(this.soulsProgress - 0.105f, 0.0f);
            }
        });
        register(WorldRenderEvent.class, event2 -> {
            if (Mc.INSTANCE.isWorldLoaded() && isState()
                    && this.mode.currentValue() == TargetEspMode.CRYSTALS) {
                CrystalRenderer.INSTANCE.prepare(this.distortion.isValue());
            }
            if (Mc.INSTANCE.isWorldLoaded() && isPresentTarget(this.target)
                    && (isValidTarget(this.target) || !this.toggleAnimator.isZero()
                    || this.markerProgress > 0.0f || this.soulsProgress > 0.0f)) {
                if (event2.frustum() != null
                        && !event2.frustum().isVisible(this.target.getBoundingBox())) {
                    return;
                }
                Mc mc = Mc.INSTANCE;
                Camera camera = mc.getCamera();
                Vec3 pos = camera.position();
                PoseStack matrixStack = event2.matrixStack();
                float tickDelta = mc.getTickDelta();
                double dInterpolate = MathUtil.interpolate(this.target.xo, this.target.getX(), tickDelta) - pos.x();
                double dInterpolate2 = MathUtil.interpolate(this.target.yo, this.target.getY(), tickDelta) - pos.y();
                double dInterpolate3 = MathUtil.interpolate(this.target.zo, this.target.getZ(), tickDelta) - pos.z();
                float f = this.target.hurtTime / 10.0f;
                float width = this.target.getBbWidth();
                switch ((TargetEspMode) this.mode.currentValue()) {
                    case CUBE:
                        renderMarker(matrixStack, camera, dInterpolate, dInterpolate2, dInterpolate3,
                                tickDelta);
                        break;
                    case GHOSTS:
                        if (this.ghostType.currentValue() == GhostType.FIRST) {
                            renderSouls(matrixStack, camera, dInterpolate, dInterpolate2, dInterpolate3,
                                    width, this.target.getBbHeight(), tickDelta, true);
                        } else if (this.ghostType.currentValue() == GhostType.SECOND) {
                            renderCelestialSouls(matrixStack, camera, dInterpolate, dInterpolate2,
                                    dInterpolate3, width, this.target.getBbHeight(), f);
                        } else {
                            renderTrailingSouls(matrixStack, camera, dInterpolate, dInterpolate2,
                                    dInterpolate3, width, this.target.getBbHeight(), f, tickDelta);
                        }
                        break;
                    case CRYSTALS:
                        CrystalRenderer.INSTANCE.render(event2, this.target,
                                effectColor(this.color.getFirstColor(), f),
                                effectColor(this.color.getGradientEndColor(), f),
                                MathUtil.interpolate(this.prevCrystalProgress, this.crystalProgress, tickDelta),
                                this.prevCrystalRotation, this.crystalRotation, false,
                                this.distortion.isValue());
                        break;
                    case JELLO:
                        TargetJelloRenderer.INSTANCE.render(event2, dInterpolate, dInterpolate2,
                                dInterpolate3, width, this.target.getBbHeight(),
                                effectColor(this.color.getFirstColor(), f),
                                effectColor(this.color.getGradientEndColor(), f));
                        break;
                }
            }
        });
    }

    private static boolean isValidTarget(LivingEntity entity) {
        return isPresentTarget(entity) && entity != Mc.INSTANCE.getPlayer()
                && entity.isAlive() && entity.getHealth() > 0.0f && !isInvisible(entity);
    }

    private static boolean isPresentTarget(LivingEntity entity) {
        if (entity == null || !Mc.INSTANCE.isWorldLoaded() || entity.level() != Mc.INSTANCE.getWorld()) {
            return false;
        }
        FakePlayerModule fakePlayer = Raven.INSTANCE.moduleRepository().get(FakePlayerModule.class);
        return entity == fakePlayer.getFakePlayer()
                || Mc.INSTANCE.getWorld().getEntity(entity.getId()) == entity;
    }

    private int effectColor(int base, float hurt) {
        int color = hurt > 0.0f ? ColorUtil.interpolateColor(base, 0xFFFF0000, hurt) : base;
        return ColorUtil.replAlpha(color,
                Math.round(ColorUtil.alpha(color) * this.toggleAnimator.smoothAnimation()));
    }

    private void renderMarker(PoseStack matrices, Camera camera, double x, double y,
                              double z, float tickDelta) {
        float progress = MathUtil.interpolate(this.previousMarkerProgress, this.markerProgress, tickDelta);
        if (progress <= 0.001f) {
            return;
        }
        float remaining = 1.0f - progress;
        float alpha = this.markerActive
                ? 1.0f - remaining * remaining * remaining
                : 1.0f - remaining * remaining;
        float size = 1.2f * (this.markerActive ? 1.0f : 1.0f + (1.0f - alpha) * 0.15f);
        int firstColor = this.color.getGradientColor(0.0f);
        int secondColor = this.color.getGradientColor(1.0f);
        float hurt = this.target.hurtTime / 10.0f;
        if (hurt > 0.0f) {
            firstColor = ColorUtil.interpolateColor(firstColor, 0xFFFF0000, hurt);
            secondColor = ColorUtil.interpolateColor(secondColor, 0xFFFF0000, hurt);
        }
        int topLeft = ColorUtil.replAlpha(firstColor, ColorUtil.alpha(firstColor) / 255.0f * alpha);
        int topRight = topLeft;
        int bottomLeft = ColorUtil.replAlpha(secondColor, ColorUtil.alpha(secondColor) / 255.0f * alpha);
        int bottomRight = bottomLeft;
        matrices.pushPose();
        matrices.translate(x, y + this.target.getBbHeight() / 2.0f, z);
        matrices.mulPose(camera.rotation());
        matrices.mulPose(Axis.ZP.rotationDegrees(MathUtil.interpolate(
                this.previousMarkerRotation, this.markerRotation, tickDelta)));
        ShapeRenderer.INSTANCE.addTexture(matrices.last().pose(), -size / 2.0f,
                -size / 2.0f, size, size, markerTexture, topLeft, bottomLeft,
                bottomRight, topRight, false, true, true);
        ShapeRenderer.INSTANCE.addTexture(matrices.last().pose(), -size / 2.0f,
                -size / 2.0f, size, size, markerTexture, topLeft, bottomLeft,
                bottomRight, topRight, false, true, true);
        matrices.popPose();
    }

    private static boolean isInvisible(LivingEntity entity) {
        return entity.isInvisible() || entity.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY);
    }

    private static boolean isTrackableTarget(LivingEntity entity) {
        return isValidTarget(entity) && Mc.INSTANCE.getPlayer().hasLineOfSight(entity);
    }

    private void renderSouls(PoseStack matrices, Camera camera, double x, double y, double z,
                             float entityWidth, float entityHeight, float tickDelta,
                             boolean additiveGlow) {
        float progress = MathUtil.interpolate(this.prevSoulsProgress, this.soulsProgress, tickDelta);
        if (progress <= 0.001f) return;

        float rotation = MathUtil.interpolate(this.prevSoulsRotation, this.soulsRotation, tickDelta);
        float alpha = this.soulsActive
                ? 1.0f - (float) Math.pow(1.0f - progress, 3.0f)
                : progress * progress;
        float targetExpand = this.soulsActive ? 0.0f : 0.5f;
        this.soulsExpandOffset += (targetExpand - this.soulsExpandOffset) * 0.12f;
        alpha *= Math.max(0.0f, 1.0f - this.soulsExpandOffset / 0.5f);
        if (alpha <= 0.01f && !this.soulsActive) return;

        float radius = entityWidth * 1.5f + this.soulsExpandOffset;
        float hurt = this.target.hurtTime / 10.0f;
        float toggleAlpha = this.toggleAnimator.smoothAnimation();

        int wormTick = 0;
        int wormCooldown = 0;
        for (int angle = 0; angle < 360; angle += 2) {
            float size = 0.13f + 0.005f * wormTick;
            float glowSize = 0.9f + 0.005f * wormTick;
            if (wormCooldown > 0) {
                wormCooldown -= 2;
                continue;
            }
            wormTick += 2;
            if (wormTick > 50) {
                wormCooldown = 100;
                wormTick = 0;
                continue;
            }

            double radians = Math.toRadians(angle + rotation);
            double soulX = x + Math.sin(radians) * radius * 0.7f;
            double soulZ = z + Math.cos(radians) * radius * 0.7f;
            double soulY = y + entityHeight / 1.5f
                    + entityHeight / 3.0f * Math.sin(Math.toRadians(angle / 2.0f + rotation / 5.0f));
            int soulColor = this.color.getAnimatedGradientColor(wormTick / 50.0f);
            if (hurt > 0.0f) {
                soulColor = ColorUtil.interpolateColor(soulColor, 0xFFFF0000, hurt);
            }
            int rgb = soulColor & 0x00FFFFFF;
            float colorAlpha = ColorUtil.alpha(soulColor) / 255.0f * toggleAlpha;
            matrices.pushPose();
            matrices.translate(soulX, soulY, soulZ);
            matrices.mulPose(camera.rotation());

            int glowAlpha = Math.max(0, Math.min(255, Math.round(255.0f * alpha * colorAlpha * 0.05f)));
            int coreAlpha = Math.max(0, Math.min(255, Math.round(255.0f * alpha * colorAlpha)));
            if (additiveGlow) {
                ShapeRenderer.INSTANCE.addTexture(matrices.last().pose(), -glowSize / 2.0f,
                        -glowSize / 2.0f, glowSize, glowSize, soulsTexture,
                        (glowAlpha << 24) | rgb, true, true, true);
            }
            ShapeRenderer.INSTANCE.addTexture(matrices.last().pose(), -size / 2.0f,
                    -size / 2.0f, size, size, soulsTexture,
                    (coreAlpha << 24) | rgb, true, true, true);
            matrices.popPose();
        }
    }

    private void renderCelestialSouls(PoseStack matrices, Camera camera, double x, double y,
                                      double z, float entityWidth, float entityHeight, float hurt) {
        float alpha = this.toggleAnimator.smoothAnimation();
        if (alpha <= 0.001f) {
            return;
        }
        double time = System.nanoTime() / 1_000_000_000.0;
        double rotation = time * 3.0 % (Math.PI * 2.0);
        float centerY = entityHeight / 2.0f + 0.2f;
        for (int strand = 0; strand < 4; strand++) {
            double strandTime = time + strand * 15.0;
            double strandRotation = strand * (Math.PI / 2.0);
            for (int point = 0; point <= 14; point++) {
                double arc = Math.toRadians(point * (60.0 / 14.0));
                double angle = arc + rotation + strandRotation;
                double soulX = x + entityWidth * Math.cos(angle);
                double soulY = y + centerY + Math.sin(strandTime + arc + strand) * 0.7;
                double soulZ = z + entityWidth * Math.sin(angle);
                float size = 0.4f * (0.5f + point / 14.0f);
                int soulColor = this.color.getAnimatedGradientColor((strand * 15.0f + point * 14.0f) / 360.0f);
                if (hurt > 0.0f) {
                    soulColor = ColorUtil.interpolateColor(soulColor, 0xFFFF0000, hurt);
                }
                soulColor = ColorUtil.replAlpha(soulColor,
                        Math.round(ColorUtil.alpha(soulColor) * alpha));
                matrices.pushPose();
                matrices.translate(soulX, soulY, soulZ);
                matrices.mulPose(camera.rotation());
                ShapeRenderer.INSTANCE.addTexture(matrices.last().pose(), -size / 2.0f,
                        -size / 2.0f, size, size, soulsTexture, soulColor,
                        true, true, true);
                matrices.popPose();
            }
        }
    }

    private void renderTrailingSouls(PoseStack matrices, Camera camera, double x, double y,
                                     double z, float entityWidth, float entityHeight,
                                     float hurt, float tickDelta) {
        float progress = MathUtil.interpolate(this.prevSoulsProgress, this.soulsProgress, tickDelta);
        if (progress <= 0.001f) {
            return;
        }
        float alpha = this.soulsActive
                ? 1.0f - (float) Math.pow(1.0f - progress, 3.0f)
                : progress * progress;
        double rotation = Math.toRadians(MathUtil.interpolate(
                this.prevSoulsRotation, this.soulsRotation, tickDelta));
        float sizeScale = Math.min(1.0f, entityHeight / 1.8f);
        for (int strand = 0; strand < 3; strand++) {
            double phase = strand * Math.PI * 2.0 / 3.0;
            for (int point = 27; point >= 0; point--) {
                float taper = 1.0f - point / 27.0f;
                double trailRotation = rotation - point * 0.043;
                double angle = trailRotation + phase;
                double radius = entityWidth * (1.1 + Math.sin(trailRotation * 0.45 + phase) * 0.1)
                        + (1.0f - alpha) * 0.18;
                double soulX = x + Math.cos(angle) * radius;
                double soulY = y + entityHeight * (0.55
                        + Math.sin(trailRotation * 0.62 + phase) * 0.32
                        + Math.sin(trailRotation * 1.14 - phase) * 0.06);
                double soulZ = z + Math.sin(angle) * radius;
                float size = (0.075f + 0.355f * (float) Math.pow(taper, 0.7)) * sizeScale;
                float tailFade = Math.min(1.0f, (28 - point) / 4.0f);
                int soulColor = this.color.getGradientColor(taper);
                if (hurt > 0.0f) {
                    soulColor = ColorUtil.interpolateColor(soulColor, 0xFFFF0000, hurt);
                }
                int coreAlpha = Math.round(ColorUtil.alpha(soulColor) * alpha
                        * (0.4f + taper * 0.6f) * tailFade);
                int glowAlpha = Math.round(coreAlpha * 0.05f);
                int rgb = soulColor & 0x00FFFFFF;
                float glowSize = (0.55f + taper * 0.45f) * sizeScale;
                matrices.pushPose();
                matrices.translate(soulX, soulY, soulZ);
                matrices.mulPose(camera.rotation());
                ShapeRenderer.INSTANCE.addTexture(matrices.last().pose(), -glowSize / 2.0f,
                        -glowSize / 2.0f, glowSize, glowSize, soulsTexture,
                        (glowAlpha << 24) | rgb, true, true, true);
                ShapeRenderer.INSTANCE.addTexture(matrices.last().pose(), -size / 2.0f,
                        -size / 2.0f, size, size, soulsTexture,
                        (coreAlpha << 24) | rgb, true, true, true);
                if (point == 0) {
                    float coreSize = size * 0.4f;
                    int highlight = ColorUtil.interpolateColor(soulColor, 0xFFFFFFFF, 0.45f);
                    ShapeRenderer.INSTANCE.addTexture(matrices.last().pose(), -coreSize / 2.0f,
                            -coreSize / 2.0f, coreSize, coreSize, soulsTexture,
                            ColorUtil.replAlpha(highlight, coreAlpha), true, true, true);
                }
                matrices.popPose();
            }
        }
    }

}
