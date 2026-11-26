package sanch3zss.raven.client.command;

import java.util.List;

import sanch3zss.raven.client.chat.Translation;

public interface ClientCommand {
    String getName();

    Translation getDescription();

    String getUsage();

    List<String> getAliases();

    void execute(CommandContext commandContext);

    List<String> getSuggestions(String[] strArr, int i);
}
