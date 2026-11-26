package sanch3zss.raven.render.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.util.Mth;
import org.joml.Vector4f;
import org.joml.Vector4i;

import sanch3zss.raven.client.input.InputInterceptor;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.MsdfFont;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;

public final class DrawCtx {
    public final InputInterceptor window;
    public final ScreenResolution resolution;
    public final PoseStack matrixStack;
    public final PixelPoint mousePosition;
    public final DrawEngine drawEngine;
    public final ColorStack colorStack;
    public final PixelPoint lastKnownMousePosition;
    public final Theme theme;
    public final LayoutScaleContext layoutContext;

    public DrawCtx(InputInterceptor inputInterceptor, ScreenResolution screenResolution, PoseStack matrixStack, PixelPoint point2, DrawEngine drawEngine, ColorStack colorStack, PixelPoint point, Theme theme, LayoutScaleContext layoutScaleContext) {
        this.window = inputInterceptor;
        this.resolution = screenResolution;
        this.matrixStack = matrixStack;
        this.mousePosition = point2;
        this.drawEngine = drawEngine;
        this.colorStack = colorStack;
        this.lastKnownMousePosition = point;
        this.theme = theme;
        this.layoutContext = layoutScaleContext;
    }

    public DrawCtx withTheme(Theme otherTheme) {
        if (otherTheme == this.theme) {
            return this;
        }
        return new DrawCtx(this.window, this.resolution, this.matrixStack, this.mousePosition, this.drawEngine,
                this.colorStack, this.lastKnownMousePosition, otherTheme, this.layoutContext);
    }

