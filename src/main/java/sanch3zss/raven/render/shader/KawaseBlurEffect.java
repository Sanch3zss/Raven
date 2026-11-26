package sanch3zss.raven.render.shader;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.*;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.resource.*;
import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.render.gl.GlStateSnapshot;

public final class KawaseBlurEffect {
    private static final int PASS_COUNT = 4;
    private static final float OFFSET = 1.5f;

    private final FullscreenQuad fullscreenQuad = new FullscreenQuad();
    private final RenderTarget[] framebuffers = new RenderTarget[PASS_COUNT];
    private ShaderProgram downsampleShader;
    private ShaderProgram upsampleShader;
    private ShaderUniform downsampleTexture;
    private ShaderUniform downsampleHalfTexelSize;
    private ShaderUniform downsampleOffset;
    private ShaderUniform upsampleTexture;
    private ShaderUniform upsampleHalfTexelSize;
    private ShaderUniform upsampleOffset;
    private int sampler;

    public void init() {
        ResourceRouter resources = new ResourceRouter("/", ClasspathResource::new);
        this.downsampleShader = new ShaderProgram(resources.route("shaders/effects/blur/kawase_downsample.fsh"),
                resources.route("shaders/shared/framebuffer.vsh"));
        this.upsampleShader = new ShaderProgram(resources.route("shaders/effects/blur/kawase_upsample.fsh"),
                resources.route("shaders/shared/framebuffer.vsh"));
        this.downsampleTexture = this.downsampleShader.uniform("uTexture");
        this.downsampleHalfTexelSize = this.downsampleShader.uniform("uHalfTexelSize");
        this.downsampleOffset = this.downsampleShader.uniform("uOffset");
        this.upsampleTexture = this.upsampleShader.uniform("uTexture");
        this.upsampleHalfTexelSize = this.upsampleShader.uniform("uHalfTexelSize");
        this.upsampleOffset = this.upsampleShader.uniform("uOffset");
        ensureFramebuffers();
    }

    public void apply() {
        apply(Mc.INSTANCE.getFramebuffer());
    }

    public RenderTarget apply(RenderTarget source) {
        if (this.downsampleShader == null || this.upsampleShader == null || source == null) {
            return null;
        }
        ensureFramebuffers(source.width, source.height);
        renderPass(this.downsampleShader, this.downsampleTexture, this.downsampleHalfTexelSize,
                this.downsampleOffset, source, this.framebuffers[0], 0.0f);
        for (int pass = 1; pass < PASS_COUNT; pass++) {
            renderPass(this.downsampleShader, this.downsampleTexture, this.downsampleHalfTexelSize,
                    this.downsampleOffset, this.framebuffers[pass - 1], this.framebuffers[pass],
                    OFFSET);
        }
        for (int pass = PASS_COUNT - 1; pass > 0; pass--) {
            renderPass(this.upsampleShader, this.upsampleTexture, this.upsampleHalfTexelSize,
                    this.upsampleOffset, this.framebuffers[pass], this.framebuffers[pass - 1],
                    OFFSET);
        }
        return this.framebuffers[0];
    }

    private void ensureFramebuffers() {
        Window window = Mc.INSTANCE.getWindow();
        ensureFramebuffers(window.getWidth(), window.getHeight());
    }

    private void ensureFramebuffers(int width, int height) {
        for (int i = 0; i < PASS_COUNT; i++) {
            int divisor = 1 << i;
            int framebufferWidth = Math.max(1, width / divisor);
            int framebufferHeight = Math.max(1, height / divisor);
            this.framebuffers[i] = FramebufferUtil.ensureFramebuffer(this.framebuffers[i], framebufferWidth,
                    framebufferHeight, () -> new MainTarget(framebufferWidth, framebufferHeight));
            FramebufferUtil.resizeIfNeeded(this.framebuffers[i], framebufferWidth, framebufferHeight);
        }
    }

    private void renderPass(ShaderProgram shader, ShaderUniform texture, ShaderUniform halfTexelSize,
                            ShaderUniform offset, RenderTarget source, RenderTarget target, float passOffset) {
        GlStateSnapshot state = GlStateSnapshot.create(1);
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            FramebufferUtil.bindForRendering(target);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDepthMask(false);
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, FramebufferUtil.getColorAttachmentId(source));
            GL33.glBindSampler(0, linearSampler());
            shader.bind();
            texture.uploadInt(0);
            halfTexelSize.uploadVec2(0.5f / source.width, 0.5f / source.height);
            offset.uploadFloat(passOffset);
            this.fullscreenQuad.draw();
            shader.unbind();
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glDepthMask(depthWrite);
            state.revert();
        }
    }

    private int linearSampler() {
        if (this.sampler == 0) {
            this.sampler = GL33.glGenSamplers();
            GL33.glSamplerParameteri(this.sampler, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL33.glSamplerParameteri(this.sampler, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL33.glSamplerParameteri(this.sampler, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL33.glSamplerParameteri(this.sampler, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }
        return this.sampler;
    }

    public RenderTarget getBlurFramebuffer() {
        return this.framebuffers[0];
    }
}
