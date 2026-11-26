package sanch3zss.raven.mixin.client.screen;

import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Scoreboard.class})
public class ScoreboardMixin {
    @Inject(method = {"removePlayerFromTeam(Ljava/lang/String;Lnet/minecraft/world/scores/PlayerTeam;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void onRemoveScoreHolderFromTeam(String str, PlayerTeam team, CallbackInfo callbackInfo) {
        if (((Scoreboard) (Object) this).getPlayersTeam(str) != team) {
            callbackInfo.cancel();
        }
    }
}
