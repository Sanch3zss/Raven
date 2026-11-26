package sanch3zss.raven.feature.module.misc;


import sanch3zss.raven.client.chat.ChatLimitType;
import sanch3zss.raven.client.chat.ChatMessageType;
import sanch3zss.raven.client.chat.ChatUtil;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringDecomposer;
import org.apache.commons.lang3.StringUtils;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.ChatStackEntry;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.network.*;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.misc.BetterChatOption;
import sanch3zss.raven.feature.world.*;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.MultiSelectSetting;
import sanch3zss.raven.util.math.interpolation.TimestampedCounter;

@Aliases(aliases = {"Better Chat", "Chat History", "Anti Spam", "Infinite Chat"})
public class BetterChatModule extends Module {
    public static final String externalSuffix = "-external";
    public final MultiSelectSetting<BetterChatOption> chatImprovements;
    public final Map<String, TimestampedCounter> messageCounters;
    public final long spamWindowMs = 10000;

    public BetterChatModule() {
        super(ModuleTab.MISC, "BetterChat");
        this.chatImprovements = new MultiSelectSetting(Lang.BETTERCHAT_CHAT_IMPROVEMENTS).values(BetterChatOption.class);
        this.messageCounters = new HashMap();
        addSettings(this.chatImprovements);
        register(ChatReceiveEvent.class, this::onChatReceive);
        register(ChatLimitEvent.class, this::onChatLimit);
    }

    public void onChatReceive(ChatReceiveEvent event) {
        Mc mc = Mc.INSTANCE;
        if (isState() && mc.isWorldLoaded() && event.type() != ChatMessageType.DISGUISED_CHAT_MESSAGE && this.chatImprovements.isSelected(BetterChatOption.ANTI_SPAM)) {
            net.minecraft.client.gui.Hud inGameHud = mc.getInGameHud();
            event.cancel();
            String str = StringDecomposer.getPlainText(event.textData()) + "-external";
            ChatComponent chatHud = inGameHud.getChat();
            long jCurrentTimeMillis = System.currentTimeMillis();
            pruneExpiredCounters(jCurrentTimeMillis);
            incrementCounter(str, jCurrentTimeMillis);
            int i = this.messageCounters.get(str).count;
            MutableComponent mutableTextLiteral = Component.literal("");
            mutableTextLiteral.append(event.applyChatDecoration().apply(event.textData()));
            if (i > 1) {
                mutableTextLiteral.append(Component.literal(" ").withStyle(ChatFormatting.GRAY).append("[x" + i + "]"));
            }
            ChatStackEntry chatStackEntry = new ChatStackEntry(false, str, true, i);
            if (chatStackEntry.remove() && StringUtils.isNotEmpty(chatStackEntry.id())) {
                ChatUtil.removeMessage(chatHud, chatStackEntry.id());
            }
            ChatUtil.addMessage(chatHud, mutableTextLiteral, chatStackEntry.id());
        }
    }

    public void pruneExpiredCounters(long j) {
        this.messageCounters.entrySet().removeIf(entry -> {
            return j - ((TimestampedCounter) entry.getValue()).timestamp > 10000;
        });
    }

    public void incrementCounter(String str, long j) {
        this.messageCounters.compute(str, (str2, value) -> {
            if (value == null) {
                return new TimestampedCounter(1, j);
            }
            value.count++;
            value.timestamp = j;
            return value;
        });
    }

    public void onChatLimit(ChatLimitEvent event2) {
        if (isState() && Mc.INSTANCE.isWorldLoaded() && event2 != null) {
            ChatLimitType type = event2.getType();
            if (type != null && ((type.isHistory() && this.chatImprovements.isSelected(BetterChatOption.ANTI_CLEAR)) || (type.isLimit() && this.chatImprovements.isSelected(BetterChatOption.INFINITY)))) {
                event2.cancel();
            }
        }
    }
}
