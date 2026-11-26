package sanch3zss.raven.feature.module.misc;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.config.RotationConfig;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.input.KeyPressState;
import sanch3zss.raven.event.client.*;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.inventory.*;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.ButtonAction;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.KeybindSetting;
import sanch3zss.raven.util.math.rotation.*;

@Aliases(aliases = {"Exp Tweaks", "Auto Experience", "AutoExperience", "Auto Repair", "Опыт"})
public class ExpTweaksModule extends Module {
    public final KeybindSetting keybindSetting;
    public final BooleanSetting noDelay;
    public boolean throwing;
    public int savedSourceSlot;
    public int savedSelectedSlot;

    public ExpTweaksModule() {
        super(ModuleTab.MISC, "ExpTweaks");
        this.keybindSetting = new KeybindSetting(Translation.clearText("Бинд"));
        this.noDelay = new BooleanSetting(Translation.clearText("Без задержки броска"));
        this.throwing = false;
        this.savedSourceSlot = -1;
        this.savedSelectedSlot = -1;
        addSettings(this.keybindSetting, this.noDelay);
        register(KeyInputEvent.class, event -> handleKey(event.key(), event.action() == KeyPressState.PRESS));
        register(MouseButtonEvent.class, event -> handleKey(event.button(), event.action() == ButtonAction.PRESS));
        register(PlayerTickEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (!isState() || !mc.isWorldLoaded() || !event.isPre()) return;
            LocalPlayer player = mc.getPlayer();
            boolean cooldown = player.getCooldowns().isOnCooldown(Items.EXPERIENCE_BOTTLE.getDefaultInstance());
            if (mc.getCurrentScreen() != null) {
                this.throwing = false;
            } else if (!cooldown && this.throwing) {
                throwExperience();
            }
            if (this.noDelay.isValue() && !cooldown && mc.getCurrentScreen() == null
                    && mc.getGameOptions().keyUse.isDown()
                    && (player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE) || player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE))) {
                mc.getMinecraft().rightClickDelay = 0;
            }
        });
    }

    public void handleKey(int key, boolean pressed) {
        if (key == this.keybindSetting.getKey()) {
            if (!pressed && this.savedSourceSlot != -1 && this.savedSelectedSlot != -1) {
                PlayerActionUtil.INSTANCE.windowClick(ContainerInput.SWAP, this.savedSourceSlot, this.savedSelectedSlot, true);
                this.savedSourceSlot = -1;
                this.savedSelectedSlot = -1;
            }
            this.throwing = pressed;
        }
    }

    public void throwExperience() {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        InventoryService inventoryService = Raven.INSTANCE.inventoryService();
        Predicate<ItemStack> predicate = itemStack -> itemStack.getItem() == Items.EXPERIENCE_BOTTLE;
        inventoryService.searcher().findItem(predicate, InventoryScope.ALL).ifPresentOrElse(
                slotResult -> throwFromSlot(slotResult, player, predicate, inventoryService), this::notifyMissing);
    }

    public void throwFromSlot(SlotSearchResult2 slotResult, LocalPlayer player, Predicate<ItemStack> predicate,
                              InventoryService inventoryService) {
        RotationManager.INSTANCE.scheduleRotation(new Rotation(player.getYRot(), 90.0f), RotationConfig.LINEAR, 1, this, 5);
        if (RotationManager.INSTANCE.getServerRotation().getPitch() > 89.0f) {
            if (slotResult.slotReference().scope() == InventoryScope.HOTBAR && !predicate.test(player.getItemInHand(InteractionHand.MAIN_HAND))) {
                inventoryService.hotbarSlotSwapper().swapTo(this, slotResult.slotReference(), 1);
            } else if (slotResult.slotReference().scope() == InventoryScope.INVENTORY) {
                PlayerActionUtil.INSTANCE.swapHand(slotResult.slotReference().slot(), InteractionHand.MAIN_HAND);
                if (this.savedSourceSlot == -1) {
                    this.savedSourceSlot = slotResult.slotReference().slot();
                }
                if (this.savedSelectedSlot == -1) {
                    this.savedSelectedSlot = Mc.INSTANCE.getPlayer().getInventory().getSelectedSlot();
                }
            }
            for (InteractionHand hand : InteractionHand.values()) {
                if (predicate.test(player.getItemInHand(hand))) {
                    inventoryService.itemInteractor().interactItem(player, hand, RotationManager.INSTANCE.getCurrentRotation());
                }
            }
        }
    }

    public void notifyMissing() {
        Raven.INSTANCE.notificationRepository().post(NotificationType.ERROR, Component.literal(Lang.NO_ITEM_FOUND.effective()
                .replace("{item}", ChatFormatting.RED + Items.EXPERIENCE_BOTTLE.getName(Items.EXPERIENCE_BOTTLE.getDefaultInstance()).getString()
                        + ChatFormatting.RESET)), 3L, TimeUnit.SECONDS);
        this.throwing = false;
    }
}
