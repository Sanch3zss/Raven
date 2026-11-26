package sanch3zss.raven.client.command;

import java.util.*;

public class CommandRegistry {
    public final Map<String, ClientCommand> commands = new LinkedHashMap<>();
    public final Map<String, String> aliases = new LinkedHashMap<>();

    public void register(ClientCommand command) {
        this.commands.put(command.getName().toLowerCase(), command);
        if (command.getAliases() == null || command.getAliases().isEmpty()) {
            return;
        }
        for (String alias : command.getAliases()) {
            this.aliases.put(alias.toLowerCase(), command.getName().toLowerCase());
        }
    }

    public void unregister(String str) {
        ClientCommand remove = this.commands.remove(str.toLowerCase());
        if (remove != null) {
            remove.getAliases().forEach(alias -> {
                this.aliases.remove(alias.toLowerCase());
            });
        }
    }

    public Optional<ClientCommand> getCommand(String str) {
        return Optional.ofNullable(this.commands.get(this.aliases.getOrDefault(str.toLowerCase(), str.toLowerCase())));
    }

    public Collection<ClientCommand> getAllCommands() {
        return Collections.unmodifiableCollection(this.commands.values());
    }

    public List<String> getCommandNames() {
        ArrayList arrayList = new ArrayList();
        arrayList.addAll(this.commands.keySet());
        arrayList.addAll(this.aliases.keySet());
        return arrayList;
    }
}
