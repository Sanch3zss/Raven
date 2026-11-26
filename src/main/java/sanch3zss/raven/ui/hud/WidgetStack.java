package sanch3zss.raven.ui.hud;


import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.gl.FramebufferUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2fStack;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.MouseButtonEvent2;
import sanch3zss.raven.feature.module.render.CustomTagsModule;
import sanch3zss.raven.feature.setting.ButtonAction;import sanch3zss.raven.render.animation.AnimationStack2;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.shader.KawaseBlurEffect;
import sanch3zss.raven.ui.hud.impl.*;
import sanch3zss.raven.util.math.interpolation.DeltaTimeTracker;

public class WidgetStack {
    public ScreenResolution resolution;
    public final List<Draggable> widgets = new ArrayList<>();
    public final DeltaTimeTracker deltaTimeTracker = new DeltaTimeTracker();
    public final AnimationStack2 animationStack = new AnimationStack2();
    public final SnapManager snapManager = new SnapManager();
    public final KawaseBlurEffect hudBlur = new KawaseBlurEffect();
    private boolean blurInitialized;
    private float hudScale = 1.0f;
    private final Map<Draggable, Float> widgetScales = new IdentityHashMap<>();
    private final List<DragRenderContext.VanillaItemDraw> vanillaItemDraws = new ArrayList<>();
    private final List<DragRenderContext.VanillaSpriteDraw> vanillaSpriteDraws = new ArrayList<>();
    private final List<DragRenderContext.VanillaTextDraw> vanillaTextDraws = new ArrayList<>();
    private final List<Integer> itemLayers = new ArrayList<>();
    private final List<Integer> spriteLayers = new ArrayList<>();
    private final List<Integer> textLayers = new ArrayList<>();
    private final List<float[]> occluders = new ArrayList<>();
    private int collectingLayer;
    private GuiGraphicsExtractor vanillaDrawContext;
    private boolean customHudPrepared;

    public Draggable activeWidget = null;

    public void init() {
        if (!this.blurInitialized) {
            this.hudBlur.init();
            this.blurInitialized = true;
        }
    }

    public void draw(GuiGraphicsExtractor vanillaDrawContext) {
        Mc mc = Mc.INSTANCE;
        clearVanillaDraws();
        if (!isHudScreenAllowed()) {
            this.customHudPrepared = false;
            this.vanillaDrawContext = null;
            return;
        }
        this.vanillaDrawContext = vanillaDrawContext;
        ScreenResolution resolution = ScreenResolution.resolution();
        float fMax = Math.max(Raven.INSTANCE.windowController().dpiScaleFactor(), 0.01f);
        this.resolution = new ScreenResolution(Math.max(1, Math.round(resolution.screenWidth() / fMax)), Math.max(1, Math.round(resolution.screenHeight() / fMax)));
        PoseStack matrixStack = new PoseStack();
        matrixStack.scale(fMax, fMax, 1.0f);
        WeightedEngine weightedEngine = new WeightedEngine(this.deltaTimeTracker.elapsedUnit(), this.animationStack);
        MouseHandler mouse = mc.getMouse();
        for (Draggable draggable : this.widgets) {
            draggable.setHudScale(this.widgetScales.getOrDefault(draggable, this.hudScale));
            draggable.setSnapGrid(this.snapManager);
            draggable.setSiblingWidgets(this.widgets);
        }
        if (mc.getCurrentScreen() instanceof ChatScreen) {
            boolean z = false;
            for (int size = this.widgets.size() - 1; size >= 0; size--) {
                if (this.widgets.get(size).onCursor(new MouseMoveInput(z, mouse, this.resolution, fMax))) {
                    z = true;
                }
            }
        } else {
            this.widgets.forEach(Draggable::resetCursor);
            this.snapManager.clear();
        }
        boolean anyDragging = isAnyDragging();
        this.snapManager.animate(weightedEngine, anyDragging);
        this.animationStack.begin();
        this.widgets.forEach(w -> w.animation(weightedEngine));
        this.animationStack.end();
        DragRenderContext dragRenderContext = new DragRenderContext(this.resolution, mouse, matrixStack,
                Raven.INSTANCE.drawEngine(), Raven.INSTANCE.theme(), fMax,
                item -> {
                    this.vanillaItemDraws.add(item);
                    this.itemLayers.add(this.collectingLayer);
                },
                sprite -> {
                    this.vanillaSpriteDraws.add(sprite);
                    this.spriteLayers.add(this.collectingLayer);
                },
                text -> {
                    this.vanillaTextDraws.add(text);
                    this.textLayers.add(this.collectingLayer);
                });
        this.widgets.forEach(w -> {
            try {
                w.updateDrag(dragRenderContext);
            } catch (Throwable t) {
                Raven.LOGGER.error("Widget drag error for {}: {}", w.getName(), t.getMessage());
            }
        });
        for (int layer = 0; layer < this.widgets.size(); layer++) {
            Draggable w = this.widgets.get(layer);
            this.collectingLayer = layer;
            try {
                w.layout(dragRenderContext);
                w.collectVanillaItems(dragRenderContext);
            } catch (Throwable t) {
                Raven.LOGGER.error("Widget layout/item collection error for {}: {}", w.getName(), t.getMessage());
            }
            this.occluders.add(w.isVisible() && w.isRendering()
                    ? new float[]{w.getX() * fMax, w.getY() * fMax,
                    (w.getX() + w.scaledWidth()) * fMax, (w.getY() + w.scaledHeight()) * fMax}
                    : null);
        }
        this.customHudPrepared = true;
    }

