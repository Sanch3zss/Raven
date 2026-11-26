package sanch3zss.raven.feature.module.render.poptweaks;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import sanch3zss.raven.feature.module.render.poptweaks.PopTweaksModule;

public class PopChamsFakePlayer extends Player {
    public PopChamsFakePlayer(PopTweaksModule popTweaksModule, Level world, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(world, gameProfile);
        setPos(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        setYRot(f);
    }

    public boolean isSpectator() {
        return false;
    }

    public boolean isCreative() {
        return false;
    }

    public GameType gameMode() {
        return GameType.SURVIVAL;
    }
}
