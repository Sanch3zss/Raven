package sanch3zss.raven.ui.component.button;

import java.util.function.BooleanSupplier;

import org.joml.Quaternionf;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.hud.AbstractWidget;
import sanch3zss.raven.util.math.interpolation.Easings;

public class LoadingButtonWidget extends AbstractWidget {
    public Translation text;
    public final float cornerRadius;
    public final float buttonWidth;
    public final float buttonHeight;
    public final boolean outlined;
    public final MsdfFont font;
    public final ClickableBehavior clickBehavior;
    public final ToggleAnimator disabledAnimation;
    public final ToggleAnimator loadingAnimation;
    public GlTexture icon;
    public int iconSize;
    public GlTexture loadingIcon;
    public Translation loadingText;
    public boolean loading;
    public float spinnerAngle;
    public int baseColor;
    public int textColor;
    public int outlineColor;
    public BooleanSupplier enabledSupplier;

    public LoadingButtonWidget(Translation translation, int i, int i2, Runnable runnable, float f, boolean z) {
        this(null, 0, translation, i, i2, runnable, f, z);
    }

    public LoadingButtonWidget(GlTexture texture2, int i, Translation translation, int i2, int i3, Runnable runnable, float f, boolean z) {
        this.font = Fonts.INTER_SEMIBOLD.get();
        this.clickBehavior = new ClickableBehavior();
        this.disabledAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
        this.loadingAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
        this.loadingText = Translation.clearText("Загрузка...");
        this.loading = false;
        this.spinnerAngle = 0.0f;
        this.baseColor = 268435455;
        this.textColor = 268435455;
        this.outlineColor = 16777215;
        this.enabledSupplier = () -> {
            return true;
        };
        this.clickBehavior.clickCallback(runnable);
        this.text = translation;
        this.cornerRadius = f;
        this.outlined = z;
        this.buttonWidth = i2;
        this.buttonHeight = i3;
        this.icon = texture2;
        this.iconSize = i;
    }

    public void primary(GlTexture texture2, int i, Translation translation) {
        this.icon = texture2;
        this.iconSize = i;
        this.text = translation;
        if (this.icon != null) {
            this.icon.setDimensions(this.iconSize, this.iconSize);
        }
    }

    public void loadingVisuals(GlTexture texture2, Translation translation) {
        this.loadingIcon = texture2;
        if (this.loadingIcon != null && this.iconSize > 0) {
            this.loadingIcon.setDimensions(this.iconSize, this.iconSize);
        }
        if (translation != null) {
            this.loadingText = translation;
        }
    }

