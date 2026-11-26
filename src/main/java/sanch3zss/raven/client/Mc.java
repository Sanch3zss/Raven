package sanch3zss.raven.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.phys.HitResult;

public enum Mc {
    INSTANCE;

    public static final EquipmentAssetManager equipmentModelLoader = new EquipmentAssetManager();
    public Minecraft minecraft;

    public ClientLevel getWorld() {
        ensureClient();
        return this.minecraft.level;
    }

    public net.minecraft.client.gui.Hud getInGameHud() {
        ensureClient();
        return this.minecraft.gui.hud;
    }

    public ServerData getServerInfo() {
        ensureClient();
        return this.minecraft.getCurrentServer();
    }

    public User getSession() {
        ensureClient();
        return this.minecraft.getUser();
    }

    public LocalPlayer getPlayer() {
        ensureClient();
        return this.minecraft.player;
    }

    public int getCurrentFps() {
        return this.minecraft.getFps();
    }

    public HitResult getCrosshairTarget() {
        ensureClient();
        return this.minecraft.hitResult;
    }

    public DeltaTracker getRenderTickCounter() {
        ensureClient();
        return this.minecraft.getDeltaTracker();
    }

    public MultiPlayerGameMode getInteractionManager() {
        ensureClient();
        return this.minecraft.gameMode;
    }

    public Options getGameOptions() {
        ensureClient();
        return this.minecraft.options;
    }

    public GameRenderer getGameRenderer() {
        ensureClient();
        return this.minecraft.gameRenderer;
    }

    public MouseHandler getMouse() {
        ensureClient();
        return this.minecraft.mouseHandler;
    }

    public SkinManager getSkinProvider() {
        ensureClient();
        return this.minecraft.getSkinManager();
    }

    public Camera getCamera() {
        return getGameRenderer().mainCamera();
    }

    public Entity getCameraEntity() {
        ensureClient();
        return this.minecraft.getCameraEntity();
    }

    public boolean isWorldLoaded() {
        Mc mc = INSTANCE;
        return (mc.getWorld() == null || mc.getPlayer() == null || mc.getNetworkHandler() == null || mc.getInteractionManager() == null) ? false : true;
    }

    public ResourceManager getResourceManager() {
        ensureClient();
        return this.minecraft.getResourceManager();
    }

    public TextureManager getTextureManager() {
        ensureClient();
        return this.minecraft.getTextureManager();
    }

    public Font getTextRenderer() {
        ensureClient();
        return this.minecraft.font;
    }

    public Window getWindow() {
        ensureClient();
        return this.minecraft.getWindow();
    }

    public ServerData getCurrentServer() {
        ensureClient();
        return this.minecraft.getCurrentServer();
    }

    public Screen getCurrentScreen() {
        ensureClient();
        return this.minecraft.gui.screen();
    }

    public AbstractContainerMenu getCurrentScreenHandler() {
        return getPlayer().containerMenu;
    }

    public RenderTarget getFramebuffer() {
        ensureClient();
        return this.minecraft.gameRenderer.mainRenderTarget();
    }

    public ItemModelResolver getItemModelManager() {
        ensureClient();
        return this.minecraft.getItemModelResolver();
    }

    public EntityRenderDispatcher getEntityRenderDispatcher() {
        ensureClient();
        return this.minecraft.getEntityRenderDispatcher();
    }

    public ClientPacketListener getNetworkHandler() {
        ensureClient();
        return this.minecraft.getConnection();
    }

    public float getTickDelta() {
        ensureClient();
        return this.minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    public boolean isSingleplayer() {
        ensureClient();
        return this.minecraft.isLocalServer();
    }

    public void setCurrentScreen(Screen screen) {
        ensureClient();
        this.minecraft.setScreenAndShow(screen);
    }

    public void ensureClient() {
        if (this.minecraft == null) {
            this.minecraft = Minecraft.getInstance();
        }
    }

    public EntityRendererProvider.Context createEntityRendererContext() {
        return new EntityRendererProvider.Context(getEntityRenderDispatcher(), new net.minecraft.client.renderer.block.BlockModelResolver(this.minecraft.getModelManager()), this.minecraft.getItemModelResolver(), this.minecraft.getMapRenderer(), getResourceManager(), this.minecraft.getEntityModels(), equipmentModelLoader, this.minecraft.getAtlasManager(), getTextRenderer(), this.minecraft.playerSkinRenderCache());
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }
}
