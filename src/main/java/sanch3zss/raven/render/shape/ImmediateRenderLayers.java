package sanch3zss.raven.render.shape;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public final class ImmediateRenderLayers {
    private static final Map<String, RenderType> CACHE = new ConcurrentHashMap<>();

    private ImmediateRenderLayers() {
    }

    public static void draw(MeshData buffer, String name, VertexFormat format, PrimitiveTopology mode, Identifier texture, boolean additive, boolean depthTest, boolean cull) {
        draw(buffer, name, format, mode, texture, additive, depthTest,
                depthTest, cull);
    }

    public static void draw(MeshData buffer, String name,
                            VertexFormat format, PrimitiveTopology mode,
                            Identifier texture, boolean additive,
                            boolean depthTest, boolean depthWrite,
                            boolean cull) {
        String key = name + "_" + format.hashCode() + "_" + mode.name()
                + "_" + texture + "_" + additive + "_" + depthTest
                + "_" + depthWrite + "_" + cull;
        RenderType layer = CACHE.computeIfAbsent(key, ignored ->
                create(key, format, mode, texture, additive,
                        depthTest, depthWrite, cull));
        draw(layer, buffer);
    }

    public static void draw(RenderType layer, MeshData buffer) {
        if (buffer == null) {
            return;
        }
        try {
            MeshData.DrawState drawState = buffer.drawState();
            if (drawState.vertexCount() == 0) {
                return;
            }
            ByteBuffer vertexData = buffer.vertexBuffer();
            GpuDevice device = RenderSystem.getDevice();
            GpuBuffer vertexGpuBuffer = device.createBuffer(() -> "Raven Immediate Vertex", GpuBuffer.USAGE_VERTEX, vertexData);
            RenderSystem.AutoStorageIndexBuffer autoIndices = RenderSystem.getSequentialBuffer(drawState.primitiveTopology());
            GpuBuffer indexGpuBuffer = autoIndices.getBuffer(drawState.indexCount());
            PreparedRenderType prepared = layer.prepare();
            prepared.drawFromBuffer(vertexGpuBuffer, indexGpuBuffer, autoIndices.type(), 0, 0, drawState.indexCount());
            vertexGpuBuffer.close();
        } finally {
            buffer.close();
        }
    }

    private static RenderType create(String key, VertexFormat format,
                                      PrimitiveTopology mode,
                                      Identifier texture, boolean additive,
                                      boolean depthTest, boolean depthWrite,
                                      boolean cull) {
        RenderPipeline.Snippet snippet = texture == null ? RenderPipelines.DEBUG_FILLED_SNIPPET : RenderPipelines.GUI_TEXTURED_SNIPPET;
        RenderPipeline pipeline = RenderPipeline.builder(snippet)
            .withLocation(Identifier.fromNamespaceAndPath("raven", "immediate/" + Integer.toUnsignedString(key.hashCode(), 16)))
            .withVertexBinding(0, format)
            .withPrimitiveTopology(mode)
            .withCull(cull)
            .withDepthStencilState(new DepthStencilState(depthTest ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, depthWrite))
            .withColorTargetState(new ColorTargetState(additive ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT))
            .build();
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(pipeline).sortOnUpload();
        if (texture != null) {
            setup.withTexture("Sampler0", texture);
        }
        return RenderType.create("raven_immediate_" + Integer.toUnsignedString(key.hashCode(), 16), setup.createRenderSetup());
    }
}
