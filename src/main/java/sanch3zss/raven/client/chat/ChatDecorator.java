package sanch3zss.raven.client.chat;

import net.minecraft.network.chat.Component;

@FunctionalInterface
public interface ChatDecorator {
    Component apply(Component text);
}
