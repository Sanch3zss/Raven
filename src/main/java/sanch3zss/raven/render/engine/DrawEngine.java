package sanch3zss.raven.render.engine;


import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.render.gl.GlStateSnapshot;
import sanch3zss.raven.render.gl.StencilBufferUtil;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.platform.memory.NativeMemoryAccessor;
import sanch3zss.raven.client.platform.nativebridge.NativeMemoryBuffer;
import sanch3zss.raven.render.texture.ItemSpriteManager;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.shader.*;
import sanch3zss.raven.render.shape.ScissorStack;
import sanch3zss.raven.render.texture.*;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.window.WindowUtil;
import sanch3zss.raven.util.math.MathUtil;

public class DrawEngine {

    public final NativeMemoryBuffer vertexBuffer;
    public final NativeMemoryBuffer indexBuffer;

    public final ShaderProgram shaderProgram;

    public final ShaderUniform orthoMatrixUniform;

    public final ShaderUniform textureSamplerUniform;
    public final ShaderUniform resolutionUniform;
    public final ShaderUniform squircleUniform;
    public final ShaderUniform alphaClipUniform;
    private boolean squircleCorners = true;
    private boolean stencilWriting;

    public void squircleCorners(boolean enabled) {
        if (this.squircleCorners == enabled) return;
        draw();
        this.squircleCorners = enabled;
    }

    public final TextureSlotAllocator textureSlotAllocator;

    public final ColorStack colorStack;
    public final int vertexArrayId;
    public final int vertexBufferId;
    public final int indexBufferId;
    public int savedActiveTexture;
    public int savedTextureBinding;
    public int savedFramebuffer;
    public int indexCount;
    public int vertexCount;

    public GlStateSnapshot stateSnapshot;

    public boolean building;

    public boolean usingStencil;
    public final int maxTextureUnits;
    public final Vector4f tempVector = new Vector4f();

    public final ScissorStack scissorStack = new ScissorStack();
    public float contentScale = 1.0f;
    public int drawCallCount = 0;
    public final ScissorBounds lastBounds = new ScissorBounds();
    public Runnable blendSwitchCallback = null;

    public DrawEngine(ShaderProgram shaderProgram, int i) {
        this.maxTextureUnits = Math.min(32, i);
        NativeMemoryAccessor nativeMemoryAccessor = new NativeMemoryAccessor();
        this.vertexBuffer = new NativeMemoryBuffer(nativeMemoryAccessor, 8388608L);
        this.indexBuffer = new NativeMemoryBuffer(nativeMemoryAccessor, 8388608L);
        this.shaderProgram = shaderProgram;
        this.orthoMatrixUniform = shaderProgram.uniform("orthographicMatrix");
        this.textureSamplerUniform = shaderProgram.uniform("textureSampler");
        this.resolutionUniform = shaderProgram.uniform("resolution");
        this.squircleUniform = shaderProgram.uniform("squircle");
        this.alphaClipUniform = shaderProgram.uniform("alphaClip");
        this.textureSlotAllocator = new TextureSlotAllocator(i);
        this.colorStack = new ColorStack();
        List<VertexAttributeType> listOf = List.of(VertexAttributeType.VEC2, VertexAttributeType.VEC2, VertexAttributeType.VEC2, VertexAttributeType.VEC2, VertexAttributeType.VEC4, VertexAttributeType.NORMALIZED_VEC4, VertexAttributeType.NORMALIZED_VEC4, VertexAttributeType.FLOAT, VertexAttributeType.FLOAT, VertexAttributeType.UNSIGNED_BYTE, VertexAttributeType.UNSIGNED_BYTE, VertexAttributeType.UNSIGNED_BYTE);
        this.vertexArrayId = GL33.glGenVertexArrays();
        this.vertexBufferId = GL33.glGenBuffers();
        this.indexBufferId = GL33.glGenBuffers();
        GL33.glBindVertexArray(this.vertexArrayId);
        GL33.glBindBuffer(34962, this.vertexBufferId);
        GL33.glBindBuffer(34963, this.indexBufferId);
        int iSum = listOf.stream().mapToInt(VertexAttributeType::size).sum();
        int size = 0;
        for (int i2 = 0; i2 < listOf.size(); i2++) {
            VertexAttributeType vertexAttributeType = listOf.get(i2);
            GL33.glEnableVertexAttribArray(i2);
            if (vertexAttributeType.integer()) {
                GL33.glVertexAttribIPointer(i2, vertexAttributeType.count(), vertexAttributeType.type(), iSum, size);
            } else {
                GL33.glVertexAttribPointer(i2, vertexAttributeType.count(), vertexAttributeType.type(), vertexAttributeType.normalized(), iSum, size);
            }
            size += vertexAttributeType.size();
        }
    }

    public void begin() {
        begin(true);
    }

    public void beginBoundFramebuffer() {
        begin(false);
    }

    private void begin(boolean bindMainFramebuffer) {
        if (this.building) {
            end();
        }
        Window window = Mc.INSTANCE.getWindow();
        updateContentScale(window);
        int width = scaledWidth(window);
        int height = scaledHeight(window);
        this.drawCallCount = 0;

        this.savedFramebuffer = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        if (bindMainFramebuffer) {
            bindMainFramebuffer(window);
        }

        setupGlState();
        beginScissor(0.0f, 0.0f, width, height);
        this.savedTextureBinding = GL33.glGetInteger(32873);
        this.savedActiveTexture = GL33.glGetInteger(34016);
        this.colorStack.begin();
        this.building = true;
    }

    private void bindMainFramebuffer(Window window) {
        try {
            RenderTarget fb = Mc.INSTANCE.getFramebuffer();
            if (fb != null) {
                int fbo = FramebufferUtil.getFramebufferId(fb);
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
                GL33.glViewport(0, 0, window.getWidth(), window.getHeight());
            }
        } catch (Exception e) {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
    }

    public void end() {
        if (!this.building) {
            return;
        }
        this.building = false;
        draw();
        GL33.glActiveTexture(this.savedActiveTexture);
        GL33.glBindTexture(3553, this.savedTextureBinding);
        restoreGlState();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, this.savedFramebuffer);
    }

    public int bindTexture(int i) {
        if (!this.building) {
            throw new RuntimeException("DrawEngine.bindTexture() called while not building.");
        }
        if (!this.textureSlotAllocator.contains(i) && this.textureSlotAllocator.isFull()) {
            draw();
        }
        return this.textureSlotAllocator.bindTexture(i);
    }

