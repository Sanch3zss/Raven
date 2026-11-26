package sanch3zss.raven.render.shape;


import sanch3zss.raven.render.theme.ColorUtil;
import java.util.*;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import sanch3zss.raven.client.*;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.render.shape.*;

public enum ShapeRenderer {
    INSTANCE;

    public final List<RenderShape> renderQueue = new ArrayList();
    public final List<LineRenderShape> linePool = new ArrayList();
    public final List<BoxRenderShape> boxPool = new ArrayList();
    public final List<BoxOutlineShape> outlinePool = new ArrayList();
    public final List<TextureQuadShape> textureQuadPool = new ArrayList();
    public final List<HemisphereShape> hemispherePool = new ArrayList();
    public final List<QuadRenderShape> quadPool = new ArrayList();
    public final List<TriangleShape> trianglePool = new ArrayList();
    public int triangleCount;
    public int lineCount;
    public int boxCount;
    public int outlineCount;
    public int textureQuadCount;
    public int hemisphereCount;
    public int quadCount;

    ShapeRenderer() {
        Raven.INSTANCE.eventDispatcher().register(WorldRenderEvent.class, event -> {
            try {
                if (this.renderQueue.isEmpty() || !Mc.INSTANCE.isWorldLoaded()) {
                    return;
                }
                PoseStack matrixStack = event.matrixStack();
                Map<RenderShapeState, List<RenderShape>> linkedHashMap = new LinkedHashMap<>();
                for (RenderShape renderShape : this.renderQueue) {
                    if (renderShape == null || renderShape.state() == null) continue;
                    linkedHashMap.computeIfAbsent(renderShape.state(), k -> new ArrayList<>()).add(renderShape);
                }
                ByteBufferBuilder byteBufferBuilder = new ByteBufferBuilder(2048);
                try {
                    for (Map.Entry<RenderShapeState, List<RenderShape>> entry : linkedHashMap.entrySet()) {
                        RenderShapeState renderShapeState = entry.getKey();
                        if (renderShapeState == null || renderShapeState.mode() == null || renderShapeState.format() == null) continue;
                        BufferBuilder bufferBuilderBegin = new BufferBuilder(byteBufferBuilder, renderShapeState.mode(), renderShapeState.format());
                        try {
                            for (RenderShape shape : entry.getValue()) {
                                if (shape != null) {
                                    try {
                                        shape.emit(matrixStack, bufferBuilderBegin);
                                    } catch (Throwable ignoredInner) {
                                    }
                                }
                            }
                        } finally {
                            try {
                                ImmediateRenderLayers.draw(renderShapeState.layer(), bufferBuilderBegin.buildOrThrow());
                            } catch (Throwable ignoredEnd) {
                            }
                        }
                    }
                } finally {
                    byteBufferBuilder.close();
                }
            } catch (Throwable ignored) {
            } finally {
                this.renderQueue.clear();
                resetCounters();
            }
        });
    }

    public void resetCounters() {
        this.lineCount = 0;
        this.boxCount = 0;
        this.outlineCount = 0;
        this.textureQuadCount = 0;
        this.quadCount = 0;
        this.hemisphereCount = 0;
        this.triangleCount = 0;
    }

    public TriangleShape nextTriangle() {
        if (this.triangleCount >= this.trianglePool.size()) {
            this.trianglePool.add(new TriangleShape());
        }
        List<TriangleShape> list = this.trianglePool;
        int i = this.triangleCount;
        this.triangleCount = i + 1;
        return list.get(i);
    }

    public LineRenderShape nextLine() {
        if (this.lineCount >= this.linePool.size()) {
            this.linePool.add(new LineRenderShape());
        }
        List<LineRenderShape> list = this.linePool;
        int i = this.lineCount;
        this.lineCount = i + 1;
        return list.get(i);
    }

    public QuadRenderShape nextQuad() {
        if (this.quadCount >= this.quadPool.size()) {
            this.quadPool.add(new QuadRenderShape());
        }
        List<QuadRenderShape> list = this.quadPool;
        int i = this.quadCount;
        this.quadCount = i + 1;
        return list.get(i);
    }

    public BoxRenderShape nextBox() {
        if (this.boxCount >= this.boxPool.size()) {
            this.boxPool.add(new BoxRenderShape());
        }
        List<BoxRenderShape> list = this.boxPool;
        int i = this.boxCount;
        this.boxCount = i + 1;
        return list.get(i);
    }

