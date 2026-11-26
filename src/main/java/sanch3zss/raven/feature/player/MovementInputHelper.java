package sanch3zss.raven.feature.player;

import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.util.math.interpolation.DirectionalInput;

public final class MovementInputHelper {
    public static KeyMapping[] getMovementKeys(boolean sneak, boolean sprint) {
        Options gameOptions = Mc.INSTANCE.getGameOptions();
        KeyMapping[] keyBindingArr = new KeyMapping[7];
        keyBindingArr[0] = gameOptions.keyUp;
        keyBindingArr[1] = gameOptions.keyDown;
        keyBindingArr[2] = gameOptions.keyLeft;
        keyBindingArr[3] = gameOptions.keyRight;
        keyBindingArr[4] = gameOptions.keyJump;
        keyBindingArr[5] = sprint ? gameOptions.keySprint : null;
        keyBindingArr[6] = sneak ? gameOptions.keyShift : null;
        return Stream.of(keyBindingArr).filter(Objects::nonNull).toArray(KeyMapping[]::new);
    }

    public static void refreshPressedKeys() {
        KeyMapping.setAll();
    }

    public static void suppressMovementKeys() {
        for (KeyMapping keyBinding : getMovementKeys(true, true)) {
            keyBinding.setDown(false);
        }
    }

    public static float getMovementDirectionOfInput(float yaw, DirectionalInput directionalInput) {
        float dir = yaw;
        float factor = 1.0f;
        if (directionalInput.backward()) {
            dir += 180.0f;
            factor = -0.5f;
        } else if (directionalInput.forward()) {
            factor = 0.5f;
        }
        if (directionalInput.left()) {
            dir -= 90.0f * factor;
        }
        if (directionalInput.right()) {
            dir += 90.0f * factor;
        }
        return dir;
    }

    public static boolean hasPlayerMovement() {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null || player.input == null) {
            return false;
        }
        return player.input.getMoveVector().y != 0.0f || player.input.getMoveVector().x != 0.0f;
    }

    public MovementInputHelper() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
