package discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.server.IntegratedServer;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.feature.module.misc.DiscordRpcModule;

public class DiscordManager {
    private static final String CLIENT_ID = "1569293953398874253";
    private static final String LOGO = "logo";
    private static final String GITHUB = "https://github.com/Sanch3zss/Raven";
    private static final String TELEGRAM = "https://t.me/ravenvisuals";
    private static final String MENU = "menu";

    private final long launchedAt = System.currentTimeMillis() / 1000L;
    private volatile String activity;
    private String session = MENU;
    private long sessionStart = this.launchedAt;
    private ScheduledFuture<?> task;
    private ByteChannel channel;
    private String lastActivity;
    private long lastSentAt;
    private long nextConnectAt;

    public DiscordManager() {}

    public void init() {
        Raven.INSTANCE.eventDispatcher().register(ClientTickEvent.class, event -> capture());
        if (Raven.INSTANCE.moduleRepository().get(DiscordRpcModule.class).isState()) start();
    }

    public synchronized void start() {
        if (this.task != null && !this.task.isDone()) return;
        this.nextConnectAt = 0L;
        this.task = Raven.INSTANCE.executor().scheduleWithFixedDelay(this::update, 1L, 2L, TimeUnit.SECONDS);
    }

    public synchronized void stopRPC() {
        if (this.task != null) this.task.cancel(false);
        close();
    }

    private void capture() {
        Minecraft minecraft = Minecraft.getInstance();
        Screen screen = minecraft.gui.screen();
        ServerData server = minecraft.getCurrentServer();
        ClientPacketListener connection = minecraft.getConnection();
        String session = this.session;
        String state;
        JsonArray party = null;
        if (minecraft.level != null && minecraft.isLocalServer()) {
            IntegratedServer integrated = minecraft.getSingleplayerServer();
            session = "local";
            state = integrated != null && integrated.isPublished() ? "Одиночная игра, открыта для сети" : "Одиночная игра";
        } else if (minecraft.level != null && server != null) {
            if (server.isLan()) {
                session = "lan:" + server.ip;
                state = "Игра по локальной сети";
            } else {
                session = "server:" + server.ip.toLowerCase(Locale.ROOT);
                state = "Сервер: " + server.ip;
                int online = connection == null ? 0 : connection.getOnlinePlayers().size();
                if (online > 0) {
                    party = new JsonArray();
                    party.add(online);
                    party.add(server.players == null ? online : Math.max(server.players.max(), online));
                }
            }
        } else if (screen instanceof ConnectScreen) {
            state = "Подключается к серверу";
        } else if (screen instanceof LevelLoadingScreen || screen instanceof ProgressScreen
                || screen instanceof GenericMessageScreen) {
            state = "Загружает мир";
        } else if (screen instanceof JoinMultiplayerScreen || screen instanceof DirectJoinServerScreen
                || screen instanceof ManageServerScreen) {
            session = MENU;
            state = "Выбирает сервер";
        } else if (screen instanceof SelectWorldScreen || screen instanceof CreateWorldScreen) {
            session = MENU;
            state = "Выбирает мир";
        } else if (screen instanceof DisconnectedScreen) {
            session = MENU;
            state = "Отключился от сервера";
        } else {
            session = MENU;
            state = "В главном меню";
        }
        if (!session.equals(this.session)) {
            this.session = session;
            this.sessionStart = MENU.equals(session) ? this.launchedAt : System.currentTimeMillis() / 1000L;
        }
        this.activity = activity(state, party).toString();
    }

    private JsonObject activity(String state, JsonArray partySize) {
        JsonObject activity = new JsonObject();
        activity.addProperty("details", SharedConstants.getCurrentVersion().name());
        activity.addProperty("state", state);

        JsonObject timestamps = new JsonObject();
        timestamps.addProperty("start", this.sessionStart);
        activity.add("timestamps", timestamps);

        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", LOGO);
        assets.addProperty("large_text", "RavenVisuals");
        activity.add("assets", assets);

        if (partySize != null) {
            JsonObject party = new JsonObject();
            party.addProperty("id", "raven-" + this.session);
            party.add("size", partySize);
            activity.add("party", party);
        }

        JsonArray buttons = new JsonArray();
        buttons.add(button("GitHub", GITHUB));
        buttons.add(button("Telegram", TELEGRAM));
        activity.add("buttons", buttons);
        return activity;
    }

