package sanch3zss.raven.feature.module.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;

@Aliases(aliases = {"CustomTags", "NameTags", "Tags", "Item Count", "Dropped Items"})
public class CustomTagsModule extends Module {
    public final BooleanSetting showSelf = new BooleanSetting(Lang.CUSTOM_TAGS_SELF).setValue(true);
    public final BooleanSetting itemCount = new BooleanSetting(Translation.clearText("Количество предметов"),
            Translation.clearText("Показывает, сколько предметов лежит на земле")).setValue(true);
    public final BooleanSetting hideBackground = new BooleanSetting(Translation.clearText("Убрать фон"),
            Translation.clearText("Убирает тёмную подложку за никами"));
    private final Map<Item, Float> modelHeights = new HashMap<>();
    private final ItemStackRenderState measureState = new ItemStackRenderState();

    public CustomTagsModule() {
        super(ModuleTab.VISUALS, "CustomTags");
        addSettings(this.showSelf, this.itemCount, this.hideBackground);
    }

    public boolean hidesBackground() {
        return isState() && this.hideBackground.isValue();
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
        if (isState() && this.itemCount.isValue() && Mc.INSTANCE.isWorldLoaded()) {
            renderItemCounts(poseStack, collector, camera);
        }
    }

    public boolean shouldShowSelf() {
        return isState() && this.showSelf.isValue();
    }

    private void renderItemCounts(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float range = 4.0f;
        List<ItemPile> piles = new ArrayList<>();
        for (Entity entity : Mc.INSTANCE.getWorld().entitiesForRendering()) {
            if (!(entity instanceof ItemEntity item) || item.getItem().isEmpty()) continue;
            Vec3 position = item.getPosition(partialTick);
            if (position.distanceToSqr(camera) > (range + 1.0f) * (range + 1.0f)) continue;
            ItemPile pile = null;
            for (ItemPile candidate : piles) {
                if (ItemStack.isSameItemSameComponents(candidate.stack, item.getItem())
                        && candidate.anchor.distanceToSqr(position) < 1.6) {
                    pile = candidate;
                    break;
                }
            }
            if (pile == null) {
                piles.add(new ItemPile(item, position, partialTick, labelHeight(item)));
            } else {
                pile.count += item.getItem().getCount();
                pile.top = Math.max(pile.top, position.y + bob(item, partialTick) + labelHeight(item));
            }
        }
        if (piles.isEmpty()) return;
        Font font = minecraft.font;
        for (ItemPile pile : piles) {
            if (pile.count <= 1) continue;
            Vec3 label = new Vec3(pile.anchor.x, pile.top + 0.12, pile.anchor.z);
            double distance = label.distanceTo(camera);
            float fade = 1.0f - Mth.clamp((float) ((distance - range * 0.65) / (range * 0.35)), 0.0f, 1.0f);
            fade = fade * fade * (3.0f - 2.0f * fade);
            if (fade <= 0.05f || !visible(camera, label)) continue;
            FormattedCharSequence text = Component.literal("x" + pile.count).getVisualOrderText();
            int alpha = Math.max(5, Math.round(fade * 255.0f));
            int light = LightCoordsUtil.getLightCoords(Mc.INSTANCE.getWorld(), BlockPos.containing(label));
            poseStack.pushPose();
            poseStack.translate(label.x - camera.x, label.y - camera.y, label.z - camera.z);
            poseStack.mulPose(minecraft.gameRenderer.mainCamera().rotation());
            poseStack.scale(0.02f, -0.02f, 0.02f);
            collector.submitText(poseStack, -font.width(text) / 2.0f, 0.0f, text, false, Font.DisplayMode.SEE_THROUGH,
                    light, alpha << 24 | 0xFFFFFF, 0, 0);
            poseStack.popPose();
        }
    }

    private boolean visible(Vec3 camera, Vec3 label) {
        return Mc.INSTANCE.getWorld().clip(new ClipContext(camera, label, ClipContext.Block.VISUAL,
                ClipContext.Fluid.NONE, Mc.INSTANCE.getPlayer())).getType() == HitResult.Type.MISS;
    }

    private static float bob(ItemEntity item, float partialTick) {
        return Mth.sin((item.tickCount + partialTick) / 10.0f + item.bobOffs) * 0.1f + 0.1f;
    }

    private float labelHeight(ItemEntity item) {
        return this.modelHeights.computeIfAbsent(item.getItem().getItem(), key -> {
            Minecraft.getInstance().getItemModelResolver().updateForNonLiving(this.measureState, item.getItem(),
                    ItemDisplayContext.GROUND, item);
            return (float) this.measureState.getModelBoundingBox().getYsize() + 0.0625f + 0.04f;
        });
    }

    private final class ItemPile {
        final ItemStack stack;
        final Vec3 anchor;
        int count;
        double top;

        ItemPile(ItemEntity item, Vec3 position, float partialTick, float height) {
            this.stack = item.getItem();
            this.anchor = position;
            this.count = item.getItem().getCount();
            this.top = position.y + bob(item, partialTick) + height;
        }
    }
}
