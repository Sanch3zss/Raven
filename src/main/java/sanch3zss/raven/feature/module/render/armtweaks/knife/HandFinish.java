package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

enum HandFinish {
    GLOVE(1.0f, 1.0f, 0.7f, 0.15f, 0.0f, 0.55f, true),
    SKIN(1.0f, 0.8f, 0.5f, 0.35f, 0.35f, 0.15f, true),
    CLOTH(1.0f, 1.0f, 0.7f, 0.15f, 0.0f, 0.55f, false);

    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("raven", "core/knife/hands");
    private final float roughness;
    private final float detail;
    private final float specular;
    private final float wrap;
    private final float subsurface;
    private final float sheen;
    private final boolean cull;
    private final Map<Identifier, RenderType> renderTypes = new HashMap<>();
    private RenderPipeline pipeline;

    HandFinish(float roughness, float detail, float specular, float wrap, float subsurface, float sheen, boolean cull) {
        this.roughness = roughness;
        this.detail = detail;
        this.specular = specular;
        this.wrap = wrap;
        this.subsurface = subsurface;
        this.sheen = sheen;
        this.cull = cull;
    }

    RenderType renderType(Identifier texture) {
        return renderTypes.computeIfAbsent(texture, this::createRenderType);
    }

    private RenderType createRenderType(Identifier texture) {
        String name = "hands_" + name().toLowerCase(Locale.ROOT);
        if (pipeline == null) {
            pipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("raven", "pipeline/" + name))
                    .withVertexShader(SHADER)
                    .withFragmentShader(SHADER)
                    .withShaderDefine("ROUGHNESS", roughness)
                    .withShaderDefine("DETAIL", detail)
                    .withShaderDefine("SPECULAR", specular)
                    .withShaderDefine("WRAP", wrap)
                    .withShaderDefine("SUBSURFACE", subsurface)
                    .withShaderDefine("SHEEN", sheen)
                    .withCull(cull)
                    .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withBindGroupLayout(ViewmodelLighting.LAYOUT)
                    .build());
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
