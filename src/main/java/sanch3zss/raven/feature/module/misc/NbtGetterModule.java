package sanch3zss.raven.feature.module.misc;

import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.KeyAction;
import sanch3zss.raven.client.input.KeyboardUtil;
import sanch3zss.raven.event.client.MouseButtonEvent2;
import sanch3zss.raven.event.player.FocusedSlotEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.ButtonAction;
import sanch3zss.raven.client.chat.ChatUtil;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.ui.setting.KeybindSetting;
import sanch3zss.raven.util.StringUtil;

@Aliases(aliases = {"NbtGetter", "NBT Getter", "Nbt Getter", "Get NBT", "Copy NBT"})
public class NbtGetterModule extends Module {
    public final KeybindSetting keybind;

    public NbtGetterModule() {
        super(ModuleTab.MISC, "NbtGetter");
        this.keybind = new KeybindSetting(
                Translation.clearText("Бинд"),
                Translation.clearText("Клавиша для копирования NBT предмета")
        );
        addSettings(this.keybind);
        setStateSilent(true);

        this.keybind.consumer(action -> {
            if (!isState() || !Mc.INSTANCE.isWorldLoaded() || action != KeyAction.PRESS) {
                return;
            }
            handleInGame();
        });

        register(FocusedSlotEvent.class, event -> {
            if (!isState() || !Mc.INSTANCE.isWorldLoaded() || !matchesBind(event.key())) {
                return;
            }
            Slot slot = event.focusedSlot();
            if (slot == null || !slot.hasItem() || slot.getItem().isEmpty()) {
                notifyNoItem();
                return;
            }
            copyItemNbt(slot.getItem());
        });

        register(MouseButtonEvent2.class, event -> {
            if (!isState() || !Mc.INSTANCE.isWorldLoaded() || event.action() != ButtonAction.PRESS || !matchesMouseBind(event.button())) {
                return;
            }
            if (Mc.INSTANCE.getCurrentScreen() instanceof AbstractContainerScreen<?> containerScreen) {
                Slot slot = containerScreen.hoveredSlot;
                if (slot == null || !slot.hasItem() || slot.getItem().isEmpty()) {
                    notifyNoItem();
                    return;
                }
                copyItemNbt(slot.getItem());
            }
        });
    }

    private void handleInGame() {
        HitResult hit = Mc.INSTANCE.getCrosshairTarget();
        if (hit instanceof EntityHitResult entityHit) {
            if (entityHit.getEntity() instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()) {
                copyItemNbt(itemEntity.getItem());
                return;
            }
            if (entityHit.getEntity() instanceof ItemFrame itemFrame && !itemFrame.getItem().isEmpty()) {
                copyItemNbt(itemFrame.getItem());
                return;
            }
        }
        if (Mc.INSTANCE.getPlayer() != null && !Mc.INSTANCE.getPlayer().getMainHandItem().isEmpty()) {
            copyItemNbt(Mc.INSTANCE.getPlayer().getMainHandItem());
            return;
        }
        notifyNoItem();
    }

    private boolean matchesBind(int key) {
        List<Integer> keys = this.keybind.getKeyBind();
        if (keys.isEmpty() || key == -1) {
            return false;
        }
        if (keys.getLast() != key) {
            return false;
        }
        for (int i = 0; i < keys.size() - 1; i++) {
            int modifier = keys.get(i);
            if (!KeyboardUtil.isKeyPressed(modifier)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchesMouseBind(int button) {
        List<Integer> keys = this.keybind.getKeyBind();
        if (keys.isEmpty() || button == -1) {
            return false;
        }
        if (keys.getLast() != button) {
            return false;
        }
        for (int i = 0; i < keys.size() - 1; i++) {
            int modifier = keys.get(i);
            if (!KeyboardUtil.isKeyPressed(modifier)) {
                return false;
            }
        }
        return true;
    }

    private void copyItemNbt(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            notifyNoItem();
            return;
        }
        String nbtString = extractNbt(stack);
        if (nbtString == null || nbtString.isEmpty()) {
            nbtString = "{}";
        }
        StringUtil.copyToClipboard(nbtString);
        Raven.INSTANCE.notificationRepository().post(
                NotificationType.SUCCESS,
                Component.literal("NBT скопирован в буфер обмена!"),
                2L,
                TimeUnit.SECONDS
        );
        ChatUtil.addChatMessage("NBT предмета " + stack.getHoverName().getString() + " скопирован в буфер обмена!");
    }

    private String extractNbt(ItemStack stack) {
        try {
            if (Mc.INSTANCE.getWorld() != null) {
                RegistryOps<Tag> ops = Mc.INSTANCE.getWorld().registryAccess().createSerializationContext(NbtOps.INSTANCE);
                var result = ItemStack.CODEC.encodeStart(ops, stack);
                if (result.isSuccess()) {
                    return result.getOrThrow().toString();
                }
            }
        } catch (Exception ignored) {
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            return customData.copyTag().toString();
        }
        return "{}";
    }

    private void notifyNoItem() {
        Raven.INSTANCE.notificationRepository().post(
                NotificationType.WARNING,
                Component.literal("Наведитесь на предмет!"),
                2L,
                TimeUnit.SECONDS
        );
    }
}
