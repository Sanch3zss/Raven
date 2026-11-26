package sanch3zss.raven.feature.module.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import sanch3zss.raven.client.*;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.event.player.StatusEffectOverlayEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.hud.*;
import sanch3zss.raven.ui.hud.impl.*;

@Aliases(aliases = {"HUD", "Interface", "Overlay", "Target Hud", "MediaPlayer", "Gif", "Watermark", "Potion List", "Cooldowns", "Coords", "Hotkeys", "Inventory HUD", "Equipment HUD"})
public class WidgetsModule extends Module {
    private final WatermarkWidget watermarkWidget;
    private final PotionListWidget potionListWidget;
    private final TargetHudWidget targetHudWidget;
    private final MediaPlayerWidget mediaPlayerWidget;
    private final GifWidget gifWidget;
    private final HotkeysWidget hotkeysWidget;
    private final CoordsWidget coordsWidget;
    private final InventoryHudWidget inventoryHudWidget;
    private final EquipmentHudWidget equipmentHudWidget;
    private final CooldownsWidget cooldownsWidget;
    private final NotificationWidget notificationWidget;

    public WidgetsModule() {
        super(null, "HUD Renderer");
        setStateSilent(true);

        WidgetStack stack = Raven.INSTANCE.widgetStack();
        this.watermarkWidget = new WatermarkWidget(() -> enabled(HudElementModules.Watermark.class));
        this.potionListWidget = new PotionListWidget(() -> enabled(HudElementModules.PotionList.class));
        this.targetHudWidget = new TargetHudWidget(() -> enabled(HudElementModules.TargetHud.class));
        this.mediaPlayerWidget = new MediaPlayerWidget(() -> enabled(HudElementModules.MediaPlayer.class));
        this.gifWidget = new GifWidget(() -> enabled(HudElementModules.Gif.class));
        this.hotkeysWidget = new HotkeysWidget(() -> enabled(HudElementModules.Hotkeys.class));
        this.coordsWidget = new CoordsWidget(() -> enabled(HudElementModules.Coords.class));
        this.inventoryHudWidget = new InventoryHudWidget(() -> enabled(HudElementModules.InventoryHud.class));
        this.equipmentHudWidget = new EquipmentHudWidget(() -> enabled(HudElementModules.EquipmentHud.class));
        this.cooldownsWidget = new CooldownsWidget(() -> enabled(HudElementModules.Cooldowns.class));
        this.notificationWidget = new NotificationWidget(() -> enabled(HudElementModules.Notifications.class));

        stack.newWidget(this.watermarkWidget);
        stack.newWidget(this.potionListWidget);
        stack.newWidget(this.targetHudWidget);
        stack.newWidget(this.mediaPlayerWidget);
        stack.newWidget(this.gifWidget);
        stack.newWidget(this.hotkeysWidget);
        stack.newWidget(this.coordsWidget);
        stack.newWidget(this.inventoryHudWidget);
        stack.newWidget(this.equipmentHudWidget);
        stack.newWidget(this.cooldownsWidget);
        stack.newWidget(this.notificationWidget);

        register(ClientTickEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                stack.update();
            }
        });
        register(WindowResizeEvent.class, event -> stack.handleResize(event.width(), event.height()));
        register(MouseButtonEvent2.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                stack.click(event);
            }
        });
        register(StatusEffectOverlayEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded() && enabled(HudElementModules.PotionList.class)) {
                event.cancel();
            }
        });
    }

    public void renderHud(GuiGraphicsExtractor drawContext) {
        WidgetStack stack = Raven.INSTANCE.widgetStack();
        if (isState() && Mc.INSTANCE.isWorldLoaded() && stack.isHudScreenAllowed()
                && stack.widgets().stream().anyMatch(Draggable::isVisible)) {
            applyScales(stack);
            stack.draw(drawContext);
        }
    }

    private void applyScales(WidgetStack stack) {
        stack.setWidgetScale(this.watermarkWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.Watermark.class).hudSize.currentValue());
        stack.setWidgetScale(this.potionListWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.PotionList.class).hudSize.currentValue());
        stack.setWidgetScale(this.targetHudWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.TargetHud.class).hudSize.currentValue());
        stack.setWidgetScale(this.mediaPlayerWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.MediaPlayer.class).hudSize.currentValue());
        stack.setWidgetScale(this.gifWidget, 1.0f);
        stack.setWidgetScale(this.hotkeysWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.Hotkeys.class).hudSize.currentValue());
        stack.setWidgetScale(this.coordsWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.Coords.class).hudSize.currentValue());
        stack.setWidgetScale(this.inventoryHudWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.InventoryHud.class).hudSize.currentValue());
        stack.setWidgetScale(this.equipmentHudWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.EquipmentHud.class).hudSize.currentValue());
        stack.setWidgetScale(this.cooldownsWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.Cooldowns.class).hudSize.currentValue());
        stack.setWidgetScale(this.notificationWidget,
                Raven.INSTANCE.moduleRepository().get(HudElementModules.Notifications.class).hudSize.currentValue());
    }

    private boolean enabled(Class<? extends Module> type) {
        return isState() && Raven.INSTANCE.moduleRepository().find(type).map(Module::isState).orElse(false);
    }

    public boolean notificationsEnabled() {
        return enabled(HudElementModules.Notifications.class);
    }

    public NotificationWidget getNotificationWidget() {
        return this.notificationWidget;
    }
}
