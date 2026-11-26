package sanch3zss.raven.client.font;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

final class CustomFontAtlas implements AutoCloseable {
    private static final int SIZE = 256;
    private static final int SPACING = 2;
    private static final int EMPTY_PIXEL = 0x00FFFFFF;

    private final String name;
    private final int detail;
    private final int mipLevels;
    private final List<Page> pages = new ArrayList<>();

    CustomFontAtlas(String name, int detail) {
        this.name = name;
        this.detail = detail;
        this.mipLevels = Integer.numberOfTrailingZeros(detail) + 2;
    }

    @Nullable SmoothGlyph add(GlyphInfo info, GlyphRaster raster, float oversample) {
        if (raster.width() + SPACING > SIZE || raster.height() + SPACING > SIZE) {
            return null;
        }
        for (Page page : this.pages) {
            SmoothGlyph glyph = page.add(info, raster, oversample);
            if (glyph != null) {
                return glyph;
            }
        }
        Page page = new Page(this.pages.size());
        this.pages.add(page);
        return page.add(info, raster, oversample);
    }

    @Override
    public void close() {
        for (Page page : this.pages) {
            page.close();
        }
        this.pages.clear();
    }

    record GlyphRaster(int width, int height, int left, int top, byte[] coverage, byte[] detailCoverage) {
    }

    private final class Page {
        private final AtlasTexture texture;
        private final AtlasTexture detailTexture;
        private final Identifier detailId;
        private final GlyphRenderTypes renderTypes;
        private final byte[] detailCoverage;
        private int shelfX;
        private int shelfY;
        private int shelfHeight;

        private Page(int index) {
            String path = CustomFontAtlas.this.name + "/" + index;
            int detailSize = SIZE * CustomFontAtlas.this.detail;
            this.texture = new AtlasTexture(path, SIZE, 1,
                    RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            this.detailTexture = new AtlasTexture(path + "/detail", detailSize, CustomFontAtlas.this.mipLevels,
                    RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                            FilterMode.LINEAR, FilterMode.LINEAR, true));
            this.detailId = Identifier.fromNamespaceAndPath("raven", path + "/detail");
            Minecraft.getInstance().getTextureManager().register(this.detailId, this.detailTexture);
            this.renderTypes = GlyphRenderTypes.createForColorTexture(this.detailId);
            this.detailCoverage = new byte[detailSize * detailSize];
        }

        private @Nullable SmoothGlyph add(GlyphInfo info, GlyphRaster raster, float oversample) {
            if (this.shelfX + raster.width() > SIZE) {
                this.shelfX = 0;
                this.shelfY += this.shelfHeight + SPACING;
                this.shelfHeight = 0;
            }
            if (this.shelfY + raster.height() > SIZE) {
                return null;
            }
            int x = this.shelfX;
            int y = this.shelfY;
            this.shelfX += raster.width() + SPACING;
            this.shelfHeight = Math.max(this.shelfHeight, raster.height());
            upload(raster, x, y);
            float left = raster.left() / oversample;
            float up = GlyphProvider.BASELINE - raster.top() / oversample;
            return new SmoothGlyph(info, this.renderTypes, this.texture.getTextureView(),
                    (float) x / SIZE, (float) (x + raster.width()) / SIZE,
                    (float) y / SIZE, (float) (y + raster.height()) / SIZE,
                    left, left + raster.width() / oversample, up, up + raster.height() / oversample);
        }

        private void upload(GlyphRaster raster, int x, int y) {
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            try (NativeImage image = new NativeImage(NativeImage.Format.RGBA, raster.width(), raster.height(), false)) {
                for (int row = 0; row < raster.height(); row++) {
                    for (int column = 0; column < raster.width(); column++) {
                        image.setPixelABGR(column, row, pixel(raster.coverage()[row * raster.width() + column] & 0xFF));
                    }
                }
                encoder.writeToTexture(this.texture.getTexture(), image, 0, 0, x, y);
            }
            int detail = CustomFontAtlas.this.detail;
            int detailSize = SIZE * detail;
            int detailWidth = raster.width() * detail;
            int detailHeight = raster.height() * detail;
            int detailX = x * detail;
            int detailY = y * detail;
            for (int row = 0; row < detailHeight; row++) {
                System.arraycopy(raster.detailCoverage(), row * detailWidth, this.detailCoverage,
                        (detailY + row) * detailSize + detailX, detailWidth);
            }
            for (int level = 0; level < CustomFontAtlas.this.mipLevels; level++) {
                int step = 1 << level;
                int x0 = detailX / step;
                int y0 = detailY / step;
                int x1 = Math.min(detailSize, detailX + detailWidth + step - 1) / step;
                int y1 = Math.min(detailSize, detailY + detailHeight + step - 1) / step;
                try (NativeImage image = new NativeImage(NativeImage.Format.RGBA, x1 - x0, y1 - y0, false)) {
                    for (int row = y0; row < y1; row++) {
                        for (int column = x0; column < x1; column++) {
                            int sum = 0;
                            for (int sampleY = 0; sampleY < step; sampleY++) {
                                int offset = (row * step + sampleY) * detailSize + column * step;
                                for (int sampleX = 0; sampleX < step; sampleX++) {
                                    sum += this.detailCoverage[offset + sampleX] & 0xFF;
                                }
                            }
                            image.setPixelABGR(column - x0, row - y0, pixel(sum / (step * step)));
                        }
                    }
                    encoder.writeToTexture(this.detailTexture.getTexture(), image, level, 0, x0, y0);
                }
            }
        }

        private void close() {
            Minecraft.getInstance().getTextureManager().release(this.detailId);
            this.texture.close();
        }
    }

    private static int pixel(int alpha) {
        return alpha << 24 | EMPTY_PIXEL;
    }

    private static final class AtlasTexture extends AbstractTexture {
        private AtlasTexture(String label, int size, int mipLevels, GpuSampler sampler) {
            GpuDevice device = RenderSystem.getDevice();
            this.texture = device.createTexture("raven:" + label, 7, GpuFormat.RGBA8_UNORM, size, size, 1, mipLevels);
            this.textureView = device.createTextureView(this.texture);
            this.sampler = sampler;
            CommandEncoder encoder = device.createCommandEncoder();
            for (int level = 0; level < mipLevels; level++) {
                int levelSize = size >> level;
                try (NativeImage image = new NativeImage(NativeImage.Format.RGBA, levelSize, levelSize, false)) {
                    image.fillRect(0, 0, levelSize, levelSize, EMPTY_PIXEL);
                    encoder.writeToTexture(this.texture, image, level, 0, 0, 0);
                }
            }
        }
    }
}
