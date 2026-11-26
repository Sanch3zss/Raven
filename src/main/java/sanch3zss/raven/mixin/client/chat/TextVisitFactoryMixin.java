package sanch3zss.raven.mixin.client.chat;

import net.minecraft.util.StringDecomposer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.network.ChatSendEvent;

@Mixin({StringDecomposer.class})
public class TextVisitFactoryMixin {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/util/StringDecomposer;iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z", ordinal = 0), method = {"iterateFormatted(Ljava/lang/String;ILnet/minecraft/network/chat/Style;Lnet/minecraft/util/FormattedCharSink;)Z"}, index = 0)
    private static String visitFormatted(String str) {
        ChatSendEvent event = new ChatSendEvent(str);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        return (!event.isCancelled() || event.getChangedText().isEmpty()) ? str : event.getChangedText();
    }
}
