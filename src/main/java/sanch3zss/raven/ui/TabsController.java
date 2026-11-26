package sanch3zss.raven.ui;

import java.util.*;

import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.feature.module.ModuleCategory;
import sanch3zss.raven.feature.server.funtime.FunTimeActivityService;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.ui.menu.layout.*;

public class TabsController {
    private final FunTimeActivityService funTimeActivityService = new FunTimeActivityService();

    public FunTimeActivityService funTimeActivityService() {
        return this.funTimeActivityService;
    }
    public final MenuTabElement player = new MenuTabElement(ModuleTab.PLAYER, Translation.clearText("Player"), "player", new ModuleGridLayout());
    public final MenuTabElement visuals = new MenuTabElement(ModuleTab.VISUALS, Translation.clearText("Visuals"), "visuals", new ModuleGridLayout());
    public final MenuTabElement misc = new MenuTabElement(ModuleTab.MISC, Translation.clearText("Misc"), "misc", new ModuleGridLayout());
    public final MenuTabElement interfaceTab = new MenuTabElement(ModuleTab.INTERFACE, Translation.clearText("Interface"), "interface", new ModuleGridLayout());
    public final MenuTabElement alphaSmp = new MenuTabElement(null,
            Translation.clearText("AlphaSmp"), "alphasmp", new ModuleGridLayout(),
            Fonts.ALPHASMP.get(), "b", false);
    public final WorldMapTabLayout worldMapLayout = new WorldMapTabLayout();
    public final MenuTabElement worldMap = new MenuTabElement(null,
            Translation.clearText("WorldMap"), "worldmap", this.worldMapLayout,
            Fonts.ALPHASMP.get(), "c", true);
    public final MenuTabElement alphaPlayer = new MenuTabElement(ModuleTab.ALPHA_PLAYER,
            Translation.clearText("Player"), "alphaplayer", new ModuleGridLayout(),
            Fonts.ALPHASMP.get(), "a", true);
    public final MenuTabElement events = new MenuTabElement(null,
            Translation.clearText("Events"), "events",
            new FunTimeActivityLayout(FunTimeActivityLayout.Kind.EVENTS, this.funTimeActivityService));
    public final MenuTabElement mines = new MenuTabElement(null,
            Translation.clearText("Mines"), "mines",
            new FunTimeActivityLayout(FunTimeActivityLayout.Kind.MINES, this.funTimeActivityService));
    public final ThemesLayout themesLayout = new ThemesLayout();
    public final WaypointsLayout waypointsLayout = new WaypointsLayout();
    public final FriendsLayout friendsLayout = new FriendsLayout();
    public final MacrosLayout macrosLayout = new MacrosLayout();
    public final PartyLayout partyLayout = new PartyLayout();
    public final MenuTabElement theme = new MenuTabElement(null, Translation.clearText("Themes"), "themes",
            this.themesLayout, Fonts.CATEGORIES2.get(), "a", false);
    public final MenuTabElement waypoints = new MenuTabElement(null, Translation.clearText("Waypoints"), "waypoints",
            this.waypointsLayout, Fonts.WAYPOINTS.get(), "a", false);
    public final MenuTabElement friends = new MenuTabElement(null, Translation.clearText("Friends"), "friends",
            this.friendsLayout, Fonts.CATEGORIES2.get(), "c", false);
    public final MenuTabElement macros = new MenuTabElement(null, Translation.clearText("Macro"), "macros",
            this.macrosLayout, Fonts.CATEGORIES2.get(), "b", false);
    public final MenuTabElement party = new MenuTabElement(null, Translation.clearText("Party"), "party",
            this.partyLayout, Fonts.CATEGORIES2.get(), "d", false);
    public final MenuTabElement config = new MenuTabElement(null, Lang.CONFIGS, "configs");
    public final List<MenuTabElement> mainTabElements = List.of(
            this.player, this.visuals, this.misc, this.interfaceTab);
    public final List<MenuTabElement> holyWorldTabElements = List.of(
            this.alphaSmp, this.worldMap, this.alphaPlayer);
    public final List<MenuTabElement> otherTabElements = List.of(this.events, this.mines);
    public final List<MenuTabElement> settingsTabElements = List.of(
            this.friends, this.party, this.waypoints, this.macros, this.theme);
    public final List<MenuTabElement> tabElements = List.of(
            this.player, this.visuals, this.misc, this.interfaceTab,
            this.alphaSmp, this.worldMap, this.alphaPlayer, this.events, this.mines,
            this.friends, this.party, this.waypoints, this.macros, this.theme);
    private boolean alphaSmpExpanded;
    public final Map<ModuleTab, MenuTabElement> tabByModuleTab = Map.of(
            ModuleTab.PLAYER, this.player,
            ModuleTab.VISUALS, this.visuals,
            ModuleTab.MISC, this.misc,
            ModuleTab.INTERFACE, this.interfaceTab,
            ModuleTab.ALPHA_PLAYER, this.alphaPlayer);

