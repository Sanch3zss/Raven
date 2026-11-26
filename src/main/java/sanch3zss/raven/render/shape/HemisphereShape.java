package sanch3zss.raven.render.shape;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;public class HemisphereShape extends RenderShape {
    public Matrix4f matrix4f;
    public Vec3 center;
    public float radius;
    public int segments, color;
    public boolean cameraRelative;

    public HemisphereShape() {
        this.state = RenderShapeState.POSITION_COLOR_TRIANGLES;
    }

    public HemisphereShape set(Matrix4f matrix4f, Vec3 vec3d, float f, int i, int i2, boolean z) {
        if (this.matrix4f == null) {
            this.matrix4f = new Matrix4f(matrix4f);
        } else {
            this.matrix4f.set(matrix4f);
        }
        this.center = vec3d;
        this.radius = f;
        this.segments = i;
        this.color = i2;
        this.cameraRelative = z;
        return this;
    }

    @Override
    public void emit(PoseStack matrixStack, BufferBuilder bufferBuilder) {
        if (center != null) {
            ShapeRenderer.INSTANCE.emitHemisphere(matrix4f, bufferBuilder, center, radius, segments, color, cameraRelative);
        }
    }
}
