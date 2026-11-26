package sanch3zss.raven.client.command.impl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.command.*;
import sanch3zss.raven.client.command.argument.KeyArgumentType;
import sanch3zss.raven.client.config.repository.MacroRepository;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.KeyboardUtil;
import sanch3zss.raven.feature.macro.MacroEntry;
import sanch3zss.raven.client.chat.ChatUtil;

public class MacroCommand implements ClientCommand {
    public final KeyArgumentType keyArgument = new KeyArgumentType();

    @Override
    public String getName() {
        return "macro";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_MACRO_DESC;
    }

    @Override
    public String getUsage() {
        return ".macro <add | remove | toggle | list | clear> [arguments]";
    }

    @Override
    public List<String> getAliases() {
        return null;
    }

    @Override
    public void execute(CommandContext commandContext) {
        String[] strArrArgs = commandContext.args();
        if (!Mc.INSTANCE.isWorldLoaded()) {
            System.err.println("ser vi che dalbaeb");
            return;
        }
        if (strArrArgs.length == 0) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_SUBCOMMAND_REQUIRED.effective().replace("{usage}", getUsage())));
        }
        MacroRepository macroRepository = Raven.INSTANCE.macroRepository();
        String lowerCase = strArrArgs[0].toLowerCase();
        String[] strArr = (String[]) Arrays.copyOfRange(strArrArgs, 1, strArrArgs.length);
        switch (lowerCase) {
            case "add":
                handleAdd(macroRepository, strArr);
                return;
            case "remove":
                handleRemove(macroRepository, strArr);
                return;
            case "toggle":
                handleToggle(macroRepository, strArr);
                return;
            case "list":
                handleList();
                return;
            case "clear":
                handleClear();
                return;
            default:
                throw new TranslatedException(Translation.clearText(Lang.COMMAND_UNKNOWN_SUBCOMMAND.effective().replace("{sub}", lowerCase)));
        }
    }

    public void handleAdd(MacroRepository macroRepo, String[] strArr) throws TranslatedException {
        if (strArr.length < 2) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_NOT_ENOUGH_ARGS.effective()));
        }
        int iIntValue = this.keyArgument.parse(strArr[0]).intValue();
        String str = strArr[1];
        String strCollectMessage = collectMessage(2, strArr);
        if (strCollectMessage.isEmpty()) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_MACRO_EMPTY_MESSAGE.effective()));
        }
        if (macroRepo.hasMacro(str)) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_MACRO_EXISTS.effective()));
        }
        macroRepo.addMacro(str, strCollectMessage, iIntValue);
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_MACRO_ADDED.effective().replace("{name}", str).replace("{key}", KeyboardUtil.keyToString(iIntValue)).replace("{command}", strCollectMessage)).withStyle(ChatFormatting.GRAY));
    }

    public void handleRemove(MacroRepository macroRepo, String[] strArr) {
        if (strArr.length < 1) {
            throw new TranslatedException(Lang.COMMAND_MACRO_NO_NAME);
        }
        MacroEntry orElseThrow = Raven.INSTANCE.macroRepository().getMacroList().stream().filter(macroEntry -> {
            return macroEntry.name().replaceAll("\\s", "").equalsIgnoreCase(strArr[0]);
        }).findFirst().orElseThrow(() -> {
            return new TranslatedException(Translation.clearText(Lang.COMMAND_MACRO_NOT_FOUND.effective().replace("{name}", strArr[0])));
        });
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_MACRO_REMOVED.effective().replace("{name}", orElseThrow.name())).withStyle(ChatFormatting.GRAY));
        macroRepo.deleteMacro(orElseThrow.name());
    }

    public void handleToggle(MacroRepository macroRepo, String[] strArr) {
        if (strArr.length < 1) {
            throw new TranslatedException(Lang.COMMAND_MACRO_NO_NAME);
        }
        MacroEntry macro = macroRepo.getMacroList().stream()
                .filter(entry -> entry.name().replaceAll("\\s", "").equalsIgnoreCase(strArr[0]))
                .findFirst()
                .orElseThrow(() -> new TranslatedException(Translation.clearText(
                        Lang.COMMAND_MACRO_NOT_FOUND.effective().replace("{name}", strArr[0]))));
        boolean enabled = !macro.enabled();
        macroRepo.setEnabled(macro.name(), enabled);
        Translation message = enabled ? Lang.COMMAND_MACRO_ENABLED : Lang.COMMAND_MACRO_DISABLED;
        ChatUtil.addChatMessage(Component.literal(message.effective().replace("{name}", macro.name()))
                .withStyle(ChatFormatting.GRAY));
    }

    public void handleList() {
        List<MacroEntry> macroList = Raven.INSTANCE.macroRepository().getMacroList();
        if (macroList.isEmpty()) {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_MACROS_NOT_FOUND.effective()).withStyle(ChatFormatting.GRAY));
            return;
        }
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_MACROS_LIST.effective()).withStyle(ChatFormatting.GRAY));
        for (MacroEntry macroEntry : macroList) {
            String strReplaceAll = macroEntry.name().replaceAll("\\s", "");
            String offMark = macroEntry.enabled() ? "" : Lang.COMMAND_MACRO_OFF_MARK.effective();
            ChatUtil.addChatMessage(Component.literal("").withStyle(ChatFormatting.GRAY).append(Component.literal(strReplaceAll + ChatFormatting.GRAY + offMark + Lang.COMMAND_MACRO_REMOVE_HINT.effective()).setStyle(Component.literal(strReplaceAll + ChatFormatting.GRAY + " (ÐÐ°Ð¶Ð¼Ð¸Ñ‚Ðµ, Ñ‡Ñ‚Ð¾Ð±Ñ‹ ÑƒÐ´Ð°Ð»Ð¸Ñ‚ÑŒ)").getStyle().withColor(ChatFormatting.WHITE).withHoverEvent(new HoverEvent.ShowText(Component.literal("").append(Component.literal(Lang.COMMAND_MACRO_HOVER_NAME.effective()).withStyle(ChatFormatting.GRAY)).append(Component.literal(strReplaceAll + "\n").withStyle(ChatFormatting.WHITE)).append(Component.literal(Lang.COMMAND_MACRO_HOVER_KEY.effective()).withStyle(ChatFormatting.GRAY)).append(Component.literal(KeyboardUtil.keyToString(macroEntry.key()).toUpperCase() + "\n").withStyle(ChatFormatting.WHITE)).append(Component.literal(Lang.COMMAND_MACRO_HOVER_COMMAND.effective()).withStyle(ChatFormatting.GRAY)).append(Component.literal(macroEntry.content()).withStyle(ChatFormatting.WHITE)))).withClickEvent(new ClickEvent.SuggestCommand(".macro remove " + strReplaceAll)))));
        }
    }

    public void handleClear() {
        Raven.INSTANCE.macroRepository().getMacroList().clear();
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BINDS_CLEARED.effective()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        if (i == 0) {
            return Stream.of(new String[]{"add", "remove", "toggle", "list", "clear"}).filter(str -> {
                return str.startsWith(strArr[0].toLowerCase());
            }).toList();
        }
        String lowerCase = strArr[0].toLowerCase();
        String str2 = i < strArr.length ? strArr[i] : "";
        switch (i) {
            case 1:
                if (lowerCase.equals("add")) {
                    return this.keyArgument.getSuggestions(str2);
                }
                if (lowerCase.equals("remove") || lowerCase.equals("toggle")) {
                    String lowerCase2 = str2.toLowerCase();
                    return (List) Raven.INSTANCE.macroRepository().getMacroList().stream().map(macroEntry -> {
                        return macroEntry.name().replaceAll("\\s", "");
                    }).filter(str3 -> {
                        return str3.replaceAll("\\s", "").toLowerCase().startsWith(lowerCase2);
                    }).collect(Collectors.toList());
                }
                break;
            case 2:
                if (lowerCase.equals("add")) {
                    return List.of("<name>");
                }
                break;
            case 3:
                if (lowerCase.equals("add")) {
                    return List.of("<command>");
                }
                break;
        }
        return List.of();
    }

    public String collectMessage(int i, String[] strArr) {
        return IntStream.range(i, strArr.length).mapToObj(index -> {
            return asString(index, strArr).orElse("");
        }).collect(Collectors.joining(" ")).trim();
    }

    public Optional<String> asString(int i, String[] strArr) {
        return Optional.ofNullable(parseArgument(i, String::valueOf, strArr));
    }

    public <T> T parseArgument(int i, Function<String, T> function, String[] strArr) {
        if (i >= strArr.length) {
            return null;
        }
        try {
            return function.apply(strArr[i]);
        } catch (Exception e) {
            return null;
        }
    }
}
