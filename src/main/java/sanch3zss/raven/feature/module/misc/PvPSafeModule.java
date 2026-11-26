package sanch3zss.raven.feature.module.misc;

import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.*;
import net.minecraft.network.chat.Component;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.event.network.OutgoingChatEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.notification.NotificationType;

@Aliases(aliases = {"PvP Safe", "Command Blocker", "Safe Commands", "Anti Hub",
        "Block Commands", "Command Guard"})
public class PvPSafeModule extends Module {
    private static final long WARNING_COOLDOWN_MS = 3000L;
    private static final Pattern DANGEROUS_COMMAND = Pattern.compile(
            "^/(spawn|home|hub|lobby|rtp|warp|back|logout|server|suicide|kill|auction|trade|duel|pay|msg|tell|w)(\\s+.*)?$",
            Pattern.CASE_INSENSITIVE);
    private final Map<String, Long> warningTimes = new ConcurrentHashMap<>();

    public PvPSafeModule() {
        super(ModuleTab.MISC, "PvP Safe");
        register(OutgoingChatEvent.class, this::onOutgoingChat);
        register(ClientTickEvent.class, event -> {
            long now = System.currentTimeMillis();
            this.warningTimes.values().removeIf(time -> now - time >= WARNING_COOLDOWN_MS);
        });
    }

    private void onOutgoingChat(OutgoingChatEvent event) {
        if (!isState() || event.message() == null) {
            return;
        }
        Matcher matcher = DANGEROUS_COMMAND.matcher(event.message().trim());
        if (!matcher.matches()) {
            return;
        }
        event.cancel();
        String command = matcher.group(1).toLowerCase();
        long now = System.currentTimeMillis();
        Long previous = this.warningTimes.put(command, now);
        if (previous != null && now - previous < WARNING_COOLDOWN_MS) {
            return;
        }
        Raven.INSTANCE.notificationRepository().post(NotificationType.WARNING,
                Component.literal("PvP Safe заблокировал /" + command), 3L, TimeUnit.SECONDS);
    }
}
