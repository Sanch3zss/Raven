package sanch3zss.raven.mixin.client.render;

import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import sanch3zss.raven.client.*;
import sanch3zss.raven.event.render.*;
import sanch3zss.raven.feature.module.player.FreeLookModule;
import sanch3zss.raven.feature.module.player.FreeLookCameraState;

@Mixin({Camera.class})
public abstract class CameraMixin {

    @ModifyArgs(method = "setupPerspective", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Projection;setupPerspective(FFFFF)V"))
    private void raven$modifyAspectRatio(Args args) {
        if (!Mc.INSTANCE.isWorldLoaded()) {
            return;
        }
        float width = args.get(3);
        float height = args.get(4);
        AspectRatioEvent event = new AspectRatioEvent(width / height);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.getAspectRatio() > 0.0f) {
            args.set(3, height * event.getAspectRatio());
        }
    }

    @Shadow
    private Entity entity;

    @Unique
    private boolean raven$freeLookFirstFrame = true;

    @Shadow
    public void setRotation(float f, float f2) {
    }

    @Inject(method = "alignWithEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1, shift = At.Shift.AFTER))
    private void raven$applyCameraRotation(CallbackInfo callbackInfo) {
        FreeLookModule freeLook = Raven.INSTANCE.moduleRepository().get(FreeLookModule.class);
        if (freeLook.isFreeLooking() && this.entity instanceof LocalPlayer player
                && player instanceof FreeLookCameraState state) {
            if (this.raven$freeLookFirstFrame) {
                state.raven$setCameraPitch(player.getXRot());
                state.raven$setCameraYaw(player.getYRot());
                this.raven$freeLookFirstFrame = false;
            }
            setRotation(state.raven$getCameraYaw(), state.raven$getCameraPitch());
            return;
        }
        if (!freeLook.isFreeLooking() && this.entity instanceof LocalPlayer) {
            this.raven$freeLookFirstFrame = true;
        }
    }
}
