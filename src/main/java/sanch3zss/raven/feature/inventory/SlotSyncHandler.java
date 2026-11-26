package sanch3zss.raven.feature.inventory;

import java.util.Objects;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import sanch3zss.raven.client.*;
import sanch3zss.raven.event.ClientListener;
import sanch3zss.raven.event.network.*;
import sanch3zss.raven.feature.module.render.WidgetsModule;
import sanch3zss.raven.feature.server.ServerUtil;
import sanch3zss.raven.ui.notification.NotificationType;

public class SlotSyncHandler implements ClientListener {
    public static final Mc mc = Mc.INSTANCE;
    public static boolean lastSprinting;
    public static int selectedSlot;

    public SlotSyncHandler() {
        Raven.INSTANCE.eventDispatcher().register(PacketSendEvent.class, this::onPacketSend);
        Raven.INSTANCE.eventDispatcher().register(PacketReceiveEvent.class, this::onPacketReceive);
    }

    public void onPacketSend(PacketSendEvent event) {
        if (mc.isWorldLoaded()) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof ServerboundPlayerCommandPacket clientCommand) {
                lastSprinting = clientCommand.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING;
            } else if (packet instanceof ServerboundSetCarriedItemPacket slotPacket) {
                selectedSlot = slotPacket.getSlot();
            }
        }
    }

    public void onPacketReceive(PacketReceiveEvent event) {
        if (mc.isWorldLoaded()) {
            if (event.getPacket() instanceof ClientboundSetHeldSlotPacket packet) {
                selectedSlot = packet.slot();
            }
            ServerUtil.packet(event);
            handleItemPickup(event);
        }
    }

    public void handleItemPickup(PacketReceiveEvent event) {
        WidgetsModule widgetsModule = (WidgetsModule) Raven.INSTANCE.moduleRepository().get(WidgetsModule.class);
        if (widgetsModule.notificationsEnabled() && widgetsModule.getNotificationWidget().getItemPickUp().isValue()) {
            LocalPlayer player = mc.getPlayer();
            Packet<?> packet = event.getPacket();
            if (packet instanceof ClientboundTakeItemEntityPacket pickupPacket) {
                if (pickupPacket.getPlayerId() == player.getId()) {
                    if (mc.getWorld().getEntity(pickupPacket.getItemId()) instanceof ItemEntity itemEntity) {
                        ItemStack stack = itemEntity.getItem();
                        if (!stack.isEmpty()) {
                            postPickupNotification("Поднят предмет: ", stack.getHoverName(), stack.getCount());
                        }
                    }
                }
            } else if (packet instanceof ClientboundContainerSetSlotPacket slotUpdate) {
                if (slotUpdate.getContainerId() == 0 && slotUpdate.getSlot() < player.containerMenu.slots.size()) {
                    ItemContainerContents containerComponent2 = (ItemContainerContents) slotUpdate.getItem().get(DataComponents.CONTAINER);
                    ItemContainerContents containerComponent3 = (ItemContainerContents) player.containerMenu.getSlot(slotUpdate.getSlot()).getItem().get(DataComponents.CONTAINER);
                    if (containerComponent2 == null || containerComponent3 == null) {
                        return;
                    }
                    containerComponent2.nonEmptyItemCopyStream().filter(outerStack -> {
                        return containerComponent3.nonEmptyItemCopyStream().noneMatch(itemStack2 -> stacksEqual(outerStack, itemStack2));
                    }).forEach(itemStack3 -> {
                        postPickupNotification("Сложен в " + slotUpdate.getItem().getHoverName().getString() + ": ", itemStack3.getHoverName(), 1);
                    });
                }
            }
        }
    }

    public boolean stacksEqual(ItemStack itemStack, ItemStack itemStack2) {
        return Objects.equals(itemStack.getComponents(), itemStack2.getComponents()) && itemStack.toString().equals(itemStack2.toString());
    }

    public void postPickupNotification(String prefix, Component text, int count) {
        MutableComponent textAppend = Component.empty().append(prefix).append(text);
        if (count > 1) {
            textAppend.append(" ×" + count);
        }
        Raven.INSTANCE.notificationRepository().post(NotificationType.INFO, textAppend, 3000L);
    }
}
