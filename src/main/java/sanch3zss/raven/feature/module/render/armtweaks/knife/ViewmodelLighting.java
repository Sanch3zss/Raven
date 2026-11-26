package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

final class ViewmodelLighting {
    static final ViewmodelLighting INSTANCE = new ViewmodelLighting();
    static final Identifier TEXTURE = Identifier.fromNamespaceAndPath("raven", "dynamic/viewmodel_lighting");
    static final BindGroupLayout LAYOUT = BindGroupLayout.builder().withSampler("Sampler3").build();
    private static final float SHADOW_DISTANCE = 160.0f;
    private DynamicTexture texture;
    private float visibility = 1.0f;
    private long lastUpdate;

    private ViewmodelLighting() {}

    void update(AbstractClientPlayer player, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) return;
        if (texture == null) {
            texture = new DynamicTexture(() -> "Raven viewmodel lighting", 4, 1, false);
            minecraft.getTextureManager().register(TEXTURE, texture);
        }
        long now = System.nanoTime();
        if (lastUpdate != 0L && now - lastUpdate < 1_000_000L) return;
        float delta = lastUpdate == 0L ? 1.0f : Math.min(1.0f, (now - lastUpdate) * 1.0e-9f);
        lastUpdate = now;

        EnvironmentAttributeProbe probe = minecraft.gameRenderer.mainCamera().attributeProbe();
        boolean sky = level.dimensionType().skybox() == DimensionType.Skybox.OVERWORLD;
        float sunAngle = probe.getValue(EnvironmentAttributes.SUN_ANGLE, partialTick) * Mth.DEG_TO_RAD;
        float moonAngle = probe.getValue(EnvironmentAttributes.MOON_ANGLE, partialTick) * Mth.DEG_TO_RAD;
        Vector3f sun = new Vector3f(-Mth.sin(sunAngle), Mth.cos(sunAngle), 0.0f);
        Vector3f moon = new Vector3f(-Mth.sin(moonAngle), Mth.cos(moonAngle), 0.0f);
        float weather = (1.0f - level.getRainLevel(partialTick) * 0.7f) * (1.0f - level.getThunderLevel(partialTick) * 0.6f);
        float moonPhase = probe.getValue(EnvironmentAttributes.MOON_PHASE, partialTick).index();
        float moonlight = 0.2f + 0.8f * (0.5f + 0.5f * Mth.cos(moonPhase * Mth.PI / 4.0f));
        float sunStrength = sky ? smoothstep(-0.04f, 0.2f, sun.y) * weather : 0.0f;
        float moonStrength = sky ? smoothstep(-0.04f, 0.2f, moon.y) * weather * moonlight : 0.0f;

        boolean daytime = sunStrength * 0.9f >= moonStrength * 0.12f;
        Vector3f direction = daytime ? sun : moon;
        Vector3f color;
        if (daytime) {
            float height = smoothstep(0.0f, 0.45f, sun.y);
            color = new Vector3f(1.0f, 0.48f + 0.47f * height, 0.24f + 0.64f * height).mul(0.9f * sunStrength);
        } else {
            color = new Vector3f(0.45f, 0.55f, 0.8f).mul(0.12f * moonStrength);
        }

        Vec3 eye = player.getEyePosition(partialTick);
        float target = 0.0f;
        if (Math.max(color.x, Math.max(color.y, color.z)) > 0.001f) {
            Vec3 end = eye.add(direction.x * SHADOW_DISTANCE, direction.y * SHADOW_DISTANCE, direction.z * SHADOW_DISTANCE);
            HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
            float skyAccess = level.getBrightness(LightLayer.SKY, BlockPos.containing(eye)) / 15.0f;
            target = hit.getType() == HitResult.Type.MISS ? skyAccess : 0.0f;
        }
        visibility += (target - visibility) * Math.min(1.0f, delta * 6.0f);

        int skyColor = probe.getValue(EnvironmentAttributes.SKY_COLOR, partialTick);
        int glow = sky ? probe.getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, partialTick) : 0;
        texture.getPixels().setPixel(0, 0, ARGB.color(unit(visibility),
                unit(direction.x * 0.5f + 0.5f), unit(direction.y * 0.5f + 0.5f), unit(direction.z * 0.5f + 0.5f)));
        texture.getPixels().setPixel(1, 0, ARGB.color(255, hdr(color.x), hdr(color.y), hdr(color.z)));
        texture.getPixels().setPixel(2, 0, ARGB.color(unit(weather), ARGB.red(skyColor), ARGB.green(skyColor), ARGB.blue(skyColor)));
        texture.getPixels().setPixel(3, 0, glow);
        texture.upload();
    }

    private static int unit(float value) {
        return Mth.clamp(Math.round(value * 255.0f), 0, 255);
    }

    private static int hdr(float value) {
        return unit((float) Math.sqrt(Math.max(value, 0.0f) / 4.0f));
    }

    private static float smoothstep(float from, float to, float value) {
        float t = Mth.clamp((value - from) / (to - from), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }
}
