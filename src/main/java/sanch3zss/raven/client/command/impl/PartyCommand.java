package sanch3zss.raven.client.command.impl;

import java.util.List;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.command.ClientCommand;
import sanch3zss.raven.client.command.CommandContext;
import sanch3zss.raven.client.chat.ChatUtil;

public class PartyCommand implements ClientCommand {
    @Override
    public String getName() {
        return "party";
    }

    @Override
    public Translation getDescription() {
        return Translation.clearText("Создать Party, войти по коду, выйти или посмотреть участников");
    }

    @Override
    public String getUsage() {
        return ".party <create | join <код> | leave | disband | info>";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public void execute(CommandContext context) {
        String[] args = context.args();
        if (args.length == 1 && args[0].equalsIgnoreCase("create")) {
            Raven.INSTANCE.partyService().create();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")
                && args[1].matches("[A-Za-z0-9]{6}")) {
            Raven.INSTANCE.partyService().join(args[1]);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("leave")) {
            Raven.INSTANCE.partyService().leave();
        } else if (args.length == 1 && args[0].equalsIgnoreCase("disband")) {
            Raven.INSTANCE.partyService().disband();
        } else if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            Raven.INSTANCE.partyService().info();
        } else {
            ChatUtil.addChatMessage("Использование: " + getUsage());
        }
    }

    @Override
    public List<String> getSuggestions(String[] args, int index) {
        if (index != 0) return List.of();
        String prefix = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ROOT);
        return List.of("create", "join", "leave", "disband", "info").stream()
                .filter(option -> option.startsWith(prefix)).toList();
    }
}
