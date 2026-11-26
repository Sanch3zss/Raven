package sanch3zss.raven.feature.module.render.targetesp;


import sanch3zss.raven.render.gl.FramebufferUtil;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.resource.ResourceRouter;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.render.shader.FullscreenQuad;
import sanch3zss.raven.render.shader.ShaderProgram;
import sanch3zss.raven.render.shader.ShaderUniform;
import sanch3zss.raven.util.math.MathUtil;

public enum DistortionRenderer {
    INSTANCE;

    private static final int MAX_LENSES = 18;
    private static final float LENS_STRENGTH = 0.1f;

    private final FullscreenQuad fullscreenQuad = new FullscreenQuad();
    private final ShaderUniform[] lenses = new ShaderUniform[MAX_LENSES];
    private ShaderProgram shader;
    private ShaderUniform sceneSampler;
    private ShaderUniform depthSampler;
    private ShaderUniform lensCount;
    private ShaderUniform aspect;
    private ShaderUniform strength;
    private ShaderUniform respectDepth;
    private RenderTarget snapshot;
    private final float[] lensData = new float[MAX_LENSES * 4];
    private int visibleLenses;
    private float minU;
    private float minV;
    private float maxU;
    private float maxV;

    public void render(WorldRenderEvent event, LivingEntity target, float alpha,
                       float previousRotation, float rotation, boolean throughWalls) {
        if (target == null || alpha <= 0.0f) return;
        initShader();

        RenderTarget main = Mc.INSTANCE.getFramebuffer();
        int width = Mc.INSTANCE.getMinecraft().getWindow().getWidth();
        int height = Mc.INSTANCE.getMinecraft().getWindow().getHeight();
        if (width <= 0 || height <= 0 || main.getDepthTexture() == null) return;
        ensureFramebuffer(width, height);

        float tickDelta = Mc.INSTANCE.getTickDelta();
        float smoothRotation = MathUtil.interpolate(previousRotation, rotation, tickDelta);
        Vec3 targetPos = new Vec3(
                MathUtil.interpolate(target.xo, target.getX(), tickDelta),
                MathUtil.interpolate(target.yo, target.getY(), tickDelta),
                MathUtil.interpolate(target.zo, target.getZ(), tickDelta));
        Vec3 cameraPos = Mc.INSTANCE.getCamera().position();
        AABB targetBox = target.getBoundingBox().move(targetPos.subtract(
                target.getX(), target.getY(), target.getZ()));
        float entityWidth = target.getBbWidth() * 1.5f;
        float entityHeight = target.getBbHeight();
        Matrix4f projection = event.projectionMatrix();
        Matrix4f viewProjection = new Matrix4f(projection).mul(event.modelViewMatrix());
        projectLenses(viewProjection, projection, targetPos, targetBox, cameraPos,
                entityWidth, entityHeight, smoothRotation, alpha, throughWalls);
        if (visibleLenses == 0) return;

        captureScene(main, width, height);
        FramebufferUtil.bindForRendering(main);
        GL11.glViewport(0, 0, width, height);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        applyScissor(width, height);
        bindTexture(0, FramebufferUtil.getColorAttachmentId(snapshot));
        bindTexture(1, ((GlTexture) snapshot.getDepthTexture()).glId());
        shader.bind();
        sceneSampler.uploadInt(0);
        depthSampler.uploadInt(1);
        aspect.uploadFloat(projection.m11() / projection.m00());
        strength.uploadFloat(LENS_STRENGTH * Mth.clamp(alpha, 0.0f, 1.0f));
        respectDepth.uploadInt(throughWalls ? 0 : 1);
        lensCount.uploadInt(visibleLenses);
        for (int i = 0; i < visibleLenses; i++) {
            int offset = i * 4;
            lenses[i].uploadVec4(lensData[offset], lensData[offset + 1],
                    lensData[offset + 2], lensData[offset + 3]);
        }
        fullscreenQuad.draw();
        shader.unbind();
    }

    public void prepare() {
        initShader();
        if (shader.programId == null) {
            shader.bind();
            shader.unbind();
        }
        fullscreenQuad.getOrCreateVao();
        int width = Mc.INSTANCE.getMinecraft().getWindow().getWidth();
        int height = Mc.INSTANCE.getMinecraft().getWindow().getHeight();
        if (width > 0 && height > 0) {
            ensureFramebuffer(width, height);
        }
    }

    public boolean needsPreparation() {
        int width = Mc.INSTANCE.getMinecraft().getWindow().getWidth();
        int height = Mc.INSTANCE.getMinecraft().getWindow().getHeight();
        return shader == null || shader.programId == null || fullscreenQuad.vaoId == null
                || snapshot == null || snapshot.width != width || snapshot.height != height;
    }

