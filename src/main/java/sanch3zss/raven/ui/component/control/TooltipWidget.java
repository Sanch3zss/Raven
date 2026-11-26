package sanch3zss.raven.ui.component.control;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.AbstractWidget;
import sanch3zss.raven.util.math.interpolation.Easings;

public class TooltipWidget extends AbstractWidget {
    static final float cornerRadius = 7.0f;
    static final float outlineThickness = 2.5f;
    static final float contentPadding = 6.0f;
    static final float spacing = 4.0f;
    static final float iconSize = 12.0f;
    static final float iconSpacing = 6.0f;
    public final MsdfFont font = Fonts.INTER_BOLD.get();
    public final ToggleAnimator visibilityAnimator = new ToggleAnimator(100, Easings.LINEAR);
    public final WidgetBounds anchorBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public final WidgetBounds tooltipBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
    public Translation text = Translation.clearText("Tooltip!");
    public GlTexture icon = new GlTexture(new ClasspathResource("/icons/menu/new/smile.png"));
    public boolean shown = false;

    public void show(Translation translation, GlTexture texture, WidgetBounds widgetBounds) {
        this.text = translation;
        this.icon = texture;
        this.anchorBounds.withPosition(widgetBounds.x(), widgetBounds.y()).withSize(widgetBounds.width(), widgetBounds.height());
        this.shown = true;
        this.visibilityAnimator.state(true);
    }

    public void hide() {
        this.shown = false;
        this.visibilityAnimator.state(false);
    }

    public boolean isVisible() {
        return this.shown || !this.visibilityAnimator.isZero();
    }

    @Override
    public void render(DrawCtx drawCtx) {
        if (this.visibilityAnimator.isZero()) {
            return;
        }
        ColorStack colorStack = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        float fSmoothAnimation = this.visibilityAnimator.smoothAnimation();
        colorStack.push();
        colorStack.alpha(fSmoothAnimation);
        drawCtx.bloom(this.tooltipBounds.x(), this.tooltipBounds.y(), this.tooltipBounds.width(), this.tooltipBounds.height(), 20.0f, sanch3zss.raven.render.gl.FramebufferUtil.getColorAttachmentId(Raven.INSTANCE.windowController().bloom().getBloomFramebuffer()));
        drawCtx.fillOutlinedRoundedRect(this.tooltipBounds.x(), this.tooltipBounds.y(), this.tooltipBounds.width(), this.tooltipBounds.height(), cornerRadius, outlineThickness, colorStack.computeColor(palette.surfaceOutline().tone(400).argb()), colorStack.computeColor(palette.surfaceBackground().tone(600).argb()));
        float fX = this.tooltipBounds.x() + 6.0f;
        float fY = this.tooltipBounds.y() + (this.tooltipBounds.height() / 2.0f);
        if (this.icon != null) {
            drawCtx.texture(this.icon, fX, fY - 6.0f, iconSize, iconSize, colorStack.computeColor(9934746));
            fX += 18.0f;
        }
        drawCtx.text(this.font, this.text.effective(), 11, fX, fY - (this.font.getHeight(11.0f) / 2.0f), colorStack.computeColor(palette.text().tone(100).argb()));
        colorStack.pop();
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        if (isVisible()) {
            float fTextWidthPhysical = layoutScaleContext.textWidthPhysical(this.font, this.text.effective(), 11);
            float height = this.font.getHeight(11.0f);
            boolean z = this.icon != null;
            float f = iconSize + fTextWidthPhysical + (z ? 18.0f : 0.0f);
            this.tooltipBounds.withSize(f, 8.0f + Math.max(height, z ? iconSize : 0.0f));
            this.tooltipBounds.withPosition((this.anchorBounds.x() + (this.anchorBounds.width() / 2.0f)) - (f / 2.0f), (this.anchorBounds.y() - this.tooltipBounds.height()) - 10.0f);
        }
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.visibilityAnimator.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void updateAnchor(WidgetBounds widgetBounds) {
        this.anchorBounds.withPosition(widgetBounds.x(), widgetBounds.y()).withSize(widgetBounds.width(), widgetBounds.height());
    }

    @Override
    public void collectBloomElements(RenderCommandQueue renderCommandQueue) {
        renderCommandQueue.record(drawCtx -> {
            if (this.visibilityAnimator.isZero()) {
                return;
            }
            ColorStack colorStack = drawCtx.drawEngine().colorStack();
            ThemePalette palette = drawCtx.theme().palette();
            colorStack.push();
            colorStack.alpha(this.visibilityAnimator.smoothAnimation());
            drawCtx.fillOutlinedRoundedRect(this.tooltipBounds.x(), this.tooltipBounds.y(), this.tooltipBounds.width(), this.tooltipBounds.height(), cornerRadius, outlineThickness, colorStack.computeColor(palette.surfaceOutline().tone(400).argb()), colorStack.computeColor(palette.surfaceBackground().tone(600).argb()));
            colorStack.pop();
        });
        super.collectBloomElements(renderCommandQueue);
    }
}
