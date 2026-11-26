package sanch3zss.raven.client.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import org.joml.Matrix4fc;

final class SmoothGlyph implements BakedGlyph {
    private final GlyphInfo info;
    private final GlyphRenderTypes renderTypes;
    private final GpuTextureView textureView;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final float left;
    private final float right;
    private final float up;
    private final float down;

    SmoothGlyph(GlyphInfo info, GlyphRenderTypes renderTypes, GpuTextureView textureView, float u0, float u1,
                float v0, float v1, float left, float right, float up, float down) {
        this.info = info;
        this.renderTypes = renderTypes;
        this.textureView = textureView;
        this.u0 = u0;
        this.u1 = u1;
        this.v0 = v0;
        this.v1 = v1;
        this.left = left;
        this.right = right;
        this.up = up;
        this.down = down;
    }

    @Override
    public GlyphInfo info() {
        return this.info;
    }

    @Override
    public TextRenderable.Styled createGlyph(float x, float y, int color, int shadowColor, Style style,
                                             float boldOffset, float shadowOffset) {
        return new Instance(this, x, y, color, shadowColor, style, boldOffset, shadowOffset);
    }

    private void renderChar(Instance instance, Matrix4fc pose, VertexConsumer buffer, int light, boolean flat) {
        boolean italic = instance.style.isItalic();
        boolean bold = instance.style.isBold();
        float zFighter = flat ? 0.0f : 0.001f;
        float depth = 0.0f;
        if (instance.hasShadow()) {
            float shadowX = instance.x + instance.shadowOffset;
            float shadowY = instance.y + instance.shadowOffset;
            render(italic, shadowX, shadowY, 0.0f, pose, buffer, instance.shadowColor, bold, light);
            if (bold) {
                render(italic, shadowX + instance.boldOffset, shadowY, zFighter, pose, buffer, instance.shadowColor,
                        true, light);
            }
            depth = flat ? 0.0f : 0.03f;
        }
        render(italic, instance.x, instance.y, depth, pose, buffer, instance.color, bold, light);
        if (bold) {
            render(italic, instance.x + instance.boldOffset, instance.y, depth + zFighter, pose, buffer,
                    instance.color, true, light);
        }
    }

    private void render(boolean italic, float x, float y, float z, Matrix4fc pose, VertexConsumer buffer, int color,
                        boolean bold, int light) {
        float x0 = x + this.left;
        float x1 = x + this.right;
        float y0 = y + this.up;
        float y1 = y + this.down;
        float shearTop = italic ? shearTop() : 0.0f;
        float shearBottom = italic ? shearBottom() : 0.0f;
        float thickness = extraThickness(bold);
        buffer.addVertex(pose, x0 + shearTop - thickness, y0 - thickness, z).setColor(color).setUv(this.u0, this.v0).setLight(light);
        buffer.addVertex(pose, x0 + shearBottom - thickness, y1 + thickness, z).setColor(color).setUv(this.u0, this.v1).setLight(light);
        buffer.addVertex(pose, x1 + shearBottom + thickness, y1 + thickness, z).setColor(color).setUv(this.u1, this.v1).setLight(light);
        buffer.addVertex(pose, x1 + shearTop + thickness, y0 - thickness, z).setColor(color).setUv(this.u1, this.v0).setLight(light);
    }

    private static float extraThickness(boolean bold) {
        return bold ? 0.1f : 0.0f;
    }

    private float shearTop() {
        return 1.0f - 0.25f * this.up;
    }

    private float shearBottom() {
        return 1.0f - 0.25f * this.down;
    }

    private record Instance(SmoothGlyph glyph, float x, float y, int color, int shadowColor, Style style,
                            float boldOffset, float shadowOffset) implements TextRenderable.Styled {
        private boolean hasShadow() {
            return this.shadowColor != 0;
        }

        @Override
        public float left() {
            float shear = this.style.isItalic() ? Math.min(this.glyph.shearTop(), this.glyph.shearBottom()) : 0.0f;
            return this.x + this.glyph.left + shear - extraThickness(this.style.isBold());
        }

        @Override
        public float top() {
            return this.y + this.glyph.up - extraThickness(this.style.isBold());
        }

        @Override
        public float right() {
            float shear = this.style.isItalic() ? Math.max(this.glyph.shearTop(), this.glyph.shearBottom()) : 0.0f;
            return this.x + this.glyph.right + (hasShadow() ? this.shadowOffset : 0.0f) + shear
                    + extraThickness(this.style.isBold());
        }

        @Override
        public float bottom() {
            return this.y + this.glyph.down + (hasShadow() ? this.shadowOffset : 0.0f)
                    + extraThickness(this.style.isBold());
        }

        @Override
        public float activeRight() {
            return this.x + this.glyph.info.getAdvance(this.style.isBold());
        }

        @Override
        public void render(Matrix4fc pose, VertexConsumer buffer, int light, boolean flat) {
            this.glyph.renderChar(this, pose, buffer, light, flat);
        }

        @Override
        public RenderType renderType(Font.DisplayMode mode) {
            return this.glyph.renderTypes.select(mode);
        }

        @Override
        public GpuTextureView textureView() {
            return this.glyph.textureView;
        }

        @Override
        public RenderPipeline guiPipeline() {
            return this.glyph.renderTypes.guiPipeline();
        }
    }
}
