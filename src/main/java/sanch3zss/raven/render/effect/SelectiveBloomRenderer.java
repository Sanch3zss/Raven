package sanch3zss.raven.render.effect;


import sanch3zss.raven.render.gl.FramebufferUtil;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.resource.ResourceRouter;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.render.gl.GlStateSnapshot;
import sanch3zss.raven.render.shader.BlurEffect;
import sanch3zss.raven.render.shader.FullscreenQuad;
import sanch3zss.raven.render.shader.ShaderProgram;
import sanch3zss.raven.render.shader.ShaderUniform;

public enum SelectiveBloomRenderer {
    INSTANCE;

    private static final int MAX_SOURCES = 32;
    private final List<Source> sources = new ArrayList<>();
    private final FullscreenQuad quad = new FullscreenQuad();
    private final BlurEffect blur = new BlurEffect();
    private ShaderProgram maskShader, compositeShader;
    private ShaderUniform depthSampler, sceneSampler, sourceCount, aspect, sourceData, colors, glowSampler;
    private RenderTarget mask;

    public void add(Vec3 position, float radius, int color) {
        if (position != null && radius > 0.0f && sources.size() < MAX_SOURCES) {
            sources.add(new Source(position, radius, color));
        }
    }

    public void render(WorldRenderEvent event) {
        try {
            if (sources.isEmpty() || !Mc.INSTANCE.isWorldLoaded()) return;
            RenderTarget main = Mc.INSTANCE.getFramebuffer();
            if (!(main.getDepthTexture() instanceof GlTexture depth)) return;
            int width = Mc.INSTANCE.getMinecraft().getWindow().getWidth();
            int height = Mc.INSTANCE.getMinecraft().getWindow().getHeight();
            if (width <= 0 || height <= 0) return;
            init();
            int maskWidth = Math.max(1, width / 2);
            int maskHeight = Math.max(1, height / 2);
            if (mask == null) {
                mask = new MainTarget(maskWidth, maskHeight);
                FramebufferUtil.setLinearTextureFilter(mask);
            } else {
                FramebufferUtil.resizeIfNeeded(mask, maskWidth, maskHeight);
            }
            Matrix4f projection = event.projectionMatrix();
            Matrix4f viewProjection = new Matrix4f(projection).mul(event.modelViewMatrix());
            Vec3 camera = Mc.INSTANCE.getCamera().position();
            float[] positions = new float[MAX_SOURCES * 4];
            float[] tints = new float[MAX_SOURCES * 3];
            int count = 0;
            boolean zeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();
            for (Source source : sources) {
                Vec3 relative = source.position.subtract(camera);
                Vector4f clip = viewProjection.transform(new Vector4f(
                        (float) relative.x, (float) relative.y, (float) relative.z, 1.0f));
                if (clip.w <= 0.05f) continue;
                float u = clip.x / clip.w * 0.5f + 0.5f;
                float v = clip.y / clip.w * 0.5f + 0.5f;
                float radius = Math.min(0.2f, source.radius * projection.m11() / clip.w);
                if (radius < 0.002f || u < -radius || u > 1.0f + radius
                        || v < -radius || v > 1.0f + radius) continue;
                float z = clip.z / clip.w;
                positions[count * 4] = u;
                positions[count * 4 + 1] = v;
                positions[count * 4 + 2] = zeroToOne ? z : z * 0.5f + 0.5f;
                positions[count * 4 + 3] = radius;
                float opacity = ((source.color >>> 24) & 255) / 255.0f;
                tints[count * 3] = ((source.color >> 16) & 255) / 255.0f * opacity;
                tints[count * 3 + 1] = ((source.color >> 8) & 255) / 255.0f * opacity;
                tints[count * 3 + 2] = (source.color & 255) / 255.0f * opacity;
                count++;
            }
            if (count == 0) return;
            GlStateSnapshot state = GlStateSnapshot.create(2);
            boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
            int oldFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
            try {
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                GL11.glDepthMask(false);
                FramebufferUtil.bindForRendering(mask);
                GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, depth.glId());
                GL13.glActiveTexture(GL13.GL_TEXTURE1);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, FramebufferUtil.getColorAttachmentId(main));
                maskShader.bind();
                depthSampler.uploadInt(0);
                sceneSampler.uploadInt(1);
                sourceCount.uploadInt(count);
                aspect.uploadFloat((float) width / height);
                GL33.glUniform4fv(sourceData.location, direct(positions));
                GL33.glUniform3fv(colors.location, direct(tints));
                quad.draw();
                maskShader.unbind();
                blur.apply(mask, 10);
                FramebufferUtil.bindForRendering(main);
                GL11.glViewport(0, 0, width, height);
                GL11.glEnable(GL11.GL_BLEND);
                GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D,
                        FramebufferUtil.getColorAttachmentId(blur.getBlurFramebuffer()));
                compositeShader.bind();
                glowSampler.uploadInt(0);
                quad.draw();
                compositeShader.unbind();
            } finally {
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, oldFramebuffer);
                GL11.glDepthMask(depthWrite);
                state.revert();
            }
        } finally {
            sources.clear();
        }
    }

    private static FloatBuffer direct(float[] values) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
        buffer.put(values).flip();
        return buffer;
    }

    private void init() {
        if (maskShader != null) return;
        ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
        maskShader = new ShaderProgram(resources.route("shaders/effects/selective_bloom/mask.fsh"),
                resources.route("shaders/shared/framebuffer.vsh"));
        compositeShader = new ShaderProgram(resources.route("shaders/effects/selective_bloom/composite.fsh"),
                resources.route("shaders/shared/framebuffer.vsh"));
        depthSampler = maskShader.uniform("DepthSampler");
        sceneSampler = maskShader.uniform("SceneSampler");
        sourceCount = maskShader.uniform("SourceCount");
        aspect = maskShader.uniform("Aspect");
        sourceData = maskShader.uniform("Sources[0]");
        colors = maskShader.uniform("Colors[0]");
        glowSampler = compositeShader.uniform("GlowSampler");
        blur.init();
    }

    private record Source(Vec3 position, float radius, int color) { }
}
