package sanch3zss.raven.feature.module.misc;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.render.shape.ShapeRenderer;

final class CustomItemRangeRenderer {
    private static final double INSET = 0.01;
    private static final int CIRCLE_SEGMENTS = 64;

    private CustomItemRangeRenderer() {
    }

    static boolean held(LocalPlayer player, Item item) {
        return player.getMainHandItem().is(item) || player.getOffhandItem().is(item);
    }

    static AABB box(BlockPos center, int minX, int minY, int minZ,
                    int maxX, int maxY, int maxZ) {
        return new AABB(center.getX() + minX + INSET,
                center.getY() + minY + INSET,
                center.getZ() + minZ + INSET,
                center.getX() + maxX - INSET,
                center.getY() + maxY - INSET,
                center.getZ() + maxZ - INSET);
    }

    static void box(WorldRenderEvent event, AABB box, int color) {
        ShapeRenderer shapes = ShapeRenderer.INSTANCE;
        Matrix4f matrix = event.matrixStack().last().pose();
        int fill = ColorUtil.replAlpha(color, Math.min(35, ColorUtil.alpha(color)));
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        shapes.addDepthTestedQuad(matrix, new Vec3(x1, y1, z1), new Vec3(x2, y1, z1),
                new Vec3(x2, y1, z2), new Vec3(x1, y1, z2), fill);
        shapes.addDepthTestedQuad(matrix, new Vec3(x1, y2, z1), new Vec3(x2, y2, z1),
                new Vec3(x2, y2, z2), new Vec3(x1, y2, z2), fill);
        shapes.addDepthTestedQuad(matrix, new Vec3(x1, y1, z1), new Vec3(x2, y1, z1),
                new Vec3(x2, y2, z1), new Vec3(x1, y2, z1), fill);
        shapes.addDepthTestedQuad(matrix, new Vec3(x1, y1, z2), new Vec3(x2, y1, z2),
                new Vec3(x2, y2, z2), new Vec3(x1, y2, z2), fill);
        shapes.addDepthTestedQuad(matrix, new Vec3(x1, y1, z1), new Vec3(x1, y1, z2),
                new Vec3(x1, y2, z2), new Vec3(x1, y2, z1), fill);
        shapes.addDepthTestedQuad(matrix, new Vec3(x2, y1, z1), new Vec3(x2, y1, z2),
                new Vec3(x2, y2, z2), new Vec3(x2, y2, z1), fill);

        outlineBox(matrix, box, color);
    }

    static void outlineBox(WorldRenderEvent event, AABB box, int color) {
        outlineBox(event.matrixStack().last().pose(), box, color);
    }

    private static void outlineBox(Matrix4f matrix, AABB box, int color) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        for (double y : new double[]{y1, y2}) {
            line(matrix, new Vec3(x1, y, z1), new Vec3(x2, y, z1), color);
            line(matrix, new Vec3(x2, y, z1), new Vec3(x2, y, z2), color);
            line(matrix, new Vec3(x2, y, z2), new Vec3(x1, y, z2), color);
            line(matrix, new Vec3(x1, y, z2), new Vec3(x1, y, z1), color);
        }
        for (double x : new double[]{x1, x2}) {
            for (double z : new double[]{z1, z2}) {
                line(matrix, new Vec3(x, y1, z), new Vec3(x, y2, z), color);
            }
        }
    }

    static void circle(WorldRenderEvent event, Vec3 center, double radius, int color) {
        Matrix4f matrix = event.matrixStack().last().pose();
        for (int index = 0; index < CIRCLE_SEGMENTS; index++) {
            double first = Math.PI * 2.0 * index / CIRCLE_SEGMENTS;
            double second = Math.PI * 2.0 * (index + 1) / CIRCLE_SEGMENTS;
            line(matrix,
                    center.add(Math.cos(first) * radius, 0.05, Math.sin(first) * radius),
                    center.add(Math.cos(second) * radius, 0.05, Math.sin(second) * radius),
                    color);
        }
    }

    private static void line(Matrix4f matrix, Vec3 first, Vec3 second, int color) {
        ShapeRenderer.INSTANCE.addDepthTestedLine(matrix, first, second, color, 2.0f);
    }
}