    public void draw() {
        Window window = Mc.INSTANCE.getWindow();
        Matrix4f matrix4fOrtho2D = new Matrix4f().ortho2D(0.0f, window.getWidth(), window.getHeight(), 0.0f);
        ByteBuffer byteBufferDirectByteBuffer = this.vertexBuffer.directByteBuffer();
        ByteBuffer byteBufferDirectByteBuffer2 = this.indexBuffer.directByteBuffer();
        GL33.glBindVertexArray(this.vertexArrayId);
        GL33.glBindBuffer(34962, this.vertexBufferId);
        GL33.glBindBuffer(34963, this.indexBufferId);
        GL33.glBufferData(34962, MemoryUtil.memSlice(byteBufferDirectByteBuffer), 35040);
        GL33.glBufferData(34963, MemoryUtil.memSlice(byteBufferDirectByteBuffer2), 35040);
        this.shaderProgram.bind();
        this.orthoMatrixUniform.uploadMatrix4f(matrix4fOrtho2D);
        this.textureSamplerUniform.uploadIntBuffer(this.textureSlotAllocator.textureSlots());
        this.resolutionUniform.uploadVec2(window.getWidth(), window.getHeight());
        this.squircleUniform.uploadFloat(this.squircleCorners ? 1.0f : 0.0f);
        this.alphaClipUniform.uploadFloat(this.stencilWriting ? 0.5f : 0.0f);
        boolean z = this.blendSwitchCallback != null;
        if (z) {
            this.blendSwitchCallback.run();
            this.blendSwitchCallback = null;
        }
        GL33.glDrawElements(4, this.indexCount, 5125, 0L);
        if (z) {
            GL33.glBlendFuncSeparate(770, 771, 1, 771);
        }
        this.drawCallCount++;
        this.indexCount = 0;
        this.vertexCount = 0;
        this.shaderProgram.unbind();
        this.textureSlotAllocator.clear();
        this.vertexBuffer.reset();
        this.indexBuffer.reset();
    }

    public void setupGlState() {
        this.stateSnapshot = GlStateSnapshot.create();
        GL33.glEnable(3042);
        GL33.glBlendEquation(32774);
        GL33.glBlendFuncSeparate(770, 771, 1, 771);
        GL33.glEnable(3089);
        GL33.glDisable(2884);
        GL33.glDisable(2929);
        GL33.glPolygonMode(1032, 6914);
        GL33.glPixelStorei(3317, 1);
        GL33.glPixelStorei(3333, 1);
        for (int i = 0; i < this.maxTextureUnits; i++) {
            GL33.glBindSampler(i, 0);
        }
    }

    public void restoreGlState() {
        this.scissorStack.end();
        this.stateSnapshot.revert();
    }

    public void switchBlendFuncAndDraw(Runnable runnable) {
        this.blendSwitchCallback = runnable;
        draw();
    }

    public void beginStencil() {
        if (this.usingStencil) {
            throw new RuntimeException("DrawEngine.beginStencil() called while already using stencil.");
        }
        this.usingStencil = true;
        draw();
        StencilBufferUtil.prepareStencil();
        this.stencilWriting = true;
    }

    public void prepareStencil(int i) {
        if (!this.usingStencil) {
            throw new RuntimeException("DrawEngine.prepareStencil() called while not using stencil.");
        }
        draw();
        this.stencilWriting = false;
        StencilBufferUtil.prepareElement(i);
    }

    public void endStencil() {
        if (!this.usingStencil) {
            throw new RuntimeException("DrawEngine.endStencil() called while not using stencil.");
        }
        draw();
        StencilBufferUtil.cleanup();
        this.usingStencil = false;
    }

    public void beginScissor(float f, float f2, float f3, float f4) {
        Window window = Mc.INSTANCE.getWindow();
        int iRound = Math.round(applyContentScale(f));
        int iRound2 = Math.round(applyContentScale(f2));
        int iRound3 = Math.round(applyContentScale(f3));
        int iRound4 = Math.round(applyContentScale(f4));
        int framebufferHeight = framebufferY(window, iRound2, iRound4);
        draw();
        this.scissorStack.push(iRound, framebufferHeight, iRound3, iRound4, f, f2, f3, f4);
    }

    public void beginScissor(Matrix4f matrix4f, float f, float f2, float f3, float f4) {
        Window window = Mc.INSTANCE.getWindow();
        float fTransformX = transformX(matrix4f, f);
        float fTransformY = transformY(matrix4f, f2);
        float fTransformX2 = transformX(matrix4f, f + f3);
        float fTransformY2 = transformY(matrix4f, f2 + f4);
        int iRound = Math.round(applyContentScale(fTransformX));
        int iRound2 = Math.round(applyContentScale(fTransformY));
        int iRound3 = Math.round(applyContentScale(fTransformX2 - fTransformX));
        int iRound4 = Math.round(applyContentScale(fTransformY2 - fTransformY));
        int framebufferHeight = framebufferY(window, iRound2, iRound4);
        draw();
        this.scissorStack.push(iRound, framebufferHeight, iRound3, iRound4, fTransformX, fTransformY, fTransformX2, fTransformY2);
    }

    public void drawDebugDataIfEnabled() {
        int i = this.drawCallCount;
        ArrayList<String> arrayList = new ArrayList();
        arrayList.add("drawcalls " + i);
        arrayList.add(Minecraft.getInstance().getFps() + " fps");
        MsdfFont msdfFont = Fonts.INTER_SEMIBOLD.get();
        Window window = Mc.INSTANCE.getWindow();
        float height = msdfFont.getHeight(12.0f);
        int screenWidth = scaledWidth(window);
        float textY = (scaledHeight(window) - 45.0f) - (height * arrayList.size());
        Matrix4f positionMatrix = new PoseStack().last().pose();
        for (String str : arrayList) {
            msdfFont(positionMatrix, msdfFont, str, (screenWidth - 10.0f) - msdfFont.getWidth(str, 12.0f), textY, 12.0f, 0.05f, -1);
            textY += height;
        }
    }

    public void endScissor() {
        draw();
        this.scissorStack.pop();
    }

    public float transformY(PoseStack matrixStack, float f) {
        Matrix4f positionMatrix = matrixStack.last().pose();
        Vector4f vector4f = new Vector4f(0.0f, f, 0.0f, 1.0f);
        positionMatrix.transform(vector4f);
        return vector4f.y();
    }

    public float transformX(PoseStack matrixStack, float f) {
        Matrix4f positionMatrix = matrixStack.last().pose();
        Vector4f vector4f = new Vector4f(f, 0.0f, 0.0f, 1.0f);
        positionMatrix.transform(vector4f);
        return vector4f.x();
    }

