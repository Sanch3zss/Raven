package sanch3zss.raven.mixin.client.screen;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.*;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.client.WindowResizeEvent;

@Mixin({Window.class})
public class WindowMixin {
    @Inject(method = {"<init>"}, at = {@At("RETURN")})
    public void init(CallbackInfo callbackInfo) {
        Window window = (Window) (Object) this;
        Raven.INSTANCE.eventDispatcher().dispatch(new WindowResizeEvent(window.getWidth(), window.getHeight()));
        Raven.INSTANCE.windowControllerAdapter().handleResize(window.getWidth(), window.getHeight());
    }

    @Inject(method = {"onResize"}, at = {@At("HEAD")})
    public void onResize(long j, int i, int i2, CallbackInfo callbackInfo) {
        Raven.INSTANCE.windowControllerAdapter().handleResize(i, i2);
        Raven.INSTANCE.eventDispatcher().dispatch(new WindowResizeEvent(i, i2));
    }
}
