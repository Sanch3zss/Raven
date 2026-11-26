package sanch3zss.raven.ui.hud;


import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.gl.StencilBufferUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.*;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.menu.window.MenuWindow;
import sanch3zss.raven.ui.setting.Setting;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.*;

public abstract class Draggable {
    public final String name;
    public float x;
    public float y;
    public float aC;
    public float L;
    public BooleanSupplier visibility;
    public boolean dragging;
    public boolean clicked;
    public boolean cursorHovered;
    public SnapManager snapGrid;
    public List<Draggable> siblingWidgets;

    public ScreenResolution resolution;
    public final List<Setting> settings = new ArrayList();

    public ToggleAnimator clickAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public ToggleAnimator hoveredAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public ToggleAnimator tooltipAnimation = new ToggleAnimator(200, Easings.EASE_IN_OUT_CUBIC);
    public boolean excludeFromSnapGrid = false;
    private float hudScale = 1.0f;

    public final Stopwatch lastResizeTimer = new Stopwatch(false);

    public Draggable(String str, BooleanSupplier booleanSupplier) {
        this.name = str;
        this.visibility = booleanSupplier;
    }

    public void addSettings(Setting... arr) {
        this.settings.addAll(Arrays.asList(arr));
    }

    public void draw(DragRenderContext dragRenderContext) {
        if (this.resolution == null || this.resolution.screenWidth() <= 0 || this.resolution.screenHeight() <= 0) {
            this.resolution = ScreenResolution.resolution();
        }
        if (isVisible()) {
            PoseStack matrices = dragRenderContext.matrixStack();
            matrices.pushPose();
            try {
                matrices.translate(this.x, this.y, 0.0f);
                float visualScale = visualScale();
                matrices.scale(visualScale, visualScale, 1.0f);
                matrices.translate(-this.x, -this.y, 0.0f);
                render(dragRenderContext);
            } finally {
                matrices.popPose();
            }
        }
    }

    public final void collectVanillaItems(DragRenderContext context) {
        if (!isVisible()) {
            return;
        }
        PoseStack matrices = context.matrixStack();
        matrices.pushPose();
        try {
            matrices.translate(this.x, this.y, 0.0f);
            float visualScale = visualScale();
            matrices.scale(visualScale, visualScale, 1.0f);
            matrices.translate(-this.x, -this.y, 0.0f);
            collectItems(context);
        } finally {
            matrices.popPose();
        }
    }

    protected void collectItems(DragRenderContext context) {
    }

    public void handleResize(int i, int i2) {
        this.resolution = new ScreenResolution(i, i2);
        this.lastResizeTimer.reset();
    }

    public void applySavedPosition(float f, float f2) {
        this.x = f;
        this.y = f2;
    }

    public void updateDrag(DragRenderContext dragRenderContext) {
        ScreenResolution resolution;
        MouseHandler mouse = Mc.INSTANCE.getMouse();
        Screen currentScreen = Mc.INSTANCE.getCurrentScreen();
        MenuWindow menuWindow = Raven.INSTANCE.menuWindow();
        if (menuWindow != null && menuWindow.visible()) {
            WidgetSettingsPopup widgetSettingsPopup2 = (WidgetSettingsPopup) Raven.INSTANCE.windowController().getWindow(WidgetSettingsPopup.class);
            if (widgetSettingsPopup2 != null) {
                widgetSettingsPopup2.closeWindow();
                return;
            }
            return;
        }
        if (!(currentScreen instanceof ChatScreen)) {
            this.dragging = false;
            this.clicked = false;
            if (this.snapGrid != null) {
                this.snapGrid.clear();
            }
            WidgetSettingsPopup widgetSettingsPopup = (WidgetSettingsPopup) Raven.INSTANCE.windowController().getWindow(WidgetSettingsPopup.class);
            if (widgetSettingsPopup != null) {
                widgetSettingsPopup.close();
                return;
            }
            return;
        }
        if (!this.dragging || (resolution = dragRenderContext.resolution()) == null || resolution.screenWidth() <= 0 || resolution.screenHeight() <= 0) {
            return;
        }
        float fScaleFactor = dragRenderContext.scaleFactor();
        float x = (float) (mouse.xpos() / ((double) fScaleFactor));
        float y = (float) (mouse.ypos() / ((double) fScaleFactor));
        float f = x - this.aC;
        float f2 = y - this.L;
        if (this.snapGrid == null || this.siblingWidgets == null || isExcludeFromSnapGrid()) {
            this.x = f;
            this.y = f2;
        } else {
            SnapResult calculateSnap = this.snapGrid.calculateSnap(this, this.siblingWidgets, f, f2, resolution.screenWidth(), resolution.screenHeight());
            this.x = calculateSnap.x;
            this.y = calculateSnap.y;
        }
        boolean z = this.x < 0.0f || this.y < 0.0f || this.x + scaledWidth() > ((float) resolution.screenWidth()) || this.y + scaledHeight() > ((float) resolution.screenHeight());
        if (resolution.isExceeding(scaledWidth(), scaledHeight()) && z) {
            this.x = MathUtil.clamp(this.x, 0.0f, resolution.screenWidth() - scaledWidth());
            this.y = MathUtil.clamp(this.y, 0.0f, resolution.screenHeight() - scaledHeight());
        }
    }

