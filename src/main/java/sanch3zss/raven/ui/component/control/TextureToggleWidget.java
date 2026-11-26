package sanch3zss.raven.ui.component.control;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.gl.StencilBufferUtil;
import java.util.function.Supplier;

import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.AbstractWidget;
import sanch3zss.raven.util.math.interpolation.Easings;

public class TextureToggleWidget extends AbstractWidget {
    public final GlTexture inactiveTexture;
    public final GlTexture activeTexture;
    public final float textureWidth;
    public final float textureHeight;
    public final Supplier<Boolean> stateSupplier;
    public ColorValue inactiveColorValue;

    public ColorValue activeColorValue;
    public Runnable clickRunnable;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public boolean lastState = false;
    public float hitboxPaddingValue = 0.0f;
    public final ToggleAnimator toggleAnimator = new ToggleAnimator(Easings.EASE_IN_OUT_CUBIC);

    public TextureToggleWidget(GlTexture texture2, GlTexture texture, Supplier<Boolean> supplier, float f, float f2) {
        this.inactiveTexture = texture2;
        this.activeTexture = texture;
        this.textureWidth = f;
        this.textureHeight = f2;
        this.stateSupplier = supplier;
        this.clickable.clickCallback(() -> {
            if (this.clickRunnable != null) {
                this.clickRunnable.run();
            }
        });
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.textureWidth, this.textureHeight);
        float f = this.hitboxPaddingValue;
        float f2 = this.hitboxPaddingValue;
        this.clickable.setDimensions(x() - f, y() - f2, width() + (f * 2.0f), height() + (f2 * 2.0f));
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        boolean zBooleanValue = this.stateSupplier.get().booleanValue();
        if (zBooleanValue != this.lastState) {
            this.toggleAnimator.state(zBooleanValue);
            this.lastState = zBooleanValue;
        }
        if (this.inactiveColorValue == null) {
            this.inactiveColorValue = drawCtx.theme().palette().text().tone(100);
        }
        if (this.activeColorValue == null) {
            this.activeColorValue = drawCtx.theme().palette().text().tone(100);
        }
        int color = colorStack.computeColor(this.inactiveColorValue.argb());
        int color2 = colorStack.computeColor(this.activeColorValue.argb());
        int iBrightenedInterpolatedColor = colorStack.brightenedInterpolatedColor(color, 0.1f, this.clickable.hoverAnimation());
        int iDarkenedInterpolatedColor = colorStack.darkenedInterpolatedColor(color2, 0.1f, this.clickable.hoverAnimation());
        float fSmoothAnimation = this.toggleAnimator.smoothAnimation();
        int inactiveColor = applyAlpha(iBrightenedInterpolatedColor, 1.0f - fSmoothAnimation);
        int activeColor = applyAlpha(iDarkenedInterpolatedColor, fSmoothAnimation);
        drawCtx.texture(this.inactiveTexture, x(), y(), this.textureWidth, this.textureHeight, inactiveColor);
        drawCtx.texture(this.activeTexture, x(), y(), this.textureWidth, this.textureHeight, activeColor);
    }

    public static int applyAlpha(int i, float f) {
        return (i & 16777215) | (((int) (((i >>> 24) & StencilBufferUtil.STENCIL_MASK) * Math.max(0.0f, Math.min(1.0f, f)))) << 24);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickable.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.toggleAnimator.animate(weightedEngine);
        this.clickable.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public TextureToggleWidget inactiveColor(ColorValue colorValue) {
        this.inactiveColorValue = colorValue;
        return this;
    }

    public TextureToggleWidget activeColor(ColorValue colorValue) {
        this.activeColorValue = colorValue;
        return this;
    }

    public TextureToggleWidget hitboxPadding(float f) {
        this.hitboxPaddingValue = f;
        return this;
    }

    public TextureToggleWidget runnable(Runnable runnable) {
        this.clickRunnable = runnable;
        return this;
    }
}
