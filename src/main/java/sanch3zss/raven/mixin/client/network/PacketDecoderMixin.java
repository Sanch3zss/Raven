package sanch3zss.raven.mixin.client.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketDecoder;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.SkipPacketException;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.server.ViaVersionBridge;

import java.util.List;
import java.util.NoSuchElementException;

@Mixin(PacketDecoder.class)
public abstract class PacketDecoderMixin {
    @Shadow @Final private ProtocolInfo<?> protocolInfo;

    @WrapOperation(method = "decode", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/codec/StreamCodec;decode(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object raven$ignoreBrokenLegacyPackets(StreamCodec<?, ?> codec, Object source, Operation<Object> original,
                                                       @Local(argsOnly = true) ChannelHandlerContext context) {
        ByteBuf input = (ByteBuf) source;
        int packetStart = input.readerIndex();

        try {
            return original.call(codec, source);
        } catch (RuntimeException exception) {
            boolean brokenEntityData = raven$isBrokenPacket(exception, "clientbound/minecraft:set_entity_data");
            boolean brokenContainerContent = raven$isBrokenPacket(exception, "clientbound/minecraft:container_set_content");
            boolean brokenEquipment = raven$isBrokenPacket(exception, "clientbound/minecraft:set_equipment");
            if (this.protocolInfo.id() != ConnectionProtocol.PLAY
                    || this.protocolInfo.flow() != PacketFlow.CLIENTBOUND
                    || exception instanceof SkipPacketException
                    || (!brokenEntityData && !brokenContainerContent && !brokenEquipment
                    && !raven$isPacketDecodeFailure(exception))
                    || !raven$isLegacyConnection(context.channel())) {
                throw exception;
            }

            try {
                input.readerIndex(packetStart);
                VarInt.read(input);
                if (brokenEntityData) {
                    int entityId = VarInt.read(input);
                    input.skipBytes(input.readableBytes());
                    Raven.LOGGER.warn("Ignored malformed legacy entity data packet for entity {}", entityId);
                    return new ClientboundSetEntityDataPacket(entityId, List.of());
                }
                if (brokenEquipment) {
                    int entityId = VarInt.read(input);
                    input.skipBytes(input.readableBytes());
                    Raven.LOGGER.warn("Ignored malformed legacy equipment packet for entity {}", entityId);
                    return new ClientboundSetEquipmentPacket(entityId, List.of());
                }
                input.skipBytes(input.readableBytes());
                if (brokenContainerContent) {
                    Raven.LOGGER.warn("Ignored malformed legacy container content packet");
                    return new ClientboundContainerSetContentPacket(-1, 0, List.of(), ItemStack.EMPTY);
                }
                Raven.LOGGER.warn("Ignored malformed legacy clientbound packet: {}", exception.toString());
                return new ClientboundSetEquipmentPacket(-1, List.of());
            } catch (RuntimeException recoveryFailure) {
                exception.addSuppressed(recoveryFailure);
                throw exception;
            }
        }
    }

    private static boolean raven$isPacketDecodeFailure(Throwable throwable) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof DecoderException) {
                return true;
            }
        }
        return false;
    }

    private static boolean raven$isBrokenPacket(Throwable throwable, String packetId) {
        boolean matchingPacket = false;
        boolean missingValue = false;

        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (cause instanceof DecoderException && cause.getMessage() != null
                    && cause.getMessage().contains(packetId)) {
                matchingPacket = true;
            }
            if (cause instanceof NoSuchElementException) {
                missingValue = true;
            }
        }

        return matchingPacket && missingValue;
    }

    private static boolean raven$isLegacyConnection(Channel channel) {
        return ViaVersionBridge.olderThan(channel, "v26_2");
    }
}
