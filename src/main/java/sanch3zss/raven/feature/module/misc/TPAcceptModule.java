package sanch3zss.raven.feature.module.misc;

import java.util.Locale;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.util.StringUtil;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.network.PacketReceiveEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.player.PvPModeDetector;
import sanch3zss.raven.feature.friend.FriendEntry;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;

@Aliases(aliases = {"TP Accept", "Auto TP Accept", "Teleport Accept", "Auto TPA", "TP Accept Helper"})
public class TPAcceptModule extends Module {
    public final BooleanSetting onlyFriends;

    public TPAcceptModule() {
        super(ModuleTab.MISC, "TP Accept");
        this.onlyFriends = new BooleanSetting(Lang.AUTOTPACCEPT_ONLYFRIENDS, Lang.AUTOTPACCEPT_ONLYFRIENDS_DESC);
        addSettings(this.onlyFriends);
        register(PacketReceiveEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (isState() && mc.isWorldLoaded() && !PvPModeDetector.isPvPMode()) {
                ClientPacketListener networkHandler = mc.getNetworkHandler();
                if ((event.getPacket()) instanceof ClientboundSystemChatPacket packet ) {
                    String lowerCase = StringUtil.stripColor(packet.content().getString()).toLowerCase();
                    if (lowerCase.contains("телепортироваться") || lowerCase.contains("has requested teleport") || lowerCase.contains("/tpyes")) {
                        if (!this.onlyFriends.isValue() || isFromFriend(lowerCase)) {
                            networkHandler.sendCommand("tpaccept");
                        }
                    }
                }
            }
        });
    }

    public boolean isFromFriend(String str) {
        for (FriendEntry friend : FriendManager.entries()) {
            if (str.contains(friend.name().toLowerCase(Locale.ROOT))) {
                return true;
            }
            if (friend.alias() != null && str.contains(friend.alias().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
