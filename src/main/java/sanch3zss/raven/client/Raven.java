package sanch3zss.raven.client;


import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.ui.window.WindowControllerAdapter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import discord.DiscordManager;
import org.slf4j.*;

import sanch3zss.raven.client.account.*;
import sanch3zss.raven.client.command.CommandManager;
import sanch3zss.raven.client.command.impl.*;
import sanch3zss.raven.client.config.*;
import sanch3zss.raven.client.config.repository.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.service.MemoryTrimmer;
import sanch3zss.raven.event.EventDispatcher;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.inventory.InventoryService;
import sanch3zss.raven.feature.module.render.party.PartyService;
import sanch3zss.raven.feature.module.ModuleProvider;
import sanch3zss.raven.feature.module.misc.InterfaceStyle;
import sanch3zss.raven.feature.server.pinned.PinnedServersController;
import sanch3zss.raven.feature.server.holyworld.HolyWorldFeatureControl;
import sanch3zss.raven.feature.server.RavenUserService;
import sanch3zss.raven.feature.server.ServerUtil;import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.TabsController;
import sanch3zss.raven.ui.hud.WidgetStack;
import sanch3zss.raven.ui.menu.card.*;
import sanch3zss.raven.ui.menu.layout.*;
import sanch3zss.raven.ui.menu.window.MenuWindow;
import sanch3zss.raven.ui.window.WindowController;

public class Raven {
    public static final Logger LOGGER = LoggerFactory.getLogger(Raven.class);
    public static Raven INSTANCE;
    public final WidgetStack widgetStack = new WidgetStack();

