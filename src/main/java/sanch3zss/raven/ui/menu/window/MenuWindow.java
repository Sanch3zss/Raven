package sanch3zss.raven.ui.menu.window;


import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.engine.OverlayCommandQueue;
import sanch3zss.raven.render.engine.OverlayManager;
import sanch3zss.raven.render.gl.FramebufferUtil;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import sanch3zss.raven.client.*;
import sanch3zss.raven.feature.module.misc.ClickGuiModule;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.InputEvent;import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.TooltipService;
import sanch3zss.raven.ui.component.layout.PlainContainer;
import sanch3zss.raven.ui.hud.*;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.header.MenuHeaderContainer;
import sanch3zss.raven.ui.menu.layout.MenuContentArea;
import sanch3zss.raven.ui.menu.overlay.*;
import sanch3zss.raven.ui.window.WindowController;
import sanch3zss.raven.util.math.interpolation.Easings;

public class MenuWindow extends AbstractWindow {
    public static final float MENU_WIDTH = MenuStyle.LARGE_WIDTH;
    public static final float MENU_HEIGHT = MenuStyle.LARGE_HEIGHT;
    public static float HEADER_HEIGHT = MenuStyle.HEADER_HEIGHT;
    public boolean needsCentering;
    public final ToggleAnimator openAnimation;
    public final MenuHeaderContainer headerContainer;

    public final MenuContentArea contentArea;

    public final DraggableContainer draggableBehavior;

    public final SearchOverlay searchContainer;

    public final ActionConfirmDialog actionConfirmationDialogContainer;

    public final PlainContainer tooltipLayer;

    public final TooltipService tooltipService;

    public final OverlayManager priorityOverlayHandler;
    public boolean menuOpen;
    private RenderTarget fadeTarget;

    public MenuWindow() {
        super(MENU_WIDTH, MENU_HEIGHT);
        this.needsCentering = true;
        this.openAnimation = new ToggleAnimator(280, 200, Easings.CIRC_OUT, Easings.CIRC_OUT);
        this.headerContainer = new MenuHeaderContainer();
        this.draggableBehavior = new DraggableContainer().target(this);
        this.searchContainer = new SearchOverlay();
        this.actionConfirmationDialogContainer = new ActionConfirmDialog();
        this.tooltipLayer = new PlainContainer();
        this.tooltipService = new TooltipService(this.tooltipLayer);
        this.priorityOverlayHandler = new OverlayManager();
        this.menuOpen = false;
        this.contentArea = new MenuContentArea();
        this.visible = false;
        addChild(this.draggableBehavior);
        addChild(this.contentArea);
        addChild(this.tooltipLayer);
        addChild(this.headerContainer);
        addChild(this.searchContainer);
        addChild(this.actionConfirmationDialogContainer);
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        if (this.needsCentering) {
            center(layoutScaleContext);
            this.needsCentering = false;
        }
        if (this.openAnimation.isZero()) {
            this.headerContainer.visible(false);
            this.contentArea.visible(false);
            this.draggableBehavior.visible(false);
            return;
        }
        this.headerContainer.setPosition(x(), y());
        this.headerContainer.visible(true);
        this.contentArea.setPosition(x() + MenuStyle.WORKSPACE_X, y() + HEADER_HEIGHT);
        this.contentArea.visible(true);
        this.searchContainer.setPosition(x(), y());
        this.searchContainer.setSize(MENU_WIDTH, MENU_HEIGHT);
        this.actionConfirmationDialogContainer.setPosition(x(), y());
        super.layout(layoutScaleContext);
        setSize(MENU_WIDTH, MENU_HEIGHT);
        this.draggableBehavior.setPosition(x(), y());
        this.draggableBehavior.setSize(width(), MenuStyle.HEADER_HEIGHT);
        this.draggableBehavior.visible(true);
        super.onMenuDrag(this.draggableBehavior.isDragging());
    }

