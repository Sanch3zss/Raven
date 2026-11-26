package sanch3zss.raven.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;

import sanch3zss.raven.event.Event;

public final class WorldRenderEvent implements Event {
    public final PoseStack matrixStack;
    public final Matrix4f modelViewMatrix;
    public final Matrix4f projectionMatrix;

    public final DeltaTracker renderTickCounter;

    public final Frustum frustum;

    public WorldRenderEvent(PoseStack matrixStack, Matrix4f matrix4f, Matrix4f matrix4f2, DeltaTracker renderTickCounter, Frustum frustum) {
        this.matrixStack = matrixStack;
        this.modelViewMatrix = matrix4f;
        this.projectionMatrix = matrix4f2;
        this.renderTickCounter = renderTickCounter;
        this.frustum = frustum;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "matrixStack=" + this.matrixStack + ", " + "modelViewMatrix=" + this.modelViewMatrix + ", " + "projectionMatrix=" + this.projectionMatrix + ", " + "renderTickCounter=" + this.renderTickCounter + ", " + "frustum=" + this.frustum + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.matrixStack, this.modelViewMatrix, this.projectionMatrix, this.renderTickCounter, this.frustum);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof WorldRenderEvent)) return false;
        WorldRenderEvent o = (WorldRenderEvent) obj;
        return java.util.Objects.equals(this.matrixStack, o.matrixStack) && java.util.Objects.equals(this.modelViewMatrix, o.modelViewMatrix) && java.util.Objects.equals(this.projectionMatrix, o.projectionMatrix) && java.util.Objects.equals(this.renderTickCounter, o.renderTickCounter) && java.util.Objects.equals(this.frustum, o.frustum);
    }
public PoseStack matrixStack() {
        return this.matrixStack;
    }

    public Matrix4f modelViewMatrix() {
        return this.modelViewMatrix;
    }

    public Matrix4f projectionMatrix() {
        return this.projectionMatrix;
    }

    public DeltaTracker renderTickCounter() {
        return this.renderTickCounter;
    }

    public Frustum frustum() {
        return this.frustum;
    }
}
