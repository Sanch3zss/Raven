
package com.holdmylua.source.global.item_model;

import com.holdmylua.source.LuaTestHMI;
import com.holdmylua.source.global.item_model.ItemModelContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;

public class ItemModelStorage {
    private static ItemModelContext rightHandData;
    private static ItemModelContext leftHandData;

    public static void addData(ItemModelContext info, ItemStack item) {
        if (!item.isEmpty() && item.getUseAnimation() != ItemUseAnimation.BLOCK && item.getUseAnimation() != ItemUseAnimation.TRIDENT) {
            if (info.bl) {
                rightHandData = info;
            } else {
                leftHandData = info;
            }
        }
    }

    public static ItemModelContext get(ItemDisplayContext displayContext) {
        ItemModelContext context = displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? rightHandData : leftHandData;
        if (context != null) {
            return context;
        }
        return new ItemModelContext(false, 0.0f, (AbstractClientPlayer)Minecraft.getInstance().player, InteractionHand.MAIN_HAND, false, LuaTestHMI.deltaTime, 0.0f, 0.0f, 0.0f, false, false, false, false, false, false, ItemStack.EMPTY);
    }

    public static void clear() {
        rightHandData = null;
        leftHandData = null;
    }
}
