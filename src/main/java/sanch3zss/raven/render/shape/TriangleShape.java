package sanch3zss.raven.render.shape;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;public class TriangleShape extends RenderShape {
    public Matrix4f matrix4f;
    public Vec3 v1, v2, v3;
    public int color;
    public boolean cameraRelative;

    public TriangleShape() {
        this.state = RenderShapeState.POSITION_COLOR_TRIANGLES;
    }

    public TriangleShape set(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3, int i, boolean z) {
        if (this.matrix4f == null) {
            this.matrix4f = new Matrix4f(matrix4f);
        } else {
            this.matrix4f.set(matrix4f);
        }
        this.v1 = vec3d;
        this.v2 = vec3d2;
        this.v3 = vec3d3;
        this.color = i;
        this.cameraRelative = z;
        return this;
    }

    @Override
    public void emit(PoseStack matrixStack, BufferBuilder bufferBuilder) {
        if (v1 != null && v2 != null && v3 != null) {
            ShapeRenderer.INSTANCE.emitTriangle(matrix4f, bufferBuilder, v1, v2, v3, color, cameraRelative);
        }
    }
}
