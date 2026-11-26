package sanch3zss.raven.ui.hud.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.screens.ChatScreen;
import org.joml.Matrix4f;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.media.RemoteImageLoader;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.texture.AnimatedTexture;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.Easings;

public final class GifWidget extends Draggable {
    private final BooleanSetting background = new BooleanSetting(Translation.clearText("Фон"))
            .setValue(true);
    private final TextFieldSetting link = new TextFieldSetting(Translation.clearText("Ссылка"))
            .setPlaceholder(Translation.clearText("https://..."))
            .setMax(2048);
    private final Setting sizeStorage = new SizeStorageSetting();
    private final ToggleAnimator handleAnimation = new ToggleAnimator(140, Easings.EASE_IN_OUT_CUBIC);
    private final AnimatedFloat fadeAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_QUAD);
    private final AnimatedFloat previewAnimation = new AnimatedFloat(200, Easings.EASE_IN_OUT_QUAD);
    private final MsdfFont font = Fonts.INTER_SEMIBOLD.get();

    private float widgetWidth = 160.0f;
    private float widgetHeight = 90.0f;
    private Corner hoveredCorner = Corner.NONE;
    private Corner resizingCorner = Corner.NONE;
    private float resizeAnchorX;
    private float resizeAnchorY;
    private Corner grabbedCorner = Corner.NONE;
    private boolean flipX;
    private boolean flipY;
    private boolean grabFlipX;
    private boolean grabFlipY;
    private String observedLink = "";
    private String requestedLink = "";
    private long linkChangedAt;
    private long animationStartedAt;
    private volatile AnimatedTexture pendingTexture;
    private volatile Throwable pendingError;
    private AnimatedTexture texture;
    private String errorMessage;
    private boolean loading;
    private boolean removeAfterFade;

    public GifWidget(BooleanSupplier visibility) {
        super("Gif", visibility);
        this.x = 420.0f;
        this.y = 260.0f;
        addSettings(this.background, this.link, this.sizeStorage);
    }

    @Override
    public void layout(DragRenderContext context) {
    }

    @Override
    public void render(DragRenderContext context) {
        acceptPendingTexture();
        float imageAlpha = this.texture == null ? 0.0f : this.fadeAnimation.animatedValue();
        float previewAlpha = this.previewAnimation.animatedValue() * (1.0f - imageAlpha);
        if (imageAlpha <= 0.001f && previewAlpha <= 0.001f) {
            return;
        }
        DrawEngine drawEngine = context.drawEngine();
        Matrix4f matrix = context.matrixStack().last().pose();
        ColorStack colors = drawEngine.colorStack();
        float padding = this.background.isValue() ? 3.0f : 0.0f;
        float radius = Math.min(5.0f, Math.min(this.widgetWidth, this.widgetHeight) / 4.0f);

        float backgroundAlpha = this.background.isValue()
                ? Math.min(1.0f, imageAlpha + previewAlpha) : previewAlpha * 0.55f;
        if (backgroundAlpha > 0.001f) {
            drawBackground(context, backgroundAlpha);
        }
        if (this.texture != null && imageAlpha > 0.001f) {
            colors.push();
            colors.alpha(imageAlpha);
            try {
                int textureId = this.texture.texture(System.currentTimeMillis() - this.animationStartedAt);
                int color = colors.computeColor(0xFFFFFFFF);
                float corner = Math.max(0.0f, radius - padding / 2.0f);
                drawEngine.roundedTexture(matrix, this.x + padding, this.y + padding,
                        Math.max(1.0f, this.widgetWidth - padding * 2.0f),
                        Math.max(1.0f, this.widgetHeight - padding * 2.0f),
                        corner, corner, corner, corner,
                        this.flipX ? 1.0f : 0.0f, this.flipY ? 1.0f : 0.0f,
                        this.flipX ? 0.0f : 1.0f, this.flipY ? 0.0f : 1.0f,
                        drawEngine.bindTexture(textureId), color, color, color, color);
            } catch (Throwable error) {
                this.errorMessage = shortMessage(error);
            }
            colors.pop();
        }
        if (previewAlpha > 0.001f) {
            colors.push();
            colors.alpha(previewAlpha);
            String label = this.loading ? "Загрузка..." : this.errorMessage != null ? "Ошибка" : "GIF";
            float size = 12.0f;
            int color = this.errorMessage != null
                    ? colors.computeColor(context.theme().palette().error().tone(300).argb())
                    : colors.computeColor(context.theme().palette().text().tone(200).argb());
            drawEngine.msdfFont(matrix, this.font, label,
                    this.x + (this.widgetWidth - this.font.getWidth(label, size)) / 2.0f,
                    this.y + (this.widgetHeight - this.font.getHeight(size)) / 2.0f,
                    size, 0.0f, color);
            colors.pop();
        }
        drawResizeHandle(context);
    }

    private void drawBackground(DragRenderContext context, float alpha) {
        DrawEngine drawEngine = context.drawEngine();
        ColorStack colors = drawEngine.colorStack();
        int tint = mixRgb(MenuStyle.surfaceColor(context.theme(), 0xFF111217), context.theme().palette().accentStart().argb(), 0.05f);
        int blurTexture = Raven.INSTANCE.widgetStack().hudBlurTexture();
        int panel = colors.computeColor(tint, 0.96f * alpha);
        HudGlassPanel.drawShadows(drawEngine, context.matrixStack().last().pose(),
                this.x, this.y, this.widgetWidth, this.widgetHeight, 5.0f,
                30.0f, colors.computeColor(tint, Math.min(1.0f, 20.5f) * alpha));
        if (blurTexture != 0 && this.background.isValue()) {
            drawEngine.roundedGlassPanel(context.matrixStack().last().pose(),
                    this.x, this.y, this.widgetWidth, this.widgetHeight, 5.0f,
                    0.0f, 0.80f, colors.computeColor(tint, alpha), 0, blurTexture);
        } else {
            drawEngine.roundedRectangle(context.matrixStack().last().pose(),
                    this.x, this.y, this.widgetWidth, this.widgetHeight, 5.0f, panel);
        }
    }

    private void drawResizeHandle(DragRenderContext context) {
        float animation = this.handleAnimation.smoothAnimation();
        Corner corner = this.resizingCorner != Corner.NONE ? this.resizingCorner : this.hoveredCorner;
        if (animation <= 0.001f || corner == Corner.NONE) {
            return;
        }
        float centerX = corner.left ? this.x : this.x + this.widgetWidth;
        float centerY = corner.top ? this.y : this.y + this.widgetHeight;
        float size = 6.0f * (0.72f + 0.28f * animation);
        float inner = Math.max(1.0f, size - 2.0f);
        DrawEngine drawEngine = context.drawEngine();
        Matrix4f matrix = context.matrixStack().last().pose();
        ColorStack colors = drawEngine.colorStack();
        drawEngine.roundedRectangle(matrix, centerX - size / 2.0f, centerY - size / 2.0f,
                size, size, 1.6f, colors.computeColor(context.theme().palette().accent().argb(), animation));
        drawEngine.roundedRectangle(matrix, centerX - inner / 2.0f, centerY - inner / 2.0f,
                inner, inner, 1.0f, colors.computeColor(MenuStyle.surfaceColor(context.theme(), MenuStyle.SURFACE_BASE_COLOR), animation));
    }

    @Override
    public boolean click(MouseButtonInput2 input, boolean hovered) {
        if (!input.isLeftButtonPressed()) {
            return false;
        }
        if (input.press() && !input.intercepted()) {
            Corner corner = cornerAt(input.mouseX(), input.mouseY());
            if (corner == Corner.NONE) {
                return false;
            }
            this.resizingCorner = corner;
            this.grabbedCorner = corner;
            this.grabFlipX = this.flipX;
            this.grabFlipY = this.flipY;
            this.resizeAnchorX = corner.left ? this.x + scaledWidth() : this.x;
            this.resizeAnchorY = corner.top ? this.y + scaledHeight() : this.y;
            this.dragging = false;
            this.clicked = true;
            return true;
        }
        if (input.release() && this.resizingCorner != Corner.NONE) {
            this.resizingCorner = Corner.NONE;
            this.clicked = false;
            return true;
        }
        return this.resizingCorner != Corner.NONE;
    }

    @Override
    public boolean cursor(MouseMoveInput input, boolean hovered) {
        if (input.intercepted()) {
            this.hoveredCorner = Corner.NONE;
            return false;
        }
        float mouseX = (float) (input.mouse().xpos() / input.scaleFactor());
        float mouseY = (float) (input.mouse().ypos() / input.scaleFactor());
        this.hoveredCorner = cornerAt(mouseX, mouseY);
        return hovered || this.hoveredCorner != Corner.NONE;
    }

    @Override
    public void updateDrag(DragRenderContext context) {
        if (this.resizingCorner == Corner.NONE) {
            super.updateDrag(context);
            return;
        }
        if (!(Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen)) {
            this.resizingCorner = Corner.NONE;
            this.clicked = false;
            return;
        }
        float scale = visualScale();
        float minimum = 24.0f * scale;
        float mouseX = MathUtil.clamp(context.mouseX(), 0.0f, context.resolution().screenWidth());
        float mouseY = MathUtil.clamp(context.mouseY(), 0.0f, context.resolution().screenHeight());
        boolean left = mouseX < this.resizeAnchorX;
        boolean top = mouseY < this.resizeAnchorY;
        float width = Math.max(minimum, Math.abs(mouseX - this.resizeAnchorX));
        float height = Math.max(minimum, Math.abs(mouseY - this.resizeAnchorY));
        this.resizingCorner = Corner.of(left, top);
        this.flipX = this.grabFlipX ^ (left != this.grabbedCorner.left);
        this.flipY = this.grabFlipY ^ (top != this.grabbedCorner.top);
        this.x = left ? this.resizeAnchorX - width : this.resizeAnchorX;
        this.y = top ? this.resizeAnchorY - height : this.resizeAnchorY;
        this.widgetWidth = width / scale;
        this.widgetHeight = height / scale;
        this.lastResizeTimer.reset();
    }

    @Override
    public void animate(WeightedEngine engine) {
        this.handleAnimation.state(Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen
                && (this.hoveredCorner != Corner.NONE || this.resizingCorner != Corner.NONE)).animate(engine);
        this.previewAnimation.destination(isVisible() && Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen ? 1.0f : 0.0f);
        this.previewAnimation.animate(engine);
        this.fadeAnimation.animate(engine);
    }

    @Override
    public void update() {
        String currentLink = this.link.getText() == null ? "" : this.link.getText().trim();
        if (!currentLink.equals(this.observedLink)) {
            this.observedLink = currentLink;
            this.linkChangedAt = System.currentTimeMillis();
            this.errorMessage = null;
            if (currentLink.isEmpty()) {
                this.requestedLink = "";
                this.loading = false;
                if (this.texture != null) {
                    this.removeAfterFade = true;
                    this.fadeAnimation.destination(0.0f);
                }
            }
        }
        if (this.removeAfterFade && this.fadeAnimation.isZero()) {
            clearTexture();
            this.removeAfterFade = false;
        }
        if (!currentLink.isEmpty() && !currentLink.equals(this.requestedLink)
                && System.currentTimeMillis() - this.linkChangedAt >= 500L) {
            request(currentLink);
        }
    }

    private void request(String url) {
        this.requestedLink = url;
        this.loading = true;
        RemoteImageLoader.load(url).whenComplete((loaded, error) -> {
            if (!url.equals(this.observedLink)) {
                if (loaded != null) loaded.close();
                return;
            }
            if (error != null) {
                this.pendingError = error;
            } else {
                this.pendingTexture = loaded;
            }
        });
    }

    private void acceptPendingTexture() {
        AnimatedTexture loaded = this.pendingTexture;
        if (loaded != null) {
            this.pendingTexture = null;
            clearTexture();
            this.texture = loaded;
            this.animationStartedAt = System.currentTimeMillis();
            this.loading = false;
            this.errorMessage = null;
            this.removeAfterFade = false;
            this.fadeAnimation.set(0.0f);
            this.fadeAnimation.destination(1.0f);
        }
        Throwable error = this.pendingError;
        if (error != null) {
            this.pendingError = null;
            this.loading = false;
            this.errorMessage = shortMessage(error);
        }
    }

    private void clearTexture() {
        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }
    }

    @Override
    protected List<String> hints() {
        List<String> hints = super.hints();
        hints.add(Lang.WIDGET_RESIZE_HINT.effective());
        return hints;
    }

    private Corner cornerAt(float mouseX, float mouseY) {
        float width = scaledWidth();
        float height = scaledHeight();
        float grab = Math.min(9.0f, Math.min(width, height) / 3.0f);
        boolean left = mouseX >= this.x - 2.0f && mouseX <= this.x + grab;
        boolean right = mouseX >= this.x + width - grab && mouseX <= this.x + width + 2.0f;
        boolean top = mouseY >= this.y - 2.0f && mouseY <= this.y + grab;
        boolean bottom = mouseY >= this.y + height - grab && mouseY <= this.y + height + 2.0f;
        if (left && top) return Corner.TOP_LEFT;
        if (right && top) return Corner.TOP_RIGHT;
        if (left && bottom) return Corner.BOTTOM_LEFT;
        if (right && bottom) return Corner.BOTTOM_RIGHT;
        return Corner.NONE;
    }

    private static String shortMessage(Throwable error) {
        Throwable current = error;
        while ((current instanceof CompletionException || current.getMessage() == null) && current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static int mixRgb(int base, int tint, float amount) {
        float inverse = 1.0f - amount;
        int red = Math.round(((base >> 16) & 0xFF) * inverse + ((tint >> 16) & 0xFF) * amount);
        int green = Math.round(((base >> 8) & 0xFF) * inverse + ((tint >> 8) & 0xFF) * amount);
        int blue = Math.round((base & 0xFF) * inverse + (tint & 0xFF) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    public boolean isRendering() {
        return this.texture != null || !this.previewAnimation.isZero()
                || Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen;
    }

    public boolean usesBackground() {
        return this.background.isValue();
    }

    @Override
    public float width() {
        return this.widgetWidth;
    }

    @Override
    public float height() {
        return this.widgetHeight;
    }

    private enum Corner {
        NONE(false, false),
        TOP_LEFT(true, true),
        TOP_RIGHT(false, true),
        BOTTOM_LEFT(true, false),
        BOTTOM_RIGHT(false, false);

        private final boolean left;
        private final boolean top;

        Corner(boolean left, boolean top) {
            this.left = left;
            this.top = top;
        }

        private static Corner of(boolean left, boolean top) {
            if (left) return top ? TOP_LEFT : BOTTOM_LEFT;
            return top ? TOP_RIGHT : BOTTOM_RIGHT;
        }
    }

    private final class SizeStorageSetting extends Setting {
        private SizeStorageSetting() {
            super(Translation.clearText("__GifSize"));
        }

        @Override
        public Map<String, Object> toSerializedData() {
            return Map.of("width", GifWidget.this.widgetWidth, "height", GifWidget.this.widgetHeight,
                    "flipX", GifWidget.this.flipX, "flipY", GifWidget.this.flipY);
        }

        @Override
        public void loadSerializedData(Map<String, Object> data) {
            Object width = data.get("width");
            Object height = data.get("height");
            if (width instanceof Number number && Float.isFinite(number.floatValue())) {
                GifWidget.this.widgetWidth = MathUtil.clamp(number.floatValue(), 24.0f, 4096.0f);
            }
            if (height instanceof Number number && Float.isFinite(number.floatValue())) {
                GifWidget.this.widgetHeight = MathUtil.clamp(number.floatValue(), 24.0f, 4096.0f);
            }
            GifWidget.this.flipX = Boolean.TRUE.equals(data.get("flipX"));
            GifWidget.this.flipY = Boolean.TRUE.equals(data.get("flipY"));
        }
    }
}
