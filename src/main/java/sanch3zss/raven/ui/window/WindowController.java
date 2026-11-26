package sanch3zss.raven.ui.window;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.engine.OverlayCommandQueue;
import sanch3zss.raven.render.engine.RenderCommandQueue;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.gl.FramebufferUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.Minecraft;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;import sanch3zss.raven.render.animation.*;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.shader.*;
import sanch3zss.raven.render.theme.Theme;
import sanch3zss.raven.ui.hud.AbstractWindow;
import sanch3zss.raven.ui.menu.window.*;
import sanch3zss.raven.util.math.interpolation.*;

public class WindowController {
    private static final float REFERENCE_WIDTH = 1920.0f;
    private static final float REFERENCE_HEIGHT = 1080.0f;
    private static final float AUTO_SCALE_MAX = 3.0f;
    private static final float AUTO_SCALE_MARGIN = 24.0f;

    public int windowVersion;
    public int backgroundAlphaValue;
    public int savedCursorMode;
    public ScreenResolution resolution;
    public static final long frameIntervalNanos = 16666667;
    public boolean dpiInitialized;
    public float dpiScaleFactor = 1.0f;
    public boolean autoDpiScale = true;
    public final List<AbstractWindow> windows = new ArrayList<>();
    public final InputInterceptor window = new InputInterceptor();
    public final DeltaTimeTracker deltaTimeTracker = new DeltaTimeTracker();
    public final AnimationStack2 animationStack = new AnimationStack2();
    public final KawaseBlurEffect menuSceneBlur = new KawaseBlurEffect();
    public final KawaseBlurEffect menuUiBlur = new KawaseBlurEffect();
    public final BlurEffect headerBlur = new BlurEffect();
    public final BlurEffect searchBlur = new BlurEffect();
    public final BloomEffect bloom = new BloomEffect();
    public final ScaledRenderTarget bloomBuffer = new ScaledRenderTarget(2);
    public int warmupFrames = 2;
    public PixelPoint mousePosition = new PixelPoint(0, 0);
    public long lastFrameTime = 0;
    public final AnimatedFloat dpiScaleAnimation = new AnimatedFloat(180, Easings.EASE_IN_OUT_CUBIC);

    public void init() {
        this.menuSceneBlur.init();
        this.menuUiBlur.init();
        this.headerBlur.init();
        this.searchBlur.init();
        this.bloom.init();
    }

    public void draw(long j) {
        MenuWindow menuWindow;
        if (this.warmupFrames > 0) {
            this.warmupFrames--;
            this.deltaTimeTracker.clear();
        }
        if (this.resolution == null) {
            this.resolution = ScreenResolution.resolution();
            if (this.autoDpiScale) {
                updateAutoDpiScale();
            }
        }
        long jNanoTime = System.nanoTime();
        boolean z = jNanoTime - this.lastFrameTime >= frameIntervalNanos;
        if (z) {
            this.lastFrameTime = jNanoTime;
        }
        WeightedEngine weightedEngine = new WeightedEngine(this.deltaTimeTracker.elapsedUnit(), this.animationStack);
        this.animationStack.begin();
        if (!this.dpiInitialized) {
            this.dpiScaleAnimation.set(this.dpiScaleFactor);
            this.dpiScaleAnimation.destination(this.dpiScaleFactor);
            this.dpiInitialized = true;
        }
        this.dpiScaleAnimation.animate(weightedEngine);
        float f = this.dpiScaleFactor;
        this.dpiScaleFactor = this.dpiScaleAnimation.animatedValue();
        if (Math.abs(f - this.dpiScaleFactor) > 1.0E-4f && (menuWindow = Raven.INSTANCE.menuWindow()) != null) {
            menuWindow.requestCentering();
        }
        for (AbstractWindow abstractWindow : this.windows) {
            abstractWindow.animation(weightedEngine);
        }
        this.animationStack.end();
        this.window.begin(j);
        PoseStack matrixStack = new PoseStack();
        DrawEngine drawEngine = Raven.INSTANCE.drawEngine();
        Theme theme = Raven.INSTANCE.theme();
        RenderCommandQueue renderCommandQueue = new RenderCommandQueue();
        OverlayCommandQueue overlayCommandQueue = new OverlayCommandQueue();
        LayoutScaleContext layoutScaleContext = new LayoutScaleContext(this.resolution, this.dpiScaleFactor);
        DrawCtx drawCtx = new DrawCtx(this.window, this.resolution, matrixStack, this.window.determineMousePosition(), drawEngine, drawEngine.colorStack(), this.mousePosition, theme, layoutScaleContext);
        boolean anyVisible = false;
        for (AbstractWindow abstractWindow2 : this.windows) {
            if (abstractWindow2.visible()) {
                abstractWindow2.layout(layoutScaleContext);
                abstractWindow2.collectBlurElements(overlayCommandQueue);
                abstractWindow2.collectBloomElements(renderCommandQueue);
                anyVisible = true;
            }
        }
        boolean hasOverlay = !overlayCommandQueue.isEmpty();
        boolean hasBloom = !renderCommandQueue.isEmpty();
        boolean shouldRenderOverlay = hasOverlay && anyVisible;
        MenuWindow activeMenu = Raven.INSTANCE.menuWindow();
        if (anyVisible && activeMenu != null && activeMenu.visible()) {
            this.menuSceneBlur.apply();
        }
        if (shouldRenderOverlay) {
            drawEngine.begin();
            try {
                overlayCommandQueue.renderRecorded(drawCtx);
            } finally {
                drawEngine.end();
            }
        }
        if (hasBloom && anyVisible) {
            FramebufferUtil.clearTransparent(this.bloomBuffer.getFramebuffer());
            this.bloomBuffer.add(() -> {
                drawEngine.beginBoundFramebuffer();
                try {
                    renderCommandQueue.renderRecorded(drawCtx);
                } finally {
                    drawEngine.end();
                }
            });
            this.bloomBuffer.renderToFramebuffer();
        }
        if (shouldRenderOverlay) {
            this.headerBlur.apply(32);
            this.searchBlur.apply(16, 2.0f);
            FramebufferUtil.bindForRendering(Mc.INSTANCE.getFramebuffer());
        }
        if (hasBloom && anyVisible) {
            this.bloom.apply(this.bloomBuffer.getFramebuffer(), 16);
        }
        drawEngine.begin();
        try {
            if (this.backgroundAlphaValue != 0) {
                drawEngine.rectangle(matrixStack.last().pose(), 0.0f, 0.0f, this.resolution.screenWidth(), this.resolution.screenHeight(), drawEngine.colorStack().darkAlpha(this.backgroundAlphaValue));
            }
            ColorPickerWindow topPicker = null;
            for (AbstractWindow abstractWindow : this.windows) {
                if (abstractWindow instanceof ColorPickerWindow picker && picker.isRendering()) {
                    topPicker = picker;
                    continue;
                }
                if (abstractWindow.visible()) {
                    abstractWindow.render(drawCtx);
                }
            }
            if (topPicker != null && topPicker.visible()) {
                topPicker.render(drawCtx);
            }
        } finally {
            drawEngine.end();
        }
        this.window.tickRefreshKeysIfNeeded();
    }

