package sanch3zss.raven.client.command;

import java.util.*;

public class CommandResolver {
    public final CommandRegistry registry;

    public CommandResolver(CommandRegistry commandRegistry) {
        this.registry = commandRegistry;
    }

    public Optional<ClientCommand> resolve(String str) {
        if (str.isEmpty()) {
            return Optional.empty();
        }
        return this.registry.getCommand(str.split("\\s+")[0]);
    }

    public List<String> getSuggestions(String str) {
        if (str.isEmpty()) {
            return this.registry.getCommandNames().stream().distinct().sorted().toList();
        }
        String[] strArrSplit = str.split("\\s+", -1);
        if (strArrSplit.length == 1) {
            String lowerCase = strArrSplit[0].toLowerCase(Locale.ROOT);
            return this.registry.getCommandNames().stream()
                    .filter(name -> name.startsWith(lowerCase)).distinct().sorted().toList();
        }
        Optional<ClientCommand> command = this.registry.getCommand(strArrSplit[0]);
        if (!command.isPresent()) {
            return Collections.emptyList();
        }
        String[] strArr = (String[]) Arrays.copyOfRange(strArrSplit, 1, strArrSplit.length);
        String currentWord = strArr[strArr.length - 1].toLowerCase(Locale.ROOT);
        return command.get().getSuggestions(strArr, strArr.length - 1).stream()
                .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).startsWith(currentWord))
                .distinct().toList();
    }
}
