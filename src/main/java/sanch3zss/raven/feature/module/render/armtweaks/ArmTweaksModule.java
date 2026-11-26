package sanch3zss.raven.feature.module.render.armtweaks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.util.*;
import net.minecraft.resources.Identifier;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.input.KeyAction;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.render.*;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.BindMode;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;
import sanch3zss.raven.feature.module.render.armtweaks.SwingAnimationType;
import sanch3zss.raven.feature.module.render.armtweaks.knife.CsAgent;
import sanch3zss.raven.feature.module.render.armtweaks.knife.GloveSkin;
import sanch3zss.raven.feature.module.render.armtweaks.knife.GloveType;
import sanch3zss.raven.feature.module.render.armtweaks.knife.KnifeRenderer;
import sanch3zss.raven.feature.module.render.armtweaks.knife.HeldTuning;
import sanch3zss.raven.feature.module.render.armtweaks.knife.KnifeSkin;
import sanch3zss.raven.feature.module.render.armtweaks.knife.KnifeVariant;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"Arm Tweaks", "Hand Tweaks", "Swing Animation", "View Model"})
public class ArmTweaksModule extends Module {
    public final ExpandableSetting swingAnimationGroup;
    public final ModeSetting<SwingAnimationType> swingAnimationType;
    public final NumberSetting swingPower;
    public final NumberSetting swingSpeed;
    public final NumberSetting swingInclination;
    public final ExpandableSetting viewModelGroup;
    public final BooleanSetting moveBothHands;
    public final NumberSetting translateX;
    public final NumberSetting translateY;
    public final NumberSetting translateZ;
    public final NumberSetting rightHandTranslateX;
    public final NumberSetting leftHandTranslateX;
    public final NumberSetting rightHandTranslateY;
    public final NumberSetting leftHandTranslateY;
    public final NumberSetting rightHandTranslateZ;
    public final NumberSetting leftHandTranslateZ;
    public final ExpandableSetting glassHandsGroup;
    public final ColorSetting handColor;
    public final NumberSetting handBlur;
    public final NumberSetting handTextureMix;
    public final ModeSetting<KnifeVariant> knife;
    public final Map<KnifeVariant, ModeSetting<KnifeSkin>> knifeSkins = new EnumMap<>(KnifeVariant.class);
    public final NumberSetting knifePattern;
    public final NumberSetting knifeWear;
    public final NumberSetting knifeSpeed;
    public final BooleanSetting knifeSounds;
    public final BooleanSetting cs2Hands;
    public final ModeSetting<CsAgent> agent;
    public final ModeSetting<GloveType> gloveType;
    public final Map<GloveType, ModeSetting<GloveSkin>> gloveSkins = new EnumMap<>(GloveType.class);
    public final NumberSetting glovePattern;
    public final NumberSetting gloveWear;
    public final KeybindSetting knifeInspect;
    public final Mc mc;