    private void clearVanillaDraws() {
        this.vanillaItemDraws.clear();
        this.vanillaSpriteDraws.clear();
        this.vanillaTextDraws.clear();
        this.itemLayers.clear();
        this.spriteLayers.clear();
        this.textLayers.clear();
        this.occluders.clear();
    }

    private List<float[]> visiblePieces(int layer, float x, float y, float width, float height) {
        List<float[]> pieces = new ArrayList<>();
        pieces.add(new float[]{x, y, x + width, y + height});
        for (int i = layer + 1; i < this.occluders.size() && !pieces.isEmpty(); i++) {
            float[] cover = this.occluders.get(i);
            if (cover == null) continue;
            List<float[]> next = new ArrayList<>();
            for (float[] piece : pieces) {
                if (cover[0] >= piece[2] || cover[2] <= piece[0] || cover[1] >= piece[3] || cover[3] <= piece[1]) {
                    next.add(piece);
                    continue;
                }
                float top = Math.max(piece[1], cover[1]);
                float bottom = Math.min(piece[3], cover[3]);
                if (cover[1] > piece[1]) next.add(new float[]{piece[0], piece[1], piece[2], cover[1]});
                if (cover[3] < piece[3]) next.add(new float[]{piece[0], cover[3], piece[2], piece[3]});
                if (cover[0] > piece[0]) next.add(new float[]{piece[0], top, cover[0], bottom});
                if (cover[2] < piece[2]) next.add(new float[]{cover[2], top, piece[2], bottom});
            }
            pieces = next;
        }
        return pieces;
    }

    private void drawClipped(GuiGraphicsExtractor drawContext, Matrix3x2fStack matrices, int layer,
                             float x, float y, float width, float height, float toGuiX, float toGuiY,
                             Runnable draw) {
        List<float[]> pieces = visiblePieces(layer, x, y, width, height);
        if (pieces.isEmpty()) return;
        boolean whole = pieces.size() == 1 && pieces.getFirst()[0] == x && pieces.getFirst()[1] == y
                && pieces.getFirst()[2] == x + width && pieces.getFirst()[3] == y + height;
        for (float[] piece : pieces) {
            int x0 = (int) Math.ceil(piece[0] * toGuiX);
            int y0 = (int) Math.ceil(piece[1] * toGuiY);
            int x1 = (int) Math.floor(piece[2] * toGuiX);
            int y1 = (int) Math.floor(piece[3] * toGuiY);
            if (!whole && (x1 <= x0 || y1 <= y0)) continue;
            matrices.pushMatrix();
            try {
                matrices.identity();
                if (!whole) drawContext.enableScissor(x0, y0, x1, y1);
                draw.run();
                if (!whole) drawContext.disableScissor();
            } finally {
                matrices.popMatrix();
            }
        }
    }

