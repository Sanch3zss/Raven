package sanch3zss.raven.feature.module.player;

import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.client.chat.ChatUtil;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.notification.NotificationType;

@Aliases(aliases = {"Death Coords", "Death Coordinates", "Point Death"})
public class DeathCoordinatesModule extends Module {
    public boolean alreadyPosted;

    public DeathCoordinatesModule() {
        super(ModuleTab.PLAYER, "DeathCoordinates");
        register(PlayerInitEvent.class, event2 -> {
            this.alreadyPosted = false;
        });
        register(PlayerTickEvent.class, event -> {
            if (isState() && event.isPre()) {
                Mc mc = Mc.INSTANCE;
                LocalPlayer player = mc.getPlayer();
                if (!(mc.getCurrentScreen() instanceof DeathScreen) || this.alreadyPosted) {
                    return;
                }
                BlockPos blockPos = player.blockPosition();
                MutableComponent coordinates = Component.literal(Lang.DEATHCOORDINATES_DEATH_TEXT.effective() + ": ")
                        .append(Component.literal("X: ").withStyle(ChatFormatting.RED))
                        .append(Component.literal(Integer.toString(blockPos.getX())))
                        .append(Component.literal(" Y: ").withStyle(ChatFormatting.RED))
                        .append(Component.literal(Integer.toString(blockPos.getY())))
                        .append(Component.literal(" Z: ").withStyle(ChatFormatting.RED))
                        .append(Component.literal(Integer.toString(blockPos.getZ())));
                Raven.INSTANCE.notificationRepository().post(NotificationType.INFO,
                        Component.literal(coordinates.getString()), 5L, TimeUnit.SECONDS);
                ChatUtil.addChatMessage(coordinates);
                this.alreadyPosted = true;
            }
        });
    }
}