    public ArmTweaksModule() {
        super(ModuleTab.VISUALS, "ArmTweaks");
        this.swingAnimationGroup = new ExpandableSetting(Lang.ARM_TWEAKS_SWING_ANIMATION, Lang.ARM_TWEAKS_SWING_ANIMATION_DESC);
        this.swingAnimationType = new ModeSetting(Lang.ARM_TWEAKS_SWING_ANIMATION_TYPE).values(SwingAnimationType.class);
        this.swingPower = new NumberSetting(Lang.ARM_TWEAKS_SWING_ANIMATION_POWER).currentValue(15.0f).range(1.0f, 15.0f).step(0.05f)
                .visible(() -> !this.swingAnimationType.isSelected(SwingAnimationType.TYPE_4)
                        && !this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE));
        this.swingSpeed = new NumberSetting(Lang.ARM_TWEAKS_SWING_ANIMATION_SPEED).currentValue(2.0f).range(1.0f, 10.0f).step(1.0f)
                .visible(() -> !this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE));
        this.swingInclination = new NumberSetting(Lang.ARM_TWEAKS_SWING_ANIMATION_INCLINATION).currentValue(25.0f).range(10.0f, 70.0f).step(1.0f).visible(() -> {
            return Boolean.valueOf(this.swingAnimationType.isSelected(SwingAnimationType.TYPE_2)
                    || this.swingAnimationType.isSelected(SwingAnimationType.TYPE_4));
        });
        this.viewModelGroup = new ExpandableSetting(Lang.ARM_TWEAKS_VIEW_MODEL, Lang.ARM_TWEAKS_VIEW_MODEL_DESC);
        this.moveBothHands = new BooleanSetting(Lang.ARM_TWEAKS_VIEW_MODEL_MOVE_BOTH_HANDS).setValue(true);
        NumberSetting step = new NumberSetting(Lang.ARM_TWEAKS_VIEW_MODEL_TRANSLATE_X, Lang.ARM_TWEAKS_VIEW_MODEL_TRANSLATE_X_DESC).currentValue(0.0f).range(-2.0f, 2.0f).step(0.1f);
        BooleanSetting booleanSetting3 = this.moveBothHands;
        Objects.requireNonNull(booleanSetting3);
        this.translateX = step.visible(booleanSetting3::isValue);
        NumberSetting step2 = new NumberSetting(Lang.ARM_TWEAKS_VIEW_MODEL_TRANSLATE_Y, Lang.ARM_TWEAKS_VIEW_MODEL_TRANSLATE_Y_DESC).currentValue(0.0f).range(-2.0f, 2.0f).step(0.1f);
        BooleanSetting booleanSetting2 = this.moveBothHands;
        Objects.requireNonNull(booleanSetting2);
        this.translateY = step2.visible(booleanSetting2::isValue);
        NumberSetting step3 = new NumberSetting(Lang.ARM_TWEAKS_VIEW_MODEL_TRANSLATE_Z, Lang.ARM_TWEAKS_VIEW_MODEL_TRANSLATE_Z_DESC).currentValue(0.0f).range(-2.0f, 2.0f).step(0.1f);
        BooleanSetting booleanSetting = this.moveBothHands;
        Objects.requireNonNull(booleanSetting);
        this.translateZ = step3.visible(booleanSetting::isValue);
        this.rightHandTranslateX = new NumberSetting(Lang.ARM_TWEAKS_VIEW_MODEL_RIGHT_HAND_TRANSLATE_X).currentValue(0.0f).range(-2.0f, 2.0f).step(0.1f).visible(() -> {
            return Boolean.valueOf(!this.moveBothHands.isValue());
        });
        this.leftHandTranslateX = new NumberSetting(Lang.ARM_TWEAKS_VIEW_MODEL_LEFT_HAND_TRANSLATE_X).currentValue(0.0f).range(-2.0f, 2.0f).step(0.1f).visible(() -> {
            return Boolean.valueOf(!this.moveBothHands.isValue());
        });
        this.rightHandTranslateY = new NumberSetting(Lang.ARM_TWEAKS_VIEW_MODEL_RIGHT_HAND_TRANSLATE_Y).currentValue(0.0f).range(-2.0f, 2.0f).step(0.1f).visible(() -> {
            return Boolean.valueOf(!this.moveBothHands.isValue());
        });
        this.leftHandTranslateY = new NumberSetting(Lang.ARM_TWEAKS_VIEW_MODEL_LEFT_HAND_TRANSLATE_Y).currentValue(0.0f).range(-2.0f, 2.0f).step(0.1f).visible(() -> {
            return Boolean.valueOf(!this.moveBothHands.isValue());
        });
        this.rightHandTranslateZ = new NumberSetting(Lang.ARM_TWEAKS_VIEW_MODEL_RIGHT_HAND_TRANSLATE_Z).currentValue(0.0f).range(-2.0f, 2.0f).step(0.1f).visible(() -> {
            return Boolean.valueOf(!this.moveBothHands.isValue());
        });
        this.leftHandTranslateZ = new NumberSetting(Lang.ARM_TWEAKS_VIEW_MODEL_LEFT_HAND_TRANSLATE_Z).currentValue(0.0f).range(-2.0f, 2.0f).step(0.1f).visible(() -> {
            return Boolean.valueOf(!this.moveBothHands.isValue());
        });
        this.glassHandsGroup = new ExpandableSetting(Lang.ARM_TWEAKS_GLASS_HANDS, Lang.ARM_TWEAKS_GLASS_HANDS_DESC);
        this.handColor = new ColorSetting(Lang.ARM_TWEAKS_GLASS_HANDS_COLOR).value(0xFF8A98FF);
        this.handBlur = new NumberSetting(Lang.ARM_TWEAKS_GLASS_HANDS_BLUR).currentValue(10.0f).range(0.0f, 30.0f).step(1.0f);
        this.handTextureMix = new NumberSetting(Lang.ARM_TWEAKS_GLASS_HANDS_TEXTURE_MIX).currentValue(0.5f).range(0.0f, 0.5f).step(0.1f);
        this.knife = new ModeSetting<KnifeVariant>(Translation.clearText("Нож"))
                .values(KnifeVariant.class)
                .migrateSavedName(KnifeVariant::migrateSavedName)
                .visible(() -> this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE));
        for (KnifeVariant variant : KnifeVariant.values()) {
            if (variant.skins().length > 1) knifeSkins.put(variant, skinSetting(variant));
        }
        this.knifePattern = new NumberSetting(Translation.clearText("Паттерн"))
                .currentValue(0.0f).range(0.0f, 1000.0f).step(1.0f)
                .visible(() -> this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE)
                        && activeKnifeSkin() != KnifeSkin.DEFAULT);
        this.knifeWear = new NumberSetting(Translation.clearText("Износ"))
                .currentValue(0.0f).range(0.0f, 1.0f).step(0.01f)
                .visible(() -> this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE)
                        && activeKnifeSkin() != KnifeSkin.DEFAULT);
        this.knifeSpeed = new NumberSetting(Translation.clearText("Скорость анимации ножа"))
                .currentValue(1.0f).range(0.5f, 2.0f).step(0.05f)
                .visible(() -> this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE));
        this.knifeSounds = new BooleanSetting(Translation.clearText("Звуки ножа")).setValue(true)
                .visible(() -> this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE));
        this.cs2Hands = new BooleanSetting(Translation.clearText("Реализм"))
                .visible(() -> this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE));
        this.agent = new ModeSetting<CsAgent>(Translation.clearText("Агент"))
                .values(CsAgent.class)
                .visible(this::showsGloves);
        this.gloveType = new ModeSetting<GloveType>(Translation.clearText("Перчатки"))
                .values(GloveType.class)
                .visible(this::showsGloves);
        for (GloveType type : GloveType.values()) {
            if (type == GloveType.DEFAULT) continue;
            GloveSkin[] skins = type.skins();
            this.gloveSkins.put(type, new ModeSetting<GloveSkin>(Translation.clearText("Скин перчаток"))
                    .options(skins).currentValue(skins[0])
                    .visible(() -> showsGloves() && this.gloveType.isSelected(type)));
        }
        this.glovePattern = new NumberSetting(Translation.clearText("Паттерн перчаток"))
                .currentValue(0.0f).range(0.0f, 1000.0f).step(1.0f)
                .visible(() -> showsGloves() && !this.gloveType.isSelected(GloveType.DEFAULT));
        this.gloveWear = new NumberSetting(Translation.clearText("Износ перчаток"))
                .currentValue(0.0f).range(0.0f, 1.0f).step(0.01f)
                .visible(() -> showsGloves() && !this.gloveType.isSelected(GloveType.DEFAULT));
        this.knifeInspect = new KeybindSetting(Translation.clearText("Осмотреть нож")) {
            @Override
            public void setType(BindMode bindMode) {
                super.setType(BindMode.HOLD);
            }
        }
                .visible(() -> this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE));
        this.mc = Mc.INSTANCE;
        List<Setting> swingSettings = new ArrayList<>(List.of(this.swingAnimationType, this.swingPower,
                this.swingSpeed, this.swingInclination, this.knife));
        swingSettings.addAll(knifeSkins.values());
        swingSettings.addAll(List.of(this.knifePattern, this.knifeWear, this.knifeSpeed, this.knifeSounds, this.knifeInspect,
                this.cs2Hands, this.agent, this.gloveType));
        swingSettings.addAll(gloveSkins.values());
        swingSettings.addAll(List.of(this.glovePattern, this.gloveWear));
        this.swingAnimationGroup.setSubSettings(swingSettings);
        this.viewModelGroup.setSubSettings(List.of(this.moveBothHands, this.translateX, this.translateY, this.translateZ, this.rightHandTranslateX, this.rightHandTranslateY, this.rightHandTranslateZ, this.leftHandTranslateX, this.leftHandTranslateY, this.leftHandTranslateZ));
        this.glassHandsGroup.setSubSettings(List.of(
                this.handColor,
                this.handColor.themeSyncSetting(),
                this.handBlur,
                this.handTextureMix
        ));
        this.knifeInspect.consumer(BindMode.HOLD, KnifeRenderer.INSTANCE::inspect);
        addSettings(this.swingAnimationGroup, this.viewModelGroup, this.glassHandsGroup);
        register(HeldItemRenderEvent.class, event2 -> {
            if (this.viewModelGroup.isValue() && isState() && this.mc.isWorldLoaded()) {
                if (!hasSpecialPose(event2.stack())) {
                    applyKnifeViewModel(event2.matrices(), getArmForHand(event2.hand()));
                }
            }
        });
        register(HandSwingEvent.class, event4 -> {
            if (this.mc.getPlayer() != null && shouldOverrideSwing(this.mc.getPlayer().getItemInHand(event4.hand()))) {
                int iMax = Math.max(1, 12 - ((int) Math.max(1.0f, Math.min(this.swingSpeed.currentValue(), 10.0f))));
                if (MobEffectUtil.hasDigSpeed(this.mc.getPlayer())) {
                    iMax -= 1 + MobEffectUtil.getDigSpeedAmplification(this.mc.getPlayer());
                }
                if (this.mc.getPlayer().hasEffect(MobEffects.MINING_FATIGUE)) {
                    iMax += (1 + this.mc.getPlayer().getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2;
                }
                event4.swingSpeed(Math.max(1, iMax));
                event4.cancel();
            }
        });
        register(ArmRenderEvent.class, event -> {
            if (shouldOverrideSwing(event.stack())
                    && (isActiveSwingHand(event) || hasIdleSwingPose(event))) {
                PoseStack matrixStack = event.matrixStack();
                if (this.swingAnimationGroup.isValue()) {
                    float fSwingProgress = event.swingProgress();
                    float fSin = (float) Math.sin(((double) fSwingProgress) * 1.5707963267948966d * 2.0d);
                    float swingFactor = normalizeRange(this.swingPower.currentValue(), 1.0f, 15.0f) * getSwingMultiplier((SwingAnimationType) this.swingAnimationType.currentValue());
                    float side = event.arm() == HumanoidArm.RIGHT ? 1.0f : -1.0f;
                    switch ((SwingAnimationType) this.swingAnimationType.currentValue()) {
                        case TYPE_1:
                            float fSin2 = Mth.sin(fSwingProgress * fSwingProgress * 3.1415927f);
                            float fSin3 = Mth.sin(Mth.sqrt(fSwingProgress) * 3.1415927f);
                            matrixStack.mulPose(Axis.YP.rotationDegrees(side * (45.0f + (fSin2 * (-20.0f) * swingFactor))));
                            matrixStack.mulPose(Axis.ZP.rotationDegrees(side * fSin3 * (-20.0f) * swingFactor));
                            matrixStack.mulPose(Axis.XP.rotationDegrees(fSin3 * (-80.0f) * swingFactor));
                            matrixStack.mulPose(Axis.YP.rotationDegrees(side * -45.0f));
                            event.cancel();
                            break;
                        case TYPE_2:
                            matrixStack.translate(0.0f, 0.2f, 0.0f);
                            matrixStack.mulPose(Axis.XP.rotationDegrees(this.swingInclination.currentValue()));
                            matrixStack.mulPose(Axis.YP.rotationDegrees(side * 45.0f));
                            matrixStack.mulPose(Axis.ZP.rotationDegrees(side * -85.0f));
                            matrixStack.mulPose(Axis.XP.rotationDegrees((-90.0f) - ((80.0f * swingFactor) * fSin)));
                            event.cancel();
                            break;
                        case TYPE_4:
                            float lunge = this.swingInclination.currentValue();
                            matrixStack.translate(side * 0.79f, -0.03f, -0.58f);
                            matrixStack.mulPose(Axis.YP.rotationDegrees(side * 45.0f));
                            matrixStack.mulPose(Axis.YP.rotationDegrees(side * fSin * -(lunge * 0.5f)));
                            matrixStack.mulPose(Axis.ZP.rotationDegrees(side * fSin * -(lunge * 0.5f)));
                            matrixStack.mulPose(Axis.XP.rotationDegrees(fSin * -lunge));
                            matrixStack.translate(side * -0.5f, 0.2f, -0.3f);
                            matrixStack.mulPose(Axis.YP.rotationDegrees(side * 30.0f));
                            matrixStack.mulPose(Axis.XP.rotationDegrees(-80.0f));
                            matrixStack.mulPose(Axis.YP.rotationDegrees(side * 60.0f));
                            event.cancel();
                            break;
                        case TYPE_5:
                            float down = -0.26f * (this.swingPower.currentValue() / 15.0f)
                                    * Mth.sin(Mth.sqrt(fSwingProgress) * 3.1415927f);
                            matrixStack.translate(0.0f, down, 0.0f);
                            event.cancel();
                            break;
                        case HMI:
                        case COUNTER_STRIKE:
                            break;
                    }
                }
            }
        });
    }

    public boolean shouldModifySwing() {
        return isState() && this.swingAnimationGroup.isValue() && this.mc.isWorldLoaded()
                && !this.swingAnimationType.isSelected(SwingAnimationType.HMI)
                && !this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE);
    }

    public boolean isHmiMode() {
        return isState() && this.swingAnimationGroup.isValue() && this.mc.isWorldLoaded()
                && this.swingAnimationType.isSelected(SwingAnimationType.HMI);
    }

    public boolean shouldOverrideSwing(ItemStack stack) {
        if (!shouldModifySwing() || stack == null || this.mc.getPlayer() == null
                || this.mc.getPlayer().isAutoSpinAttack() || this.mc.getPlayer().isScoping()
                || this.mc.getPlayer().isUsingItem()) {
            return false;
        }
        if (shouldRenderKnife() && stack.is(ItemTags.SWORDS)) return false;
        return stack.isEmpty() || !stack.has(DataComponents.MAP_ID)
                && stack.getUseAnimation() == ItemUseAnimation.NONE
                && stack.getSwingAnimation().type() == net.minecraft.world.item.SwingAnimationType.WHACK;
    }

    public KnifeVariant activeKnifeVariant() {
        return knife.currentValue();
    }

    public boolean shouldRenderCs2Hands() {
        return shouldRenderKnife() && this.cs2Hands.isValue();
    }

    public KnifeSkin activeKnifeSkin() {
        ModeSetting<KnifeSkin> skin = knifeSkins.get(activeKnifeVariant());
        return skin == null ? KnifeSkin.DEFAULT : skin.currentValue();
    }

    public GloveSkin activeGloveSkin() {
        ModeSetting<GloveSkin> skin = gloveSkins.get(gloveType.currentValue());
        return skin == null ? null : skin.currentValue();
    }

    public String activeAgentId() {
        return agent.currentValue().model;
    }

    private boolean showsGloves() {
        return this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE) && this.cs2Hands.isValue();
    }

    private ModeSetting<KnifeSkin> skinSetting(KnifeVariant variant) {
        return new ModeSetting<KnifeSkin>(Translation.clearText("Скин ножа"))
                .options(variant.skins()).currentValue(KnifeSkin.DEFAULT)
                .visible(() -> this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE)
                        && this.knife.currentValue() == variant);
    }

    public static boolean hasSpecialPose(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ItemUseAnimation animation = stack.getUseAnimation();
        return animation == ItemUseAnimation.BOW || animation == ItemUseAnimation.CROSSBOW
                || animation == ItemUseAnimation.TRIDENT || animation == ItemUseAnimation.SPEAR;
    }

    private boolean isActiveSwingHand(ArmRenderEvent event) {
        InteractionHand swingingHand = this.mc.getPlayer().swingingArm;
        return (this.mc.getPlayer().swinging || event.swingProgress() > 0.0001f)
                && event.hand() == (swingingHand == null ? InteractionHand.MAIN_HAND : swingingHand);
    }

    private boolean hasIdleSwingPose(ArmRenderEvent event) {
        return event.hand() == InteractionHand.MAIN_HAND
                && (this.swingAnimationType.isSelected(SwingAnimationType.TYPE_2)
                || this.swingAnimationType.isSelected(SwingAnimationType.TYPE_4));
    }

    public boolean shouldRenderGlassHands() {
        return isState() && this.glassHandsGroup.isValue() && this.mc.isWorldLoaded();
    }

    public boolean shouldRenderKnife() {
        return isState() && this.swingAnimationGroup.isValue() && this.mc.isWorldLoaded()
                && this.swingAnimationType.isSelected(SwingAnimationType.COUNTER_STRIKE);
    }

    public void applyKnifeViewModel(PoseStack matrices, HumanoidArm arm) {
        if (!isState() || !this.viewModelGroup.isValue()) return;
        float x = this.translateX.currentValue();
        float y = this.translateY.currentValue();
        float z = this.translateZ.currentValue();
        if (!this.moveBothHands.isValue()) {
            boolean right = arm == HumanoidArm.RIGHT;
            x = right ? this.rightHandTranslateX.currentValue() : this.leftHandTranslateX.currentValue();
            y = right ? this.rightHandTranslateY.currentValue() : this.leftHandTranslateY.currentValue();
            z = right ? this.rightHandTranslateZ.currentValue() : this.leftHandTranslateZ.currentValue();
        } else if (arm == HumanoidArm.LEFT) {
            x = -x;
        }
        matrices.translate(x, y, z);
    }

    public void renderGlassHand(HumanoidArm arm, Identifier texture, Runnable renderer) {
        if (!shouldRenderGlassHands() || isHmiMode()) {
            renderer.run();
            return;
        }
        GlassHandsRenderer.INSTANCE.renderHand(arm, texture, this.handColor.getFirstColor(),
                this.handColor.getGradientEndColor(), this.handTextureMix.currentValue(), renderer);
    }

    public static float normalizeRange(float f, float f2, float f3) {
        if (f3 <= f2) {
            return 0.0f;
        }
        return Mth.clamp((f - f2) / (f3 - f2), 0.0f, 1.0f);
    }

    public static float getSwingMultiplier(SwingAnimationType swingAnimationType) {
        return swingAnimationType == SwingAnimationType.TYPE_2 ? 0.95f : 1.0f;
    }

    public HumanoidArm getArmForHand(InteractionHand hand) {
        HumanoidArm mainArm = this.mc.getPlayer().getMainArm();
        if (hand == InteractionHand.MAIN_HAND) {
            return mainArm;
        }
        return mainArm == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }
}
