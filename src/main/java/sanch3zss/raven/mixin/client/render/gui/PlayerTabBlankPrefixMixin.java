package sanch3zss.raven.mixin.client.render.gui;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import sanch3zss.raven.client.chat.BlankNamePrefix;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabBlankPrefixMixin {
    @ModifyArg(method = "getNameForDisplay", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/PlayerTabOverlay;decorateName(Lnet/minecraft/client/multiplayer/PlayerInfo;Lnet/minecraft/network/chat/MutableComponent;)Lnet/minecraft/network/chat/Component;"),
            index = 1)
    private MutableComponent raven$stripBlankPrefix(MutableComponent name) {
        return BlankNamePrefix.strip(name);
    }
}
