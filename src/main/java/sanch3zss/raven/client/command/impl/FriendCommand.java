package sanch3zss.raven.client.command.impl;

import java.io.IOException;
import java.util.*;
import java.util.stream.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.command.*;
import sanch3zss.raven.client.config.ConfigErrorNotice;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.feature.friend.FriendManager;
import sanch3zss.raven.client.chat.ChatUtil;

public class FriendCommand implements ClientCommand {
    public static final Logger logger = LoggerFactory.getLogger(FriendCommand.class);

    @Override
    public String getName() {
        return "friend";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_FRIEND_DESC;
    }

    @Override
    public String getUsage() {
        return ".friend <add | remove | list | clear> [name]";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(CommandContext commandContext) throws TranslatedException {
        String[] strArrArgs = commandContext.args();
        if (strArrArgs.length == 0) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_SUBCOMMAND_REQUIRED.effective().replace("{usage}", getUsage())));
        }
        String lowerCase = strArrArgs[0].toLowerCase();
        String[] strArr = (String[]) Arrays.copyOfRange(strArrArgs, 1, strArrArgs.length);
        switch (lowerCase) {
            case "add":
                addFriend(strArr);
                return;
            case "remove":
                removeFriend(strArr);
                return;
            case "list":
                listFriends();
                return;
            case "clear":
                clearFriends();
                return;
            default:
                throw new TranslatedException(Translation.clearText(Lang.COMMAND_UNKNOWN_SUBCOMMAND.effective().replace("{sub}", lowerCase)));
        }
    }

    public void addFriend(String[] strArr) throws TranslatedException {
        if (strArr.length < 1) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_NOT_ENOUGH_ARGS.effective()));
        }
        String str = strArr[0];
        if (FriendManager.isFriend(str)) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_FRIEND_ALREADY.effective().replace("{name}", str)));
        }
        FriendManager.add(str);
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_FRIEND_ADDED.effective().replace("{name}", str)).withStyle(ChatFormatting.GRAY));
    }

    public void removeFriend(String[] strArr) throws TranslatedException {
        if (strArr.length < 1) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_NOT_ENOUGH_ARGS.effective()));
        }
        String str = strArr[0];
        if (!FriendManager.isFriend(str)) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_FRIEND_NOT_FOUND.effective().replace("{name}", str)));
        }
        FriendManager.remove(str);
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_FRIEND_REMOVED.effective().replace("{name}", str)).withStyle(ChatFormatting.GRAY));
    }

    public void listFriends() {
        Set<String> friends = FriendManager.getFriends();
        if (friends.isEmpty()) {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_FRIENDS_NOT_FOUND.effective()).withStyle(ChatFormatting.GRAY));
        } else {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_FRIENDS_LIST.effective().replace("{list}", (String) friends.stream().sorted().collect(Collectors.joining(", ")))).withStyle(ChatFormatting.GRAY));
        }
    }

    public void clearFriends() {
        FriendManager.clear();
        try {
            Raven.INSTANCE.configManager().saveFriends();
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_FRIENDS_CLEARED.effective()).withStyle(ChatFormatting.GRAY));
        } catch (IOException e) {
            logger.error("Failed to persist friends after clear", e);
            ChatUtil.addChatMessage(ConfigErrorNotice.withDiscord("сохранении списка друзей"));
        }
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        if (i == 0) {
            return Stream.of(new String[]{"add", "remove", "list", "clear"}).filter(str -> {
                return str.startsWith(strArr[0].toLowerCase());
            }).toList();
        }
        if (i == 1) {
            String lowerCase = strArr[0].toLowerCase();
            String str2 = i < strArr.length ? strArr[i] : "";
            if (lowerCase.equals("remove")) {
                String lowerCase2 = str2.toLowerCase();
                return (List) FriendManager.getFriends().stream().filter(str3 -> {
                    return str3.toLowerCase().startsWith(lowerCase2);
                }).sorted().collect(Collectors.toList());
            }
            if (lowerCase.equals("add")) {
                String lowerCase3 = str2.toLowerCase();
                Minecraft minecraftClient = Minecraft.getInstance();
                if (minecraftClient.player == null || minecraftClient.getConnection() == null) {
                    return List.of();
                }
                Set<String> friends = FriendManager.getFriends();
                return minecraftClient.getConnection().getOnlinePlayers().stream().map(playerListEntry -> {
                    return playerListEntry.getProfile().name();
                }).filter(str4 -> {
                    return str4.toLowerCase().startsWith(lowerCase3);
                }).filter(str5 -> {
                    return !friends.contains(str5);
                }).sorted().toList();
            }
        }
        return List.of();
    }
}
