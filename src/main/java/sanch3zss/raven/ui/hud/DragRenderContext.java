package sanch3zss.raven.ui.hud;


import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.engine.ScreenResolution;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.MouseHandler;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;import sanch3zss.raven.render.theme.Theme;

public final class DragRenderContext {
    public final ScreenResolution resolution;
    public final MouseHandler mousePosition;
    public final PoseStack matrixStack;

    public final DrawEngine drawEngine;

    public final Theme theme;
    public final float aF;
    private final Consumer<VanillaItemDraw> vanillaItemSink;
    private final Consumer<VanillaSpriteDraw> vanillaSpriteSink;
    private final Consumer<VanillaTextDraw> vanillaTextSink;

    public DragRenderContext(ScreenResolution screenResolution, MouseHandler mouse, PoseStack matrixStack,
                             DrawEngine drawEngine, Theme theme, float f,
                             Consumer<VanillaItemDraw> vanillaItemSink,
                             Consumer<VanillaSpriteDraw> vanillaSpriteSink,
                             Consumer<VanillaTextDraw> vanillaTextSink) {
        this.resolution = screenResolution;
        this.mousePosition = mouse;
        this.matrixStack = matrixStack;
        this.drawEngine = drawEngine;
        this.theme = theme;
        this.aF = f;
        this.vanillaItemSink = vanillaItemSink;
        this.vanillaSpriteSink = vanillaSpriteSink;
        this.vanillaTextSink = vanillaTextSink;
    }

    public void drawVanillaItem(ItemStack stack, float x, float y, float size, boolean overlay) {
        drawVanillaItem(stack, x, y, size, overlay, 1.0f);
    }

    public void drawVanillaItem(ItemStack stack, float x, float y, float size, boolean overlay,
                                float alpha) {
        if (this.vanillaItemSink == null || stack == null || stack.isEmpty() || size <= 0.0f) {
            return;
        }
        Matrix4f matrix = this.matrixStack.last().pose();
        Vector3f topLeft = matrix.transformPosition(x, y, 0.0f, new Vector3f());
        Vector3f bottomRight = matrix.transformPosition(x + size, y + size, 0.0f, new Vector3f());
        this.vanillaItemSink.accept(new VanillaItemDraw(stack.copy(), topLeft.x, topLeft.y,
                bottomRight.x - topLeft.x, bottomRight.y - topLeft.y, overlay,
                Math.max(0.0f, Math.min(1.0f, alpha))));
    }

    public record VanillaItemDraw(ItemStack stack, float x, float y, float width, float height,
                                  boolean overlay, float alpha) {
    }

    public void drawVanillaSprite(Identifier sprite, float x, float y, float size, float alpha) {
        if (this.vanillaSpriteSink == null || sprite == null || size <= 0.0f) {
            return;
        }
        Matrix4f matrix = this.matrixStack.last().pose();
        Vector3f topLeft = matrix.transformPosition(x, y, 0.0f, new Vector3f());
        Vector3f bottomRight = matrix.transformPosition(x + size, y + size, 0.0f, new Vector3f());
        this.vanillaSpriteSink.accept(new VanillaSpriteDraw(sprite, topLeft.x, topLeft.y,
                bottomRight.x - topLeft.x, bottomRight.y - topLeft.y,
                Math.max(0.0f, Math.min(1.0f, alpha))));
    }

    public record VanillaSpriteDraw(Identifier sprite, float x, float y, float width, float height,
                                    float alpha) {
    }

    public void drawVanillaText(Component component, float x, float y, float scale, int color) {
        if (this.vanillaTextSink == null || component == null) {
            return;
        }
        Matrix4f matrix = this.matrixStack.last().pose();
        Vector3f origin = matrix.transformPosition(x, y, 0.0f, new Vector3f());
        Vector3f extent = matrix.transformPosition(x + scale, y + scale, 0.0f, new Vector3f());
        float pixelScale = Math.max(1.0f,
                Math.round(Math.abs(extent.y - origin.y) * 9.0f)) / 9.0f;
        this.vanillaTextSink.accept(new VanillaTextDraw(component,
                Math.round(origin.x), Math.round(origin.y), pixelScale, pixelScale, color));
    }

    public record VanillaTextDraw(Component component, float x, float y,
                                  float scaleX, float scaleY, int color) {
    }

    public float mouseX() {
        return (float) (this.mousePosition.xpos() / ((double) this.aF));
    }

    public float mouseY() {
        return (float) (this.mousePosition.ypos() / ((double) this.aF));
    }

    public void sizeAnimation(PoseStack matrixStack, float f, float f2, float f3) {
        matrixStack.translate(f / 2.0f, f2 / 2.0f, 0.0f);
        matrixStack.scale(f3, f3, 0.0f);
        matrixStack.translate((-f) / 2.0f, (-f2) / 2.0f, 0.0f);
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "resolution=" + this.resolution + ", " + "mousePosition=" + this.mousePosition + ", " + "matrixStack=" + this.matrixStack + ", " + "drawEngine=" + this.drawEngine + ", " + "theme=" + this.theme + ", " + "aF=" + this.aF + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.resolution, this.mousePosition, this.matrixStack, this.drawEngine, this.theme, this.aF);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DragRenderContext)) return false;
        DragRenderContext o = (DragRenderContext) obj;
        return java.util.Objects.equals(this.resolution, o.resolution) && java.util.Objects.equals(this.mousePosition, o.mousePosition) && java.util.Objects.equals(this.matrixStack, o.matrixStack) && java.util.Objects.equals(this.drawEngine, o.drawEngine) && java.util.Objects.equals(this.theme, o.theme) && java.util.Objects.equals(this.aF, o.aF);
    }
public ScreenResolution resolution() {
        return this.resolution;
    }

    public MouseHandler mousePosition() {
        return this.mousePosition;
    }

    public PoseStack matrixStack() {
        return this.matrixStack;
    }

    public DrawEngine drawEngine() {
        return this.drawEngine;
    }

    public Theme theme() {
        return this.theme;
    }

    public float scaleFactor() {
        return this.aF;
    }
}
