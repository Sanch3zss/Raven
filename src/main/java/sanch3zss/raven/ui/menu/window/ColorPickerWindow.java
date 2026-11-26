package sanch3zss.raven.ui.menu.window;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.gl.StencilBufferUtil;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;

import org.joml.Vector4f;
import org.lwjgl.opengl.GL33;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;import sanch3zss.raven.render.animation.AnimatedFloat;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.button.LoadingButtonWidget;
import sanch3zss.raven.ui.component.control.*;
import sanch3zss.raven.ui.component.input.TextInputField;
import sanch3zss.raven.ui.hud.AbstractWindow;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ColorPickerWindow extends AbstractWindow {
    private enum ValueMode {
        HEX, RGB, HSB
    }

    public final GlTexture clickIcon;
    public final GlTexture plusIcon;
    public final GlTexture checkmarkIcon;
    private static GlTexture hueTexture;
    public final MsdfFont font;
    public final MsdfFont iconFont;
    static final int animationDuration = 260;
    static final float cornerRadius = 10.0f;
    static final float outlineWidth = 1.0f;
    static final float padding = 8.0f;
    static final float svBoxWidth = 132.0f;
    static final float svBoxHeight = 112.0f;
    static final float barWidth = 8.0f;
    static final float rgbaRowHeight = 15.0f;
    static final float separatorWidth = 1.5f;
    static final float headerHeight = 12.0f;
    static final float labelColumnWidth = 60.0f;
    static final float valueColumnWidth = 40.0f;
    static final float margin = 10.0f;
    static final float swatchRadius = 4.0f;
    static final float half = 2.0f;
    static final float baseHeight = 190.0f;
    static final float rowHeightIncrement = 23.0f;
    static final int maxSwatches = 29;
    static final float snapThreshold = 50.0f;
    public final WidgetBounds svBounds;
    public final WidgetBounds hueBounds;
    public final WidgetBounds alphaBounds;
    private final ClickableBehavior[] modeClickables = {
            new ClickableBehavior(), new ClickableBehavior(), new ClickableBehavior()
    };
    private ValueMode valueMode = ValueMode.HEX;
    private final AnimatedFloat modeAnimation = new AnimatedFloat(240, Easings.EASE_IN_OUT_CUBIC);
    private final TextInputField valueField;

    public final WidgetBounds addSwatchBounds;
    public final WidgetBounds dragBarBounds;
    public final ToggleAnimator openAnimator;
    public List<ColorSwatch> swatches;
    public float hue;
    public float saturation;
    public float brightness;
    public float alpha;
    public boolean draggingSV;

    public boolean draggingHue;

    public boolean draggingAlpha;

    public boolean draggingSide;
    public boolean onRightSide;
    private boolean showAlpha = true;
    private float anchorX = Float.NaN;
    private float anchorY = Float.NaN;
    public final ClickableBehavior addSwatchClickable;

    public final LoadingButtonWidget closeButton;
    public final MenuWindow menuWindow;

    public ColorSwatch selectedSwatch;
    public Consumer<Integer> colorCallback;
    public Consumer<Float> alphaCallback;

    public Runnable applyAction;

    public ColorPickerWindow(float f, float f2, float f3, float f4) {
        super(160.0f, baseHeight);
        this.clickIcon = new GlTexture(new ClasspathResource("/icons/menu/new/click.png"));
        this.plusIcon = new GlTexture(new ClasspathResource("/icons/menu/new/plus.png"));
        this.checkmarkIcon = new GlTexture(new ClasspathResource("/icons/menu/new/checkmark.png"));
        this.font = Fonts.INTER_EXTRA_BOLD.get();
        this.iconFont = Fonts.INTER_SEMIBOLD.get();
        this.valueField = new TextInputField(this.font, 9);
        this.valueField.maxLength = 20;
        this.valueField.changeCallback = this::applyValue;
        this.svBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.hueBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.alphaBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.addSwatchBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.dragBarBounds = new WidgetBounds(0.0f, 0.0f, 0.0f, 0.0f);
        this.openAnimator = new ToggleAnimator(animationDuration, 180,
                Easings.EASE_IN_OUT_CUBIC, Easings.EASE_IN_OUT_CUBIC);
        this.swatches = null;
        this.onRightSide = true;
        this.addSwatchClickable = new ClickableBehavior();
        this.selectedSwatch = null;
        int iArgb = Raven.INSTANCE.theme().palette().accentStart().argb();
        if (this.swatches == null) {
            this.swatches = new ArrayList(Arrays.asList(
                    new ColorSwatch(ColorValue.fromHex("F4F4F6").argb(), true),
                    new ColorSwatch(ColorValue.fromHex("F05A61").argb(), true),
                    new ColorSwatch(ColorValue.fromHex("F58B45").argb(), true),
                    new ColorSwatch(ColorValue.fromHex("EBCB4B").argb(), true),
                    new ColorSwatch(ColorValue.fromHex("51C98A").argb(), true),
                    new ColorSwatch(ColorValue.fromHex("48C7C4").argb(), true),
                    new ColorSwatch(ColorValue.fromHex("5B83EE").argb(), true),
                    new ColorSwatch(ColorValue.fromHex("9664D5").argb(), true),
                    new ColorSwatch(iArgb, true),
                    new ColorSwatch(ColorValue.fromHex("E7BC37").argb(), true),
                    new ColorSwatch(ColorValue.fromHex("8673FA").argb(), true),
                    new ColorSwatch(ColorValue.fromHex("B8FF8C").argb(), true)));
        }
        this.closeButton = new LoadingButtonWidget(Lang.CLOSE, Math.round(width() - 24.0f), 25, this::onCloseClicked, 7.0f, true);
        this.hue = f;
        this.saturation = f2;
        this.brightness = f3;
        this.alpha = f4;
        this.menuWindow = Raven.INSTANCE.menuWindow();
        this.clickIcon.setDimensions(12, 12);
        this.addSwatchClickable.clickCallback(() -> {
            if (this.swatches.size() < maxSwatches) {
                addSwatch();
            }
        });
        for (int i = 0; i < this.modeClickables.length; i++) {
            int modeIndex = i;
            this.modeClickables[i].clickCallback(() -> {
                this.valueMode = ValueMode.values()[modeIndex];
                this.valueField.focused(false);
                this.modeAnimation.destination(modeIndex);
            });
        }
        for (ColorSwatch colorSwatch : this.swatches) {
            colorSwatch.clickable().clickCallback(() -> {
                selectSwatch(colorSwatch);
            });
            if (!colorSwatch.system()) {
                colorSwatch.clickable().rightClickCallback(() -> {
                    removeSwatch(colorSwatch);
                });
            }
        }
    }

    public void selectSwatch(ColorSwatch colorSwatch) {
        clearSelection();
        this.selectedSwatch = colorSwatch;
        int iColor = colorSwatch.color();
        int i = (iColor >> 16) & StencilBufferUtil.STENCIL_MASK;
        int i2 = (iColor >> 8) & StencilBufferUtil.STENCIL_MASK;
        int i3 = iColor & StencilBufferUtil.STENCIL_MASK;
        int i4 = (iColor >> 24) & StencilBufferUtil.STENCIL_MASK;
        float[] fArrRGBtoHSB = Color.RGBtoHSB(i, i2, i3, (float[]) null);
        this.hue = fArrRGBtoHSB[0];
        this.saturation = fArrRGBtoHSB[1];
        this.brightness = fArrRGBtoHSB[2];
        this.alpha = i4 / 255.0f;
        colorSwatch.selectAnim().state(true);
        emitColorAndAlpha();
    }

    void applyValue(String text) {
        if (this.valueMode == ValueMode.HEX) {
            String hex = text.startsWith("#") ? text.substring(1) : text;
            if (hex.length() != 6) {
                return;
            }
            int rgb;
            try {
                rgb = Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                return;
            }
            float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
            this.hue = hsb[0];
            this.saturation = hsb[1];
            this.brightness = hsb[2];
        } else {
            String[] parts = text.replaceAll("[^0-9]+", " ").trim().split(" ");
            if (parts.length != 3) {
                return;
            }
            int[] numbers = new int[3];
            try {
                for (int i = 0; i < 3; i++) {
                    numbers[i] = Integer.parseInt(parts[i]);
                }
            } catch (NumberFormatException e) {
                return;
            }
            if (this.valueMode == ValueMode.RGB) {
                float[] hsb = Color.RGBtoHSB(Math.min(numbers[0], 255), Math.min(numbers[1], 255),
                        Math.min(numbers[2], 255), null);
                this.hue = hsb[0];
                this.saturation = hsb[1];
                this.brightness = hsb[2];
            } else {
                this.hue = Math.min(numbers[0], 360) / 360.0f;
                this.saturation = Math.min(numbers[1], 100) / 100.0f;
                this.brightness = Math.min(numbers[2], 100) / 100.0f;
            }
        }
        clearSelection();
        emitColorAndAlpha();
    }

    public void removeSwatch(ColorSwatch colorSwatch) {
        if (colorSwatch.system()) {
            return;
        }
        if (this.selectedSwatch == colorSwatch) {
            clearSelection();
        }
        this.swatches.remove(colorSwatch);
        setSize(width(), computeHeight(this.swatches.size() + 1));
    }

    public void onCloseClicked() {
        closePicker();
    }

    public void updateColor(float f, float f2, float f3, float f4) {
        this.hue = f;
        this.saturation = f2;
        this.brightness = f3;
        this.alpha = f4;
    }

    public static float computeHeight(int i) {
        return baseHeight;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        updatePosition();
        updateBounds();
        float rowY = this.svBounds.y() + this.svBounds.height() + 27.0f;
        this.valueField.listen(x() + padding + 15.0f, rowY, width() - padding * 2.0f - 15.0f, 18.0f,
                width() - padding * 2.0f - 21.0f, x() + padding + 17.0f, layoutScaleContext.scaleFactor());
        super.layout(layoutScaleContext);
    }

    void updatePosition() {
        if (!Float.isNaN(this.anchorX) && !Float.isNaN(this.anchorY)) {
            float dpi = Math.max(Raven.INSTANCE.windowController().dpiScaleFactor(), 0.01f);
            float screenWidth = sanch3zss.raven.render.engine.ScreenResolution.resolution().screenWidth() / dpi;
            float screenHeight = sanch3zss.raven.render.engine.ScreenResolution.resolution().screenHeight() / dpi;
            float clampedX = Math.max(5.0f, Math.min(this.anchorX, screenWidth - width() - 5.0f));
            float clampedY = Math.max(5.0f, Math.min(this.anchorY, screenHeight - height() - 5.0f));
            setPosition(clampedX, clampedY);
            return;
        }
        if (this.onRightSide) {
            setPosition(this.menuWindow.x() + MenuWindow.MENU_WIDTH + margin, this.menuWindow.y());
        } else {
            setPosition((this.menuWindow.x() - width()) - margin, this.menuWindow.y());
        }
    }

    public ColorPickerWindow anchor(float anchorX, float anchorY) {
        this.anchorX = anchorX;
        this.anchorY = anchorY;
        return this;
    }

    void updateBounds() {
        float svWidth = this.showAlpha ? svBoxWidth - barWidth - 4.0f : svBoxWidth;
        this.svBounds.withPosition(x() + padding, y() + padding).withSize(svWidth, svBoxHeight);
        this.hueBounds.withPosition(this.svBounds.x() + this.svBounds.width() + 4.0f, this.svBounds.y())
                .withSize(barWidth, svBoxHeight);
        if (this.showAlpha) {
            this.alphaBounds.withPosition(this.hueBounds.x() + barWidth + 4.0f, this.svBounds.y())
                    .withSize(barWidth, svBoxHeight);
        } else {
            this.alphaBounds.withPosition(0.0f, 0.0f).withSize(0.0f, 0.0f);
        }
        this.dragBarBounds.withPosition(x(), y()).withSize(width(), 7.0f);
        float modeY = this.svBounds.y() + this.svBounds.height() + 7.0f;
        float modeWidth = (width() - padding * 2.0f) / this.modeClickables.length;
        for (int i = 0; i < this.modeClickables.length; i++) {
            this.modeClickables[i].setDimensions(x() + padding + i * modeWidth, modeY, modeWidth, 16.0f);
        }
        this.checkmarkIcon.setDimensions(12, 12);
        this.clickIcon.setDimensions(12, 12);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        drawCtx = drawCtx.withTheme(MenuStyle.menuTheme(drawCtx.theme()));
        ColorStack colorStack = drawCtx.colorStack();
        ThemePalette palette = drawCtx.theme().palette();
        this.closeButton.baseColor(palette.surfaceBackground().tone(600).argb());
        this.closeButton.textColor(palette.text().tone(100).argb());
        this.closeButton.outlineColor(palette.surfaceOutline().tone(400).argb());
        if (this.openAnimator.isZero()) {
            return;
        }
        colorStack.push();
        if (!this.openAnimator.isOne()) {
            colorStack.alpha(this.openAnimator.smoothAnimation());
        }
        MenuStyle.drawDropdownPanel(drawCtx, x(), y(), width(), height(), cornerRadius);
        renderSaturationBrightness(drawCtx, colorStack);
        renderHueBar(drawCtx, colorStack);
        if (this.showAlpha) {
            renderAlphaBar(drawCtx, Color.HSBtoRGB(this.hue, this.saturation, this.brightness), colorStack);
        }
        renderModeRow(drawCtx);
        renderValueRow(drawCtx, colorStack, palette,
                Color.HSBtoRGB(this.hue, this.saturation, this.brightness));
        renderSwatches(drawCtx, colorStack);
        colorStack.pop();
    }

    private void renderModeRow(DrawCtx drawCtx) {
        float rowY = this.svBounds.y() + this.svBounds.height() + 7.0f;
        float hover = 0.0f;
        for (ClickableBehavior clickable : this.modeClickables) {
            hover = Math.max(hover, clickable.hoverAnimation().smoothAnimation());
        }
        MenuStyle.drawSegmentedControl(drawCtx, this.font, 9, new String[]{"HEX", "RGB", "HSB"},
                x() + padding, rowY, width() - padding * 2.0f, 16.0f,
                this.modeAnimation.animatedValue(), hover);
    }

    private void renderValueRow(DrawCtx drawCtx, ColorStack colors, ThemePalette palette, int rgb) {
        float rowY = this.svBounds.y() + this.svBounds.height() + 27.0f;
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        String value = switch (this.valueMode) {
            case HEX -> String.format("#%06X", rgb & 0x00FFFFFF);
            case RGB -> red + ", " + green + ", " + blue;
            case HSB -> Math.round(this.hue * 360.0f) + "deg, "
                    + Math.round(this.saturation * 100.0f) + "%, "
                    + Math.round(this.brightness * 100.0f) + "%";
        };
        if (!this.valueField.focused() && !this.valueField.text().equals(value)) {
            this.valueField.setTextSilently(value);
        }
        MenuStyle.drawTextInput(drawCtx, this.valueField, this.font, 9, this.valueField.text(), "",
                x() + padding, rowY, width() - padding * 2.0f, 18.0f, 17.0f);
        drawCtx.circle(x() + padding + 8.0f, rowY + 9.0f, 4.0f,
                colors.computeColor((rgb & 0x00FFFFFF) | 0xFF000000));
    }

    private void renderSwatches(DrawCtx drawCtx, ColorStack colors) {
        float swatchY = this.svBounds.y() + this.svBounds.height() + 50.0f;
        float swatchSize = 13.0f;
        float gap = 4.0f;
        int count = Math.min(8, this.swatches.size());
        for (int i = 0; i < count; i++) {
            ColorSwatch swatch = this.swatches.get(i);
            float swatchX = x() + padding + i * (swatchSize + gap);
            float selected = swatch.selectAnim().smoothAnimation();
            if (selected > 0.01f) {
                drawCtx.circle(swatchX + swatchSize / 2.0f, swatchY + swatchSize / 2.0f,
                        swatchSize / 2.0f + 1.5f, colors.computeColor(0xFFFFFFFF, 0.9f * selected));
                drawCtx.circle(swatchX + swatchSize / 2.0f, swatchY + swatchSize / 2.0f,
                        swatchSize / 2.0f, colors.computeColor(0xFF1C1C1E, selected));
            }
            drawCtx.circle(swatchX + swatchSize / 2.0f, swatchY + swatchSize / 2.0f,
                    swatchSize / 2.0f - 2.0f * selected, colors.computeColor(swatch.color() | 0xFF000000));
            swatch.clickable().setDimensions(swatchX, swatchY, swatchSize, swatchSize);
        }
        for (int i = count; i < this.swatches.size(); i++) {
            this.swatches.get(i).clickable().setDimensions(0.0f, 0.0f, 0.0f, 0.0f);
        }
    }

    float renderRgbaRow(DrawCtx drawCtx, ColorStack colorStack2, ThemePalette palette2) {
        float fY = this.hueBounds.y() + this.hueBounds.height() + 12.0f;
        drawCtx.fillOutlinedRoundedRect(this.svBounds.x(), fY, width() - 24.0f, rgbaRowHeight, barWidth, outlineWidth, colorStack2.computeColor(palette2.surfaceOutline().tone(300).argb()), colorStack2.computeColor(palette2.surfaceBackground().tone(900).argb(), 0.0f));
        float fX = this.svBounds.x() + 1.0f;
        drawCtx.fillRoundedRect(fX, fY + 1.0f, 58.0f, 24.0f, new Vector4f(0.0f, 0.0f, barWidth, barWidth), colorStack2.computeColor(palette2.surfaceBackground().tone(600).argb()));
        drawCtx.textureVerticalC(this.clickIcon, this.svBounds.x() + cornerRadius, fY + 13.0f, this.clickIcon.width(), this.clickIcon.height(), colorStack2.computeColor(palette2.text().tone(100).argb()));
        drawCtx.text(this.font, "RGBA", 10, this.svBounds.x() + cornerRadius + this.clickIcon.width() + 4.0f, (fY + 13.0f) - (this.font.getHeight(margin) / half), colorStack2.computeColor(palette2.text().tone(100).argb()));
        float f = (fX + labelColumnWidth) - 3.0f;
        drawSeparator(drawCtx, colorStack2, palette2, f, fY);
        float f2 = f + 1.0f;
        int iHSBtoRGB = Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        int[] iArr = {(iHSBtoRGB >> 16) & StencilBufferUtil.STENCIL_MASK, (iHSBtoRGB >> 8) & StencilBufferUtil.STENCIL_MASK, iHSBtoRGB & StencilBufferUtil.STENCIL_MASK, (int) (clamp01(this.alpha) * 255.0f)};
        for (int i = 0; i < iArr.length; i++) {
            String strValueOf = String.valueOf(iArr[i]);
            drawCtx.text(this.font, strValueOf, 10, f2 + ((valueColumnWidth - drawCtx.textWidthPhysical(this.font, strValueOf, 10)) / half), (fY + 13.0f) - (this.font.getHeight(margin) / half), colorStack2.computeColor(palette2.text().tone(100).argb()));
            f2 += valueColumnWidth;
            if (i != iArr.length - 1) {
                drawSeparator(drawCtx, colorStack2, palette2, f2, fY);
                f2 += 1.0f;
            }
        }
        return fY + rgbaRowHeight;
    }

    void drawSeparator(DrawCtx drawCtx, ColorStack colorStack2, ThemePalette palette2, float f, float f2) {
        drawCtx.fillRect(f, f2 + 1.0f, separatorWidth, 24.0f, colorStack2.computeColor(palette2.surfaceOutline().tone(500).argb()));
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        if (!opened() || this.openAnimator.isZero()) {
            return false;
        }
        InputEvent inputEvent = inputEventContext.inputEvent();
        boolean dragging = this.draggingSV || this.draggingHue || this.draggingAlpha || this.draggingSide;
        if (!dragging && this.valueField.handleInput(inputEventContext, z)) {
            return true;
        }
        if (!dragging) {
            boolean modeHandled = false;
            for (int i = this.modeClickables.length - 1; i >= 0; i--) {
                modeHandled |= this.modeClickables[i].handleInput(inputEventContext, z || modeHandled);
            }
            if (modeHandled) {
                return true;
            }
        }
        if (inputEvent instanceof CursorMoveInput) {
            PixelPoint logicalMousePosition = inputEventContext.logicalMousePosition();
            if (handleDrag(logicalMousePosition) || handleSideDrag(logicalMousePosition)) {
                return true;
            }
        }
        if (inputEvent instanceof MouseButtonInput) {
            MouseButtonInput mouseButtonInput = (MouseButtonInput) inputEvent;
            if (mouseButtonInput.button() == 0) {
                if (mouseButtonInput.action().press()
                        && !inputEventContext.inArea(x(), y(), width(), height())) {
                    closePicker();
                    return true;
                }
                if (mouseButtonInput.action().release()) {
                    boolean z2 = this.draggingSV || this.draggingHue || this.draggingAlpha || this.draggingSide;
                    if (this.draggingSide) {
                        this.draggingSide = false;
                    }
                    this.draggingHue = false;
                    this.draggingSV = false;
                    this.draggingAlpha = false;
                    return z2;
                }
                if (inputEventContext.inArea(this.dragBarBounds.x(), this.dragBarBounds.y(), this.dragBarBounds.width(), this.dragBarBounds.height())) {
                    this.draggingSide = true;
                    return true;
                }
                if (inputEventContext.inArea(this.svBounds.x(), this.svBounds.y(), this.svBounds.width(), this.svBounds.height())) {
                    this.draggingSV = true;
                    return true;
                }
                if (inputEventContext.inArea(this.hueBounds.x(), this.hueBounds.y(), this.hueBounds.width(), this.hueBounds.height())) {
                    this.draggingHue = true;
                    return true;
                }
                if (this.showAlpha && inputEventContext.inArea(this.alphaBounds.x(), this.alphaBounds.y(), this.alphaBounds.width(), this.alphaBounds.height())) {
                    this.draggingAlpha = true;
                    updateAlpha(inputEventContext.logicalMousePosition().y());
                    clearSelection();
                    emitColorAndAlpha();
                    return true;
                }
                for (int i = Math.min(8, this.swatches.size()) - 1; i >= 0; i--) {
                    if (this.swatches.get(i).clickable().handleInput(inputEventContext, z)) {
                        return true;
                    }
                }
                if (mouseButtonInput.action().press()
                        && inputEventContext.inArea(x(), y(), width(), height())) {
                    return true;
                }
            }
        }
        if (!this.draggingSV && !this.draggingHue && !this.draggingAlpha && !this.draggingSide) {
            Iterator<ColorSwatch> it = this.swatches.iterator();
            while (it.hasNext()) {
                if (it.next().clickable().handleInput(inputEventContext, z)) {
                    return true;
                }
            }
            if (this.addSwatchClickable.handleInput(inputEventContext, z)) {
                return true;
            }
        }
        if (inputEvent instanceof CursorMoveInput
                && inputEventContext.inArea(x(), y(), width(), height())) {
            return true;
        }
        return super.handleInput(inputEventContext, z);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.menuWindow != null && !this.menuWindow.menuOpen && this.openAnimator.state()) {
            closePicker();
        }
        this.openAnimator.animate(weightedEngine);
        this.modeAnimation.animate(weightedEngine);
        this.valueField.animate(weightedEngine);
        this.addSwatchClickable.animate(weightedEngine);
        for (ClickableBehavior modeClickable : this.modeClickables) {
            modeClickable.animate(weightedEngine);
        }
        for (ColorSwatch colorSwatch : this.swatches) {
            colorSwatch.clickable().animate(weightedEngine);
            colorSwatch.selectAnim().animate(weightedEngine);
        }
        super.animation(weightedEngine);
    }

    boolean handleDrag(PixelPoint point) {
        if (this.draggingSV) {
            updateSaturationBrightness(point.x(), point.y());
            clearSelection();
            emitColorAndAlpha();
            return true;
        }
        if (this.draggingHue) {
            updateHue(point.y());
            clearSelection();
            emitColorAndAlpha();
            return true;
        }
        if (this.draggingAlpha) {
            updateAlpha(point.y());
            clearSelection();
            emitColorAndAlpha();
            return true;
        }
        return false;
    }

    boolean handleSideDrag(PixelPoint point) {
        if (!this.draggingSide) {
            return false;
        }
        float fX = this.menuWindow.x() + (MenuWindow.MENU_WIDTH / half);
        float fX2 = point.x();
        if (this.onRightSide) {
            if (fX2 >= fX - snapThreshold) {
                return true;
            }
            this.onRightSide = false;
            updatePosition();
            updateBounds();
            return true;
        }
        if (fX2 <= fX + snapThreshold) {
            return true;
        }
        this.onRightSide = true;
        updatePosition();
        updateBounds();
        return true;
    }

    public void clearSelection() {
        if (this.selectedSwatch != null) {
            this.selectedSwatch.selectAnim().state(false);
            this.selectedSwatch = null;
        }
    }

    int renderSaturationBrightness(DrawCtx drawCtx, ColorStack colorStack2) {
        int color = colorStack2.computeColor(Color.HSBtoRGB(this.hue, 1.0f, 1.0f));
        float radius = 10.0f;
        int hueColor = colorStack2.computeColor(Color.HSBtoRGB(this.hue, 1.0f, 1.0f) | 0xFF000000);
        LayoutScaleContext layout = drawCtx.layoutContext();
        drawCtx.drawEngine().saturationValue(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(this.svBounds.x()), layout.toPhysical(this.svBounds.y()),
                layout.toPhysical(this.svBounds.width()), layout.toPhysical(this.svBounds.height()),
                layout.toPhysical(radius), hueColor);
        float thumbX = this.svBounds.x() + clamp(this.saturation * this.svBounds.width(), 7.0f, this.svBounds.width() - 7.0f);
        float thumbY = this.svBounds.y() + clamp((1.0f - this.brightness) * this.svBounds.height(), 7.0f, this.svBounds.height() - 7.0f);
        drawThumb(drawCtx, colorStack2, thumbX, thumbY, 7.0f,
                Color.HSBtoRGB(this.hue, this.saturation, this.brightness));
        return color;
    }

    void renderHueBar(DrawCtx drawCtx, ColorStack colorStack2) {
        float barLength = this.hueBounds.height() - this.hueBounds.width();
        float radius = this.hueBounds.width() / 2.0f;
        LayoutScaleContext layout = drawCtx.layoutContext();
        int slot = drawCtx.drawEngine().bindTexture(hueTexture().texture());
        int tint = colorStack2.white();
        float edge = radius / barLength;
        float physicalRadius = layout.toPhysical(radius);
        drawCtx.drawEngine().roundedTexture(drawCtx.matrixStack().last().pose(),
                layout.toPhysical(this.hueBounds.x()), layout.toPhysical(this.hueBounds.y()),
                layout.toPhysical(this.hueBounds.width()), layout.toPhysical(this.hueBounds.height()),
                physicalRadius, physicalRadius, physicalRadius, physicalRadius,
                0.0f, -edge, 1.0f, 1.0f + edge, slot, tint, tint, tint, tint);
        drawThumb(drawCtx, colorStack2, this.hueBounds.x() + radius,
                this.hueBounds.y() + radius + (this.hue * barLength), radius + 2.5f,
                Color.HSBtoRGB(this.hue, 1.0f, 1.0f));
    }

    private static GlTexture hueTexture() {
        if (hueTexture == null) {
            ByteBuffer pixels = ByteBuffer.allocateDirect(256 * 4);
            for (int i = 0; i < 256; i++) {
                int rgb = Color.HSBtoRGB(i / 255.0f, 1.0f, 1.0f);
                pixels.put((byte) (rgb >> 16)).put((byte) (rgb >> 8)).put((byte) rgb).put((byte) 0xFF);
            }
            pixels.flip();
            hueTexture = new GlTexture(pixels).setDimensions(1, 256).wrapX(GL33.GL_CLAMP_TO_EDGE)
                    .wrapY(GL33.GL_CLAMP_TO_EDGE);
        }
        return hueTexture;
    }

    private void drawThumb(DrawCtx drawCtx, ColorStack colors, float x, float y, float radius, int rgb) {
        drawCtx.circle(x, y, radius, colors.white());
        drawCtx.circle(x, y, radius - 2.5f, colors.computeColor((rgb & 0x00FFFFFF) | 0xFF000000));
    }

    void renderAlphaBar(DrawCtx drawCtx, int rgb, ColorStack colorStack2) {
        int transparent = colorStack2.computeColor(rgb & 0x00FFFFFF, 0.0f);
        int opaque = colorStack2.computeColor((rgb & 0x00FFFFFF) | 0xFF000000);
        float radius = this.alphaBounds.width() / 2.0f;
        LayoutScaleContext layoutContext = drawCtx.layoutContext();
        drawCtx.drawEngine().roundedChecker(drawCtx.matrixStack().last().pose(),
                layoutContext.toPhysical(this.alphaBounds.x()), layoutContext.toPhysical(this.alphaBounds.y()),
                layoutContext.toPhysical(this.alphaBounds.width()), layoutContext.toPhysical(this.alphaBounds.height()),
                layoutContext.toPhysical(radius), layoutContext.toPhysical(half),
                colorStack2.computeColor(0.7f, 120, 120, 124), colorStack2.computeColor(0.7f, 60, 60, 64));
        drawCtx.fillGradientRoundedRect(this.alphaBounds.x(), this.alphaBounds.y(), this.alphaBounds.width(),
                this.alphaBounds.height(), (int) radius, transparent, transparent, opaque, opaque);
        float pointerY = this.alphaBounds.y() + radius
                + (1.0f - this.alpha) * (this.alphaBounds.height() - this.alphaBounds.width());
        drawCtx.circle(this.alphaBounds.x() + radius, pointerY, radius + 2.5f, colorStack2.white());
        drawCtx.circle(this.alphaBounds.x() + radius, pointerY, radius,
                colorStack2.computeColor((rgb & 0x00FFFFFF) | 0xFF000000, this.alpha));
    }

    void updateSaturationBrightness(float f, float f2) {
        this.saturation = clamp01((f - this.svBounds.x()) / this.svBounds.width());
        this.brightness = 1.0f - clamp01((f2 - this.svBounds.y()) / this.svBounds.height());
    }

    void updateHue(float f) {
        float radius = this.hueBounds.width() / 2.0f;
        this.hue = clamp01((f - this.hueBounds.y() - radius)
                / Math.max(1.0f, this.hueBounds.height() - this.hueBounds.width()));
    }

    void emitColor() {
        if (this.colorCallback != null) {
            this.colorCallback.accept(Color.HSBtoRGB(this.hue, this.saturation, this.brightness));
        }
    }

    void emitAlpha() {
        if (this.alphaCallback != null) {
            this.alphaCallback.accept(this.alpha);
        }
    }

    void addSwatch() {
        if (this.swatches == null || this.swatches.size() >= maxSwatches) {
            return;
        }
        ColorSwatch colorSwatch = new ColorSwatch((Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215) | (((int) (clamp01(this.alpha) * 255.0f)) << 24), false);
        colorSwatch.clickable().clickCallback(() -> selectSwatch(colorSwatch));
        colorSwatch.clickable().rightClickCallback(() -> removeSwatch(colorSwatch));
        this.swatches.add(colorSwatch);
        setSize(width(), computeHeight(this.swatches.size() + 1));
    }

    public void emitColorAndAlpha() {
        emitColor();
        emitAlpha();
    }

    void updateAlpha(float f) {
        float radius = this.alphaBounds.width() / 2.0f;
        this.alpha = 1.0f - clamp01((f - this.alphaBounds.y() - radius)
                / Math.max(1.0f, this.alphaBounds.height() - this.alphaBounds.width()));
    }

    static float clamp01(float f) {
        if (f < 0.0f) {
            return 0.0f;
        }
        return Math.min(f, 1.0f);
    }

    static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public void togglePicker() {
        if (this.openAnimator.isOne() || this.openAnimator.state()) {
            closePicker();
        } else {
            openPicker();
        }
    }

    public void openPicker() {
        visible(true);
        Raven.INSTANCE.windowController().bringToFront(this);
        this.openAnimator.state(true);
    }

    public void closePicker() {
        if (!this.openAnimator.state()) {
            return;
        }
        if (this.applyAction != null) {
            this.applyAction.run();
        }
        this.draggingHue = false;
        this.draggingSV = false;
        this.draggingAlpha = false;
        this.openAnimator.state(false);
    }

    public boolean opened() {
        return this.openAnimator.isOne() || this.openAnimator.state();
    }

    public boolean isRendering() {
        return !this.openAnimator.isZero();
    }

    public ColorPickerWindow colorConsumer(Consumer<Integer> consumer) {
        this.colorCallback = consumer;
        return this;
    }

    public ColorPickerWindow alphaConsumer(Consumer<Float> consumer) {
        this.alphaCallback = consumer;
        return this;
    }

    public ColorPickerWindow showAlpha(boolean showAlpha) {
        this.showAlpha = showAlpha;
        return this;
    }

    public ColorPickerWindow applyCallback(Runnable runnable) {
        this.applyAction = runnable;
        return this;
    }
}
