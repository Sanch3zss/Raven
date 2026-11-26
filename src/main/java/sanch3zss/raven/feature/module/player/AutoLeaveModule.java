package sanch3zss.raven.feature.module.player;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.misc.AntiRelogModule;
import sanch3zss.raven.feature.player.PvPModeDetector;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.ModeSetting;
import sanch3zss.raven.ui.setting.NumberSetting;
import sanch3zss.raven.ui.setting.TextFieldSetting;

@Aliases(aliases = {"Auto Leave", "Auto Disconnect", "Auto Hub"})
public class AutoLeaveModule extends Module {
    private final NumberSetting radius = new NumberSetting(Translation.clearText("Радиус"))
            .range(2.0f, 48.0f).currentValue(8.0f).step(0.5f);
    private final ModeSetting<Destination> destination = new ModeSetting<Destination>(
            Translation.clearText("Куда выходить")).values(Destination.class);
    private final TextFieldSetting command = new TextFieldSetting(Translation.clearText("Команда"))
            .setPlaceholder(Translation.clearText("spawn"))
            .visible(() -> this.destination.isSelected(Destination.COMMAND));
    private final BooleanSetting ignoreFriends = new BooleanSetting(
            Translation.clearText("Не учитывать друзей"));
    private boolean triggered;

    public AutoLeaveModule() {
        super(ModuleTab.PLAYER, "AutoLeave");
        addSettings(this.radius, this.destination, this.command, this.ignoreFriends);
        register(PlayerTickEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (!isState() || !event.isPre() || !mc.isWorldLoaded() || mc.isSingleplayer()) {
                return;
            }
            if (PvPModeDetector.isPvPMode()
                    || Raven.INSTANCE.moduleRepository().get(AntiRelogModule.class).isInCombat()) {
                this.triggered = false;
                return;
            }

            LocalPlayer localPlayer = mc.getPlayer();
            FakePlayerModule fakePlayerModule = Raven.INSTANCE.moduleRepository().get(FakePlayerModule.class);
            double radiusSquared = (double) this.radius.currentValue() * this.radius.currentValue();
            boolean playerNearby = false;
            for (AbstractClientPlayer other : mc.getWorld().players()) {
                if (other == localPlayer || other == fakePlayerModule.getFakePlayer()
                        || this.ignoreFriends.isValue() && FriendManager.isFriend(other.getName().getString())) {
                    continue;
                }
                if (localPlayer.distanceToSqr(other) <= radiusSquared) {
                    playerNearby = true;
                    break;
                }
            }

            if (!playerNearby) {
                this.triggered = false;
            } else if (!this.triggered) {
                this.triggered = true;
                if (this.destination.isSelected(Destination.HUB)) {
                    mc.getNetworkHandler().sendCommand("hub");
                } else if (this.destination.isSelected(Destination.COMMAND)) {
                    String text = this.command.getText() == null ? "" : this.command.getText().trim();
                    if (text.startsWith("/")) text = text.substring(1);
                    if (!text.isEmpty()) mc.getNetworkHandler().sendCommand(text);
                } else {
                    var connection = mc.getNetworkHandler();
                    mc.getMinecraft().schedule(() -> {
                        if (mc.getNetworkHandler() == connection && !PvPModeDetector.isPvPMode()
                                && !Raven.INSTANCE.moduleRepository().get(AntiRelogModule.class).isInCombat()) {
                            mc.getMinecraft().disconnectFromWorld(Component.literal("AutoLeave: игрок рядом"));
                        }
                    });
                }
            }
        });
    }

    @Override
    public void activate() {
        this.triggered = false;
        super.activate();
    }

    @Override
    public void deactivate() {
        this.triggered = false;
        super.deactivate();
    }

    private enum Destination implements DisplayNamed {
        HUB("Хаб"),
        DISCONNECT("Выйти с сервера"),
        COMMAND("Своя команда");

        private final Translation displayName;

        Destination(String name) {
            this.displayName = Translation.clearText(name);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }
}
