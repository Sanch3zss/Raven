package sanch3zss.raven.mixin.client.sound;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.client.SoundPlayEvent;

@Mixin({SoundEngine.class})
public class SoundManagerMixin {
    private float raven$volumeMultiplier = 1.0f;

    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void raven$onPlayNoDelay(SoundInstance soundInstance, CallbackInfoReturnable<SoundEngine.PlayResult> callbackInfo) {
        if (soundInstance == null) {
            return;
        }
        this.raven$volumeMultiplier = 1.0f;
        SoundPlayEvent event = new SoundPlayEvent(soundInstance);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            callbackInfo.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        } else {
            this.raven$volumeMultiplier = event.getVolumeMultiplier();
        }
    }

    @ModifyExpressionValue(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"))
    private float raven$applyRemovalVolume(float original) {
        return original * this.raven$volumeMultiplier;
    }
}
