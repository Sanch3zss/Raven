package sanch3zss.raven.mixin.client.network;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.network.*;

@Mixin({Connection.class})
public class ClientConnectionMixin {
    @Inject(method = {"genericsFtw"}, at = {@At("HEAD")}, cancellable = true)
    private static <T extends PacketListener> void handlePacket(Packet<T> packet, PacketListener packetListener, CallbackInfo callbackInfo) {
        PacketReceiveEvent event = new PacketReceiveEvent(packet);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = {"send(Lnet/minecraft/network/protocol/Packet;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void send(Packet<?> packet, CallbackInfo callbackInfo) {
        PacketSendEvent event2 = new PacketSendEvent(packet);
        Raven.INSTANCE.eventDispatcher().dispatch(event2);
        if (event2.isCancelled()) {
            callbackInfo.cancel();
        }
    }
}