    public float transformY(Matrix4f matrix4f, float f) {
        Vector4f vector4f = new Vector4f(0.0f, f, 0.0f, 1.0f);
        matrix4f.transform(vector4f);
        return vector4f.y();
    }

    public float transformX(Matrix4f matrix4f, float f) {
        Vector4f vector4f = new Vector4f(f, 0.0f, 0.0f, 1.0f);
        matrix4f.transform(vector4f);
        return vector4f.x();
    }

    public void emitShape(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, float f13, float f14, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, ShapeType shapeType) {
        Window window = Mc.INSTANCE.getWindow();
        Vector4f vector4fTransform = matrix4f.transform(this.tempVector.set(f, f2, 0.0f, 1.0f));
        float fX = vector4fTransform.x();
        float fY = vector4fTransform.y();
        float fX2 = vector4fTransform.x();
        float fY2 = vector4fTransform.y();
        Vector4f vector4fTransform2 = matrix4f.transform(this.tempVector.set(f + f3, f2 + f4, 0.0f, 1.0f));
        float fX3 = vector4fTransform2.x() - fX;
        float fY3 = vector4fTransform2.y() - fY;
        float fX4 = vector4fTransform2.x() - fX2;
        float fY4 = vector4fTransform2.y() - fY2;
        this.lastBounds.setGui(fX, fY, fX3, fY3);
        float scaledX = applyContentScale(fX2);
        float scaledY = applyContentScale(fY2);
        float scaledWidth = applyContentScale(fX4);
        float scaledHeight = applyContentScale(fY4);
        float framebufferHeight = framebufferY(window, scaledY, scaledHeight);
        this.lastBounds.setGl(scaledX, framebufferHeight, scaledWidth, scaledHeight);
        float fMax = Math.max(matrix4f.m00(), matrix4f.m11()) * this.contentScale;
        float f15 = f9 * fMax;
        float f16 = f10 * fMax;
        float f17 = f11 * fMax;
        float f18 = f12 * fMax;
        float f19 = f13 * this.contentScale;
        float f20 = f14 * this.contentScale;
        int iMode = shapeType.mode();
        writeTransformedPosition(matrix4f, f, f2 + f4);
        writeVec2(scaledX, framebufferHeight);
        writeVec2(scaledWidth, scaledHeight);
        writeVec2(f5, f8);
        writeVec4(f15, f16, f17, f18);
        writeColor(i5);
        writeColor(i);
        writeFloat(f19);
        writeFloat(f20);
        writeByte(i9);
        writeByte(iMode);
        writeByte(i10);
        writeTransformedPosition(matrix4f, f + f3, f2 + f4);
        writeVec2(scaledX, framebufferHeight);
        writeVec2(scaledWidth, scaledHeight);
        writeVec2(f7, f8);
        writeVec4(f15, f16, f17, f18);
        writeColor(i6);
        writeColor(i2);
        writeFloat(f19);
        writeFloat(f20);
        writeByte(i9);
        writeByte(iMode);
        writeByte(i10);
        writeTransformedPosition(matrix4f, f + f3, f2);
        writeVec2(scaledX, framebufferHeight);
        writeVec2(scaledWidth, scaledHeight);
        writeVec2(f7, f6);
        writeVec4(f15, f16, f17, f18);
        writeColor(i7);
        writeColor(i3);
        writeFloat(f19);
        writeFloat(f20);
        writeByte(i9);
        writeByte(iMode);
        writeByte(i10);
        writeTransformedPosition(matrix4f, f, f2);
        writeVec2(scaledX, framebufferHeight);
        writeVec2(scaledWidth, scaledHeight);
        writeVec2(f5, f6);
        writeVec4(f15, f16, f17, f18);
        writeColor(i8);
        writeColor(i4);
        writeFloat(f19);
        writeFloat(f20);
        writeByte(i9);
        writeByte(iMode);
        writeByte(i10);
        writeTriangle(this.vertexCount, this.vertexCount + 1, this.vertexCount + 2);
        writeTriangle(this.vertexCount, this.vertexCount + 2, this.vertexCount + 3);
        this.vertexCount += 4;
        this.indexCount += 6;
    }

    public void emitTexturedQuad(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, int i, int i2, int i3, int i4, int i5, int i6, ShapeType shapeType) {
        Window window = Mc.INSTANCE.getWindow();
        Vector4f vector4f = new Vector4f();
        matrix4f.transform(vector4f.set(f, f2, 0.0f, 1.0f));
        float fX = vector4f.x();
        float fY = vector4f.y();
        matrix4f.transform(vector4f.set(f3, f4, 0.0f, 1.0f));
        float fX2 = vector4f.x();
        float fY2 = vector4f.y();
        matrix4f.transform(vector4f.set(f5, f6, 0.0f, 1.0f));
        float fX3 = vector4f.x();
        float fY3 = vector4f.y();
        matrix4f.transform(vector4f.set(f7, f8, 0.0f, 1.0f));
        float fX4 = vector4f.x();
        float fY4 = vector4f.y();
        float fMin = Math.min(Math.min(fX, fX2), Math.min(fX3, fX4));
        float fMax = Math.max(Math.max(fX, fX2), Math.max(fX3, fX4));
        float fMin2 = Math.min(Math.min(fY, fY2), Math.min(fY3, fY4));
        float fMax2 = Math.max(Math.max(fY, fY2), Math.max(fY3, fY4));
        float f13 = fMax - fMin;
        float f14 = fMax2 - fMin2;
        float framebufferHeight = framebufferY(window, applyContentScale(fMin2), applyContentScale(f14));
        float scaledX1 = applyContentScale(fX);
        float scaledY1 = applyContentScale(fY);
        float scaledX2 = applyContentScale(fX2);
        float scaledY2 = applyContentScale(fY2);
        float scaledX3 = applyContentScale(fX3);
        float scaledY3 = applyContentScale(fY3);
        float scaledX4 = applyContentScale(fX4);
        float scaledY4 = applyContentScale(fY4);
        float scaledMinX = applyContentScale(fMin);
        float scaledWidth = applyContentScale(f13);
        float scaledHeight = applyContentScale(f14);
        this.lastBounds.setGui(fMin, fMin2, f13, f14);
        this.lastBounds.setGl(scaledMinX, framebufferHeight, scaledWidth, scaledHeight);
        int iMode = shapeType.mode();
        writeVec2(scaledX1, scaledY1);
        writeVec2(scaledMinX, framebufferHeight);
        writeVec2(scaledWidth, scaledHeight);
        writeVec2(f9, f12);
        writeVec4(0.0f, 0.0f, 0.0f, 0.0f);
        writeColor(i);
        writeColor(-1);
        writeFloat(0.0f);
        writeFloat(0.0f);
        writeByte(i5);
        writeByte(iMode);
        writeByte(i6);
        writeVec2(scaledX2, scaledY2);
        writeVec2(scaledMinX, framebufferHeight);
        writeVec2(scaledWidth, scaledHeight);
        writeVec2(f11, f12);
        writeVec4(0.0f, 0.0f, 0.0f, 0.0f);
        writeColor(i2);
        writeColor(-1);
        writeFloat(0.0f);
        writeFloat(0.0f);
        writeByte(i5);
        writeByte(iMode);
        writeByte(i6);
        writeVec2(scaledX3, scaledY3);
        writeVec2(scaledMinX, framebufferHeight);
        writeVec2(scaledWidth, scaledHeight);
        writeVec2(f11, f10);
        writeVec4(0.0f, 0.0f, 0.0f, 0.0f);
        writeColor(i3);
        writeColor(-1);
        writeFloat(0.0f);
        writeFloat(0.0f);
        writeByte(i5);
        writeByte(iMode);
        writeByte(i6);
        writeVec2(scaledX4, scaledY4);
        writeVec2(scaledMinX, framebufferHeight);
        writeVec2(scaledWidth, scaledHeight);
        writeVec2(f9, f10);
        writeVec4(0.0f, 0.0f, 0.0f, 0.0f);
        writeColor(i4);
        writeColor(-1);
        writeFloat(0.0f);
        writeFloat(0.0f);
        writeByte(i5);
        writeByte(iMode);
        writeByte(i6);
        writeTriangle(this.vertexCount, this.vertexCount + 1, this.vertexCount + 2);
        writeTriangle(this.vertexCount, this.vertexCount + 2, this.vertexCount + 3);
        this.vertexCount += 4;
        this.indexCount += 6;
    }

