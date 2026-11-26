package sanch3zss.raven.feature.module.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.server.ServerUtil;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;

public final class ShulkerBoxTooltipModule extends Module {
    private final BooleanSetting colored = new BooleanSetting(Translation.clearText("Цвет коробки")).setValue(true);

    public ShulkerBoxTooltipModule() {
        super(ModuleTab.VISUALS, "ShulkerBoxTooltip");
        addSettings(colored);
        ClientTooltipComponentCallback.EVENT.register(data ->
                data instanceof ShulkerPreviewTooltip.Contents contents ? new ShulkerPreviewTooltip(contents) : null);
    }

    public boolean handles(ItemStack stack) {
        return isState() && stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
    }

    public Optional<TooltipComponent> preview(ItemStack stack) {
        if (!handles(stack)) return Optional.empty();
        List<ItemStack> items = contents(stack);
        if (items.stream().allMatch(ItemStack::isEmpty)) return Optional.empty();
        return Optional.of(new ShulkerPreviewTooltip.Contents(items, color(stack)));
    }

    private int color(ItemStack stack) {
        if (!colored.isValue()) return 0xFFFFFF;
        DyeColor dye = ((ShulkerBoxBlock) ((BlockItem) stack.getItem()).getBlock()).getColor();
        if (dye == null) return 0x976797;
        int rgb = dye.getTextureDiffuseColor();
        int red = Math.max(38, rgb >> 16 & 0xFF);
        int green = Math.max(38, rgb >> 8 & 0xFF);
        int blue = Math.max(38, rgb & 0xFF);
        return red << 16 | green << 8 | blue;
    }

    private static List<ItemStack> contents(ItemStack stack) {
        List<ItemStack> items = new ArrayList<>(stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY)
                .allItemsCopyStream().toList());
        if (items.stream().anyMatch(item -> !item.isEmpty())) {
            while (items.size() < 27) items.add(ItemStack.EMPTY);
            return items;
        }
        if (!ServerUtil.isConnectedToServer("holyworld")) return items;
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.getList("backpack-inventory").ifPresent(list -> {
            Map<Integer, ItemStack> slots = new HashMap<>();
            for (Tag element : list) {
                if (element instanceof CompoundTag entry) {
                    CompoundTag item = entry.getCompoundOrEmpty("item");
                    slots.put(entry.getIntOr("slot", 0), new ItemStack(BuiltInRegistries.ITEM.getValue(
                            Identifier.parse(item.getStringOr("id", "minecraft:air"))), item.getIntOr("Count", 0)));
                }
            }
            if (slots.isEmpty()) return;
            int size = Math.max(backpackSize(tag), 27);
            for (int slot = 0; slot < size; slot++) {
                items.add(slots.getOrDefault(slot, ItemStack.EMPTY));
            }
        });
        return items;
    }

    private static int backpackSize(CompoundTag tag) {
        return switch (tag.getCompoundOrEmpty("PublicBukkitValues").getStringOr("litebackpacks:backpack", "").replace("\"", "")) {
            case "infinity" -> 36;
            case "huge" -> 27;
            case "big" -> 21;
            case "normal" -> 15;
            case "mini" -> 9;
            default -> -1;
        };
    }
}
