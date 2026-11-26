package sanch3zss.raven.mixin.accessor;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ServerSelectionList.OnlineServerEntry.class})
public interface ServerEntryAccessor {
    @Accessor("serverData")
    ServerData raven$getServer();
}
