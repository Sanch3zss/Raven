package sanch3zss.raven.render.shape;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import sanch3zss.raven.client.Mc;
public class TextureQuadShape extends RenderShape {
    public Matrix4f matrix4f;
    public float x, y, width, height;
    public Identifier texture;
    public int topLeft, bottomLeft, bottomRight, topRight;
    public boolean z, z2, z3;
    private boolean worldBillboard;
    private double worldX, worldY, worldZ;

    public TextureQuadShape() {
        this.state = RenderShapeState.POSITION_COLOR_QUADS;
    }

    public TextureQuadShape set(Matrix4f matrix4f, float f, float f2, float f3, float f4, Identifier identifier, int i, boolean z, boolean z2) {
        return set(matrix4f, f, f2, f3, f4, identifier, i, z, z2, false);
    }

    public TextureQuadShape set(Matrix4f matrix4f, float f, float f2, float f3, float f4, Identifier identifier, int i, boolean z, boolean z2, boolean z3) {
        if (this.matrix4f == null) {
            this.matrix4f = new Matrix4f(matrix4f);
        } else {
            this.matrix4f.set(matrix4f);
        }
        this.x = f;
        this.y = f2;
        this.width = f3;
        this.height = f4;
        this.texture = identifier;
        this.worldBillboard = false;
        this.topLeft = i;
        this.bottomLeft = i;
        this.bottomRight = i;
        this.topRight = i;
        this.z = z;
        this.z2 = z2;
        this.z3 = z3;
        this.state = RenderShapeState.getTextureQuadState(identifier, z, z2, z3);
        return this;
    }

    public TextureQuadShape setWorldBillboard(double worldX, double worldY, double worldZ,
                                               float size, Identifier texture, int color,
                                               boolean depthTest, boolean blend, boolean additiveBlend) {
        set(new Matrix4f(), -size * 0.5f, -size * 0.5f, size, size,
                texture, color, depthTest, blend, additiveBlend);
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.worldBillboard = true;
        return this;
    }

    public TextureQuadShape setSolidMarker(Matrix4f matrix, float x, float y, float width, float height,
                                           Identifier texture, int topLeft, int bottomLeft,
                                           int bottomRight, int topRight) {
        set(matrix, x, y, width, height, texture, topLeft, bottomLeft, bottomRight, topRight,
                false, true, false);
        this.state = RenderShapeState.getSolidMarkerState(texture);
        return this;
    }

    public TextureQuadShape set(Matrix4f matrix4f, float f, float f2, float f3, float f4,
                                Identifier identifier, int topLeft, int bottomLeft,
                                int bottomRight, int topRight, boolean z, boolean z2, boolean z3) {
        set(matrix4f, f, f2, f3, f4, identifier, topLeft, z, z2, z3);
        this.topLeft = topLeft;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
        this.topRight = topRight;
        return this;
    }

    @Override
    public void emit(PoseStack matrixStack, BufferBuilder bufferBuilder) {
        Matrix4f pose = matrix4f;
        if (worldBillboard) {
            Camera camera = Mc.INSTANCE.getCamera();
            Vec3 cameraPosition = camera.position();
            pose = new Matrix4f(matrixStack.last().pose())
                    .translate((float) (worldX - cameraPosition.x()),
                            (float) (worldY - cameraPosition.y()),
                            (float) (worldZ - cameraPosition.z()))
                    .rotate(camera.rotation());
        }
        ShapeRenderer.INSTANCE.emitTextureQuad(pose, bufferBuilder, x, y, width, height,
                topLeft, bottomLeft, bottomRight, topRight);
    }
}
