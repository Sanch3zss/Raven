package sanch3zss.raven.client.command.impl;

import java.util.*;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.command.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.feature.world.BoxEspManager;
import sanch3zss.raven.client.chat.ChatUtil;

public class BoxCommand implements ClientCommand {
    public static List<String> blockNameCache;
    public static List<String> entityNameCache;
    public static final List<String> defaultColors = List.of("FF0000", "00FF00", "0000FF", "FFFF00", "FF00FF", "00FFFF", "FFFFFF", "FFA500", "FF4500", "9400D3");

    public static List<String> getBlockNames() {
        if (blockNameCache == null) {
            blockNameCache = BuiltInRegistries.BLOCK.stream().filter(block -> {
                return block != Blocks.AIR;
            }).map(block2 -> {
                return BuiltInRegistries.BLOCK.getKey(block2).getPath();
            }).sorted().toList();
        }
        return blockNameCache;
    }

    public static List<String> getEntityNames() {
        if (entityNameCache == null) {
            entityNameCache = BuiltInRegistries.ENTITY_TYPE.stream().map(entityType -> {
                return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath();
            }).sorted().toList();
        }
        return entityNameCache;
    }

    @Override
    public String getName() {
        return "box";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_BOX_DESC;
    }

    @Override
    public String getUsage() {
        return ".box <add|remove|list|clear> <block|entity> <name> [#AARRGGBB]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("boxesp");
    }

