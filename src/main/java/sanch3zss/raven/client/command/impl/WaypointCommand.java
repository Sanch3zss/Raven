package sanch3zss.raven.client.command.impl;

import java.util.*;
import java.util.stream.Collectors;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.joml.Vector3i;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.command.*;
import sanch3zss.raven.client.config.repository.WaysRepository;
import sanch3zss.raven.client.config.storage.Waypoint;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.chat.ChatUtil;
import sanch3zss.raven.feature.waypoint.WaypointRenderer;

public class WaypointCommand implements ClientCommand {
    public WaypointCommand() {
        new WaypointRenderer();
    }

    @Override
    public String getName() {
        return "waypoint";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_WAY_DESC;
    }

    @Override
    public String getUsage() {
        return ".waypoint <add | remove | list | clear> [args]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("way", "waypoint");
    }

    @Override
    public void execute(CommandContext commandContext) throws TranslatedException {
        String[] strArrArgs = commandContext.args();
        if (strArrArgs.length == 0) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_SUBCOMMAND_REQUIRED.effective().replace("{usage}", getUsage())));
        }
        switch (strArrArgs[0].toLowerCase()) {
            case "add":
                addWaypoint(strArrArgs);
                return;
            case "remove":
                removeWaypoint(strArrArgs);
                return;
            case "list":
                listWaypoints();
                return;
            case "clear":
                clearWaypoints();
                return;
            default:
                throw new TranslatedException(Translation.clearText(Lang.COMMAND_UNKNOWN_SUBCOMMAND.effective().replace("{sub}", strArrArgs[0])));
        }
    }

    public void addWaypoint(String[] strArr) throws TranslatedException {
        WaysRepository waysRepository = Raven.INSTANCE.waysRepository();
        String[] args = Arrays.stream(strArr).filter(arg -> !arg.isEmpty()).toArray(String[]::new);
        if (args.length < 5) {
            throw new TranslatedException(Lang.COMMAND_INVALID_ARG_COUNT);
        }
        String str = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 3));
        try {
            int i = Integer.parseInt(args[args.length - 3]);
            int i2 = Integer.parseInt(args[args.length - 2]);
            int i3 = Integer.parseInt(args[args.length - 1]);
            waysRepository.getWaypoints().removeIf(waypoint -> {
                return waypoint.name().equalsIgnoreCase(str) || waypoint.vector().equals(new Vector3i(i, i2, i3));
            });
            waysRepository.add(str, new Vector3i(i, i2, i3));
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_WAYPOINT_ADDED.effective().replace("{name}", str)).withStyle(ChatFormatting.GRAY));
        } catch (NumberFormatException e) {
            throw new TranslatedException(Lang.COMMAND_WAY_INVALID_COORDS);
        }
    }

    public void removeWaypoint(String[] strArr) throws TranslatedException {
        WaysRepository waysRepository = Raven.INSTANCE.waysRepository();
        String str = String.join(" ", Arrays.copyOfRange(strArr, 1, strArr.length)).trim().replaceAll("\\s+", " ");
        if (str.isEmpty()) {
            throw new TranslatedException(Lang.COMMAND_INVALID_ARG_COUNT);
        }
        if (!waysRepository.remove(str)) {
            throw new TranslatedException(Translation.clearText(Lang.COMMAND_WAYPOINT_NOT_FOUND.effective().replace("{name}", str)));
        }
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_WAYPOINT_REMOVED.effective().replace("{name}", str)).withStyle(ChatFormatting.DARK_GRAY));
    }

    public void listWaypoints() {
        WaysRepository waysRepository = Raven.INSTANCE.waysRepository();
        if (waysRepository.isEmpty()) {
            ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_WAYPOINTS_NOT_FOUND.effective()).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_WAYPOINTS_LIST.effective()).withStyle(ChatFormatting.DARK_GRAY));
        for (Waypoint waypoint : waysRepository.getWaypoints()) {
            ChatUtil.addChatMessage(Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY).append(Component.literal(waypoint.name()).setStyle(Style.EMPTY.withColor(ChatFormatting.WHITE).withClickEvent(new ClickEvent.SuggestCommand(".waypoint remove " + waypoint.name())).withHoverEvent(new HoverEvent.ShowText(Component.literal(Lang.COMMAND_WAYPOINT_COORDS.effective()).append(Component.literal("[" + waypoint.vector().x + ", " + waypoint.vector().y + ", " + waypoint.vector().z + "]").withStyle(ChatFormatting.RED)))))));
        }
    }

    public void clearWaypoints() {
        Raven.INSTANCE.waysRepository().clearList();
        ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_WAYPOINTS_CLEARED.effective()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        if (i == 0) {
            return List.of("add", "remove", "list", "clear");
        }
        String lowerCase = strArr[0].toLowerCase();
        if (lowerCase.equals("remove")) {
            String typed = String.join(" ", Arrays.copyOfRange(strArr, 1, Math.min(i + 1, strArr.length))).toLowerCase();
            String before = String.join(" ", Arrays.copyOfRange(strArr, 1, Math.min(i, strArr.length)));
            int skip = before.isEmpty() ? 0 : before.length() + 1;
            return Raven.INSTANCE.waysRepository().getWaypoints().stream().map(Waypoint::name)
                    .filter(name -> name.toLowerCase().startsWith(typed) && name.length() >= skip)
                    .map(name -> name.substring(skip))
                    .collect(Collectors.toList());
        }
        if (!lowerCase.equals("add")) {
            return List.of();
        }
        switch (i) {
            case 1:
                return List.of("<name>");
            case 2:
                return List.of("<x>");
            case 3:
                return List.of("<y>");
            case 4:
                return List.of("<z>");
            default:
                return List.of();
        }
    }
}
