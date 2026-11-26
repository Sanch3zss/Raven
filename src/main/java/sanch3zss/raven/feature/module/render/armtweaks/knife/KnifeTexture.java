package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.GpuFormat;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.Identifier;

final class KnifeTexture extends SimpleTexture {
    private static final int MAX_MIP_LEVELS = 5;

    KnifeTexture(Identifier location) {
        super(location);
    }

    @Override
    protected void doLoad(NativeImage image) {
        close();
        texture = createMipmapped(resourceId().toString(), image);
        textureView = RenderSystem.getDevice().createTextureView(texture);
        sampler = mipmapSampler();
    }

    static GpuTexture createMipmapped(String label, NativeImage image) {
        GpuDevice device = RenderSystem.getDevice();
        int levels = 1;
        while (levels < MAX_MIP_LEVELS && (image.getWidth() >> levels) >= 16 && (image.getHeight() >> levels) >= 16) {
            levels++;
        }
        GpuTexture created = device.createTexture(() -> label, 5, GpuFormat.RGBA8_UNORM,
                image.getWidth(), image.getHeight(), 1, levels);
        CommandEncoder encoder = device.createCommandEncoder();
        encoder.writeToTexture(created, image, 0, 0, 0, 0);
        NativeImage previous = image;
        for (int level = 1; level < levels; level++) {
            NativeImage next = downsample(previous);
            encoder.writeToTexture(created, next, level, 0, 0, 0);
            if (previous != image) previous.close();
            previous = next;
        }
        if (previous != image) previous.close();
        return created;
    }

    static GpuSampler mipmapSampler() {
        return RenderSystem.getSamplerCache().getSampler(AddressMode.REPEAT, AddressMode.REPEAT,
                FilterMode.LINEAR, FilterMode.LINEAR, true);
    }

    private static NativeImage downsample(NativeImage source) {
        int width = Math.max(1, source.getWidth() / 2);
        int height = Math.max(1, source.getHeight() / 2);
        NativeImage result = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                long red = 0;
                long green = 0;
                long blue = 0;
                int alpha = 0;
                int opaque = 0;
                for (int dy = 0; dy < 2; dy++) {
                    for (int dx = 0; dx < 2; dx++) {
                        int color = source.getPixel(Math.min(source.getWidth() - 1, x * 2 + dx),
                                Math.min(source.getHeight() - 1, y * 2 + dy));
                        int a = color >>> 24;
                        alpha = Math.max(alpha, a);
                        if (a < 26) continue;
                        red += (color >> 16 & 255) * a;
                        green += (color >> 8 & 255) * a;
                        blue += (color & 255) * a;
                        opaque += a;
                    }
                }
                if (opaque == 0) {
                    result.setPixel(x, y, 0);
                    continue;
                }
                int r = (int) (red / opaque);
                int g = (int) (green / opaque);
                int b = (int) (blue / opaque);
                result.setPixel(x, y, alpha << 24 | r << 16 | g << 8 | b);
            }
        }
        return result;
    }
}
