package sanch3zss.raven.client.command.impl;


import sanch3zss.raven.render.gl.StencilBufferUtil;
import sanch3zss.raven.render.theme.ColorUtil;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.command.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.chat.ChatUtil;public class HelpCommand implements ClientCommand {
    public final CommandManager commandManager;

    public HelpCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_HELP_DESC;
    }

    @Override
    public String getUsage() {
        return ".help";
    }

    @Override
    public List<String> getAliases() {
        return List.of("?");
    }

    @Override
    public void execute(CommandContext commandContext) {
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_HELP_AVAILABLE.effective()).withStyle(ChatFormatting.GRAY));
        this.commandManager.getAllCommands().stream().sorted(Comparator.comparing(ClientCommand::getName)).forEach(cmd -> {
            if (cmd == this) {
                return;
            }
            String str = "." + cmd.getName();
            ChatUtil.addChatMessage(Component.literal(str).setStyle(Style.EMPTY.withColor(ColorUtil.argb(StencilBufferUtil.STENCIL_MASK, 100, 100, 100)).withClickEvent(new ClickEvent.SuggestCommand(str)).withHoverEvent(new HoverEvent.ShowText(Component.literal(Lang.COMMAND_HELP_HOVER.effective().replace("{usage}", cmd.getDescription().effective() + "\n" + cmd.getUsage())).withStyle(ChatFormatting.GRAY)))));
        });
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        return List.of();
    }
}
