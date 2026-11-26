package sanch3zss.raven.feature.module.misc;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.config.CredentialKey;
import sanch3zss.raven.client.config.GamePaths;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.service.scheduling.ActionScheduler;
import sanch3zss.raven.event.network.ChatReceiveEvent;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.misc.AutoAuthMode;
import sanch3zss.raven.client.chat.ChatUtil;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.StringUtil;

@Aliases(aliases = {"Auto Auth", "Auto Login", "Auto Register", "Server Auth", "Login Helper"})
public class AutoAuthModule extends Module {
    public static final Pattern registerPattern = Pattern.compile("(?i)(^|\\s)/(register|reg)(\\s|$)");
    public static final Pattern loginPattern = Pattern.compile("(?i)(^|\\s)/(login|l)(\\s|$)");
    public final ModeSetting<AutoAuthMode> modeSetting;
    public final TextFieldSetting customValueSetting;
    public final Path defaultConfigPath;
    public boolean credentialsLoaded;
    public final Map<CredentialKey, String> credentials;

    public final ActionScheduler actionScheduler;
    public boolean processing;

    public AutoAuthModule() {
        super(ModuleTab.MISC, "AutoAuth");
        this.modeSetting = new ModeSetting(Lang.AUTOAUTH_MODE).values(AutoAuthMode.class);
        this.customValueSetting = new TextFieldSetting(Lang.AUTOAUTH_CUSTOM_VALUE).setPlaceholder(Lang.AUTOAUTH_CUSTOM_VALUE_PLACEHOLDER).setText("1337").setPassword(true).visible(() -> {
            return Boolean.valueOf(this.modeSetting.isSelected(AutoAuthMode.CUSTOM));
        });
        this.defaultConfigPath = GamePaths.configDirectory().resolve("auto-auth.json");
        this.credentials = new HashMap<>();
        this.actionScheduler = new ActionScheduler();
        this.processing = false;
        addSettings(this.modeSetting, this.customValueSetting, new ButtonSetting(Lang.AUTOAUTH_OPEN_DIRECTORY_BUTTON, Lang.AUTOAUTH_OPEN_DIRECTORY_BUTTON_DESC).setButtonName(Lang.AUTOAUTH_OPEN_DIRECTORY_BUTTON_NAME).setRunnable(() -> {
            try {
                Path configPath = getConfigPath();
                Path directory = configPath.toAbsolutePath().getParent();
                Files.createDirectories(directory);
                openDirectory(directory);
                Raven.INSTANCE.notificationRepository().post(NotificationType.INFO, Component.literal(Lang.AUTOAUTH_OPEN_DIRECTORY_SUCCESS.effective()), 3L, TimeUnit.SECONDS);
            } catch (IOException | UnsupportedOperationException e) {
                Raven.INSTANCE.notificationRepository().post(NotificationType.ERROR, Component.literal(Lang.AUTOAUTH_OPEN_DIRECTORY_ERROR.effective()), 3L, TimeUnit.SECONDS);
                Raven.LOGGER.error("Failed to open directory with credentials", e);
            }
        }));
        register(ChatReceiveEvent.class, event2 -> {
            String strMessage;
            if (!isState() || this.processing || (strMessage = event2.message()) == null) {
                return;
            }
            loadCredentials();
            String strRemoveSymbols = StringUtil.removeSymbols(strMessage);
            if (registerPattern.matcher(strRemoveSymbols).find()) {
                scheduleAuthAction(this::doRegister);
            } else if (loginPattern.matcher(strRemoveSymbols).find()) {
                scheduleAuthAction(this::doLogin);
            }
        });
        register(PlayerTickEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                this.actionScheduler.update().cleanupIfFinished();
            }
        });
    }

    public void scheduleAuthAction(Runnable runnable) {
        this.processing = true;
        this.actionScheduler.addTickStep(10, () -> {
            runnable.run();
            this.processing = false;
        }, () -> {
            return Mc.INSTANCE.getMinecraft().getConnection() != null;
        });
    }

    public void doRegister() {
        String password = generatePassword();
        if (password == null) {
            return;
        }
        this.credentials.put(currentCredentialKey(), password);
        sendAuthCommand("register " + password + " " + password, "Успешная регистрация! §7(наведите для просмотра пароля)", password);
        saveCredentials();
    }

    public void doLogin() {
        CredentialKey key = currentCredentialKey();
        if (this.credentials.containsKey(key)) {
            String str = this.credentials.get(key);
            sendAuthCommand("login " + str, "Успешная авторизация! §7(наведите для просмотра пароля)", str);
        } else if (this.modeSetting.isSelected(AutoAuthMode.CUSTOM)) {
            String text = this.customValueSetting.getText();
            sendAuthCommand("login " + text, "Успешная авторизация! §7(наведите для просмотра пароля)", text);
        }
    }

    public String generatePassword() {
        if (this.modeSetting.currentValue() == AutoAuthMode.RANDOM) {
            return generateRandomString(12);
        } else if (this.modeSetting.currentValue() == AutoAuthMode.CUSTOM) {
            return getCustomPassword();
        }
        return null;
    }

    public String getCustomPassword() {
        String text = this.customValueSetting.getText();
        if (text.isEmpty()) {
            return null;
        }
        return text;
    }

    public String generateRandomString(int i) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i2 = 0; i2 < i; i2++) {
            sb.append("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".charAt(random.nextInt("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".length())));
        }
        return sb.toString();
    }

    public void sendAuthCommand(String str, String str2, String str3) {
        Mc.INSTANCE.getNetworkHandler().sendCommand(str);
        ChatUtil.addChatMessage(Component.literal(str2 + ": ").append(Component.literal("******").withStyle(style -> {
            return style.withHoverEvent(new HoverEvent.ShowText(Component.literal("§7" + Lang.AUTOAUTH_CUSTOM_VALUE.effective() + ": §f" + str3)));
        })));
    }

    public void saveCredentials() {
        try {
            Raven.INSTANCE.configManager().saveAutoAuth(this.credentials);
        } catch (IOException e) {
            Raven.LOGGER.error("Failed to save credentials", e);
        }
    }

    public CredentialKey currentCredentialKey() {
        ServerData currentServer = Mc.INSTANCE.getCurrentServer();
        return new CredentialKey(currentServer != null ? currentServer.ip : "local", Mc.INSTANCE.getSession().getName());
    }

    public void loadCredentials() {
        if (this.credentialsLoaded || Raven.INSTANCE.configManager() == null) {
            return;
        }
        try {
            this.credentials.clear();
            this.credentials.putAll(Raven.INSTANCE.configManager().loadAutoAuth());
            this.credentialsLoaded = true;
        } catch (IOException e) {
            Raven.LOGGER.error("Failed to load credentials", e);
        }
    }

    public Path getConfigPath() {
        return Raven.INSTANCE.configManager() != null ? Raven.INSTANCE.configManager().autoAuthConfig().path() : this.defaultConfigPath;
    }

    private static void openDirectory(Path directory) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    desktop.open(directory.toFile());
                    return;
                } catch (IOException | UnsupportedOperationException ignored) {
                }
            }
        }
        String platform = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String command = platform.contains("win") ? "explorer.exe"
                : platform.contains("linux") ? "xdg-open"
                : platform.contains("mac") ? "open" : null;
        if (command == null) throw new IOException("No file manager available for " + platform);
        new ProcessBuilder(command, directory.toString()).start();
    }
}
