package sanch3zss.raven.mixin.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import sanch3zss.raven.client.*;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.event.render.HandledScreenRenderEvent;

@Mixin({AbstractContainerScreen.class})
public abstract class HandledScreenMixin<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T> {

    @Shadow
    protected Slot hoveredSlot;

    @Shadow
    public int imageWidth;

    @Shadow
    public int imageHeight;

    @Shadow
    protected abstract void slotClicked(Slot slot, int slotId, int button, ContainerInput actionType);

    protected HandledScreenMixin(Component text) {
        super(text);
    }

    @Inject(method = {"keyPressed"}, at = {@At("HEAD")})
    private void onKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        Raven.INSTANCE.eventDispatcher().dispatch(new FocusedSlotEvent(this.hoveredSlot, input.key()));
    }

    @Inject(method = {"extractRenderState"}, at = {@At("RETURN")})
    public void onRender(GuiGraphicsExtractor drawContext, int i, int i2, float f, CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new HandledScreenRenderEvent(drawContext, this.imageWidth, this.imageHeight));
    }
}
