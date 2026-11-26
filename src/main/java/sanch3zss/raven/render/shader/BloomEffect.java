package sanch3zss.raven.render.shader;

import com.mojang.blaze3d.pipeline.RenderTarget;

public class BloomEffect {
    public static final int downscaleFactor = 2;
    public final BlurEffect blurEffect = new BlurEffect();

    public RenderTarget bloomFramebuffer;

    public void init() {
        this.blurEffect.init();
    }

    public void apply(RenderTarget framebuffer, int i) {
        this.blurEffect.apply(framebuffer, i);
        this.bloomFramebuffer = this.blurEffect.getBlurFramebuffer();
    }

    public RenderTarget getBloomFramebuffer() {
        return this.bloomFramebuffer;
    }
}
