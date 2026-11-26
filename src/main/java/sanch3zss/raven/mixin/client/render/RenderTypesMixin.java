package sanch3zss.raven.mixin.client.render;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sanch3zss.raven.feature.module.render.enchantcolor.CustomEnchantColorRenderer;

@Mixin(RenderTypes.class)
public abstract class RenderTypesMixin {
    @Inject(method = "glint", at = @At("RETURN"), cancellable = true)
    private static void raven$customItemGlint(CallbackInfoReturnable<RenderType> callbackInfo) {
        callbackInfo.setReturnValue(CustomEnchantColorRenderer.INSTANCE.itemGlint(callbackInfo.getReturnValue()));
    }

    @Inject(method = "glintTranslucent", at = @At("RETURN"), cancellable = true)
    private static void raven$customTranslucentGlint(CallbackInfoReturnable<RenderType> callbackInfo) {
        callbackInfo.setReturnValue(CustomEnchantColorRenderer.INSTANCE.translucentGlint(callbackInfo.getReturnValue()));
    }

    @Inject(method = "entityGlint", at = @At("RETURN"), cancellable = true)
    private static void raven$customEntityGlint(CallbackInfoReturnable<RenderType> callbackInfo) {
        callbackInfo.setReturnValue(CustomEnchantColorRenderer.INSTANCE.entityGlint(callbackInfo.getReturnValue()));
    }

    @Inject(method = "armorEntityGlint", at = @At("RETURN"), cancellable = true)
    private static void raven$customArmorGlint(CallbackInfoReturnable<RenderType> callbackInfo) {
        callbackInfo.setReturnValue(CustomEnchantColorRenderer.INSTANCE.armorGlint(callbackInfo.getReturnValue()));
    }
}
