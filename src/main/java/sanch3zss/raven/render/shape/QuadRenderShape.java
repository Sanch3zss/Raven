package sanch3zss.raven.render.shape;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;public class QuadRenderShape extends RenderShape {
    public Matrix4f matrix4f;
    public Vec3 v1, v2, v3, v4;
    public int color;
    public boolean cameraRelative;

    public QuadRenderShape() {
        this.state = RenderShapeState.POSITION_COLOR_QUADS;
    }

    public QuadRenderShape set(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3, Vec3 vec3d4, int i, boolean z) {
        this.state = RenderShapeState.POSITION_COLOR_QUADS;
        if (this.matrix4f == null) {
            this.matrix4f = new Matrix4f(matrix4f);
        } else {
            this.matrix4f.set(matrix4f);
        }
        this.v1 = vec3d;
        this.v2 = vec3d2;
        this.v3 = vec3d3;
        this.v4 = vec3d4;
        this.color = i;
        this.cameraRelative = z;
        return this;
    }

    public QuadRenderShape depthTested() {
        this.state = RenderShapeState.POSITION_COLOR_QUADS_DEPTH;
        return this;
    }

    @Override
    public void emit(PoseStack matrixStack, BufferBuilder bufferBuilder) {
        if (v1 != null && v2 != null && v3 != null && v4 != null) {
            ShapeRenderer.INSTANCE.emitQuad(matrix4f, bufferBuilder, v1, v2, v3, v4, color, cameraRelative);
        }
    }
}
