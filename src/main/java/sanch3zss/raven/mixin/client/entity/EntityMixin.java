package sanch3zss.raven.mixin.client.entity;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.feature.module.player.FreeLookModule;
import sanch3zss.raven.feature.module.player.FreeLookCameraState;

@Mixin({Entity.class})
public abstract class EntityMixin implements FreeLookCameraState {
    @Unique
    private float raven$cameraPitch;
    @Unique
    private float raven$cameraYaw;
    @Unique
    private float raven$freeLookAnchorYaw;
    @Unique
    private boolean raven$freeLookHasAnchor;

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void raven$changeFreeLookDirection(double deltaX, double deltaY, CallbackInfo callbackInfo) {
        if ((Object) this instanceof LocalPlayer
                && Raven.INSTANCE.moduleRepository().get(FreeLookModule.class).isFreeLooking()) {
            if (!this.raven$freeLookHasAnchor) {
                this.raven$freeLookAnchorYaw = this.raven$cameraYaw;
                this.raven$freeLookHasAnchor = true;
            }
            this.raven$cameraPitch = Mth.clamp(
                    this.raven$cameraPitch + (float) (deltaY * 0.15d), -90.0f, 90.0f);
            this.raven$cameraYaw += (float) (deltaX * 0.15d);
            callbackInfo.cancel();
        } else if (this.raven$freeLookHasAnchor) {
            this.raven$freeLookHasAnchor = false;
        }
    }

    @Override
    public float raven$getCameraPitch() {
        return this.raven$cameraPitch;
    }

    @Override
    public float raven$getCameraYaw() {
        return this.raven$cameraYaw;
    }

    @Override
    public void raven$setCameraPitch(float pitch) {
        this.raven$cameraPitch = pitch;
    }

    @Override
    public void raven$setCameraYaw(float yaw) {
        this.raven$cameraYaw = yaw;
        this.raven$freeLookAnchorYaw = yaw;
        this.raven$freeLookHasAnchor = true;
    }
    @ModifyArgs(method = {"moveRelative"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
    public void updateVelocityArgs(Args args) {
        Entity entity = (Entity) (Object) this;
        if (entity instanceof LocalPlayer) {
            MovementYawEvent event = new MovementYawEvent(entity.getYRot());
            Raven.INSTANCE.eventDispatcher().dispatch(event);
            args.set(2, Float.valueOf(event.getYaw()));
        }
    }

}
