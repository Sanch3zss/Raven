package sanch3zss.raven.client.command;

import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.TranslatedException;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.network.PacketSendEvent;
import sanch3zss.raven.client.chat.ChatUtil;

public class CommandManager {
    public String prefix = ".";
    public final CommandRegistry repository = new CommandRegistry();

    public final CommandResolver resolver = new CommandResolver(this.repository);

    public CommandManager() {
        Raven.INSTANCE.eventDispatcher().register(PacketSendEvent.class, event -> {
            if (event.getPacket() instanceof ServerboundChatPacket packet) {
                String strChatMessage = packet.message();
                if (strChatMessage.startsWith(this.prefix)) {
                    event.cancel();
                    dispatch(strChatMessage.substring(this.prefix.length()));
                }
            }
        });
    }

    public void dispatch(String str) {
        if (str.trim().isEmpty()) {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_USAGE_GENERIC.effective()).withStyle(ChatFormatting.RED));
            return;
        }
        try {
            Optional<ClientCommand> optionalResolve = this.resolver.resolve(str);
            if (optionalResolve.isEmpty()) {
                ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_UNKNOWN_COMMAND.effective().replace("{command}", str.split("\\s+")[0])).withStyle(ChatFormatting.RED));
                return;
            }
            ClientCommand command = optionalResolve.get();
            String[] args = str.split("\\s+", -1);
            command.execute(new CommandContext(args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0]));
        } catch (TranslatedException e) {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_ERROR_PREFIX.effective().replace("{error}", e.getMessage())).withStyle(ChatFormatting.RED));
        } catch (Exception e2) {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_UNEXPECTED_ERROR.effective()).withStyle(ChatFormatting.RED));
            e2.printStackTrace();
        }
    }

    public void registerCommand(ClientCommand command) {
        this.repository.register(command);
    }

    public void unregisterCommand(String str) {
        this.repository.unregister(str);
    }

    public Collection<ClientCommand> getAllCommands() {
        return this.repository.getAllCommands();
    }

    public List<String> getSuggestions(String str) {
        return this.resolver.getSuggestions(str);
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(String str) {
        this.prefix = str;
    }

    public CommandRegistry getRepository() {
        return this.repository;
    }
}