    @Override
    public void render(DrawCtx drawCtx) {
        drawCtx = drawCtx.withTheme(MenuStyle.menuTheme(drawCtx.theme()));
        InputInterceptor window = drawCtx.window();
        Raven.INSTANCE.windowController().backgroundAlpha(0);
        if (!this.openAnimation.isZero()) {
            if (this.menuOpen) {
                window.interceptCursorfScreenNotPresent(true);
                window.interceptKeyboardIfScreenPresent(true);
            }
            ColorStack colorStack = drawCtx.drawEngine().colorStack();
            boolean fading = !this.openAnimation.isOne();
            int previousFramebuffer = 0;
            if (fading) {
                drawCtx.drawEngine().draw();
                previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
                bindFadeTarget();
            }
            colorStack.push();
            drawCtx.matrixStack().pushPose();
            if (fading) {
                float progress = this.openAnimation.smoothAnimation();
                float centerX = drawCtx.layoutContext().toPhysical(x() + MENU_WIDTH * 0.5f);
                float centerY = drawCtx.layoutContext().toPhysical(y() + MENU_HEIGHT * 0.5f);
                float baseScale = this.menuOpen ? 0.93f : 0.96f;
                float offset = this.menuOpen ? 14.0f : 10.0f;
                float scale = baseScale + (1.0f - baseScale) * progress;
                float yOffset = (1.0f - progress) * offset;
                drawCtx.matrixStack().translate(centerX,
                        centerY + drawCtx.layoutContext().toPhysical(yOffset), 0.0f);
                drawCtx.matrixStack().scale(scale, scale, scale);
                drawCtx.matrixStack().translate(-centerX, -centerY, 0.0f);
            }
            renderBackground(drawCtx);
            renderChild(this.draggableBehavior, drawCtx);
            renderChild(this.contentArea, drawCtx);
            renderChild(this.headerContainer, drawCtx);
            drawCtx.drawEngine().draw();
            Raven.INSTANCE.windowController().menuUiBlur().apply(Mc.INSTANCE.getFramebuffer());
            this.contentArea.renderOverlays(drawCtx);
            this.headerContainer.renderMenus(drawCtx);
            this.priorityOverlayHandler.renderPopup(drawCtx);
            renderChild(this.tooltipLayer, drawCtx);
            renderChild(this.searchContainer, drawCtx);
            renderChild(this.actionConfirmationDialogContainer, drawCtx);
            colorStack.pop();
            drawCtx.matrixStack().popPose();
            if (fading) {
                drawCtx.drawEngine().draw();
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
                drawFadeTarget(drawCtx, this.openAnimation.smoothAnimation());
            }
            if (!window.cursorVisible() && this.menuOpen && inGame()) {
                this.openAnimation.state(false);
                this.menuOpen = false;
                closeColorPicker();
            }
        }
    }

    private void bindFadeTarget() {
        RenderTarget main = Mc.INSTANCE.getFramebuffer();
        int width = main.width;
        int height = main.height;
        this.fadeTarget = FramebufferUtil.ensureFramebuffer(this.fadeTarget, width, height,
                () -> new MainTarget(width, height));
        FramebufferUtil.resizeIfNeeded(this.fadeTarget, width, height);
        FramebufferUtil.bindForRendering(this.fadeTarget);
        GL11.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
    }