    public boolean shouldRefreshBlurEveryFrame() {
        MenuWindow menuWindow = Raven.INSTANCE.menuWindow();
        return menuWindow != null && menuWindow.shouldRefreshBlurEveryFrame();
    }

    public boolean handleInput(InputEventContext inputEventContext) {
        InputEvent inputEvent = inputEventContext.inputEvent();
        this.mousePosition = inputEventContext.logicalMousePosition();
        boolean z = false;
        boolean isButton = inputEvent.type() == InputType.BUTTON;
        int size = this.windows.size();
        int version = this.windowVersion;
        for (int index = this.windows.size() - 1; index >= 0; index--) {
            AbstractWindow candidate = this.windows.get(index);
            if (candidate instanceof ColorPickerWindow picker && picker.opened()
                    && picker.handleInput(inputEventContext, false)) {
                return true;
            }
        }
        if (!this.windows.isEmpty()) {
            for (int size2 = this.windows.size() - 1; size2 >= 0; size2--) {
                AbstractWindow abstractWindow2 = this.windows.get(size2);
                if (abstractWindow2 instanceof ColorPickerWindow picker && picker.opened()) {
                    continue;
                }
                if (abstractWindow2.handleInput(inputEventContext, false)) {
                    z = true;
                    if (!isButton || this.windows.size() != size || this.windowVersion != version || size2 == this.windows.size() - 1) {
                        break;
                    }
                    this.windows.remove(size2);
                    this.windows.add(abstractWindow2);
                    this.windowVersion++;
                    break;
                }
            }
        }
        return z;
    }

    public boolean interceptKeyboardIfScreenPresent() {
        return Mc.INSTANCE.getCurrentScreen() != null && this.window.interceptKeyboardIfScreenPresent();
    }

    public boolean interceptKeyboard() {
        return window().interceptKeyboard();
    }

    public boolean interceptCursorIfScreenNotPresent() {
        return Mc.INSTANCE.getCurrentScreen() == null && this.window.interceptCursorfScreenNotPresent();
    }

    public void handleResize(int i, int i2) {
        this.resolution = new ScreenResolution(i, i2);
        if (this.autoDpiScale) {
            updateAutoDpiScale();
        }
        MenuWindow menuWindow = Raven.INSTANCE.menuWindow();
        if (menuWindow != null) {
            menuWindow.requestCentering();
        }
    }

    public void newWindow(AbstractWindow abstractWindow2) {
        if (this.windows.contains(abstractWindow2)) {
            return;
        }
        this.windows.add(abstractWindow2);
        this.windowVersion++;
    }

