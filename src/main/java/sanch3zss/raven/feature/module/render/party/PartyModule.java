package sanch3zss.raven.feature.module.render.party;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.UUID;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.KeyAction;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.event.render.Render2DEvent;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.render.party.PartyService;
import sanch3zss.raven.client.chat.ChatUtil;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.ColorSetting;
import sanch3zss.raven.ui.setting.ExpandableSetting;
import sanch3zss.raven.ui.setting.KeybindSetting;
import sanch3zss.raven.ui.setting.NumberSetting;

public class PartyModule extends Module {
    private static final int TEXTURE_SIZE = 85;
    private final GlTexture arrowTexture = new GlTexture(new ClasspathResource("/textures/arrow.png"))
            .generateMipMap(true).minFilter(9987).magFilter(9729)
            .wrapX(33071).wrapY(33071).anisotropy(16.0f);
    private final NumberSetting arrowSize = new NumberSetting(Translation.clearText("Размер стрелок"))
            .range(0.2f, 1.0f).currentValue(0.4f).step(0.05f);
    private final NumberSetting arrowDistance = new NumberSetting(Translation.clearText("Расстояние от центра"))
            .range(35.0f, 200.0f).currentValue(85.0f).step(1.0f);
    private final BooleanSetting showNames = new BooleanSetting(Translation.clearText("Показывать ники"))
            .setValue(true);
    private final BooleanSetting showMeters = new BooleanSetting(Translation.clearText("Показывать дистанцию"))
            .setValue(false);
    private final ExpandableSetting showFriends = new ExpandableSetting(
            Translation.clearText("Отображение друзей"))
            .settings(this.arrowSize, this.arrowDistance, this.showNames, this.showMeters).setValue(true);
    private final KeybindSetting markKey = new KeybindSetting(Translation.clearText("Бинд метки"));
    private final ColorSetting markColor = new ColorSetting(Translation.clearText("Цвет метки"))
            .value(0xFFFF6B73);
    private final ExpandableSetting markEntities = new ExpandableSetting(
            Translation.clearText("Отметить энтити"))
            .settings(this.markKey, this.markColor).setValue(true);

    public PartyModule() {
        super(ModuleTab.VISUALS, "PartyHelper");
        addSettings(this.showFriends, this.markEntities);
        this.markKey.consumer(action -> {
            if (action == KeyAction.PRESS) markTarget();
        });
        register(Render2DEvent.class, this::renderArrows);
    }

    private void markTarget() {
        if (!isState() || !this.markEntities.isValue() || !Mc.INSTANCE.isWorldLoaded()
                || Mc.INSTANCE.getCurrentScreen() != null) return;
        if (!(Mc.INSTANCE.getCrosshairTarget() instanceof EntityHitResult target)) {
            ChatUtil.addChatMessage("Наведитесь на энтити для метки.");
            return;
        }
        if (!(target.getEntity() instanceof LivingEntity)) {
            ChatUtil.addChatMessage("Можно отметить игрока или моба.");
            return;
        }
        Raven.INSTANCE.partyService().mark(target.getEntity(), this.markColor.getColor());
    }

    private void renderArrows(Render2DEvent event) {
        if (!isState() || !this.showFriends.isValue() || !event.isPost()
                || !Mc.INSTANCE.isWorldLoaded()) return;
        PartyService service = Raven.INSTANCE.partyService();
        if (!service.isConnected()) return;
        String dimension = PartyService.dimension();
        var camera = Mc.INSTANCE.getCamera();
        ScreenResolution screen = ScreenResolution.resolution();
        float centerX = screen.screenWidth() / 2.0f;
        float centerY = screen.screenHeight() / 2.0f;
        float radius = this.arrowDistance.currentValue();
        float scale = this.arrowSize.currentValue();
        PoseStack matrices = event.matrixStack();
        DrawEngine draw = Raven.INSTANCE.drawEngine();
        draw.begin();
        int texture = draw.bindTexture(this.arrowTexture.textureWithSTB());
        double yaw = Math.toRadians(camera.yRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        for (PartyService.Position position : service.positions()) {
            if (!dimension.equals(position.dimension())) continue;
            PartyService.Member member = service.members().stream()
                    .filter(value -> value.id().equals(position.id()) && value.connected())
                    .findFirst().orElse(null);
            if (member == null) continue;
            double x = position.interpolatedX() - camera.position().x;
            double z = position.interpolatedZ() - camera.position().z;
            if (x * x + z * z < 0.01d) continue;
            double rotatedX = -((z * cos) - (x * sin));
            double rotatedY = -((x * cos) + (z * sin));
            float angle = (float) Math.toDegrees(Math.atan2(rotatedX, rotatedY));
            float arrowX = centerX + (float) Math.cos(Math.toRadians(angle)) * radius;
            float arrowY = centerY + (float) Math.sin(Math.toRadians(angle)) * radius;
            int color = PartyService.COLORS[member.color() % PartyService.COLORS.length];
            matrices.pushPose();
            matrices.translate(arrowX, arrowY, 0.0f);
            matrices.pushPose();
            matrices.mulPose(Axis.ZP.rotationDegrees(angle));
            matrices.scale(scale, scale, 1.0f);
            draw.texture(matrices.last().pose(), -TEXTURE_SIZE / 2.0f,
                    -TEXTURE_SIZE / 2.0f, TEXTURE_SIZE, TEXTURE_SIZE, texture, color);
            matrices.popPose();
            String label = label(member.name(), Math.sqrt(x * x + z * z
                    + Math.pow(position.interpolatedY() - camera.position().y, 2.0)));
            if (!label.isEmpty()) {
                matrices.mulPose(Axis.ZP.rotationDegrees(angle + 90.0f));
                draw.msdfFontHorizontalC(new Matrix4f(matrices.last().pose()), Fonts.INTER_SEMIBOLD.get(), label,
                        0.0f, TEXTURE_SIZE * scale * 0.3f + 2.0f, 10.0f, 0.0f, color);
            }
            matrices.popPose();
        }
        draw.end();
    }

    private String label(String name, double distance) {
        String meters = Math.round(distance) + "м";
        if (this.showNames.isValue() && this.showMeters.isValue()) return name + " " + meters;
        if (this.showNames.isValue()) return name;
        return this.showMeters.isValue() ? meters : "";
    }

    public int tint(UUID entityId, int original) {
        if (!isState() || !this.markEntities.isValue() || entityId == null
                || !Mc.INSTANCE.isWorldLoaded()) return original;
        long now = System.currentTimeMillis();
        for (PartyService.Mark mark : Raven.INSTANCE.partyService().marks()) {
            if (!mark.entity().equals(entityId) || mark.expiresAt() <= now
                    || !mark.dimension().equals(PartyService.dimension())) continue;
            float remaining = (mark.expiresAt() - now) / 1000.0f;
            float life = Math.min(1.0f, remaining);
            float age = 5.0f - remaining;
            float fadeIn = Math.clamp(age / 0.25f, 0.0f, 1.0f);
            float pulse = 0.68f + 0.17f * (float) Math.sin(age * 2.4f);
            return ColorUtil.interpolateColor(original, mark.color(), pulse * life * fadeIn);
        }
        return original;
    }
}
