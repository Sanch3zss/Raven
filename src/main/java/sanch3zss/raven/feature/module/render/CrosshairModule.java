package sanch3zss.raven.feature.module.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.render.*;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.util.math.MathUtil;

public class CrosshairModule extends Module {
    public final Mc mc;
    public final ModeSetting<CrosshairStyle> styleSetting;
    public final ColorSetting colorSetting;
    public final BooleanSetting targetSetting;
    public final ColorSetting targetColorSetting;
    public final NumberSetting radiusSetting;
    public final NumberSetting gapSetting;
    public final NumberSetting lengthSetting;
    public final NumberSetting thicknessSetting;
    public final BooleanSetting cooldownSetting;
    public final NumberSetting cooldownMultiplierSetting;
    public final BooleanSetting outlineSetting;
    public final BooleanSetting tModeSetting;
    public final BooleanSetting centerDotSetting;
    public final NumberSetting centerSizeSetting;
    private float cooldown;
    private float targetBlend;

    public CrosshairModule() {
        super(ModuleTab.VISUALS, "Crosshair");
        this.mc = Mc.INSTANCE;
        this.styleSetting = new ModeSetting<CrosshairStyle>(Lang.CROSSHAIR_TYPE, Lang.CROSSHAIR_TYPE_DESC).values(CrosshairStyle.class);
        this.colorSetting = new ColorSetting(Lang.CROSSHAIR_COLOR, Lang.CROSSHAIR_COLOR_DESC).setColor(0xFFFFFFFF);
        this.targetSetting = new BooleanSetting(Translation.clearText("Подсветка цели"),
                Translation.clearText("Меняет цвет прицела, когда он наведён на сущность")).setValue(true);
        this.targetColorSetting = new ColorSetting(Translation.clearText("Цвет цели")).withoutThemeSync()
                .setColor(0xFFFF4545).visible(this.targetSetting::isValue);
        this.thicknessSetting = new NumberSetting(Lang.CROSSHAIR_THICKNESS, Lang.CROSSHAIR_THICKNESS_DESC)
                .currentValue(1.0f).range(0.5f, 4.0f).step(0.5f).unit(SettingUnit.PIXELS);
        this.lengthSetting = new NumberSetting(Lang.CROSSHAIR_LENGTH, Lang.CROSSHAIR_LENGTH_DESC)
                .currentValue(4.0f).range(1.0f, 12.0f).step(0.5f).unit(SettingUnit.PIXELS)
                .visible(() -> this.styleSetting.isSelected(CrosshairStyle.DEFAULT));
        this.gapSetting = new NumberSetting(Lang.CROSSHAIR_GAP, Lang.CROSSHAIR_GAP_DESC)
                .currentValue(2.0f).range(0.0f, 10.0f).step(0.5f).unit(SettingUnit.PIXELS)
                .visible(() -> this.styleSetting.isSelected(CrosshairStyle.DEFAULT));
        this.radiusSetting = new NumberSetting(Lang.CROSSHAIR_RADIUS, Lang.CROSSHAIR_RADIUS_DESC)
                .currentValue(5.0f).range(2.0f, 12.0f).step(0.5f).unit(SettingUnit.PIXELS)
                .visible(() -> this.styleSetting.isSelected(CrosshairStyle.CIRCLE));
        this.outlineSetting = new BooleanSetting(Lang.CROSSHAIR_OUTLINE, Lang.CROSSHAIR_OUTLINE_DESC)
                .visible(() -> this.styleSetting.isSelected(CrosshairStyle.DEFAULT));
        this.tModeSetting = new BooleanSetting(Lang.CROSSHAIR_TMODE, Lang.CROSSHAIR_TMODE_DESC)
                .visible(() -> this.styleSetting.isSelected(CrosshairStyle.DEFAULT));
        this.centerDotSetting = new BooleanSetting(Lang.CROSSHAIR_CENTER_DOT, Lang.CROSSHAIR_CENTER_DOT_DESC);
        this.centerSizeSetting = new NumberSetting(Lang.CROSSHAIR_CENTER_SIZE, Lang.CROSSHAIR_CENTER_SIZE_DESC)
                .currentValue(1.0f).range(0.5f, 4.0f).step(0.5f).unit(SettingUnit.PIXELS)
                .visible(this.centerDotSetting::isValue);
        this.cooldownSetting = new BooleanSetting(Lang.CROSSHAIR_COOLDOWN, Lang.CROSSHAIR_COOLDOWN_DESC);
        this.cooldownMultiplierSetting = new NumberSetting(Lang.CROSSHAIR_COOLDOWN_MULTIPLIER, Lang.CROSSHAIR_COOLDOWN_MULTIPLIER_DESC)
                .currentValue(4.0f).range(1.0f, 10.0f).step(0.5f).unit(SettingUnit.PIXELS)
                .visible(() -> this.cooldownSetting.isValue() && this.styleSetting.isSelected(CrosshairStyle.DEFAULT));
        ExpandableSetting appearance = new ExpandableSetting(Translation.clearText("Внешний вид"))
                .navigationOnly().setSubSettings(List.of(this.styleSetting, this.colorSetting,
                        this.colorSetting.themeSyncSetting(), this.thicknessSetting, this.lengthSetting,
                        this.gapSetting, this.radiusSetting, this.outlineSetting, this.tModeSetting));
        ExpandableSetting center = new ExpandableSetting(Translation.clearText("Центр"))
                .navigationOnly().setSubSettings(List.of(this.centerDotSetting, this.centerSizeSetting));
        ExpandableSetting target = new ExpandableSetting(Translation.clearText("Цель"))
                .navigationOnly().setSubSettings(List.of(this.targetSetting, this.targetColorSetting));
        ExpandableSetting cooldown = new ExpandableSetting(Translation.clearText("Кулдаун"))
                .navigationOnly().setSubSettings(List.of(this.cooldownSetting, this.cooldownMultiplierSetting));
        addSettings(appearance, center, target, cooldown);
        register(Render2DEvent.class, event -> {
            if (!event.isPost() || !isState() || !this.mc.isWorldLoaded() || this.mc.getMinecraft().gui.hud.isHidden()
                    || this.mc.getGameOptions().getCameraType() != CameraType.FIRST_PERSON
                    || this.mc.getCurrentScreen() != null && !(this.mc.getCurrentScreen() instanceof ChatScreen)) {
                return;
            }
            LocalPlayer player = this.mc.getPlayer();
            HitResult hit = this.mc.getCrosshairTarget();
            boolean onEntity = this.targetSetting.isValue() && hit != null && hit.getType() == HitResult.Type.ENTITY;
            this.cooldown = MathUtil.lerp(this.cooldown,
                    this.cooldownSetting.isValue() ? 1.0f - player.getAttackStrengthScale(0.0f) : 0.0f, 14.0f);
            this.targetBlend = MathUtil.lerp(this.targetBlend, onEntity ? 1.0f : 0.0f, 16.0f);
            Matrix4f matrix = event.matrixStack().last().pose();
            DrawEngine drawEngine = Raven.INSTANCE.drawEngine();
            ColorStack colors = drawEngine.colorStack();
            ScreenResolution resolution = ScreenResolution.resolution();
            float scale = this.mc.getWindow().getGuiScale();
            int base = ColorUtil.interpolateColor(this.colorSetting.getColor(),
                    this.targetColorSetting.getColor(), this.targetBlend);
            int color = colors.computeColor(base, ColorUtil.alpha(base));
            drawEngine.begin();
            if (this.styleSetting.isSelected(CrosshairStyle.CIRCLE)) {
                drawCircle(matrix, drawEngine, colors.computeColor(base, ColorUtil.alpha(base) / 4),
                        resolution, scale, color);
            } else {
                drawClassic(matrix, drawEngine, colors.computeColor(0x000000, Math.min(200, ColorUtil.alpha(base))),
                        resolution, scale, color);
            }
            drawEngine.end();
        });
        register(CrosshairRenderEvent.class, event -> {
            if (isState() && this.mc.isWorldLoaded()) {
                event.cancel();
            }
        });
    }

