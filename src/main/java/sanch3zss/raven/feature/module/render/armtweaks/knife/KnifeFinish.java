package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

enum KnifeFinish {
    STEEL(0.2f, 0.32f, 0.18f, 1.35f, false),
    SATIN(0.4f, 0.32f, 0.18f, 1.25f, false),
    PAINTED(0.2f, 0.32f, 0.18f, 1.0f, true);

    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("raven", "core/knife/metal");
    private final float roughness;
    private final float metalSaturation;
    private final float metalDark;
    private final float metalGain;
    private final boolean atlas;
    private final Map<Identifier, RenderType> renderTypes = new HashMap<>();
    private RenderPipeline pipeline;

    KnifeFinish(float roughness, float metalSaturation, float metalDark, float metalGain, boolean atlas) {
        this.roughness = roughness;
        this.metalSaturation = metalSaturation;
        this.metalDark = metalDark;
        this.metalGain = metalGain;
        this.atlas = atlas;
    }

    float uScale() {
        return atlas ? 0.5f : 1.0f;
    }

    RenderType renderType(Identifier texture) {
        return renderTypes.computeIfAbsent(texture, this::createRenderType);
    }

    private RenderType createRenderType(Identifier texture) {
        String name = "knife_" + name().toLowerCase(Locale.ROOT);
        if (pipeline == null) {
            RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("raven", "pipeline/" + name))
                    .withVertexShader(SHADER)
                    .withFragmentShader(SHADER)
                    .withShaderDefine("ALPHA_CUTOUT", 0.1f)
                    .withShaderDefine("BASE_ROUGHNESS", roughness)
                    .withShaderDefine("METAL_SATURATION", metalSaturation)
                    .withShaderDefine("METAL_DARK", metalDark)
                    .withShaderDefine("METAL_GAIN", metalGain)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withBindGroupLayout(ViewmodelLighting.LAYOUT)
                    .withCull(false);
            if (atlas) {
                builder.withShaderDefine("ATLAS");
            }
            pipeline = RenderPipelines.register(builder.build());
        }
        RenderSetup setup = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", texture)
                .withTexture("Sampler3", ViewmodelLighting.TEXTURE)
                .useLightmap()
                .useOverlay()
                .createRenderSetup();
        return RenderType.create("raven_" + name, setup);
    }
}
