package sanch3zss.raven.feature.module.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import sanch3zss.raven.client.Raven;

public enum CustomSkyRenderer {
    INSTANCE;

    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("raven", "core/sky/custom");
    private static final Identifier GRADIENT = Identifier.fromNamespaceAndPath("raven", "dynamic/custom_sky_gradient");
    private RenderPipeline pipeline;
    private final int[] colors = new int[256];
    private GpuBuffer dome;
    private DynamicTexture gradient;
    private float night;

    public void starBrightness(float starBrightness) {
        this.night = Math.min(1.0f, starBrightness * 2.0f);
    }

    public boolean active() {
        Raven raven = Raven.INSTANCE;
        if (raven == null || raven.moduleRepository() == null) return false;
        AmbienceModule module = raven.moduleRepository().get(AmbienceModule.class);
        return module.isState() && module.customSky.isValue();
    }

    public boolean render(RenderTarget target, int skyColor) {
        if (!active()) return false;
        AmbienceModule module = Raven.INSTANCE.moduleRepository().get(AmbienceModule.class);
        updateGradient(module);
        if (this.dome == null) this.dome = buildDome();
        if (this.pipeline == null) this.pipeline = pipeline();
        float time = (Util.getMillis() % 3_600_000L) / 1000.0f;
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(RenderSystem.getModelViewMatrixCopy(),
                ARGB.vector4fFromARGB32(skyColor), new Vector3f(time, this.night, 0.0f), new Matrix4f());
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Raven sky",
                target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(this.pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);
            pass.bindTexture("Sampler0", this.gradient.getTextureView(), this.gradient.getSampler());
            pass.setVertexBuffer(0, this.dome.slice());
            pass.draw(36, 1, 0, 0);
        }
        return true;
    }

    private void updateGradient(AmbienceModule module) {
        boolean changed = this.gradient == null;
        for (int i = 0; i < this.colors.length; i++) {
            int color = module.skyColor.getGradientColor(i / 255.0f);
            if (this.colors[i] != color) {
                this.colors[i] = color;
                changed = true;
            }
        }
        if (!changed) return;
        if (this.gradient == null) {
            this.gradient = new DynamicTexture(() -> "Raven sky gradient", new NativeImage(this.colors.length, 1, false));
            Minecraft.getInstance().getTextureManager().register(GRADIENT, this.gradient);
        }
        NativeImage pixels = this.gradient.getPixels();
        for (int i = 0; i < this.colors.length; i++) {
            pixels.setPixel(i, 0, 0xFF000000 | this.colors[i] & 0xFFFFFF);
        }
        this.gradient.upload();
    }

    private static RenderPipeline pipeline() {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_FOG_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("raven", "pipeline/custom_sky"))
                .withVertexShader(SHADER)
                .withFragmentShader(SHADER)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withVertexBinding(0, DefaultVertexFormat.POSITION)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .build());
    }

    private static GpuBuffer buildDome() {
        float[][] corners = {
                {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
                {-1, -1, 1}, {1, -1, 1}, {1, 1, 1}, {-1, 1, 1}
        };
        int[] faces = {0, 1, 2, 0, 2, 3, 5, 4, 7, 5, 7, 6, 4, 0, 3, 4, 3, 7, 1, 5, 6, 1, 6, 2, 3, 2, 6, 3, 6, 7, 4, 5, 1, 4, 1, 0};
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(faces.length * DefaultVertexFormat.POSITION.getVertexSize())) {
            BufferBuilder builder = new BufferBuilder(bytes, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION);
            for (int index : faces) {
                builder.addVertex(corners[index][0] * 100.0f, corners[index][1] * 100.0f, corners[index][2] * 100.0f);
            }
            try (MeshData mesh = builder.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(() -> "Raven sky dome", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
            }
        }
    }
}
