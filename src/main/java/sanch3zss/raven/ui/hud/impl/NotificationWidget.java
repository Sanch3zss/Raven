package sanch3zss.raven.ui.hud.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.ModuleStateEvent;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import sanch3zss.raven.render.animation.AnimatedFloat;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.hud.Draggable;
import sanch3zss.raven.ui.notification.*;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.interpolation.*;

public class NotificationWidget extends Draggable {
    public float width;
    public float height;
    public static final float rowHeight = 20.0f;
    public static final int horizontalPadding = 8;
    public static final int iconSize = 13;
    public static final int verticalPadding = 8;
    public Notification previewNotification;
    public final AnimatedFloat previewAnimation;
    public static final Translation placeholderText = Lang.NOTIFICATIONS_PLACEHOLDER;
    public int previewTypeIndex;
    public final Stopwatch previewCycleStopwatch;
    public final List<ModuleStateEvent> pendingModuleEvents;
    public final Stopwatch moduleEventStopwatch;
    public final MsdfFont font;
    private final MsdfFont iconFont;

    public final ModeSetting<NotificationDirection> direction;
    public final BooleanSetting itemPickUp;

    public NotificationWidget(BooleanSupplier booleanSupplier) {
        super("Notifications", booleanSupplier);
        this.previewAnimation = new AnimatedFloat(200, Easings.LINEAR);
        this.previewTypeIndex = 0;
        this.previewCycleStopwatch = new Stopwatch();
        this.pendingModuleEvents = new ArrayList<>();
        this.moduleEventStopwatch = new Stopwatch();
        this.font = Fonts.INTER_BOLD.get();
        this.iconFont = Fonts.ETC.get();
        this.direction = new ModeSetting<NotificationDirection>(Lang.WIDGET_NOTIFICATIONS_DIRECTION, Lang.WIDGET_NOTIFICATIONS_DIRECTION_DESC).values(NotificationDirection.class);
        this.itemPickUp = new BooleanSetting(Lang.WIDGET_NOTIFICATIONS_ITEM_PICK_UP, Lang.WIDGET_NOTIFICATIONS_ITEM_PICK_UP_DESC);
        setExcludeFromSnapGrid(true);
        addSettings(this.direction, this.itemPickUp);
        Raven.INSTANCE.eventDispatcher().register(ModuleStateEvent.class, event -> {
            if (isVisible() && Mc.INSTANCE.isWorldLoaded()) {
                synchronized (this.pendingModuleEvents) {
                    this.pendingModuleEvents.add(event);
                    this.moduleEventStopwatch.reset();
                }
            }
        });
        this.y = 50.0f;
    }

    @Override
    public void layout(DragRenderContext dragRenderContext) {
        float maxWidth = 0.0f;
        if (this.previewNotification != null && this.previewNotification.valueAnimation().animatedValue() > 0.0f) {
            maxWidth = 40.0f + this.font.getWidthWithStyles(this.previewNotification.message(), 12.0f);
        } else {
            for (Notification notification : Raven.INSTANCE.notificationRepository().getNotifications()) {
                if (notification.valueAnimation().animatedValue() > 0.0f) {
                    maxWidth = Math.max(maxWidth,
                            38.0f + this.font.getWidthWithStyles(notification.message(), 12.0f));
                }
            }
        }
        this.width = maxWidth;
        this.height = rowHeight;
        this.x = (dragRenderContext.resolution().screenWidth() - maxWidth * hudScale()) / 2.0f;
    }

    @Override
    public void render(DragRenderContext dragRenderContext) {
        if (!isVisible()) {
            return;
        }
        float fScreenHeight = dragRenderContext.resolution().screenHeight() / 2.0f;
        boolean stackUpwards = switch (this.direction.currentValue()) {
            case AUTO -> this.y + (this.height / 2.0f) > fScreenHeight;
            case DOWNWARD -> false;
            case UPWARD -> true;
        };
        float f = 0.0f;
        List<Notification> notifications = Raven.INSTANCE.notificationRepository().getNotifications();
        if (this.previewNotification != null) {
            float fAnimatedValue = this.previewNotification.valueAnimation().animatedValue();
            if (fAnimatedValue > 0.0f) {
                float widthWithStyles = 36.0f + this.font.getWidthWithStyles(this.previewNotification.message(), 12.0f) + 4.0f;
                float rowX = this.x + (this.width - widthWithStyles) / 2.0f;
                renderNotification(dragRenderContext, this.previewNotification, rowX, this.y,
                        widthWithStyles, rowHeight, fAnimatedValue);
                return;
            }
        }
        for (int i = 0; i < notifications.size(); i++) {
            Notification notification = stackUpwards ? notifications.get((notifications.size() - 1) - i) : notifications.get(i);
            float fAnimatedValue2 = notification.valueAnimation().animatedValue();
            if (fAnimatedValue2 > 0.0f) {
                float widthWithStyles2 = 28.0f + this.font.getWidthWithStyles(notification.message(), 12.0f) + 8.0f + 2.0f;
                float hiddenProgress = 1.0f - fAnimatedValue2;
                float horizontalSlide = hiddenProgress * hiddenProgress * 18.0f;
                float rowX = this.x + (this.width - widthWithStyles2) / 2.0f;
                renderNotification(dragRenderContext, notification, rowX + horizontalSlide,
                        stackUpwards ? this.y - f : this.y + f, widthWithStyles2, rowHeight, fAnimatedValue2);
                f += 30.0f * fAnimatedValue2;
            }
        }
    }