    private void drawClassic(Matrix4f matrix, DrawEngine drawEngine, int outlineColor, ScreenResolution resolution,
                             float scale, int color) {
        int thickness = Math.max(1, Math.round(this.thicknessSetting.currentValue() * scale));
        int length = Math.max(1, Math.round(this.lengthSetting.currentValue() * scale));
        int gap = Math.round((this.gapSetting.currentValue()
                + this.cooldownMultiplierSetting.currentValue() * this.cooldown) * scale);
        int x = resolution.screenWidth() / 2 - thickness / 2;
        int y = resolution.screenHeight() / 2 - thickness / 2;
        List<int[]> parts = new ArrayList<>();
        if (!this.tModeSetting.isValue()) parts.add(new int[]{x, y - gap - length, thickness, length});
        parts.add(new int[]{x, y + thickness + gap, thickness, length});
        parts.add(new int[]{x - gap - length, y, length, thickness});
        parts.add(new int[]{x + thickness + gap, y, length, thickness});
        if (this.centerDotSetting.isValue()) {
            int size = Math.max(1, Math.round(this.centerSizeSetting.currentValue() * scale));
            if ((thickness - size) % 2 != 0) size++;
            int offset = (thickness - size) / 2;
            parts.add(new int[]{x + offset, y + offset, size, size});
        }
        if (this.outlineSetting.isValue()) {
            int outline = Math.max(1, Math.round(scale * 0.5f));
            for (int[] part : parts) {
                drawEngine.rectangle(matrix, part[0] - outline, part[1] - outline,
                        part[2] + outline * 2, part[3] + outline * 2, outlineColor);
            }
        }
        for (int[] part : parts) {
            drawEngine.rectangle(matrix, part[0], part[1], part[2], part[3], color);
        }
    }

    private void drawCircle(Matrix4f matrix, DrawEngine drawEngine, int trackColor, ScreenResolution resolution,
                            float scale, int color) {
        float centerX = resolution.screenWidth() * 0.5f;
        float centerY = resolution.screenHeight() * 0.5f;
        float ring = Math.max(1.0f, this.thicknessSetting.currentValue() * scale);
        float radius = Math.max(ring + 1.0f, this.radiusSetting.currentValue() * scale);
        float sweep = 360.0f * (1.0f - this.cooldown);
        drawEngine.arc(matrix, centerX, centerY, radius, 0.0f, 360.0f, ring, trackColor);
        if (sweep > 0.5f) {
            drawEngine.arc(matrix, centerX, centerY, radius, 90.0f - sweep, 90.0f, ring, color);
        }
        if (this.centerDotSetting.isValue()) {
            float dot = Math.max(1.0f, this.centerSizeSetting.currentValue() * scale) * 0.5f;
            drawEngine.circle(matrix, centerX, centerY, dot, color);
        }
    }
}
