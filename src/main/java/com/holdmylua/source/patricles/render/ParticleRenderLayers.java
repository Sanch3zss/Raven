
package com.holdmylua.source.patricles.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

public class ParticleRenderLayers {
    private static final Function<Identifier, RenderType> ADDITIVE_PARTICLE_LAYER;
    static BlendFunction ADDITIVE;
    static RenderPipeline ADDITIVE_PARTICLE;

    public static RenderType additiveParticle(Identifier texture) {
        return ADDITIVE_PARTICLE_LAYER.apply(texture);
    }

    static {
        ADDITIVE = new BlendFunction(BlendFactor.SRC_ALPHA, BlendFactor.ONE);
        ADDITIVE_PARTICLE = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation("pipeline/additive_particle_effect")
                .withDepthStencilState(Optional.empty())
                .withColorTargetState(new ColorTargetState(ADDITIVE))
                .build());
        ADDITIVE_PARTICLE_LAYER = Util.memoize(texture -> RenderType.create("fire_screen_effect", RenderSetup.builder(ADDITIVE_PARTICLE).withTexture("Sampler0", texture).createRenderSetup()));
    }
}

