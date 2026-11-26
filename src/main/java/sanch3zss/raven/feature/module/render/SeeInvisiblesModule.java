package sanch3zss.raven.feature.module.render;

import java.util.Locale;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.render.effect.SelectiveBloomRenderer;
import sanch3zss.raven.render.shape.ShapeRenderer;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.ColorSetting;

@Aliases(aliases = {"Reveal Invisibles", "Invisible Detection", "See Hidden", "Invisible Vision", "Detect Invisibles", "Anti-Invisibility", "See Stealth", "See Invisibles", "Reveal Stealth Targets"})
public class SeeInvisiblesModule extends Module {
    private static final int SEGMENTS = 64;
    private static final double FULL_CIRCLE = Math.PI * 2.0;
    private static final int FRIEND_GREEN = 0xFF63E987;
    private static final int FRIEND_GREEN_LIGHT = 0xFFA8FFC1;
    private final ColorSetting color;

    public SeeInvisiblesModule() {
        super(ModuleTab.VISUALS, "SeeInvisibles");
        this.color = new ColorSetting(Translation.clearText("Цвет")).value(0xFFFFFFFF);
        this.color.setSecondColor(0xFFFF7777);
        addSettings(this.color);
        register(ClientTickEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded() && !onHolyWorld()) setState(false);
        });
        register(WorldRenderEvent.class, this::render);
    }

    @Override
    public void setState(boolean enabled) {
        if (enabled && Mc.INSTANCE.isWorldLoaded() && !onHolyWorld()) return;
        super.setState(enabled);
    }

    @Override
    public boolean isBlocked() {
        return Mc.INSTANCE.isWorldLoaded() && !onHolyWorld() || super.isBlocked();
    }

    private boolean onHolyWorld() {
        var server = Mc.INSTANCE.getCurrentServer();
        return server != null && server.ip != null
                && server.ip.toLowerCase(Locale.ROOT).contains("holyworld");
    }

    private void render(WorldRenderEvent event) {
        Mc mc = Mc.INSTANCE;
        if (!isState() || !mc.isWorldLoaded()) return;
        LocalPlayer player = mc.getPlayer();
        Vec3 camera = mc.getCamera().position();
        float partialTick = mc.getTickDelta();
        Matrix4f matrix = event.matrixStack().last().pose();
        for (var entity : mc.getWorld().entitiesForRendering()) {
            if (!(entity instanceof Player target) || target == player || !target.isAlive()
                    || !target.isInvisibleTo(player) || target.hasEffect(MobEffects.GLOWING)) continue;
            if (event.frustum() != null
                    && !event.frustum().isVisible(target.getBoundingBox().inflate(0.7))) continue;
            Vec3 feet = target.getPosition(partialTick).add(0.0, 0.045, 0.0);
            if (mc.getWorld().clip(new ClipContext(camera, feet.add(0.0, 0.08, 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player))
                    .getType() != HitResult.Type.MISS) continue;
            drawFootBloom(matrix, feet, target.tickCount + partialTick, target.getBbWidth(),
                    FriendManager.isFriend(target.getName().getString()));
        }
    }

    private void drawFootBloom(Matrix4f matrix, Vec3 feet, float time, float width, boolean friend) {
        float phase = time * 0.14f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(phase);
        float radius = width * 0.5f + 0.08f + 0.015f * pulse;
        float gradientRotation = phase * 0.3f;
        float opacity = 0.55f + 0.35f * pulse;
        for (int segment = 0; segment < SEGMENTS; segment++) {
            double startAngle = segment * FULL_CIRCLE / SEGMENTS;
            double endAngle = segment == SEGMENTS - 1 ? 0.0 : (segment + 1) * FULL_CIRCLE / SEGMENTS;
            float midpoint = (float) ((segment + 0.5) * FULL_CIRCLE / SEGMENTS);
            float gradient = 0.5f - 0.5f * (float) Math.cos(midpoint + gradientRotation);
            int color = withOpacity(gradientColor(friend, gradient), opacity);
            double startCos = Math.cos(startAngle);
            double startSin = Math.sin(startAngle);
            double endCos = Math.cos(endAngle);
            double endSin = Math.sin(endAngle);
            float halfWidth = 0.015f;
            Vec3 outerStart = feet.add(startCos * (radius + halfWidth), 0.0, startSin * (radius + halfWidth));
            Vec3 outerEnd = feet.add(endCos * (radius + halfWidth), 0.0, endSin * (radius + halfWidth));
            Vec3 innerEnd = feet.add(endCos * (radius - halfWidth), 0.0, endSin * (radius - halfWidth));
            Vec3 innerStart = feet.add(startCos * (radius - halfWidth), 0.0, startSin * (radius - halfWidth));
            ShapeRenderer.INSTANCE.addDepthTestedQuad(matrix, outerStart, outerEnd, innerEnd, innerStart, color);
        }
        float bloomOpacity = 0.8f * (0.7f + 0.3f * pulse);
        int first = withOpacity(gradientColor(friend, 0.0f), bloomOpacity);
        int second = withOpacity(gradientColor(friend, 1.0f), bloomOpacity);
        if (first == second) {
            SelectiveBloomRenderer.INSTANCE.add(feet, radius + 0.16f, first);
            return;
        }
        double firstAngle = -gradientRotation;
        Vec3 firstSource = feet.add(Math.cos(firstAngle) * radius * 0.55, 0.0,
                Math.sin(firstAngle) * radius * 0.55);
        Vec3 secondSource = feet.add(-Math.cos(firstAngle) * radius * 0.55, 0.0,
                -Math.sin(firstAngle) * radius * 0.55);
        SelectiveBloomRenderer.INSTANCE.add(firstSource, radius * 0.85f + 0.1f, first);
        SelectiveBloomRenderer.INSTANCE.add(secondSource, radius * 0.85f + 0.1f, second);
    }

    private int gradientColor(boolean friend, float position) {
        return friend ? ColorUtil.interpolateOklab(FRIEND_GREEN, FRIEND_GREEN_LIGHT, position)
                : this.color.getGradientColor(position);
    }

    private static int withOpacity(int color, float opacity) {
        return ColorUtil.replAlpha(color,
                Math.clamp(Math.round(ColorUtil.alpha(color) * opacity), 0, 255));
    }
}
