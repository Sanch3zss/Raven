package sanch3zss.raven.event.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import sanch3zss.raven.event.Event;
import sanch3zss.raven.event.render.Render2DStage;

public final class Render2DEvent implements Event {
    public final PoseStack matrixStack;

    public final Render2DStage stage;

    public final DeltaTracker tickCounter;

    public final GuiGraphicsExtractor drawContext;

    public Render2DEvent(PoseStack matrixStack, Render2DStage render2DStage, DeltaTracker renderTickCounter, GuiGraphicsExtractor drawContext) {
        this.matrixStack = matrixStack;
        this.stage = render2DStage;
        this.tickCounter = renderTickCounter;
        this.drawContext = drawContext;
    }

    public boolean isPost() {
        return this.stage == Render2DStage.POST;
    }

    public boolean isPre() {
        return this.stage == Render2DStage.PRE;
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "matrixStack=" + this.matrixStack + ", " + "stage=" + this.stage + ", " + "tickCounter=" + this.tickCounter + ", " + "drawContext=" + this.drawContext + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.matrixStack, this.stage, this.tickCounter, this.drawContext);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Render2DEvent)) return false;
        Render2DEvent o = (Render2DEvent) obj;
        return java.util.Objects.equals(this.matrixStack, o.matrixStack) && java.util.Objects.equals(this.stage, o.stage) && java.util.Objects.equals(this.tickCounter, o.tickCounter) && java.util.Objects.equals(this.drawContext, o.drawContext);
    }
public PoseStack matrixStack() {
        return this.matrixStack;
    }

    public Render2DStage stage() {
        return this.stage;
    }

    public DeltaTracker tickCounter() {
        return this.tickCounter;
    }

    public GuiGraphicsExtractor drawContext() {
        return this.drawContext;
    }
}
