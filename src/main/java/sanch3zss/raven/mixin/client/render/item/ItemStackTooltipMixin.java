package sanch3zss.raven.mixin.client.render.item;

import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TooltipProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.ShulkerBoxTooltipModule;

@Mixin(ItemStack.class)
public abstract class ItemStackTooltipMixin {
    @Inject(method = "getTooltipImage", at = @At("HEAD"), cancellable = true)
    private void raven$shulkerPreview(CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        ShulkerBoxTooltipModule module = raven$module();
        if (module == null) return;
        Optional<TooltipComponent> preview = module.preview((ItemStack) (Object) this);
        if (preview.isPresent()) cir.setReturnValue(preview);
    }

    @Inject(method = "addToTooltip", at = @At("HEAD"), cancellable = true)
    private <T extends TooltipProvider> void raven$hideContainerLines(DataComponentType<T> type, Item.TooltipContext context,
                                                                     TooltipDisplay display, Consumer<Component> consumer,
                                                                     TooltipFlag flag, CallbackInfo ci) {
        if (type != DataComponents.CONTAINER) return;
        ShulkerBoxTooltipModule module = raven$module();
        if (module != null && module.handles((ItemStack) (Object) this)) ci.cancel();
    }

    @Unique
    private static ShulkerBoxTooltipModule raven$module() {
        Raven raven = Raven.INSTANCE;
        return raven == null || raven.moduleRepository() == null ? null
                : raven.moduleRepository().get(ShulkerBoxTooltipModule.class);
    }
}