    public void quadTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, int i, int i2, int i3, int i4, int i5) {
        emitTexturedQuad(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, i2, i3, i4, i5, i, 0, ShapeType.TEXTURE);
    }

    public void quadPixelTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4,
                                 float f5, float f6, float f7, float f8, float f9, float f10,
                                 float f11, float f12, int texture, int i2, int i3, int i4, int i5) {
        emitTexturedQuad(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12,
                i2, i3, i4, i5, texture, 0, ShapeType.PIXEL_TEXTURE);
    }

    public void rectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2, int i3, int i4) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1, -1, -1, -1, i, i2, i3, i4, 0, 0, ShapeType.COLOR);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int i, int i2) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f6, f7, f8, f9, f10, -1, -1, -1, -1, i, i, i, i, bindTexture(i2), 0, ShapeType.BLUR);
    }

    public void blur(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, i, i2);
    }

    public void blur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, 0.0f, 0.0f, i, i2);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i) {
        texture(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, i, i, i, i, i);
    }

    public void textureVerticalC(Matrix4f matrix4f, GlTexture glTexture, float f, float f2, int i, int i2) {
        texture(matrix4f, f, f2 - (((float) i2) / 2.0f), i, i2, bindTexture(glTexture.textureWithSTB()), -1);
    }

    public void roundedShadow(Matrix4f matrix4f, float x, float y, float width, float height, float radius, float softness, int color) {
        float glowRadius = Math.max(0.0f, softness);
        float shadowExtent = glowRadius * 2.25f + 1.0f;
        float matrixScale = Math.max(Math.abs(matrix4f.m00()), Math.abs(matrix4f.m11()));
        emitShape(matrix4f, x - shadowExtent, y - shadowExtent,
                width + shadowExtent * 2.0f, height + shadowExtent * 2.0f,
                0.0f, 0.0f, 1.0f, 1.0f, radius, radius, radius, radius,
                shadowExtent * matrixScale, glowRadius * matrixScale,
                -1, -1, -1, -1, color, color, color, color, 0, 0, ShapeType.SHADOW);
    }

    public void roundedHorizontalGradient(Matrix4f matrix, float x, float y, float width, float height,
                                          float radius, ColorStack.HorizontalGradient gradient) {
        int start = gradient.startColor();
        int endTop = gradient.endTopColor();
        int endBottom = gradient.endBottomColor();
        emitShape(matrix, x, y, width, height,
                0.0f, 0.0f, 1.0f, 1.0f,
                radius, radius, radius, radius,
                gradient.transitionStart(), gradient.transitionEnd(),
                endBottom, endBottom, endTop, endTop,
                start, start, start, start,
                0, 0, ShapeType.HORIZONTAL_GRADIENT);
    }

    public void roundedGlassPanel(Matrix4f matrix, float x, float y, float width, float height,
                                  float radius, float farShadowRadius, float tintMix,
                                  int panelColor, int farShadowColor, int blurTexture) {
        float farRadius = Math.max(0.0f, farShadowRadius);
        float mix = MathUtil.clamp(tintMix, 0.0f, 1.0f);
        float transitionAlpha = MathUtil.clamp(this.colorStack.alphaMultiplier(), 0.0f, 1.0f);
        if (farRadius > 0.0f) {

            roundedShadow(matrix, x, y, width, height, radius, farRadius * 0.35f, farShadowColor);
        }
        emitShape(matrix, x, y, width, height,
                transitionAlpha, mix, transitionAlpha, mix,
                radius, radius, radius, radius,
                0.0f, 0.0f,
                0, 0, 0, 0,
                panelColor, panelColor, panelColor, panelColor,
                bindTexture(blurTexture), 0, ShapeType.GLASS_PANEL);
    }

    public void colorWell(Matrix4f matrix, float x, float y, float size, float ringWidth, float gap,
                          int innerColor, int ringColor) {
        float ringRatio = ringWidth / Math.max(size * 0.5f, 0.001f);
        float gapRatio = gap / Math.max(size * 0.5f, 0.001f);
        emitShape(matrix, x, y, size, size,
                ringRatio, gapRatio, ringRatio, gapRatio,
                0.0f, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f,
                ringColor, ringColor, ringColor, ringColor,
                innerColor, innerColor, innerColor, innerColor,
                0, 0, ShapeType.COLOR_WELL);
    }

    public void saturationValue(Matrix4f matrix, float x, float y, float width, float height, float radius,
                                int hueColor) {
        emitShape(matrix, x, y, width, height,
                0.0f, 0.0f, 1.0f, 1.0f,
                radius, radius, radius, radius,
                0.0f, 0.0f,
                0, 0, 0, 0,
                hueColor, hueColor, hueColor, hueColor,
                0, 0, ShapeType.SATURATION_VALUE);
    }

    public void skinFace(Matrix4f matrix, float x, float y, float width, float height, float radius,
                         float u0, float v0, float u1, float v1, float hatOffset, int texture, int color) {
        emitShape(matrix, x, y, width, height,
                u0, v0, u1, v1,
                radius, radius, radius, radius,
                hatOffset, 0.0f,
                0, 0, 0, 0,
                color, color, color, color,
                texture, 0, ShapeType.SKIN_FACE);
    }

    public void circularGlassRing(Matrix4f matrix, float centerX, float centerY, float radius,
                                  float ringThickness, float tintMix, int panelColor, int blurTexture) {
        float padding = 2.0f;
        float diameter = Math.max(0.0f, radius + padding) * 2.0f;
        float mix = MathUtil.clamp(tintMix, 0.0f, 1.0f);
        float transitionAlpha = MathUtil.clamp(this.colorStack.alphaMultiplier(), 0.0f, 1.0f);
        emitShape(matrix, centerX - radius - padding, centerY - radius - padding, diameter, diameter,
                transitionAlpha, mix, transitionAlpha, mix,
                radius, radius, radius, radius,
                Math.max(0.0f, ringThickness), 0.0f,
                0, 0, 0, 0,
                panelColor, panelColor, panelColor, panelColor,
                bindTexture(blurTexture), 0, ShapeType.GLASS_RING);
    }

    public void outerMask(Matrix4f matrix4f, int i, int i2) {
        Window window = Mc.INSTANCE.getWindow();
        emitShape(matrix4f, 0.0f, 0.0f, scaledWidth(window), scaledHeight(window), 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0, 0, 0, 0, 0, 0, bindTexture(i), bindTexture(i2), ShapeType.OUTER_MASK);
    }

    public void alphaMask(Matrix4f matrix4f, int i, int i2) {
        Window window = Mc.INSTANCE.getWindow();
        float width = scaledWidth(window);
        float height = scaledHeight(window);
        int iBindTexture = bindTexture(i);
        int iArgb = this.colorStack.argb(i2, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK);
        emitShape(matrix4f, 0.0f, 0.0f, width, height, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0, 0, 0, 0, iArgb, iArgb, iArgb, iArgb, iBindTexture, 0, ShapeType.ALPHA_MASK);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, 0.0f, 0.0f, i, i2);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, 0.0f, i, i2);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, f7, i, i2);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, 0.0f, i, i2);
    }

    public void roundedBlur(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i, int i2) {
        roundedBlur(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, 0.0f, i, i2);
    }

    public void rectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i) {
        rectangle(matrix4f, f, f2, f3, f4, i, i, i, i);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2, int i3, int i4, int i5) {
        emitShape(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1, -1, -1, -1, i2, i3, i4, i5, i, 0, ShapeType.TEXTURE);
    }

    public void chevron(Matrix4f matrix4f, float x, float y, float width, float height,
                        float thickness, int color) {
        chevron(matrix4f, x, y, width, height, thickness, 0, color);
    }

    public void chevron(Matrix4f matrix4f, float x, float y, float width, float height,
                        float thickness, int direction, int color) {
        float scale = Math.max(matrix4f.m00(), matrix4f.m11());
        emitShape(matrix4f, x, y, width, height, 0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 0.0f, 0.0f, thickness * scale, direction,
                -1, -1, -1, -1, color, color, color, color,
                0, 0, ShapeType.CHEVRON);
    }

    public void glyphTexture(Matrix4f matrix4f, float x, float y, float width, float height,
                             float u0, float v0, float u1, float v1, int texture, int color) {
        emitShape(matrix4f, x, y, width, height, u0, v0, u1, v1,
                0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                -1, -1, -1, -1, color, color, color, color,
                texture, 0, ShapeType.GLYPH_TEXTURE);
    }

    public void textureMsdf(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2, int i3, int i4, int i5) {
        textureMsdf(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, i, i2, i3, i4, i5, 10.0f);
    }

    public void textureMsdf(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2, int i3, int i4, int i5, float distanceRange) {
        float softness = 5.0f / Math.max(distanceRange, 1.0f);
        emitShape(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, 0.0f, 0.0f, 0.0f, 0.05f, softness, -1, -1, -1, -1, i2, i3, i4, i5, i, 0, ShapeType.MSDF_FONT);
    }

    public void itemStack(Matrix4f matrix4f, ItemStack itemStack, float f, float f2, float f3, float f4) {
        ItemSpriteTextures spriteTextures;
        Map<Direction, SpriteRegion> mapSprites;
        float f5;
        if (itemStack == null || itemStack.isEmpty() || (spriteTextures = ItemSpriteManager.INSTANCE.getSpriteTextures(itemStack)) == null || (mapSprites = spriteTextures.sprites()) == null || mapSprites.isEmpty()) {
            return;
        }
        float f6 = 32.0f * f3;
        float f7 = f6 / 2.0f;
        float f8 = f6 / 4.0f;
        float f9 = f6 / 16.0f;
        float f10 = (f2 - f8) - (f9 / 2.0f);
        for (Map.Entry<Direction, SpriteRegion> entry : mapSprites.entrySet()) {
            Direction direction = (Direction) entry.getKey();
            SpriteRegion spriteRegion = (SpriteRegion) entry.getValue();
            if (spriteRegion != null) {
                int iBindTexture = bindTexture(FramebufferUtil.getTextureId(Mc.INSTANCE.getTextureManager().getTexture(spriteRegion.atlasId())));
                float fMinU = spriteRegion.minU() + 1.0E-4f;
                float fMinV = spriteRegion.minV() + 1.0E-4f;
                float fMaxU = spriteRegion.maxU() - 1.0E-4f;
                float fMaxV = spriteRegion.maxV() - 1.0E-4f;
                switch (direction) {
                    case UP:
                        f5 = 1.0f;
                        break;
                    case WEST:
                        f5 = 0.7f;
                        break;
                    case EAST:
                        f5 = 0.85f;
                        break;
                    default:
                        f5 = 1.0f;
                        break;
                }
                float f11 = f5;
                int color = this.colorStack.computeColor(Math.max(0, Math.min(StencilBufferUtil.STENCIL_MASK, (int) (f4 * 255.0f))), (int) (255.0f * f11), (int) (255.0f * f11), (int) (255.0f * f11));
                if (spriteTextures.type().equals("3D")) {
                    switch (direction) {
                        case UP:
                            quadPixelTexture(matrix4f, f + f7, f10 + f8, f + f6, f10 + f7, f + f7, (f10 + f6) - f8, f, f10 + f7, fMinU, fMinV, fMaxU, fMaxV, iBindTexture, color, color, color, color);
                            break;
                        case WEST:
                            float f12 = f + f7;
                            float f13 = f10 + f7 + f8;
                            quadPixelTexture(matrix4f, f + f6, f10 + f7, f + f6, f10 + f6 + f9, f + f7, f10 + f6 + f8 + f9, f12, f13, fMinU, fMinV, fMaxU, fMaxV, iBindTexture, color, color, color, color);
                            break;
                        case EAST:
                            quadPixelTexture(matrix4f, f, f10 + f6 + f9, f + f7, f10 + f6 + f8 + f9, (f + f6) - f7, f10 + f7 + f8, f, f10 + f7, fMinU, fMinV, fMaxU, fMaxV, iBindTexture, color, color, color, color);
                            break;
                    }
                } else {
                    f6 = 32.0f * f3;
                    float f14 = f + f6;
                    float f15 = f2 + f6;
                    int color2 = this.colorStack.computeColor(spriteRegion.color(), f4);
                    quadPixelTexture(matrix4f, f, f15, f14, f15, f14, f2, f, f2, fMinU, fMinV, fMaxU, fMaxV, iBindTexture, color2, color2, color2, color2);
                }
            }
        }
    }

    public void drawLine(PoseStack matrixStack, MsdfFont msdfFont, int i, int i2, float f, String str, String str2, int i3, int i4) {
        int width = (int) ((i2 - (msdfFont.getWidth(str, i) + msdfFont.getWidth(str2, i))) / 2.0f);
        msdfFont(matrixStack.last().pose(), msdfFont, str, width, f, i, 0.0f, i3);
        msdfFont(matrixStack.last().pose(), msdfFont, str2, width + msdfFont.getWidth(str, i), f, i, 0.0f, i4);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, int i) {
        circle(matrix4f, f, f2, f3, 0.0f, 0.0f, 360.0f, 0.0f, -1, i, i, i, i);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i) {
        circle(matrix4f, f, f2, f3, f4, 0.0f, 360.0f, 0.0f, -1, i, i, i, i);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, int i, int i2, int i3, int i4) {
        circle(matrix4f, f, f2, f3, 0.0f, 0.0f, 360.0f, 0.0f, -1, i, i2, i3, i4);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2, int i3, int i4) {
        circle(matrix4f, f, f2, f3, f4, 0.0f, 360.0f, 0.0f, -1, i, i2, i3, i4);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2) {
        circle(matrix4f, f, f2, f3, 0.0f, 0.0f, 360.0f, f4, i, i2, i2, i2, i2);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        circle(matrix4f, f, f2, f3, f4, 0.0f, 360.0f, f5, i, i2, i2, i2, i2);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i) {
        circle(matrix4f, f, f2, f3, 0.0f, f4, f5, 0.0f, -1, i, i, i, i);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        circle(matrix4f, f, f2, f3, f4, f5, f6, 0.0f, -1, i, i, i, i);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        circle(matrix4f, f, f2, f3, 0.0f, f4, f5, f6, i, i2, i2, i2, i2);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, int i2) {
        circle(matrix4f, f, f2, f3, f4, f5, f6, f7, i, i2, i2, i2, i2);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, int i3, int i4, int i5) {
        circle(matrix4f, f, f2, f3, 0.0f, f4, f5, f6, i, i2, i3, i4, i5);
    }

    public void circle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, int i, int i2, int i3, int i4, int i5) {
        float f8 = f3 * 2.0f;
        float fRad = MathUtil.rad(f5);
        float fRad2 = MathUtil.rad(f6);
        float f9 = f7 > 0.0f ? 1.0f : 0.0f;
        float f10 = (f7 > 0.0f ? f7 * 0.5f : 0.0f) + (f7 > 0.0f ? f9 : f9 + 1.0f) + 1.0f;
        float f11 = (f3 * 2.0f) + (f10 * 2.0f);
        emitShape(matrix4f, (f - f3) - f10, (f2 - f3) - f10, f11, f11, fRad, fRad2, fRad, fRad2, f3, f4, 0.0f, 0.0f, f7, f9, i, i, i, i, i2, i3, i4, i5, 0, 0, ShapeType.CIRCLE);
    }

    public void msdfFont(Matrix4f matrix4f, MsdfFont msdfFont, String str, float f, float f2, float f3, float f4, int i) {
        msdfFont.applyGlyphs(matrix4f, this, bindTexture(msdfFont.getTextureId()), str, f3, 0.0f, 0.0f, f, f2 + ((msdfFont.metrics().ascent() != 0.0f ? msdfFont.metrics().ascent() : msdfFont.metrics().lineHeight()) * f3), i);
    }

    public void msdfFontHorizontalGradient(Matrix4f matrix4f, MsdfFont msdfFont, String str, float x, float y,
                                           float size, int startColor, int endColor) {
        float ascent = msdfFont.metrics().ascent() != 0.0f
                ? msdfFont.metrics().ascent()
                : msdfFont.metrics().lineHeight();
        msdfFont.applyGlyphsWithHorizontalGradient(matrix4f, this, bindTexture(msdfFont.getTextureId()), str,
                size, 0.0f, 0.0f, x, y + ascent * size, startColor, endColor);
    }

    public void msdfText(Matrix4f matrix4f, MsdfFont msdfFont, Component text, float f, float f2, float f3, float f4, float f5, int i) {
        int iBindTexture = bindTexture(msdfFont.getTextureId());
        float fAscent = msdfFont.metrics().ascent() != 0.0f ? msdfFont.metrics().ascent() : msdfFont.metrics().lineHeight();
        float height = msdfFont.getHeight(f3);
        int i2 = (i >> 24) & StencilBufferUtil.STENCIL_MASK;
        float[] fArr = {0.0f};
        float[] fArr2 = {0.0f};
        int[] iArr = {i};
        boolean[] zArr = {false};
        int[] iArrGlyphCodes = msdfFont.glyphCodes();
        ThreadLocalRandom threadLocalRandomCurrent = ThreadLocalRandom.current();
        int[] iArr2 = {-1};
        text.getVisualOrderText().accept((charIndex, style, codePoint) -> {
            int i3;
            char c = (char) codePoint;
            if (c == '\n') {
                fArr[0] = 0.0f;
                fArr2[0] = fArr2[0] + height;
                iArr2[0] = -1;
                return true;
            }
            if (style.getColor() != null) {
                iArr[0] = (i2 << 24) | (style.getColor().getValue() & 16777215);
                zArr[0] = true;
            } else if (zArr[0]) {
                iArr[0] = i;
                zArr[0] = false;
            }
            int i4 = c;
            if (style.isObfuscated() && !Character.isWhitespace(c)) {
                if (iArrGlyphCodes.length == 0) {
                    return true;
                }
                int i5 = 0;
                do {
                    i3 = iArrGlyphCodes[threadLocalRandomCurrent.nextInt(iArrGlyphCodes.length)];
                    i5++;
                    if (msdfFont.glyphWidth(i3, f3) > 0.01f) {
                        break;
                    }
                } while (i5 < 20);
                i4 = i3;
            }
            int iGlyphIndex = msdfFont.glyphIndex(i4);
            if (iGlyphIndex == -1) {
                iArr2[0] = -1;
                return true;
            }
            if (iArr2[0] != -1) {
                fArr[0] = fArr[0] + (msdfFont.kerningAdvance(iArr2[0], i4) * f3);
            }
            fArr[0] = fArr[0] + msdfFont.applyGlyph(matrix4f, this, iGlyphIndex, f3, f + fArr[0], f2 + fArr2[0] + (fAscent * f3), iArr[0], iBindTexture) + f4 + f5;
            iArr2[0] = i4;
            return true;
        });
    }

    public void msdfText(Matrix4f matrix4f, MsdfFont msdfFont, Component text, float f, float f2, float f3, int i) {
        msdfText(matrix4f, msdfFont, text, f, f2, f3, 0.05f, 0.0f, i);
    }

    public void hollowCircle(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i) {
        circle(matrix4f, f, f2, f3, Math.max(0.0f, f3 - f4), 0.0f, 360.0f, 0.0f, -1, i, i, i, i);
    }

    public void arc(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i) {
        circle(matrix4f, f, f2, f3, Math.max(0.0f, f3 - f6), f4, f5, 0.0f, -1, i, i, i, i);
    }

    public void circleChecker(Matrix4f matrix4f, float f, float f2, float f3, int i, int i2) {
        roundedChecker(matrix4f, f - f3, f2 - f3, f3 * 2.0f, f3 * 2.0f, f3, 2.0f, i, i2);
    }

    public void roundedChecker(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f5, f5, f5, 0.0f, f6, i, i, i, i, i2, i2, i2, i2, 0, 0, ShapeType.CHECKER);
    }

    public void msdfFontHorizontalC(Matrix4f matrix4f, MsdfFont msdfFont, String str, float f, float f2, float f3, float f4, int i) {
        msdfFont(matrix4f, msdfFont, str, f - Math.round(msdfFont.getWidth(str, f3) / 2.0f), f2, f3, f4, i);
    }

    public void msdfFontVerticalC(Matrix4f matrix4f, MsdfFont msdfFont, String str, float f, float f2, float f3, float f4, int i) {
        msdfFont(matrix4f, msdfFont, str, f, f2 - Math.round(msdfFont.getHeight(f3) / 2.0f), f3, f4, i);
    }

    public void msdfFontVerticalCHorizontalC(Matrix4f matrix4f, MsdfFont msdfFont, String str, float f, float f2, float f3, float f4, int i) {
        msdfFont(matrix4f, msdfFont, str, f - Math.round(msdfFont.getWidth(str, f3) / 2.0f), Math.round(f2 - (msdfFont.getHeight(f3) / 2.0f)), f3, f4, i);
    }

    public void msdfFontWithHorizontalGradient(Matrix4f matrix4f, MsdfFont msdfFont, String str, float f, float f2, float f3, float f4, int i, int i2) {
        msdfFont.applyGlyphsWithHorizontalGradient(matrix4f, this, bindTexture(msdfFont.getTextureId()), str, f3, 0.0f, 0.0f, f, f2 + ((msdfFont.metrics().ascent() != 0.0f ? msdfFont.metrics().ascent() : msdfFont.metrics().lineHeight()) * f3), i, i2);
    }

    public void textureMsdf(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2) {
        textureMsdf(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, i, i2, i2, i2, i2);
    }

    public void textureMsdf(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2, float distanceRange) {
        textureMsdf(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, i, i2, i2, i2, i2, distanceRange);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2) {
        texture(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, i, i2, i2, i2, i2);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2, int i3, int i4, int i5) {
        texture(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, i, i2, i3, i4, i5);
    }

    public void textureVerticalC(Matrix4f matrix4f, GlTexture texture, float f, float f2, int i, int i2, int i3) {
        texture(matrix4f, f, f2 - (Mth.ceil(i2) / 2.0f), i, i2, bindTexture(texture.textureWithSTB()), i3);
    }

    public void textureVerticalCHorizontalC(Matrix4f matrix4f, GlTexture texture, float f, float f2, int i, int i2, int i3) {
        texture(matrix4f, f - (Mth.ceil(i) / 2.0f), f2 - (Mth.ceil(i2) / 2.0f), i, i2, bindTexture(texture.textureWithSTB()), i3);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2) {
        texture(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, i, i2, i2, i2, i2);
    }

    public void texture(Matrix4f matrix4f, float f, float f2, float f3, float f4, int i, int i2, boolean z) {
        if (z) {
            texture(matrix4f, f, f2, f3, f4, 0.0f, 1.0f, 1.0f, 0.0f, i, i2, i2, i2, i2);
        } else {
            texture(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, i, i2, i2, i2, i2);
        }
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f6, f7, f8, f9, f10, i, i2, i3, i4, i5, i6, i7, i8, 0, 0, ShapeType.ROUNDED_RECTANGLE);
    }

    public void radialRoundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f5, f5, f5, 0.0f, 0.0f, i2, i2, i2, i2, i, i, i, i, 0, 0, ShapeType.RADIAL_ROUNDED_RECTANGLE);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, 0.0f, 0.0f, -1, -1, -1, -1, i, i, i, i);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, 1.0f, i, i, i, i, i2, i2, i2, i2);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, 1.0f, i, i2, i3, i4, i5, i6, i7, i8);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2, int i3, int i4) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, 0.0f, 0.0f, -1, -1, -1, -1, i, i2, i3, i4);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, int i, int i2, int i3, int i4, int i5) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, 1.0f, i, i, i, i, i2, i3, i4, i5);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i, int i2, int i3, int i4, int i5) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, 1.0f, i, i, i, i, i2, i3, i4, i5);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, 0.0f, -1, -1, -1, -1, i, i, i, i);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, int i, int i2) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, f10, i, i, i, i, i2, i2, i2, i2);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, 0.0f, f9, -1, -1, -1, -1, i, i, i, i);
    }

    public void roundedRectangle(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i, int i2) {
        roundedRectangle(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, f9, 0.0f, i, i, i, i, i2, i2, i2, i2);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2, int i3, int i4, int i5) {
        emitShape(matrix4f, f, f2, f3, f4, 0.0f, 0.0f, 1.0f, 1.0f, f5, f6, f7, f8, 0.0f, 0.0f, -1, -1, -1, -1, i2, i3, i4, i5, i, 0, ShapeType.ROUNDED_TEXTURE);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, float f10, float f11, float f12, int i, int i2, int i3, int i4, int i5) {
        emitShape(matrix4f, f, f2, f3, f4, f9, f10, f11, f12, f5, f6, f7, f8, 0.0f, 0.0f, -1, -1, -1, -1, i2, i3, i4, i5, i, 0, ShapeType.ROUNDED_TEXTURE);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int i, int i2) {
        roundedTexture(matrix4f, f, f2, f3, f4, f5, f6, f7, f8, i, i2, i2, i2, i2);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2, int i3, int i4, int i5) {
        roundedTexture(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, i, i2, i3, i4, i5);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, int i, int i2) {
        roundedTexture(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, i, i2, i2, i2, i2);
    }

    public void roundedTexture(Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, int i, int i2) {
        roundedTexture(matrix4f, f, f2, f3, f4, f5, f5, f5, f5, f6, f7, f8, f9, i, i2, i2, i2, i2);
    }

    public void writeTransformedPosition(Matrix4f matrix4f, float f, float f2) {
        Vector4f vector4fTransform = matrix4f.transform(this.tempVector.set(f, f2, 0.0f, 1.0f));
        writeVec2(applyContentScale(vector4fTransform.x()), applyContentScale(vector4fTransform.y()));
    }

    public void updateContentScale(Window window) {
        if (WindowUtil.isMac()) {
            this.contentScale = WindowUtil.getWindowContentScale(window.handle());
        } else {
            this.contentScale = 1.0f;
        }
    }

    public float applyContentScale(float f) {
        return this.contentScale == 1.0f ? f : f * this.contentScale;
    }

    private static int framebufferY(Window window, int top, int height) {
        return upperLeftClipOrigin() ? top : window.getHeight() - top - height;
    }

    private static float framebufferY(Window window, float top, float height) {
        return upperLeftClipOrigin() ? top : window.getHeight() - top - height;
    }

    private static boolean upperLeftClipOrigin() {
        return org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.ARBClipControl.GL_CLIP_ORIGIN)
                == org.lwjgl.opengl.ARBClipControl.GL_UPPER_LEFT;
    }

    public int scaledWidth(Window window) {
        return Math.max(1, Math.round(window.getWidth() / this.contentScale));
    }

    public int scaledHeight(Window window) {
        return Math.max(1, Math.round(window.getHeight() / this.contentScale));
    }

    public void writeVec2(float f, float f2) {
        this.vertexBuffer.requireMoreFreeBytes(8L);
        long jEffectiveAddress = this.vertexBuffer.effectiveAddress();
        this.vertexBuffer.writeFloat(jEffectiveAddress, f);
        this.vertexBuffer.writeFloat(jEffectiveAddress + 4, f2);
        this.vertexBuffer.offset(8L);
    }

    public void writeTriangle(int i, int i2, int i3) {
        this.indexBuffer.requireMoreFreeBytes(12L);
        long jEffectiveAddress = this.indexBuffer.effectiveAddress();
        this.indexBuffer.writeInt(jEffectiveAddress, i);
        this.indexBuffer.writeInt(jEffectiveAddress + 4, i2);
        this.indexBuffer.writeInt(jEffectiveAddress + 8, i3);
        this.indexBuffer.offset(12L);
    }

    public void writeVec4(float f, float f2, float f3, float f4) {
        this.vertexBuffer.requireMoreFreeBytes(16L);
        long jEffectiveAddress = this.vertexBuffer.effectiveAddress();
        this.vertexBuffer.writeFloat(jEffectiveAddress, f);
        this.vertexBuffer.writeFloat(jEffectiveAddress + 4, f2);
        this.vertexBuffer.writeFloat(jEffectiveAddress + 8, f3);
        this.vertexBuffer.writeFloat(jEffectiveAddress + 12, f4);
        this.vertexBuffer.offset(16L);
    }

    public void writeColorBytes(int i, int i2, int i3, int i4) {
        this.vertexBuffer.requireMoreFreeBytes(4L);
        long jEffectiveAddress = this.vertexBuffer.effectiveAddress();
        this.vertexBuffer.writeByte(jEffectiveAddress, i);
        this.vertexBuffer.writeByte(jEffectiveAddress + 1, i2);
        this.vertexBuffer.writeByte(jEffectiveAddress + 2, i3);
        this.vertexBuffer.writeByte(jEffectiveAddress + 3, i4);
        this.vertexBuffer.offset(4L);
    }

    public void writeInt(int i) {
        this.vertexBuffer.requireMoreFreeBytes(4L);
        this.vertexBuffer.writeInt(this.vertexBuffer.effectiveAddress(), i);
        this.vertexBuffer.offset(4L);
    }

    public void writeFloat(float f) {
        this.vertexBuffer.requireMoreFreeBytes(4L);
        this.vertexBuffer.writeFloat(this.vertexBuffer.effectiveAddress(), f);
        this.vertexBuffer.offset(4L);
    }

    public void writeByte(int i) {
        this.vertexBuffer.requireMoreFreeBytes(1L);
        this.vertexBuffer.writeByte(this.vertexBuffer.effectiveAddress(), i);
        this.vertexBuffer.offset(1L);
    }

    public void writeColor(int i) {
        writeColorBytes((i >> 16) & StencilBufferUtil.STENCIL_MASK, (i >> 8) & StencilBufferUtil.STENCIL_MASK, i & StencilBufferUtil.STENCIL_MASK, (i >> 24) & StencilBufferUtil.STENCIL_MASK);
    }

    public ColorStack colorStack() {
        return this.colorStack;
    }

    public ScissorStack scissorStack() {
        return this.scissorStack;
    }

    public boolean building() {
        return this.building;
    }

    public ScissorBounds lastBounds() {
        return this.lastBounds;
    }
}
