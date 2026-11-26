package sanch3zss.raven.mixin.client.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.render.GammaEvent;

@Mixin({LightmapRenderStateExtractor.class})
public class LightMapTextureManagerMixin {
    @ModifyExpressionValue(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 1))
    private Object onBrightness(Object obj) {
        GammaEvent event = new GammaEvent();
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        return event.isCancelled() ? Double.valueOf(event.getGamma()) : obj;
    }
}
