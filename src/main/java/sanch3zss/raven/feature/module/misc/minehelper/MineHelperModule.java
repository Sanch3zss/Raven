package sanch3zss.raven.feature.module.misc.minehelper;


import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.engine.ScreenResolution;
import sanch3zss.raven.render.gl.StencilBufferUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.service.ActionSequenceController;
import sanch3zss.raven.event.player.*;
import sanch3zss.raven.event.render.Render2DEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.inventory.*;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.player.*;
import sanch3zss.raven.feature.module.misc.minehelper.MineHelperAction;
import sanch3zss.raven.feature.world.BlockUtil;import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.ui.setting.MultiSelectSetting;
import sanch3zss.raven.util.math.interpolation.Stopwatch;

@Aliases(aliases = {"Fun Time", "Mine Helper"})
public class MineHelperModule extends Module {
    public final MultiSelectSetting<MineHelperAction> helpActions;
    public final Mc mc;
    public MineHelperCountdown countdown;
    public boolean nearMine;
    public boolean cleaningInProgress;
    public final Stopwatch notificationTimer;
    public static final Pattern timePattern = Pattern.compile("\\d{2}:\\d{2}");
    public static final Set<Item> valuableItems = Set.of(new Item[]{Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT, Items.NETHERITE_INGOT, Items.NETHERITE_SCRAP, Items.ANCIENT_DEBRIS, Items.LAPIS_LAZULI, Items.REDSTONE, Items.COAL, Items.RAW_IRON, Items.RAW_GOLD, Items.RAW_COPPER, Items.AMETHYST_SHARD, Items.EXPERIENCE_BOTTLE, Items.ENDER_PEARL, Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE});
    public static final Set<Item> junkItems = Set.of(new Item[]{Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.DIRT, Items.GRAVEL, Items.FLINT, Items.ANDESITE, Items.DIORITE, Items.GRANITE, Items.TUFF, Items.NETHERRACK, Items.STONE, Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.BONE, Items.GUNPOWDER, Items.STRING});
    public static final Set<Item> toolItems = Set.of(Items.DIAMOND_SWORD, Items.NETHERITE_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_AXE, Items.DIAMOND_HOE, Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_AXE, Items.NETHERITE_HOE);
    public static final Set<Item> armorItems = Set.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS, Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);

    public MineHelperModule() {
        super(ModuleTab.MISC, "MineHelper");
        this.helpActions = new MultiSelectSetting(Lang.MINE_HELPER_HELP_TYPE, Lang.MINE_HELPER_HELP_TYPE_DESC).values(MineHelperAction.class);
        this.mc = Mc.INSTANCE;
        this.countdown = null;
        this.notificationTimer = new Stopwatch();
        addSettings(this.helpActions);
        register(Render2DEvent.class, event -> {
            if (isState() && this.mc.isWorldLoaded() && this.helpActions.isSelected(MineHelperAction.NEXT_MINE) && this.countdown != null) {
                PoseStack matrixStack = event.matrixStack();
                DrawEngine drawEngine = Raven.INSTANCE.drawEngine();
                ColorStack colorStack = drawEngine.colorStack();
                int iScreenWidth = ScreenResolution.resolution().screenWidth();
                MsdfFont msdfFont = Fonts.INTER_MEDIUM.get();
                float height = msdfFont.getHeight(14.0f);
                float size = this.mc.getInGameHud().getBossOverlay().events.size() * 45;
                drawEngine.begin();
                int white = colorStack.computeColor(255, 255, 255, StencilBufferUtil.STENCIL_MASK);
                drawEngine.drawLine(matrixStack, msdfFont, 14, iScreenWidth, size, Lang.MINE_HELPER_NEXT_MINE_LABEL.effective(), this.countdown.nextType(), white, white);
                drawEngine.drawLine(matrixStack, msdfFont, 14, iScreenWidth, size + height, Lang.MINE_HELPER_TIME_LEFT_LABEL.effective(), this.countdown.formattedTime(), white, white);
                drawEngine.end();
            }
        });
        register(PlayerTickEvent.class, event2 -> {
            if (isState() && this.mc.isWorldLoaded() && event2.isPre()) {
                LocalPlayer player = this.mc.getPlayer();
                ClientLevel world = this.mc.getWorld();
                if (this.helpActions.isSelected(MineHelperAction.NEXT_MINE)
                        || this.helpActions.isSelected(MineHelperAction.CLEAN_INVENTORY)) {
                    if (this.countdown == null || player.tickCount % 10 == 0) {
                        List<ArmorStand> nearby = new ArrayList<>();
                        for (Entity entity : world.entitiesForRendering()) {
                            if (entity instanceof ArmorStand stand
                                    && stand.distanceToSqr(player) < 32.0d * 32.0d) {
                                nearby.add(stand);
                            }
                        }
                        ArmorStand title = nearby.stream()
                                .filter(stand -> getArmorStandName(stand).contains("Авто-Шахта"))
                                .min(Comparator.comparingDouble(stand -> stand.distanceToSqr(player)))
                                .orElse(null);
                        this.nearMine = title != null;
                        this.countdown = title == null ? null : parseCountdown(nearby.stream()
                                .filter(stand -> Math.abs(stand.getX() - title.getX()) < 2.0d
                                        && Math.abs(stand.getZ() - title.getZ()) < 2.0d
                                        && Math.abs(stand.getY() - title.getY()) < 4.0d)
                                .filter(stand -> {
                                    String name = getArmorStandName(stand);
                                    return name.contains("Авто-Шахта") || name.contains("Следующая:")
                                            || name.contains("Обновление через:")
                                            || timePattern.matcher(name).matches();
                                })
                                .sorted(Comparator.comparingDouble(stand -> -stand.getY()))
                                .toList()).orElse(null);
                    }
                }
                if (this.helpActions.isSelected(MineHelperAction.CLEAN_INVENTORY)) {
                    int emptySlots = PlayerActionUtil.INSTANCE.getEmptySlots(player.getInventory());
                    InventoryItemFinder searcher = Raven.INSTANCE.inventoryService().searcher();
                    if (shouldCleanInventory(emptySlots)
                            && (this.nearMine || BlockUtil.isWithInMine(player.blockPosition()))) {
                        List<SlotSearchResult2> listFindAllItems = searcher.findAllItems(this::isTrashItem, InventoryScope.HOTBAR, InventoryScope.INVENTORY);
                        if (!listFindAllItems.isEmpty() && emptySlots <= 2 && ActionSequenceController.isIdle()) {
                            this.cleaningInProgress = true;
                            ActionSequenceController.submit(() -> {
                                try {
                                    listFindAllItems.forEach(slotResult -> {
                                        if (slotResult.found() && isTrashItem(player.getInventory()
                                                .getItem(slotResult.slotReference().slot()))) {
                                            PlayerActionUtil.INSTANCE.windowClick(ContainerInput.THROW, slotResult.slotReference().increasedSlot(), 1);
                                        }
                                    });
                                } finally {
                                    this.cleaningInProgress = false;
                                }
                            });
                        }
                    } else if (this.cleaningInProgress) {
                        this.cleaningInProgress = false;
                    }
                }
            }
        });
        register(BlockBreakEvent2.class, blockBreakEvent2 -> {
            if (isState() && this.mc.isWorldLoaded() && this.helpActions.isSelected(MineHelperAction.SAVE_PICKAXE)) {
                ItemStack mainHandStack = this.mc.getPlayer().getInventory().getSelectedItem();
                if (!mainHandStack.isEmpty() && mainHandStack.is(ItemTags.PICKAXES) && mainHandStack.isDamageableItem()) {
                    if (1.0f - (mainHandStack.getDamageValue() / (float) mainHandStack.getMaxDamage()) <= 0.15f) {
                        if (this.notificationTimer.hasElapsed(5L, TimeUnit.SECONDS)) {
                            Raven.INSTANCE.notificationRepository().post(NotificationType.WARNING, Component.literal(Lang.MINE_HELPER_PICKAXE_LOW_DURABILITY.effective()), 2L, TimeUnit.SECONDS);
                            this.notificationTimer.reset();
                        }
                        blockBreakEvent2.cancel();
                    }
                }
            }
        });
    }

    public boolean shouldCleanInventory(int i) {
        return !this.cleaningInProgress && i <= 2;
    }

    public boolean isTrashItem(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return false;
        }
        Item item = itemStack.getItem();
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        if ((item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) || (item instanceof PlayerHeadItem) || item == Items.TOTEM_OF_UNDYING || toolItems.contains(item) || armorItems.contains(item) || valuableItems.contains(item) || path.endsWith("_ore") || path.contains("raw_") || path.endsWith("_wire") || item.components().has(DataComponents.FOOD) || itemStack.isEnchanted()) {
            return false;
        }
        return junkItems.contains(item) || isJunkBlockPath(path);
    }

    public boolean isJunkBlockPath(String str) {
        return str.equals("cobblestone") || str.equals("cobbled_deepslate") || str.equals("dirt") || str.equals("gravel") || str.equals("andesite") || str.equals("diorite") || str.equals("granite") || str.equals("tuff") || str.equals("netherrack") || str.equals("stone");
    }

    public Optional<MineHelperCountdown> parseCountdown(List<ArmorStand> list) {
        if (list.size() < 4) {
            return Optional.empty();
        }
        String titleLine = getArmorStandName(list.get(0));
        String nextLine = getArmorStandName(list.get(1));
        String updateLine = getArmorStandName(list.get(2));
        String timerLine = getArmorStandName(list.get(3));
        if (!titleLine.contains("Авто-Шахта") || !nextLine.contains("Следующая:") || !updateLine.contains("Обновление через:") || !timePattern.matcher(timerLine).matches()) {
            return Optional.empty();
        }
        String[] timerParts = timerLine.split(":");
        if (timerParts.length != 2) {
            return Optional.empty();
        }
        return Optional.of(new MineHelperCountdown(nextLine.replace("Следующая: ", "").trim(), (Integer.parseInt(timerParts[0]) * 60) + Integer.parseInt(timerParts[1]), System.currentTimeMillis()));
    }

    public String getArmorStandName(ArmorStand armorStandEntity) {
        return armorStandEntity.getCustomName() != null ? armorStandEntity.getCustomName().getString() : "";
    }
    public int getTimeLeftColor(int i) {
        if (i <= 10) {
            return -43691;
        }
        return i <= 60 ? -171 : -11141291;
    }

    public int getRarityColor(String str) {
        String lowerCase = str.toLowerCase(Locale.ROOT);
        if (lowerCase.contains("мифическ")) {
            return -5614081;
        }
        return lowerCase.contains("легендарн") ? -22016 : -5592406;
    }
}
