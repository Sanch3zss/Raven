package sanch3zss.raven.mixin.accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MultiPlayerGameMode.class})
public interface InteractionManagerAccessor {
    @Invoker("ensureHasSentCarriedItem")
    void syncSlot();
}
