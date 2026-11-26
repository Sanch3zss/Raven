package sanch3zss.raven.mixin.client.render.entity;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.BlankNamePrefix;
import sanch3zss.raven.feature.module.render.CustomTagsModule;
import sanch3zss.raven.feature.module.render.ClientUsersModule;
import sanch3zss.raven.feature.module.render.chinahat.ChinaHatRenderState;
import sanch3zss.raven.feature.module.render.chinahat.ChinaHatRenderLayer;
import sanch3zss.raven.mixin.accessor.LivingEntityRendererAccessor;

@Mixin(AvatarRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void raven$addChinaHatLayer(EntityRendererProvider.Context context, boolean slim,
                                        CallbackInfo callbackInfo) {
        ((LivingEntityRendererAccessor) this).raven$addLayer(
                new ChinaHatRenderLayer((AvatarRenderer<?>) (Object) this));
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/Avatar;D)Z", at = @At("RETURN"), cancellable = true)
    private void raven$selectCustomTagTargets(Avatar player, double distance,
                                               CallbackInfoReturnable<Boolean> callbackInfo) {
        CustomTagsModule module = Raven.INSTANCE.moduleRepository().get(CustomTagsModule.class);
        if (player != Minecraft.getInstance().player || !module.shouldShowSelf()) {
            return;
        }
        callbackInfo.setReturnValue(!((Entity) player).isInvisible() && distance < 4096.0d
                && Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON);
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V", at = @At("TAIL"))
    private void raven$updateCustomTagState(Avatar player, AvatarRenderState state,
                                            float tickProgress, CallbackInfo callbackInfo) {
        if (state.nameTag != null) {
            state.nameTag = BlankNamePrefix.strip(state.nameTag.copy());
        }
        ClientUsersModule clientUsers = Raven.INSTANCE.moduleRepository().get(ClientUsersModule.class);
        if (state.nameTag != null &&clientUsers.shouldMark(player.getScoreboardName())) {
            state.nameTag = clientUsers.decorate(state.nameTag);
        }
        ChinaHatRenderState hatState = (ChinaHatRenderState) state;
        Equippable equippable = player.getItemBySlot(EquipmentSlot.HEAD).get(DataComponents.EQUIPPABLE);
        hatState.raven$setLocalPlayer(player == Minecraft.getInstance().player);
        hatState.raven$setHeadArmor(equippable != null && equippable.slot() == EquipmentSlot.HEAD
                && equippable.assetId().isPresent());
        hatState.raven$setHatLayer(player.isModelPartShown(PlayerModelPart.HAT));
        hatState.raven$setHatRadius(((Entity) player).getBbWidth());
    }

}
