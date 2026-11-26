package sanch3zss.raven.feature.module.render.jumpcircle;


import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.render.theme.ColorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.resource.ResourceRouter;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.render.gl.GlStateSnapshot;
import sanch3zss.raven.render.shader.ShaderProgram;
import sanch3zss.raven.render.shader.ShaderUniform;

public enum JumpCircleShaderRenderer {
    INSTANCE;

    public record Circle(Vec3 position, float diameter, float opacity) {}

    private static final float HALO_QUAD_SCALE = 2.0f;
    private static final int DISC_SEGMENTS = 192;
    private static final float[] DISC = createDisc();

    private ShaderProgram shader;
    private ShaderUniform mvp, time, firstColor, secondColor, opacity;
    private int vao;
    private int vbo;

    public void render(WorldRenderEvent event, List<Circle> circles, int first, int second) {
        if (circles.isEmpty()) return;

        GlStateSnapshot state = GlStateSnapshot.create(1);
        int oldFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        boolean oldDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int oldDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        try {
            initialize();
            FramebufferUtil.bindForRendering(Mc.INSTANCE.getFramebuffer());
            GL11.glEnable(GL11.GL_BLEND);
            GL14.glBlendEquation(GL14.GL_FUNC_ADD);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_GEQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);

            shader.bind();
            GL30.glBindVertexArray(vao);
            time.uploadFloat((System.nanoTime() % 3_600_000_000_000L) / 1_000_000_000.0f);
            uploadColor(firstColor, first);
            uploadColor(secondColor, second);

            PoseStack matrices = event.matrixStack();
            for (Circle circle : circles) {
                if (circle.diameter <= 0.0f || circle.opacity <= 0.0f) continue;
                matrices.pushPose();
                try {
                    float quadSize = circle.diameter * HALO_QUAD_SCALE;
                    double half = quadSize * 0.5;
                    matrices.translate(circle.position.x - half, circle.position.y,
                            circle.position.z - half);
                    matrices.mulPose(Axis.XP.rotationDegrees(90.0f));
                    matrices.scale(quadSize, quadSize, 1.0f);
                    mvp.uploadMatrix4f(new Matrix4f(event.projectionMatrix()).mul(matrices.last().pose()));
                    opacity.uploadFloat(circle.opacity);
                    GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, DISC.length / 5);
                } finally {
                    matrices.popPose();
                }
            }
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, oldFramebuffer);
            GL11.glDepthFunc(oldDepthFunc);
            GL11.glDepthMask(oldDepthMask);
            state.revert();
        }
    }

    private void initialize() {
        if (shader == null) {
            ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
            shader = new ShaderProgram(resources.route("shaders/effects/jump_circle/circle.fsh"),
                    resources.route("shaders/effects/jump_circle/circle.vsh"));
            mvp = shader.uniform("Mvp");
            time = shader.uniform("Time");
            firstColor = shader.uniform("FirstColor");
            secondColor = shader.uniform("SecondColor");
            opacity = shader.uniform("Opacity");
        }
        if (vao != 0) return;

        vao = GL30.glGenVertexArrays();
        vbo = GL15.glGenBuffers();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, DISC, GL15.GL_STATIC_DRAW);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0L);
        GL20.glEnableVertexAttribArray(1);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES,
                3L * Float.BYTES);
    }

    private static void uploadColor(ShaderUniform uniform, int color) {
        uniform.uploadVec4(ColorUtil.red(color) / 255.0f, ColorUtil.green(color) / 255.0f,
                ColorUtil.blue(color) / 255.0f, ColorUtil.alpha(color) / 255.0f);
    }

    private static float[] createDisc() {
        float[] vertices = new float[DISC_SEGMENTS * 15];
        for (int segment = 0; segment < DISC_SEGMENTS; segment++) {
            double angle0 = Math.PI * 2.0 * segment / DISC_SEGMENTS;
            double angle1 = Math.PI * 2.0 * (segment + 1) / DISC_SEGMENTS;
            int offset = segment * 15;
            putVertex(vertices, offset, 0.5f, 0.5f);
            putVertex(vertices, offset + 5,
                    0.5f + 0.5f * (float) Math.cos(angle0),
                    0.5f + 0.5f * (float) Math.sin(angle0));
            putVertex(vertices, offset + 10,
                    0.5f + 0.5f * (float) Math.cos(angle1),
                    0.5f + 0.5f * (float) Math.sin(angle1));
        }
        return vertices;
    }

    private static void putVertex(float[] vertices, int offset, float x, float y) {
        vertices[offset] = x;
        vertices[offset + 1] = y;
        vertices[offset + 2] = 0.0f;
        vertices[offset + 3] = x;
        vertices[offset + 4] = y;
    }
}
