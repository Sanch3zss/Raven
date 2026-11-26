package sanch3zss.raven.feature.server;

import io.netty.channel.Channel;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import sanch3zss.raven.mixin.accessor.ConnectionAccessor;

public final class ViaVersionBridge {
    private static boolean attempted;
    private static Method protocolTranslation;
    private static Method targetVersion;
    private static Method olderThan;
    private static Class<?> versionClass;
    private static final Map<String, Object> versions = new HashMap<>();

    private ViaVersionBridge() {
    }

    public static boolean olderThan(Channel channel, String version) {
        if (channel == null || !FabricLoader.getInstance().isModLoaded("viafabricplus") || !resolve()) {
            return false;
        }
        try {
            Object reference = versions.computeIfAbsent(version, name -> {
                try {
                    return versionClass.getField(name).get(null);
                } catch (ReflectiveOperationException error) {
                    return null;
                }
            });
            if (reference == null) return false;
            Object api = Class.forName("com.viaversion.viafabricplus.ViaFabricPlus").getMethod("api").invoke(null);
            Object target = targetVersion.invoke(protocolTranslation.invoke(api), channel);
            return (boolean) olderThan.invoke(target, reference);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            return false;
        }
    }

    public static boolean serverOlderThan(String version) {
        ClientPacketListener listener = Minecraft.getInstance().getConnection();
        if (listener == null) return false;
        return olderThan(((ConnectionAccessor) listener.getConnection()).raven$getChannel(), version);
    }

    private static synchronized boolean resolve() {
        if (attempted) return targetVersion != null;
        attempted = true;
        try {
            Class<?> api = Class.forName("com.viaversion.viafabricplus.api.ViaFabricPlusAPI");
            Class<?> translation = Class.forName("com.viaversion.viafabricplus.api.protocoltranslator.ProtocolTranslation");
            versionClass = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
            protocolTranslation = api.getMethod("protocolTranslation");
            targetVersion = translation.getMethod("targetVersion", Channel.class);
            olderThan = versionClass.getMethod("olderThan", versionClass);
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            targetVersion = null;
            return false;
        }
    }
}
