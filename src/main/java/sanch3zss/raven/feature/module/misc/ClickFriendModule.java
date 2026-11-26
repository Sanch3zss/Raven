package sanch3zss.raven.feature.module.misc;

import java.util.concurrent.TimeUnit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.ui.setting.KeybindSetting;

@Aliases(aliases = {"Click Friend", "Friend Manager", "Add Friend", "Remove Friend", "Toggle Friend"})
public class ClickFriendModule extends Module {
    public final KeybindSetting keybind;

    public ClickFriendModule() {
        super(ModuleTab.MISC, "ClickFriend");
        this.keybind = new KeybindSetting(Lang.CLICKFRIEND_KEY);
        addSettings(this.keybind);
        this.keybind.consumer(value -> {
            Mc mc = Mc.INSTANCE;
            if (isState() && mc.isWorldLoaded()) {
                if ((mc.getCrosshairTarget()) instanceof EntityHitResult crosshairTarget ) {
                    if ((crosshairTarget.getEntity()) instanceof Player entity ) {
                        String string = entity.getName().getString();
                        String str = ChatFormatting.RED + string + ChatFormatting.RESET;
                        if (FriendManager.isFriend(string)) {
                            FriendManager.remove(string);
                            Raven.INSTANCE.notificationRepository().post(NotificationType.INFO, Component.literal(Lang.CLICKFRIEND_REMOVED.effective().replace("{name}", str)), 3L, TimeUnit.SECONDS);
                        } else {
                            FriendManager.add(string);
                            Raven.INSTANCE.notificationRepository().post(NotificationType.INFO, Component.literal(Lang.CLICKFRIEND_ADDED.effective().replace("{name}", str)), 3L, TimeUnit.SECONDS);
                        }
                    }
                }
            }
        });
    }
}