    @Override
    public void execute(CommandContext commandContext) {
        String[] strArrArgs = commandContext.args();
        if (strArrArgs.length == 0) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_SUBCOMMAND_REQUIRED.effective().replace("{usage}", getUsage())));
        }
        BoxEspManager boxEspManager = BoxEspManager.INSTANCE;
        switch (strArrArgs[0].toLowerCase()) {
            case "add":
                handleAdd(boxEspManager, strArrArgs);
                return;
            case "remove":
                handleRemove(boxEspManager, strArrArgs);
                return;
            case "list":
                handleList(boxEspManager);
                return;
            case "clear":
                handleClear(boxEspManager);
                return;
            default:
                throw new TranslatedException(Translation.clearText(Lang.COMMAND_UNKNOWN_SUBCOMMAND.effective().replace("{sub}", strArrArgs[0])));
        }
    }

    public void handleAdd(BoxEspManager boxEspManager, String[] strArr) {
        requireArgs(strArr, 3);
        String str = strArr[2];
        int color = strArr.length >= 4 ? parseColor(strArr[3]) : 0;
        switch (strArr[1].toLowerCase()) {
            case "block":
                Block blockOrElseThrow = resolveBlock(str).orElseThrow(() -> notFoundException(Lang.COMMAND_BOX_BLOCK_NOT_FOUND.effective(), str));
                boxEspManager.getBlocks().put(blockOrElseThrow, color);
                ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BOX_ADDED.effective().replace("{type}", ChatFormatting.RED + "block" + ChatFormatting.GRAY).replace("{name}", ChatFormatting.RED + blockOrElseThrow.getName().getString() + ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
                return;
            case "entity":
                EntityType<?> entityTypeOrElseThrow = resolveEntity(str).orElseThrow(() -> notFoundException(Lang.COMMAND_BOX_ENTITY_NOT_FOUND.effective(), str));
                boxEspManager.getEntities().put(entityTypeOrElseThrow, color);
                ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BOX_ADDED.effective().replace("{type}", ChatFormatting.RED + "entity" + ChatFormatting.GRAY).replace("{name}", ChatFormatting.RED + entityTypeOrElseThrow.getDescription().getString() + ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
                return;
            default:
                throw new TranslatedException(Translation.clearText(Lang.COMMAND_BOX_INVALID_TYPE.effective()));
        }
    }

    public void handleRemove(BoxEspManager boxEspManager, String[] strArr) {
        requireArgs(strArr, 3);
        String str = strArr[2];
        switch (strArr[1].toLowerCase()) {
            case "block":
                Block blockOrElseThrow = resolveBlock(str).orElseThrow(() -> {
                    return notFoundException(Lang.COMMAND_BOX_BLOCK_NOT_FOUND.effective(), str);
                });
                if (boxEspManager.getBlocks().remove(blockOrElseThrow) == null) {
                    throw new TranslatedException(Translation.clearText(Lang.COMMAND_BOX_NOT_TRACKED.effective().replace("{name}", blockOrElseThrow.getName().getString())));
                }
                ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BOX_REMOVED.effective().replace("{name}", ChatFormatting.RED + blockOrElseThrow.getName().getString() + ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
                return;
            case "entity":
                EntityType<?> entityTypeOrElseThrow = resolveEntity(str).orElseThrow(() -> {
                    return notFoundException(Lang.COMMAND_BOX_ENTITY_NOT_FOUND.effective(), str);
                });
                if (boxEspManager.getEntities().remove(entityTypeOrElseThrow) == null) {
                    throw new TranslatedException(Translation.clearText(Lang.COMMAND_BOX_NOT_TRACKED.effective().replace("{name}", entityTypeOrElseThrow.getDescription().getString())));
                }
                ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BOX_REMOVED.effective().replace("{name}", ChatFormatting.RED + entityTypeOrElseThrow.getDescription().getString() + ChatFormatting.GRAY)).withStyle(ChatFormatting.GRAY));
                return;
            default:
                throw new TranslatedException(Translation.clearText(Lang.COMMAND_BOX_INVALID_TYPE.effective()));
        }
    }

    public void handleList(BoxEspManager boxEspManager) {
        if (boxEspManager.getBlocks().isEmpty() && boxEspManager.getEntities().isEmpty()) {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BOX_LIST_EMPTY.effective()).withStyle(ChatFormatting.GRAY));
            return;
        }
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BOX_LIST_HEADER.effective()).withStyle(ChatFormatting.GRAY));
        boxEspManager.getBlocks().forEach((block, num) -> {
            ChatUtil.addChatMessage(Component.literal("  " + ChatFormatting.YELLOW + "block " + ChatFormatting.RED + block.getName().getString() + (num.intValue() != 0 ? ChatFormatting.GRAY + " #" + String.format("%08X", num) : "")));
        });
        boxEspManager.getEntities().forEach((entityType, num2) -> {
            ChatUtil.addChatMessage(Component.literal("  " + ChatFormatting.YELLOW + "entity " + ChatFormatting.RED + entityType.getDescription().getString() + (num2.intValue() != 0 ? ChatFormatting.GRAY + " #" + String.format("%08X", num2) : "")));
        });
    }

    public void handleClear(BoxEspManager boxEspManager) {
        boxEspManager.clearAll();
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_BOX_CLEARED.effective()).withStyle(ChatFormatting.GRAY));
    }

    public static Optional<Block> resolveBlock(String str) {
        Block block;
        Identifier identifierTryParse = Identifier.tryParse(str.contains(":") ? str : "minecraft:" + str);
        if (identifierTryParse != null && BuiltInRegistries.BLOCK.containsKey(identifierTryParse) && (block = (Block) BuiltInRegistries.BLOCK.getValue(identifierTryParse)) != Blocks.AIR) {
            return Optional.of(block);
        }
        String strReplace = str.toLowerCase().replace("_", " ");
        return BuiltInRegistries.BLOCK.stream().filter(block2 -> {
            return block2 != Blocks.AIR;
        }).filter(block3 -> {
            String lowerCase = block3.getName().getString().toLowerCase();
            return lowerCase.equals(strReplace) || lowerCase.replace(" ", "_").equals(str.toLowerCase());
        }).findFirst();
    }

    public static Optional<EntityType<?>> resolveEntity(String str) {
        Identifier identifierTryParse = Identifier.tryParse(str.contains(":") ? str : "minecraft:" + str);
        if (identifierTryParse != null && BuiltInRegistries.ENTITY_TYPE.containsKey(identifierTryParse)) {
            return Optional.of((EntityType) BuiltInRegistries.ENTITY_TYPE.getValue(identifierTryParse));
        }
        String strReplace = str.toLowerCase().replace("_", " ");
        return BuiltInRegistries.ENTITY_TYPE.stream().filter(entityType -> {
            String lowerCase = entityType.getDescription().getString().toLowerCase();
            return lowerCase.equals(strReplace) || lowerCase.replace(" ", "_").equals(str.toLowerCase());
        }).findFirst();
    }

    public static int parseColor(String str) throws TranslatedException {
        try {
            String strSubstring = str.startsWith("#") ? str.substring(1) : str;
            long j = Long.parseLong(strSubstring, 16);
            if (strSubstring.length() <= 6) {
                j |= 4278190080L;
            }
            return (int) j;
        } catch (NumberFormatException e) {
            throw new TranslatedException(Translation.clearText("Invalid color '" + str + "'. Use hex like FF0000 or FFFF0000"));
        }
    }

    public static List<String> filterSuggestions(List<String> list, String str, int i) {
        if (str.isEmpty()) {
            return list.size() <= i ? list : list.subList(0, i);
        }
        int i2 = 0;
        int size = list.size();
        while (i2 < size) {
            int i3 = (i2 + size) >>> 1;
            if (list.get(i3).compareTo(str) < 0) {
                i2 = i3 + 1;
            } else {
                size = i3;
            }
        }
        ArrayList arrayList = new ArrayList();
        for (int i4 = i2; i4 < list.size() && arrayList.size() < i; i4++) {
            String str2 = list.get(i4);
            if (!str2.startsWith(str)) {
                break;
            }
            arrayList.add(str2);
        }
        return arrayList;
    }

    public static TranslatedException notFoundException(String str, String str2) {
        return new TranslatedException(Translation.clearText(str.replace("{name}", str2)));
    }

    public static void requireArgs(String[] strArr, int i) throws TranslatedException {
        if (strArr.length < i) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_NOT_ENOUGH_ARGS.effective()));
        }
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        String lowerCase = i < strArr.length ? strArr[i].toLowerCase() : "";
        String lowerCase2 = strArr.length > 0 ? strArr[0].toLowerCase() : "";
        if (i == 0) {
            return Stream.of(new String[]{"add", "remove", "list", "clear"}).filter(str -> {
                return str.startsWith(lowerCase);
            }).toList();
        }
        if (i == 1 && (lowerCase2.equals("add") || lowerCase2.equals("remove"))) {
            return Stream.of(new String[]{"block", "entity"}).filter(str2 -> {
                return str2.startsWith(lowerCase);
            }).toList();
        }
        String lowerCase3 = strArr.length > 1 ? strArr[1].toLowerCase() : "";
        if (i == 2) {
            if (lowerCase2.equals("add")) {
                if (lowerCase3.equals("block")) {
                    return filterSuggestions(getBlockNames(), lowerCase, 30);
                }
                if (lowerCase3.equals("entity")) {
                    return filterSuggestions(getEntityNames(), lowerCase, 30);
                }
            }
            if (lowerCase2.equals("remove")) {
                BoxEspManager boxEspManager = BoxEspManager.INSTANCE;
                if (lowerCase3.equals("block")) {
                    return boxEspManager.getBlocks().keySet().stream().map(block -> {
                        return BuiltInRegistries.BLOCK.getKey(block).getPath();
                    }).filter(str3 -> {
                        return str3.startsWith(lowerCase);
                    }).sorted().toList();
                }
                if (lowerCase3.equals("entity")) {
                    return boxEspManager.getEntities().keySet().stream().map(entityType -> {
                        return BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath();
                    }).filter(str4 -> {
                        return str4.startsWith(lowerCase);
                    }).sorted().toList();
                }
            }
        }
        return (i == 3 && lowerCase2.equals("add")) ? defaultColors.stream().filter(str5 -> {
            return str5.toLowerCase().startsWith(lowerCase);
        }).toList() : List.of();
    }
}
