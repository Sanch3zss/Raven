package sanch3zss.raven.feature.server;

import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.apache.commons.lang3.StringUtils;

import sanch3zss.raven.client.*;
import sanch3zss.raven.event.network.PacketReceiveEvent;
import sanch3zss.raven.feature.server.ServerType;
import sanch3zss.raven.util.StringUtil;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.Stopwatch;

public final class ServerUtil {
    public static int anarchy;
    public static String server = "Vanilla";
    public static Stopwatch timer = new Stopwatch();
    public static float TPS = 20.0f;

    public static void tick() {
        if (Mc.INSTANCE.isWorldLoaded()) {
            anarchy = parseAnarchyNumber();
            server = getServer();
        }
    }

    public static Optional<Integer> valid1_17() {
        return Optional.of(Integer.valueOf(getProtocolVersion())).filter(num -> {
            return Mc.INSTANCE.isSingleplayer() || (num.intValue() > 754 && num.intValue() < 767);
        });
    }

    public static void packet(PacketReceiveEvent event) {
        if (event.getPacket() instanceof ClientboundSetTimePacket) {
            long elapsedTime = timer.getElapsedTime(TimeUnit.NANOSECONDS);
            if (elapsedTime > 0) {
                TPS = MathUtil.clamp(20.0f * (1.0E9f / elapsedTime), 0.0f, 20.0f);
            }
            timer.reset();
        }
    }

    public static void registerViaVersionClamp() {

    }

    public static int getProtocolVersion() {
        return 769;
    }

    public static String getWorldType() {
        return !Mc.INSTANCE.isWorldLoaded() ? "null" : Mc.INSTANCE.getWorld().dimension().identifier().getPath();
    }

    public static int parseAnarchyNumber() {
        String strSubstringBetween;
        Scoreboard scoreboard = Mc.INSTANCE.getWorld().getScoreboard();
        Objective objectiveForSlot = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        switch (server) {
            case "FunTime":
                if (objectiveForSlot == null) {
                    return -1;
                }
                String[] strArrSplit = objectiveForSlot.getDisplayName().getString().split("-");
                if (strArrSplit.length > 1) {
                    return Integer.parseInt(strArrSplit[1]);
                }
                return -1;
            case "HolyWorld":
                Pattern patternCompile = Pattern.compile("^(.*?)#(\\d+)(?:\\s*\\(1\\.20\\))?");
                for (PlayerScoreEntry scoreboardEntry : scoreboard.listPlayerScores(objectiveForSlot)) {
                    String string = PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(scoreboardEntry.owner()), scoreboardEntry.ownerName()).getString();
                    if (!string.isEmpty() && (strSubstringBetween = StringUtils.substringBetween(string, "-◆-", " -◆-")) != null && !strSubstringBetween.isEmpty()) {
                        Matcher matcher = patternCompile.matcher(strSubstringBetween.trim());
                        if (matcher.find()) {
                            return Integer.parseInt(matcher.group(2));
                        }
                    }
                }
                return -1;
            default:
                return -1;
        }
    }

    public static String getServer() {
        if (!Mc.INSTANCE.isWorldLoaded() || Mc.INSTANCE.getNetworkHandler().serverBrand() == null || Mc.INSTANCE.getServerInfo() == null) {
            return "Vanilla";
        }
        String lowerCase = Mc.INSTANCE.getServerInfo().ip.toLowerCase();
        String strRemoveSymbols = StringUtil.removeSymbols(Mc.INSTANCE.getNetworkHandler().serverBrand().toLowerCase());
        if (strRemoveSymbols.contains("botfilter")) {
            return "FunTime";
        }
        if (lowerCase.contains("funtime") || lowerCase.contains("skytime") || lowerCase.contains("space-times") || lowerCase.contains("funsky")) {
            return "CopyTime";
        }
        if (strRemoveSymbols.contains("holywоrld") || strRemoveSymbols.contains("vk.com/idwok")) {
            return "HolyWorld";
        }
        if (lowerCase.contains("reallyworld") || strRemoveSymbols.contains("reallyworld")) {
            return "ReallyWorld";
        }
        if (lowerCase.contains("spookytime") || strRemoveSymbols.contains("spookytime")) {
            return "SpookyTime";
        }
        if (lowerCase.contains("aresmine") || strRemoveSymbols.contains("Folia (Velocity)")) {
            return "AresMine";
        }
        if (lowerCase.contains("cakeworld") || strRemoveSymbols.contains("cakeworld")) {
            return "RwCopy";
        }
        return (lowerCase.contains("dexland") || lowerCase.contains("mineblaze")) ? "DexLand" : "Vanilla";
    }

    public static boolean isConnectedToServer(String str) {
        return server.toLowerCase().contains(str);
    }

    public static boolean isConnectedToAllFuntimeServers() {
        return server.equalsIgnoreCase("funtime") || server.equalsIgnoreCase("copytime");
    }

    public static String getServerIp() {
        if (Mc.INSTANCE.isSingleplayer() || !Mc.INSTANCE.isWorldLoaded()) {
            return "localhost";
        }
        SocketAddress address = Mc.INSTANCE.getNetworkHandler().getConnection().getRemoteAddress();
        return address instanceof InetSocketAddress ? ((InetSocketAddress) address).getHostString() : "localhost";
    }

    public static void reconnectToServer() {
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient == null || minecraftClient.isLocalServer()) {
            return;
        }
        ServerData currentServerEntry = minecraftClient.getCurrentServer();
        if (currentServerEntry == null) {
            currentServerEntry = new ServerData("ReallyWorld", "mc.reallyworld.ru:25565", ServerData.Type.OTHER);
        }
        ServerAddress serverAddress = ServerAddress.parseString(currentServerEntry.ip);
        ServerData serverInfo = currentServerEntry;
        minecraftClient.execute(() -> {
            Screen screen = Mc.INSTANCE.getCurrentScreen();
            if (screen != null) {
                screen.onClose();
            }
            ConnectScreen.startConnecting(screen, minecraftClient, serverAddress, serverInfo, false, (TransferState) null);
        });
    }

    public ServerUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static int getAnarchy() {
        return anarchy;
    }
}