    public void renderPreparedCustomHud() {
        if (!this.customHudPrepared) {
            return;
        }

        this.customHudPrepared = false;
        if (!isHudScreenAllowed() || this.resolution == null) {
            return;
        }

        Mc mc = Mc.INSTANCE;
        float dpi = Math.max(Raven.INSTANCE.windowController().dpiScaleFactor(), 0.01f);
        PoseStack matrixStack = new PoseStack();
        matrixStack.scale(dpi, dpi, 1.0f);
        DrawEngine drawEngine = Raven.INSTANCE.drawEngine();
        DragRenderContext context = new DragRenderContext(this.resolution, mc.getMouse(), matrixStack,
                drawEngine, Raven.INSTANCE.theme(), dpi, null, null, null);
        boolean anyDragging = isAnyDragging();

        drawEngine.begin();
        try {
            List<float[]> drawn = new ArrayList<>();
            for (Draggable widget : this.widgets) {
                try {
                    widget.layout(context);
                    float[] bounds = widget.isVisible() && widget.isRendering()
                            ? new float[]{widget.getX(), widget.getY(),
                            widget.getX() + widget.scaledWidth(), widget.getY() + widget.scaledHeight()}
                            : null;
                    if (bounds != null && this.blurInitialized && overlapsAny(drawn, bounds)) {
                        drawEngine.end();
                        this.hudBlur.apply();
                        drawEngine.begin();
                    }
                    widget.draw(context);
                    if (bounds != null) drawn.add(bounds);
                } catch (Throwable t) {
                    Raven.LOGGER.error("Widget final layout/draw error for {}: {}", widget.getName(), t.getMessage());
                }
            }
            this.widgets.forEach(widget -> {
                try {
                    if (widget.hasTooltip()) {
                        widget.drawTooltip(context);
                    }
                } catch (Throwable t) {
                    Raven.LOGGER.error("Widget final tooltip error for {}: {}", widget.getName(), t.getMessage());
                }
            });
            if (anyDragging) {
                this.snapManager.render(context);
            }
        } finally {
            drawEngine.end();
        }
    }

    private static boolean overlapsAny(List<float[]> drawn, float[] bounds) {
        for (float[] other : drawn) {
            if (bounds[0] < other[2] && bounds[2] > other[0] && bounds[1] < other[3] && bounds[3] > other[1]) {
                return true;
            }
        }
        return false;
    }

    public boolean flushVanillaItems() {
        GuiGraphicsExtractor drawContext = this.vanillaDrawContext;
        this.vanillaDrawContext = null;
        if (drawContext == null || this.vanillaItemDraws.isEmpty()
                && this.vanillaSpriteDraws.isEmpty() && this.vanillaTextDraws.isEmpty()) {
            clearVanillaDraws();
            return false;
        }
        float toGuiX = drawContext.guiWidth()
                / (float) Math.max(1, Mc.INSTANCE.getWindow().getWidth());
        float toGuiY = drawContext.guiHeight()
                / (float) Math.max(1, Mc.INSTANCE.getWindow().getHeight());
        drawContext.nextStratum();
        Matrix3x2fStack matrices = drawContext.pose();
        for (int i = 0; i < this.vanillaItemDraws.size(); i++) {
            DragRenderContext.VanillaItemDraw item = this.vanillaItemDraws.get(i);
            drawClipped(drawContext, matrices, this.itemLayers.get(i), item.x(), item.y(),
                    item.width(), item.height(), toGuiX, toGuiY, () -> {
                        matrices.translate(item.x() * toGuiX, item.y() * toGuiY)
                                .scale(item.width() * toGuiX / 16.0f, item.height() * toGuiY / 16.0f);
                        VanillaItemAlphaContext.draw(item.alpha(), () -> {
                            drawContext.item(item.stack(), 0, 0);
                            if (item.overlay()) {
                                drawContext.itemDecorations(Mc.INSTANCE.getTextRenderer(), item.stack(), 0, 0);
                            }
                        });
                    });
        }
        for (int i = 0; i < this.vanillaSpriteDraws.size(); i++) {
            DragRenderContext.VanillaSpriteDraw sprite = this.vanillaSpriteDraws.get(i);
            drawClipped(drawContext, matrices, this.spriteLayers.get(i), sprite.x(), sprite.y(),
                    sprite.width(), sprite.height(), toGuiX, toGuiY, () -> {
                        matrices.translate(sprite.x() * toGuiX, sprite.y() * toGuiY)
                                .scale(sprite.width() * toGuiX / 16.0f, sprite.height() * toGuiY / 16.0f);
                        drawContext.blitSprite(RenderPipelines.GUI_TEXTURED, sprite.sprite(), 0, 0, 16, 16,
                                ARGB.white(sprite.alpha()));
                    });
        }
        for (int i = 0; i < this.vanillaTextDraws.size(); i++) {
            DragRenderContext.VanillaTextDraw text = this.vanillaTextDraws.get(i);
            float width = Mc.INSTANCE.getTextRenderer().width(text.component()) * text.scaleX();
            float height = Mc.INSTANCE.getTextRenderer().lineHeight * text.scaleY();
            drawClipped(drawContext, matrices, this.textLayers.get(i), text.x(), text.y(),
                    width, height, toGuiX, toGuiY, () -> {
                        matrices.translate(text.x() * toGuiX, text.y() * toGuiY)
                                .scale(text.scaleX() * toGuiX, text.scaleY() * toGuiY);
                        drawContext.text(Mc.INSTANCE.getTextRenderer(), text.component(),
                                0, 0, text.color(), false);
                    });
        }
        clearVanillaDraws();
        return true;
    }

