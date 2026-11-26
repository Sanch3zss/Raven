package sanch3zss.raven.render.shader;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import java.nio.FloatBuffer;
import java.util.HashMap;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.resource.*;
import sanch3zss.raven.render.gl.FramebufferUtil;

public class BlurEffect {
    public final Mc mc = Mc.INSTANCE;
    public final FullscreenQuad fullscreenQuad = new FullscreenQuad();

    public final HashMap<Integer, float[]> weightCache = new HashMap<>();
    public final HashMap<Integer, FloatBuffer> kernelBufferCache = new HashMap<>();
    public final HashMap<Integer, FloatBuffer> offsetBufferCache = new HashMap<>();
    public static final int downscaleFactor = 2;
    public ShaderUniform directionUniform;
    public ShaderUniform pairCountUniform;
    public ShaderUniform kernelUniform;
    public ShaderUniform offsetsUniform;
    public ShaderUniform textureUniform;
    public ShaderUniform brightnessUniform;

    public ShaderProgram blurShader;
    public RenderTarget horizontalFramebuffer;
    public RenderTarget blurFramebuffer;

    public void init() {
        ResourceRouter resourceRouter = new ResourceRouter("/", ClasspathResource::new);
        this.blurShader = new ShaderProgram(resourceRouter.route("shaders/effects/blur/gaussian.fsh"), resourceRouter.route("shaders/shared/framebuffer.vsh"));
        this.directionUniform = this.blurShader.uniform("uDirection");
        this.textureUniform = this.blurShader.uniform("uTexture");
        this.pairCountUniform = this.blurShader.uniform("uPairCount");
        this.brightnessUniform = this.blurShader.uniform("uBrightness");
        this.kernelUniform = this.blurShader.uniform("uKernel");
        this.offsetsUniform = this.blurShader.uniform("uOffsets");
        Window window = Minecraft.getInstance().getWindow();
        int framebufferWidth = window.getWidth() / 2;
        int framebufferHeight = window.getHeight() / 2;
        this.horizontalFramebuffer = FramebufferUtil.ensureFramebuffer(this.horizontalFramebuffer, framebufferWidth, framebufferHeight, () -> new MainTarget(framebufferWidth, framebufferHeight));
        this.blurFramebuffer = FramebufferUtil.ensureFramebuffer(this.blurFramebuffer, framebufferWidth, framebufferHeight, () -> new MainTarget(framebufferWidth, framebufferHeight));
    }

    public void apply(int i) {
        if (this.blurShader == null) {
            return;
        }
        int iMax = Math.max(1, Math.min(i, 60));
        Window window = this.mc.getWindow();
        int framebufferWidth = window.getWidth() / 2;
        int framebufferHeight = window.getHeight() / 2;
        this.horizontalFramebuffer = FramebufferUtil.ensureFramebuffer(this.horizontalFramebuffer, framebufferWidth, framebufferHeight, () -> new MainTarget(framebufferWidth, framebufferHeight));
        this.blurFramebuffer = FramebufferUtil.ensureFramebuffer(this.blurFramebuffer, framebufferWidth, framebufferHeight, () -> new MainTarget(framebufferWidth, framebufferHeight));
        FramebufferUtil.resizeIfNeeded(this.horizontalFramebuffer, framebufferWidth, framebufferHeight);
        FramebufferUtil.resizeIfNeeded(this.blurFramebuffer, framebufferWidth, framebufferHeight);
        this.blurShader.bind();
        this.textureUniform.uploadInt(0);
        this.pairCountUniform.uploadInt(iMax / 2);
        this.kernelUniform.uploadFloatBuffer(getKernelBuffer(iMax, 3.0f));
        this.offsetsUniform.uploadFloatBuffer(getOffsetBuffer(iMax, 3.0f));
        this.brightnessUniform.uploadFloat(0.0f);
        renderPass(this.mc.getFramebuffer(), this.horizontalFramebuffer, 1.0f / framebufferWidth, 0.0f);
        renderPass(this.horizontalFramebuffer, this.blurFramebuffer, 0.0f, 1.0f / framebufferHeight);
        this.blurShader.unbind();
    }

    public void apply(int i, float f) {
        if (this.blurShader == null) {
            return;
        }
        int iMax = Math.max(1, Math.min(i, 60));
        Window window = this.mc.getWindow();
        int framebufferWidth = window.getWidth() / 2;
        int framebufferHeight = window.getHeight() / 2;
        this.horizontalFramebuffer = FramebufferUtil.ensureFramebuffer(this.horizontalFramebuffer, framebufferWidth, framebufferHeight, () -> new MainTarget(framebufferWidth, framebufferHeight));
        this.blurFramebuffer = FramebufferUtil.ensureFramebuffer(this.blurFramebuffer, framebufferWidth, framebufferHeight, () -> new MainTarget(framebufferWidth, framebufferHeight));
        FramebufferUtil.resizeIfNeeded(this.horizontalFramebuffer, framebufferWidth, framebufferHeight);
        FramebufferUtil.resizeIfNeeded(this.blurFramebuffer, framebufferWidth, framebufferHeight);
        this.blurShader.bind();
        this.textureUniform.uploadInt(0);
        this.pairCountUniform.uploadInt(iMax / 2);
        this.kernelUniform.uploadFloatBuffer(getKernelBuffer(iMax, f));
        this.offsetsUniform.uploadFloatBuffer(getOffsetBuffer(iMax, f));
        this.brightnessUniform.uploadFloat(0.0f);
        renderPass(this.mc.getFramebuffer(), this.horizontalFramebuffer, 1.0f / framebufferWidth, 0.0f);
        renderPass(this.horizontalFramebuffer, this.blurFramebuffer, 0.0f, 1.0f / framebufferHeight);
        this.blurShader.unbind();
    }

