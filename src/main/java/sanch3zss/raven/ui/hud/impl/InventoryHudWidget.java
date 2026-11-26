package sanch3zss.raven.ui.hud.impl;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.hud.HudGlassPanel;
import sanch3zss.raven.ui.hud.DragRenderContext;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.hud.Draggable;

public class InventoryHudWidget extends Draggable {
    public InventoryHudWidget(BooleanSupplier visibility) {
        super("InventoryHud", visibility);
        this.x = 280.0f;
        this.y = 300.0f;
    }

    @Override
    public void layout(DragRenderContext context) {
    }

    @Override
    public void render(DragRenderContext context) {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (!isVisible() || player == null) {
            return;
        }
        drawGlassPanel(context);
    }

    @Override
    protected void collectItems(DragRenderContext context) {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (!isVisible() || player == null) {
            return;
        }
        List<ItemStack> main = player.getInventory().getNonEquipmentItems();
        for (int index = 0; index < 9 * 3; index++) {
            int inventoryIndex = index + 9;
            if (inventoryIndex >= main.size()) {
                break;
            }
            ItemStack stack = main.get(inventoryIndex);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            float itemX = this.x + 3.0f + (index % 9) * 17.0f;
            float itemY = this.y + 3.0f + (index / 9) * 17.0f;
            context.drawVanillaItem(stack, itemX, itemY, 16.0f, true, 1.0f);
        }
    }

    private void drawGlassPanel(DragRenderContext context) {
        DrawEngine drawEngine = context.drawEngine();
        ColorStack colors = drawEngine.colorStack();
        int blurTexture = Raven.INSTANCE.widgetStack().hudBlurTexture();
        int tint = mixRgb(sanch3zss.raven.ui.menu.MenuStyle.surfaceColor(context.theme(), 0xFF111217), context.theme().palette().accentStart().argb(), 0.05f);
        HudGlassPanel.drawShadows(drawEngine, context.matrixStack().last().pose(),
                this.x, this.y, (3.0f * 2.0f + 9 * 17.0f), (3.0f * 2.0f + 3 * 17.0f), 6.0f, 30.0f,
                colors.computeColor(tint, 20.5f));
        if (blurTexture != 0) {
            drawEngine.roundedGlassPanel(context.matrixStack().last().pose(),
                    this.x, this.y, (3.0f * 2.0f + 9 * 17.0f), (3.0f * 2.0f + 3 * 17.0f), 6.0f, 0.0f, 0.80f,
                    colors.computeColor(tint, 1.0f),
                    0, blurTexture);
        } else {
            drawEngine.roundedRectangle(context.matrixStack().last().pose(),
                    this.x, this.y, (3.0f * 2.0f + 9 * 17.0f), (3.0f * 2.0f + 3 * 17.0f), 6.0f, colors.computeColor(tint, 0.96f));
        }
    }

    private static int mixRgb(int base, int tint, float amount) {
        float inverse = 1.0f - amount;
        int red = Math.round(((base >> 16) & 0xFF) * inverse + ((tint >> 16) & 0xFF) * amount);
        int green = Math.round(((base >> 8) & 0xFF) * inverse + ((tint >> 8) & 0xFF) * amount);
        int blue = Math.round((base & 0xFF) * inverse + (tint & 0xFF) * amount);
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }

    public boolean isRendering() {
        return isVisible() && Mc.INSTANCE.getPlayer() != null;
    }

    @Override
    public boolean click(MouseButtonInput2 input, boolean hovered) {
        return false;
    }

    @Override
    public boolean cursor(MouseMoveInput input, boolean hovered) {
        return hovered && !input.intercepted();
    }

    @Override
    public void animate(WeightedEngine engine) {
    }

    @Override
    public void update() {
    }

    @Override
    public float width() {
        return (3.0f * 2.0f + 9 * 17.0f);
    }

    @Override
    public float height() {
        return (3.0f * 2.0f + 3 * 17.0f);
    }

    @Override
    protected float baseScale() {
        return 1.75f;
    }
}
