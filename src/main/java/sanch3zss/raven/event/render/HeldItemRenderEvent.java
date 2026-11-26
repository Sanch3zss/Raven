package sanch3zss.raven.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import sanch3zss.raven.event.Event;

public final class HeldItemRenderEvent implements Event {
    public final InteractionHand hand;

    public final PoseStack matrices;
    public final ItemStack stack;

    public HeldItemRenderEvent(InteractionHand hand, PoseStack matrixStack, ItemStack itemStack) {
        this.hand = hand;
        this.matrices = matrixStack;
        this.stack = itemStack;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "hand=" + this.hand + ", " + "matrices=" + this.matrices + ", " + "stack=" + this.stack + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.hand, this.matrices, this.stack);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof HeldItemRenderEvent)) return false;
        HeldItemRenderEvent o = (HeldItemRenderEvent) obj;
        return java.util.Objects.equals(this.hand, o.hand) && java.util.Objects.equals(this.matrices, o.matrices) && java.util.Objects.equals(this.stack, o.stack);
    }
public InteractionHand hand() {
        return this.hand;
    }

    public PoseStack matrices() {
        return this.matrices;
    }

    public ItemStack stack() {
        return this.stack;
    }
}
