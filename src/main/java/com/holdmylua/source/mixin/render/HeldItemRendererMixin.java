
package com.holdmylua.source.mixin.render;

import com.holdmylua.source.LuaTestHMI;
import com.holdmylua.source.access.AlternateBlockRenderer;
import com.holdmylua.source.access.ItemStackAccessor;
import com.holdmylua.source.access.LivingEntityAccessor;
import com.holdmylua.source.global.DispatcherStorage;
import com.holdmylua.source.global.GlobalsStorage;
import com.holdmylua.source.global.item_model.ItemModelContext;
import com.holdmylua.source.global.item_model.ItemModelStorage;
import com.holdmylua.source.lua_runtime.LuaScriptCache;
import com.holdmylua.source.lua_runtime.ScriptHolder;
import com.holdmylua.source.patricles.Particle;
import com.holdmylua.source.patricles.ParticleRenderManager;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sanch3zss.raven.integration.hmi.HmiModeBridge;

@Mixin(value={ItemInHandRenderer.class})
public abstract class HeldItemRendererMixin {
    @Unique
    boolean mainHandSwitchEvent = false;
    @Unique
    boolean offHandSwitchEvent = false;
    @Unique
    Item prevMainHand = Items.AIR;
    @Unique
    Item prevOffHand = Items.AIR;
    @Unique
    private final ArrayList<Particle> particles = new ArrayList();
    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    private ItemStack mainHandItem;
    @Shadow
    private ItemStack offHandItem;
    @Unique
    private boolean swingMHand = false;
    @Unique
    private boolean swingOHand = false;
    @Unique
    private float mainHandSwingProgress = 0.0f;
    @Unique
    private float offHandSwingProgress = 0.0f;

    @Shadow
    protected abstract void renderPlayerArm(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, float var5, HumanoidArm var6);

    @Shadow
    protected abstract void renderMap(PoseStack var1, SubmitNodeCollector var2, int var3, ItemStack var4);

