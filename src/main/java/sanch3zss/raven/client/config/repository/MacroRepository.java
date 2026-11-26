package sanch3zss.raven.client.config.repository;

import java.io.IOException;
import java.util.*;

import org.slf4j.*;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.config.ConfigErrorNotice;
import sanch3zss.raven.client.input.KeyPressState;
import sanch3zss.raven.event.client.KeyInputEvent;
import sanch3zss.raven.feature.macro.MacroEntry;
import sanch3zss.raven.client.chat.ChatUtil;

public class MacroRepository {
    public final List<MacroEntry> macroList = new ArrayList<>();
    public static final Logger logger = LoggerFactory.getLogger(MacroRepository.class);

    public MacroRepository() {
        Raven.INSTANCE.eventDispatcher().register(KeyInputEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded() && !this.macroList.isEmpty() && event.action() == KeyPressState.PRESS) {
                int iKey = event.key();
                this.macroList.stream().filter(macro -> macro.enabled() && macro.key() == iKey).findFirst()
                        .ifPresent(this::run);
            }
        });
    }

    public void run(MacroEntry macro) {
        if (!Mc.INSTANCE.isWorldLoaded()) {
            return;
        }
        try {
            if (macro.content().startsWith("/")) {
                Mc.INSTANCE.getNetworkHandler().sendCommand(macro.content().substring(1));
            } else {
                Mc.INSTANCE.getNetworkHandler().sendChat(macro.content());
            }
        } catch (Exception e) {
            logger.error("Failed to send macro command {}", macro.name(), e);
            ChatUtil.addChatMessage("Ошибка при отправке команды. См. latest.log");
        }
    }

    public void setKey(String name, int key) {
        for (int i = 0; i < this.macroList.size(); i++) {
            MacroEntry entry = this.macroList.get(i);
            if (entry.name().equalsIgnoreCase(name)) {
                this.macroList.set(i, new MacroEntry(entry.name(), entry.content(), key, entry.disabled));
                try {
                    Raven.INSTANCE.configManager().saveMacros();
                } catch (IOException e) {
                    logger.error("Failed to persist macros after setKey(name={})", name, e);
                    ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("сохранении макроса"));
                }
                return;
            }
        }
    }

    public void setEnabled(String name, boolean enabled) {
        for (int i = 0; i < this.macroList.size(); i++) {
            MacroEntry entry = this.macroList.get(i);
            if (entry.name().equalsIgnoreCase(name)) {
                this.macroList.set(i, new MacroEntry(entry.name(), entry.content(), entry.key(), !enabled));
                try {
                    Raven.INSTANCE.configManager().saveMacros();
                } catch (IOException e) {
                    logger.error("Failed to persist macros after setEnabled(name={})", name, e);
                    ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("сохранении макроса"));
                }
                return;
            }
        }
    }

    public boolean isEmpty() {
        return this.macroList.isEmpty();
    }

    public void addMacro(String str, String str2, int i) {
        this.macroList.add(new MacroEntry(str, str2, i));
        try {
            Raven.INSTANCE.configManager().saveMacros();
        } catch (IOException e) {
            logger.error("Failed to persist macros after addMacro(name={})", str, e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("сохранении макросов"));
        }
    }

    public boolean hasMacro(String str) {
        for (MacroEntry entry : this.macroList) {
            if (entry.name().equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    public void deleteMacro(String str) {
        if (this.macroList.removeIf(macro -> macro.name().equalsIgnoreCase(str))) {
            try {
                Raven.INSTANCE.configManager().saveMacros();
            } catch (IOException e) {
                logger.error("Failed to persist macros after deleteMacro(name={})", str, e);
                ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("удалении макроса"));
            }
        }
    }

    public void clearList() {
        if (this.macroList.isEmpty()) {
            return;
        }
        this.macroList.clear();
        try {
            Raven.INSTANCE.configManager().saveMacros();
        } catch (IOException e) {
            logger.error("Failed to persist macros after clearList()", e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("очистке макросов"));
        }
    }

    public List<MacroEntry> getMacroList() {
        return this.macroList;
    }
}
