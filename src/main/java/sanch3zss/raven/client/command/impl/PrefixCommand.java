package sanch3zss.raven.client.command.impl;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.command.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.chat.ChatUtil;

public class PrefixCommand implements ClientCommand {
    @Override
    public String getName() {
        return "prefix";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_PREFIX_DESC;
    }

    @Override
    public String getUsage() {
        return Raven.INSTANCE.commandDispatcher().getPrefix() + "prefix <символ>";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public void execute(CommandContext commandContext) throws TranslatedException {
        String[] strArrArgs = commandContext.args();
        if (strArrArgs.length == 0) {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_PREFIX_CURRENT.effective().replace("{prefix}", ChatFormatting.RED + Raven.INSTANCE.commandDispatcher().getPrefix() + ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
            return;
        }
        String str = strArrArgs[0];
        if (str.isBlank() || str.length() > 1) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_PREFIX_INVALID.effective()));
        }
        String prefix = Raven.INSTANCE.commandDispatcher().getPrefix();
        Raven.INSTANCE.commandDispatcher().setPrefix(str);
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_PREFIX_SET.effective().replace("{old}", ChatFormatting.RED + prefix + ChatFormatting.GRAY).replace("{new}", ChatFormatting.RED + str + ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        return List.of();
    }
}