    public void renderNotification(DragRenderContext context, Notification notification,
                                   float f, float f2, float f3, float f4, float f5) {
        DrawEngine drawEngine2 = context.drawEngine();
        ThemePalette palette = context.theme().palette();
        PoseStack matrixStack = context.matrixStack();
        ColorStack colorStack2 = drawEngine2.colorStack();
        colorStack2.push();
        colorStack2.alpha(f5);
        drawGlassPanel(context, f, f2, f3, f4);
        colorStack2.pop();
        float f6 = f + 8.0f;
        if (notification.itemStack() == null || notification.itemStack().isEmpty()) {
            if (isModuleStateNotification(notification.type())) {
                String glyph = notification.type() == NotificationType.MODULE_ENABLED ? "a" : "d";
                drawCenteredStateIcon(context, glyph, f6, f2, iconSize, f4,
                        colorStack2.computeColor(0xFFFFFFFF, f5));
            } else {
                drawEngine2.texture(matrixStack.last().pose(), f6,
                        (f2 + (f4 / 2.0f)) - 6.0f, 12.0f, 12.0f,
                        drawEngine2.bindTexture(notification.type().getTexture().textureWithSTB()),
                        colorStack2.computeColor(0xFFFFFFFF, f5));
            }
        } else {
            drawEngine2.itemStack(matrixStack.last().pose(), notification.itemStack(), f6,
                    (f2 + (f4 / 2.0f)) - 6.0f, 12.0f, 12.0f);
        }
        float f7 = f6 + 12.0f + 6.0f;
        drawEngine2.msdfFont(matrixStack.last().pose(), this.font,
                notification.message().getString(), f7,
                (f2 + (f4 / 2.0f)) - (this.font.getHeight(12.0f) / 2.0f),
                12.0f, 0.0f,
                colorStack2.computeColor(palette.text().tone(100).argb(), f5));
    }

    private void drawCenteredStateIcon(DragRenderContext context, String glyph,
                                       float slotX, float rowY, float slotSize, float rowH,
                                       int color) {
        int codePoint = glyph.codePointAt(0);
        float iconX = this.iconFont.centeredGlyphX(codePoint, 13.125f, slotX, slotSize);
        float iconY = this.iconFont.centeredGlyphY(codePoint, 13.125f, rowY, rowH);
        context.drawEngine().msdfFont(context.matrixStack().last().pose(),
                this.iconFont, glyph, iconX, iconY, 13.125f, 0.0f, color);
    }

    private boolean isModuleStateNotification(NotificationType type) {
        return type == NotificationType.MODULE_ENABLED || type == NotificationType.MODULE_DISABLED;
    }

