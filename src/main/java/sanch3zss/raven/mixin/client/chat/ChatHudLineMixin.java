package sanch3zss.raven.mixin.client.chat;

import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.*;

import sanch3zss.raven.accessor.ChatLineIdAccessor;

@Mixin({GuiMessage.class})
public abstract class ChatHudLineMixin implements ChatLineIdAccessor {

    @Unique
    private String raven$id = null;

    @Override
    @Unique
    public void raven$setId(String str) {
        this.raven$id = str;
    }

    @Override
    @Unique
    public String raven$getId() {
        return this.raven$id;
    }
}
