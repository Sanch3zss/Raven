package sanch3zss.raven.feature.module.render;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.NumberSetting;

public final class HudElementModules {
    private HudElementModules() {
    }

    private abstract static class Element extends Module {
        public final NumberSetting hudSize;

        private Element(String name, boolean customSize) {
            super(ModuleTab.INTERFACE, name);
            setStateSilent(true);
            this.hudSize = customSize
                    ? new NumberSetting(Translation.clearText("HUD Size"))
                    .currentValue(1.0f).range(0.5f, 2.0f).step(0.05f)
                    : null;
            if (this.hudSize != null) {
                addSettings(this.hudSize);
            }
        }
    }

    public static final class Watermark extends Element {
        public Watermark() {
            super("Watermark", true);
        }
    }

    public static final class PotionList extends Element {
        public PotionList() {
            super("PotionList", true);
        }
    }

    public static final class TargetHud extends Element {
        public TargetHud() {
            super("Target HUD", true);
        }
    }

    public static final class MediaPlayer extends Element {
        public MediaPlayer() {
            super("MediaPlayer", true);
        }
    }

    public static final class Gif extends Element {
        public Gif() {
            super("Gif", false);
        }
    }

    public static final class Hotkeys extends Element {
        public Hotkeys() {
            super("Hotkeys", true);
        }
    }

    public static final class Coords extends Element {
        public Coords() {
            super("Coords", true);
        }
    }

    public static final class InventoryHud extends Element {
        public InventoryHud() {
            super("Inventory HUD", true);
        }
    }

    public static final class EquipmentHud extends Element {
        public EquipmentHud() {
            super("Equipment HUD", true);
        }
    }

    public static final class Cooldowns extends Element {
        public Cooldowns() {
            super("Cooldowns", true);
        }
    }

    public static final class Notifications extends Element {
        public Notifications() {
            super("Notifications", true);
        }
    }

}
