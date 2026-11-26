package sanch3zss.raven.feature.module.player;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.player.ItemScrollerModule;
import sanch3zss.raven.mixin.accessor.AbstractContainerScreenAccessor;

public final class ItemScrollerController {
    private static Screen openScreen;
    private static Slot previousSlot;
    private static boolean dragging;
    private static boolean initialized;

    private ItemScrollerController() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.allowMouseClick(screen).register((_screen, event) -> {
                onMouseClicked(screen, event);
                return true;
            });
            ScreenMouseEvents.allowMouseRelease(screen).register((_screen, event) -> {
                onMouseReleased(screen, event);
                return true;
            });
        });
    }

    private static void updateScreen(Screen screen) {
        if (screen != openScreen) {
            openScreen = screen;
            previousSlot = null;
            dragging = false;
        }
    }

    private static boolean enabled() {
        return Raven.INSTANCE != null
                && Raven.INSTANCE.moduleRepository().get(ItemScrollerModule.class).isState();
    }

    private static void onMouseClicked(Screen screen, MouseButtonEvent event) {
        updateScreen(screen);
        if (!enabled() || event.button() != 0 || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }
        previousSlot = accessor(containerScreen).raven$invokeGetHoveredSlot(event.x(), event.y());
        Minecraft minecraft = Minecraft.getInstance();
        dragging = minecraft.player != null && minecraft.player.containerMenu.getCarried().isEmpty();
    }

    private static void onMouseReleased(Screen screen, MouseButtonEvent event) {
        updateScreen(screen);
        if (event.button() == 0) {
            dragging = false;
            previousSlot = null;
        }
    }

    public static boolean onMouseDragged(Screen screen, MouseButtonEvent event) {
        updateScreen(screen);
        if (!enabled() || !dragging || event.button() != 0
                || !(screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }

        AbstractContainerScreenAccessor accessor = accessor(containerScreen);
        Slot slot = accessor.raven$invokeGetHoveredSlot(event.x(), event.y());
        if (slot == previousSlot) {
            return false;
        }
        previousSlot = slot;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.player.containerMenu.getCarried().isEmpty()
                || !isShiftDown(minecraft) || slot == null || !slot.hasItem() || !slot.isActive()) {
            return false;
        }
        if (containerScreen instanceof CreativeModeInventoryScreen
                && slot.container != minecraft.player.getInventory()) {
            return false;
        }

        accessor.raven$invokeSlotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);

        return false;
    }

    private static boolean isShiftDown(Minecraft minecraft) {
        return InputConstants.isKeyDown(minecraft.getWindow(), 340)
                || InputConstants.isKeyDown(minecraft.getWindow(), 344);
    }

    private static AbstractContainerScreenAccessor accessor(AbstractContainerScreen<?> screen) {
        return (AbstractContainerScreenAccessor) screen;
    }

    public static void reset() {
        openScreen = null;
        previousSlot = null;
        dragging = false;
    }
}
