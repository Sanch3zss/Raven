package sanch3zss.raven.ui.hud;

import java.util.*;

import org.joml.Matrix3x2fc;

public final class VanillaItemAlphaContext {
    private static final ThreadLocal<Float> ALPHA = ThreadLocal.withInitial(() -> 1.0f);
    private static final Map<Matrix3x2fc, Float> POSE_ALPHAS =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private VanillaItemAlphaContext() {
    }

    public static float currentAlpha() {
        return ALPHA.get();
    }

    public static void draw(float alpha, Runnable drawAction) {
        float previous = ALPHA.get();
        ALPHA.set(Math.max(0.0f, Math.min(1.0f, alpha)));
        try {
            drawAction.run();
        } finally {
            ALPHA.set(previous);
        }
    }

    public static void registerPose(Matrix3x2fc pose, float alpha) {
        if (pose != null) {
            POSE_ALPHAS.put(pose, Math.max(0.0f, Math.min(1.0f, alpha)));
        }
    }

    public static float alphaForPose(Matrix3x2fc pose) {
        Float alpha = POSE_ALPHAS.get(pose);
        return alpha == null ? 1.0f : alpha;
    }

    public static void clearPoses() {
        POSE_ALPHAS.clear();
    }
}
