package sanch3zss.raven.event.network;

import net.minecraft.network.protocol.Packet;
import sanch3zss.raven.event.CancellableEvent;

public class PacketSendEvent extends CancellableEvent {
    public final Packet<?> packet;

    public Packet<?> getPacket() {
        return this.packet;
    }

    public PacketSendEvent(Packet<?> packet) {
        this.packet = packet;
    }
}
