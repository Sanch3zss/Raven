package sanch3zss.raven.feature.module.misc;

import java.util.Locale;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.event.player.UseItemEvent;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.ColorSetting;

public class HWHelperModule extends Module {
    private static final int STUN_RADIUS = 15;
    private static final long STUN_DURATION_MS = 15_000L;

    private final BooleanSetting explosiveTrap = new BooleanSetting(
            Translation.clearText("Взрывная трапка"),
            Translation.clearText("Показывает зону взрывной трапки в руке.")).setValue(true);
    private final BooleanSetting trap = new BooleanSetting(
            Translation.clearText("Трапка"),
            Translation.clearText("Показывает зону трапки в руке.")).setValue(true);
    private final BooleanSetting stun = new BooleanSetting(
            Translation.clearText("Стан"),
            Translation.clearText("Показывает куб стана в руке и после активации.")).setValue(true);
    private final ColorSetting rangeColor = new ColorSetting(
            Translation.clearText("Цвет зоны")).value(0xFFFFFFFF);
    private BlockPos stunCenter;
    private long stunStartedAt;

    public HWHelperModule() {
        super(ModuleTab.MISC, "HW Helper");
        addSettings(this.explosiveTrap, this.trap, this.stun, this.rangeColor);
        register(WorldRenderEvent.class, this::renderRange);
        register(UseItemEvent.class, event -> {
            LocalPlayer player = Mc.INSTANCE.getPlayer();
            if (isState() && this.stun.isValue() && player != null && holdsStun(player)) {
                this.stunCenter = player.blockPosition();
                this.stunStartedAt = System.currentTimeMillis();
            }
        });
    }

    private void renderRange(WorldRenderEvent event) {
        Mc mc = Mc.INSTANCE;
        if (!isState() || !mc.isWorldLoaded()) return;
        LocalPlayer player = mc.getPlayer();
        BlockPos center = player.blockPosition();
        int color = this.rangeColor.getColor();
        if (this.explosiveTrap.isValue() && CustomItemRangeRenderer.held(player, Items.PRISMARINE_SHARD)) {
            CustomItemRangeRenderer.box(event, CustomItemRangeRenderer.box(center, -3, 0, -3, 4, 6, 4), color);
        }
        if (this.trap.isValue() && CustomItemRangeRenderer.held(player, Items.POPPED_CHORUS_FRUIT)) {
            CustomItemRangeRenderer.box(event, CustomItemRangeRenderer.box(center, -2, 0, -2, 3, 4, 3), color);
        }
        if (!this.stun.isValue()) {
            return;
        }
        long elapsed = System.currentTimeMillis() - this.stunStartedAt;
        if (this.stunCenter != null && elapsed < STUN_DURATION_MS) {
            float left = 1.0f - elapsed / (float) STUN_DURATION_MS;
            CustomItemRangeRenderer.box(event, stunBox(this.stunCenter),
                    ColorUtil.replAlpha(color, Math.round(ColorUtil.alpha(color) * (0.35f + 0.65f * left))));
        } else if (holdsStun(player)) {
            CustomItemRangeRenderer.outlineBox(event, stunBox(center), color);
        }
    }

    private static AABB stunBox(BlockPos center) {
        return CustomItemRangeRenderer.box(center, -STUN_RADIUS, -STUN_RADIUS, -STUN_RADIUS,
                STUN_RADIUS, STUN_RADIUS, STUN_RADIUS);
    }

    private static boolean holdsStun(LocalPlayer player) {
        return isStun(player.getMainHandItem()) || isStun(player.getOffhandItem());
    }

    private static boolean isStun(ItemStack stack) {
        return stack.is(Items.NETHER_STAR)
                && stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains("стан");
    }
}
