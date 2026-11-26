package sanch3zss.raven.feature.module.player;

import java.util.UUID;
import java.util.Optional;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.render.poptweaks.PopTweaksModule;
import sanch3zss.raven.feature.player.*;
import sanch3zss.raven.feature.module.render.armtweaks.knife.KnifeRenderer;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"Fake Player", "FakePlayer", "Dummy", "Training Dummy"})
public final class FakePlayerModule extends Module {
    private static final int FAKE_PLAYER_ID = -49204;

    public final TextFieldSetting botName;
    public final ModeSetting<FakePlayerPose> pose;
    private RemotePlayer fakePlayer;

    public FakePlayerModule() {
        super(ModuleTab.MISC, "FakePlayer");
        this.botName = new TextFieldSetting(Lang.FAKEPLAYER_NAME)
                .setPlaceholder(Lang.FAKEPLAYER_NAME_PLACEHOLDER)
                .setText("FakePlayer")
                .setMin(1)
                .setMax(16);
        this.pose = new ModeSetting<FakePlayerPose>(Lang.FAKEPLAYER_POSE)
                .values(FakePlayerPose.class);
        addSettings(this.botName, this.pose);

        register(PlayerTickEvent.class, event -> {
            if (isState() && event.isPre() && Mc.INSTANCE.isWorldLoaded()) {
                updateFakePlayer();
            }
        });
        register(PlayerInitEvent.class, event -> {
            if (isState()) {
                setState(false);
            }
        });
    }

