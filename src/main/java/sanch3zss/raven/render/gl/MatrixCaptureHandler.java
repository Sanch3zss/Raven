package sanch3zss.raven.render.gl;

import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.ClientListener;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.util.math.ProjectionUtil;

public class MatrixCaptureHandler implements ClientListener {
    private static Matrix4f worldProjection;

    public static void captureWorldProjection(Matrix4fc projection) {
        worldProjection = new Matrix4f(projection);
    }

    public static Matrix4f worldProjection(Matrix4fc fallback) {
        return worldProjection == null ? new Matrix4f(fallback) : new Matrix4f(worldProjection);
    }

    public MatrixCaptureHandler() {
        Raven.INSTANCE.eventDispatcher().register(WorldRenderEvent.class, event -> {
            if (event.modelViewMatrix() != null) {
                ProjectionUtil.LAST_MODEL_VIEW.set(new Matrix4f(event.modelViewMatrix()));
            }
            if (event.projectionMatrix() != null) {
                ProjectionUtil.LAST_PROJECTION.set(new Matrix4f(event.projectionMatrix()));
            }
        });
    }
}
