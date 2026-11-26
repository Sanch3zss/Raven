package sanch3zss.raven.feature.module.misc;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

public final class DiscordRpcModule extends Module {
    public DiscordRpcModule() {
        super(ModuleTab.MISC, "DiscordRpc");
        setStateSilent(true);
    }

    @Override
    public void activate() {
        if (Raven.INSTANCE.discordManager() != null) Raven.INSTANCE.discordManager().start();
        super.activate();
    }

    @Override
    public void deactivate() {
        if (Raven.INSTANCE.discordManager() != null) Raven.INSTANCE.discordManager().stopRPC();
        super.deactivate();
    }
}