    public void setManualDpiScaleFactor(float f) {
        this.autoDpiScale = false;
        float sanitized = sanitizeDpiScale(f);
        if (!this.dpiInitialized) {
            this.dpiScaleFactor = sanitized;
            this.dpiScaleAnimation.set(sanitized);
            this.dpiInitialized = true;
        }
        this.dpiScaleAnimation.destination(sanitized);
        MenuWindow menuWindow = Raven.INSTANCE.menuWindow();
        if (menuWindow != null) {
            menuWindow.requestCentering();
        }
    }

    public void enableAutoDpiScale() {
        this.autoDpiScale = true;
        updateAutoDpiScale();
        MenuWindow menuWindow = Raven.INSTANCE.menuWindow();
        if (menuWindow != null) {
            menuWindow.requestCentering();
        }
    }

    public void updateAutoDpiScale() {
        ScreenResolution res;
        if (this.resolution != null) {
            res = this.resolution;
        } else if (Mc.INSTANCE.getWindow() == null) {
            return;
        } else {
            res = ScreenResolution.resolution();
        }
        float computed = computeDpiScale(res);
        if (!this.dpiInitialized) {
            this.dpiScaleFactor = computed;
            this.dpiScaleAnimation.set(computed);
            this.dpiInitialized = true;
        }
        this.dpiScaleAnimation.destination(computed);
    }

    public float computeDpiScale(ScreenResolution screenResolution) {
        float screenWidth = Math.max(1.0f, screenResolution.screenWidth());
        float screenHeight = Math.max(1.0f, screenResolution.screenHeight());
        float referenceScale = Math.max(1.0f, Math.min(screenWidth / REFERENCE_WIDTH,
                screenHeight / REFERENCE_HEIGHT));
        float availableWidth = Math.max(1.0f, screenWidth - AUTO_SCALE_MARGIN * 2.0f);
        float availableHeight = Math.max(1.0f, screenHeight - AUTO_SCALE_MARGIN * 2.0f);
        float fitScale = Math.min(availableWidth / MenuWindow.MENU_WIDTH,
                availableHeight / MenuWindow.MENU_HEIGHT);
        float upperBound = Math.min(AUTO_SCALE_MAX, fitScale);
        return Math.max(0.01f, Math.min(referenceScale, upperBound));
    }

    public float sanitizeDpiScale(float f) {
        if (f <= 0.0f) {
            return 1.0f;
        }
        return f;
    }

    public void removeWindow(AbstractWindow abstractWindow2) {
        if (this.windows.remove(abstractWindow2)) {
            this.windowVersion++;
        }
    }

    public void bringToFront(AbstractWindow abstractWindow2) {
        if (!this.windows.remove(abstractWindow2)) {
            newWindow(abstractWindow2);
        } else {
            this.windows.add(abstractWindow2);
            this.windowVersion++;
        }
    }

    public <T extends AbstractWindow> T getWindow(Class<T> cls) {
        for (AbstractWindow abstractWindow2 : this.windows) {
            if (cls.isInstance(abstractWindow2)) {
                return cls.cast(abstractWindow2);
            }
        }
        return null;
    }

    public void showCursor(InputInterceptor inputInterceptor) {
        Mc.INSTANCE.getMinecraft().cursorEntered();
        this.savedCursorMode = inputInterceptor.cursorInputMode();
        inputInterceptor.showCursor();
    }

    public void revertCursor(InputInterceptor inputInterceptor) {
        if (Mc.INSTANCE.getCurrentScreen() == null) {
            inputInterceptor.cursorInputMode(this.savedCursorMode);
        }
        Mc.INSTANCE.getMinecraft().cursorEntered();
    }

    public void recenterMouse() {
        if (this.resolution == null || Mc.INSTANCE.getCurrentScreen() != null) {
            return;
        }
        this.window.mousePosition(new PixelPoint(this.resolution.screenWidth() / 2, this.resolution.screenHeight() / 2));
    }

    public float dpiScaleFactor() {
        return this.dpiScaleFactor;
    }

    public boolean autoDpiScale() {
        return this.autoDpiScale;
    }

    public InputInterceptor window() {
        return this.window;
    }

    public BlurEffect headerBlur() {
        return this.headerBlur;
    }

    public KawaseBlurEffect menuSceneBlur() {
        return this.menuSceneBlur;
    }

    public KawaseBlurEffect menuUiBlur() {
        return this.menuUiBlur;
    }

    public BlurEffect searchBlur() {
        return this.searchBlur;
    }

    public BloomEffect bloom() {
        return this.bloom;
    }

    public ScaledRenderTarget bloomBuffer() {
        return this.bloomBuffer;
    }

    public WindowController backgroundAlpha(int i) {
        this.backgroundAlphaValue = i;
        return this;
    }

    public ScreenResolution resolution() {
        return this.resolution;
    }
}