    public BoxOutlineShape nextOutline() {
        if (this.outlineCount >= this.outlinePool.size()) {
            this.outlinePool.add(new BoxOutlineShape());
        }
        List<BoxOutlineShape> list = this.outlinePool;
        int i = this.outlineCount;
        this.outlineCount = i + 1;
        return list.get(i);
    }

    public TextureQuadShape nextTextureQuad() {
        if (this.textureQuadCount >= this.textureQuadPool.size()) {
            this.textureQuadPool.add(new TextureQuadShape());
        }
        List<TextureQuadShape> list = this.textureQuadPool;
        int i = this.textureQuadCount;
        this.textureQuadCount = i + 1;
        return list.get(i);
    }

    public HemisphereShape nextHemisphere() {
        if (this.hemisphereCount >= this.hemispherePool.size()) {
            this.hemispherePool.add(new HemisphereShape());
        }
        List<HemisphereShape> list = this.hemispherePool;
        int i = this.hemisphereCount;
        this.hemisphereCount = i + 1;
        return list.get(i);
    }

    public void addTriangle(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3, int i) {
        addTriangle(matrix4f, vec3d, vec3d2, vec3d3, i, false);
    }

    public void addTriangle(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3, int i, boolean z) {
        this.renderQueue.add(nextTriangle().set(matrix4f, vec3d, vec3d2, vec3d3, i, z));
    }

    public void addFilledQuad(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3, Vec3 vec3d4, int i) {
        addFilledQuad(matrix4f, vec3d, vec3d2, vec3d3, vec3d4, i, false);
    }

    public void addFilledQuad(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3, Vec3 vec3d4, int i, boolean z) {
        this.renderQueue.add(nextQuad().set(matrix4f, vec3d, vec3d2, vec3d3, vec3d4, i, z));
    }

    public void addDepthTestedQuad(Matrix4f matrix4f, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, int color) {
        this.renderQueue.add(nextQuad().set(matrix4f, v1, v2, v3, v4, color, true).depthTested());
    }

    public void addLine(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, int i, float f) {
        addLine(matrix4f, vec3d, vec3d2, i, f, true);
    }

    public void addBox(Matrix4f matrix4f, AABB box, int i) {
        addBox(matrix4f, box, i, true);
    }

    public void addOutlineWireframe(Matrix4f matrix4f, AABB box, int i, int i2, float f) {
        addOutlineWireframe(matrix4f, box, i, i2, f, true);
    }

    public void addOutline(Matrix4f matrix4f, AABB box, int i, float f) {
        addOutline(matrix4f, box, i, f, true);
    }

    public void addHemisphere(Matrix4f matrix4f, Vec3 vec3d, float f, int i, int i2) {
        addHemisphere(matrix4f, vec3d, f, i, i2, true);
    }

    public void addWireframe(Matrix4f matrix4f, AABB box, int i, float f) {
        addWireframe(matrix4f, box, i, f, true);
    }

    public void addLine(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, int i, float f, boolean z) {
        this.renderQueue.add(nextLine().set(matrix4f, vec3d, vec3d2, i, f, z).withoutDepthTest());
    }

    public void addDepthTestedLine(Matrix4f matrix4f, Vec3 start, Vec3 end, int color, float width) {
        this.renderQueue.add(nextLine().set(matrix4f, start, end, color, width, true).depthTested());
    }

    public void addBox(Matrix4f matrix4f, AABB box, int i, boolean z) {
        this.renderQueue.add(nextBox().set(matrix4f, box, i, false, true, z));
    }

