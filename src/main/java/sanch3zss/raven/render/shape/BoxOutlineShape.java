package sanch3zss.raven.render.shape;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;public class BoxOutlineShape extends RenderShape {
    public Matrix4f matrix4f;
    public AABB box;
    public int color, color2;
    public boolean z, z2, cameraRelative;
    public float width;

    public BoxOutlineShape() {
        this.state = RenderShapeState.POSITION_COLOR_LINES;
    }

    public BoxOutlineShape set(Matrix4f matrix4f, AABB box, int i, int i2, boolean z, boolean z2, float f, boolean z3) {
        if (this.matrix4f == null) {
            this.matrix4f = new Matrix4f(matrix4f);
        } else {
            this.matrix4f.set(matrix4f);
        }
        this.box = box;
        this.color = i;
        this.color2 = i2;
        this.z = z;
        this.z2 = z2;
        this.width = f;
        this.cameraRelative = z3;
        return this;
    }

    @Override
    public void emit(PoseStack matrixStack, BufferBuilder bufferBuilder) {
        if (box != null) {
            ShapeRenderer.INSTANCE.emitBoxOutline(
                    matrix4f, bufferBuilder, box, color, color2,
                    z, z2, width, cameraRelative
            );
        }
    }
}
