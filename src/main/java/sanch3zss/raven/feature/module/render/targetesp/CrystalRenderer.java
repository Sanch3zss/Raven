package sanch3zss.raven.feature.module.render.targetesp;


import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.render.theme.ColorUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.resource.*;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.render.gl.GlStateSnapshot;
import sanch3zss.raven.render.shader.*;
import sanch3zss.raven.util.math.MathUtil;

public enum CrystalRenderer {
    INSTANCE;

    private static final Identifier GLOW_TEXTURE = Identifier.fromNamespaceAndPath("raven", "textures/crystal_glow.png");
    private static final Vector3f[] VERTICES = {
            new Vector3f(0, 1.55f, 0), new Vector3f(0, -1.55f, 0),
            new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0),
            new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)
    };
    private static final int[][] FACES = {
            {0, 4, 2}, {0, 3, 4}, {0, 5, 3}, {0, 2, 5},
            {1, 2, 4}, {1, 4, 3}, {1, 3, 5}, {1, 5, 2}
    };

    private Mesh surfaceMesh;
    private Mesh glowMesh;
    private ShaderProgram crsShader;
    private ShaderUniform mode;
    private ShaderUniform surfaceMvp, surfaceModelView;
    private ShaderUniform surfaceColor, surfaceTime, surfaceRoundness, surfaceHighlight;
    private ShaderUniform glowMvp, glowTexture, glowTime, glowIntensity, glowRadius, glowPulse, glowBloom;

    public void render(WorldRenderEvent event, LivingEntity target, int firstColor, int secondColor, float alpha,
                       float previousRotation, float rotation, boolean throughWalls,
                       boolean distortion) {
        if (target == null || alpha <= 0) return;
        initShaders();

        GlStateSnapshot state = GlStateSnapshot.create(2);
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            float tickDelta = Mc.INSTANCE.getTickDelta();
            float smoothRotation = MathUtil.interpolate(previousRotation, rotation, tickDelta);
            Vec3 targetPos = new Vec3(
                    MathUtil.interpolate(target.xo, target.getX(), tickDelta),
                    MathUtil.interpolate(target.yo, target.getY(), tickDelta),
                    MathUtil.interpolate(target.zo, target.getZ(), tickDelta));
            Vec3 cameraPos = Mc.INSTANCE.getCamera().position();
            Vec3 renderPos = targetPos.subtract(cameraPos);
            AABB targetBox = target.getBoundingBox().move(targetPos.subtract(
                    target.getX(), target.getY(), target.getZ()));
            float easedAlpha = Mth.clamp(alpha, 0, 1);
            float visibleAlpha = easedAlpha;
            float entityWidth = target.getBbWidth() * 1.5f;
            float entityHeight = target.getBbHeight();

            renderVisibleCrystals(event, renderPos, entityWidth, entityHeight, smoothRotation,
                    easedAlpha, visibleAlpha, firstColor, secondColor, targetPos, targetBox, cameraPos, throughWalls);
            if (distortion) {
                DistortionRenderer.INSTANCE.render(event, target, alpha,
                        previousRotation, rotation, throughWalls);
            }
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glDepthMask(previousDepthMask);
            state.revert();
        }
    }

    public void prepare(boolean distortion) {
        initShaders();
        if (crsShader.programId != null && surfaceMesh.vao != null && glowMesh.vao != null
                && (!distortion || !DistortionRenderer.INSTANCE.needsPreparation())) {
            return;
        }
        GlStateSnapshot state = GlStateSnapshot.create(2);
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            if (crsShader.programId == null) {
                crsShader.bind();
                crsShader.unbind();
            }
            surfaceMesh.prepare();
            glowMesh.prepare();
            FramebufferUtil.getTextureId(Mc.INSTANCE.getTextureManager().getTexture(GLOW_TEXTURE));
            if (distortion) {
                DistortionRenderer.INSTANCE.prepare();
            }
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glDepthMask(previousDepthMask);
            state.revert();
        }
    }

    private void initShaders() {
        if (surfaceMesh == null) {
            surfaceMesh = new Mesh(crystal(), 6, 3, 3);
            glowMesh = new Mesh(quad(), 5, 3, 2);
        }
        if (crsShader != null) return;
        ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
        crsShader = new ShaderProgram(resources.route("shaders/effects/crystal/crs.fsh"),
                resources.route("shaders/effects/crystal/crs.vsh"));
        mode = crsShader.uniform("Mode");
        surfaceMvp = glowMvp = crsShader.uniform("Mvp");
        surfaceModelView = crsShader.uniform("ModelViewMat");
        surfaceColor = crsShader.uniform("BaseColor");
        surfaceTime = glowTime = crsShader.uniform("Time");
        surfaceRoundness = crsShader.uniform("Roundness");
        surfaceHighlight = crsShader.uniform("HighlightStrength");
        glowTexture = crsShader.uniform("Sampler0");
        glowIntensity = crsShader.uniform("GlowIntensity");
        glowRadius = crsShader.uniform("GlowRadius");
        glowPulse = crsShader.uniform("PulseSpeed");
        glowBloom = crsShader.uniform("BloomStrength");
    }

    private void renderVisibleCrystals(WorldRenderEvent event, Vec3 renderPos, float entityWidth,
                                       float entityHeight, float rotation, float radiusProgress,
                                       float visibleAlpha, int firstColor, int secondColor,
                                       Vec3 targetPos, AABB targetBox,
                                       Vec3 cameraPos, boolean throughWalls) {
        PoseStack matrices = event.matrixStack();
        matrices.pushPose();
        matrices.translate(renderPos.x, renderPos.y, renderPos.z);
        GL11.glEnable(GL11.GL_BLEND);
        GL20.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        if (throughWalls) GL11.glDisable(GL11.GL_DEPTH_TEST);
        else GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        float time = (System.currentTimeMillis() % 600_000L) / 1000.0f;
        crsShader.bind();
        mode.uploadInt(0);
        surfaceTime.uploadFloat(time);
        surfaceRoundness.uploadFloat(0.72f);
        surfaceHighlight.uploadFloat(0.88f);
        forEachCrystal(matrices, entityWidth, entityHeight, rotation, radiusProgress, true,
                targetPos, targetBox, cameraPos, throughWalls, (stack, angle) -> {
            int color = ColorUtil.gradientWave(firstColor, secondColor, angle / 336.0f);
            surfaceColor.uploadVec3(((color >> 16) & 255) / 255f,
                    ((color >> 8) & 255) / 255f, (color & 255) / 255f);
            surfaceModelView.uploadMatrix4f(new Matrix4f(stack.last().pose()));
            surfaceMvp.uploadMatrix4f(mvp(event, stack));
            surfaceMesh.draw(visibleAlpha * 0.94f, 0.0f, 0.0f, 0.0f);
        });
        crsShader.unbind();

        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL20.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0f, -1.0f);
        bindTexture(0, FramebufferUtil.getTextureId(Mc.INSTANCE.getTextureManager().getTexture(GLOW_TEXTURE)));
        crsShader.bind();
        mode.uploadInt(1);
        glowTexture.uploadInt(0);
        glowTime.uploadFloat(time);
        glowIntensity.uploadFloat(1.18f);
        glowRadius.uploadFloat(1.05f);
        glowPulse.uploadFloat(1.65f);
        glowBloom.uploadFloat(1.12f);
        forEachCrystal(matrices, entityWidth, entityHeight, rotation, radiusProgress, false,
                targetPos, targetBox, cameraPos, throughWalls, (stack, angle) -> {
            int color = ColorUtil.gradientWave(firstColor, secondColor, angle / 336.0f);
            float r = ((color >> 16) & 255) / 255f;
            float g = ((color >> 8) & 255) / 255f;
            float b = (color & 255) / 255f;
            billboard(stack);
            float phase = rotation * 0.055f + angle * 0.37f;
            stack.translate((float) Math.sin(phase) * 0.045f,
                    (float) Math.cos(phase * 1.7f) * 0.075f, 0);
            glowMvp.uploadMatrix4f(mvp(event, new Matrix4f(stack.last().pose()).scale(0.88f, 0.88f, 1.0f)));
            glowMesh.draw(r, g, b, visibleAlpha * 0.13f);

            float outerGlowAlpha = visibleAlpha * (0.032f + 0.014f * (float) Math.sin(phase));
            float haloR = Mth.lerp(0.18f, r, 1.0f);
            float haloG = Mth.lerp(0.18f, g, 1.0f);
            float haloB = Mth.lerp(0.18f, b, 1.0f);
            glowMvp.uploadMatrix4f(mvp(event, new Matrix4f(stack.last().pose()).scale(1.232f, 1.232f, 1.0f)));
            glowMesh.draw(haloR, haloG, haloB, outerGlowAlpha);
        });

        crsShader.unbind();
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        matrices.popPose();
    }

    private void forEachCrystal(PoseStack matrices, float entityWidth, float entityHeight,
                                float rotation, float alpha, boolean orient, Vec3 targetPos,
                                AABB targetBox, Vec3 cameraPos, boolean occludeByTarget,
                                CrystalConsumer consumer) {
        for (int angle = 0; angle < 336; angle += 20) {
            float scale = 1.0f - 0.3f * alpha;
            float x = (float) Math.cos(Math.toRadians(angle + rotation * 2)) * entityWidth * scale;
            float z = (float) Math.sin(Math.toRadians(angle + rotation * 2)) * entityWidth * scale;
            float y = crystalYOffset(angle, entityHeight);
            if (occludeByTarget && isBehindTarget(targetBox, cameraPos, targetPos.add(x, y, z))) {
                continue;
            }
            matrices.pushPose();
            matrices.translate(x, y, z);
            if (orient) {
                Vector3f direction = new Vector3f(-x, entityHeight * 0.5f - 1, -z).normalize();
                matrices.mulPose(new Quaternionf().rotationTo(new Vector3f(0, 1, 0), direction));
                matrices.scale(0.126f, 0.126f, 0.126f);
            }
            consumer.accept(matrices, angle);
            matrices.popPose();
        }
    }

    private static boolean isBehindTarget(AABB targetBox, Vec3 cameraPos, Vec3 point) {
        return targetBox.clip(cameraPos, point)
                .filter(hit -> cameraPos.distanceToSqr(hit) + 1.0E-4d
                        < cameraPos.distanceToSqr(point))
                .isPresent();
    }

    private static float crystalYOffset(int angle, float entityHeight) {
        float y = 0.1f + entityHeight * Math.abs((float) Math.cos(angle));
        if (angle == 220) {
            y -= entityHeight * 0.18f;
        }
        return y;
    }

    private static void billboard(PoseStack matrices) {
        matrices.mulPose(Axis.YP.rotationDegrees(-Mc.INSTANCE.getCamera().yRot()));
        matrices.mulPose(Axis.XP.rotationDegrees(Mc.INSTANCE.getCamera().xRot()));
    }

    private static Matrix4f mvp(WorldRenderEvent event, PoseStack matrices) {
        return new Matrix4f(event.projectionMatrix()).mul(matrices.last().pose());
    }

    private static Matrix4f mvp(WorldRenderEvent event, Matrix4f matrix) {
        return new Matrix4f(event.projectionMatrix()).mul(matrix);
    }

    private static float[] crystal() {
        float[] data = new float[FACES.length * 3 * 6];
        int offset = 0;
        for (int[] face : FACES) {
            for (int index : face) {
                Vector3f vertex = VERTICES[index];
                Vector3f normal = new Vector3f(vertex.x, vertex.y / 1.55f, vertex.z).normalize();
                data[offset++] = vertex.x;
                data[offset++] = vertex.y;
                data[offset++] = vertex.z;
                data[offset++] = normal.x;
                data[offset++] = normal.y;
                data[offset++] = normal.z;
            }
        }
        return data;
    }

    private static float[] quad() {
        return new float[]{
                -0.5f, -0.5f, 0, 0, 0, 0.5f, -0.5f, 0, 1, 0,
                 0.5f,  0.5f, 0, 1, 1, -0.5f, -0.5f, 0, 0, 0,
                 0.5f,  0.5f, 0, 1, 1, -0.5f,  0.5f, 0, 0, 1
        };
    }

    private static void bindTexture(int unit, int texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }

    @FunctionalInterface
    private interface CrystalConsumer {
        void accept(PoseStack matrices, int angle);
    }

    private static final class Mesh {
        private final float[] data;
        private final int stride;
        private final int[] attributes;
        private Integer vao;
        private Integer vbo;

        private Mesh(float[] data, int stride, int... attributes) {
            this.data = data;
            this.stride = stride;
            this.attributes = attributes;
        }

        void draw(float x, float y, float z, float w) {
            prepare();
            GL30.glBindVertexArray(vao);
            GL20.glDisableVertexAttribArray(2);
            GL20.glVertexAttrib4f(2, x, y, z, w);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, data.length / stride);
        }

        void prepare() {
            if (vao == null) {
                vao = GL30.glGenVertexArrays();
                vbo = GL15.glGenBuffers();
                GL30.glBindVertexArray(vao);
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
                GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
                int offset = 0;
                for (int i = 0; i < attributes.length; i++) {
                    GL20.glEnableVertexAttribArray(i);
                    GL20.glVertexAttribPointer(i, attributes[i], GL11.GL_FLOAT, false,
                            stride * Float.BYTES, (long) offset * Float.BYTES);
                    offset += attributes[i];
                }
            }
        }
    }
}
