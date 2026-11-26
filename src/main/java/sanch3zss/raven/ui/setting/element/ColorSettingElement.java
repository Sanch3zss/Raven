package sanch3zss.raven.ui.setting.element;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.gl.StencilBufferUtil;
import java.awt.Color;
import java.util.List;
import java.util.function.*;

import org.joml.Vector4f;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.InputEventContext;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.EventCallback;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.component.control.ClickableBehavior;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.layout.ModuleFrame;
import sanch3zss.raven.ui.menu.window.ColorPickerWindow;
import sanch3zss.raven.ui.setting.ColorSetting;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public class ColorSettingElement extends ModuleFrame {
    static final int nameTextSize = 14;
    static final int descriptionTextSize = 13;
    static final int hexTextSize = 10;
    static final float descriptionGap = 3.0f;
    static final float horizontalPadding = 10.0f;
    static final float barHeight = 18.0f;
    static final float verticalPadding = 10.0f;
    static final float swatchRadius = 15.0f;
    static final float swatchHoverScale = 1.5f;
    static final long copiedDurationMs = 1000;

    public final List<ColorSwatch2> swatches;
    public final Translation name;
    public final Translation description;
    public String wrappedDescription;
    public String cachedDescriptionText;
    public float descriptionHeight;
    public Supplier<Boolean> visibilitySupplier;

    public Consumer<Integer> colorCallback;
    public Consumer<Float> alphaCallback;
    public final Supplier<Integer> colorSupplier;
    public final Supplier<Float> alphaSupplier;
    public Runnable commitAction;
    public final MsdfFont titleFont = Fonts.INTER_SEMIBOLD.get();
    public final MsdfFont descriptionFont = Fonts.INTER_MEDIUM.get();
    public final MsdfFont hexFont = Fonts.INTER_EXTRA_BOLD.get();
    public final GlTexture brushIcon = new GlTexture(new ClasspathResource("/icons/menu/new/brush.png"));
    public final HighlightAnimation highlightAnimation = new HighlightAnimation(300, Easings.EASE_IN_OUT_CUBIC);
    public final ClickableBehavior barClickable = new ClickableBehavior();
    public final ClickableBehavior secondClickable = new ClickableBehavior();
    public final ClickableBehavior hexClickable = new ClickableBehavior();
    public final ToggleAnimator copiedAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator hexAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator copyAnimator = new ToggleAnimator(250, Easings.EASE_IN_OUT_CUBIC);
    public final ToggleAnimator gradientSwatchAnimator = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    public boolean copied = false;
    public long copiedTime = 0;
    public float hue = 0.0f;
    public float saturation = 1.0f;
    public float brightness = 1.0f;
    public float alpha = 1.0f;
    public ColorSwatch2 selectedSwatch = null;
    public float cachedWidth = -1.0f;
    public boolean descriptionDirty = true;
    public boolean pickerOpen = false;
    public ColorPickerWindow pickerWindow = null;
    private ColorSetting gradientSetting;
    private boolean secondPickerActive;
    private float pickerAnchorX = Float.NaN;
    private float pickerAnchorY = Float.NaN;

    public ColorSettingElement(Translation translation2, Translation translation, Supplier<Integer> supplier, Supplier<Float> supplier2) {
        this.name = translation2;
        this.description = translation;
        this.colorSupplier = supplier;
        this.alphaSupplier = supplier2;
        initFromSuppliers(supplier, supplier2);
        this.swatches = createSwatches();
        bindSwatchCallbacks();
        bindCopyCallback();
        this.barClickable.clickCallback(() -> openPicker(false));
        this.secondClickable.clickCallback(() -> openPicker(true));    }

    private void openPicker(boolean second) {
        boolean samePicker = this.pickerWindow != null && this.pickerWindow.opened()
                && this.secondPickerActive == second;
        ColorPickerWindow picker = (ColorPickerWindow) Raven.INSTANCE.windowController().getWindow(ColorPickerWindow.class);
        if (samePicker && picker == this.pickerWindow) {
            picker.closePicker();
            return;
        }
        int source = second && this.gradientSetting != null
                ? this.gradientSetting.getSecondColorWithoutAlpha()
                : Color.HSBtoRGB(this.hue, this.saturation, this.brightness);
        float opacity = second && this.gradientSetting != null
                ? this.gradientSetting.getSecondAlpha() : this.alpha;
        float[] hsb = Color.RGBtoHSB(ColorUtil.red(source), ColorUtil.green(source), ColorUtil.blue(source), null);
        if (picker == null) {
            picker = new ColorPickerWindow(hsb[0], hsb[1], hsb[2], opacity);
            Raven.INSTANCE.windowController().newWindow(picker);
        } else {
            picker.updateColor(hsb[0], hsb[1], hsb[2], opacity);
        }
        picker.colorConsumer(color -> {
            if (second && this.gradientSetting != null) {
                int currentAlpha = this.gradientSetting.getSecondColor() & 0xFF000000;
                this.gradientSetting.setSecondColor(currentAlpha | (color & 0xFFFFFF));
            } else {
                if (this.colorCallback != null) {
                    this.colorCallback.accept((Math.round(this.alpha * 255.0f) << 24) | (color & 0xFFFFFF));
                }
                if (this.alphaCallback != null) {
                    this.alphaCallback.accept(this.alpha);
                }
                float[] selected = Color.RGBtoHSB(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), null);
                this.hue = selected[0];
                this.saturation = selected[1];
                this.brightness = selected[2];
            }
        });
        picker.alphaConsumer(value -> {
            if (second && this.gradientSetting != null) {
                this.gradientSetting.setSecondAlpha(value);
            } else {
                this.alpha = value;
                if (this.alphaCallback != null) {
                    this.alphaCallback.accept(value);
                }
            }
        });
        picker.showAlpha(this.alphaCallback != null);
        picker.applyCallback(() -> {
            if (this.commitAction != null) {
                this.commitAction.run();
            }
        });
        this.pickerWindow = picker;
        this.pickerOpen = true;
        this.secondPickerActive = second;
        picker.anchor(this.pickerAnchorX, this.pickerAnchorY);
        if (!picker.opened()) {
            picker.openPicker();
        }
    }

    public void initFromSuppliers(Supplier<Integer> supplier, Supplier<Float> supplier2) {
        int color = supplier.get();
        float[] hsb = Color.RGBtoHSB(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = supplier2.get();
    }

    public void syncFromSupplier() {
        if (this.pickerOpen) {
            return;
        }
        int color = this.colorSupplier.get();
        float alphaValue = this.alphaSupplier.get();
        int currentRgb = Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215;
        int targetRgb = color & 16777215;
        int alphaCurrent = (int) (clamp01(this.alpha) * 255.0f);
        int alphaTarget = (int) (clamp01(alphaValue) * 255.0f);
        if (currentRgb != targetRgb || Math.abs(alphaCurrent - alphaTarget) > 1) {
            float[] hsb = Color.RGBtoHSB(ColorUtil.red(color), ColorUtil.green(color), ColorUtil.blue(color), null);
            this.hue = hsb[0];
            this.saturation = hsb[1];
            this.brightness = hsb[2];
            this.alpha = alphaValue;
            updateSelectedSwatch();
        }
    }

    public List<ColorSwatch2> createSwatches() {
        return List.of(
            new ColorSwatch2(ThemePalette.mistyBlue.argb()),
            new ColorSwatch2(ThemePalette.golden.argb()),
            new ColorSwatch2(ThemePalette.violet.argb()),
            new ColorSwatch2(ColorValue.fromHex("C5C6C8").argb()),
            new ColorSwatch2(ThemePalette.red.argb()),
            new ColorSwatch2(ThemePalette.mint.argb()),
            new ColorSwatch2(Raven.INSTANCE.theme().palette().accentStart().argb())
        );
    }

    public void bindSwatchCallbacks() {
        this.swatches.forEach(swatch -> swatch.clickable.clickCallback(() -> selectSwatch(swatch)));
    }

    public void selectSwatch(ColorSwatch2 swatch) {
        this.selectedSwatch = swatch;
        if (this.colorCallback != null) {
            this.colorCallback.accept(swatch.color());
        }
        applySwatchColor(swatch);
        ColorPickerWindow picker = (ColorPickerWindow) Raven.INSTANCE.windowController().getWindow(ColorPickerWindow.class);
        if (picker != null) {
            picker.updateColor(this.hue, this.saturation, this.brightness, this.alpha);
        }
        if (this.commitAction != null) {
            this.commitAction.run();
        }
    }

    public void applySwatchColor(ColorSwatch2 swatch) {
        float[] hsb = Color.RGBtoHSB(ColorUtil.red(swatch.color()), ColorUtil.green(swatch.color()), ColorUtil.blue(swatch.color()), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        int iAlpha = ColorUtil.alpha(swatch.color());
        this.alpha = (iAlpha == 0 && (swatch.color() >>> 24) == 0) ? 1.0f : iAlpha / 255.0f;
        if (this.alphaCallback != null) {
            this.alphaCallback.accept(this.alpha);
        }
    }

    public void bindCopyCallback() {
        this.hexClickable.clickCallback(() -> {
            StringUtil.copyToClipboard(String.format("#%06X", Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215));
            this.copied = true;
            this.copiedTime = System.currentTimeMillis();
        });
    }

    @Override
    public void highlight() {
        this.highlightAnimation.trigger();
    }

    @Override
    public void clearHighlight() {
        this.highlightAnimation.clearHighlight();
    }

    @Override
    public void draw(DrawCtx drawCtx, float f, float f2) {
        updateWrappedDescription(f);
        drawDescription(drawCtx, drawName(drawCtx, y()));
        ColorStack colors = drawCtx.colorStack();
        int color = colors.computeColor(Color.HSBtoRGB(this.hue, this.saturation, this.brightness), this.alpha);
        float buttonX = x() + f - 48.0f;
        float swatchY = y() + (Math.max(barHeight, this.titleFont.getHeight(nameTextSize)) - barHeight) / 2.0f;
        float scale = Math.max(drawCtx.layoutContext().scaleFactor(), 0.01f);
        this.pickerAnchorX = buttonX + 48.0f
                + drawCtx.matrixStack().last().pose().m30() / scale;
        this.pickerAnchorY = swatchY
                + drawCtx.matrixStack().last().pose().m31() / scale;
        float firstX = buttonX + 24.0f * (1.0f - this.gradientSwatchAnimator.smoothAnimation());
        MenuStyle.drawColorWell(drawCtx, firstX - 2.0f, swatchY - 2.0f, 22.0f, color,
                this.barClickable.hoverAnimation().smoothAnimation());
        this.barClickable.setDimensions(firstX, swatchY, 18.0f, 18.0f);
        if (this.gradientSwatchAnimator.smoothAnimation() > 0.001f) {
            colors.push();
            colors.alpha(this.gradientSwatchAnimator.smoothAnimation());
            int second = colors.computeColor(this.gradientSetting.getSecondColor());
            MenuStyle.drawColorWell(drawCtx, buttonX + 22.0f, swatchY - 2.0f, 22.0f, second,
                    this.secondClickable.hoverAnimation().smoothAnimation());
            colors.pop();
        }
        this.secondClickable.setDimensions(buttonX + 24.0f, swatchY, 18.0f, 18.0f);
    }

    public void drawBar(DrawCtx drawCtx, float f, float f2) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        sanch3zss.raven.ui.menu.MenuStyle.drawControlField(drawCtx, x(), f2, f, barHeight,
                6.0f, this.barClickable.hoverAnimation().smoothAnimation());
        float swatchesEndX = drawSwatches(drawCtx, colorStack, drawDivider(drawCtx, colorStack, drawPreview(drawCtx, colorStack, f2), f2) + 10.0f + 9.0f, f2);
        drawDivider(drawCtx, colorStack, swatchesEndX, f2);
        drawHexLabel(drawCtx, palette, colorStack, swatchesEndX, f2, f);
    }

    public float drawPreview(DrawCtx drawCtx, ColorStack colorStack2, float f) {
        float fX = x() + 14.0f;
        int color = colorStack2.computeColor(Color.HSBtoRGB(this.hue, this.saturation, this.brightness), this.alpha);
        drawCtx.fillRoundedRect(x(), f, 40.0f, barHeight, new Vector4f(8.0f, 8.0f, 0.0f, 0.0f),
                colorStack2.interpolate(color, colorStack2.autoBrightenDarken(color, 0.2f), this.barClickable.hoverAnimation().smoothAnimation()));
        return fX + this.brushIcon.width() + 12.0f;
    }

    public float drawDivider(DrawCtx drawCtx, ColorStack colorStack2, float f, float f2) {
        drawCtx.fillRect(f, f2, 1.0f, barHeight, colorStack2.computeColor(drawCtx.theme().palette().surfaceOutline().tone(400).argb()));
        return f;
    }

    public float drawSwatches(DrawCtx drawCtx, ColorStack colorStack2, float f, float f2) {
        int i = 0;
        while (i < this.swatches.size()) {
            drawSwatch(drawCtx, colorStack2, this.swatches.get(i), f, f2);
            f += i < this.swatches.size() - 1 ? 19.0f : 18.0f;
            i++;
        }
        return f;
    }

    public void drawSwatch(DrawCtx drawCtx, ColorStack colorStack2, ColorSwatch2 colorSwatch22, float f, float f2) {
        float f3 = f2 + 12.5f;
        float radius = (10.0f + Math.max(colorSwatch22.clickable.hoverAnimation().smoothAnimation()
                * swatchHoverScale, colorSwatch22.selectAnim.smoothAnimation() * 2.0f)) / 2.0f;
        drawCtx.circle(f, f3, radius + 0.8f, colorStack2.computeColor(0xFFFFFFFF, 0.9f));
        drawCtx.circle(f, f3, radius, colorStack2.computeColor(colorSwatch22.color()));
        colorSwatch22.clickable.setDimensions(f - (19.0f / 2.0f), f2, 19.0f, barHeight);
        if (colorSwatch22 == this.selectedSwatch) {
            MenuStyle.drawCheckmark(drawCtx, f, f3, 6.5f,
                    colorStack2.computeColor(drawCtx.theme().palette().surfaceBackground().tone(900).argb(),
                            colorSwatch22.selectAnim.smoothAnimation()));
        }
    }

    public void drawHexLabel(DrawCtx drawCtx, ThemePalette palette2, ColorStack colorStack2, float f, float f2, float f3) {
        float fX = (x() + f3) - f;
        this.hexClickable.setDimensions(f, f2, fX, barHeight);
        String str = String.format("#%06X", Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215);
        float height = (f2 + 12.5f) - (this.hexFont.getHeight(10.0f) / 2.0f);
        float f4 = f + (fX / 2.0f);
        int color = colorStack2.computeColor(palette2.text().tone(100).argb());
        if (this.copied) {
            drawCenteredText(drawCtx, "Copied", f4, height, colorStack2.computeColor(color, this.copiedAnimator.smoothAnimation()));
        } else if (this.hexClickable.hoverAnimation().smoothAnimation() <= 0.01f) {
            drawCenteredText(drawCtx, str, f4, height, colorStack2.computeColor(color, this.hexAnimator.smoothAnimation()));
        } else {
            drawCenteredText(drawCtx, str, f4, height, colorStack2.computeColor(color, this.hexAnimator.smoothAnimation()));
            drawCenteredText(drawCtx, "Copy", f4, height, colorStack2.computeColor(color, this.copyAnimator.smoothAnimation()));
        }
    }

    public void drawCenteredText(DrawCtx drawCtx, String str, float f, float f2, int i) {
        drawCtx.text(this.hexFont, str, hexTextSize, (f - (drawCtx.textWidthPhysical(this.hexFont, str, hexTextSize) / 2.0f)) - 2.0f, f2, i);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.brushIcon.setDimensions(12, 12);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        boolean zHandleInput = this.hexClickable.handleInput(inputEventContext, z);
        for (ColorSwatch2 swatch : this.swatches) {
            zHandleInput |= swatch.clickable.handleInput(inputEventContext, zHandleInput);
        }
        boolean zHandleInput2 = zHandleInput | this.barClickable.handleInput(inputEventContext, zHandleInput);
        if (showsSecondColor()) {
            zHandleInput2 |= this.secondClickable.handleInput(inputEventContext, zHandleInput2);
        }
        return zHandleInput2 | super.handleInput(inputEventContext, zHandleInput2);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        if (this.visibilitySupplier != null) {
            visible(Boolean.TRUE.equals(this.visibilitySupplier.get()));
        }
        if (this.pickerWindow != null && !this.pickerWindow.opened()) {
            this.pickerWindow = null;
            this.pickerOpen = false;
        }
        syncFromSupplier();
        updateSelectedSwatch();
        this.barClickable.animate(weightedEngine);
        this.secondClickable.animate(weightedEngine);
        this.gradientSwatchAnimator.state(showsSecondColor());
        this.gradientSwatchAnimator.animate(weightedEngine);
        updateCopiedState();
        animateSwatches(weightedEngine);
        this.highlightAnimation.animate(weightedEngine);
        super.animation(weightedEngine);
    }

    public void updateCopiedState() {
        if (!this.copied || System.currentTimeMillis() - this.copiedTime <= copiedDurationMs) {
            return;
        }
        this.copied = false;
    }

    public void animateSwatches(WeightedEngine weightedEngine) {
        boolean z = this.hexClickable.hoverAnimation().smoothAnimation() > 0.01f;
        this.hexAnimator.state(!z && !this.copied);
        this.copyAnimator.state(z && !this.copied);
        this.copiedAnimator.state(this.copied);
        this.hexClickable.animate(weightedEngine);
        this.hexAnimator.animate(weightedEngine);
        this.copyAnimator.animate(weightedEngine);
        this.copiedAnimator.animate(weightedEngine);
        this.swatches.forEach(colorSwatch22 -> {
            colorSwatch22.selectAnim.state(this.selectedSwatch == colorSwatch22);
            colorSwatch22.clickable.animate(weightedEngine);
            colorSwatch22.selectAnim.animate(weightedEngine);
        });
    }

    @Override
    public void handleClose() {        super.handleClose();
    }

    @Override
    public float height() {
        if (this.description != null && this.descriptionDirty && this.selectedSwatch != null) {
            updateWrappedDescription(width());
        }
        float height = Math.max(barHeight, this.titleFont.getHeight(nameTextSize));
        if (this.description != null && this.wrappedDescription != null) {
            height += descriptionGap + this.descriptionHeight;
        }
        return height;
    }

    public float drawName(DrawCtx drawCtx, float f) {
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        float rowHeight = Math.max(barHeight, this.titleFont.getHeight(nameTextSize));
        float textY = this.titleFont.centeredGlyphY('H', nameTextSize, f, rowHeight);
        drawCtx.text(this.titleFont, this.name.effective(), nameTextSize, x(), textY, colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.highlightAnimation.value()));
        return f + rowHeight;
    }

    public float drawDescription(DrawCtx drawCtx, float f) {
        if (this.wrappedDescription == null) {
            return f;
        }
        ThemePalette palette = drawCtx.theme().palette();
        ColorStack colorStack = drawCtx.drawEngine().colorStack();
        int iInterpolate = colorStack.interpolate(colorStack.computeColor(palette.text().tone(100).argb()), colorStack.computeColor(palette.text().tone(100).argb()), this.highlightAnimation.value());
        float f2 = f + descriptionGap;
        drawCtx.text(this.descriptionFont, this.wrappedDescription, descriptionTextSize, x(), f2, iInterpolate);
        return f2 + this.descriptionHeight;
    }

    public void markDescriptionDirty() {
        this.descriptionDirty = true;
        this.cachedWidth = -1.0f;
        this.cachedDescriptionText = null;
    }

    public void updateWrappedDescription(float f) {
        if (this.description == null) {
            clearWrappedDescription(f);
            return;
        }
        String strEffective = this.description.effective();
        if (isDescriptionCacheValid(f, strEffective)) {
            return;
        }
        computeWrappedDescription(f, strEffective == null ? "" : strEffective);
    }

    public void updateSelectedSwatch() {
        int current = currentColor();
        ColorSwatch2 colorSwatch22 = null;
        for (ColorSwatch2 colorSwatch2 : this.swatches) {
            if (colorSwatch2.color() == current) {
                colorSwatch22 = colorSwatch2;
                break;
            }
        }
        if (this.selectedSwatch != colorSwatch22) {
            this.selectedSwatch = colorSwatch22;
            for (ColorSwatch2 s : this.swatches) {
                s.selectAnim.state(s == this.selectedSwatch);
            }
        }
    }

    public void clearWrappedDescription(float f) {
        this.wrappedDescription = null;
        this.descriptionHeight = 0.0f;
        this.descriptionDirty = false;
        this.cachedWidth = f;
        this.cachedDescriptionText = null;
    }

    public boolean isDescriptionCacheValid(float f, String str) {
        return !this.descriptionDirty && f == this.cachedWidth && str != null && str.equals(this.cachedDescriptionText);
    }

    public int currentColor() {
        return ((((int) (clamp01(this.alpha) * 255.0f)) & StencilBufferUtil.STENCIL_MASK) << 24) | (Color.HSBtoRGB(this.hue, this.saturation, this.brightness) & 16777215);
    }

    public static float clamp01(float f) {
        if (f < 0.0f) {
            return 0.0f;
        }
        return Math.min(f, 1.0f);
    }

    public void computeWrappedDescription(float f, String str) {
        this.wrappedDescription = StringUtil.formatTextToFitWidth(str, f, this.descriptionFont, descriptionTextSize);
        this.descriptionHeight = this.descriptionFont.getHeightWithLineBreaks(this.wrappedDescription, descriptionTextSize);
        this.cachedWidth = f;
        this.cachedDescriptionText = str;
        this.descriptionDirty = false;
    }

    public Translation name() {
        return this.name;
    }

    public ColorSettingElement visibleSupplier(Supplier<Boolean> supplier) {
        this.visibilitySupplier = supplier;
        return this;
    }

    public ColorSettingElement colorConsumer(Consumer<Integer> consumer) {
        this.colorCallback = consumer;
        return this;
    }

    public ColorSettingElement alphaConsumer(Consumer<Float> consumer) {
        this.alphaCallback = consumer;
        return this;
    }

    public ColorSettingElement commitRunnable(Runnable runnable) {
        this.commitAction = runnable;
        return this;
    }

    public ColorSettingElement gradient(ColorSetting setting) {
        this.gradientSetting = setting;
        return this;
    }

    private boolean showsSecondColor() {
        return this.gradientSetting != null
                && this.gradientSetting.mode() == sanch3zss.raven.render.theme.ColorMode.GRADIENT;
    }
}