    public void animation(WeightedEngine weightedEngine) {
        this.hoveredAnimation.state(this.cursorHovered).animate(weightedEngine);
        this.tooltipAnimation.state(this.cursorHovered && (Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen)).animate(weightedEngine);
        this.clickAnimation.state(this.clicked).animate(weightedEngine);
        animate(weightedEngine);
    }

    public void updateWidget() {
        if (isVisible()) {
            update();
        }
    }

    public boolean onCursor(MouseMoveInput mouseMoveInput) {
        boolean zIsWithinBounds = mouseMoveInput.isWithinBounds(this.x, this.y, scaledWidth(), scaledHeight());
        boolean zCursor = cursor(mouseMoveInput, zIsWithinBounds);
        if (!isVisible()) {
            this.cursorHovered = false;
            return false;
        }
        if (mouseMoveInput.intercepted()) {
            this.cursorHovered = false;
        } else {
            this.cursorHovered = zIsWithinBounds;
            if (zIsWithinBounds) {
                return true;
            }
        }
        return zCursor;
    }

    public boolean onClick(MouseButtonInput2 mouseButtonInput2) {
        if (!isVisible()) {
            return false;
        }
        boolean zIsWithinBounds = mouseButtonInput2.isWithinBounds(this.x, this.y, scaledWidth(), scaledHeight());
        boolean zClick = click(mouseButtonInput2, zIsWithinBounds);
        if (mouseButtonInput2.isLeftButtonPressed()) {
            if (!mouseButtonInput2.intercepted() && mouseButtonInput2.press() && zIsWithinBounds) {
                if (zClick) {
                    return true;
                }
                this.clicked = true;
                float fMouseX = mouseButtonInput2.mouseX();
                float fMouseY = mouseButtonInput2.mouseY();
                this.aC = fMouseX - this.x;
                this.L = fMouseY - this.y;
                this.dragging = true;
                return true;
            }
            if (mouseButtonInput2.release()) {
                this.clicked = false;
                this.dragging = false;
                if (this.snapGrid == null) {
                    return true;
                }
                this.snapGrid.clear();
                return true;
            }
        }
        if (!mouseButtonInput2.isRightButtonPressed() || mouseButtonInput2.intercepted() || !mouseButtonInput2.press() || !zIsWithinBounds || zClick || this.dragging) {
            return false;
        }
        WidgetSettingsPopup widgetSettingsPopup2 = (WidgetSettingsPopup) Raven.INSTANCE.windowController().getWindow(WidgetSettingsPopup.class);
        if (this.settings.isEmpty()) {
            if (widgetSettingsPopup2 == null) {
                return true;
            }
            widgetSettingsPopup2.close();
            return true;
        }
        if (widgetSettingsPopup2 != null) {
            if (widgetSettingsPopup2.matchesWidget(this.name) && widgetSettingsPopup2.isOpen()) {
                widgetSettingsPopup2.closeWindow();
                return true;
            }
            widgetSettingsPopup2.close();
            Raven.INSTANCE.windowController().removeWindow(widgetSettingsPopup2);
        }
        WidgetSettingsPopup widgetSettingsPopup = new WidgetSettingsPopup(this.name, this.settings);
        Raven.INSTANCE.windowController().newWindow(widgetSettingsPopup);
        widgetSettingsPopup.openAt(this.x, this.y, scaledWidth(), scaledHeight(), mouseButtonInput2.resolution(), mouseButtonInput2.scaleFactor());
        return true;
    }

    public void setHudScale(float hudScale) {
        this.hudScale = MathUtil.clamp(hudScale, 0.5f, 2.0f);
    }

    public float scaledWidth() {
        return width() * visualScale();
    }

    public float scaledHeight() {
        return height() * visualScale();
    }

    public float hudScale() {
        return this.hudScale;
    }

    protected float baseScale() {
        return 1.0f;
    }

    protected float visualScale() {
        return this.hudScale * baseScale();
    }

