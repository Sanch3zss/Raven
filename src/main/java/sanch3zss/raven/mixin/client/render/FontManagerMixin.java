package sanch3zss.raven.mixin.client.render;

import net.minecraft.client.gui.font.FontManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sanch3zss.raven.client.font.CustomFonts;

@Mixin(FontManager.class)
public class FontManagerMixin {
    @Inject(method = "apply", at = @At("TAIL"))
    private void raven$reinstallCustomFont(CallbackInfo callbackInfo) {
        CustomFonts.INSTANCE.reinstall((FontManager) (Object) this);
    }
}
