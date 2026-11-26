package sanch3zss.raven.feature.module.render.poptweaks;

import java.util.ArrayList;
import java.util.List;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import sanch3zss.raven.render.shape.ImmediateRenderLayers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.resources.sounds.SoundInstance;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.audio.CustomSoundPicker;
import sanch3zss.raven.client.audio.WavSoundPlayer;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.client.SoundPlayEvent;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.network.PacketReceiveEvent;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.render.animation.AnimationStack2;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.MathUtil;
import sanch3zss.raven.util.math.interpolation.DeltaTimeTracker;

@Aliases(aliases = {"PopTweaks", "Pop Tweaks", "PopChams", "Pop Chams", "Totem Pop"})
public class PopTweaksModule extends Module {
    public final List<PopChamsModel> models;
    public final BooleanSetting replaceTotemParticles;
    public final ColorSetting color;
    private final TextFieldSetting totemSound;
    private final NumberSetting totemVolume;
    public final DeltaTimeTracker deltaTracker;
    public final AnimationStack2 animationStack;

    public PopTweaksModule() {
        super(ModuleTab.VISUALS, "PopTweaks");
        this.models = new ArrayList<>();
        this.replaceTotemParticles = new BooleanSetting(Lang.POPTWEAKS_REPLACE_TOTEM_PARTICLES);
        this.color = new ColorSetting(Lang.POPCHAMS_COLOR);
        this.deltaTracker = new DeltaTimeTracker();
        this.animationStack = new AnimationStack2();
        this.totemSound = new TextFieldSetting(Translation.clearText("Путь к звуку тотема"))
                .setText("").visible(() -> false);
        this.totemVolume = new NumberSetting(Translation.clearText("Громкость тотема"))
                .currentValue(70.0f).range(1.0f, 100.0f).step(1.0f).unit(SettingUnit.PERCENTS)
                .visible(this::hasCustomSound);
        ButtonSetting chooseSound = new ButtonSetting(Translation.clearText("Звук тотема"))
                .setButtonName(Translation.clearText("Выбрать звук"))
                .setRunnable(() -> CustomSoundPicker.choose(path -> {
                    this.totemSound.setText(path);
                    WavSoundPlayer.INSTANCE.playCustomSound(path, this.totemVolume.currentValue());
                }));
        ButtonSetting resetSound = new ButtonSetting(Translation.clearText(""))
                .setButtonName(Translation.clearText("Сбросить"))
                .setRunnable(() -> this.totemSound.setText(""))
                .visible(this::hasCustomSound);
        addSettings(this.replaceTotemParticles, this.color, chooseSound, this.totemVolume, resetSound, this.totemSound);
        register(SoundPlayEvent.class, event -> {
            SoundInstance sound = event.getSoundInstance();
            if (!isState() || !hasCustomSound() || !Mc.INSTANCE.isWorldLoaded()
                    || !sound.getIdentifier().getNamespace().equals("minecraft")
                    || !sound.getIdentifier().getPath().equals("item.totem.use")) {
                return;
            }
            event.cancel();
            double distance = Math.sqrt(Mc.INSTANCE.getPlayer().distanceToSqr(sound.getX(), sound.getY(), sound.getZ()));
            float attenuation = (float) Math.max(0.0, 1.0 - distance / 16.0);
            if (attenuation > 0.0f) {
                WavSoundPlayer.INSTANCE.playCustomSound(this.totemSound.getText(),
                        this.totemVolume.currentValue() * attenuation);
            }
        });
        register(PlayerTickEvent.class, event -> {
            if (Mc.INSTANCE.isWorldLoaded() && event.isPre()) {
                this.models.removeIf(PopChamsModel::tick);
            }
        });
        register(WorldRenderEvent.class, event3 -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded() && !this.models.isEmpty()) {
                PoseStack matrixStack = event3.matrixStack();
                WeightedEngine weightedEngine = new WeightedEngine(this.deltaTracker.elapsedUnit(), this.animationStack);
                this.animationStack.begin();
                this.models.forEach(popChamsModel -> popChamsModel.animate(weightedEngine));
                this.animationStack.end();
                this.models.forEach(model -> renderModel(matrixStack, model));
            }
        });
        register(PacketReceiveEvent.class, event2 -> {
            ClientLevel world;
            Entity entity;
            if (isState()) {
                if (event2.getPacket() instanceof ClientboundEntityEventPacket packet) {
                    if (packet.getEventId() != 35 || (world = Mc.INSTANCE.getWorld()) == null || (entity = packet.getEntity(world)) == null || !(entity instanceof AbstractClientPlayer player)) {
                        return;
                    }
                    onTotemPop(player);
                }
            }
        });
    }

    private boolean hasCustomSound() {
        return !this.totemSound.getText().isEmpty();
    }

    public boolean shouldReplaceTotemParticles() {
        return isState() && this.replaceTotemParticles.isValue();
    }

    public void onTotemPop(AbstractClientPlayer player) {
        if (isState() && Mc.INSTANCE.isWorldLoaded()) {
            this.models.add(createModel(player, this.color.getColor()));
        }
    }

    @Override
    public void deactivate() {
        this.models.clear();
        super.deactivate();
    }

    public PopChamsModel createModel(AbstractClientPlayer otherClientPlayerEntity, int color) {
        Mc mc = Mc.INSTANCE;
        PopChamsFakePlayer popChamsFakePlayer = new PopChamsFakePlayer(this, mc.getWorld(), BlockPos.ZERO, otherClientPlayerEntity.yBodyRot, new GameProfile(otherClientPlayerEntity.getUUID(), otherClientPlayerEntity.getName().getString()));
        popChamsFakePlayer.copyPosition(otherClientPlayerEntity);
        ((Player) popChamsFakePlayer).yBodyRot = otherClientPlayerEntity.yBodyRot;
        ((Player) popChamsFakePlayer).yHeadRot = otherClientPlayerEntity.yHeadRot;
        ((Player) popChamsFakePlayer).attackAnim = otherClientPlayerEntity.attackAnim;
        ((Player) popChamsFakePlayer).swingTime = otherClientPlayerEntity.swingTime;
        popChamsFakePlayer.setShiftKeyDown(otherClientPlayerEntity.isShiftKeyDown());
        ((Player) popChamsFakePlayer).walkAnimation.setSpeed(otherClientPlayerEntity.walkAnimation.speed());
        ((Player) popChamsFakePlayer).walkAnimation.update(otherClientPlayerEntity.walkAnimation.position(), otherClientPlayerEntity.walkAnimation.speed(), 1.0f);
        return new PopChamsModel(popChamsFakePlayer, otherClientPlayerEntity.getSkin().body().texturePath(), color);
    }

    public void renderModel(PoseStack matrixStack, PopChamsModel popChamsModel) {
        Mc mc = Mc.INSTANCE;
        Vec3 pos = mc.getEntityRenderDispatcher().camera.position();
        PlayerModel model = popChamsModel.getModel();
        Player player = popChamsModel.getPlayer();
        Identifier texture = popChamsModel.getTexture();
        int animatedColor = this.color.getAnimatedGradientColor(popChamsModel.getAnimation().smoothAnimation() * 0.15f);
        int iApplyOpacity = ColorUtil.applyOpacity(animatedColor, ColorUtil.alpha(animatedColor) * popChamsModel.getAnimation().smoothAnimation());
        double x = player.getX() - pos.x();
        double y = player.getY() - pos.y();
        double z = player.getZ() - pos.z();
        mc.getTickDelta();
        float pos2 = player.walkAnimation.position();
        float speed = player.walkAnimation.speed();
        AvatarRenderState playerEntityRenderState = new AvatarRenderState();
        playerEntityRenderState.bodyRot = player.getVisualRotationYInDegrees();
        playerEntityRenderState.yRot = player.yHeadRot - player.yBodyRot;
        playerEntityRenderState.xRot = player.getXRot();
        playerEntityRenderState.deathTime = player.deathTime;
        playerEntityRenderState.walkAnimationPos = pos2;
        playerEntityRenderState.walkAnimationSpeed = speed;
        model.setupAnim(playerEntityRenderState);
        Identifier renderTexture = texture != null ? texture : Identifier.withDefaultNamespace("textures/misc/white.png");
        RenderType renderType = RenderTypes.entityTranslucent(renderTexture);
        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(2048);
        BufferBuilder consumer = new BufferBuilder(byteBuffer, renderType.primitiveTopology(), renderType.format());
        matrixStack.pushPose();
        matrixStack.translate((float) x, (float) y, (float) z);
        matrixStack.mulPose(Axis.YP.rotation(MathUtil.rad(180.0f - player.yBodyRot)));
        matrixStack.scale(-1.0f, -1.0f, 1.0f);
        matrixStack.translate(0.0f, -1.501f, 0.0f);
        model.renderToBuffer(
                matrixStack,
                consumer,
                net.minecraft.util.LightCoordsUtil.FULL_BRIGHT,
                0,
                iApplyOpacity
        );
        ImmediateRenderLayers.draw(renderType, consumer.buildOrThrow());
        byteBuffer.close();
        matrixStack.popPose();
    }

}
