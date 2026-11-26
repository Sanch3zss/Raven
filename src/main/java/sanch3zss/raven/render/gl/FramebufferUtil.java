package sanch3zss.raven.render.gl;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.lwjgl.opengl.*;
import sanch3zss.raven.mixin.accessor.GpuDeviceAccessor;

public final class FramebufferUtil {
    public static RenderTarget ensureFramebuffer(RenderTarget framebuffer, int i, int i2, Supplier<RenderTarget> supplier) {
        if (!hasValidColorAttachment(framebuffer)) {
            framebuffer = supplier.get();
            clearTransparent(framebuffer);
        }
        setLinearTextureFilter(framebuffer);
        return framebuffer;
    }

    public static boolean hasValidColorAttachment(RenderTarget framebuffer) {
        int colorId = getColorAttachmentId(framebuffer);
        return colorId > 0 && GL11.glIsTexture(colorId);
    }

    public static void resizeIfNeeded(RenderTarget framebuffer, int i, int i2) {
        if (framebuffer.width == i && framebuffer.height == i2) {
            return;
        }
        framebuffer.resize(i, i2);
        clearTransparent(framebuffer);
        setLinearTextureFilter(framebuffer);
    }

    public static void clearTransparent(RenderTarget framebuffer) {
        renderTo(framebuffer, () -> {
            GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        });
    }

    public static void setLinearTextureFilter(RenderTarget framebuffer) {
        int colorId = getColorAttachmentId(framebuffer);
        if (colorId <= 0) {
            return;
        }
        int iGlGetInteger = GL11.glGetInteger(34016);
        int iGlGetInteger2 = GL11.glGetInteger(32873);
        GL11.glBindTexture(3553, colorId);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glTexParameteri(3553, 10242, 33071);
        GL11.glTexParameteri(3553, 10243, 33071);
        GL11.glBindTexture(3553, iGlGetInteger2);
        GL13.glActiveTexture(iGlGetInteger);
    }

    public static int getColorAttachmentId(RenderTarget framebuffer) {
        if (framebuffer == null || framebuffer.getColorTexture() == null) {
            return 0;
        }
        return ((GlTexture) framebuffer.getColorTexture()).glId();
    }

    public static int getTextureId(AbstractTexture texture) {
        if (texture == null || texture.getTexture() == null) {
            return 0;
        }
        return ((GlTexture) texture.getTexture()).glId();
    }

    public static void renderTo(RenderTarget framebuffer, Runnable renderer) {
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        bindForRendering(framebuffer);
        try {
            renderer.run();
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    }

    public static int getFramebufferId(RenderTarget framebuffer) {
        if (framebuffer == null || framebuffer.getColorTexture() == null) {
            return 0;
        }
        Object backend = ((GpuDeviceAccessor) (Object) RenderSystem.getDevice()).raven$getBackend();
        if (!(backend instanceof GlDevice device)
                || !(framebuffer.getColorTexture() instanceof GlTexture colorTexture)) {
            throw new IllegalStateException("Raven's OpenGL renderer requires the Blaze3D OpenGL backend");
        }
        GlTexture depthTexture = framebuffer.getDepthTexture() instanceof GlTexture texture ? texture : null;
        return device.frameBufferCache().getFbo(
                device.directStateAccess(), Collections.singletonList(colorTexture), depthTexture);
    }

    public static void bindForRendering(RenderTarget framebuffer) {
        if (framebuffer == null) {
            return;
        }
        int framebufferId = getFramebufferId(framebuffer);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebufferId);
        GL11.glViewport(0, 0, framebuffer.width, framebuffer.height);
    }

    public FramebufferUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
