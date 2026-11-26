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

public class LabeledIconButton extends AbstractWidget {
    public final MsdfFont font;
    public final GlTexture icon;
    public final float cornerRadius;
    public final float paddingX;
    public final float paddingY;
    public final int iconSize;
    public final Translation label;
    public final ClickableBehavior clickBehavior = new ClickableBehavior();
    public final int textSize = 12;

    public LabeledIconButton(Translation translation, Runnable runnable, MsdfFont msdfFont, GlTexture texture, float f, float f2, float f3, int i) {
        this.label = translation;
        this.font = msdfFont;
        this.icon = texture;
        this.cornerRadius = f;
        this.paddingX = f2;
        this.paddingY = f3;
        this.iconSize = i;
        this.clickBehavior.clickCallback(() -> {
            if (runnable != null) {
                runnable.run();
            }
        });
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        float fWidth = width();
        float fHeight = height();
        ToggleAnimator hoverAnimation = this.clickBehavior.hoverAnimation();
        int iInterpolate = colorStack.interpolate(colorStack.computeColor(palette.surfaceBackground().tone(700).argb(), 0.0f), colorStack.computeColor(palette.surfaceBackground().tone(700).argb(), 140), hoverAnimation);
        int iInterpolate2 = colorStack.interpolate(colorStack.computeColor(palette.text().tone(500).argb()), colorStack.computeColor(palette.text().tone(400).argb()), hoverAnimation);
        drawCtx.fillOutlinedRoundedRect(x(), y(), fWidth, fHeight, this.cornerRadius, 2.5f, colorStack.computeColor(palette.surfaceOutline().tone(600).argb()), iInterpolate);
        drawCtx.texture(this.icon, x() + this.paddingX, (y() + (fHeight / 2.0f)) - (this.iconSize / 2), this.iconSize, this.iconSize, colorStack.computeColor(iInterpolate2));
        drawCtx.text(this.font, this.label.effective(), 12, x() + this.paddingX + this.iconSize + 5.0f, (y() + (fHeight / 2.0f)) - (this.font.getHeight(12.0f) / 2.0f), iInterpolate2);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        return this.clickBehavior.handleInput(inputEventContext, z);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.iconSize + 5.0f + layoutScaleContext.textWidthPhysical(this.font, this.label.effective(), 12) + (this.paddingX * 2.0f), Math.max(this.iconSize, this.font.metrics().lineHeight()) + (this.paddingY * 2.0f));
        this.clickBehavior.setDimensions(x(), y(), width(), height());
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickBehavior.animate(weightedEngine);
    }
}
