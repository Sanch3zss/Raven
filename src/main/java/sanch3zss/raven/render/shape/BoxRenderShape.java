package sanch3zss.raven.render.shape;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;public class BoxRenderShape extends RenderShape {
    public Matrix4f matrix4f;
    public AABB box;
    public int color;
    public boolean z, z2, cameraRelative;

    public BoxRenderShape() {
        this.state = RenderShapeState.POSITION_COLOR_QUADS;
    }

    public BoxRenderShape set(Matrix4f matrix4f, AABB box, int i, boolean z, boolean z2, boolean z3) {
        if (this.matrix4f == null) {
            this.matrix4f = new Matrix4f(matrix4f);
        } else {
            this.matrix4f.set(matrix4f);
        }
        this.box = box;
        this.color = i;
        this.z = z;
        this.z2 = z2;
        this.cameraRelative = z3;
        return this;
    }

    @Override
    public void emit(PoseStack matrixStack, BufferBuilder bufferBuilder) {
        if (box != null) {
            ShapeRenderer.INSTANCE.emitBox(matrix4f, bufferBuilder, box, color, cameraRelative);
        }
    }
}
