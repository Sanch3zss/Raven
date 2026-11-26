package sanch3zss.raven;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;

import sanch3zss.raven.client.RavenBootstrap;
import sanch3zss.raven.feature.module.player.ItemScrollerController;
import sanch3zss.raven.feature.module.render.enchantcolor.CustomEnchantColorRenderer;

public class RavenInit implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)
                .registerReloadListener(CustomEnchantColorRenderer.INSTANCE);
        ItemScrollerController.initialize();
        RavenBootstrap.init();
        new com.holdmylua.source.LuaTestHMI().onInitialize();
    }
}
