
package com.holdmylua.source;

import com.holdmylua.source.global.GlobalsStorage;
import com.holdmylua.source.lua_runtime.resource_controller.LuaAnimationResourceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LuaTestHMI
implements ModInitializer {
    public static final String MOD_ID = "holdmyitems";
    public static Matrix4f matricesMain = new Matrix4f();
    public static Matrix4f matricesOff = new Matrix4f();
    public static float tickProgress = 0.0f;
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"holdmyitems");
    public static Minecraft client = Minecraft.getInstance();
    public static float prevTime = 0.0f;
    public static float deltaTime = 0.0f;

    public void onInitialize() {
        ClassLoader parentClassLoader = this.getClass().getClassLoader();
        System.out.println("Using parent class loader: " + String.valueOf(parentClassLoader));
        HudElementRegistry.addLast((Identifier)Identifier.fromNamespaceAndPath((String)"example", (String)"hud"), (context, tickCounter) -> {
            AtomicInteger y = new AtomicInteger(10);
            GlobalsStorage.debugTextRenderer.get().forEach(str -> {
                context.text(Minecraft.getInstance().font, str, 10, y.get(), -1);
                y.addAndGet(10);
            });
            GlobalsStorage.debugTextRenderer.clear();
        });
        ResourceManagerHelper.get((PackType)PackType.CLIENT_RESOURCES).registerReloadListener((IdentifiableResourceReloadListener)new LuaAnimationResourceLoader());
        Identifier packIdentifier = Identifier.fromNamespaceAndPath((String)MOD_ID, (String)"pack_test");

        FabricLoader.getInstance().getModContainer(MOD_ID)
                .or(() -> FabricLoader.getInstance().getModContainer("raven"))
                .ifPresent(container -> {
            ResourceLoader.registerBuiltinPack((Identifier)packIdentifier, (ModContainer)container, (Component)Component.nullToEmpty((String)"Hold My Items"), (PackActivationType)PackActivationType.ALWAYS_ENABLED);
            LOGGER.info("Registered embedded resource pack: {}", (Object)packIdentifier);
        });
        LOGGER.info("What you staring at?");
    }
}
