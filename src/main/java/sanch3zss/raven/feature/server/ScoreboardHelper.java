package sanch3zss.raven.feature.server;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import sanch3zss.raven.client.Mc;

public class ScoreboardHelper {
    public static final ScoreboardHelper INSTANCE = new ScoreboardHelper();
    public Component header;

    public String getHeaderAsString() {
        return this.header == null ? "" : this.header.getString();
    }

    public boolean headerContains(String str) {
        return getHeaderAsString().toLowerCase().contains(str);
    }

    public float getHealthBelowName(LivingEntity livingEntity) {
        try {
            Player clientPlayerEntity;
            ScoreAccess orCreateScore;
            if ((livingEntity instanceof Player) && (clientPlayerEntity = (Player) livingEntity) != Mc.INSTANCE.getPlayer()) {
                ClientLevel world = Mc.INSTANCE.getWorld();
                if (world != null && world.getScoreboard() != null) {
                    Objective objectiveForSlot = world.getScoreboard().getDisplayObjective(DisplaySlot.BELOW_NAME);
                    if (objectiveForSlot != null && (orCreateScore = world.getScoreboard().getOrCreatePlayerScore(ScoreHolder.forNameOnly(livingEntity.getScoreboardName()), objectiveForSlot)) != null) {
                        return orCreateScore.get() == 0 ? clientPlayerEntity.getHealth() : orCreateScore.get();
                    }
                }
            }
            return livingEntity.getHealth();
        } catch (Throwable ignored) {
            return livingEntity != null ? livingEntity.getHealth() : 20.0f;
        }
    }

    public void setHeader(Component text) {
        this.header = text;
    }

    public Component getHeader() {
        return this.header;
    }
}
