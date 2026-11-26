package sanch3zss.raven.ui.component.button;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.font.MsdfFont;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.hud.AbstractWidget;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ToggleTextButton extends AbstractWidget {
    public final MsdfFont font;
    public final GlTexture icon;
    public final float cornerRadius;
    public final float paddingX;
    public final float paddingY;
    public final int iconSize;
    public boolean active;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public Translation labelText = Translation.clearText("");
    public int textSize = 12;
    public final ToggleAnimator activeAnimator = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);

    public ToggleTextButton(Runnable runnable, MsdfFont msdfFont, GlTexture texture, float f, float f2, float f3, int i) {
        this.font = msdfFont;
        this.icon = texture;
        this.cornerRadius = f;
        this.paddingX = f2;
        this.paddingY = f3;
        this.iconSize = i;
        this.clickable.clickCallback(() -> {
            toggle();
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    public ToggleTextButton label(Translation translation, int i) {
        this.labelText = translation;
        this.textSize = i;
        return this;
    }

    public void setActive(boolean z) {
        if (this.active == z) {
            return;
        }
        this.active = z;
    }

    public void toggle() {
        setActive(!this.active);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        float fWidth = width();
        float fHeight = height();
        ToggleAnimator hoverAnimation = this.clickable.hoverAnimation();
        ToggleAnimator animator = this.activeAnimator;
        int iInterpolate = colorStack.interpolate(colorStack.interpolate(colorStack.computeColor(palette.surfaceBackground().tone(700).argb(), 0.0f), colorStack.computeColor(palette.surfaceBackground().tone(700).argb(), 140), hoverAnimation), colorStack.computeColor(palette.surfaceBackground().tone(700).argb()), animator, animator);
        int iInterpolate2 = colorStack.interpolate(colorStack.computeColor(palette.text().tone(500).argb()), colorStack.computeColor(palette.text().tone(400).argb()), animator, animator);
        drawCtx.fillOutlinedRoundedRect(x(), y(), fWidth, fHeight, this.cornerRadius, 2.5f, colorStack.computeColor(palette.surfaceOutline().tone(600).argb()), iInterpolate);
        drawCtx.texture(this.icon, x() + this.paddingX, (y() + (fHeight / 2.0f)) - (this.iconSize / 2), this.iconSize, this.iconSize, colorStack.computeColor(iInterpolate2));
        drawCtx.text(this.font, this.labelText.effective(), this.textSize, x() + this.paddingX + this.iconSize + 5.0f, (y() + (fHeight / 2.0f)) - (this.font.getHeight(this.textSize) / 2.0f), iInterpolate2);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickable.handleInput(inputEventContext, z);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.iconSize + 5.0f + layoutScaleContext.textWidthPhysical(this.font, this.labelText.effective(), this.textSize) + (this.paddingX * 2.0f), this.iconSize + this.font.metrics().lineHeight() + (this.paddingY * 2.0f));
        this.clickable.setDimensions(x(), y(), width(), height());
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickable.animate(weightedEngine);
        this.activeAnimator.state(this.active).animate(weightedEngine);
    }

    public boolean isActive() {
        return this.active;
    }
}
