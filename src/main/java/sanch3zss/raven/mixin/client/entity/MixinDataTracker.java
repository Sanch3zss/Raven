package sanch3zss.raven.mixin.client.entity;

import net.minecraft.core.Rotations;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({SynchedEntityData.class})
public abstract class MixinDataTracker {
    @Inject(method = {"set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V"}, at = {@At("HEAD")}, cancellable = true)
    private <T> void onSet(EntityDataAccessor<T> trackedData, T t, CallbackInfo callbackInfo) {
        EntityDataSerializer trackedDataHandlerDataType = trackedData.serializer();
        if (trackedDataHandlerDataType == EntityDataSerializers.BYTE && !(t instanceof Byte)) {
            callbackInfo.cancel();
        }
        if (trackedDataHandlerDataType != EntityDataSerializers.ROTATIONS || (t instanceof Rotations)) {
            return;
        }
        callbackInfo.cancel();
    }
}
