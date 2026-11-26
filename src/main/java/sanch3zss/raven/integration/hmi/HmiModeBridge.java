package sanch3zss.raven.integration.hmi;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;
import sanch3zss.raven.feature.module.render.armtweaks.SwingAnimationType;

public final class HmiModeBridge {
    private HmiModeBridge() {
    }

    public static boolean isActive() {
        try {
            ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
            return module != null && module.isHmiMode();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public static void applyViewModel(PoseStack matrices, InteractionHand hand, ItemStack stack) {
        if (ArmTweaksModule.hasSpecialPose(stack)) {
            return;
        }
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (module == null || !module.viewModelGroup.isValue()) {
            return;
        }

        float x;
        float y;
        float z;
        if (module.moveBothHands.isValue()) {
            x = module.translateX.currentValue();
            y = module.translateY.currentValue();
            z = module.translateZ.currentValue();
            if (hand != InteractionHand.MAIN_HAND) {
                x = -x;
            }
        } else {
            boolean right = module.getArmForHand(hand) == HumanoidArm.RIGHT;
            x = right ? module.rightHandTranslateX.currentValue() : module.leftHandTranslateX.currentValue();
            y = right ? module.rightHandTranslateY.currentValue() : module.leftHandTranslateY.currentValue();
            z = right ? module.rightHandTranslateZ.currentValue() : module.leftHandTranslateZ.currentValue();
        }
        matrices.translate(x, y, z);
    }
}
