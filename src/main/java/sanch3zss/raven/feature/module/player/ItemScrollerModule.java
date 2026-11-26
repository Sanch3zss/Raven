package sanch3zss.raven.feature.module.player;

import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.player.ItemScrollerController;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

@Aliases(aliases = {"Item Scrolling", "Item Scroller", "Item Scroll", "Fast Item Move", "Mouse Tweaks"})
public class ItemScrollerModule extends Module {
    public ItemScrollerModule() {
        super(ModuleTab.PLAYER, "ItemScroller");
    }

    @Override
    public void deactivate() {
        ItemScrollerController.reset();
        super.deactivate();
    }
}
