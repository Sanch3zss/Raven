package sanch3zss.raven.feature.module.render.targetesp;


import sanch3zss.raven.render.shape.ImmediateRenderLayers;
import sanch3zss.raven.render.theme.ColorUtil;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.joml.Matrix4f;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.event.render.WorldRenderEvent;

public enum TargetJelloRenderer {
    INSTANCE;

    private static final int SEGMENTS = 72;

    public void render(WorldRenderEvent event, double x, double y, double z,
                       float entityWidth, float entityHeight, int firstColor, int secondColor) {
        if (ColorUtil.alpha(firstColor) <= 0 && ColorUtil.alpha(secondColor) <= 0) return;

        float time = (Mc.INSTANCE.getPlayer().tickCount + Mc.INSTANCE.getTickDelta()) / 4.0f;
        float halfHeight = entityHeight * 0.5f;
        float rimY = (float) y + halfHeight + (float) Math.sin(time) * halfHeight;
        float lowerY = rimY - (float) Math.cos(time) * 0.5f;
        Matrix4f matrix = event.matrixStack().last().pose();

        try (ByteBufferBuilder memory = new ByteBufferBuilder(SEGMENTS * 80)) {
            BufferBuilder buffer = new BufferBuilder(memory, PrimitiveTopology.QUADS,
                    DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < SEGMENTS; i++) {
                float angle = (float) (Math.PI * 2.0 * i / SEGMENTS);
                float nextAngle = i == SEGMENTS - 1 ? 0.0f
                        : (float) (Math.PI * 2.0 * (i + 1) / SEGMENTS);
                float x0 = (float) x + (float) Math.sin(angle) * entityWidth;
                float z0 = (float) z + (float) Math.cos(angle) * entityWidth;
                float x1 = (float) x + (float) Math.sin(nextAngle) * entityWidth;
                float z1 = (float) z + (float) Math.cos(nextAngle) * entityWidth;
                int colorA = ColorUtil.gradientWave(firstColor, secondColor, (float) i / SEGMENTS);
                int colorB = ColorUtil.gradientWave(firstColor, secondColor, (float) (i + 1) / SEGMENTS);
                buffer.addVertex(matrix, x1, lowerY, z1).setColor(ColorUtil.replAlpha(colorB, 0));
                buffer.addVertex(matrix, x0, lowerY, z0).setColor(ColorUtil.replAlpha(colorA, 0));
                buffer.addVertex(matrix, x0, rimY, z0).setColor(withAlpha(colorA, 120.0f / 255.0f));
                buffer.addVertex(matrix, x1, rimY, z1).setColor(withAlpha(colorB, 120.0f / 255.0f));
            }
            ImmediateRenderLayers.draw(buffer.buildOrThrow(), "target_jello_fill",
                    DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS,
                    null, true, true, false, false);
        }

        drawRingGlow(matrix, x, rimY, z, entityWidth, firstColor, secondColor, 0.52f);
    }

    private void drawRingGlow(Matrix4f matrix, double x, double y, double z, float radius,
                              int firstColor, int secondColor, float opacity) {
        try (ByteBufferBuilder memory = new ByteBufferBuilder(SEGMENTS * 80)) {
            BufferBuilder buffer = new BufferBuilder(memory, PrimitiveTopology.QUADS,
                    DefaultVertexFormat.POSITION_COLOR);
            float innerRadius = Math.max(0.0f, radius - 0.016f);
            for (int i = 0; i < SEGMENTS; i++) {
                float angle = (float) (Math.PI * 2.0 * i / SEGMENTS);
                float nextAngle = i == SEGMENTS - 1 ? 0.0f
                        : (float) (Math.PI * 2.0 * (i + 1) / SEGMENTS);
                float x0 = (float) x + (float) Math.sin(angle) * radius;
                float z0 = (float) z + (float) Math.cos(angle) * radius;
                float x1 = (float) x + (float) Math.sin(nextAngle) * radius;
                float z1 = (float) z + (float) Math.cos(nextAngle) * radius;
                float innerX0 = (float) x + (float) Math.sin(angle) * innerRadius;
                float innerZ0 = (float) z + (float) Math.cos(angle) * innerRadius;
                float innerX1 = (float) x + (float) Math.sin(nextAngle) * innerRadius;
                float innerZ1 = (float) z + (float) Math.cos(nextAngle) * innerRadius;
                int colorA = withAlpha(ColorUtil.gradientWave(firstColor, secondColor, (float) i / SEGMENTS), opacity);
                int colorB = withAlpha(ColorUtil.gradientWave(firstColor, secondColor, (float) (i + 1) / SEGMENTS), opacity);
                buffer.addVertex(matrix, x0, (float) y, z0).setColor(colorA);
                buffer.addVertex(matrix, innerX0, (float) y, innerZ0).setColor(colorA);
                buffer.addVertex(matrix, innerX1, (float) y, innerZ1).setColor(colorB);
                buffer.addVertex(matrix, x1, (float) y, z1).setColor(colorB);
            }
            ImmediateRenderLayers.draw(buffer.buildOrThrow(), "target_jello_rim",
                    DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS,
                    null, true, true, false, false);
        }
    }

    private static int withAlpha(int color, float opacity) {
        return ColorUtil.replAlpha(color, Math.round(ColorUtil.alpha(color) * opacity));
    }
}
