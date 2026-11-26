package sanch3zss.raven.mixin.client.network;


import sanch3zss.raven.feature.server.ScoreboardHelper;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.event.network.ChunkLoadEvent;
import sanch3zss.raven.event.player.BlockUpdateEvent;
import sanch3zss.raven.event.render.RenderOverlayEvent;
import sanch3zss.raven.feature.world.*;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerModule;
import sanch3zss.raven.event.render.RenderOverlayType;
import sanch3zss.raven.feature.world.BlockUpdateEntry;

@Mixin({ClientPacketListener.class})
public abstract class ClientPlayNetworkHandlerMixin {

    @Shadow
    private ClientLevel level;

    @Redirect(method = {"handleEntityEvent"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;displayItemActivation(Lnet/minecraft/world/item/ItemStack;)V"))
    private void onEntityStatus(GameRenderer gameRenderer, ItemStack itemStack) {
        RenderOverlayEvent event = new RenderOverlayEvent(RenderOverlayType.TOTEM_POP);
        Raven.INSTANCE.eventDispatcher().dispatch(event);
        if (event.isCancelled()) {
            return;
        }
        gameRenderer.displayItemActivation(itemStack);
    }

    @Inject(method = {"handleLevelChunkWithLight"}, at = {@At("RETURN")})
    private void onChunkDataHook(ClientboundLevelChunkWithLightPacket chunkDataS2CPacket, CallbackInfo callbackInfo) {
        Raven.INSTANCE.eventDispatcher().dispatch(new ChunkLoadEvent(new ChunkPos(chunkDataS2CPacket.getX(), chunkDataS2CPacket.getZ())));
        scanChunk(chunkDataS2CPacket.getLightData().getBlockUpdates(), this.level.getChunk(chunkDataS2CPacket.getX(), chunkDataS2CPacket.getZ()), BlockUpdateType.LOAD);
    }

    @Inject(method = {"handleForgetLevelChunk"}, at = {@At("HEAD")})
    private void onUnloadChunkHook(ClientboundForgetLevelChunkPacket unloadChunkS2CPacket, CallbackInfo callbackInfo) {
        scanChunk(new ArrayList(), this.level.getChunk(unloadChunkS2CPacket.pos().x(), unloadChunkS2CPacket.pos().z()), BlockUpdateType.UNLOAD);
    }

    @Inject(method = {"handleTabListCustomisation"}, at = {@At("HEAD")})
    public void onPlayerListHeader(ClientboundTabListPacket playerListHeaderS2CPacket, CallbackInfo callbackInfo) {
        Component textHeader = playerListHeaderS2CPacket.header();
        if (textHeader != null) {
            ScoreboardHelper.INSTANCE.setHeader(textHeader);
        }
    }

    @Inject(method = "handlePlayerInfoRemove", at = @At("TAIL"))
    private void raven$trackDepartedPlayers(ClientboundPlayerInfoRemovePacket packet, CallbackInfo callbackInfo) {
        Raven.INSTANCE.moduleRepository().get(LeaveTrackerModule.class)
                .onProfilesRemoved(packet.profileIds());
    }

    @Unique
    private void scanChunk(List<byte[]> list, LevelChunk worldChunk, BlockUpdateType blockUpdateType) {
        int startX = worldChunk.getPos().getMinBlockX();
        int startZ = worldChunk.getPos().getMinBlockZ();
        ArrayList arrayList = new ArrayList();
        byte[] bArr = list.isEmpty() ? null : (byte[]) list.getFirst();
        DataLayer chunkNibbleArray = bArr == null ? null : new DataLayer(Arrays.copyOf(bArr, bArr.length));
        for (int i = 0; i <= worldChunk.getHighestFilledSectionIndex(); i++) {
            int i2 = i;
            arrayList.add(CompletableFuture.runAsync(() -> {
                ArrayList arrayList2 = new ArrayList();
                LevelChunkSection section = worldChunk.getSection(i2);
                int bottomY = (i2 + (worldChunk.getMinY() >> 4)) << 4;
                for (int i3 = 0; i3 < 16; i3++) {
                    for (int i4 = 0; i4 < 16; i4++) {
                        for (int i5 = 0; i5 < 16; i5++) {
                            arrayList2.add(new BlockUpdateEntry(new BlockPos(startX | i3, bottomY | i5, startZ | i4), blockUpdateType.equals(BlockUpdateType.LOAD) ? section.getBlockState(i3, i5, i4) : Blocks.AIR.defaultBlockState(), chunkNibbleArray == null ? 0 : chunkNibbleArray.get(i3, i5, i4)));
                        }
                    }
                }
                if (arrayList2.isEmpty()) {
                    return;
                }
                Raven.INSTANCE.eventDispatcher().dispatch(new BlockUpdateEvent(arrayList2, blockUpdateType));
            }));
        }
        CompletableFuture.allOf((CompletableFuture[]) arrayList.toArray(new CompletableFuture[0])).join();
    }
}
