package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;

final class PaintedTexture extends AbstractTexture {
    PaintedTexture(String label, NativeImage image) {
        texture = KnifeTexture.createMipmapped(label, image);
        textureView = RenderSystem.getDevice().createTextureView(texture);
        sampler = KnifeTexture.mipmapSampler();
    }
}