    private void drawGlassPanel(DragRenderContext context, float x, float y, float w, float h) {
        DrawEngine engine = context.drawEngine();
        ColorStack colors = engine.colorStack();
        PoseStack matrices = context.matrixStack();
        int blurTexture = Raven.INSTANCE.widgetStack().hudBlurTexture();
        int tint = mixRgb(sanch3zss.raven.ui.menu.MenuStyle.surfaceColor(context.theme(), 0xFF111217),
                context.theme().palette().accentStart().argb(), 0.075f);
        HudGlassPanel.drawShadows(engine, matrices.last().pose(), x, y, w, h,
                6.0f, 25.5f,
                colors.computeColor(tint, 1.5f));
        if (blurTexture != 0) {
            engine.roundedGlassPanel(matrices.last().pose(), x, y, w, h,
                    6.0f, 0.0f, 0.8f,
                    colors.computeColor(tint, 1.0f),
                    0, blurTexture);
        } else {
            engine.roundedRectangle(matrices.last().pose(), x, y, w, h,
                    6.0f, colors.computeColor(tint, 0.96f));
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
    public void animate(WeightedEngine weightedEngine) {
        if (isVisible()) {
            Raven.INSTANCE.notificationRepository().getNotifications().forEach(notification -> {
                notification.valueAnimation().animate(weightedEngine);
            });
            this.previewAnimation.animate(weightedEngine);
        }
    }

    @Override
    public void update() {
        if (isVisible()) {
            synchronized (this.pendingModuleEvents) {
                if (!this.pendingModuleEvents.isEmpty()) {
                    List<ModuleStateEvent> list = new ArrayList<>(this.pendingModuleEvents);
                    this.pendingModuleEvents.clear();
                    TreeSet<String> enabledSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                    TreeSet<String> disabledSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
                    for (ModuleStateEvent event : list) {
                        String name = event.module().getName();
                        if (event.moduleState()) {
                            enabledSet.add(name);
                        } else {
                            disabledSet.add(name);
                        }
                    }
                    if (!enabledSet.isEmpty()) {
                        List<String> enabledList = enabledSet.stream().limit(4).toList();
                        Raven.INSTANCE.notificationRepository().post(NotificationType.MODULE_ENABLED, Component.literal(enabledList.size() == 1 ? Lang.NOTIFICATIONS_ENABLED_SINGLE.effective().replace("{module}", enabledList.getFirst()) : Lang.NOTIFICATIONS_ENABLED_MULTIPLE.effective().replace("{modules}", String.join(", ", enabledList)).replace("{extra}", enabledSet.size() > 4 ? Lang.NOTIFICATIONS_MORE.effective().replace("{count}", String.valueOf(enabledSet.size() - 4)) : "")), 3L, TimeUnit.SECONDS);
                    }
                    if (!disabledSet.isEmpty()) {
                        List<String> disabledList = disabledSet.stream().limit(4).toList();
                        Raven.INSTANCE.notificationRepository().post(NotificationType.MODULE_DISABLED, Component.literal(disabledList.size() == 1 ? Lang.NOTIFICATIONS_DISABLED_SINGLE.effective().replace("{module}", disabledList.getFirst()) : Lang.NOTIFICATIONS_DISABLED_MULTIPLE.effective().replace("{modules}", String.join(", ", disabledList)).replace("{extra}", disabledSet.size() > 4 ? Lang.NOTIFICATIONS_MORE.effective().replace("{count}", String.valueOf(disabledSet.size() - 4)) : "")), 3L, TimeUnit.SECONDS);
                    }
                }
            }
            Raven.INSTANCE.notificationRepository().tick();
            boolean isChat = Mc.INSTANCE.getCurrentScreen() instanceof ChatScreen;
            boolean hasNotifications = !Raven.INSTANCE.notificationRepository().getNotifications().isEmpty();
            if (!isChat || hasNotifications) {
                if (this.previewNotification != null) {
                    this.previewAnimation.destination(0.0f);
                    if (this.previewAnimation.animatedValue() <= 0.01f) {
                        this.previewNotification = null;
                    }
                }
                return;
            }
            long now = System.currentTimeMillis();
            if (this.previewNotification == null) {
                this.previewAnimation.destination(1.0f);
                this.previewNotification = new Notification(this.previewAnimation, NotificationType.INFO, Component.literal(placeholderText.effective()), now, Long.MAX_VALUE, null);
                this.previewCycleStopwatch.reset();
                return;
            }
            this.previewAnimation.destination(1.0f);
            if (this.previewCycleStopwatch.hasElapsed(700L, TimeUnit.MILLISECONDS)) {
                NotificationType[] arrValues = NotificationType.values();
                this.previewTypeIndex = (this.previewTypeIndex + 1) % arrValues.length;
                this.previewNotification = new Notification(this.previewAnimation, arrValues[this.previewTypeIndex], Component.literal(placeholderText.effective()), now, Long.MAX_VALUE, null);
                this.previewCycleStopwatch.reset();
            }
        }
    }

    public int resolve(NotificationType notificationType, ThemePalette palette, ColorStack colorStack2, float f) {
        return switch (notificationType) {
            case INFO -> colorStack2.computeColor(palette.accent().argb(), f);
            case SUCCESS, MODULE_ENABLED -> colorStack2.computeColor(ThemePalette.deepGreen.argb(), f);
            case WARNING -> colorStack2.computeColor(ThemePalette.darkGolden.argb(), f);
            case ERROR, MODULE_DISABLED -> colorStack2.computeColor(ThemePalette.darkRed.argb(), f);
            case EVENT -> colorStack2.computeColor(palette.text().tone(50).argb(), f);
        };
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
    public boolean click(MouseButtonInput2 mouseButtonInput2, boolean z) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput mouseMoveInput, boolean z) {
        return z && !mouseMoveInput.intercepted();
    }

    public BooleanSetting getItemPickUp() {
        return this.itemPickUp;
    }

    public boolean isRendering() {
        if (this.previewNotification != null && this.previewNotification.valueAnimation().animatedValue() > 0.01f) {
            return true;
        }
        return Raven.INSTANCE.notificationRepository().getNotifications().stream()
                .anyMatch(notification -> notification.valueAnimation().animatedValue() > 0.01f);
    }
}
