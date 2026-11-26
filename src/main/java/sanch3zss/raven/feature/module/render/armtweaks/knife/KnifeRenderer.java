package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.input.KeyAction;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;

public final class KnifeRenderer {
    public static final KnifeRenderer INSTANCE = new KnifeRenderer();
    private static final Set<String> COMBO_CLIPS = Set.of("melee_1", "melee_2");
    private static final Set<String> COMBO_BONES = Set.of("root", "knife_and_right", "righthand", "lefthand");
    private static final String ROOT = "knives";
    private static final String BORROWED_ROOT = "knives/borrowed";
    private final Map<String, KnifeModel> models = new HashMap<>();
    private final Map<String, KnifeAnimation> animations = new HashMap<>();
    private final Map<KnifeVariant, KnifeAnimation.Clip> borrowedInspects = new HashMap<>();
    private final Set<Identifier> adjustedTextures = new HashSet<>();
    private KnifeVariant equipped;
    private Item equippedItem;
    private int equippedSlot = -1;
    private String action;
    private long actionStarted;
    private String previousAction;
    private long previousStarted;
    private long idleStarted;
    private long soundsStarted;
    private float soundsPlayedUntil;
    private final List<SoundInstance> playingSounds = new ArrayList<>();
    private int attackIndex;
    private int inspectIndex;
    private final Set<String> failedAssets = new HashSet<>();
    private boolean inspectHeld;
    private boolean inspectLooping;
    private String heldInspectAction;
    private boolean preparedCritical;
    private long preparedAttackAt;
    private Entity attackTarget;
    private Matrix4f referenceView;
    private boolean offhandDaggers;
    private Matrix4f referenceLeftHand;
    private Matrix4f offhandCameraPose;
    private Matrix4f offhandRelativePose;
    private Matrix4f offhandUsePose;
    private AbstractClientPlayer offhandPlayer;
    private boolean offhandArmAlreadyRendered;
    private float offhandUseProgress;
    private float offhandUseElapsed;
    private long offhandUseFrame;
    private Item offhandUseItem;
    private int offhandFace;
    private boolean offhandFaceLocked;
    private final Map<Item, AABB> groundBounds = new HashMap<>();
    private final ItemStackRenderState boundsState = new ItemStackRenderState();

    private KnifeRenderer() {}

    public static boolean eligible(InteractionHand hand, ItemStack stack) {
        return hand == InteractionHand.MAIN_HAND && stack != null && stack.is(ItemTags.SWORDS);
    }

    public boolean suppressesUseReequip(InteractionHand hand) {
        if (hand != InteractionHand.OFF_HAND) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return false;
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        return module.shouldRenderKnife() && eligible(InteractionHand.MAIN_HAND, minecraft.player.getMainHandItem());
    }

    Entity attackTarget() {
        return System.nanoTime() - preparedAttackAt < 500_000_000L ? attackTarget : null;
    }

    public void prepareAttack(AbstractClientPlayer player, HitResult target) {
        attackTarget = target instanceof EntityHitResult hit ? hit.getEntity() : null;
        preparedCritical = player != null && eligible(InteractionHand.MAIN_HAND, player.getMainHandItem())
                && target instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity
                && player.getAttackStrengthScale(0.5f) > 0.9f
                && player.fallDistance > 0.0f && !player.onGround() && !player.onClimbable()
                && !player.isInWater() && !player.hasEffect(MobEffects.BLINDNESS)
                && !player.isPassenger() && !player.isSprinting();
        preparedAttackAt = System.nanoTime();
    }