    private boolean requiresHudBlur() {
        WidgetSettingsPopup settingsPopup = (WidgetSettingsPopup) Raven.INSTANCE.windowController()
                .getWindow(WidgetSettingsPopup.class);
        return settingsPopup != null && settingsPopup.isOpen()
                || this.widgets.stream().anyMatch(widget -> widget.isVisible() && (
                widget instanceof HotkeysWidget hotkeys && hotkeys.isRendering()
                        || widget instanceof CoordsWidget
                        || widget instanceof EquipmentHudWidget equipment && equipment.isRendering()
                        || widget instanceof InventoryHudWidget inventory && inventory.isRendering()
                        || widget instanceof PotionListWidget potions && potions.isRendering()
                        || widget instanceof CooldownsWidget cooldowns && cooldowns.isRendering()
                        || widget instanceof NotificationWidget notifications && notifications.isRendering()
                        || widget instanceof TargetHudWidget targetHud && targetHud.isRendering()
                        || widget instanceof MediaPlayerWidget mediaPlayer && mediaPlayer.isRendering()
                        || widget instanceof GifWidget gif && gif.isRendering() && gif.usesBackground()
                        || widget instanceof WatermarkWidget));
    }

    public void captureHudBlur() {
        if (!isHudScreenAllowed() || !requiresHudBlur()) {
            return;
        }
        init();
        this.hudBlur.apply();
    }

    public boolean isHudScreenAllowed() {
        return Mc.INSTANCE.getCurrentScreen() == null || Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen;
    }

    public int hudBlurTexture() {
        return this.blurInitialized
                && FramebufferUtil.hasValidColorAttachment(this.hudBlur.getBlurFramebuffer())
                ? FramebufferUtil.getColorAttachmentId(this.hudBlur.getBlurFramebuffer())
                : 0;
    }

    public void setHudScale(float hudScale) {
        this.hudScale = Math.max(0.5f, Math.min(hudScale, 2.0f));
        this.widgets.forEach(widget -> widget.setHudScale(this.hudScale));
    }

    public void setWidgetScale(Draggable widget, float scale) {
        if (widget == null) {
            return;
        }
        float clamped = Math.max(0.5f, Math.min(scale, 2.0f));
        this.widgetScales.put(widget, clamped);
        widget.setHudScale(clamped);
    }

    public void click(MouseButtonEvent2 mouseButtonEvent2) {
        Mc mc = Mc.INSTANCE;
        if (mc.getCurrentScreen() instanceof ChatScreen) {
            MouseHandler mouse = mc.getMouse();
            boolean z = false;
            for (int size = this.widgets.size() - 1; size >= 0; size--) {
                Draggable draggable = this.widgets.get(size);
                if (draggable.onClick(new MouseButtonInput2(z, mouse, mouseButtonEvent2.action(), this.resolution, mouseButtonEvent2.button(), Math.max(Raven.INSTANCE.windowController().dpiScaleFactor(), 0.01f)))) {
                    if (!z) {
                        markActive(draggable);
                    }
                    z = true;
                }
            }
            if (mouseButtonEvent2.action() == ButtonAction.RELEASE) {
                this.snapManager.clear();
            }
        }
    }

    public boolean isAnyDragging() {
        for (Draggable draggable : this.widgets) {
            if (draggable.isDragging() && draggable.isVisible()) {
                return true;
            }
        }
        return false;
    }

    public void handleResize(int width, int height) {
        this.widgets.forEach(draggable -> draggable.handleResize(width, height));
    }

    public void update() {
        this.widgets.forEach(Draggable::updateWidget);
    }

    public void markActive(Draggable draggable) {
        if (draggable == this.activeWidget || draggable == null) {
            return;
        }
        this.activeWidget = draggable;
        if (this.widgets.size() > 1) {
            bringToFront(draggable);
        }
    }

    public void newWidget(Draggable draggable) {
        if (this.widgets.contains(draggable)) {
            return;
        }
        this.widgets.add(draggable);
        draggable.setSnapGrid(this.snapManager);
        draggable.setSiblingWidgets(this.widgets);
    }

    public void bringToFront(Draggable draggable) {
        ArrayList<Draggable> list = new ArrayList<>(this.widgets);
        int size = this.widgets.size() - 1;
        int iIndexOf = this.widgets.indexOf(draggable);
        if (iIndexOf == size) {
            return;
        }
        int i = 0;
        for (int i2 = size; i2 >= 0; i2--) {
            if (i2 == iIndexOf) {
                i = 1;
            } else {
                this.widgets.set((i2 - 1) + i, list.get(i2));
            }
        }
        this.widgets.set(size, draggable);
    }

    public List<Draggable> widgets() {
        return this.widgets;
    }
}