    private void initShader() {
        if (shader != null) return;
        ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
        shader = new ShaderProgram(resources.route("shaders/effects/distortion/composite.fsh"),
                resources.route("shaders/shared/framebuffer.vsh"));
        sceneSampler = shader.uniform("SceneSampler");
        depthSampler = shader.uniform("DepthSampler");
        lensCount = shader.uniform("LensCount");
        aspect = shader.uniform("Aspect");
        strength = shader.uniform("Strength");
        respectDepth = shader.uniform("RespectDepth");
        for (int i = 0; i < lenses.length; i++) {
            lenses[i] = shader.uniform("Lens" + i);
        }
    }

    private void ensureFramebuffer(int width, int height) {
        if (snapshot == null) {
            snapshot = new MainTarget(width, height);
            FramebufferUtil.clearTransparent(snapshot);
            FramebufferUtil.setLinearTextureFilter(snapshot);
            return;
        }
        FramebufferUtil.resizeIfNeeded(snapshot, width, height);
    }

    private void captureScene(RenderTarget main, int width, int height) {
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, FramebufferUtil.getFramebufferId(main));
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, FramebufferUtil.getFramebufferId(snapshot));
        GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
    }

    private void projectLenses(Matrix4f viewProjection, Matrix4f projection, Vec3 targetPos,
                               AABB targetBox, Vec3 cameraPos, float entityWidth, float entityHeight,
                               float rotation, float alpha, boolean throughWalls) {
        visibleLenses = 0;
        minU = 1.0f;
        minV = 1.0f;
        maxU = 0.0f;
        maxV = 0.0f;
        float scale = 1.0f - 0.3f * alpha;
        float screenAspect = Math.abs(projection.m11() / projection.m00());
        boolean zeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
        for (int angle = 0; angle < 336 && visibleLenses < MAX_LENSES; angle += 20) {
            float orbit = (float) Math.toRadians(angle + rotation * 2.0f);
            float x = (float) Math.cos(orbit) * entityWidth * scale;
            float z = (float) Math.sin(orbit) * entityWidth * scale;
            float y = crystalYOffset(angle, entityHeight);
            if (!throughWalls && isBehindTarget(targetBox, cameraPos, targetPos.add(x, y, z))) {
                continue;
            }
            Vec3 relative = targetPos.add(x, y, z).subtract(cameraPos);
            Vector4f clip = viewProjection.transform(new Vector4f(
                    (float) relative.x, (float) relative.y, (float) relative.z, 1.0f));
            if (clip.w <= 0.05f) continue;

            float u = clip.x / clip.w * 0.5f + 0.5f;
            float v = clip.y / clip.w * 0.5f + 0.5f;
            float depth = clip.z / clip.w;
            if (!zeroToOne) depth = depth * 0.5f + 0.5f;
            float radius = Math.min(0.45f * projection.m11() / clip.w * 0.5f, 0.35f);
            if (u < -radius * 2.0f || u > 1.0f + radius * 2.0f
                    || v < -radius * 2.0f || v > 1.0f + radius * 2.0f) {
                continue;
            }
            int offset = visibleLenses++ * 4;
            lensData[offset] = u;
            lensData[offset + 1] = v;
            lensData[offset + 2] = depth;
            lensData[offset + 3] = radius;
            float horizontalRadius = radius / screenAspect;
            minU = Math.min(minU, u - horizontalRadius);
            minV = Math.min(minV, v - radius);
            maxU = Math.max(maxU, u + horizontalRadius);
            maxV = Math.max(maxV, v + radius);
        }
    }

    private static boolean isBehindTarget(AABB targetBox, Vec3 cameraPos, Vec3 point) {
        return targetBox.clip(cameraPos, point)
                .filter(hit -> cameraPos.distanceToSqr(hit) + 1.0E-4d
                        < cameraPos.distanceToSqr(point))
                .isPresent();
    }

    private void applyScissor(int width, int height) {
        int x = Math.max(0, (int) Math.floor(minU * width));
        int y = Math.max(0, (int) Math.floor(minV * height));
        int right = Math.min(width, (int) Math.ceil(maxU * width));
        int top = Math.min(height, (int) Math.ceil(maxV * height));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x, y, Math.max(0, right - x), Math.max(0, top - y));
    }

    private static float crystalYOffset(int angle, float entityHeight) {
        float y = 0.1f + entityHeight * Math.abs((float) Math.cos(angle));
        if (angle == 220) y -= entityHeight * 0.18f;
        return y;
    }

    private static void bindTexture(int unit, int texture) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
    }
}
