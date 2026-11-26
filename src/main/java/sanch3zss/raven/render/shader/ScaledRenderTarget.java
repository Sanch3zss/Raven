package sanch3zss.raven.render.shader;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.util.*;
import net.minecraft.client.Minecraft;

import sanch3zss.raven.render.gl.FramebufferUtil;

public final class ScaledRenderTarget {
    public final int scaleFactor;
    public RenderTarget framebuffer;
    public boolean initialized;
    public final Minecraft client = Minecraft.getInstance();
    public final List<Runnable> renderQueue = new ArrayList();

    public ScaledRenderTarget(int i) {
        this.scaleFactor = Math.max(1, i);
    }

    public void init() {
        if (this.initialized) {
            return;
        }
        ensureFramebuffer();
        this.initialized = true;
    }

    public void add(Runnable runnable) {
        ensureInitialized();
        if (runnable != null) {
            this.renderQueue.add(runnable);
        }
    }

    public void addAll(Runnable... runnableArr) {
        ensureInitialized();
        if (runnableArr == null) {
            return;
        }
        for (Runnable runnable : runnableArr) {
            if (runnable != null) {
                this.renderQueue.add(runnable);
            }
        }
    }

    public void clearQueue() {
        this.renderQueue.clear();
    }

    public void renderToFramebuffer() {
        ensureInitialized();
        ensureFramebuffer();
        FramebufferUtil.renderTo(this.framebuffer, () -> {
            for (Runnable runnable : this.renderQueue) {
                runnable.run();
            }
        });
        clearQueue();
    }

    public RenderTarget getFramebuffer() {
        ensureInitialized();
        ensureFramebuffer();
        return this.framebuffer;
    }

    public void ensureFramebuffer() {
        int iMax = Math.max(1, this.client.getWindow().getWidth() / this.scaleFactor);
        int iMax2 = Math.max(1, this.client.getWindow().getHeight() / this.scaleFactor);
        this.framebuffer = FramebufferUtil.ensureFramebuffer(this.framebuffer, iMax, iMax2, () -> new MainTarget(iMax, iMax2));
        FramebufferUtil.resizeIfNeeded(this.framebuffer, this.client.getWindow().getWidth(), this.client.getWindow().getHeight());
    }

    public void ensureInitialized() {
        if (this.initialized) {
            return;
        }
        init();
    }
}