    protected boolean isWithinScaledBounds(MouseButtonInput2 input, float x, float y, float width, float height) {
        float localX = this.x + (input.mouseX() - this.x) / visualScale();
        float localY = this.y + (input.mouseY() - this.y) / visualScale();
        return localX >= x && localX <= x + width && localY >= y && localY <= y + height;
    }

    protected boolean isWithinScaledBounds(MouseMoveInput input, float x, float y, float width, float height) {
        float mouseX = (float) (input.mouse().xpos() / input.scaleFactor());
        float mouseY = (float) (input.mouse().ypos() / input.scaleFactor());
        float localX = this.x + (mouseX - this.x) / visualScale();
        float localY = this.y + (mouseY - this.y) / visualScale();
        return localX >= x && localX <= x + width && localY >= y && localY <= y + height;
    }

    public void drawTooltip(DragRenderContext dragRenderContext) {
        float fSmoothAnimation = this.tooltipAnimation.smoothAnimation();
        if (fSmoothAnimation <= 0.0f) {
            return;
        }
        DrawEngine drawEngine = dragRenderContext.drawEngine();
        PoseStack matrixStack = dragRenderContext.matrixStack();
        ColorStack colorStack = drawEngine.colorStack();
        List<String> arrayList = hints();
        float height = Fonts.INTER_SEMIBOLD.get().getHeight(12.0f);
        float size = (this.y - (height * arrayList.size())) - 4.0f;
        boolean z = size < 0.0f;
        if (z) {
            size = this.y + scaledHeight() + 4.0f;
        }
        float f = (z ? -4 : 4) * (1.0f - fSmoothAnimation);
        for (int i = 0; i < arrayList.size(); i++) {
            drawEngine.msdfFont(matrixStack.last().pose(), Fonts.INTER_SEMIBOLD.get(), (String) arrayList.get(i), ((int) this.x) + 3, size + (i * height) + f, 12.0f, 0.0f, colorStack.computeColor(fSmoothAnimation, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK, StencilBufferUtil.STENCIL_MASK));
        }
    }

    protected List<String> hints() {
        List<String> hints = new ArrayList<>();
        hints.add(Lang.WIDGET_DRAG_HINT.effective());
        if (!this.settings.isEmpty()) {
            hints.add(Lang.WIDGET_SETTINGS_HINT.effective());
        }
        return hints;
    }

    public boolean hasTooltip() {
        return true;
    }

    public abstract void layout(DragRenderContext dragRenderContext);

    public abstract void render(DragRenderContext dragRenderContext);

    public abstract boolean click(MouseButtonInput2 mouseButtonInput2, boolean z);

    public abstract boolean cursor(MouseMoveInput mouseMoveInput, boolean z);

    public abstract void animate(WeightedEngine weightedEngine);

    public abstract void update();

    public abstract float width();

    public abstract float height();

    public void resetCursor() {
        this.cursorHovered = false;
    }

    public boolean isVisible() {
        return this.visibility.getAsBoolean();
    }

    public boolean isRendering() {
        return isVisible();
    }

    public String getName() {
        return this.name;
    }

    public List<Setting> getSettings() {
        return this.settings;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getDragX() {
        return this.aC;
    }

    public float getDragY() {
        return this.L;
    }

    public BooleanSupplier getVisibility() {
        return this.visibility;
    }

    public boolean isDragging() {
        return this.dragging;
    }

    public ToggleAnimator getClickAnimation() {
        return this.clickAnimation;
    }

    public ToggleAnimator getHoveredAnimation() {
        return this.hoveredAnimation;
    }

    public ToggleAnimator getTooltipAnimation() {
        return this.tooltipAnimation;
    }

    public boolean isClicked() {
        return this.clicked;
    }

    public boolean isCursorHovered() {
        return this.cursorHovered;
    }

    public SnapManager getSnapGrid() {
        return this.snapGrid;
    }

    public List<Draggable> getSiblingWidgets() {
        return this.siblingWidgets;
    }

    public boolean isExcludeFromSnapGrid() {
        return this.excludeFromSnapGrid;
    }

    public ScreenResolution getResolution() {
        return this.resolution;
    }

    public Stopwatch getLastResizeTimer() {
        return this.lastResizeTimer;
    }

    public void setX(float f) {
        this.x = f;
    }

    public void setY(float f) {
        this.y = f;
    }

    public void setDragX(float f) {
        this.aC = f;
    }

    public void setDragY(float f) {
        this.L = f;
    }

    public void setSnapGrid(SnapManager snapManager) {
        this.snapGrid = snapManager;
    }

    public void setSiblingWidgets(List<Draggable> list) {
        this.siblingWidgets = list;
    }

    public void setExcludeFromSnapGrid(boolean z) {
        this.excludeFromSnapGrid = z;
    }
}
