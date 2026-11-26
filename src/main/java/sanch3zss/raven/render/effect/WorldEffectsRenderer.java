package sanch3zss.raven.render.effect;


import sanch3zss.raven.render.gl.FramebufferUtil;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.resource.ResourceRouter;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.module.render.AmbienceModule;
import sanch3zss.raven.feature.module.render.MotionBlurModule;
import sanch3zss.raven.render.shader.FullscreenQuad;
import sanch3zss.raven.render.shader.ShaderProgram;
import sanch3zss.raven.render.shader.ShaderUniform;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.render.gl.GlStateSnapshot;

public enum WorldEffectsRenderer {
    INSTANCE;

    private final FullscreenQuad quad = new FullscreenQuad();
    private final Matrix4f previousViewProjection = new Matrix4f();
    private ShaderProgram shader;
    private ShaderUniform sceneSampler, depthSampler, inverseViewProjection, previousProjection;
    private ShaderUniform cameraDelta, grade, screenSize;
    private ShaderUniform motionStrength, zeroToOne;
    private ShaderUniform fogNear, fogFar, fogDistance, fogTime;
    private RenderTarget snapshot;
    private Vec3 previousCamera;
    private Object previousWorld;
    private long lastFrame;
    private int previousWidth, previousHeight;
    private float previousYaw, previousPitch, previousProjectionX;

    public void render(WorldRenderEvent event) {
        var raven = Raven.INSTANCE;
        if (raven == null || !Mc.INSTANCE.isWorldLoaded()) {
            previousCamera = null;
            return;
        }
        AmbienceModule ambience = raven.moduleRepository().get(AmbienceModule.class);
        MotionBlurModule blur = raven.moduleRepository().get(MotionBlurModule.class);
        boolean gradeEnabled = ambience.isState() && ambience.colorGrade.isValue();
        boolean blurEnabled = blur.isState();
        boolean fogEnabled = ambience.isState() && ambience.fog.isValue();
        if (!gradeEnabled && !blurEnabled && !fogEnabled) {
            previousCamera = null;
            return;
        }
        RenderTarget main = Mc.INSTANCE.getFramebuffer();
        if (!(main.getDepthTexture() instanceof GlTexture)) return;
        int width = Mc.INSTANCE.getMinecraft().getWindow().getWidth();
        int height = Mc.INSTANCE.getMinecraft().getWindow().getHeight();
        if (width <= 0 || height <= 0) return;
        initShader();
        if (snapshot == null) {
            snapshot = new MainTarget(width, height);
            FramebufferUtil.setLinearTextureFilter(snapshot);
        } else {
            FramebufferUtil.resizeIfNeeded(snapshot, width, height);
        }
        if (!(snapshot.getDepthTexture() instanceof GlTexture snapshotDepth)) return;
        Matrix4f viewProjection = new Matrix4f(event.projectionMatrix()).mul(event.modelViewMatrix());
        Matrix4f inverse = new Matrix4f(viewProjection).invert();
        Vec3 camera = Mc.INSTANCE.getCamera().position();
        float yaw = Mc.INSTANCE.getCamera().yRot();
        float pitch = Mc.INSTANCE.getCamera().xRot();
        long now = System.nanoTime();
        long elapsed = now - lastFrame;
        boolean continuous = previousCamera != null && previousWorld == Mc.INSTANCE.getWorld()
                && elapsed < 250_000_000L && camera.distanceToSqr(previousCamera) < 36.0
                && width == previousWidth && height == previousHeight
                && Math.abs(Mth.wrapDegrees(yaw - previousYaw)) < 60.0f
                && Math.abs(pitch - previousPitch) < 60.0f
                && Math.abs(event.projectionMatrix().m00() - previousProjectionX) < 0.02f;
        Vec3 delta = continuous ? camera.subtract(previousCamera) : Vec3.ZERO;

        GlStateSnapshot state = GlStateSnapshot.create(2);
        int oldFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, FramebufferUtil.getFramebufferId(main));
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, FramebufferUtil.getFramebufferId(snapshot));
            GL30.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height,
                    GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
            FramebufferUtil.bindForRendering(main);
            GL11.glViewport(0, 0, width, height);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, FramebufferUtil.getColorAttachmentId(snapshot));
            GL13.glActiveTexture(GL13.GL_TEXTURE1);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, snapshotDepth.glId());
            shader.bind();
            sceneSampler.uploadInt(0);
            depthSampler.uploadInt(1);
            inverseViewProjection.uploadMatrix4f(inverse);
            previousProjection.uploadMatrix4f(continuous ? previousViewProjection : viewProjection);
            cameraDelta.uploadVec3((float) delta.x, (float) delta.y, (float) delta.z);
            screenSize.uploadVec2(width, height);
            float frameScale = continuous ? Mth.clamp(16_666_667.0f / Math.max(elapsed, 1L), 0.75f, 3.0f) : 1.0f;
            motionStrength.uploadFloat(blurEnabled && continuous
                    ? Math.min(1.0f, blur.strength.currentValue()) * frameScale : 0.0f);
            grade.uploadVec3(gradeEnabled ? ambience.saturation.currentValue() : 1.0f,
                    gradeEnabled ? ambience.contrast.currentValue() : 1.0f,
                    gradeEnabled ? ambience.warmth.currentValue() : 0.0f);
            int fogStart = ambience.fogColor.getFirstColor();
            int fogEnd = ambience.fogColor.getGradientEndColor();
            fogNear.uploadVec4(ColorUtil.red(fogStart) / 255.0f, ColorUtil.green(fogStart) / 255.0f,
                    ColorUtil.blue(fogStart) / 255.0f, ColorUtil.alpha(fogStart) / 255.0f);
            fogFar.uploadVec4(ColorUtil.red(fogEnd) / 255.0f, ColorUtil.green(fogEnd) / 255.0f,
                    ColorUtil.blue(fogEnd) / 255.0f, ColorUtil.alpha(fogEnd) / 255.0f);
            fogDistance.uploadFloat(fogEnabled ? ambience.fogDistance.currentValue() : 0.0f);
            fogTime.uploadFloat((now / 1_000_000L % 3_600_000L) / 1000.0f);
            zeroToOne.uploadInt(RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1 : 0);
            quad.draw();
            shader.unbind();
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, oldFramebuffer);
            GL11.glDepthMask(depthWrite);
            state.revert();
        }
        previousViewProjection.set(viewProjection);
        previousCamera = camera;
        previousWorld = Mc.INSTANCE.getWorld();
        lastFrame = now;
        previousWidth = width;
        previousHeight = height;
        previousYaw = yaw;
        previousPitch = pitch;
        previousProjectionX = event.projectionMatrix().m00();
    }

    private void initShader() {
        if (shader != null) return;
        ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
        shader = new ShaderProgram(resources.route("shaders/effects/world_post/composite.fsh"),
                resources.route("shaders/shared/framebuffer.vsh"));
        sceneSampler = shader.uniform("SceneSampler");
        depthSampler = shader.uniform("DepthSampler");
        inverseViewProjection = shader.uniform("InverseViewProjection");
        previousProjection = shader.uniform("PreviousViewProjection");
        cameraDelta = shader.uniform("CameraDelta");
        grade = shader.uniform("Grade");
        screenSize = shader.uniform("ScreenSize");
        motionStrength = shader.uniform("MotionStrength");
        zeroToOne = shader.uniform("ZeroToOne");
        fogNear = shader.uniform("FogStartColor");
        fogFar = shader.uniform("FogEndColor");
        fogDistance = shader.uniform("FogDistance");
        fogTime = shader.uniform("Time");
    }
}
