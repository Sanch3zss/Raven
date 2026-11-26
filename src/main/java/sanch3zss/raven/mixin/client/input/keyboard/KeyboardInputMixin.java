package sanch3zss.raven.mixin.client.input.keyboard;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.MovementUpdateSource;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.util.math.interpolation.DirectionalInput;
import sanch3zss.raven.util.math.rotation.*;

@Mixin({KeyboardInput.class})
public abstract class KeyboardInputMixin {

    @Shadow
    @Final
    private Options options;

    @ModifyExpressionValue(method = {"tick"}, at = {@At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;")})
    private Input modifyInput(Input playerInput) {
        MovementInputEvent event2 = new MovementInputEvent(DirectionalInput.fromInput(playerInput), playerInput.jump(), playerInput.shift(), playerInput.sprint());
        Raven.INSTANCE.eventDispatcher().dispatch(event2);
        DirectionalInput transformDirection = transformDirection(event2.getInput());
        MovementUpdateEvent event = new MovementUpdateEvent(transformDirection, playerInput.sprint(), MovementUpdateSource.INPUT);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        return new Input(transformDirection.forward(), transformDirection.backward(), transformDirection.left(), transformDirection.right(), event2.isJumping(), event2.isSneaking(), event.isSprint());
    }

    @Unique
    private DirectionalInput transformDirection(DirectionalInput directionalInput) {
        LocalPlayer clientPlayerEntity = Minecraft.getInstance().player;
        Rotation currentRotation = RotationManager.INSTANCE.getCurrentRotation();
        ScheduledRotation currentStrategy = RotationManager.INSTANCE.getCurrentStrategy();
        float movementMultiplier = KeyboardInput.calculateImpulse(directionalInput.forward(), directionalInput.backward());
        float movementMultiplier2 = KeyboardInput.calculateImpulse(directionalInput.left(), directionalInput.right());
        if (currentStrategy == null || ((currentStrategy.moveCorrection() && currentStrategy.focusedCorrection()) || currentRotation == null || clientPlayerEntity == null || !currentStrategy.moveCorrection())) {
            return directionalInput;
        }
        float radians = (float) Math.toRadians(clientPlayerEntity.getYRot() - currentRotation.getYaw());
        return DirectionalInput.fromMovementForwardAndSideways(Math.round((movementMultiplier * Mth.cos(radians)) + (movementMultiplier2 * Mth.sin(radians))), Math.round((movementMultiplier2 * Mth.cos(radians)) - (movementMultiplier * Mth.sin(radians))));
    }
}
