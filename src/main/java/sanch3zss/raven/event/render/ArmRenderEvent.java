package sanch3zss.raven.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import sanch3zss.raven.event.CancellableEvent;

public class ArmRenderEvent extends CancellableEvent {
    public final HumanoidArm arm;
    public final InteractionHand hand;
    public final PoseStack matrixStack;
    public final float swingProgress;
    public final ItemStack stack;

    public ArmRenderEvent(InteractionHand hand, HumanoidArm arm, PoseStack matrixStack, float f, ItemStack stack) {
        this.hand = hand;
        this.arm = arm;
        this.matrixStack = matrixStack;
        this.swingProgress = f;
        this.stack = stack;
    }

    public HumanoidArm arm() {
        return this.arm;
    }

    public InteractionHand hand() {
        return this.hand;
    }

    public PoseStack matrixStack() {
        return this.matrixStack;
    }

    public float swingProgress() {
        return this.swingProgress;
    }

    public ItemStack stack() {
        return this.stack;
    }
}