    @Shadow
    public abstract void renderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, PoseStack var4, SubmitNodeCollector var5, int var6);

    @Shadow
    private void submitArmWithItem(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack item, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords) {
    }

    @Unique
    private void copyAppearanceComponents(ItemStack source, ItemStack target) {
        if (source.has(DataComponents.ENCHANTMENTS)) {
            target.set(DataComponents.ENCHANTMENTS, source.get(DataComponents.ENCHANTMENTS));
        }
        if (source.has(DataComponents.ITEM_MODEL)) {
            target.set(DataComponents.ITEM_MODEL, source.get(DataComponents.ITEM_MODEL));
        }
        if (source.has(DataComponents.CUSTOM_MODEL_DATA)) {
            target.set(DataComponents.CUSTOM_MODEL_DATA, source.get(DataComponents.CUSTOM_MODEL_DATA));
        }
        if (source.has(DataComponents.CUSTOM_DATA)) {
            target.set(DataComponents.CUSTOM_DATA, source.get(DataComponents.CUSTOM_DATA));
        }
    }

    @Unique
    private void applyArmMatrices(PoseStack matrices, int light, float equipProgress, float swingProgress, HumanoidArm arm) {
        boolean bl = arm != HumanoidArm.LEFT;
        float f = bl ? 1.0f : -1.0f;
        float g = Mth.sqrt((float)swingProgress);
        float h = -0.3f * Mth.sin((double)(g * (float)Math.PI));
        float i = 0.4f * Mth.sin((double)(g * ((float)Math.PI * 2)));
        float j = -0.4f * Mth.sin((double)(swingProgress * (float)Math.PI));
        matrices.translate(f * (h + 0.64000005f), i + -0.6f + equipProgress * -0.6f, j + -0.71999997f);
        matrices.mulPose((Quaternionfc)Axis.YP.rotationDegrees(f * 45.0f));
        float k = Mth.sin((double)(swingProgress * swingProgress * (float)Math.PI));
        float l = Mth.sin((double)(g * (float)Math.PI));
        matrices.mulPose((Quaternionfc)Axis.YP.rotationDegrees(f * l * 70.0f));
        matrices.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(f * k * -20.0f));
        LocalPlayer abstractClientPlayerEntity = this.minecraft.player;
        matrices.translate(f * -1.0f, 3.6f, 3.5f);
        matrices.mulPose((Quaternionfc)Axis.ZP.rotationDegrees(f * 120.0f));
        matrices.mulPose((Quaternionfc)Axis.XP.rotationDegrees(200.0f));
        matrices.mulPose((Quaternionfc)Axis.YP.rotationDegrees(f * -135.0f));
        matrices.translate(f * 5.6f, 0.0f, 0.0f);
    }

    @Unique
    private void itemPose(PoseStack matrices, ItemStack item, boolean bl, float swingProgress, AbstractClientPlayer player, boolean mainHand, InteractionHand hand, float equipProgress, float mainHandSwingProgress, float offHandSwingProgress, boolean mainHandSwitchEvent, boolean offHandSwitchEvent, boolean swingMHand, boolean swingOHand, boolean interact, boolean blockBreaking, List<Particle> particles) throws ScriptException, NoSuchMethodException {
        int l = bl ? 1 : -1;
        matrices.translate(0.5 * (double)l, -0.15, -0.85);
        matrices.rotateAround((Quaternionfc)Axis.XP.rotationDegrees(15.0f), 0.5f, 0.5f, 0.5f);
        matrices.scale(0.9f, 0.9f, 0.9f);
        ScriptHolder.itemScriptCache.execute(matrices, bl, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand, LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent, offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
        for (LuaScriptCache scriptCache : ScriptHolder.itemAddonsCache) {
            scriptCache.execute(matrices, bl, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand, LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent, offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
        }
    }

    @Unique
    private void mainHandPose(PoseStack matrices, ItemStack item, boolean bl, float swingProgress, float equipProgress, AbstractClientPlayer player, boolean mainHand, InteractionHand hand, float mainHandSwingProgress, float offHandSwingProgress, boolean mainHandSwitchEvent, boolean offHandSwitchEvent, boolean swingMHand, boolean swingOHand, boolean interact, boolean blockBreaking, List<Particle> particles) throws ScriptException, NoSuchMethodException {
        int l = bl ? 1 : -1;
        ScriptHolder.handRelativeScriptCache.execute(matrices, bl, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand, LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent, offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
        for (LuaScriptCache scriptCache : ScriptHolder.handRelativeAddonsCache) {
            scriptCache.execute(matrices, bl, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand, LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent, offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
        }
        if (!item.isEmpty()) {
            matrices.translate(1.5 * (double)l, -0.3, -0.6);
            matrices.rotateAround((Quaternionfc)Axis.XP.rotationDegrees(15.0f), 0.5f * (float)l, 0.5f, 0.5f);
            matrices.rotateAround((Quaternionfc)Axis.YP.rotationDegrees((float)(35 * l)), 0.5f * (float)l, 0.5f, 0.5f);
            matrices.rotateAround((Quaternionfc)Axis.ZP.rotationDegrees((float)(-65 * l)), 0.5f * (float)l, 0.5f, 0.5f);
            matrices.scale(0.9f, 0.9f, 0.9f);
        }
    }

    @Unique
    private void scenePoseMain(PoseStack matrices, ItemStack item, boolean bl, float swingProgress, float equipProgress, AbstractClientPlayer player, boolean mainHand, InteractionHand hand, float mainHandSwingProgress, float offHandSwingProgress, boolean mainHandSwitchEvent, boolean offHandSwitchEvent, boolean swingMHand, boolean swingOHand, boolean interact, boolean blockBreaking, List<Particle> particles) throws ScriptException, NoSuchMethodException {
        ScriptHolder.handScriptCache.execute(matrices, bl, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand, LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent, offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
        for (LuaScriptCache scriptCache : ScriptHolder.handAddonsCache) {
            scriptCache.execute(matrices, bl, GlobalsStorage.registry, swingProgress, item, player, hand, mainHand, LuaTestHMI.deltaTime, equipProgress, mainHandSwingProgress, offHandSwingProgress, mainHandSwitchEvent, offHandSwitchEvent, swingMHand, swingOHand, interact, blockBreaking, particles);
        }
        if (!item.isEmpty()) {
            matrices.translate(0.0, -0.35, 0.2);
        }
    }

    @Redirect(method={"submitHandsWithItems"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/ItemInHandRenderer;submitArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"))
    private void renderOverhaul(ItemInHandRenderer itemInHandRenderer, AbstractClientPlayer player, float tickProgress, float f2, InteractionHand hand, float swingProgress, ItemStack itemStack, float equipProgress, PoseStack matrices, SubmitNodeCollector orderedRenderCommandQueue, int light) throws ScriptException, NoSuchMethodException {
        if (!HmiModeBridge.isActive()) {
            this.submitArmWithItem(player, tickProgress, f2, hand, swingProgress, itemStack, equipProgress, matrices, orderedRenderCommandQueue, light);
            return;
        }
        if (!player.isScoping()) {
            ItemStack item = itemStack;
            ((ItemStackAccessor)(Object)item).hMI5_0$setTransform(-1);
            boolean bl = hand == InteractionHand.MAIN_HAND;
            boolean interact = false;
            boolean blockBreaking = false;
            if (bl && player.isUsingItem() && player.getUsedItemHand() != hand && (player.getOffhandItem().getUseAnimation() == ItemUseAnimation.BOW || player.getOffhandItem().getUseAnimation() == ItemUseAnimation.BOW || player.getOffhandItem().getUseAnimation() == ItemUseAnimation.CROSSBOW && !CrossbowItem.isCharged((ItemStack)player.getOffhandItem()))) {
                item = ItemStack.EMPTY;
            }
            if (!bl && player.isUsingItem() && player.getUsedItemHand() != hand && (player.getMainHandItem().getUseAnimation() == ItemUseAnimation.BOW || player.getMainHandItem().getUseAnimation() == ItemUseAnimation.CROSSBOW && !CrossbowItem.isCharged((ItemStack)player.getMainHandItem()))) {
                item = ItemStack.EMPTY;
            }
            if (bl) {
                boolean bl2 = this.mainHandSwitchEvent = item.getItem() != this.prevMainHand;
            }
            if (!bl) {
                this.offHandSwitchEvent = item.getItem() != this.prevOffHand;
            }
            matrices.pushPose();
            HmiModeBridge.applyViewModel((PoseStack)matrices, (InteractionHand)hand, (ItemStack)item);
            if (player instanceof LivingEntityAccessor) {
                float offHandProgress;
                float mainHandProgress;
                LivingEntityAccessor accessor = (LivingEntityAccessor)player;
                this.mainHandSwingProgress = mainHandProgress = accessor.hMI5_0$getMainHandSwingProgress((float)tickProgress);
                this.offHandSwingProgress = offHandProgress = accessor.hMI5_0$getOffHandSwingProgress((float)tickProgress);
                swingProgress = bl ? mainHandProgress : offHandProgress;
                this.swingMHand = accessor.hMI5_0$getMHandEvent();
                this.swingOHand = accessor.hMI5_0$getOHandEvent();
                interact = bl ? accessor.hMI5_0$getMInteract() : accessor.hMI5_0$getOInteract();
                blockBreaking = accessor.hMI5_0$getBlockBreak();
            }
            HumanoidArm arm = bl ? player.getMainArm() : player.getMainArm().getOpposite();
            boolean bl2 = arm == HumanoidArm.RIGHT;
            int l = bl2 ? 1 : -1;
            this.scenePoseMain((PoseStack)matrices, item, bl2, swingProgress, (float)equipProgress, (AbstractClientPlayer)player, bl, (InteractionHand)hand, this.mainHandSwingProgress, this.offHandSwingProgress, this.mainHandSwitchEvent, this.offHandSwitchEvent, this.swingMHand, this.swingOHand, interact, blockBreaking, this.particles);
            matrices.pushPose();
            this.mainHandPose((PoseStack)matrices, item, bl2, swingProgress, (float)equipProgress, (AbstractClientPlayer)player, bl, (InteractionHand)hand, this.mainHandSwingProgress, this.offHandSwingProgress, this.mainHandSwitchEvent, this.offHandSwitchEvent, this.swingMHand, this.swingOHand, interact, blockBreaking, this.particles);
            int combinedLight = LightCoordsUtil.lightCoordsWithEmission((int)light, (int)Block.byItem((Item)item.getItem()).defaultBlockState().getLightEmission());
            if (player.isInvisible()) {
                this.applyArmMatrices((PoseStack)matrices, combinedLight, 0.0f, 0.0f, arm);
            } else {
                this.renderPlayerArm((PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, combinedLight, 0.0f, 0.0f, arm);
            }
            matrices.popPose();
            matrices.pushPose();
            if (!(Block.byItem((Item)item.getItem()) == Blocks.AIR || Block.byItem((Item)item.getItem()).defaultBlockState().is(BlockTags.INSIDE_STEP_SOUND_BLOCKS) || Block.byItem((Item)item.getItem()).defaultBlockState().is(BlockTags.CROPS) || item.getUseAnimation() == ItemUseAnimation.EAT || item.is(Items.REDSTONE) || item.is(ItemTags.BANNERS) || item.is(ItemTags.SKULLS) || !GlobalsStorage.renderAsBlock.getOrDefault(item.getItem().toString(), true).booleanValue())) {
                swingProgress = 0.0f;
                BlockState blockState = Block.byItem((Item)item.getItem()).defaultBlockState();
                this.itemPose((PoseStack)matrices, item, bl2, swingProgress, (AbstractClientPlayer)player, bl, (InteractionHand)hand, (float)equipProgress, this.mainHandSwingProgress, this.offHandSwingProgress, this.mainHandSwitchEvent, this.offHandSwitchEvent, this.swingMHand, this.swingOHand, interact, blockBreaking, this.particles);
                matrices.translate(0.22 * (double)l, 0.25, 0.2);
                if (item.is(Items.LEVER) || blockState.is(BlockTags.BUTTONS)) {
                    blockState = (BlockState)blockState.setValue((Property)BlockStateProperties.ATTACH_FACE, (Comparable)AttachFace.FLOOR);
                }
                if (item.getHoverName().toString().toLowerCase().contains("torch") || Block.byItem((Item)item.getItem()) instanceof LanternBlock || blockState.is(BlockTags.ALL_HANGING_SIGNS)) {
                    matrices.translate(-0.05 * (double)l, 0.0, 0.0);
                    matrices.scale(1.75f, 1.75f, 1.75f);
                } else {
                    matrices.translate(-0.25 * (double)l, -0.05, 0.0);
                }
                matrices.pushPose();
                matrices.scale(0.3f, 0.3f, 0.3f);
                matrices.translate(-0.9 * (double)l, -0.45, -0.7);
                matrices.popPose();
                if (!bl2) {
                    matrices.translate(-0.3f, 0.0f, 0.0f);
                }
                matrices.scale(0.3f, 0.3f, 0.3f);
                matrices.translate(-0.9 * (double)l, -0.45, -0.7);
                if (item.is(Items.BELL)) {
                    BellBlockEntity bellBlockEntity = new BellBlockEntity(BlockPos.ZERO, Blocks.BELL.defaultBlockState());
                    BellRenderState state = new BellRenderState();
                    state.lightCoords = light;
                    this.minecraft.getBlockEntityRenderDispatcher().getRenderer((BlockEntity)bellBlockEntity).submit((BlockEntityRenderState)state, (PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, new CameraRenderState());
                    AlternateBlockRenderer.renderSingleBlockWithEmission((BlockState)Blocks.BELL.defaultBlockState().setValue((Property)BlockStateProperties.BELL_ATTACHMENT, (Comparable)BellAttachType.CEILING), (PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, (int)light, this.minecraft.level, (AbstractClientPlayer)player);
                } else {
                    if (blockState.hasProperty((Property)BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                        matrices.pushPose();
                        matrices.translate(0.0f, 1.0f, 0.0f);
                        AlternateBlockRenderer.renderSingleBlockWithEmission((BlockState)blockState.setValue((Property)BlockStateProperties.DOUBLE_BLOCK_HALF, (Comparable)DoubleBlockHalf.UPPER), (PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, (int)light, this.minecraft.level, (AbstractClientPlayer)player);
                        matrices.popPose();
                    }
                    AlternateBlockRenderer.renderSingleBlockWithEmission(blockState, (PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, (int)light, this.minecraft.level, (AbstractClientPlayer)player);
                }
                matrices.pushPose();
                if (!bl2) {
                    matrices.translate(1.0f, 0.0f, 0.0f);
                }
                ParticleRenderManager.draw(this.particles, (PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, "ITEM", (InteractionHand)hand, (int)light, (AbstractClientPlayer)player, (float)tickProgress);
                matrices.popPose();
            } else if (item.getUseAnimation() == ItemUseAnimation.BLOCK || item.getUseAnimation() == ItemUseAnimation.BRUSH || item.getUseAnimation() == ItemUseAnimation.TRIDENT) {
                ItemStack renderStack2;
                this.itemPose((PoseStack)matrices, item, bl2, swingProgress, (AbstractClientPlayer)player, bl, (InteractionHand)hand, (float)equipProgress, this.mainHandSwingProgress, this.offHandSwingProgress, this.mainHandSwitchEvent, this.offHandSwitchEvent, this.swingMHand, this.swingOHand, interact, blockBreaking, this.particles);
                ItemStack renderStack = new ItemStack((ItemLike)item.getItem(), item.getCount());
                this.copyAppearanceComponents(item, renderStack);
                ItemStack itemStack2 = renderStack2 = bl ? GlobalsStorage.mainHandItem : GlobalsStorage.offHandItem;
                if (!renderStack2.is(Items.AIR)) {
                    renderStack = renderStack2;
                }
                DispatcherStorage.setItem(item);
                ItemModelStorage.addData(new ItemModelContext(bl2, swingProgress, (AbstractClientPlayer)player, (InteractionHand)hand, bl, LuaTestHMI.deltaTime, (float)equipProgress, this.mainHandSwingProgress, this.offHandSwingProgress, this.mainHandSwitchEvent, this.offHandSwitchEvent, this.swingMHand, this.swingOHand, interact, blockBreaking, item), item);
                this.renderItem((LivingEntity)player, renderStack, bl2 ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, (PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, (int)light);
            } else {
                this.itemPose((PoseStack)matrices, item, bl2, swingProgress, (AbstractClientPlayer)player, bl, (InteractionHand)hand, (float)equipProgress, this.mainHandSwingProgress, this.offHandSwingProgress, this.mainHandSwitchEvent, this.offHandSwitchEvent, this.swingMHand, this.swingOHand, interact, blockBreaking, this.particles);
                if (item.has(DataComponents.MAP_ID)) {
                    matrices.pushPose();
                    matrices.translate(-0.05 * (double)l, 0.2, 0.1);
                    matrices.mulPose((Quaternionfc)Axis.YP.rotationDegrees((float)(-12 * l)));
                    this.renderMap((PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, (int)light, item);
                    matrices.popPose();
                } else {
                    ItemStack renderStack;
                    ItemStack itemStack3 = renderStack = bl ? GlobalsStorage.mainHandItem : GlobalsStorage.offHandItem;
                    if (renderStack.is(Items.AIR)) {
                        renderStack = item;
                    }
                    DispatcherStorage.setItem(item);
                    ItemModelStorage.addData(new ItemModelContext(bl2, swingProgress, (AbstractClientPlayer)player, (InteractionHand)hand, bl, LuaTestHMI.deltaTime, (float)equipProgress, this.mainHandSwingProgress, this.offHandSwingProgress, this.mainHandSwitchEvent, this.offHandSwitchEvent, this.swingMHand, this.swingOHand, interact, blockBreaking, item), item);
                    this.renderItem((LivingEntity)player, renderStack, bl2 ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, (PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, (int)light);
                }
                if (bl2) {
                    LuaTestHMI.matricesMain.set((Matrix4fc)matrices.last().pose());
                } else {
                    LuaTestHMI.matricesOff.set((Matrix4fc)matrices.last().pose());
                }
                matrices.pushPose();
                ParticleRenderManager.draw(this.particles, (PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, "ITEM", (InteractionHand)hand, (int)light, (AbstractClientPlayer)player, (float)tickProgress);
                matrices.popPose();
            }
            matrices.popPose();
            matrices.popPose();
            if (bl) {
                this.prevMainHand = item.getItem();
            }
            if (!bl) {
                this.prevOffHand = item.getItem();
            }
            matrices.pushPose();
            ParticleRenderManager.draw(this.particles, (PoseStack)matrices, (SubmitNodeCollector)orderedRenderCommandQueue, "SCREEN", (InteractionHand)hand, (int)light, (AbstractClientPlayer)player, (float)tickProgress);
            matrices.popPose();
            LuaTestHMI.tickProgress = tickProgress;
            GlobalsStorage.offHandItem = ItemStack.EMPTY;
            GlobalsStorage.mainHandItem = ItemStack.EMPTY;
        }
    }
}

