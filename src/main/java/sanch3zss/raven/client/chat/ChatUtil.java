package sanch3zss.raven.client.chat;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import sanch3zss.raven.accessor.ChatLineIdAccessor;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.render.theme.ThemePalette;

public final class ChatUtil {
    public static void removeMessage(ChatComponent chatHud, String str) {
        if (!runOnClientThread(() -> removeMessage(chatHud, str))) {
            return;
        }
        chatHud.allMessages.removeIf(chatHudLine -> {
            return str.equals(((ChatLineIdAccessor) ChatLineIdAccessor.class.cast(chatHudLine)).raven$getId());
        });
        chatHud.trimmedMessages.removeIf(visible -> {
            return str.equals(((ChatLineIdAccessor) ChatLineIdAccessor.class.cast(visible)).raven$getId());
        });
    }

    public static void addMessage(ChatComponent chatHud, Component text, String str) {
        if (!runOnClientThread(() -> addMessage(chatHud, text, str))) {
            return;
        }
        GuiMessage chatHudLine = new GuiMessage(
                Mc.INSTANCE.getMinecraft().gui.hud.getGuiTicks(),
                text,
                (MessageSignature) null,
                GuiMessageSource.SYSTEM_CLIENT,
                Mc.INSTANCE.isSingleplayer() ? GuiMessageTag.systemSinglePlayer() : GuiMessageTag.system()
        );
        ((ChatLineIdAccessor) ChatLineIdAccessor.class.cast(chatHudLine)).raven$setId(str);
        chatHud.logChatMessage(chatHudLine);
        chatHud.addMessageToDisplayQueue(chatHudLine);
    }

    public static void addChatMessage(Component text) {
        if (!runOnClientThread(() -> addChatMessage(text))) {
            return;
        }
        MutableComponent mutableTextAppend = buildPrefix().copy().append(text);
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player != null) {
            player.sendSystemMessage(mutableTextAppend);
        }
    }

    public static Component gradientText(String text, int firstColor, int secondColor) {
        MutableComponent result = Component.literal("");
        int last = Math.max(1, text.length() - 1);
        for (int index = 0; index < text.length(); index++) {
            int color = ColorUtil.interpolateOklab(firstColor, secondColor,
                    index / (float) last) & 0xFFFFFF;
            result.append(Component.literal(String.valueOf(text.charAt(index)))
                    .withStyle(style -> style.withColor(color)));
        }
        return result;
    }

    public static void addChatMessage(String str, boolean z) {
        if (!runOnClientThread(() -> addChatMessage(str, z))) {
            return;
        }
        MutableComponent mutableTextAppend = buildPrefix().copy().append(Component.literal(str).withStyle(style -> {
            return style.withColor(ChatFormatting.WHITE);
        }));
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player != null) {
            if (z) {
                player.sendOverlayMessage(mutableTextAppend);
            } else {
                player.sendSystemMessage(mutableTextAppend);
            }
        }
    }

    public static void addChatMessage(String str) {
        addChatMessage(str, false);
    }

    public static void addChatMessageWithPassword(String str, String str2) {
        if (!runOnClientThread(() -> addChatMessageWithPassword(str, str2))) {
            return;
        }
        MutableComponent mutableTextAppend = buildPrefix().copy().append(Component.literal(str + ": ").withStyle(style -> {
            return style.withColor(ChatFormatting.WHITE);
        }));
        mutableTextAppend.append(Component.literal("******").withStyle(style2 -> {
            return style2.withHoverEvent(new HoverEvent.ShowText(Component.literal(ChatFormatting.GRAY + Lang.CHAT_PASSWORD.effective().replace("{password}", str2)))).withColor(TextColor.fromLegacyFormat(ChatFormatting.RED));
        }));
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player != null) {
            player.sendSystemMessage(mutableTextAppend);
        }
    }

    public static Component buildPrefix() {
        ThemePalette palette = Raven.INSTANCE.theme().palette();
        return Component.literal("").append(gradientText("Raven Visuals",
                palette.accentStart().argb(), palette.gradientEnd().argb())).append(Component.literal(" -> ").withStyle(style -> {
            return style.withColor(ChatFormatting.DARK_GRAY);
        }));
    }

    private static boolean runOnClientThread(Runnable runnable) {
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient.isSameThread()) {
            return true;
        }
        minecraftClient.execute(runnable);
        return false;
    }

    public ChatUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
