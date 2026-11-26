package sanch3zss.raven.mixin.client.render.entity;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.*;

import sanch3zss.raven.feature.module.render.chinahat.ChinaHatRenderState;

@Mixin(AvatarRenderState.class)
public class PlayerEntityRenderStateMixin implements ChinaHatRenderState {
    @Unique private boolean raven$localPlayer;
    @Unique private boolean raven$headArmor;
    @Unique private boolean raven$hatLayer;
    @Unique private float raven$hatRadius;

    @Override public boolean raven$isLocalPlayer() { return this.raven$localPlayer; }
    @Override public void raven$setLocalPlayer(boolean value) { this.raven$localPlayer = value; }
    @Override public boolean raven$hasHeadArmor() { return this.raven$headArmor; }
    @Override public void raven$setHeadArmor(boolean value) { this.raven$headArmor = value; }
    @Override public boolean raven$hasHatLayer() { return this.raven$hatLayer; }
    @Override public void raven$setHatLayer(boolean value) { this.raven$hatLayer = value; }
    @Override public float raven$hatRadius() { return this.raven$hatRadius; }
    @Override public void raven$setHatRadius(float value) { this.raven$hatRadius = value; }
}
