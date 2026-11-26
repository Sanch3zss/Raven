package sanch3zss.raven.ui.component.control;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.AbstractWidget;

public class IconLabelBadge extends AbstractWidget {
    public final MsdfFont font = Fonts.INTER_EXTRA_BOLD.get();
    static final int textSize = 9;
    static final float padding = 6.0f;
    public final Translation label;
    public final GlTexture icon;
    public float width;
    public Integer baseColor;
    public Integer textColor;

    public IconLabelBadge(GlTexture texture, Translation translation) {
        this.label = translation;
        this.icon = texture;
    }

    @Override
    public void render(DrawCtx drawCtx) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.colorStack();
        int iIntValue = this.baseColor != null ? this.baseColor.intValue() : palette.accent().argb();
        int iIntValue2 = this.textColor != null ? this.textColor.intValue() : iIntValue;
        float height = this.font.getHeight(9.0f);
        float fHeight = height() / 2.0f;
        drawCtx.fillRoundedRect(x(), y(), width(), height(), 5.0f, colorStack.computeColor(iIntValue, 0.15f));
        drawCtx.textureVerticalC(this.icon, x() + padding, y() + fHeight, 10, 10, colorStack.computeColor(iIntValue2));
        drawCtx.text(this.font, this.label.effective().toUpperCase(), textSize, x() + padding + 10.0f + 4.0f, (y() + fHeight) - (height / 2.0f), colorStack.computeColor(iIntValue2));
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.width = 26.0f + layoutScaleContext.textWidthPhysical(this.font, this.label.effective().toUpperCase(), textSize);
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return 19.0f;
    }

    public void setBaseColor(Integer num) {
        this.baseColor = num;
    }

    public void setTextColor(Integer num) {
        this.textColor = num;
    }
}