    public void addCornerBox(Matrix4f matrix4f, AABB box, int color, float lineWidth, boolean cameraRelative) {
        if (box == null) return;
        double minX = box.minX;
        double maxX = box.maxX;
        double minY = box.minY;
        double maxY = box.maxY;
        double minZ = box.minZ;
        double maxZ = box.maxZ;

        double lenX = (maxX - minX) * 0.25d;
        double lenY = (maxY - minY) * 0.25d;
        double lenZ = (maxZ - minZ) * 0.25d;

        double[] xVals = {minX, maxX};
        double[] yVals = {minY, maxY};
        double[] zVals = {minZ, maxZ};

        for (double x : xVals) {
            double dx = (x == minX) ? lenX : -lenX;
            for (double y : yVals) {
                double dy = (y == minY) ? lenY : -lenY;
                for (double z : zVals) {
                    double dz = (z == minZ) ? lenZ : -lenZ;
                    Vec3 corner = new Vec3(x, y, z);
                    addLine(matrix4f, corner, new Vec3(x + dx, y, z), color, lineWidth, cameraRelative);
                    addLine(matrix4f, corner, new Vec3(x, y + dy, z), color, lineWidth, cameraRelative);
                    addLine(matrix4f, corner, new Vec3(x, y, z + dz), color, lineWidth, cameraRelative);
                }
            }
        }
    }

    public void addOutlineWireframe(Matrix4f matrix4f, AABB box, int i, int i2, float f, boolean z) {
        this.renderQueue.add(nextOutline().set(matrix4f, box, i, i2, true, true, f, z));
    }

    public void addWireframe(Matrix4f matrix4f, AABB box, int i, float f, boolean z) {
        this.renderQueue.add(nextOutline().set(matrix4f, box, -1, i, false, true, f, z));
    }

    public void addOutline(Matrix4f matrix4f, AABB box, int i, float f, boolean z) {
        this.renderQueue.add(nextOutline().set(matrix4f, box, i, -1, true, false, f, z));
    }

