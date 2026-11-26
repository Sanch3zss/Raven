package sanch3zss.raven.ui.component.layout;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.DrawEngine;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;import sanch3zss.raven.render.animation.AnimatedFloat;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.ui.hud.Widget;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ScrollArea extends Widget {
    public static final float scrollSpeed = 80.0f;
    public float pendingScroll;
    public float scrollOffset;
    public float areaWidth;
    public float areaHeight;
    public float contentHeight;
    public final AnimatedFloat scrollAnimation = new AnimatedFloat(120, Easings.EASE_OUT_CUBIC);
    public final WidgetBounds physicalBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (z) {
            return false;
        }
        InputEvent inputEvent = inputEventContext.inputEvent();
        if (!(inputEvent instanceof ScrollInput scrollInput)) {
            return false;
        }
        double deltaY = scrollInput.deltaY();
        if (!inputEventContext.inPhysicalArea(inputEventContext.currentMousePosition(), this.physicalBounds.x(), this.physicalBounds.y(), this.physicalBounds.width(), this.physicalBounds.height())) {
            return false;
        }
        boolean controlDown = InputConstants.isKeyDown(Mc.INSTANCE.getWindow(), InputConstants.KEY_LCONTROL) || InputConstants.isKeyDown(Mc.INSTANCE.getWindow(), InputConstants.KEY_RCONTROL);
        float delta = (float) ((-deltaY) * 80.0d * (controlDown ? 3.0f : 1.0f));
        if (this.pendingScroll != 0.0f && Math.signum(delta) != Math.signum(this.pendingScroll)) {
            this.pendingScroll = 0.0f;
        }
        this.pendingScroll += delta;
        return true;
    }

    public void beginArea(DrawCtx drawCtx, float f, float f2, float f3, float f4) {
        this.scrollOffset += this.pendingScroll;
        this.pendingScroll = 0.0f;
        LayoutScaleContext layoutContext = drawCtx.layoutContext();
        this.areaWidth = f3;
        this.areaHeight = f4;
        DrawEngine drawEngine = drawCtx.drawEngine();
        PoseStack matrixStack = drawCtx.matrixStack();
        clampScroll(f4);
        float physical = layoutContext.toPhysical(f);
        float physical2 = layoutContext.toPhysical(f2);
        float physical3 = layoutContext.toPhysical(f3);
        float physical4 = layoutContext.toPhysical(f4);
        listen(drawCtx, physical, physical2, physical3, physical4);
        drawEngine.beginScissor(matrixStack.last().pose(), physical, physical2, physical3, physical4);
        this.scrollAnimation.destination(this.scrollOffset);
        matrixStack.pushPose();
        matrixStack.translate(0.0f, -layoutContext.toPhysical(scrollY()), 0.0f);
    }

    public void scrollToBottomSmooth() {
        float maxScroll = Math.max(0.0f, this.contentHeight - this.areaHeight);
        if (Math.abs(this.scrollOffset - maxScroll) < 1.0f) {
            return;
        }
        this.scrollOffset = maxScroll;
        clampScroll(this.areaHeight);
    }

    public void scrollToBottomImmediate() {
        this.scrollOffset = Math.max(0.0f, this.contentHeight - this.areaHeight);
        clampScroll(this.areaHeight);
        this.scrollAnimation.set(this.scrollOffset);
        this.scrollAnimation.destination(this.scrollOffset);
        this.pendingScroll = 0.0f;
    }

    @Deprecated
    public void scrollToBottom() {
        scrollToBottomSmooth();
    }

    public void endArea(DrawCtx drawCtx, float f) {
        DrawEngine drawEngine = drawCtx.drawEngine();
        PoseStack matrixStack = drawCtx.matrixStack();
        this.contentHeight = f;
        drawEngine.endScissor();
        matrixStack.popPose();
    }

    public void listen(DrawCtx drawCtx, float f, float f2, float f3, float f4) {
        Matrix4f positionMatrix = drawCtx.matrixStack().last().pose();
        DrawEngine drawEngine = drawCtx.drawEngine();
        Vector4f vector4f = new Vector4f(f, f2, 0.0f, 1.0f);
        Vector4f vector4f2 = new Vector4f(f + f3, f2 + f4, 0.0f, 1.0f);
        vector4f.mul(positionMatrix);
        vector4f2.mul(positionMatrix);
        this.physicalBounds.withPosition(drawEngine.transformX(positionMatrix, f), drawEngine.transformY(positionMatrix, f2)).withSize(vector4f2.x - vector4f.x, vector4f2.y - vector4f.y);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.areaWidth, this.areaHeight);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.scrollAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void scrollBy(float f) {
        this.scrollOffset += f;
        clampScroll(this.areaHeight);
    }

    public void scrollTo(float f) {
        this.scrollOffset = f;
        clampScroll(this.areaHeight);
        this.scrollAnimation.set(this.scrollOffset);
        this.scrollAnimation.destination(this.scrollOffset);
        this.pendingScroll = 0.0f;
    }

    public float scrollY() {
        return this.scrollAnimation.animatedValue();
    }

    public float visibleTop() {
        return scrollY();
    }

    public float visibleBottom() {
        return scrollY() + this.areaHeight;
    }

    public void clampScroll(float f) {
        if (this.contentHeight > f) {
            this.scrollOffset = MathUtil.clamp(this.scrollOffset, 0.0f, this.contentHeight - f);
        } else {
            this.scrollOffset = 0.0f;
        }
    }
}