    public final EnumMap<ModuleTab, Set<ModuleCategory>> selectedCategories = new EnumMap<>(ModuleTab.class);
    public MenuTabElement current = this.player;
    public MenuTabElement previousTabElement;

    public TabsController() {
        this.alphaSmp.clickableBehavior().clickCallback(() -> this.alphaSmpExpanded = !this.alphaSmpExpanded);
        for (ModuleTab tab : ModuleTab.values()) {
            this.selectedCategories.put(tab, new HashSet());
        }
    }

    public void open(MenuTabElement tabElement2) {
        if (tabElement2 == null || !tabElement2.active()) {
            return;
        }
        this.previousTabElement = this.current;
        this.current = tabElement2;
        tabElement2.open();
    }

    public void revert() {
        open(this.previousTabElement);
    }

    public MenuTabElement tabElement(ModuleTab tab) {
        if (tab == null) {
            return null;
        }
        return this.tabByModuleTab.get(tab);
    }

    public void ensureCategoryVisible(MenuTabElement tabElement2, ModuleCategory category) {
        Set<ModuleCategory> set;
        if (tabElement2 == null || category == null) {
            return;
        }
        ModuleTab moduleTab = tabElement2.moduleTab();
        if (category.supports(moduleTab) && (set = this.selectedCategories.get(moduleTab)) != null && !set.isEmpty() && set.add(category)) {
            applyCategoryFilter(moduleTab);
        }
    }

    public Set<ModuleCategory> selectedCategories(ModuleTab tab) {
        return tab == null ? Set.of() : Collections.unmodifiableSet((Set) this.selectedCategories.getOrDefault(tab, Set.of()));
    }

    public void toggleCategory(ModuleTab tab, ModuleCategory category) {
        if (tab == null || category == null || !category.supports(tab)) {
            return;
        }
        Set<ModuleCategory> set = this.selectedCategories.get(tab);
        if (set.contains(category)) {
            set.remove(category);
        } else {
            set.add(category);
        }
        applyCategoryFilter(tab);
    }

    public void applyCategoryFilter(ModuleTab tab) {
        MenuTabElement tabElement = tabElement(tab);
        if (tabElement == null) {
            return;
        }
        tabElement.applyCategoryFilter(selectedCategories(tab));
    }

    public void setActive(MenuTabElement tabElement2, boolean z) {
        if (tabElement2 == null) {
            return;
        }
        tabElement2.setActive(z);
    }

    public MenuTabElement player() {
        return this.player;
    }

    public MenuTabElement visuals() {
        return this.visuals;
    }

    public MenuTabElement misc() {
        return this.misc;
    }

    public MenuTabElement interfaceTab() {
        return this.interfaceTab;
    }

    public MenuTabElement events() {
        return this.events;
    }

    public MenuTabElement mines() {
        return this.mines;
    }

    public MenuTabElement theme() {
        return this.theme;
    }

    public MenuTabElement config() {
        return this.config;
    }

    public Map<ModuleTab, MenuTabElement> tabByModuleTab() {
        return this.tabByModuleTab;
    }

    public EnumMap<ModuleTab, Set<ModuleCategory>> selectedCategories() {
        return this.selectedCategories;
    }

    public MenuTabElement current() {
        return this.current;
    }

    public MenuTabElement previousTabElement() {
        return this.previousTabElement;
    }

    public List<MenuTabElement> tabElements() {
        return this.tabElements;
    }

    public List<MenuTabElement> mainTabElements() {
        return this.mainTabElements;
    }

    public List<MenuTabElement> otherTabElements() {
        return this.otherTabElements;
    }

    public List<MenuTabElement> holyWorldTabElements() {
        return this.holyWorldTabElements;
    }

    public List<MenuTabElement> settingsTabElements() {
        return this.settingsTabElements;
    }

    public void closeSettingsPages() {
        this.themesLayout.close();
        this.waypointsLayout.close();
        this.friendsLayout.close();
        this.partyLayout.close();
        this.macrosLayout.close();
    }

    public boolean alphaSmpExpanded() {
        return this.alphaSmpExpanded;
    }
}