    public void addTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, Identifier identifier, int i, boolean z, boolean z2) {
        this.renderQueue.add(nextTextureQuad().set(matrix4f, f, f2, f3, f4, identifier, i, z, z2));
    }

    public void addTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, Identifier identifier, int i, boolean z, boolean z2, boolean z3) {
        this.renderQueue.add(nextTextureQuad().set(matrix4f, f, f2, f3, f4, identifier, i, z, z2, z3));
    }

    public void addTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4,
                           Identifier identifier, int topLeft, int bottomLeft, int bottomRight,
                           int topRight, boolean z, boolean z2, boolean z3) {
        this.renderQueue.add(nextTextureQuad().set(matrix4f, f, f2, f3, f4, identifier,
                topLeft, bottomLeft, bottomRight, topRight, z, z2, z3));
    }

    public void addSolidMarker(Matrix4f matrix, float x, float y, float width, float height,
                               Identifier texture, int topLeft, int bottomLeft,
                               int bottomRight, int topRight) {
        this.renderQueue.add(nextTextureQuad().setSolidMarker(matrix, x, y, width, height,
                texture, topLeft, bottomLeft, bottomRight, topRight));
    }

    public void addWorldBillboard(double x, double y, double z, float size,
                                  Identifier texture, int color, boolean depthTest,
                                  boolean blend, boolean additiveBlend) {
        this.renderQueue.add(nextTextureQuad().setWorldBillboard(x, y, z, size,
                texture, color, depthTest, blend, additiveBlend));
    }

    public void addHemisphere(Matrix4f matrix4f, Vec3 vec3d, float f, int i, int i2, boolean z) {
        this.renderQueue.add(nextHemisphere().set(matrix4f, vec3d, f, i, i2, z));
    }

    public void drawTriangle(Matrix4f matrix4f, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3, int i) {
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(512);
        BufferBuilder bufferBuilderBegin = new BufferBuilder(byteBuffer, PrimitiveTopology.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        float fRed = ColorUtil.red(i) / 255.0f;
        float fGreen = ColorUtil.green(i) / 255.0f;
        float fBlue = ColorUtil.blue(i) / 255.0f;
        float fAlpha = ColorUtil.alpha(i) / 255.0f;
        bufferBuilderBegin.addVertex(matrix4f, (float) vec3d.x, (float) vec3d.y, (float) vec3d.z).setColor(fRed, fGreen, fBlue, fAlpha);
        bufferBuilderBegin.addVertex(matrix4f, (float) vec3d2.x, (float) vec3d2.y, (float) vec3d2.z).setColor(fRed, fGreen, fBlue, fAlpha);
        bufferBuilderBegin.addVertex(matrix4f, (float) vec3d3.x, (float) vec3d3.y, (float) vec3d3.z).setColor(fRed, fGreen, fBlue, fAlpha);
        ImmediateRenderLayers.draw(RenderShapeState.POSITION_COLOR_TRIANGLES.layer(), bufferBuilderBegin.buildOrThrow());
        byteBuffer.close();
    }

    public void emitTriangle(Matrix4f matrix4f, BufferBuilder bufferBuilder, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3, int i, boolean z) {
        double d = 0.0d;
        double d2 = 0.0d;
        double d3 = 0.0d;
        if (z) {
            Vec3 pos = Mc.INSTANCE.getCamera().position();
            d = pos.x;
            d2 = pos.y;
            d3 = pos.z;
        }
        float f = (float) (vec3d.x - d);
        float f2 = (float) (vec3d.y - d2);
        float f3 = (float) (vec3d.z - d3);
        float f4 = (float) (vec3d2.x - d);
        float f5 = (float) (vec3d2.y - d2);
        float f6 = (float) (vec3d2.z - d3);
        float f7 = (float) (vec3d3.x - d);
        float f8 = (float) (vec3d3.y - d2);
        float f9 = (float) (vec3d3.z - d3);
        bufferBuilder.addVertex(matrix4f, f, f2, f3).setColor(i);
        bufferBuilder.addVertex(matrix4f, f4, f5, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f7, f8, f9).setColor(i);
    }

    public void emitQuad(Matrix4f matrix4f, BufferBuilder bufferBuilder, Vec3 vec3d, Vec3 vec3d2, Vec3 vec3d3, Vec3 vec3d4, int i, boolean z) {
        double d = 0.0d;
        double d2 = 0.0d;
        double d3 = 0.0d;
        if (z) {
            Vec3 pos = Mc.INSTANCE.getCamera().position();
            d = pos.x;
            d2 = pos.y;
            d3 = pos.z;
        }
        float f = (float) (vec3d.x - d);
        float f2 = (float) (vec3d.y - d2);
        float f3 = (float) (vec3d.z - d3);
        float f4 = (float) (vec3d2.x - d);
        float f5 = (float) (vec3d2.y - d2);
        float f6 = (float) (vec3d2.z - d3);
        float f7 = (float) (vec3d3.x - d);
        float f8 = (float) (vec3d3.y - d2);
        float f9 = (float) (vec3d3.z - d3);
        float f10 = (float) (vec3d4.x - d);
        float f11 = (float) (vec3d4.y - d2);
        float f12 = (float) (vec3d4.z - d3);
        bufferBuilder.addVertex(matrix4f, f, f2, f3).setColor(i);
        bufferBuilder.addVertex(matrix4f, f4, f5, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f7, f8, f9).setColor(i);
        bufferBuilder.addVertex(matrix4f, f10, f11, f12).setColor(i);
    }

    public void emitLine(Matrix4f matrix4f, BufferBuilder bufferBuilder,
                         Vec3 vec3d, Vec3 vec3d2, int i,
                         float width, boolean z) {
        Vec3 vec3dSubtract = vec3d;
        Vec3 vec3dSubtract2 = vec3d2;
        if (z) {
            Vec3 pos = Mc.INSTANCE.getCamera().position();
            vec3dSubtract = vec3d.subtract(pos);
            vec3dSubtract2 = vec3d2.subtract(pos);
        }
        Vec3 vec3dSubtract3 = vec3dSubtract2.subtract(vec3dSubtract);
        double length = vec3dSubtract3.length();
        if (length < 1.0E-6d) {
            return;
        }
        Vec3 vec3dMultiply = vec3dSubtract3.scale(1.0d / length);
        bufferBuilder.addVertex(matrix4f, (float) vec3dSubtract.x,
                        (float) vec3dSubtract.y, (float) vec3dSubtract.z)
                .setColor(ColorUtil.red(i), ColorUtil.green(i),
                        ColorUtil.blue(i), ColorUtil.alpha(i))
                .setNormal((float) vec3dMultiply.x, (float) vec3dMultiply.y,
                        (float) vec3dMultiply.z)
                .setLineWidth(width);
        bufferBuilder.addVertex(matrix4f, (float) vec3dSubtract2.x,
                        (float) vec3dSubtract2.y, (float) vec3dSubtract2.z)
                .setColor(ColorUtil.red(i), ColorUtil.green(i),
                        ColorUtil.blue(i), ColorUtil.alpha(i))
                .setNormal((float) vec3dMultiply.x, (float) vec3dMultiply.y,
                        (float) vec3dMultiply.z)
                .setLineWidth(width);
    }

    public void emitBox(Matrix4f matrix4f, BufferBuilder bufferBuilder, AABB box, int i, boolean z) {
        double d = 0.0d;
        double d2 = 0.0d;
        double d3 = 0.0d;
        if (z) {
            Vec3 pos = Mc.INSTANCE.getCamera().position();
            d = pos.x;
            d2 = pos.y;
            d3 = pos.z;
        }
        float f = (float) (box.minX - d);
        float f2 = (float) (box.maxX - d);
        float f3 = (float) (box.minY - d2);
        float f4 = (float) (box.maxY - d2);
        float f5 = (float) (box.minZ - d3);
        float f6 = (float) (box.maxZ - d3);
        bufferBuilder.addVertex(matrix4f, f, f3, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f3, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f3, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f3, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f4, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f4, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f4, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f4, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f3, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f4, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f4, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f3, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f3, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f4, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f4, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f3, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f3, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f4, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f4, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f, f3, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f3, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f4, f5).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f4, f6).setColor(i);
        bufferBuilder.addVertex(matrix4f, f2, f3, f6).setColor(i);
    }

    public void emitTextureQuad(Matrix4f matrix4f, BufferBuilder bufferBuilder, float f, float f2, float f3, float f4, int i) {
        emitTextureQuad(matrix4f, bufferBuilder, f, f2, f3, f4, i, i, i, i);
    }

    public void emitTextureQuad(Matrix4f matrix4f, BufferBuilder bufferBuilder, float f, float f2,
                                float f3, float f4, int topLeft, int bottomLeft,
                                int bottomRight, int topRight) {
        bufferBuilder.addVertex(matrix4f, f, f2, 0.0f).setUv(0.0f, 0.0f).setColor(topLeft);
        bufferBuilder.addVertex(matrix4f, f, f2 + f4, 0.0f).setUv(0.0f, 1.0f).setColor(bottomLeft);
        bufferBuilder.addVertex(matrix4f, f + f3, f2 + f4, 0.0f).setUv(1.0f, 1.0f).setColor(bottomRight);
        bufferBuilder.addVertex(matrix4f, f + f3, f2, 0.0f).setUv(1.0f, 0.0f).setColor(topRight);
    }

    public void emitBoxOutline(Matrix4f matrix4f, BufferBuilder bufferBuilder,
                               AABB box, int i, int i2, boolean z, boolean z2,
                               float width, boolean z3) {
        if (z || z2) {
            float f = (float) box.minX;
            float f2 = (float) box.maxX;
            float f3 = (float) box.minY;
            float f4 = (float) box.maxY;
            float f5 = (float) box.minZ;
            float f6 = (float) box.maxZ;
            if (z) {
                emitLineSegment(matrix4f, bufferBuilder, f, f3, f5, f2, f3, f5, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f2, f3, f5, f2, f3, f6, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f2, f3, f6, f, f3, f6, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f3, f6, f, f3, f5, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f4, f5, f2, f4, f5, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f2, f4, f5, f2, f4, f6, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f2, f4, f6, f, f4, f6, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f4, f6, f, f4, f5, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f3, f5, f, f4, f5, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f2, f3, f5, f2, f4, f5, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f2, f3, f6, f2, f4, f6, i, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f3, f6, f, f4, f6, i, width, z3);
            }
            if (z2) {
                emitLineSegment(matrix4f, bufferBuilder, f, f3, f5, f2, f3, f6, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f3, f6, f2, f3, f5, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f4, f5, f2, f4, f6, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f4, f6, f2, f4, f5, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f4, f6, f2, f3, f6, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f3, f6, f2, f4, f6, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f2, f3, f5, f, f4, f5, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f3, f5, f2, f4, f5, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f3, f5, f, f4, f6, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f, f4, f5, f, f3, f6, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f2, f3, f5, f2, f4, f6, i2, width, z3);
                emitLineSegment(matrix4f, bufferBuilder, f2, f3, f6, f2, f4, f5, i2, width, z3);
            }
        }
    }

    public void emitHemisphere(Matrix4f matrix4f, BufferBuilder bufferBuilder, Vec3 vec3d, float f, int i, int i2, boolean z) {
        double d = 0.0d;
        double d2 = 0.0d;
        double d3 = 0.0d;
        if (z) {
            Vec3 pos = Mc.INSTANCE.getCamera().position();
            d = pos.x;
            d2 = pos.y;
            d3 = pos.z;
        }
        int iMax = Math.max(24, i);
        int iMax2 = Math.max(12, i / 2);
        float f2 = (float) (vec3d.x - d);
        float f3 = (float) (vec3d.y - d2);
        float f4 = (float) (vec3d.z - d3);
        for (int i3 = 0; i3 < iMax2; i3++) {
            float f5 = i3 / iMax2;
            float f6 = (i3 + 1) / iMax2;
            float f7 = f5 * 1.5707964f;
            float f8 = f6 * 1.5707964f;
            float fSin = Mth.sin(f7);
            float fCos = Mth.cos(f7);
            float fSin2 = Mth.sin(f8);
            float fCos2 = Mth.cos(f8);
            for (int i4 = 0; i4 < iMax; i4++) {
                float f9 = i4 / iMax;
                float f10 = (i4 + 1) / iMax;
                float f11 = f9 * 6.2831855f;
                float f12 = f10 * 6.2831855f;
                float fCos3 = Mth.cos(f11);
                float fSin3 = Mth.sin(f11);
                float fCos4 = Mth.cos(f12);
                float fSin4 = Mth.sin(f12);
                float f13 = f2 + (f * fSin2 * fCos3);
                float f14 = f3 + (f * fCos2);
                float f15 = f4 + (f * fSin2 * fSin3);
                float f16 = f2 + (f * fSin2 * fCos4);
                float f17 = f3 + (f * fCos2);
                float f18 = f4 + (f * fSin2 * fSin4);
                float f19 = f2 + (f * fSin * fCos3);
                float f20 = f3 + (f * fCos);
                float f21 = f4 + (f * fSin * fSin3);
                bufferBuilder.addVertex(matrix4f, f13, f14, f15).setColor(i2);
                bufferBuilder.addVertex(matrix4f, f19, f20, f21).setColor(i2);
                bufferBuilder.addVertex(matrix4f, f16, f17, f18).setColor(i2);
                bufferBuilder.addVertex(matrix4f, f16, f17, f18).setColor(i2);
                bufferBuilder.addVertex(matrix4f, f19, f20, f21).setColor(i2);
                bufferBuilder.addVertex(matrix4f, f2 + (f * fSin * fCos4), f3 + (f * fCos), f4 + (f * fSin * fSin4)).setColor(i2);
            }
        }
    }

    public void emitLineSegment(Matrix4f matrix4f,
                                BufferBuilder bufferBuilder,
                                float f, float f2, float f3,
                                float f4, float f5, float f6,
                                int i, float width, boolean z) {
        float f7 = f;
        float f8 = f2;
        float f9 = f3;
        float f10 = f4;
        float f11 = f5;
        float f12 = f6;
        if (z) {
            Vec3 pos = Mc.INSTANCE.getCamera().position();
            float f13 = (float) pos.x;
            float f14 = (float) pos.y;
            float f15 = (float) pos.z;
            f7 -= f13;
            f8 -= f14;
            f9 -= f15;
            f10 -= f13;
            f11 -= f14;
            f12 -= f15;
        }
        float f16 = f10 - f7;
        float f17 = f11 - f8;
        float f18 = f12 - f9;
        double dSqrt = Math.sqrt((f16 * f16) + (f17 * f17) + (f18 * f18));
        if (dSqrt < 1.0E-6d) {
            return;
        }
        float f19 = (float) (1.0d / dSqrt);
        float f20 = f16 * f19;
        float f21 = f17 * f19;
        float f22 = f18 * f19;
        bufferBuilder.addVertex(matrix4f, f7, f8, f9)
                .setColor(ColorUtil.red(i), ColorUtil.green(i),
                        ColorUtil.blue(i), ColorUtil.alpha(i))
                .setNormal(f20, f21, f22)
                .setLineWidth(width);
        bufferBuilder.addVertex(matrix4f, f10, f11, f12)
                .setColor(ColorUtil.red(i), ColorUtil.green(i),
                        ColorUtil.blue(i), ColorUtil.alpha(i))
                .setNormal(f20, f21, f22)
                .setLineWidth(width);
    }
}