    public void loading(boolean z) {
        this.loading = z;
        this.loadingAnimation.state(z);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        float fSmoothAnimation = this.disabledAnimation.smoothAnimation();
        float fSmoothAnimation2 = this.loadingAnimation.smoothAnimation();
        int color = colorStack.computeColor(this.baseColor);
        int iInterpolate = colorStack.interpolate(colorStack.interpolate(color, colorStack.autoBrightenDarken(color, 0.05f), this.clickBehavior.hoverAnimation()), colorStack.computeColor(palette.surfaceBackground().tone(600).argb(), 0.55f), fSmoothAnimation);
        int iInterpolate2 = colorStack.interpolate(colorStack.computeColor(this.outlineColor), colorStack.computeColor(palette.surfaceOutline().tone(400).argb(), 0.55f), fSmoothAnimation);
        drawCtx.fillOutlinedRoundedRect(x(), y(), this.buttonWidth, this.buttonHeight, this.cornerRadius, this.outlined ? 2.5f : 0.0f, colorStack.interpolate(iInterpolate2, colorStack.autoBrightenDarken(iInterpolate2, 0.05f), this.clickBehavior.hoverAnimation()), iInterpolate);
        float fX = x() + (this.buttonWidth / 2.0f);
        float fY = y() + (this.buttonHeight / 2.0f);
        int color2 = colorStack.computeColor(this.textColor);
        int color3 = colorStack.computeColor(palette.text().tone(400).argb(), 0.55f);
        GlTexture texture2 = this.icon;
        GlTexture texture = this.loadingIcon != null ? this.loadingIcon : this.icon;
        float fX2 = x() + 10.0f;
        if (texture2 != null) {
            colorStack.push();
            colorStack.alpha(1.0f - fSmoothAnimation2);
            drawCtx.textureVerticalC(texture2, fX2, fY, texture2.width(), texture2.height(), colorStack.interpolate(color2, colorStack.computeColor(palette.text().tone(500).argb(), 0.55f), fSmoothAnimation));
            colorStack.pop();
        }
        if (texture != null) {
            colorStack.push();
            colorStack.alpha(fSmoothAnimation2);
            drawCtx.matrixStack().pushPose();
            float physical = drawCtx.layoutContext().toPhysical(fX2 + (texture.width() / 2.0f));
            float physical2 = drawCtx.layoutContext().toPhysical(fY);
            drawCtx.matrixStack().translate(physical, physical2, 0.0f);
            drawCtx.matrixStack().mulPose(new Quaternionf().rotateZ((float) Math.toRadians(this.spinnerAngle)));
            drawCtx.matrixStack().translate(-physical, -physical2, 0.0f);
            drawCtx.textureVerticalC(texture, fX2, fY, texture.width(), texture.height(), colorStack.interpolate(color2, colorStack.computeColor(palette.text().tone(500).argb(), 0.55f), fSmoothAnimation));
            drawCtx.matrixStack().popPose();
            colorStack.pop();
        }
        String strEffective = this.text.effective();
        String strEffective2 = this.loadingText.effective();
        float fTextWidthPhysical = drawCtx.textWidthPhysical(this.font, strEffective, 12);
        float fTextWidthPhysical2 = drawCtx.textWidthPhysical(this.font, strEffective2, 12);
        float height = this.font.getHeight(12.0f);
        colorStack.push();
        colorStack.alpha(1.0f - fSmoothAnimation2);
        drawCtx.text(this.font, strEffective, 12, fX - (fTextWidthPhysical / 2.0f), fY - (height / 2.0f), colorStack.interpolate(color2, color3, fSmoothAnimation));
        colorStack.pop();
        colorStack.push();
        colorStack.alpha(fSmoothAnimation2);
        drawCtx.text(this.font, strEffective2, 12, fX - (fTextWidthPhysical2 / 2.0f), fY - (height / 2.0f), colorStack.interpolate(color2, color3, fSmoothAnimation));
        colorStack.pop();
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (!this.enabledSupplier.getAsBoolean() || this.loading) {
            return false;
        }
        return this.clickBehavior.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.disabledAnimation.state(!this.enabledSupplier.getAsBoolean());
        this.disabledAnimation.animate(weightedEngine);
        this.loadingAnimation.state(this.loading);
        this.loadingAnimation.animate(weightedEngine);
        this.clickBehavior.animate(weightedEngine);
        if (this.loadingAnimation.smoothAnimation() <= 0.001f) {
            this.spinnerAngle = 0.0f;
            return;
        }
        this.spinnerAngle += weightedEngine.weight() * 360.0f;
        if (this.spinnerAngle >= 360.0f) {
            this.spinnerAngle -= 360.0f;
        }
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.buttonWidth, this.buttonHeight);
        if (this.icon != null) {
            this.icon.setDimensions(this.iconSize, this.iconSize);
        }
        if (this.loadingIcon != null) {
            this.loadingIcon.setDimensions(this.iconSize, this.iconSize);
        }
        this.clickBehavior.setDimensions(x(), y(), width(), height());
    }

    public LoadingButtonWidget text(Translation translation) {
        this.text = translation;
        return this;
    }

    public void enabled(BooleanSupplier booleanSupplier) {
        this.enabledSupplier = booleanSupplier;
    }

    public Translation text() {
        return this.text;
    }

    public boolean loading() {
        return this.loading;
    }

    public int baseColor() {
        return this.baseColor;
    }

    public LoadingButtonWidget baseColor(int i) {
        this.baseColor = i;
        return this;
    }

    public int textColor() {
        return this.textColor;
    }

    public LoadingButtonWidget textColor(int i) {
        this.textColor = i;
        return this;
    }

    public int outlineColor() {
        return this.outlineColor;
    }

    public LoadingButtonWidget outlineColor(int i) {
        this.outlineColor = i;
        return this;
    }

    public BooleanSupplier enabledSupplier() {
        return this.enabledSupplier;
    }

    public LoadingButtonWidget enabledSupplier(BooleanSupplier booleanSupplier) {
        this.enabledSupplier = booleanSupplier;
        return this;
    }
}