    public final ScheduledExecutorService executor = Executors.newScheduledThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors() / 2), runnable -> {
        Thread thread = new Thread(runnable, "raven-worker");
        thread.setDaemon(true);
        return thread;
    });

    public final WaysRepository waysRepository = new WaysRepository();

    public final WindowController windowController = new WindowController();

    public final WindowControllerAdapter windowControllerAdapter = new WindowControllerAdapter(this.windowController);

    public final TabsController tabsController = new TabsController();

    public final PinnedServersController pinnedServersController = new PinnedServersController();

    public final RavenUserService ravenUserService = new RavenUserService();

    public final HolyWorldFeatureControl featureControl = new HolyWorldFeatureControl();

    public final boolean devMode = detectDevMode();

    public ConfigManager configManager;

    public CommandManager commandDispatcher;

    public final MacroRepository macroRepository;

    public final DiscordManager discordManager;
    public final MemoryTrimmer memoryTrimmer = new MemoryTrimmer();

    public final InventoryService inventoryService;

    public final HandlerRepository handlerRepository;

    public final EventDispatcher eventDispatcher;
    public final PartyService partyService;

    public final NotificationRepository notificationRepository;

    public final ModuleProvider moduleProvider;

    public final ModuleRepository moduleRepository;

    public final ListenerRepository listenerRepository;

    public ConfigFile defaultConfigFile;

    public final Lang languages;

    public DrawEngine drawEngine;

    public UserSession userSession;

    public final ClientInfo clientInfo;

    public MenuWindow menuWindow;

    public Theme theme;
    public InterfaceStyle interfaceStyle = InterfaceStyle.DARK;
    public boolean viaLoaded;

    public Raven(UserSession userSession) {
        if (INSTANCE != null) {
            throw new IllegalStateException(getClass().getName() + " already initialized!");
        }
        INSTANCE = this;
        this.theme = ThemeData.defaultDark();
        this.userSession = userSession;
        this.languages = new Lang();
        this.languages.load();
        this.pinnedServersController.init();
        this.eventDispatcher = new EventDispatcher();
        this.partyService = new PartyService();
        this.macroRepository = new MacroRepository();
        this.notificationRepository = new NotificationRepository();
        this.listenerRepository = new ListenerRepository();
        this.moduleProvider = new ModuleProvider();
        this.moduleRepository = new ModuleRepository(this.moduleProvider.getAll());
        captureDefaultConfig();
        this.handlerRepository = new HandlerRepository();
        this.inventoryService = new InventoryService();
        this.clientInfo = new ClientInfo("4.1.3", "Stable", "09.03.2026");
        this.discordManager = new DiscordManager();
        this.discordManager.init();
        initConfigs();
        registerCommands();
        setupMenu();
        this.tabsController.worldMapLayout.initializeAtlas();
        this.featureControl.initialize();
        this.configManager.loadLocalConfig();
        this.ravenUserService.init();
        this.executor.scheduleAtFixedRate(this.configManager::saveLocalConfig, 30L, 30L, TimeUnit.SECONDS);
        this.memoryTrimmer.start(this.executor);
        registerShutdownHook();
        this.viaLoaded = false;
    }

    public void captureDefaultConfig() {
        try {
            ConfigSerializer configSerializer = new ConfigSerializer();
            this.defaultConfigFile = configSerializer.deserialize(configSerializer.serialize(this.moduleRepository, this.widgetStack));
        } catch (IOException e) {
            LOGGER.error("Failed to capture default config", e);
        }
    }

    public void registerCommands() {
        this.commandDispatcher = new CommandManager();
        this.commandDispatcher.registerCommand(new BindCommand());
        this.commandDispatcher.registerCommand(new MacroCommand());
        this.commandDispatcher.registerCommand(new ReconnectCommand());
        this.commandDispatcher.registerCommand(new GpsCommand());
        this.commandDispatcher.registerCommand(new WaypointCommand());
        this.commandDispatcher.registerCommand(new FriendCommand());
        this.commandDispatcher.registerCommand(new PartyCommand());
        this.commandDispatcher.registerCommand(new HelpCommand(this.commandDispatcher));
        this.commandDispatcher.registerCommand(new PrefixCommand());
        this.commandDispatcher.registerCommand(new BoxCommand());
    }

    public void setupMenu() {
        this.menuWindow = new MenuWindow();
        this.windowController.newWindow(this.menuWindow);
        Map.of(ModuleTab.PLAYER, this.tabsController.player(), ModuleTab.VISUALS, this.tabsController.visuals(),
                ModuleTab.MISC, this.tabsController.misc(), ModuleTab.INTERFACE, this.tabsController.interfaceTab(),
                ModuleTab.ALPHA_PLAYER, this.tabsController.alphaPlayer).forEach((tab, targetTab) -> {
            this.moduleRepository.getModules().stream().filter(module -> {
                return module.getModuleTab() == tab;
            }).forEach(module -> {
                String[] aliases = module.getClass().isAnnotationPresent(Aliases.class) ? module.getClass().getAnnotation(Aliases.class).aliases() : new String[0];
                targetTab.newFrame(new ModuleCard(module, aliases));
            });
        });
        ConfigTabLayout configTabLayout = new ConfigTabLayout();
        this.tabsController.config().setRenderStrategy(configTabLayout);
        configTabLayout.updateConfigList();
    }

    public void initConfigs() {
        try {
            this.configManager = new ConfigManager(GamePaths.configDirectory(), this.waysRepository, this.macroRepository);
            this.configManager.loadMacros();
            this.configManager.loadFriends();
            this.configManager.loadWays();
            this.configManager.loadMenuState();
        } catch (Exception e) {
            LOGGER.error("Failed to init configs", e);
        }
    }

    public void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.executor.shutdownNow();
            try {
                this.executor.awaitTermination(5L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            this.discordManager.stopRPC();
            try {
                this.configManager.saveMacros();
                this.configManager.saveFriends();
                this.configManager.saveWays();
                this.configManager.saveMenuState();
                this.configManager.saveLocalConfig();
            } catch (Exception e2) {
                LOGGER.error("Failed to save on shutdown", e2);
            }
        }));
    }

    public boolean devMode() {
        return this.devMode;
    }

    public static boolean detectDevMode() {
        return ManagementFactory.getRuntimeMXBean().getInputArguments().stream().anyMatch(str -> {
            return str.contains("-Ddebugger.agent.enable.coroutines=true");
        });
    }

    public WidgetStack widgetStack() {
        return this.widgetStack;
    }

    public ScheduledExecutorService executor() {
        return this.executor;
    }

    public WaysRepository waysRepository() {
        return this.waysRepository;
    }

    public WindowController windowController() {
        return this.windowController;
    }

    public WindowControllerAdapter windowControllerAdapter() {
        return this.windowControllerAdapter;
    }

    public TabsController tabsController() {
        return this.tabsController;
    }

    public PinnedServersController pinnedServersController() {
        return this.pinnedServersController;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    public CommandManager commandDispatcher() {
        return this.commandDispatcher;
    }

    public MacroRepository macroRepository() {
        return this.macroRepository;
    }

    public DiscordManager discordManager() {
        return this.discordManager;
    }

    public InventoryService inventoryService() {
        return this.inventoryService;
    }

    public HandlerRepository handlerRepository() {
        return this.handlerRepository;
    }

    public EventDispatcher eventDispatcher() {
        return this.eventDispatcher;
    }

    public PartyService partyService() {
        return this.partyService;
    }

    public NotificationRepository notificationRepository() {
        return this.notificationRepository;
    }

    public ModuleProvider moduleProvider() {
        return this.moduleProvider;
    }

    public ModuleRepository moduleRepository() {
        return this.moduleRepository;
    }

    public ListenerRepository listenerRepository() {
        return this.listenerRepository;
    }

    public HolyWorldFeatureControl featureControl() {
        return this.featureControl;
    }

    public ConfigFile defaultConfigFile() {
        return this.defaultConfigFile;
    }

    public Lang languages() {
        return this.languages;
    }

    public DrawEngine drawEngine() {
        return this.drawEngine;
    }

    public UserSession userSession() {
        return this.userSession;
    }

    public ClientInfo clientInfo() {
        return this.clientInfo;
    }

    public MenuWindow menuWindow() {
        return this.menuWindow;
    }

    public Theme theme() {
        return this.theme;
    }

    public InterfaceStyle interfaceStyle() {
        return this.interfaceStyle;
    }

    public void interfaceStyle(InterfaceStyle style) {
        this.interfaceStyle = style;
    }

    public void updateThemeAccent(int argb) {
        if (this.theme == null) {
            return;
        }
        updateThemeGradient(argb, this.theme.palette().accentBright().argb());
    }

    public void updateThemeAccentBright(int argb) {
        if (this.theme == null) {
            return;
        }
        updateThemeGradient(this.theme.palette().accentStart().argb(), argb);
    }

    public void updateThemeColorMode(ColorMode mode) {
        if (this.theme == null || mode == null || this.theme.palette().colorMode == mode) {
            return;
        }
        this.theme = Theme.of(this.theme.id(), this.theme.name(), this.theme.owner(), this.theme.kind(),
                this.theme.type(), this.theme.palette().withColorMode(mode),
                this.theme.createdAt(), Instant.now());
    }

    public void updateThemeGradient(int first, int second) {
        if (this.theme == null) {
            return;
        }
        ThemePalette current = this.theme.palette();
        ThemePalette updated = new ThemePalette(
                new ColorValue(0xFF000000 | (first & 0xFFFFFF)),
                new ColorValue((second >>> 24) == 0 ? 0xFF000000 | second : second),
                current.favorite(), current.frameBackground(), current.text(), current.error(),
                current.surfaceOutline(), current.surfaceBackground(), current.colorMode);
        this.theme = Theme.of(this.theme.id(), this.theme.name(), this.theme.owner(), this.theme.kind(),
                this.theme.type(), updated, this.theme.createdAt(), Instant.now());
    }

    public boolean viaLoaded() {
        return this.viaLoaded;
    }

    public Raven drawEngine(DrawEngine drawEngine) {
        this.drawEngine = drawEngine;
        return this;
    }
}
