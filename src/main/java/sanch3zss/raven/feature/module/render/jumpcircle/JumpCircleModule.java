package sanch3zss.raven.feature.module.render.jumpcircle;


import sanch3zss.raven.render.shape.ShapeRenderer;
import sanch3zss.raven.render.theme.ColorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.player.JumpEvent;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.feature.module.render.jumpcircle.JumpCircleTrailPoint;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.interpolation.Easings;

public class JumpCircleModule extends Module {
    static final List<JumpCircleTrailPoint> trailPoints = new ArrayList<>();
    public final Identifier circleTexture;
    public final Mc mc;

    public final NumberSetting lifetimeSetting;
    public final NumberSetting sizeSetting;
    public final ModeSetting<RenderStyle> styleSetting;
    public final ColorSetting colorSetting;

    public JumpCircleModule() {
        super(ModuleTab.VISUALS, "JumpCircle");
        this.circleTexture = Identifier.fromNamespaceAndPath("raven", "textures/circle.png");
        this.mc = Mc.INSTANCE;
        this.lifetimeSetting = new NumberSetting(Lang.PARTICLES_LIFETIME).range(500.0f, 5000.0f).currentValue(1500.0f).step(5.0f).unit(SettingUnit.MILLISECONDS);
        this.sizeSetting = new NumberSetting(Lang.PARTICLES_SIZE).range(0.5f, 3.0f).currentValue(3.0f).unit(SettingUnit.UNITS);
        this.styleSetting = new ModeSetting<RenderStyle>(Translation.clearText("Тип")).values(RenderStyle.class);
        this.colorSetting = new ColorSetting(Lang.TARGETESP_COLOR);
        addSettings(this.styleSetting, this.lifetimeSetting, this.sizeSetting, this.colorSetting);
        register(JumpEvent.class, event -> {
            if (isState() && this.mc.isWorldLoaded()) {
                addTrailPoint(this.mc.getPlayer());
            }
        });
        register(WorldRenderEvent.class, event2 -> {
            if (trailPoints.isEmpty()) {
                return;
            }
            if (!this.styleSetting.isSelected(RenderStyle.SHADER)) {
                Vec3 pos = this.mc.getCamera().position();
                for (JumpCircleTrailPoint point : trailPoints) {
                    float progress = point.getProgress();
                    if (progress <= 1.0f) {
                        renderCircle(event2.matrixStack(), point.position.subtract(pos),
                                this.sizeSetting.currentValue(), 1.0f - progress, 0);
                    }
                }
            }
            trailPoints.removeIf(point -> point.getProgress() > 1.0f);
        });
    }

    public void renderShaderCircles(WorldRenderEvent event) {
        if (!isState() || trailPoints.isEmpty() || !this.styleSetting.isSelected(RenderStyle.SHADER)) {
            return;
        }
        Vec3 pos = this.mc.getCamera().position();
        List<JumpCircleShaderRenderer.Circle> circles = new ArrayList<>();
        for (JumpCircleTrailPoint point : trailPoints) {
            float remaining = 1.0f - point.getProgress();
            if (remaining < 0.0f) continue;
            float diameter = circleDiameter(this.sizeSetting.currentValue(), remaining);
            float opacity = circleOpacity(remaining);
            if (diameter > 0.0f && opacity > 0.0f) {
                circles.add(new JumpCircleShaderRenderer.Circle(
                        point.position.subtract(pos).add(0.0d, 0.03d, 0.0d), diameter, opacity));
            }
        }
        if (!circles.isEmpty()) {
            JumpCircleShaderRenderer.INSTANCE.render(event, circles,
                    this.colorSetting.getAnimatedGradientColor(0.0f),
                    this.colorSetting.getAnimatedGradientColor(0.5f));
        }
    }

    public void renderCircle(PoseStack matrixStack, Vec3 vec3d, double d, float f, int i) {
        double dEase = circleDiameter(d, f);
        float fMin = circleOpacity(f);
        if (dEase <= 0.0d || fMin <= 0.0f) {
            return;
        }
        matrixStack.pushPose();
        matrixStack.translate(vec3d.x() - (dEase / 2.0d), vec3d.y(), vec3d.z() - (dEase / 2.0d));
        matrixStack.mulPose(Axis.XP.rotationDegrees(90.0f));
        int first = withOpacity(this.colorSetting.getAnimatedGradientColor(0.0f), fMin);
        int middle = withOpacity(this.colorSetting.getAnimatedGradientColor(0.25f), fMin);
        int second = withOpacity(this.colorSetting.getAnimatedGradientColor(0.5f), fMin);
        ShapeRenderer.INSTANCE.addTexture(new Matrix4f(matrixStack.last().pose()),
                0.0f, 0.0f, (float) dEase, (float) dEase, this.circleTexture,
                first, middle, second, middle, true, true, true);
        matrixStack.popPose();
    }

    private static float circleDiameter(double size, float remaining) {
        return (float) (size * 1.5d * Easings.EASE_OUT_CUBIC.ease(1.0f - remaining));
    }

    private static float circleOpacity(float f) {
        float f2 = (f > 0.5f ? 1.0f - f : f) * 2.0f;
        return Math.min((f2 < 0.5f ? 2.0f * f2 * f2 : 1.0f - (((float) Math.pow(((-2.0f) * f2) + 2.0f, 2.0d)) / 2.0f)) * 1.75f, 1.0f);
    }

    private static int withOpacity(int color, float opacity) {
        return ColorUtil.replAlpha(color, Math.round(ColorUtil.alpha(color) * opacity));
    }

    public void addTrailPoint(Entity entity) {
        trailPoints.add(new JumpCircleTrailPoint(this, entity.getPosition(Mc.INSTANCE.getTickDelta()).add(0.0d, 0.01d, 0.0d)));
    }

    public enum RenderStyle implements DisplayNamed {
        TEXTURE(Translation.clearText("Текстура")),
        SHADER(Translation.clearText("Шейдер"));

        private final Translation name;

        RenderStyle(Translation name) {
            this.name = name;
        }

        @Override
        public Translation getDisplayName() {
            return name;
        }
    }
}
