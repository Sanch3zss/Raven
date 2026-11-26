package sanch3zss.raven.feature.module.misc;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.BindMode;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

@Aliases(aliases = {"ClickGui", "Click Gui", "Menu", "Gui", "Меню"})
public class ClickGuiModule extends Module {
    public ClickGuiModule() {
        super(ModuleTab.INTERFACE, "ClickGui");
        setBind(GLFW.GLFW_KEY_RIGHT_SHIFT, BindMode.TOGGLE);
        this.state = true;
    }

    @Override
    public void setState(boolean z) {
    }

    @Override
    public Module setStateSilent(boolean z) {
        return this;
    }

    @Override
    public void setBind(List<Integer> list, BindMode bindMode) {
        super.setBind(list, BindMode.TOGGLE);
    }

    public List<Integer> openKeys() {
        return hasKeyBind() ? getKeyBind() : List.of(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public boolean isOpenKey(int key) {
        List<Integer> keys = openKeys();
        if (keys.getLast() != key) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        return keys.subList(0, keys.size() - 1).stream()
                .allMatch(modifier -> InputConstants.isKeyDown(minecraft.getWindow(), modifier));
    }
}