    public void onSwing(InteractionHand hand, ItemStack stack) {
        if (!eligible(hand, stack)) return;
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!module.shouldRenderKnife()) return;
        long now = System.nanoTime();
        boolean heavy = preparedCritical && now - preparedAttackAt < 500_000_000L;
        preparedCritical = false;
        if (module.shouldRenderCs2Hands()) {
            Cs2Viewmodel.INSTANCE.swing(heavy);
            Cs2Body.INSTANCE.attack(heavy);
            return;
        }
        startAction(heavy ? "melee_3" : (attackIndex++ & 1) == 0 ? "melee_1" : "melee_2", now);
        inspectLooping = false;
    }

    private void startAction(String next, long now) {
        if (action != null) {
            previousAction = action;
            previousStarted = actionStarted;
        }
        action = next;
        actionStarted = now;
    }

    public void inspect(KeyAction keyAction) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (module.shouldRenderCs2Hands()) {
            Minecraft minecraft = Minecraft.getInstance();
            boolean pressed = keyAction == KeyAction.PRESS;
            if (!pressed || minecraft.player != null && Mc.INSTANCE.getCurrentScreen() == null
                    && eligible(InteractionHand.MAIN_HAND, minecraft.player.getMainHandItem())) {
                Cs2Viewmodel.INSTANCE.inspect(pressed);
            }
            return;
        }
        inspectHeld = keyAction == KeyAction.PRESS;
        if (inspectHeld) playInspectAnimation();
        else heldInspectAction = null;
    }

    private void playInspectAnimation() {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        Minecraft minecraft = Minecraft.getInstance();
        if (!module.shouldRenderKnife() || minecraft.player == null || Mc.INSTANCE.getCurrentScreen() != null
                || !eligible(InteractionHand.MAIN_HAND, minecraft.player.getMainHandItem())) return;
        KnifeVariant variant = module.activeKnifeVariant();
        KnifeAnimation animation = inspectAnimation(variant);
        if (animation == null) return;
        String candidate;
        if (variant == KnifeVariant.SKELETON) {
            candidate = "inspect_1";
        } else if (variant == KnifeVariant.BUTTERFLY) {
            candidate = new String[]{"inspect_1", "inspect_2", "inspect"}[inspectIndex++ % 3];
        } else if (animation.clip("inspect_2") != null) {
            candidate = inspectIndex++ % 2 == 0 ? "inspect" : "inspect_2";
        } else {
            candidate = "inspect";
        }
        startAction(animation.clip(candidate) == null ? "inspect" : candidate, System.nanoTime());
        heldInspectAction = action;
        inspectLooping = false;
    }

    public boolean render(AbstractClientPlayer player, InteractionHand hand, ItemStack stack,
                          ItemStack offhandStack, float offhandLowered,
                          PoseStack poseStack, SubmitNodeCollector collector, int light) {
        if (hand != InteractionHand.MAIN_HAND) return false;
        offhandCameraPose = null;
        offhandRelativePose = null;
        offhandUsePose = null;
        offhandPlayer = null;
        offhandArmAlreadyRendered = false;
        offhandDaggers = false;
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!module.shouldRenderKnife() || !eligible(hand, stack)) {
            Cs2Viewmodel.INSTANCE.reset();
            stopSounds();
            equipped = null;
            inspectHeld = false;
            inspectLooping = false;
            heldInspectAction = null;
            offhandUseProgress = 0.0f;
            return false;
        }
        if (player.isScoping() || player.isAutoSpinAttack()
                || player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND) return false;
        ViewmodelLighting.INSTANCE.update(player, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));
        KnifeVariant variant = module.activeKnifeVariant();
        if (module.shouldRenderCs2Hands()) {
            stopSounds();
            equipped = null;
            KnifeSkin skin = module.activeKnifeSkin();
            Identifier texture = knifeTexture(variant, skin);
            KnifeFinish finish = variant.finish(skin);
            GlassHandsRenderer glass = GlassHandsRenderer.INSTANCE;
            RenderType knifeType = glass.knifeRenderType(texture, finish.renderType(texture), module.shouldRenderGlassHands());
            return Cs2Viewmodel.INSTANCE.render(player, stack, offhandStack, variant, knifeType, finish.uScale(),
                    glass.knifeOverlay(module.shouldRenderGlassHands()), poseStack, collector, light);
        }
        Cs2Viewmodel.INSTANCE.reset();
        KnifeModel model = loadModel(variant);
        KnifeAnimation animation = loadAnimation(variant);
        if (model == null || animation == null) return false;
        int slot = player.getInventory().getSelectedSlot();
        if (equipped != variant || equippedItem != stack.getItem() || equippedSlot != slot) {
            equipped = variant;
            equippedItem = stack.getItem();
            equippedSlot = slot;
            action = "draw";
            previousAction = null;
            actionStarted = System.nanoTime();
            idleStarted = actionStarted;
        }
        long now = System.nanoTime();
        float speed = module.knifeSpeed.currentValue();
        KnifeAnimation.Clip idle = animation.clip("static_idle");
        model.stabilize(idle);
        KnifeAnimation.Clip active = clip(variant, animation, action);
        float time = (now - actionStarted) * 0.000000001f * speed;
        if (active != null && active.duration() > 0.0f && time >= active.duration()
                && inspectHeld && action.startsWith("inspect")) {
            actionStarted = now;
            time = 0.0f;
            inspectLooping = true;
        } else if (active != null && time >= active.duration() && inspectHeld
                && clip(variant, animation, heldInspectAction) != null) {
            action = heldInspectAction;
            active = clip(variant, animation, action);
            actionStarted = now;
            time = 0.0f;
            inspectLooping = false;
        } else if (active == null || active.duration() <= 0.0f || time >= active.duration()) {
            action = null;
            active = null;
            inspectLooping = false;
        }
        if (active != null && module.knifeSounds.isValue()) playSounds(active, time);
        float weight = active == null ? 0.0f
                : transitionWeight(action, time, active.duration(), inspectHeld, inspectLooping);
        KnifeAnimation.Clip previous = clip(variant, animation, previousAction);
        float previousTime = (now - previousStarted) * 0.000000001f * speed;
        float previousWeight = 0.0f;
        if (previous != null && active != null && weight < 1.0f && time < active.duration() * 0.5f) {
            previousWeight = transitionWeight(previousAction, previousTime, previous.duration(), false, false);
        } else {
            previousAction = null;
            previous = null;
        }
        float idleTime = idle == null || idle.duration() <= 0.0f ? 0.0f
                : ((now - idleStarted) * 0.000000001f * speed) % idle.duration();
        KnifeModel.Frame frame = model.frame(idle, idleTime, previous, previousTime, previousWeight,
                previous != null && nativeInspect(variant, previousAction), active, time, Math.max(0.0f, weight),
                active != null && nativeInspect(variant, action));
        Matrix4f cameraPose = new Matrix4f(poseStack.last().pose());
        poseStack.pushPose();
        module.applyKnifeViewModel(poseStack, player.getMainArm());
        poseStack.translate(0.0f, 1.5f, 0.0f);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        model.positionToIdleView(poseStack);
        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        if (leftHanded) poseStack.scale(-1.0f, 1.0f, 1.0f);
        KnifeSkin skin = module.activeKnifeSkin();
        Identifier texture = knifeTexture(variant, skin);
        GlassHandsRenderer glass = GlassHandsRenderer.INSTANCE;
        KnifeFinish finish = variant.finish(skin);
        float uScale = finish.uScale();
        RenderType knifeType = glass.knifeRenderType(texture, finish.renderType(texture), module.shouldRenderGlassHands());
        int knifeOverlay = glass.knifeOverlay(module.shouldRenderGlassHands());
        float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
        if (!offhandStack.isEmpty()) updateOffhandUse(player, offhandStack, partialTick);
        boolean usingOffhand = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND;
        boolean daggers = variant.daggers();
        boolean offhandReplacesDagger = daggers && heldModel(offhandStack)
                && offhandStack.getUseAnimation() != ItemUseAnimation.BLOCK;
        boolean showLeftKnife = offhandStack.isEmpty()
                || daggers && !offhandReplacesDagger && !offhandStack.has(DataComponents.MAP_ID);
        if (frame.leftHand != null) {
            offhandFaceLocked = action != null;
            offhandCameraPose = cameraPose;
            Matrix4f toCamera = new Matrix4f(cameraPose).invert().mul(poseStack.last().pose());
            offhandRelativePose = new Matrix4f(toCamera).mul(frame.leftHand).rotateZ((float) Math.PI);
            if (offhandReplacesDagger) {
                Matrix4f reference = referenceLeftHand(module, player);
                Matrix4f idleHand = model.frame(idle, idleTime, null, 0.0f, 0.0f).leftHand;
                if (reference != null && idleHand != null) {
                    Matrix4f idlePose = new Matrix4f(toCamera).mul(idleHand).rotateZ((float) Math.PI);
                    offhandRelativePose = new Matrix4f(offhandRelativePose).mul(idlePose.invert()).mul(reference);
                }
            }
            offhandPlayer = player;
            offhandDaggers = daggers;
        }
        float offhandSwing = offhandSwing(player, partialTick);
        if (daggers && offhandRelativePose != null
                && (usingOffhand || offhandUseProgress > 0.001f || offhandSwing > 0.0f)) {
            HumanoidArm offhandArm = player.getMainArm().getOpposite();
            ItemUseAnimation animationType = offhandStack.getUseAnimation();
            if (isConsumable(animationType)) {
                offhandUsePose = consumableMotion(offhandRelativePose, offhandArm,
                        animationType, offhandUseProgress, offhandUseElapsed);
            } else {
                int side = offhandArm == HumanoidArm.RIGHT ? 1 : -1;
                offhandUsePose = heldUseMotion(player, offhandStack,
                        utilityMotion(offhandRelativePose, animationType, side, offhandUseProgress,
                                offhandUseElapsed, offhandSwing),
                        offhandRelativePose, side, leftHanded, partialTick);
            }
        }
        float drop = showLeftKnife ? lowerOffset(offhandLowered) : 0.0f;
        Matrix4f localMotion = offhandUsePose;
        if (drop != 0.0f) {
            Matrix4f lowered = new Matrix4f().translation(0.0f, drop, 0.0f);
            localMotion = localMotion == null ? lowered : lowered.mul(localMotion);
        }
        Matrix4f renderLeftMotion = localMotion == null ? null : new Matrix4f(cameraPose).mul(localMotion)
                .mul(new Matrix4f(cameraPose).invert());
        collector.submitCustomGeometry(poseStack, knifeType,
                (pose, buffer) -> frame.emit(pose, buffer, light, knifeOverlay, showLeftKnife, renderLeftMotion, uScale));
        renderArm(player, collector, poseStack, frame.rightHand,
                leftHanded ? HumanoidArm.LEFT : HumanoidArm.RIGHT, light, leftHanded);
        if (showLeftKnife) {
            PoseStack leftArmPose = poseStack;
            if (renderLeftMotion != null) {
                leftArmPose = new PoseStack();
                leftArmPose.mulPose(renderLeftMotion);
                leftArmPose.mulPose(poseStack.last().pose());
            }
            renderArm(player, collector, leftArmPose, frame.leftHand,
                    leftHanded ? HumanoidArm.RIGHT : HumanoidArm.LEFT, light, leftHanded);
            offhandArmAlreadyRendered = frame.leftHand != null;
        }
        poseStack.popPose();
        return true;
    }

    RenderType knifeRenderType(KnifeVariant variant, KnifeSkin skin) {
        return variant.finish(skin).renderType(knifeTexture(variant, skin));
    }

    private Identifier knifeTexture(KnifeVariant variant, KnifeSkin skin) {
        if (variant.painted()) {
            ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
            Identifier painted = PaintedSkins.INSTANCE.texture(variant.model, skin,
                    (int) module.knifePattern.currentValue(), module.knifeWear.currentValue());
            if (painted != null) return painted;
        }
        Identifier texture = Identifier.fromNamespaceAndPath("raven",
                ROOT + "/textures/melee/" + variant.model + "_uv.png");
        if (adjustedTextures.add(texture)) {
            Minecraft.getInstance().getTextureManager().registerAndLoad(texture, new KnifeTexture(texture));
        }
        return texture;
    }

    private void playSounds(KnifeAnimation.Clip clip, float time) {
        if (soundsStarted != actionStarted) {
            soundsStarted = actionStarted;
            soundsPlayedUntil = -1.0f;
            stopSounds();
        }
        for (KnifeAnimation.Sound sound : clip.sounds()) {
            if (sound.time() <= soundsPlayedUntil || sound.time() > time) continue;
            if (Minecraft.getInstance().getSoundManager().getSoundEvent(sound.event()) == null) continue;
            SoundInstance instance = new SimpleSoundInstance(sound.event(), SoundSource.PLAYERS,
                    1.0f, 1.0f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0, true);
            Minecraft.getInstance().getSoundManager().play(instance);
            playingSounds.add(instance);
        }
        soundsPlayedUntil = time;
    }

    private void stopSounds() {
        if (playingSounds.isEmpty()) return;
        for (SoundInstance instance : playingSounds) Minecraft.getInstance().getSoundManager().stop(instance);
        playingSounds.clear();
    }

    public boolean renderOffhand(AbstractClientPlayer player, ItemStack stack, float frameInterp, float lowered,
                                 SubmitNodeCollector collector, int light) {
        if (offhandCameraPose == null || offhandRelativePose == null || offhandPlayer != player || stack.isEmpty()
                || stack.has(DataComponents.MAP_ID)) return false;
        Matrix4f cameraPose = new Matrix4f(offhandCameraPose).translate(0.0f, lowerOffset(lowered), 0.0f);
        Matrix4f relativePose = offhandRelativePose;
        Matrix4f usePose = offhandUsePose;
        boolean armAlreadyRendered = offhandArmAlreadyRendered;
        offhandDaggers = false;
        offhandCameraPose = null;
        offhandRelativePose = null;
        offhandUsePose = null;
        offhandPlayer = null;
        offhandArmAlreadyRendered = false;
        HumanoidArm arm = player.getMainArm().getOpposite();
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        ItemUseAnimation useAnimation = stack.getUseAnimation();
        float progress = offhandUseProgress;
        if (isConsumable(useAnimation)) {
            renderConsumableOffhand(player, stack, collector, light, cameraPose, relativePose, arm,
                    useAnimation, progress, offhandUseElapsed, armAlreadyRendered, usePose);
            return true;
        }
        boolean mirrored = player.getMainArm() == HumanoidArm.LEFT;
        Matrix4f motion = usePose == null
                ? heldUseMotion(player, stack,
                        utilityMotion(relativePose, useAnimation, side, progress, offhandUseElapsed,
                                offhandSwing(player, frameInterp)),
                        relativePose, side, mirrored, frameInterp)
                : usePose;
        HeldTuning tuning = tuning(stack);
        ItemStack renderStack = tuning != null ? stack.copy() : stack;
        Matrix4f matrix = new Matrix4f(cameraPose).mul(motion).mul(relativePose);
        PoseStack handStack = new PoseStack();
        handStack.mulPose(matrix);
        if (player.getMainArm() == HumanoidArm.LEFT) handStack.scale(-1.0f, 1.0f, 1.0f);
        if (!armAlreadyRendered) {
            renderArmAt(player, collector, handStack, arm, light);
        }
        if (tuning == null) {
            renderOnTopFace(player, stack, matrix, relativePose, mirrored, side, 1.0f, collector, light);
            return true;
        }
        PoseStack itemStackPose = new PoseStack();
        itemStackPose.mulPose(matrix);
        if (mirrored) itemStackPose.scale(-1.0f, 1.0f, 1.0f);
        placeInHand(itemStackPose, side);
        tuning.applyIdle(itemStackPose, side);
        GlassHandsRenderer.INSTANCE.renderViewModelItem(() ->
                Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderItem(player, renderStack,
                        arm == HumanoidArm.RIGHT ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
                                : ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                        itemStackPose, collector, light));
        return true;
    }

    private static float lowerOffset(float lowered) {
        float drop = Math.max(0.0f, Math.min(1.0f, lowered));
        return -0.6f * drop * drop * (3.0f - 2.0f * drop);
    }

    private Matrix4f referenceLeftHand(ArmTweaksModule module, AbstractClientPlayer player) {
        if (referenceLeftHand == null) {
            KnifeModel model = loadModel(BORROWED_ROOT, KnifeVariant.CLASSIC.model);
            KnifeAnimation animation = loadAnimation(KnifeVariant.CLASSIC);
            if (model == null || animation == null) return null;
            KnifeModel.Frame frame = model.frame(animation.clip("static_idle"), 0.0f, null, 0.0f, 0.0f);
            if (frame.leftHand == null) return null;
            PoseStack view = new PoseStack();
            view.translate(0.0f, 1.5f, 0.0f);
            view.mulPose(Axis.ZP.rotationDegrees(180.0f));
            model.positionToIdleView(view);
            referenceView = new Matrix4f(view.last().pose());
            referenceLeftHand = new Matrix4f(frame.leftHand);
        }
        PoseStack pose = new PoseStack();
        module.applyKnifeViewModel(pose, player.getMainArm());
        Matrix4f result = new Matrix4f(pose.last().pose()).mul(referenceView);
        if (player.getMainArm() == HumanoidArm.LEFT) result.scale(-1.0f, 1.0f, 1.0f);
        return result.mul(referenceLeftHand).rotateZ((float) Math.PI);
    }

    private static Vector3f gripPosition(Matrix4f motion, Matrix4f relativePose, int side, boolean mirrored) {
        PoseStack grip = new PoseStack();
        grip.mulPose(motion);
        grip.mulPose(relativePose);
        if (mirrored) grip.scale(-1.0f, 1.0f, 1.0f);
        placeInHand(grip, side);
        return grip.last().pose().getTranslation(new Vector3f());
    }

    private Matrix4f heldUseMotion(AbstractClientPlayer player, ItemStack stack, Matrix4f motion,
                                   Matrix4f relativePose, int side, boolean mirrored, float frameInterp) {
        HeldTuning tuning = tuning(stack);
        if (tuning == null) return motion;
        float progress = offhandUseProgress;
        float weight = progress * progress * (3.0f - 2.0f * progress);
        float charge = 0.0f;
        if (stack.getUseAnimation() == ItemUseAnimation.TRIDENT && player.isUsingItem()
                && player.getUsedItemHand() == InteractionHand.OFF_HAND) {
            charge = Math.min(1.0f, (player.getTicksUsingItem() + frameInterp) / 10.0f);
        }
        Vector3f grip = gripPosition(motion, relativePose, side, mirrored);
        return new Matrix4f(tuning.useMotion(side, weight, charge, grip)).mul(motion);
    }

    private static HeldTuning tuning(ItemStack stack) {
        return switch (stack.getUseAnimation()) {
            case BLOCK -> HeldTuning.SHIELD;
            case TRIDENT, SPEAR -> HeldTuning.TRIDENT;
            default -> null;
        };
    }

    private void updateOffhandUse(AbstractClientPlayer player, ItemStack stack, float partialTick) {
        if (offhandUseItem != stack.getItem()) {
            offhandUseItem = stack.getItem();
            offhandUseProgress = 0.0f;
            offhandUseElapsed = 0.0f;
        }
        ItemUseAnimation useAnimation = stack.getUseAnimation();
        boolean using = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.OFF_HAND;
        boolean timed = using && (isConsumable(useAnimation) || useAnimation == ItemUseAnimation.BRUSH
                || useAnimation == ItemUseAnimation.TOOT_HORN);
        boolean active = timed || using && heldModel(stack);
        long now = System.nanoTime();
        float delta = offhandUseFrame == 0 || now - offhandUseFrame > 1_000_000_000L
                ? 0.0f : Math.min(0.1f, (now - offhandUseFrame) * 0.000000001f);
        offhandUseFrame = now;
        offhandUseProgress = Math.max(0.0f, Math.min(1.0f, offhandUseProgress + (active ? 5.0f : -5.0f) * delta));
        if (timed) {
            offhandUseElapsed = Math.max(0.0f, player.getTicksUsingItem() + partialTick) / 20.0f;
        } else if (offhandUseProgress <= 0.0f) {
            offhandUseElapsed = 0.0f;
        }
    }

    private static boolean heldModel(ItemStack stack) {
        ItemUseAnimation animation = stack.getUseAnimation();
        return ArmTweaksModule.hasSpecialPose(stack) || animation == ItemUseAnimation.BLOCK;
    }

    private static void placeInHand(PoseStack pose, int side) {
        alignWithArm(pose, side);
        pose.translate(side / 16.0f, 2.0f / 16.0f, -10.0f / 16.0f);
    }

    private static void alignWithArm(PoseStack pose, int side) {
        pose.translate(-side * 5.0f / 16.0f, 2.0f / 16.0f, 0.0f);
        pose.mulPose(Axis.ZP.rotation(side * 0.1f));
        pose.mulPose(Axis.XP.rotationDegrees(-90.0f));
        pose.mulPose(Axis.YP.rotationDegrees(180.0f));
    }

    private void renderOnTopFace(AbstractClientPlayer player, ItemStack stack, Matrix4f matrix,
                                 Matrix4f relativePose, boolean mirrored, int side, float flatScale,
                                 SubmitNodeCollector collector, int light) {
        PoseStack rest = new PoseStack();
        rest.mulPose(relativePose);
        if (mirrored) rest.scale(-1.0f, 1.0f, 1.0f);
        alignWithArm(rest, side);
        Matrix4f arm = rest.last().pose();
        int face = offhandFace;
        float best = faceUp(arm, face) + 0.25f;
        for (int candidate = 0; candidate < 4 && !offhandFaceLocked; candidate++) {
            float up = faceUp(arm, candidate);
            if (up > best) {
                best = up;
                face = candidate;
            }
        }
        offhandFace = face;
        PoseStack pose = new PoseStack();
        pose.mulPose(matrix);
        if (mirrored) pose.scale(-1.0f, 1.0f, 1.0f);
        alignWithArm(pose, side);
        pose.translate(side / 16.0f, 0.0f, -10.0f / 16.0f);
        pose.mulPose(Axis.ZP.rotationDegrees(face * 90.0f));
        pose.translate(0.0f, 2.0f / 16.0f, 0.0f);
        PoseStack itemPose = new PoseStack();
        itemPose.mulPose(withoutStretch(pose.last().pose()));
        AABB bounds = groundBounds(stack);
        ItemDisplayContext context;
        if (bounds.getZsize() <= 0.0625) {
            itemPose.scale(flatScale, flatScale, flatScale);
            context = side > 0 ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        } else {
            itemPose.translate(0.0f, (float) -bounds.minY, (float) -bounds.minZ);
            context = ItemDisplayContext.GROUND;
        }
        GlassHandsRenderer.INSTANCE.renderViewModelItem(() ->
                Minecraft.getInstance().gameRenderer.itemInHandRenderer.renderItem(player, stack, context,
                        itemPose, collector, light));
    }

    private static Matrix4f withoutStretch(Matrix4f matrix) {
        float scale = (float) Math.cbrt(Math.abs(matrix.determinant3x3()));
        Vector3f up = matrix.getColumn(1, new Vector3f()).normalize();
        Vector3f right = matrix.getColumn(0, new Vector3f());
        right.sub(new Vector3f(up).mul(right.dot(up))).normalize();
        Vector3f forward = new Vector3f(right).cross(up);
        if (matrix.determinant3x3() < 0.0f) forward.negate();
        right.mul(scale);
        up.mul(scale);
        forward.mul(scale);
        return new Matrix4f(
                right.x, right.y, right.z, 0.0f,
                up.x, up.y, up.z, 0.0f,
                forward.x, forward.y, forward.z, 0.0f,
                matrix.m30(), matrix.m31(), matrix.m32(), 1.0f);
    }

    private AABB groundBounds(ItemStack stack) {
        return groundBounds.computeIfAbsent(stack.getItem(), item -> {
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.getItemModelResolver().updateForTopItem(boundsState, stack, ItemDisplayContext.GROUND,
                    minecraft.level, null, 0);
            return boundsState.getModelBoundingBox();
        });
    }

    private static float faceUp(Matrix4f arm, int face) {
        float angle = (float) Math.toRadians(face * 90.0f);
        return arm.transformDirection(new Vector3f(-(float) Math.sin(angle), (float) Math.cos(angle), 0.0f),
                new Vector3f()).normalize().y();
    }

    private static float offhandSwing(AbstractClientPlayer player, float partialTick) {
        return player.swingingArm == InteractionHand.OFF_HAND ? player.getAttackAnim(partialTick) : 0.0f;
    }

    private void renderConsumableOffhand(AbstractClientPlayer player, ItemStack stack,
                                                SubmitNodeCollector collector, int light,
                                                Matrix4f cameraPose, Matrix4f relativePose, HumanoidArm arm,
                                                ItemUseAnimation useAnimation, float progress,
                                                float elapsedSeconds, boolean armAlreadyRendered,
                                                Matrix4f cachedUsePose) {
        boolean drinking = useAnimation == ItemUseAnimation.DRINK;
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        Matrix4f usePose = cachedUsePose == null
                ? consumableMotion(relativePose, arm, useAnimation, progress, elapsedSeconds)
                : cachedUsePose;
        Matrix4f matrix = new Matrix4f(cameraPose).mul(usePose).mul(relativePose);
        if (!armAlreadyRendered) {
            PoseStack hand = new PoseStack();
            hand.mulPose(matrix);
            if (player.getMainArm() == HumanoidArm.LEFT) hand.scale(-1.0f, 1.0f, 1.0f);
            renderArmAt(player, collector, hand, arm, light);
        }
        renderOnTopFace(player, stack, matrix, relativePose, player.getMainArm() == HumanoidArm.LEFT, side,
                drinking ? 0.75f : 0.65f, collector, light);
    }

    private static Matrix4f consumableMotion(Matrix4f relativePose, HumanoidArm arm,
                                             ItemUseAnimation useAnimation, float progress,
                                             float elapsedSeconds) {
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        return pivotMotion(relativePose, side, ConsumableMotion.sample(elapsedSeconds, progress,
                useAnimation == ItemUseAnimation.DRINK));
    }

    private static Matrix4f pivotMotion(Matrix4f relativePose, int side, ConsumableMotion.Pose motion) {
        float pivotX = relativePose.m30() + side * 0.22f;
        float pivotY = relativePose.m31() - 0.18f;
        float pivotZ = relativePose.m32() + 0.12f;
        PoseStack usePose = new PoseStack();
        usePose.translate(side * motion.x() / 16.0f, motion.y() / 16.0f, motion.z() / 16.0f);
        usePose.rotateAround(Axis.ZP.rotationDegrees(-side * motion.roll()), pivotX, pivotY, pivotZ);
        usePose.rotateAround(Axis.YP.rotationDegrees(side * motion.yaw()), pivotX, pivotY, pivotZ);
        usePose.rotateAround(Axis.XP.rotationDegrees(-motion.pitch()), pivotX, pivotY, pivotZ);
        return new Matrix4f(usePose.last().pose());
    }

    private static Matrix4f utilityMotion(Matrix4f relativePose, ItemUseAnimation animation, int side,
                                          float progress, float elapsedSeconds, float placeSwing) {
        PoseStack pose = new PoseStack();
        if (progress > 0.0f) {
            ConsumableMotion.Pose motion = switch (animation) {
                case BRUSH -> ConsumableMotion.brush(elapsedSeconds, progress);
                case TOOT_HORN -> ConsumableMotion.horn(elapsedSeconds, progress);
                default -> null;
            };
            if (motion != null) pose.mulPose(pivotMotion(relativePose, side, motion));
        }
        if (placeSwing > 0.0f) {
            float reach = (float) Math.sin(Math.sqrt(placeSwing) * Math.PI);
            float lift = (float) Math.sin(Math.sqrt(placeSwing) * Math.PI * 2.0);
            float twist = (float) Math.sin(placeSwing * placeSwing * Math.PI);
            pose.translate(-side * 0.09f * reach, 0.05f * lift, -0.14f * reach);
            pose.mulPose(Axis.XP.rotationDegrees(-26.0f * reach));
            pose.mulPose(Axis.YP.rotationDegrees(side * 8.0f * twist));
            pose.mulPose(Axis.ZP.rotationDegrees(-side * 9.0f * twist));
        }
        return new Matrix4f(pose.last().pose());
    }

    private static boolean isConsumable(ItemUseAnimation animation) {
        return animation == ItemUseAnimation.EAT || animation == ItemUseAnimation.DRINK;
    }

    private static float transitionWeight(String action, float time, float duration,
                                          boolean inspectHeld, boolean inspectLooping) {
        float entry = "draw".equals(action) || inspectLooping ? 1.0f
                : Math.min(1.0f, time / 0.055f);
        float exitWindow = Math.min(0.28f, duration * 0.3f);
        float exit = inspectHeld && action.startsWith("inspect") ? 1.0f
                : Math.max(0.0f, Math.min(1.0f, (duration - time) / exitWindow));
        entry = 1.0f - (1.0f - entry) * (1.0f - entry) * (1.0f - entry);
        exit = exit * exit * exit * (exit * (exit * 6.0f - 15.0f) + 10.0f);
        return Math.max(0.0f, entry * exit);
    }

    private static void renderArm(AbstractClientPlayer player, SubmitNodeCollector collector,
                                  PoseStack base, Matrix4f handPose, HumanoidArm arm, int light,
                                  boolean mirrored) {
        if (handPose == null) return;
        PoseStack stack = new PoseStack();
        stack.mulPose(base.last().pose());
        stack.mulPose(handPose);
        stack.mulPose(Axis.ZP.rotationDegrees(180.0f));
        if (mirrored) stack.scale(-1.0f, 1.0f, 1.0f);
        renderArmAt(player, collector, stack, arm, light);
    }

    private static void renderArmAt(AbstractClientPlayer player, SubmitNodeCollector collector,
                                    PoseStack stack, HumanoidArm arm, int light) {
        AvatarRenderer<AbstractClientPlayer> renderer = Minecraft.getInstance()
                .getEntityRenderDispatcher().getPlayerRenderer(player);
        Identifier skin = player.getSkin().body().texturePath();
        if (arm == HumanoidArm.RIGHT) {
            boolean sleeve = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
            renderer.renderRightHand(stack, collector, light, skin, sleeve);
        } else {
            boolean sleeve = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
            renderer.renderLeftHand(stack, collector, light, skin, sleeve);
        }
    }

    private KnifeModel loadModel(KnifeVariant variant) {
        String key = ROOT + "/" + variant.model;
        KnifeModel cached = models.get(key);
        if (cached != null || failedAssets.contains(key)) return cached;
        KnifeModel model = loadModel(ROOT, variant.model);
        if (model == null) return null;
        if (variant.borrowsAnimation()) {
            KnifeModel source = loadModel(BORROWED_ROOT, variant.model);
            KnifeAnimation animation = loadAnimation(variant);
            if (source != null && animation != null) retarget(model, source, animation, variant.boneAliases);
        }
        return model;
    }

    private static void retarget(KnifeModel model, KnifeModel source, KnifeAnimation animation,
                                 Map<String, String> aliases) {
        for (String bone : animation.boneNames()) {
            String target = aliases.getOrDefault(bone, bone);
            float[] animatedPivot = source.pivot(bone);
            float[] targetPivot = model.pivot(target);
            if (animatedPivot == null || targetPivot == null) continue;
            boolean moved = Math.abs(animatedPivot[0] - targetPivot[0]) > 0.0001f
                    || Math.abs(animatedPivot[1] - targetPivot[1]) > 0.0001f
                    || Math.abs(animatedPivot[2] - targetPivot[2]) > 0.0001f;
            if (moved || !target.equals(bone)) model.alias(target, bone, moved ? animatedPivot : null);
        }
    }

    private KnifeModel loadModel(String root, String name) {
        String key = root + "/" + name;
        if (failedAssets.contains(key)) return null;
        try {
            KnifeModel model = models.get(key);
            if (model == null) {
                model = new KnifeModel(root, name);
                models.put(key, model);
            }
            return model;
        } catch (Exception exception) {
            failedAssets.add(key);
            Raven.LOGGER.error("Failed to load CS2 knife model {}", key, exception);
            return null;
        }
    }

    private static boolean nativeInspect(KnifeVariant variant, String action) {
        return variant.borrowsAnimation()
                && action != null && action.startsWith("inspect") && !action.equals(variant.borrowedInspect());
    }

    private KnifeAnimation.Clip clip(KnifeVariant variant, KnifeAnimation animation, String name) {
        if (name == null) return null;
        if (name.equals(variant.borrowedInspect())) return borrowedInspect(variant, animation);
        if (!nativeInspect(variant, name)) return animation.clip(name);
        KnifeAnimation inspect = inspectAnimation(variant);
        return inspect == null ? null : inspect.clip(name);
    }

    private KnifeAnimation.Clip borrowedInspect(KnifeVariant variant, KnifeAnimation animation) {
        KnifeAnimation.Clip cached = borrowedInspects.get(variant);
        if (cached != null) return cached;
        KnifeAnimation.Clip motion = animation.clip("inspect");
        KnifeAnimation own = inspectAnimation(variant);
        KnifeAnimation.Clip sounds = own == null ? null : own.clip(variant.borrowedInspect());
        if (motion == null || sounds == null) return motion;
        KnifeAnimation.Clip timing;
        try {
            timing = new KnifeAnimation(BORROWED_ROOT, variant.model).clip("inspect");
        } catch (Exception exception) {
            Raven.LOGGER.error("Failed to load CS2 knife animation {}", variant.model, exception);
            timing = motion;
        }
        KnifeAnimation.Clip clip = KnifeAnimation.withSounds(motion, sounds, timing);
        borrowedInspects.put(variant, clip);
        return clip;
    }

    private KnifeAnimation inspectAnimation(KnifeVariant variant) {
        if (!variant.borrowsAnimation()) return loadAnimation(variant);
        String key = ROOT + "/" + variant.model;
        if (failedAssets.contains(key)) return null;
        try {
            KnifeAnimation animation = animations.get(key);
            if (animation == null) {
                animation = new KnifeAnimation(ROOT, variant.model);
                animations.put(key, animation);
            }
            return animation;
        } catch (Exception exception) {
            failedAssets.add(key);
            Raven.LOGGER.error("Failed to load CS2 knife animation {}", key, exception);
            return null;
        }
    }

    private KnifeAnimation loadAnimation(KnifeVariant variant) {
        String root = variant.borrowsAnimation() ? BORROWED_ROOT : ROOT;
        String key = root + "/" + variant.model;
        if (failedAssets.contains(key)) return null;
        try {
            KnifeAnimation animation = animations.get(key);
            if (animation == null) {
                animation = new KnifeAnimation(root, variant.model);
                if (variant.borrowsAnimation()) {
                    animation.borrowSounds(new KnifeAnimation(ROOT, variant.model));
                }
                animations.put(key, animation);
                KnifeAnimation.Clip ownSwing = animation.clip("melee_1");
                boolean swingsOwnBlade = ownSwing != null && ownSwing.bones().containsKey(variant.model);
                if (!variant.borrowsAnimation() && !swingsOwnBlade) {
                    KnifeAnimation combo = loadAnimation(KnifeVariant.SKELETON);
                    if (combo != null) animation.borrowClips(combo, COMBO_CLIPS, COMBO_BONES);
                }
            }
            return animation;
        } catch (Exception exception) {
            failedAssets.add(key);
            Raven.LOGGER.error("Failed to load CS2 knife animation {}", key, exception);
            return null;
        }
    }
}
