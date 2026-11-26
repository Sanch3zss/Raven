package sanch3zss.raven.feature.friend;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.slf4j.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.ConfigErrorNotice;
import sanch3zss.raven.client.chat.ChatUtil;

public class FriendManager {
    public static final Set<FriendEntry> friends = new HashSet<>();
    public static final Logger logger = LoggerFactory.getLogger(FriendManager.class);

    public static boolean isFriend(String str) {
        return friends.stream().anyMatch(friend -> friend.name().equals(str));
    }

    public static void add(String str) {
        friends.add(new FriendEntry(str));
        save("сохранении списка друзей");
    }

    public static void load(String name, String alias) {
        friends.add(new FriendEntry(name, alias));
    }

    public static void remove(String str) {
        if (isFriend(str)) {
            friends.remove(new FriendEntry(str));
            save("удалении друга");
        }
    }

    public static void setAlias(String name, String alias) {
        if (!isFriend(name)) {
            return;
        }
        friends.remove(new FriendEntry(name));
        friends.add(new FriendEntry(name, alias));
        save("сохранении ника друга");
    }

    public static void clear() {
        friends.clear();
    }

    public static Set<String> getFriends() {
        return friends.stream().map(FriendEntry::name).collect(Collectors.toSet());
    }

    public static List<FriendEntry> entries() {
        return friends.stream().sorted(Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT))).toList();
    }

    public static String applyAliases(String text) {
        String result = text;
        for (FriendEntry entry : friends) {
            if (entry.alias() != null && result.contains(entry.name())) {
                result = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(entry.name()) + "(?![A-Za-z0-9_])")
                        .matcher(result).replaceAll(Matcher.quoteReplacement(entry.alias()));
            }
        }
        return result;
    }

    public static Component applyAliases(Component component) {
        if (component == null || friends.stream().noneMatch(entry -> entry.alias() != null)) {
            return component;
        }
        String plain = component.getString();
        if (applyAliases(plain).equals(plain)) {
            return component;
        }
        MutableComponent result = Component.empty();
        component.visit((style, text) -> {
            result.append(Component.literal(applyAliases(text)).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static void save(String action) {
        try {
            Raven.INSTANCE.configManager().saveFriends();
        } catch (IOException e) {
            logger.error("Failed to persist friends while {}", action, e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord(action));
        }
    }
}
