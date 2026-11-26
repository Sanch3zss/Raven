package sanch3zss.raven.mixin.client.input;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ClientInput.class})
public interface InputMixin {
    @Accessor("moveVector")
    Vec2 raven$getMovementVector();

    @Accessor("moveVector")
    void raven$setMovementVector(Vec2 movementVector);

}