    @Override
    public void activate() {
        if (!Mc.INSTANCE.isWorldLoaded()) {
            setState(false);
            return;
        }

        LocalPlayer player = Mc.INSTANCE.getPlayer();
        ClientLevel world = Mc.INSTANCE.getWorld();
        String name = this.botName.getText() == null ? "" : this.botName.getText().trim();
        if (name.isEmpty()) {
            name = "FakePlayer";
        } else if (name.length() > 16) {
            name = name.substring(0, 16);
        }

        this.fakePlayer = new RemotePlayer(world, new GameProfile(UUID.randomUUID(), name));
        this.fakePlayer.noPhysics = true;
        this.fakePlayer.copyPosition(player);
        this.fakePlayer.setYHeadRot(player.getYHeadRot());
        this.fakePlayer.setYBodyRot(player.yBodyRot);
        this.fakePlayer.yHeadRotO = this.fakePlayer.getYHeadRot();
        this.fakePlayer.yBodyRotO = this.fakePlayer.yBodyRot;
        this.fakePlayer.setId(FAKE_PLAYER_ID);
        equipArmor();
        this.fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        this.fakePlayer.setHealth(20.0f);
        this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 99999, 3));
        this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 99999, 9));
        this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 99999, 1));
        super.activate();
    }

    @Override
    public void deactivate() {
        if (this.fakePlayer != null) {
            this.fakePlayer = null;
        }
        super.deactivate();
    }

    private void updateFakePlayer() {
        if (this.fakePlayer == null) {
            return;
        }
        this.fakePlayer.tickCount++;
        this.fakePlayer.setOldPosAndRot();
        this.fakePlayer.yHeadRotO = this.fakePlayer.getYHeadRot();
        this.fakePlayer.yBodyRotO = this.fakePlayer.yBodyRot;
        this.fakePlayer.walkAnimation.update(0.0f, 0.4f, 1.0f);
        if (this.fakePlayer.hurtTime > 0) {
            this.fakePlayer.hurtTime--;
        }
        boolean swimming = this.pose.currentValue() == FakePlayerPose.SWIMMING;
        this.fakePlayer.setSwimming(swimming);
        this.fakePlayer.setPose(this.pose.currentValue().entityPose());
        if (!this.fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            this.fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        }
    }

    private void equipArmor() {
        equipUnbreakable(EquipmentSlot.HEAD, Items.DIAMOND_HELMET);
        equipUnbreakable(EquipmentSlot.CHEST, Items.DIAMOND_CHESTPLATE);
        equipUnbreakable(EquipmentSlot.LEGS, Items.DIAMOND_LEGGINGS);
        equipUnbreakable(EquipmentSlot.FEET, Items.DIAMOND_BOOTS);
    }

    private void equipUnbreakable(EquipmentSlot slot, Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        this.fakePlayer.setItemSlot(slot, stack);
    }

    private void handleAttack() {
        if (this.fakePlayer == null || !Mc.INSTANCE.isWorldLoaded() || this.fakePlayer.hurtTime > 0) {
            return;
        }

        LocalPlayer player = Mc.INSTANCE.getPlayer();
        playSound(player.fallDistance > 0.0f
                ? SoundEvents.PLAYER_ATTACK_CRIT
                : SoundEvents.PLAYER_ATTACK_STRONG);
        this.fakePlayer.handleDamageEvent(Mc.INSTANCE.getWorld().damageSources().generic());

        float damage = player.getAttackStrengthScale(0.5f) >= 0.85f ? 1.0f : 0.5f;
        this.fakePlayer.setHealth(this.fakePlayer.getHealth() - damage);
        if (this.fakePlayer.getHealth() > 0.0f
                || !this.fakePlayer.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            return;
        }

        playSound(SoundEvents.TOTEM_USE);
        Mc.INSTANCE.getMinecraft().particleEngine.createTrackingEmitter(
                this.fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 30);
        Raven.INSTANCE.moduleRepository().get(PopTweaksModule.class).onTotemPop(this.fakePlayer);
        this.fakePlayer.setHealth(20.0f);
        this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        this.fakePlayer.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        this.fakePlayer.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    private void playSound(SoundEvent sound) {
        Mc.INSTANCE.getWorld().playLocalSound(this.fakePlayer.getX(), this.fakePlayer.getY(),
                this.fakePlayer.getZ(), sound, SoundSource.PLAYERS, 1.0f, 1.0f, false);
    }

    public RemotePlayer getFakePlayer() {
        return this.fakePlayer;
    }

    public RemotePlayer lookedAtFakePlayer() {
        if (!isState() || this.fakePlayer == null || !Mc.INSTANCE.isWorldLoaded()) {
            return null;
        }
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        float partialTick = Mc.INSTANCE.getRenderTickCounter().getGameTimeDeltaPartialTick(false);
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 end = eye.add(player.getViewVector(partialTick).scale(12.0d));
        Optional<Vec3> intersection = this.fakePlayer.getBoundingBox().clip(eye, end);
        if (intersection.isEmpty()) {
            return null;
        }
        HitResult hit = Mc.INSTANCE.getMinecraft().hitResult;
        if (hit != null && hit.getType() != HitResult.Type.MISS
                && eye.distanceToSqr(hit.getLocation()) + 0.001d
                < eye.distanceToSqr(intersection.get())) {
            return null;
        }
        return this.fakePlayer;
    }

    public boolean attackIfLookedAt() {
        RemotePlayer target = lookedAtFakePlayer();
        if (target == null) {
            return false;
        }
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        ItemStack heldItem = player.getMainHandItem();
        if (player.isHandsBusy() || Mc.INSTANCE.getMinecraft().gameMode.isSpectator()
                || !heldItem.isItemEnabled(Mc.INSTANCE.getWorld().enabledFeatures())
                || player.cannotAttackWithItem(heldItem, 0)
                || !player.isWithinAttackRange(heldItem, target.getBoundingBox(), 0.0d)) {
            return false;
        }
        AttackEntityEvent attack = new AttackEntityEvent(target);
        Raven.INSTANCE.eventDispatcher().dispatch(attack);
        if (attack.isCancelled()) {
            return true;
        }
        KnifeRenderer.INSTANCE.prepareAttack(player, new EntityHitResult(target));
        handleAttack();
        player.resetAttackStrengthTicker();
        player.swing(InteractionHand.MAIN_HAND, false);
        return true;
    }
}
