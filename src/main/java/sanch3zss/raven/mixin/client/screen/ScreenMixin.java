package sanch3zss.raven.mixin.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.player.AutoSensitivityModule;

@Mixin({Screen.class})
public class ScreenMixin {
    @Inject(method = "defaultHandleGameClickEvent", at = @At("HEAD"), cancellable = true)
    private static void raven$handleClientClick(ClickEvent event, Minecraft minecraft, Screen activeScreen, CallbackInfo ci) {
        if (event instanceof ClickEvent.Custom custom && custom.id().getNamespace().equals("raven")) {
            Raven.INSTANCE.moduleRepository().get(AutoSensitivityModule.class).onClick(custom.id());
            ci.cancel();
        }
    }
}
