package sanch3zss.raven.feature.module.render.armtweaks.knife;

final class ConsumableMotion {
    private ConsumableMotion() {}

    static Pose sample(float elapsedSeconds, float blend, boolean drinking) {
        float t = Math.max(0.0f, elapsedSeconds);
        float weight = Math.max(0.0f, Math.min(1.0f, blend));
        weight = weight * weight * (3.0f - 2.0f * weight);
        float settle = smooth(t / 0.45f);
        float x;
        float y;
        float z;
        float pitch;
        float yaw;
        float roll;
        if (drinking) {
            float tip = Math.min(1.0f, t / 1.2f);
            tip = tip * tip * (3.0f - 2.0f * tip);
            float sway = (float) Math.sin(t * Math.PI * 1.6) * 0.25f * settle;
            x = -2.4f;
            y = 4.6f + 0.6f * tip + sway;
            z = 3.2f;
            pitch = -(38.0f + 22.0f * tip);
            yaw = -6.0f;
            roll = -6.0f;
        } else {
            float bob = (0.5f - 0.5f * (float) Math.cos(t * Math.PI * 5.0)) * settle;
            x = -2.0f;
            y = 4.0f + 0.9f * bob;
            z = 3.0f;
            pitch = -(36.0f + 4.0f * bob);
            yaw = -5.0f;
            roll = -8.0f + 2.0f * bob;
        }
        return new Pose(x * weight, y * weight, z * weight, pitch * weight, yaw * weight, roll * weight);
    }

    static Pose brush(float elapsedSeconds, float blend) {
        float weight = smooth(blend);
        float wave = (float) Math.sin(Math.max(0.0f, elapsedSeconds) * Math.PI * 3.2);
        return new Pose((-1.5f + 0.8f * wave) * weight, 1.5f * weight, 1.0f * weight,
                -12.0f * weight, (-8.0f + 14.0f * wave) * weight, -6.0f * weight);
    }

    static Pose horn(float elapsedSeconds, float blend) {
        float weight = smooth(blend);
        float t = Math.max(0.0f, elapsedSeconds);
        float tremor = (float) Math.sin(t * 40.0f) * 0.12f * Math.min(1.0f, t / 0.4f);
        return new Pose(-2.6f * weight, (4.3f + tremor) * weight, 3.0f * weight,
                -24.0f * weight, -10.0f * weight, -6.0f * weight);
    }

    private static float smooth(float value) {
        float clamped = Math.max(0.0f, Math.min(1.0f, value));
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    record Pose(float x, float y, float z, float pitch, float yaw, float roll) {}
}
