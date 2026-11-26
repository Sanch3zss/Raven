package sanch3zss.raven.mixin.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.player.RemotePlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.player.FakePlayerModule;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerModule;

@Mixin(LevelExtractor.class)
public abstract class LevelExtractorMixin {
    @Inject(method = "extractVisibleEntities", at = @At("TAIL"))
    private void raven$extractFakePlayer(Camera camera, Frustum frustum, DeltaTracker deltaTracker,
                                         LevelRenderState output, CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getInstance();
        FakePlayerModule module = Raven.INSTANCE.moduleRepository().get(FakePlayerModule.class);
        RemotePlayer fakePlayer = module.getFakePlayer();
        if (module.isState() && fakePlayer != null && fakePlayer.level() == minecraft.level
                && frustum.isVisible(fakePlayer.getBoundingBox())) {
            output.entityRenderStates.add(minecraft.getEntityRenderDispatcher().extractEntity(
                    fakePlayer, deltaTracker.getGameTimeDeltaPartialTick(false)));
        }
        LeaveTrackerModule tracker = Raven.INSTANCE.moduleRepository().get(LeaveTrackerModule.class);
        tracker.capture(output.entityRenderStates);
        tracker.appendGhosts(frustum, output);
        output.lastEntityRenderStateCount = output.entityRenderStates.size();
    }
}
