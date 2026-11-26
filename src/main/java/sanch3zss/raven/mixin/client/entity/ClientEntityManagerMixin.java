package sanch3zss.raven.mixin.client.entity;

import net.minecraft.world.entity.*;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.player.EntityLifecycleEvent;
import sanch3zss.raven.feature.entity.EntityLifecycleAction;

@Mixin({TransientEntitySectionManager.Callback.class})
public class ClientEntityManagerMixin<T extends EntityAccess> {

    @Shadow
    @Final
    private T entity;

    @Inject(method = {"<init>"}, at = {@At("TAIL")})
    protected void init(CallbackInfo callbackInfo) {
        Entity entity = (Entity) (this.entity);
        if (entity instanceof Entity) {
            Raven.INSTANCE.eventDispatcher().dispatch(new EntityLifecycleEvent(entity, EntityLifecycleAction.ADD));
        }
    }

    @Inject(method = {"onRemove"}, at = {@At("HEAD")})
    public void removeEntityHook(Entity.RemovalReason removalReason, CallbackInfo callbackInfo) {
        Entity entity = (Entity) (this.entity);
        if (entity instanceof Entity) {
            Raven.INSTANCE.eventDispatcher().dispatch(new EntityLifecycleEvent(entity, EntityLifecycleAction.REMOVE));
        }
    }
}