    public void fillRoundedRect(float f, float f2, float f3, float f4, Vector4f vector4f, int i) {
        Vector4f clampedRadii = clampRadii(f3, f4, vector4f);
        this.drawEngine.roundedRectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.layoutContext.toPhysical(clampedRadii.x), this.layoutContext.toPhysical(clampedRadii.y), this.layoutContext.toPhysical(clampedRadii.z), this.layoutContext.toPhysical(clampedRadii.w), i);
    }

    public void fillVerticalGradientRect(float f, float f2, float f3, float f4, int i, int i2) {
        fillGradientRoundedRect(f, f2, f3, f4, 0, i2, i2, i, i);
    }

    public void fillGradientRoundedRect(float f, float f2, float f3, float f4, int i, int i2, int i3, int i4, int i5) {
        this.drawEngine.roundedRectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.layoutContext.toPhysical(clampRadius(f3, f4, i)), i2, i3, i4, i5);
    }

    public void radialRoundedRectangle(float f, float f2, float f3, float f4, float f5, int i, int i2) {
        this.drawEngine.radialRoundedRectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.layoutContext.toPhysical(f5), i, i2);
    }

    public void fillGradientOutlinedRoundedRect(float f, float f2, float f3, float f4, int i, float f5, Vector4i vector4i, Vector4i vector4i2) {
        this.drawEngine.roundedRectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.layoutContext.toPhysical(i), this.layoutContext.toPhysical(f5), vector4i.x, vector4i.y, vector4i.z, vector4i.w, vector4i2.x, vector4i2.y, vector4i2.z, vector4i2.w);
    }

    public void fillRoundedRect(float f, float f2, float f3, float f4, float f5, int i) {
        float radius = clampRadius(f3, f4, f5);
        this.drawEngine.roundedRectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.layoutContext.toPhysical(radius), this.layoutContext.toPhysical(radius), this.layoutContext.toPhysical(radius), this.layoutContext.toPhysical(radius), i);
    }

    public void fillOutlinedRoundedRect(float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        this.drawEngine.roundedRectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.layoutContext.toPhysical(clampRadius(f3, f4, f5)), this.layoutContext.toPhysical(f6), i, i2);
    }

    public void bloom(float f, float f2, float f3, float f4, float f5, int i) {
        float fScreenWidth = this.resolution.screenWidth();
        float fScreenHeight = this.resolution.screenHeight();
        float physical = this.layoutContext.toPhysical(f - f5);
        float physical2 = this.layoutContext.toPhysical(f2 - f5);
        float physical3 = this.layoutContext.toPhysical(f3 + (f5 * 2.0f));
        float physical4 = this.layoutContext.toPhysical(f4 + (f5 * 2.0f));
        this.drawEngine.texture(this.matrixStack.last().pose(), physical, physical2, physical3, physical4, physical / fScreenWidth, 1.0f - (physical2 / fScreenHeight), (physical + physical3) / fScreenWidth, 1.0f - ((physical2 + physical4) / fScreenHeight), this.drawEngine.bindTexture(i), this.colorStack.computeColor(this.colorStack.white(), 150));
    }

    public void fillOutlinedRoundedRect(float f, float f2, float f3, float f4, Vector4f vector4f, float f5, int i, int i2) {
        Vector4f clampedRadii = clampRadii(f3, f4, vector4f);
        this.drawEngine.roundedRectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.layoutContext.toPhysical(clampedRadii.x), this.layoutContext.toPhysical(clampedRadii.y), this.layoutContext.toPhysical(clampedRadii.z), this.layoutContext.toPhysical(clampedRadii.w), this.layoutContext.toPhysical(f5), i, i2);
    }

    public void roundedBlur(int i, float f, float f2, float f3, float f4, Vector4f vector4f, int i2) {
        this.drawEngine.roundedBlur(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.layoutContext.toPhysical(vector4f.x), this.layoutContext.toPhysical(vector4f.y), this.layoutContext.toPhysical(vector4f.z), this.layoutContext.toPhysical(vector4f.w), i2, i);
    }

    public void roundedBlur(int i, float f, float f2, float f3, float f4, float f5, int i2) {
        float physical = this.layoutContext.toPhysical(f5);
        this.drawEngine.roundedBlur(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), physical, physical, physical, physical, i2, i);
    }

    public void circle(float f, float f2, float f3, int i) {
        this.drawEngine.circle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), i);
    }

    public void arc(float centerX, float centerY, float radius, float startDegrees, float endDegrees, float thickness, int color) {
        this.drawEngine.arc(this.matrixStack.last().pose(),
                this.layoutContext.toPhysical(centerX), this.layoutContext.toPhysical(centerY),
                this.layoutContext.toPhysical(radius), startDegrees, endDegrees,
                this.layoutContext.toPhysical(thickness), color);
    }

    public void outlineCircle(float f, float f2, float f3, float f4, int i, int i2) {
        this.drawEngine.circle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), i, i2);
    }

    public void hairline(float x, float y, float width, int color) {
        float thickness = Math.max(1.0f, Math.round(this.layoutContext.toPhysical(1.0f)));
        this.drawEngine.rectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysicalAligned(x),
                this.layoutContext.toPhysicalAligned(y), this.layoutContext.toPhysicalAligned(width), thickness, color);
    }

    public void verticalHairline(float x, float y, float height, int color) {
        float thickness = Math.max(1.0f, Math.round(this.layoutContext.toPhysical(1.0f)));
        this.drawEngine.rectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysicalAligned(x),
                this.layoutContext.toPhysicalAligned(y), thickness, this.layoutContext.toPhysicalAligned(height), color);
    }

    public void fillRect(float f, float f2, float f3, float f4, int i) {
        this.drawEngine.rectangle(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), i);
    }

    public void texture(int i, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i2) {
        this.drawEngine.texture(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), f5, f6, f7, f8, this.drawEngine.bindTexture(i), i2);
    }

    public void texture(int i, float f, float f2, float f3, float f4, int i2) {
        this.drawEngine.texture(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.drawEngine.bindTexture(i), i2);
    }

    public void texture(GlTexture texture, float f, float f2, float f3, float f4, int i) {
        this.drawEngine.texture(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.drawEngine.bindTexture(texture.textureWithSTB()), i);
    }

    public void roundedTexture(GlTexture texture, float f, float f2, float f3, float f4, float f5, int i) {
        this.drawEngine.roundedTexture(this.matrixStack.last().pose(), this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2), this.layoutContext.toPhysical(f3), this.layoutContext.toPhysical(f4), this.layoutContext.toPhysical(f5), this.drawEngine.bindTexture(texture.textureWithSTB()), i);
    }

    public void textureVerticalC(GlTexture texture, float f, float f2, int i, int i2, int i3) {
        texture(texture, f, f2 - (Mth.ceil(i2) / 2.0f), i, i2, i3);
    }

    public void textureVerticalCHorizontalC(GlTexture texture, float f, float f2, int i, int i2, int i3) {
        texture(texture, f - (Mth.ceil(texture.width()) / 2.0f), f2 - (Mth.ceil(texture.height()) / 2.0f), i, i2, i3);
    }

    public void roundedTextureVerticalCHorizontalC(GlTexture texture, float f, float f2, int i, int i2, float f3, int i3) {
        roundedTexture(texture, f - (Mth.ceil(texture.width()) / 2.0f), f2 - (Mth.ceil(texture.height()) / 2.0f), i, i2, f3, i3);
    }

    public void textureVerticalCHorizontalC(GlTexture texture, float f, float f2, int i) {
        texture(texture, f - (Mth.ceil(texture.width()) / 2.0f), f2 - (Mth.ceil(texture.height()) / 2.0f), texture.width(), texture.height(), i);
    }

    public void text(MsdfFont msdfFont, String str, int i, float f, float f2, int i2) {
        float fScaleFactor = this.layoutContext.scaleFactor();
        drawEngine().msdfFont(matrixStack().last().pose(), msdfFont, str,
                this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2),
                Math.max(1.0f, i * fScaleFactor), 0.05f * fScaleFactor, i2);
    }

    public void textWithHorizontalGradient(MsdfFont msdfFont, String str, int i, float f, float f2, int i2, int i3) {
        float fScaleFactor = this.layoutContext.scaleFactor();
        drawEngine().msdfFontWithHorizontalGradient(matrixStack().last().pose(), msdfFont, str,
                this.layoutContext.toPhysical(f), this.layoutContext.toPhysical(f2),
                Math.max(1.0f, i * fScaleFactor), 0.05f * fScaleFactor, i2, i3);
    }

    public float textWidthPhysical(MsdfFont msdfFont, String str, int i) {
        float fScaleFactor = this.layoutContext.scaleFactor();
        return msdfFont.getWidth(str, Math.max(1.0f, i * fScaleFactor)) / fScaleFactor;
    }

    public float clampRadius(float f, float f2, float f3) {
        if (f3 <= 0.0f) {
            return 0.0f;
        }
        return Math.min(f3, 0.5f * Math.min(f, f2));
    }

    public Vector4f clampRadii(float f, float f2, Vector4f vector4f) {
        float fMin = 0.5f * Math.min(f, f2);
        return new Vector4f(Math.min(Math.max(vector4f.x, 0.0f), fMin), Math.min(Math.max(vector4f.y, 0.0f), fMin), Math.min(Math.max(vector4f.z, 0.0f), fMin), Math.min(Math.max(vector4f.w, 0.0f), fMin));
    }

    public PixelPoint logicalMousePosition() {
        return this.layoutContext.toLogical(this.mousePosition);
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "window=" + this.window + ", " + "resolution=" + this.resolution + ", " + "matrixStack=" + this.matrixStack + ", " + "mousePosition=" + this.mousePosition + ", " + "drawEngine=" + this.drawEngine + ", " + "colorStack=" + this.colorStack + ", " + "lastKnownMousePosition=" + this.lastKnownMousePosition + ", " + "theme=" + this.theme + ", " + "layoutContext=" + this.layoutContext + "]";
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.window, this.resolution, this.matrixStack, this.mousePosition, this.drawEngine, this.colorStack, this.lastKnownMousePosition, this.theme, this.layoutContext);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DrawCtx)) return false;
        DrawCtx o = (DrawCtx) obj;
        return Objects.equals(this.window, o.window) && Objects.equals(this.resolution, o.resolution) && Objects.equals(this.matrixStack, o.matrixStack) && Objects.equals(this.mousePosition, o.mousePosition) && Objects.equals(this.drawEngine, o.drawEngine) && Objects.equals(this.colorStack, o.colorStack) && Objects.equals(this.lastKnownMousePosition, o.lastKnownMousePosition) && Objects.equals(this.theme, o.theme) && Objects.equals(this.layoutContext, o.layoutContext);
    }

    public InputInterceptor window() {
        return this.window;
    }

    public ScreenResolution resolution() {
        return this.resolution;
    }

    public PoseStack matrixStack() {
        return this.matrixStack;
    }

    public PixelPoint mousePosition() {
        return this.mousePosition;
    }

    public DrawEngine drawEngine() {
        return this.drawEngine;
    }

    public ColorStack colorStack() {
        return this.colorStack;
    }

    public PixelPoint lastKnownMousePosition() {
        return this.lastKnownMousePosition;
    }

    public Theme theme() {
        return this.theme;
    }

    public LayoutScaleContext layoutContext() {
        return this.layoutContext;
    }
}
