package sanch3zss.raven.ui.hud.impl;

import sanch3zss.raven.ui.hud.DragRenderContext;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.feature.server.ServerUtil;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class CoordsWidget extends Draggable {
    static final float widgetHeight = 20.0f;
    static final int iconSize = 8;
    static final float groupSpacing = 6.0f;
    static final float columnSpacing = 6.0f;
    static final float cornerRadius = 6.0f;
    static final float copyButtonHeight = 14.0f;
    static final float buttonCornerRadius = 4.0f;
    static final float buttonInnerPadding = 4.0f;
    static final float copyTextGap = 3.0f;
    static final float labelFontSize = 8.5f;
    static final float valueFontSize = 8.5f;
    static final float defaultMargin = 10.0f;
    static final float horizontalPadding = 7.0f;
    static final long copiedDurationMs = 700;
    static final String[] valueLabels = {"X", "Y", "Z", "BPS", "TPS"};
    public final GlTexture copyIcon;
    public final GlTexture checkmarkIcon;
    public final MsdfFont coordFont;

    public final MsdfFont buttonFont;
    public final ToggleAnimator expandAnimator;
    public final ToggleAnimator buttonHoverAnimator;
    public final ToggleAnimator copiedAnimator;

    public final WidgetBounds copyButtonBounds;
    public float width;
    public float height;
    public long copiedUntil;

    public CoordsWidget(BooleanSupplier booleanSupplier) {
        super("Coords", booleanSupplier);
        this.copyIcon = new GlTexture(new ClasspathResource("/icons/menu/new/copy.png"));
        this.checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
        this.coordFont = Fonts.INTER_EXTRA_BOLD.get();
        this.buttonFont = Fonts.INTER_BOLD.get();
        this.expandAnimator = ToggleAnimator.times(2, 90, Easings.LINEAR);
        this.buttonHoverAnimator = new ToggleAnimator(180, Easings.LINEAR);
        this.copiedAnimator = new ToggleAnimator(220, Easings.EASE_OUT_CUBIC);
        this.copyButtonBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.x = defaultMargin;
        this.y = defaultMargin;
    }

    @Override
    public void layout(DragRenderContext dragRenderContext) {
        if (isVisible()) {
            String[] values = getDisplayValues();
            float width = horizontalPadding * 2.0f;
            for (int i = 0; i < valueLabels.length; i++) {
                width = width + this.coordFont.getWidth(valueLabels[i], labelFontSize)
                        + this.coordFont.getWidth(" ", valueFontSize)
                        + this.coordFont.getWidth(values[i], valueFontSize);
                if (i != valueLabels.length - 1) {
                    width += 6.0f;
                }
            }
            float width2 = this.buttonFont.getWidth(Lang.WIDGET_COORDS_COPY.effective(), defaultMargin);
            float width3 = 21.0f + width2 + ((this.buttonFont.getWidth(Lang.WIDGET_COORDS_COPIED.effective(), defaultMargin) - width2) * this.copiedAnimator.smoothAnimation());
            this.copyButtonBounds.withSize(width3, copyButtonHeight);
            this.width = width + ((6.0f + width3)
                    * Math.min(this.expandAnimator.smoothAnimation(), 1.0f));
            this.height = widgetHeight;
        }
    }

    @Override
    public void render(DragRenderContext dragRenderContext) {
        if (isVisible()) {
            DrawEngine drawEngine = dragRenderContext.drawEngine();
            PoseStack matrixStack = dragRenderContext.matrixStack();
            ColorStack colorStack = drawEngine.colorStack();
            ThemePalette palette = dragRenderContext.theme().palette();
            float fHeight = this.y + (height() / 2.0f);
            drawGlassPanel(dragRenderContext);
            String[] values = getDisplayValues();
            float width = this.x + horizontalPadding;
            int textColor = colorStack.computeColor(0xFFFFFFFF);
            int labelColor = colorStack.computeColor(palette.accent().argb());
            float textY = fHeight - (this.coordFont.getHeight(valueFontSize) / 2.0f);
            for (int i = 0; i < valueLabels.length; i++) {
                String str = valueLabels[i];
                String strValueOf = values[i];
                drawEngine.msdfFont(matrixStack.last().pose(), this.coordFont, str, width, textY,
                        labelFontSize, 0.05f, labelColor);
                float width2 = width + this.coordFont.getWidth(str, labelFontSize) + this.coordFont.getWidth(" ", valueFontSize);
                drawEngine.msdfFont(matrixStack.last().pose(), this.coordFont, strValueOf, width2, textY,
                        valueFontSize, 0.05f, textColor);
                width = width2 + this.coordFont.getWidth(strValueOf, valueFontSize);
                if (i != valueLabels.length - 1) {
                    width += 6.0f;
                }
            }
            this.copyButtonBounds.withPosition(width + 6.0f, fHeight - (this.copyButtonBounds.height() / 2.0f));
            float fMax = Math.max(this.expandAnimator.smoothAnimation() - 1.0f, 0.0f);
            float fSmoothAnimation = this.copiedAnimator.smoothAnimation();
            float f = 1.0f - fSmoothAnimation;
            colorStack.push();
            colorStack.alpha(fMax);
            drawEngine.roundedRectangle(matrixStack.last().pose(), this.copyButtonBounds.x(), this.copyButtonBounds.y(), this.copyButtonBounds.width(), this.copyButtonBounds.height(), buttonCornerRadius, colorStack.computeColor(11843002, 0.1f + (0.1f * this.buttonHoverAnimator.smoothAnimation())));
            float fX = this.copyButtonBounds.x() + 4.0f;
            float fY = this.copyButtonBounds.y() + (this.copyButtonBounds.height() / 2.0f);
            float f2 = fX + defaultMargin + copyTextGap;
            float fY2 = (this.copyButtonBounds.y() + (this.copyButtonBounds.height() / 2.0f)) - (this.buttonFont.getHeight(defaultMargin) / 2.0f);
            colorStack.push();
            colorStack.alpha(f * fMax);
            drawEngine.textureVerticalC(matrixStack.last().pose(), this.copyIcon, fX, fY, iconSize, iconSize, colorStack.computeColor(palette.text().tone(100).argb()));
            drawEngine.msdfFont(matrixStack.last().pose(), this.buttonFont, Lang.WIDGET_COORDS_COPY.effective(), f2, fY2, defaultMargin, 0.05f, colorStack.computeColor(palette.text().tone(100).argb()));
            colorStack.pop();
            colorStack.push();
            colorStack.alpha(fSmoothAnimation * fMax);
            drawEngine.textureVerticalC(matrixStack.last().pose(), this.checkmarkIcon, fX, fY, iconSize, iconSize, colorStack.computeColor(palette.text().tone(100).argb()));
            drawEngine.msdfFont(matrixStack.last().pose(), this.buttonFont, Lang.WIDGET_COORDS_COPIED.effective(), f2, fY2, defaultMargin, 0.05f, colorStack.computeColor(palette.text().tone(100).argb()));
            colorStack.pop();
            colorStack.pop();
        }
    }

    private void drawGlassPanel(DragRenderContext context) {
        DrawEngine drawEngine = context.drawEngine();
        ColorStack colors = drawEngine.colorStack();
        int blurTexture = Raven.INSTANCE.widgetStack().hudBlurTexture();
        int tint = mixRgb(sanch3zss.raven.ui.menu.MenuStyle.surfaceColor(context.theme(), 0xFF111217), context.theme().palette().accentStart().argb(), 0.075f);
        HudGlassPanel.drawShadows(drawEngine, context.matrixStack().last().pose(),
                this.x, this.y, this.width, this.height, cornerRadius,
                25.5f, colors.computeColor(tint, 1.5f));
        if (blurTexture != 0) {
            drawEngine.roundedGlassPanel(context.matrixStack().last().pose(),
                    this.x, this.y, this.width, this.height, cornerRadius, 0.0f,
                    0.8f, colors.computeColor(tint, 1.0f),
                    0, blurTexture);
        } else {
            drawEngine.roundedRectangle(context.matrixStack().last().pose(),
                    this.x, this.y, this.width, this.height, cornerRadius,
                    colors.computeColor(tint, 0.96f));
        }
    }

    private static int mixRgb(int base, int tint, float amount) {
        float inverse = 1.0f - amount;
        int red = Math.round(((base >> 16) & 0xFF) * inverse + ((tint >> 16) & 0xFF) * amount);
        int green = Math.round(((base >> 8) & 0xFF) * inverse + ((tint >> 8) & 0xFF) * amount);
        int blue = Math.round((base & 0xFF) * inverse + (tint & 0xFF) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    @Override
    public boolean click(MouseButtonInput2 mouseButtonInput2, boolean z) {
        boolean zIsWithinBounds = isWithinScaledBounds(mouseButtonInput2, this.copyButtonBounds.x(), this.copyButtonBounds.y(), this.copyButtonBounds.width(), this.copyButtonBounds.height());
        if (mouseButtonInput2.button() != 0 || !mouseButtonInput2.press() || !z || !zIsWithinBounds || mouseButtonInput2.intercepted()) {
            return false;
        }
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null) {
            return true;
        }
        StringUtil.copyToClipboard(((int) player.getX()) + ", " + ((int) player.getY()) + ", " + ((int) player.getZ()));
        this.copiedUntil = System.currentTimeMillis() + copiedDurationMs;
        Raven.INSTANCE.notificationRepository().post(NotificationType.INFO, Component.literal(Lang.WIDGET_COORDS_COPIED_IN_CLIPBOARD.effective()), 3L, TimeUnit.SECONDS);
        return true;
    }

    @Override
    public boolean cursor(MouseMoveInput mouseMoveInput, boolean z) {
        boolean zIsWithinBounds = isWithinScaledBounds(mouseMoveInput, this.copyButtonBounds.x(), this.copyButtonBounds.y(), this.copyButtonBounds.width(), this.copyButtonBounds.height());
        boolean z2 = z && !mouseMoveInput.intercepted();
        this.expandAnimator.state(z2);
        boolean z3 = zIsWithinBounds && this.expandAnimator.state() && !mouseMoveInput.intercepted();
        this.buttonHoverAnimator.state(z3);
        return z2 || z3;
    }

    @Override
    public void animate(WeightedEngine weightedEngine) {
        this.expandAnimator.animate(weightedEngine);
        this.buttonHoverAnimator.animate(weightedEngine);
        this.copiedAnimator.state(System.currentTimeMillis() < this.copiedUntil).animate(weightedEngine);
    }

    @Override
    public void update() {
    }

    @Override
    public float width() {
        return this.width;
    }

    @Override
    public float height() {
        return this.height;
    }

    @Override
    public boolean hasTooltip() {
        return false;
    }

    public static int[] getPlayerCoords() {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        return player == null ? new int[]{0, 0, 0} : new int[]{(int) player.getX(), (int) player.getY(), (int) player.getZ()};
    }

    private static String[] getDisplayValues() {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null) {
            return new String[]{"0", "0", "0", "0.0", formatDecimal(ServerUtil.TPS)};
        }
        double movedX = player.getX() - player.xo;
        double movedZ = player.getZ() - player.zo;
        double bps = Math.hypot(movedX, movedZ) * 20.0d;
        return new String[]{
                String.valueOf((int) player.getX()),
                String.valueOf((int) player.getY()),
                String.valueOf((int) player.getZ()),
                formatDecimal(bps),
                formatDecimal(ServerUtil.TPS)
        };
    }

    private static String formatDecimal(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