    private void drawFadeTarget(DrawCtx drawCtx, float progress) {
        DrawEngine drawEngine = drawCtx.drawEngine();
        int channel = Math.round(Math.max(0.0f, Math.min(1.0f, progress)) * 255.0f);
        int color = channel << 24 | channel << 16 | channel << 8 | channel;
        drawEngine.texture(new Matrix4f(), 0.0f, 0.0f, drawCtx.resolution().screenWidth(),
                drawCtx.resolution().screenHeight(), 0.0f, 1.0f, 1.0f, 0.0f,
                drawEngine.bindTexture(FramebufferUtil.getColorAttachmentId(this.fadeTarget)), color);
        drawEngine.switchBlendFuncAndDraw(() -> GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA));
    }

    public void renderChild(Widget widget, DrawCtx drawCtx) {
        if (widget instanceof WidgetParent) {
            ((WidgetParent) widget).render(drawCtx);
        }
    }

    public void renderBackground(DrawCtx drawCtx) {
        MenuStyle.drawRootGlass(drawCtx, x(), y(), MENU_WIDTH, MENU_HEIGHT, MenuStyle.WINDOW_RADIUS);
        MenuStyle.drawSidebarPanel(drawCtx, x(), y(), MenuStyle.SIDEBAR_WIDTH, MENU_HEIGHT, MenuStyle.WINDOW_RADIUS);
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.openAnimation.animate(weightedEngine);
        boolean zIsZero = this.openAnimation.isZero();
        if (!zIsZero) {
            this.visible = true;
            super.animation(weightedEngine);
            return;
        }
        Raven.INSTANCE.windowController().backgroundAlpha(0);
        if (this.visible) {
            close();
        }
        this.headerContainer.visible(false);
        this.contentArea.visible(false);
        this.draggableBehavior.visible(false);
    }

    @Override
    public boolean handleInput(InputEventContext inputEventContext, boolean z) {
        InputEvent inputEvent = inputEventContext.inputEvent();
        if (this.menuOpen && !z && inputEvent instanceof KeyInput keyInput
                && keyInput.keyAction().press()
                && keyInput.keyCode() == GLFW.GLFW_KEY_F
                && (keyInput.modifiers().mods() & GLFW.GLFW_MOD_CONTROL) != 0) {
            this.searchContainer.open();
            return true;
        }
        boolean z2 = this.menuOpen;
        boolean zHandleInput = z;
        if (z2) {
            boolean zHandleInput2 = zHandleInput | this.actionConfirmationDialogContainer.handleInput(inputEventContext, zHandleInput);
            boolean zHandleInput3 = zHandleInput2 | this.searchContainer.handleInput(inputEventContext, zHandleInput2);
            zHandleInput3 |= this.priorityOverlayHandler.handlePopupInput(inputEventContext, zHandleInput3);
            boolean zHandleInput4 = zHandleInput3 | this.headerContainer.handleInput(inputEventContext, zHandleInput3);
            boolean zHandleInput6 = zHandleInput4 | this.contentArea.handleInput(inputEventContext, zHandleInput4);
            zHandleInput = zHandleInput6 | this.draggableBehavior.handleInput(inputEventContext, zHandleInput6);
        }
        if (!zHandleInput) {
            if (inputEvent instanceof KeyInput) {
                KeyInput keyInput = (KeyInput) inputEvent;
                WindowController windowController = Raven.INSTANCE.windowController();
                if (keyInput.keyAction().press()) {
                    int iKeyCode = keyInput.keyCode();
                    if (Raven.INSTANCE.moduleRepository().get(ClickGuiModule.class).isOpenKey(iKeyCode)) {
                        if (!this.menuOpen && typingInScreen()) {
                            return false;
                        }
                        this.menuOpen = !this.menuOpen;
                        this.openAnimation.state(this.menuOpen);
                        if (this.menuOpen) {
                            open();
                            KeyMapping.releaseAll();
                            if (inGame()) {
                                windowController.showCursor(windowController.window());
                                windowController.recenterMouse();
                            }
                        } else {
                            closeMenu(windowController);
                        }
                        return true;
                    }
                    if (iKeyCode == 256 && visible() && this.menuOpen) {
                        this.menuOpen = false;
                        this.openAnimation.state(false);
                        closeMenu(windowController);
                        return true;
                    }
                }
            }
        }
        if (this.menuOpen && (inputEventContext.inputEvent() instanceof KeyInput
                || inputEventContext.inputEvent() instanceof MouseButtonInput
                || inputEventContext.inputEvent() instanceof ScrollInput)) {
            return true;
        }
        return zHandleInput;
    }

    @Override
    public void collectBlurElements(OverlayCommandQueue overlayCommandQueue2) {

    }

    @Override
    public void close() {
        closeColorPicker();
        this.draggableBehavior.setDragging(false);
        super.close();
    }

    private void closeMenu(WindowController windowController) {
        closeColorPicker();
        if (inGame()) {
            windowController.revertCursor(windowController.window());
            windowController.recenterMouse();
            KeyMapping.setAll();
        }
    }

    private static boolean inGame() {
        return Mc.INSTANCE.isWorldLoaded() && Mc.INSTANCE.getCurrentScreen() == null;
    }

    private static boolean typingInScreen() {
        Screen screen = Mc.INSTANCE.getCurrentScreen();
        return screen != null && screen.getFocused() instanceof EditBox;
    }

    private void closeColorPicker() {
        ColorPickerWindow picker = (ColorPickerWindow) Raven.INSTANCE.windowController()
                .getWindow(ColorPickerWindow.class);
        if (picker != null) {
            picker.closePicker();
        }
    }

    public void requestCentering() {
        this.needsCentering = true;
    }

    public boolean shouldRefreshBlurEveryFrame() {
        return !this.openAnimation.finished() || !this.searchContainer.openAnimator.finished() || !this.actionConfirmationDialogContainer.openAnimator.finished();
    }

    public boolean hasOpenGlassOverlay() {
        return this.headerContainer.hasOpenGlassOverlay()
                || !this.searchContainer.openAnimator.isZero()
                || this.actionConfirmationDialogContainer.isOpen()
                || this.priorityOverlayHandler.hasOpenOverlay();
    }

    public MenuContentArea contentArea() {
        return this.contentArea;
    }

    public DraggableContainer draggableBehavior() {
        return this.draggableBehavior;
    }

    public SearchOverlay searchContainer() {
        return this.searchContainer;
    }

    public ActionConfirmDialog actionConfirmationDialogContainer() {
        return this.actionConfirmationDialogContainer;
    }

    public TooltipService tooltipService() {
        return this.tooltipService;
    }

    public OverlayManager priorityOverlayHandler() {
        return this.priorityOverlayHandler;
    }
}
