package sanch3zss.raven.render.shape;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;public class LineRenderShape extends RenderShape {
    public Matrix4f matrix4f;
    public Vec3 start, end;
    public int color;
    public float width;
    public boolean cameraRelative;

    public LineRenderShape() {
        this.state = RenderShapeState.POSITION_COLOR_LINES;
    }

    public LineRenderShape set(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, int i, float f, boolean z) {
        if (this.matrix4f == null) {
            this.matrix4f = new Matrix4f(matrix4f);
        } else {
            this.matrix4f.set(matrix4f);
        }
        this.start = vec3d;
        this.end = vec3d2;
        this.color = i;
        this.width = f;
        this.cameraRelative = z;
        return this;
    }

    public LineRenderShape depthTested() {
        this.state = RenderShapeState.POSITION_COLOR_LINES_DEPTH;
        return this;
    }

    public LineRenderShape withoutDepthTest() {
        this.state = RenderShapeState.POSITION_COLOR_LINES;
        return this;
    }

    @Override
    public void emit(PoseStack matrixStack, BufferBuilder bufferBuilder) {
        if (start != null && end != null) {
            ShapeRenderer.INSTANCE.emitLine(
                    matrix4f, bufferBuilder, start, end,
                    color, width, cameraRelative
            );
        }
    }
}
