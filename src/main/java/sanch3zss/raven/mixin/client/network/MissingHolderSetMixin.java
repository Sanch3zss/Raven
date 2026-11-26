package sanch3zss.raven.mixin.client.network;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.HolderSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import sanch3zss.raven.client.Raven;

@Mixin(targets = "net.minecraft.network.codec.ByteBufCodecs$31")
public abstract class MissingHolderSetMixin {
    @Unique
    private static final AtomicBoolean raven$missingTagLogged = new AtomicBoolean();

    @WrapOperation(method = "decode(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Lnet/minecraft/core/HolderSet;",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow()Ljava/lang/Object;"))
    private Object raven$emptyMissingTag(Optional<?> holderSet, Operation<Object> original) {
        if (holderSet.isPresent()) {
            return original.call(holderSet);
        }
        if (raven$missingTagLogged.compareAndSet(false, true)) {
            Raven.LOGGER.warn("Unknown registry tag in a received item; using an empty holder set so the item remains visible");
        }
        return HolderSet.empty();
    }
}
