package sanch3zss.raven.client.command.impl;


import sanch3zss.raven.feature.server.funtime.FunTimeServerHandler;
import sanch3zss.raven.feature.server.holyworld.HolyWorldServerHandler;
import sanch3zss.raven.feature.server.reallyworld.ReallyWorldServerHandler;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.command.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.service.ReconnectHandler;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.event.render.HandledScreenRenderEvent;
import sanch3zss.raven.feature.server.*;
import sanch3zss.raven.feature.world.*;

public class ReconnectCommand implements ClientCommand {
    public ReconnectTask activeTask;
    public final List<ReconnectHandler> handlers = new ArrayList();

    public final Mc mc = Mc.INSTANCE;

    public ReconnectCommand() {
        this.handlers.add(new FunTimeServerHandler());
        this.handlers.add(new HolyWorldServerHandler());
        this.handlers.add(new ReallyWorldServerHandler());
        Raven.INSTANCE.eventDispatcher().register(PlayerTickEvent.class, event -> {
            if (this.activeTask != null) {
                try {
                    this.activeTask.tick();
                } catch (ReconnectException e) {
                    ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_ERROR_PREFIX.effective().replace("{error}", e.getMessage())).withStyle(ChatFormatting.RED));
                }
                if (this.activeTask.isComplete()) {
                    this.activeTask = null;
                }
            }
        });
        Raven.INSTANCE.eventDispatcher().register(HandledScreenRenderEvent.class, event2 -> {
            if (this.activeTask != null) {
                try {
                    this.activeTask.handledScreenTick();
                } catch (ReconnectException e) {
                    ChatUtil.addChatMessage(Component.literal(Lang.COMMAND_ERROR_PREFIX.effective().replace("{error}", e.getMessage())).withStyle(ChatFormatting.RED));
                }
            }
        });
    }

    @Override
    public void execute(CommandContext commandContext) throws TranslatedException {
        if (!this.mc.isWorldLoaded()) {
            throw new TranslatedException(Lang.COMMAND_WORLD_NOT_LOADED);
        }
        if (this.mc.isSingleplayer()) {
            throw new TranslatedException(Lang.COMMAND_SINGLEPLAYER_ONLY);
        }
        for (ReconnectHandler reconnectHandler : this.handlers) {
            for (String serverId : reconnectHandler.getServerIds()) {
                if (ServerUtil.isConnectedToServer(serverId)) {
                    this.activeTask = reconnectHandler.createProcess();
                    return;
                }
            }
        }
        throw new TranslatedException(Lang.COMMAND_UNSUPPORTED_SERVER);
    }

    @Override
    public String getName() {
        return "rct";
    }

    @Override
    public Translation getDescription() {
        return Lang.COMMAND_RCT_DESC;
    }

    @Override
    public String getUsage() {
        return ".rct";
    }

    @Override
    public List<String> getAliases() {
        return List.of("reconnect");
    }

    @Override
    public List<String> getSuggestions(String[] strArr, int i) {
        return List.of();
    }
}
