package sanch3zss.raven.mixin.client.screen;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.platform.FramerateLimitTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({FramerateLimitTracker.class})
public class InactivityFpsLimiterMixin {

    @Shadow
    private int framerateLimit;

    @ModifyReturnValue(method = {"getFramerateLimit"}, at = {@At("RETURN")})
    public int update(int i) {
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient.level != null || (minecraftClient.gui.screen() == null && minecraftClient.gui.overlay() == null)) {
            return this.framerateLimit;
        }
        return 200;
    }
}
