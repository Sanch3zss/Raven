package sanch3zss.raven.render.shape;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

public class RenderShapeState {
    public static boolean textureEnabled;
    public static final RenderShapeState POSITION_COLOR_TRIANGLES = new RenderShapeState(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR, false, false, false, false, 1.0f, null);
    public static final RenderShapeState POSITION_COLOR_QUADS = new RenderShapeState(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR, false, false, false, false, 1.0f, null);
    public static final RenderShapeState POSITION_COLOR_QUADS_DEPTH = new RenderShapeState(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR, true, false, true, false, 1.0f, null);
    public static final RenderShapeState POSITION_COLOR_LINES = new RenderShapeState(
            PrimitiveTopology.LINES,
            DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
            true, false, false, false, 1.0f, null
    );
    public static final RenderShapeState POSITION_COLOR_LINES_DEPTH = new RenderShapeState(
            PrimitiveTopology.LINES,
            DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH,
            true, false, true, false, 1.0f, null
    );

    private static final java.util.Map<String, RenderShapeState> TEXTURE_QUAD_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Identifier SOLID_MARKER_SHADER = Identifier.fromNamespaceAndPath("raven", "core/solid_marker");

    public static RenderShapeState getTextureQuadState(Identifier texture, boolean depthTest, boolean blend, boolean additiveBlend) {
        String key = texture + "_" + depthTest + "_" + blend + "_" + additiveBlend;
        return TEXTURE_QUAD_CACHE.computeIfAbsent(key, k -> new RenderShapeState(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR, blend, additiveBlend, depthTest, false, 1.0f, texture));
    }

    public static RenderShapeState getSolidMarkerState(Identifier texture) {
        return TEXTURE_QUAD_CACHE.computeIfAbsent(texture + "_solid_marker", k ->
                new RenderShapeState(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR,
                        true, false, false, false, 1.0f, texture, SOLID_MARKER_SHADER));
    }

    private final PrimitiveTopology mode;
    private final VertexFormat format;
    private final boolean blend;
    private final boolean additiveBlend;
    private final boolean depthTest;
    private final boolean cull;
    private final float lineWidth;
    private final Identifier texture;
    private final RenderType layer;

    public RenderShapeState() {
        this(PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR, true, false, true, false, 1.0f, null);
    }

    public RenderShapeState(PrimitiveTopology mode, VertexFormat format, boolean blend, boolean additiveBlend, boolean depthTest, boolean cull, float lineWidth, Identifier texture) {
        this(mode, format, blend, additiveBlend, depthTest, cull, lineWidth, texture, null);
    }

    private RenderShapeState(PrimitiveTopology mode, VertexFormat format, boolean blend,
                             boolean additiveBlend, boolean depthTest, boolean cull,
                             float lineWidth, Identifier texture, Identifier shader) {
        this.mode = mode != null ? mode : PrimitiveTopology.TRIANGLES;
        this.format = format != null ? format : DefaultVertexFormat.POSITION_COLOR;
        this.blend = blend;
        this.additiveBlend = additiveBlend;
        this.depthTest = depthTest;
        this.cull = cull;
        this.lineWidth = lineWidth;
        this.texture = texture;
        boolean lines = this.mode == PrimitiveTopology.LINES;
        RenderPipeline.Snippet snippet = lines
                ? RenderPipelines.LINES_SNIPPET
                : texture == null
                    ? RenderPipelines.DEBUG_FILLED_SNIPPET
                    : RenderPipelines.GUI_TEXTURED_SNIPPET;
        String key = this.mode.name().toLowerCase() + "_" + Integer.toUnsignedString(hashCode(), 16);
        RenderPipeline.Builder builder = RenderPipeline.builder(snippet)
            .withLocation(Identifier.fromNamespaceAndPath("raven", "shape/" + key))
            .withVertexBinding(0, this.format)
            .withPrimitiveTopology(this.mode)
            .withCull(this.cull)
            .withDepthStencilState(new DepthStencilState(this.depthTest ? CompareOp.GREATER_THAN_OR_EQUAL : CompareOp.ALWAYS_PASS, !this.blend && this.depthTest));
        if (shader != null) {
            builder.withVertexShader(shader).withFragmentShader(shader);
        }
        if (this.blend) {
            builder.withColorTargetState(new ColorTargetState(this.additiveBlend ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT));
        } else {
            builder.withColorTargetState(ColorTargetState.DEFAULT);
        }
        RenderPipeline pipeline = builder.build();
        RenderSetup.RenderSetupBuilder setup = RenderSetup.builder(
                shader == null ? pipeline : RenderPipelines.register(pipeline));
        if (texture != null) {
            setup.withTexture("Sampler0", texture);
        }
        this.layer = RenderType.create("raven_shape_" + key, setup.createRenderSetup());
    }

    public boolean additiveBlend() { return additiveBlend; }
    public boolean blend() { return blend; }
    public boolean cull() { return cull; }
    public boolean depthTest() { return depthTest; }
    public VertexFormat format() { return format; }
    public float lineWidth() { return lineWidth; }
    public PrimitiveTopology mode() { return mode; }
    public Identifier texture() { return texture; }
    public RenderType layer() { return layer; }
}
