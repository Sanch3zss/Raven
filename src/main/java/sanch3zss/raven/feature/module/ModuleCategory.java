package sanch3zss.raven.feature.module;

import java.util.*;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

public enum ModuleCategory {
    ATTACK(Lang.CATEGORY_ATTACK, "/icons/menu/new/tabs/combat.png", ModuleTab.MISC),
    DEFENSE(Lang.CATEGORY_DEFENSE, "/icons/menu/new/shield.png", ModuleTab.MISC),
    AUTOMATION(Lang.CATEGORY_AUTOMATION, "/icons/menu/new/binary.png", ModuleTab.PLAYER, ModuleTab.MISC),
    EXPLOITS(Lang.CATEGORY_EXPLOITS, "/icons/menu/new/debug.png", ModuleTab.PLAYER, ModuleTab.MISC),
    BOOST(Lang.CATEGORY_BOOST, "/icons/menu/new/bolt.png", ModuleTab.PLAYER),
    ANTI_LIMITS(Lang.CATEGORY_ANTI_LIMITS, "/icons/menu/new/cross.png", ModuleTab.PLAYER),
    CONVENIENCE(Lang.CATEGORY_CONVENIENCE, "/icons/menu/new/spark.png", ModuleTab.PLAYER, ModuleTab.MISC),
    VISUAL_SCREEN(Lang.CATEGORY_SCREEN, "/icons/menu/new/display.png", ModuleTab.VISUALS),
    VISUAL_PLAYERS(Lang.CATEGORY_PLAYERS, "/icons/menu/new/group.png", ModuleTab.VISUALS),
    VISUAL_WORLD(Lang.CATEGORY_WORLD, "/icons/menu/new/planet.png", ModuleTab.VISUALS),
    VISUAL_EFFECTS(Lang.CATEGORY_EFFECTS, "/icons/menu/new/spark.png", ModuleTab.VISUALS),
    INTERFACE(Lang.CATEGORY_INTERFACE, "/icons/menu/new/frame.png", ModuleTab.INTERFACE),
    ALPHA_PLAYER(Translation.clearText("Игрок"), "/icons/menu/new/tabs/player.png", ModuleTab.ALPHA_PLAYER),
    UTILITIES(Lang.CATEGORY_UTILITIES, "/icons/menu/new/leaf.png", ModuleTab.PLAYER, ModuleTab.MISC);

    public final Translation displayName;
    public final String iconPath;
    public GlTexture iconTexture = null;
    public final Set<ModuleTab> moduleTabs;

    ModuleCategory(Translation translation, String str, ModuleTab... arr) {
        this.displayName = translation;
        this.iconPath = str;
        this.moduleTabs = Set.of(arr);
    }

    public String displayName() {
        return this.displayName.effective();
    }

    public GlTexture icon() {
        if (this.iconTexture == null) {
            this.iconTexture = new GlTexture(new ClasspathResource(this.iconPath));
        }
        return this.iconTexture;
    }

    public boolean supports(ModuleTab tab) {
        return tab != null && this.moduleTabs.contains(tab);
    }

    public static List<ModuleCategory> categoriesForTab(ModuleTab tab) {
        if (tab == null) {
            return Collections.emptyList();
        }
        return switch (tab) {
            case PLAYER -> List.of(AUTOMATION, CONVENIENCE, ANTI_LIMITS, BOOST);
            case VISUALS -> List.of(VISUAL_SCREEN, VISUAL_PLAYERS, VISUAL_WORLD, VISUAL_EFFECTS);
            case MISC -> List.of(AUTOMATION, UTILITIES, CONVENIENCE, EXPLOITS, DEFENSE);
            case INTERFACE -> List.of(INTERFACE);
            case ALPHA_PLAYER -> List.of();
            default -> Collections.emptyList();
        };
    }

    public static List<ModuleTab> supportedTabs(ModuleCategory category) {
        if (category == null || category.moduleTabs == null) {
            return List.of();
        }
        return Arrays.stream(ModuleTab.values())
                .filter(category.moduleTabs::contains)
                .toList();
    }

    public Translation getDisplayName() {
        return this.displayName;
    }

    public String getIconPath() {
        return this.iconPath;
    }

    public Set<ModuleTab> getModuleTabs() {
        return this.moduleTabs;
    }
}