    public void apply(RenderTarget framebuffer, int i) {
        if (this.blurShader == null) {
            return;
        }
        int iMax = Math.max(1, Math.min(i, 60));
        int i2 = framebuffer.width;
        int i3 = framebuffer.height;
        this.horizontalFramebuffer = FramebufferUtil.ensureFramebuffer(this.horizontalFramebuffer, i2, i3, () -> new MainTarget(i2, i3));
        this.blurFramebuffer = FramebufferUtil.ensureFramebuffer(this.blurFramebuffer, i2, i3, () -> new MainTarget(i2, i3));
        int i4 = iMax / 2;
        float f = framebuffer.width;
        float f2 = framebuffer.height;
        this.blurShader.bind();
        this.textureUniform.uploadInt(0);
        this.pairCountUniform.uploadInt(i4);
        this.kernelUniform.uploadFloatBuffer(getKernelBuffer(iMax, 3.0f));
        this.offsetsUniform.uploadFloatBuffer(getOffsetBuffer(iMax, 3.0f));
        this.brightnessUniform.uploadFloat(1.0f);
        renderPass(framebuffer, this.horizontalFramebuffer, (1.0f / f) * 2.0f, 0.0f);
        renderPass(this.horizontalFramebuffer, this.blurFramebuffer, 0.0f, (1.0f / f2) * 2.0f);
        this.blurShader.unbind();
    }

    private void renderPass(RenderTarget source, RenderTarget target, float directionX, float directionY) {
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        try {
            FramebufferUtil.bindForRendering(target);
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, FramebufferUtil.getColorAttachmentId(source));
            this.directionUniform.uploadVec2(directionX, directionY);
            this.fullscreenQuad.draw();
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    }

    public RenderTarget getBlurFramebuffer() {
        return this.blurFramebuffer;
    }

    public float[] computeGaussianWeights(int i, float f) {
        float[] fArr = this.weightCache.get(i);
        if (fArr != null) {
            return fArr;
        }
        float[] fArr2 = new float[i];
        float f2 = i / f;
        if (f2 <= 0.0f) {
            f2 = 1.0f;
        }
        float f3 = 0.0f;
        int i2 = 0;
        while (i2 < i) {
            float f4 = i2 / f2;
            fArr2[i2] = (float) (Math.exp(((-0.5f) * f4) * f4) / (Math.abs(f2) * 2.5066283f));
            f3 += i2 == 0 ? fArr2[i2] : fArr2[i2] * 2.0f;
            i2++;
        }
        for (int i3 = 0; i3 < i; i3++) {
            fArr2[i3] = fArr2[i3] / f3;
        }
        this.weightCache.put(i, fArr2);
        return fArr2;
    }

    public FloatBuffer getKernelBuffer(int i, float f) {
        FloatBuffer floatBufferCreateFloatBuffer = this.kernelBufferCache.get(i);
        if (floatBufferCreateFloatBuffer == null) {
            float[] weights = computeGaussianWeights(i, f);
            float[] fArr = new float[(i / 2) + 1];
            fArr[0] = weights[0];
            int i2 = 1;
            for (int i3 = 1; i3 < i; i3 += 2) {
                int i4 = i2;
                i2++;
                fArr[i4] = weights[i3] + (i3 + 1 < i ? weights[i3 + 1] : 0.0f);
            }
            floatBufferCreateFloatBuffer = BufferUtils.createFloatBuffer(fArr.length);
            floatBufferCreateFloatBuffer.put(fArr);
            floatBufferCreateFloatBuffer.flip();
            this.kernelBufferCache.put(i, floatBufferCreateFloatBuffer);
        } else {
            floatBufferCreateFloatBuffer.rewind();
        }
        return floatBufferCreateFloatBuffer;
    }

    public FloatBuffer getOffsetBuffer(int i, float f) {
        FloatBuffer floatBufferCreateFloatBuffer = this.offsetBufferCache.get(i);
        if (floatBufferCreateFloatBuffer == null) {
            float[] weights = computeGaussianWeights(i, f);
            int i2 = i / 2;
            float[] fArr = i2 == 0 ? new float[]{0.0f} : new float[i2];
            int i3 = 0;
            for (int i4 = 1; i4 < i; i4 += 2) {
                float f2 = weights[i4];
                float f3 = i4 + 1 < i ? weights[i4 + 1] : 0.0f;
                float f4 = f2 + f3;
                int i5 = i3;
                i3++;
                fArr[i5] = f4 == 0.0f ? i4 : ((i4 * f2) + ((i4 + 1) * f3)) / f4;
            }
            floatBufferCreateFloatBuffer = BufferUtils.createFloatBuffer(fArr.length);
            floatBufferCreateFloatBuffer.put(fArr);
            floatBufferCreateFloatBuffer.flip();
            this.offsetBufferCache.put(i, floatBufferCreateFloatBuffer);
        } else {
            floatBufferCreateFloatBuffer.rewind();
        }
        return floatBufferCreateFloatBuffer;
    }
}
