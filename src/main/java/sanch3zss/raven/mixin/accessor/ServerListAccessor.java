package sanch3zss.raven.mixin.accessor;

import java.util.List;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ServerList.class})
public interface ServerListAccessor {
    @Accessor("serverList")
    List<ServerData> raven$getServers();
}
