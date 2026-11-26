package sanch3zss.raven.render.shape;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import sanch3zss.raven.render.shape.RenderShapeState;

public class RenderShape {
    protected RenderShapeState state = RenderShapeState.POSITION_COLOR_TRIANGLES;

    public void emit(PoseStack matrixStack, BufferBuilder bufferBuilder) {
    }

    public RenderShapeState state() {
        return this.state;
    }
}
