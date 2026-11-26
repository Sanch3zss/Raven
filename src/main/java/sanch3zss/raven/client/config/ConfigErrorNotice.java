package sanch3zss.raven.client.config;

import java.net.URI;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

public final class ConfigErrorNotice {
    public static final String discordUrl = "https://discord.gg/gPQ7yBY9zy";

    public ConfigErrorNotice() {
    }

    public static Component withDiscord() {
        return withDiscord("сохранении");
    }

    public static Component withDiscord(String str) {
        return Component.literal("").append(Component.literal("Ошибка конфигурации ").withStyle(ChatFormatting.RED)).append(Component.literal("[Исправить]").setStyle(Component.literal("[Исправить]").getStyle().withColor(ChatFormatting.GREEN).withHoverEvent(new HoverEvent.ShowText(Component.literal("").append(Component.literal("Произошла ошибка при %s конфигурации.\n".formatted(str)).withStyle(ChatFormatting.GRAY)).append(Component.literal("Пожалуйста, отправьте latest.log файл\n").withStyle(ChatFormatting.GRAY)).append(Component.literal("администрации в Discord.\n\n").withStyle(ChatFormatting.GRAY)).append(Component.literal("Нажмите, чтобы перейти на сервер.").withStyle(ChatFormatting.YELLOW)))).withClickEvent(new ClickEvent.OpenUrl(URI.create(discordUrl)))));
    }
}
