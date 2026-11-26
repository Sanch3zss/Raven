package sanch3zss.raven.util.math;

import com.mojang.blaze3d.platform.Window;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

public final class ProjectionUtil {
    private static final float CAMERA_PLANE_EPSILON = 0.001f;
    private static final int[][] BOX_EDGES = {
            {0, 1}, {0, 2}, {0, 4},
            {1, 3}, {1, 5},
            {2, 3}, {2, 6},
            {3, 7},
            {4, 5}, {4, 6},
            {5, 7},
            {6, 7}
    };
    public static final AtomicReference<Matrix4f> LAST_PROJECTION = new AtomicReference<>();
    public static final AtomicReference<Matrix4f> LAST_MODEL_VIEW = new AtomicReference<>();

    public static Optional<Vector2f> worldToScreen(Vec3 vec3d) {
        Matrix4f matrix4f = LAST_PROJECTION.get();
        Matrix4f matrix4f2 = LAST_MODEL_VIEW.get();
        if (matrix4f == null || matrix4f2 == null) {
            return Optional.empty();
        }
        Matrix4f matrix4fMul = new Matrix4f(matrix4f).mul(matrix4f2);
        Vec3 vec3dSubtract = vec3d.subtract(Minecraft.getInstance().gameRenderer.mainCamera().position());
        Vector4f vector4f = new Vector4f((float) vec3dSubtract.x, (float) vec3dSubtract.y, (float) vec3dSubtract.z, 1.0f);
        matrix4fMul.transform(vector4f);
        if (vector4f.w <= 0.0f || Float.isNaN(vector4f.w)) {
            return Optional.empty();
        }
        float f = vector4f.x / vector4f.w;
        float f2 = vector4f.y / vector4f.w;
        Window window = Minecraft.getInstance().getWindow();
        float framebufferWidth = (f + 1.0f) * 0.5f * window.getWidth();
        float framebufferHeight = (1.0f - f2) * 0.5f * window.getHeight();
        return (Float.isNaN(framebufferWidth) || Float.isNaN(framebufferHeight) || Float.isInfinite(framebufferWidth) || Float.isInfinite(framebufferHeight)) ? Optional.empty() : Optional.of(new Vector2f(framebufferWidth, framebufferHeight));
    }

    public static boolean isOutOfScreen(Vector4f vector4f) {
        return vector4f == null || (vector4f.x < 0.0f && vector4f.z < 1.0f) || (vector4f.y < 0.0f && vector4f.w < 1.0f);
    }

    public static Optional<Vector4f> boxToScreen(AABB box) {
        Matrix4f matrix4f = LAST_PROJECTION.get();
        Matrix4f matrix4f2 = LAST_MODEL_VIEW.get();
        if (matrix4f == null || matrix4f2 == null) {
            return Optional.empty();
        }
        Matrix4f matrix4fMul = new Matrix4f(matrix4f).mul(matrix4f2);
        Vec3 camPos = Minecraft.getInstance().gameRenderer.mainCamera().position();

        Window window = Minecraft.getInstance().getWindow();
        float fw = window.getWidth();
        float fh = window.getHeight();
        if (fw <= 0.0f || fh <= 0.0f) {
            return Optional.empty();
        }

        Vec3[] corners = {
                new Vec3(box.minX, box.minY, box.minZ),
                new Vec3(box.minX, box.minY, box.maxZ),
                new Vec3(box.minX, box.maxY, box.minZ),
                new Vec3(box.minX, box.maxY, box.maxZ),
                new Vec3(box.maxX, box.minY, box.minZ),
                new Vec3(box.maxX, box.minY, box.maxZ),
                new Vec3(box.maxX, box.maxY, box.minZ),
                new Vec3(box.maxX, box.maxY, box.maxZ)
        };
        Vector4f[] clipCorners = new Vector4f[corners.length];
        float[] bounds = {
                Float.POSITIVE_INFINITY,
                Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY,
                Float.NEGATIVE_INFINITY
        };
        int projectedPoints = 0;

        for (int i = 0; i < corners.length; i++) {
            Vec3 relative = corners[i].subtract(camPos);
            Vector4f clip = new Vector4f(
                    (float) relative.x,
                    (float) relative.y,
                    (float) relative.z,
                    1.0f
            );
            matrix4fMul.transform(clip);
            clipCorners[i] = clip;
            if (clip.w > CAMERA_PLANE_EPSILON && includeProjectedPoint(clip, fw, fh, bounds)) {
                projectedPoints++;
            }
        }

        for (int[] edge : BOX_EDGES) {
            Vector4f from = clipCorners[edge[0]];
            Vector4f to = clipCorners[edge[1]];
            boolean fromInFront = from.w > CAMERA_PLANE_EPSILON;
            boolean toInFront = to.w > CAMERA_PLANE_EPSILON;
            if (fromInFront == toInFront) {
                continue;
            }

            float t = (CAMERA_PLANE_EPSILON - from.w) / (to.w - from.w);
            Vector4f clipped = new Vector4f(
                    from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t,
                    from.z + (to.z - from.z) * t,
                    CAMERA_PLANE_EPSILON
            );
            if (includeProjectedPoint(clipped, fw, fh, bounds)) {
                projectedPoints++;
            }
        }

        if (projectedPoints == 0) {
            return Optional.empty();
        }

        if (bounds[2] < 0.0f || bounds[0] > fw || bounds[3] < 0.0f || bounds[1] > fh) {
            return Optional.empty();
        }

        float minX = Math.max(0.0f, bounds[0]);
        float minY = Math.max(0.0f, bounds[1]);
        float maxX = Math.min(fw, bounds[2]);
        float maxY = Math.min(fh, bounds[3]);
        float width = Math.max(0.0f, maxX - minX);
        float height = Math.max(0.0f, maxY - minY);
        return (width == 0.0f && height == 0.0f)
                ? Optional.empty()
                : Optional.of(new Vector4f(minX, minY, width, height));
    }

    private static boolean includeProjectedPoint(
            Vector4f clip,
            float framebufferWidth,
            float framebufferHeight,
            float[] bounds
    ) {
        if (clip.w < CAMERA_PLANE_EPSILON
                || !Float.isFinite(clip.x)
                || !Float.isFinite(clip.y)
                || !Float.isFinite(clip.w)) {
            return false;
        }

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY)) {
            return false;
        }

        float screenX = (ndcX + 1.0f) * 0.5f * framebufferWidth;
        float screenY = (1.0f - ndcY) * 0.5f * framebufferHeight;
        bounds[0] = Math.min(bounds[0], screenX);
        bounds[1] = Math.min(bounds[1], screenY);
        bounds[2] = Math.max(bounds[2], screenX);
        bounds[3] = Math.max(bounds[3], screenY);
        return true;
    }

    public static float centerX(Vector4f vector4f) {
        return vector4f.x + (vector4f.z / 2.0f);
    }

    public ProjectionUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
