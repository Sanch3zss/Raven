package sanch3zss.raven.ui.component.button;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.MsdfFont;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.TooltipService;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.hud.AbstractWidget;
import sanch3zss.raven.ui.menu.MenuStyle;

public class IconButtonWidget extends AbstractWidget {
    public final GlTexture icon;
    public final MsdfFont iconFont;
    public final String glyph;
    public final int glyphSize;
    public final float iconWidth;
    public final float iconHeight;
    public final ClickableBehavior clickable = new ClickableBehavior();
    public Translation tooltipText = null;
    public GlTexture tooltipIcon = null;
    public boolean tooltipShown = false;
    public int color = -1;

    public IconButtonWidget(GlTexture texture, Runnable runnable, float f, float f2) {
        this(texture, null, null, 0, runnable, f, f2);
    }

    public IconButtonWidget(MsdfFont iconFont, String glyph, int glyphSize, Runnable runnable, float f, float f2) {
        this(null, iconFont, glyph, glyphSize, runnable, f, f2);
    }

    private IconButtonWidget(GlTexture texture, MsdfFont iconFont, String glyph, int glyphSize,
                             Runnable runnable, float f, float f2) {
        this.clickable.clickCallback(runnable);
        this.icon = texture;
        this.iconFont = iconFont;
        this.glyph = glyph;
        this.glyphSize = glyphSize;
        this.iconWidth = f;
        this.iconHeight = f2;
    }

    public IconButtonWidget tooltip(Translation translation, GlTexture texture) {
        this.tooltipText = translation;
        this.tooltipIcon = texture;
        return this;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        DrawEngine drawEngine = drawCtx.drawEngine();
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawEngine.colorStack();
        if (this.color == -1) {
            this.color = palette.text().tone(500).argb();
        }
        int color = colorStack.computeColor(this.color);
        int iconColor = colorStack.interpolate(color, colorStack.brighten(color, 0.8f), this.clickable.hoverAnimation());
        if (this.icon != null) {
            drawCtx.texture(this.icon, x(), y(), this.iconWidth, this.iconHeight, iconColor);
        } else {
            MenuStyle.drawCenteredIcon(drawCtx, this.iconFont, this.glyph, this.glyphSize,
                    x(), y(), this.iconWidth, this.iconHeight, iconColor);
        }
        TooltipService tooltipService = Raven.INSTANCE.menuWindow().tooltipService();
        if (tooltipService != null && tooltipService.isOwnedBy(this) && this.tooltipShown) {
            tooltipService.updateAnchor(this, bounds(), drawCtx);
        }
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = this.clickable.handleInput(inputEventContext, z);
        boolean zState = this.clickable.hoverAnimation().state();
        TooltipService tooltipService = Raven.INSTANCE.menuWindow().tooltipService();
        if ((inputEventContext.inputEvent() instanceof CursorMoveInput) && tooltipService != null && this.tooltipText != null) {
            if (zState && !this.tooltipShown) {
                this.tooltipShown = true;
                tooltipService.show(this, bounds(), this.tooltipText, this.tooltipIcon);
            } else if (!zState && this.tooltipShown) {
                this.tooltipShown = false;
                tooltipService.hide(this);
            }
        }
        return zHandleInput;
    }

    public WidgetBounds bounds() {
        return new WidgetBounds(x(), y(), width(), height());
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        setSize(this.iconWidth, this.iconHeight);
        this.clickable.setDimensions(x(), y(), width(), height());
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.clickable.animate(weightedEngine);
    }

    public void setColor(int i) {
        this.color = i;
    }
}
