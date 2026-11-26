package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class HeldTuning {
    static final HeldTuning SHIELD = new HeldTuning(
            new float[]{-0.09f, -0.13f, 0.55f, -9.0f, 180.0f, 91.0f, 1.0f},
            new float[]{-0.41f, 0.02f, -0.01f, 6.0f, 34.0f, 10.0f, 0.0f});
    static final HeldTuning TRIDENT = new HeldTuning(
            new float[]{0.0f, -0.17f, -0.09f, -46.0f, 12.0f, 45.0f, 0.8f},
            new float[]{0.29f, 0.95f, 0.05f, 79.0f, 0.0f, 0.0f, 0.03f});

    private final float[] idle;
    private final float[] use;

    public HeldTuning(float[] idle, float[] use) {
        this.idle = idle;
        this.use = use;
    }

    Matrix4f useMotion(int side, float weight, float charge, Vector3f grip) {
        PoseStack pose = new PoseStack();
        pose.translate(side * use[0] * weight, use[1] * weight, use[2] * weight + use[6] * charge);
        pose.rotateAround(Axis.ZP.rotationDegrees(side * use[5] * weight), grip.x(), grip.y(), grip.z());
        pose.rotateAround(Axis.YP.rotationDegrees(side * use[4] * weight), grip.x(), grip.y(), grip.z());
        pose.rotateAround(Axis.XP.rotationDegrees(use[3] * weight), grip.x(), grip.y(), grip.z());
        return new Matrix4f(pose.last().pose());
    }

    void applyIdle(PoseStack pose, int side) {
        pose.translate(side * idle[0], idle[1], idle[2]);
        pose.mulPose(Axis.ZP.rotationDegrees(side * idle[5]));
        pose.mulPose(Axis.YP.rotationDegrees(side * idle[4]));
        pose.mulPose(Axis.XP.rotationDegrees(idle[3]));
        pose.scale(idle[6], idle[6], idle[6]);
    }
}