    private static JsonObject button(String label, String url) {
        JsonObject button = new JsonObject();
        button.addProperty("label", label);
        button.addProperty("url", url);
        return button;
    }

    private synchronized void update() {
        long now = System.currentTimeMillis();
        try {
            if (this.channel == null) {
                if (now < this.nextConnectAt) return;
                connect();
            }
            String json = this.activity;
            if (json == null || json.equals(this.lastActivity) && now - this.lastSentAt < 60_000L) return;
            JsonObject args = new JsonObject();
            args.addProperty("pid", ProcessHandle.current().pid());
            args.add("activity", JsonParser.parseString(json));
            JsonObject command = new JsonObject();
            command.addProperty("cmd", "SET_ACTIVITY");
            command.add("args", args);
            command.addProperty("nonce", UUID.randomUUID().toString());
            write(1, command.toString());
            read();
            this.lastActivity = json;
            this.lastSentAt = now;
        } catch (IOException error) {
            close();
            this.nextConnectAt = now + 5_000L;
        } catch (Throwable error) {
            Raven.LOGGER.warn("Discord RPC update failed", error);
        }
    }

    private void connect() throws IOException {
        for (int index = 0; index < 10; index++) {
            try {
                this.channel = open(index);
            } catch (IOException ignored) {
                continue;
            }
            JsonObject handshake = new JsonObject();
            handshake.addProperty("v", 1);
            handshake.addProperty("client_id", CLIENT_ID);
            write(0, handshake.toString());
            read();
            this.lastActivity = null;
            return;
        }
        throw new IOException("Discord is not running");
    }

    private static ByteChannel open(int index) throws IOException {
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            return new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + index, "rw").getChannel();
        }
        for (Path path : socketPaths(index)) {
            if (Files.exists(path)) {
                return SocketChannel.open(UnixDomainSocketAddress.of(path));
            }
        }
        throw new IOException("No Discord socket " + index);
    }

    private static List<Path> socketPaths(int index) {
        List<Path> paths = new ArrayList<>();
        for (String variable : List.of("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")) {
            String value = System.getenv(variable);
            if (value == null || value.isBlank()) continue;
            Path base = Path.of(value);
            paths.add(base.resolve("discord-ipc-" + index));
            paths.add(base.resolve("app/com.discordapp.Discord/discord-ipc-" + index));
            paths.add(base.resolve("snap.discord/discord-ipc-" + index));
        }
        paths.add(Path.of("/tmp/discord-ipc-" + index));
        return paths;
    }

    private void write(int opcode, String json) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(8 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(opcode).putInt(payload.length).put(payload).flip();
        while (buffer.hasRemaining()) this.channel.write(buffer);
    }

    private JsonObject read() throws IOException {
        ByteBuffer header = readFully(8).order(ByteOrder.LITTLE_ENDIAN);
        int opcode = header.getInt();
        int length = header.getInt();
        String json = new String(readFully(length).array(), StandardCharsets.UTF_8);
        if (opcode == 2) throw new IOException("Discord closed the connection: " + json);
        JsonObject response = JsonParser.parseString(json).getAsJsonObject();
        JsonElement event = response.get("evt");
        if (event != null && !event.isJsonNull() && "ERROR".equals(event.getAsString())) {
            Raven.LOGGER.warn("Discord RPC error: {}", json);
        }
        return response;
    }

    private ByteBuffer readFully(int length) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(length);
        while (buffer.hasRemaining()) {
            if (this.channel.read(buffer) < 0) throw new IOException("Discord pipe closed");
        }
        return buffer.flip();
    }

    private synchronized void close() {
        if (this.channel == null) return;
        try {
            this.channel.close();
        } catch (IOException ignored) {
        }
        this.channel = null;
        this.lastActivity = null;
    }
}
