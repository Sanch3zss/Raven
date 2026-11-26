package sanch3zss.raven.util.math.interpolation;

import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

public class DirectionalInput {
    public static final DirectionalInput NONE = new DirectionalInput(false, false, false, false);

    private final boolean forward;
    private final boolean backward;
    private final boolean left;
    private final boolean right;

    public DirectionalInput() {
        this(false, false, false, false);
    }

    public DirectionalInput(boolean z, boolean z2, boolean z3, boolean z4) {
        this.forward = z;
        this.backward = z2;
        this.left = z3;
        this.right = z4;
    }

    public boolean forward() {
        return this.forward;
    }

    public boolean backward() {
        return this.backward;
    }

    public boolean left() {
        return this.left;
    }

    public boolean right() {
        return this.right;
    }

    public boolean isMoving() {
        return this.forward || this.backward || this.left || this.right;
    }

    public static DirectionalInput fromInput(Input playerInput) {
        return new DirectionalInput(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right());
    }

    public static DirectionalInput fromPlayerInput(LocalPlayer clientPlayerEntity) {
        return fromInput(clientPlayerEntity.input.keyPresses);
    }

    public static DirectionalInput fromGameOptions(Options gameOptions) {
        return new DirectionalInput(gameOptions.keyUp.isDown(), gameOptions.keyDown.isDown(), gameOptions.keyLeft.isDown(), gameOptions.keyRight.isDown());
    }

    public static DirectionalInput fromMovementForwardAndSideways(float f, float f2) {
        return new DirectionalInput(f > 0.0f, f < 0.0f, f2 > 0.0f, f2 < 0.0f);
    }
}
