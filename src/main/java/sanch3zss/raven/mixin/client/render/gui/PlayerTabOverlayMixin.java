package sanch3zss.raven.mixin.client.render.gui;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.feature.module.render.ClientUsersModule;

@Mixin(PlayerTabOverlay.class)
public abstract class PlayerTabOverlayMixin {
    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void raven$decorateClientUser(PlayerInfo info, CallbackInfoReturnable<Component> callbackInfo) {
        ClientUsersModule module = Raven.INSTANCE.moduleRepository().get(ClientUsersModule.class);
        if (module.shouldMark(info.getProfile().name())) {
            callbackInfo.setReturnValue(module.decorate(callbackInfo.getReturnValue()));
        }
        callbackInfo.setReturnValue(FriendManager.applyAliases(callbackInfo.getReturnValue()));
    }
}
