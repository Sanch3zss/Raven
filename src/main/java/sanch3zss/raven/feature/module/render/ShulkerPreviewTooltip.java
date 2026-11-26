package sanch3zss.raven.feature.module.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public final class ShulkerPreviewTooltip implements ClientTooltipComponent {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath("raven", "shulker_box_tooltip");

    private final Contents contents;
    private final List<ItemStack> shown;
    private final int columns;
    private final int rows;

    public ShulkerPreviewTooltip(Contents contents) {
        this.contents = contents;
        shown = Minecraft.getInstance().hasShiftDown() ? merged(contents.items()) : contents.items();
        columns = Math.min(9, Math.max(1, shown.size()));
        rows = (shown.size() + 8) / 9;
    }

    @Override
    public int getHeight(Font font) {
        return 14 + rows * 18 + 2;
    }

    @Override
    public int getWidth(Font font) {
        return 14 + columns * 18;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 14 + columns * 18, 14 + rows * 18,
                0xFF000000 | contents.color());
        for (int slot = 0; slot < shown.size(); slot++) {
            ItemStack stack = shown.get(slot);
            if (stack.isEmpty()) continue;
            int itemX = x + 8 + slot % 9 * 18;
            int itemY = y + 8 + slot / 9 * 18;
            graphics.item(stack, itemX, itemY);
            graphics.itemDecorations(font, stack, itemX, itemY, countText(stack.getCount()));
        }
    }

    private static String countText(int count) {
        if (count <= 1) return null;
        if (count < 1000) return String.valueOf(count);
        return String.format("%.1fk", count / 1000.0f);
    }

    private static List<ItemStack> merged(List<ItemStack> items) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            ItemStack same = null;
            for (ItemStack candidate : merged) {
                if (ItemStack.isSameItemSameComponents(candidate, stack)) {
                    same = candidate;
                    break;
                }
            }
            if (same != null) same.grow(stack.getCount());
            else merged.add(stack.copy());
        }
        merged.sort(Comparator.comparingInt(ItemStack::getCount).reversed());
        return merged;
    }

    public record Contents(List<ItemStack> items, int color) implements TooltipComponent {
    }
}
