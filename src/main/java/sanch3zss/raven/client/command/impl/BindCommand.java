package sanch3zss.raven.client.command.impl;

import java.util.*;
import java.util.stream.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.command.*;
import sanch3zss.raven.client.command.argument.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.KeyboardUtil;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.BindMode;
import sanch3zss.raven.client.chat.ChatUtil;

public class BindCommand implements ClientCommand {
    public final ModuleArgumentParser moduleParser = new ModuleArgumentParser();

    public final KeyArgumentType keyParser = new KeyArgumentType();

    public final EnumArgumentType<BindMode> bindModeParser = ArgumentTypeFactory.enumType(BindMode.class);

    @Override
    public String getName() {
        return "bind";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_BIND_DESC;
    }

    @Override
    public String getUsage() {
        return ".bind <add | remove | list | clear> [arguments]";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public void execute(CommandContext commandContext) throws TranslatedException {
        String[] strArrArgs = commandContext.args();
        if (!Mc.INSTANCE.isWorldLoaded()) {
            System.err.println("ser vi che dalbaeb");
            return;
        }
        if (strArrArgs.length == 0) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_SUBCOMMAND_REQUIRED.effective().replace("{usage}", getUsage())));
        }
        String lowerCase = strArrArgs[0].toLowerCase();
        String[] strArr = (String[]) Arrays.copyOfRange(strArrArgs, 1, strArrArgs.length);
        switch (lowerCase) {
            case "add":
                addBind(commandContext, strArr);
                return;
            case "remove":
                removeBind(commandContext, strArr);
                return;
            case "list":
                listBinds();
                return;
            case "clear":
                clearBinds();
                return;
            default:
                throw new TranslatedException(Translation.clearText(Lang.COMMAND_UNKNOWN_SUBCOMMAND.effective().replace("{sub}", lowerCase)));
        }
    }

    public void removeBind(CommandContext commandContext, String[] strArr) throws TranslatedException {
        if (strArr.length < 1) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_NO_MODULE.effective()));
        }
        Module module = (Module) commandContext.getArgument(1, this.moduleParser);
        module.setBind(List.of(), BindMode.TOGGLE);
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BIND_REMOVED.effective().replace("{module}", ChatFormatting.RED + module.getName() + ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
    }

    public void listBinds() {
        List<Module> list = Raven.INSTANCE.moduleRepository().getModules().stream().filter(Module::hasKeyBind).toList();
        if (list.isEmpty()) {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BINDS_NOT_FOUND.effective()).withStyle(ChatFormatting.GRAY));
        } else {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BINDS_LIST.effective()).withStyle(ChatFormatting.GRAY));
            list.forEach(module -> {
                ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BIND_INFO.effective().replace("{name}", ChatFormatting.RED + module.getName().replaceAll("\\s", "") + ChatFormatting.GRAY).replace("{key}", ChatFormatting.RED + KeyboardUtil.keysToString(module.getKeyBind()).toUpperCase() + ChatFormatting.GRAY).replace("{type}", ChatFormatting.RED + module.getType().name() + ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
            });
        }
    }

    public void clearBinds() {
        Raven.INSTANCE.moduleRepository().getModules().forEach(module -> {
            module.setBind(List.of(), BindMode.TOGGLE);
        });
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BINDS_CLEARED.effective()).withStyle(ChatFormatting.GRAY));
    }

    public void addBind(CommandContext commandContext, String[] strArr) throws TranslatedException {
        if (strArr.length < 2) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_NOT_ENOUGH_ARGS.effective()));
        }
        Module module = (Module) commandContext.getArgument(1, this.moduleParser);
        int iIntValue = ((Integer) commandContext.getArgument(2, this.keyParser)).intValue();
        BindMode bindMode = BindMode.TOGGLE;
        if (strArr.length >= 3) {
            bindMode = (BindMode) commandContext.getArgument(3, this.bindModeParser);
        }
        module.setBind(List.of(iIntValue), bindMode);
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BIND_ADDED.effective().replace("{module}", ChatFormatting.RED + module.getName().replaceAll("\\s", "") + ChatFormatting.GRAY).replace("{key}", ChatFormatting.RED + KeyboardUtil.keyToString(iIntValue) + ChatFormatting.GRAY).replace("{type}", ChatFormatting.RED + bindMode.name() + ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        if (i == 0) {
            return Stream.of("add", "remove", "list", "clear").filter(str -> {
                return str.startsWith(strArr[0].toLowerCase());
            }).toList();
        }
        String lowerCase = strArr[0].toLowerCase();
        String str2 = i < strArr.length ? strArr[i] : "";
        switch (i) {
            case 1:
                if (lowerCase.equals("add")) {
                    return this.moduleParser.getSuggestions(str2);
                }
                if (lowerCase.equals("remove")) {
                    String lowerCase2 = str2.toLowerCase();
                    return Raven.INSTANCE.moduleRepository().getModules().stream().filter(Module::hasKeyBind).map(module -> {
                        return module.getName().replaceAll("\\s", "");
                    }).filter(str3 -> {
                        return str3.replaceAll("\\s", "").toLowerCase().startsWith(lowerCase2);
                    }).collect(Collectors.toList());
                }
                break;
            case 2:
                if (lowerCase.equals("add")) {
                    return this.keyParser.getSuggestions(str2);
                }
                break;
            case 3:
                if (lowerCase.equals("add")) {
                    return this.bindModeParser.getSuggestions(str2);
                }
                break;
        }
        return List.of();
    }
}
