
package com.holdmylua.source.lua_runtime;

import com.holdmylua.source.LuaTestHMI;
import com.holdmylua.source.global.GlobalsStorage;
import com.holdmylua.source.global.item_model.ItemModelContext;
import com.holdmylua.source.lua_runtime.LuaScriptManager;
import com.holdmylua.source.model.ModelPartAnimator;
import com.holdmylua.source.scripting.custom_api.KeyBindManager;
import com.holdmylua.source.scripting.script_wrappers.Easings;
import com.holdmylua.source.scripting.script_wrappers.I;
import com.holdmylua.source.scripting.script_wrappers.JSItems;
import com.holdmylua.source.scripting.script_wrappers.JSTags;
import com.holdmylua.source.scripting.script_wrappers.M;
import com.holdmylua.source.scripting.script_wrappers.P;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class ModelScriptCache {
    private static ItemModelContext data = new ItemModelContext(false, 0.0f, (AbstractClientPlayer)Minecraft.getInstance().player, InteractionHand.MAIN_HAND, false, LuaTestHMI.deltaTime, 0.0f, 0.0f, 0.0f, false, false, false, false, false, false, ItemStack.EMPTY);
    private final Globals globals;
    private final LuaValue chunk;
    private boolean canRun = true;
    private final M mInstance = new M();
    private final I iInstance = new I();
    private final JSItems jsItemsInstance = new JSItems();
    private final JSTags jsTagsInstance = new JSTags();
    private final P pInstance = new P();
    private final Easings easingsInstance = new Easings();
    private final KeyBindManager keyBindManagerInstance = new KeyBindManager();

    public ModelScriptCache(String sourceCode) throws IOException {
        this.globals = LuaScriptManager.getInstance().sharedGlobals;
        this.globals.set("M", CoerceJavaToLua.coerce((Object)this.mInstance));
        this.globals.set("I", CoerceJavaToLua.coerce((Object)this.iInstance));
        this.globals.set("Items", CoerceJavaToLua.coerce((Object)this.jsItemsInstance));
        this.globals.set("Tags", CoerceJavaToLua.coerce((Object)this.jsTagsInstance));
        this.globals.set("P", CoerceJavaToLua.coerce((Object)this.pInstance));
        this.globals.set("Easings", CoerceJavaToLua.coerce((Object)this.easingsInstance));
        this.globals.set("KeyBindManager", CoerceJavaToLua.coerce((Object)this.keyBindManagerInstance));
        this.globals.set("registry", CoerceJavaToLua.coerce(GlobalsStorage.registry));
        this.globals.set("animator", CoerceJavaToLua.coerce((Object)GlobalsStorage.modelPartAnimator));
        this.globals.set("debugger", CoerceJavaToLua.coerce((Object)GlobalsStorage.debugTextRenderer));
        this.globals.set("data", CoerceJavaToLua.coerce((Object)data));
        this.chunk = this.globals.load(sourceCode);
    }

    public void executeModel(ItemModelContext data, ItemStack itemStack, AbstractClientPlayer player, ModelPartAnimator modelPartAnimator) {
        if (!this.canRun) {
            return;
        }
        try {
            ModelScriptCache.data.set(data);
            this.chunk.call();
        }
        catch (Exception e) {
            System.err.println("[HoldMyItems] Lua runtime error: " + e.getMessage());
            SystemToast.addOrUpdate(Minecraft.getInstance().gui.toastManager(), SystemToast.SystemToastId.PACK_LOAD_FAILURE, Component.nullToEmpty("HMI Lua Runtime error!"), Component.nullToEmpty(e.getMessage()));
            this.canRun = false;
        }
    }
}

