package sanch3zss.raven.ui.menu.layout;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import org.joml.Vector3i;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.command.ClientCommand;
import sanch3zss.raven.client.command.impl.GpsCommand;
import sanch3zss.raven.client.config.LocalPreferencesFile;
import sanch3zss.raven.client.input.*;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.feature.server.funtime.FunTimeActivityService;
import sanch3zss.raven.feature.server.funtime.FunTimeActivityService.*;
import sanch3zss.raven.feature.setting.MultiSelectOption;
import sanch3zss.raven.client.chat.ChatUtil;
import sanch3zss.raven.render.engine.DrawCtx;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.render.data.LayoutScaleContext;
import sanch3zss.raven.render.font.*;
import sanch3zss.raven.render.theme.ColorStack;
import sanch3zss.raven.ui.component.control.*;
import sanch3zss.raven.ui.component.layout.*;
import sanch3zss.raven.ui.menu.MenuStyle;
import sanch3zss.raven.ui.menu.window.MenuWindow;
import sanch3zss.raven.util.math.interpolation.Easings;

public final class FunTimeActivityLayout extends AbstractTabLayout {
    private static final float HEADER_HEIGHT = 88.0f;
    private static final Pattern ANARCHY_NUMBER = Pattern.compile(
            "(?:анархия|anarchy|an)\\D{0,8}(\\d{1,4})", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private final Kind kind;
    private final FunTimeActivityService service;
    private final MsdfFont regular = Fonts.INTER_MEDIUM.get();
    private final MsdfFont semiBold = Fonts.INTER_SEMIBOLD.get();
    private final MsdfFont waypointFont = Fonts.WAYPOINTS.get();
    private final MsdfFont categoryFont = Fonts.CATEGORIES.get();
    private final MultiSelectDropdown<String> primaryFilter = new MultiSelectDropdown<>(
            this.regular, null, MenuStyle.DROPDOWN_RADIUS, 7.0f, 5.0f, 11, 210.0f);
    private final MultiSelectDropdown<String> secondaryFilter = new MultiSelectDropdown<>(
            this.regular, null, MenuStyle.DROPDOWN_RADIUS, 7.0f, 5.0f, 11, 210.0f);
    private final ToggleSwitch autoWarpToggle = new ToggleSwitch(30.0f, 18.0f, () -> this.autoWarpMine);
    private final Set<String> eventStatuses = new LinkedHashSet<>();
    private final Set<String> eventTypes = new LinkedHashSet<>();
    private final Set<String> mineRarities = new LinkedHashSet<>();
    private final Set<String> nextMineRarities = new LinkedHashSet<>();
    private boolean preferencesLoaded;
    private boolean autoWarpMine;
    private String markerMode = "WAYPOINT";
    private final ToggleAnimator markerModeAnimation = new ToggleAnimator(220, Easings.EASE_IN_OUT_CUBIC);
    private int pendingMine = -1;
    private long pendingMineStartedAt;
    private long mineReachedAt;
    private String pendingServerAddress = "";
    private boolean autoWarpListenerRegistered;
    private ScrollbarWidget scrollbar;

    public FunTimeActivityLayout(Kind kind, FunTimeActivityService service) {
        this.kind = kind;
        this.service = service;

        configureDropdowns();
        this.primaryFilter.onSelect(values -> applyDropdownSelection(primaryGroup(),
                this.primaryFilter, values));
        this.secondaryFilter.onSelect(values -> applyDropdownSelection(secondaryGroup(),
                this.secondaryFilter, values));
        this.autoWarpToggle.runnable(this::toggleAutoWarpMine);
    }

    private FilterGroup primaryGroup() {
        return this.kind == Kind.EVENTS ? FilterGroup.EVENT_STATUS : FilterGroup.MINE_CURRENT;
    }

    private FilterGroup secondaryGroup() {
        return this.kind == Kind.EVENTS ? FilterGroup.EVENT_TYPE : FilterGroup.MINE_NEXT;
    }

    private void configureDropdowns() {
        configureDropdown(this.primaryFilter, primaryGroup());
        configureDropdown(this.secondaryFilter, secondaryGroup());
    }

    private void configureDropdown(MultiSelectDropdown<String> dropdown, FilterGroup group) {
        Set<String> selected = selection(group).isEmpty()
                ? Set.of("all") : Set.copyOf(selection(group));
        dropdown.setOptions(options(group), selected);
    }

    private List<MultiSelectOption<String>> options(FilterGroup group) {
        List<MultiSelectOption<String>> all = List.of(option("all", "Все"));
        return switch (group) {
            case EVENT_STATUS -> List.of(
                    all.getFirst(), option("active", "Активные"),
                    option("soon", "Скоро"), option("open", "Открыты"));
            case EVENT_TYPE -> List.of(
                    all.getFirst(), option("airdrop", "Аирдроп"),
                    option("altarundead", "Алтарь нежити"),
                    option("beacon", "Маяк-убийца"),
                    option("hellm", "Адская резня"),
                    option("meteor_rain", "Метеоритный дождь"),
                    option("myst_beacon", "Загадочный маяк"),
                    option("vulkan", "Вулкан"),
                    option("geyser", "Гейзер"),
                    option("deathchest", "Сундук смерти"));
            case MINE_CURRENT, MINE_NEXT -> List.of(
                    all.getFirst(), option("default", "Обычная"),
                    option("mythical", "Мифическая"),
                    option("legendary", "Легендарная"));
        };
    }

    private MultiSelectOption<String> option(String id, String label) {
        return new MultiSelectOption<>(id, label);
    }

    private void applyDropdownSelection(FilterGroup group, MultiSelectDropdown<String> dropdown,
                                        Set<String> values) {
        Set<String> selection = selection(group);
        boolean previouslyAll = selection.isEmpty();
        LinkedHashSet<String> normalized = new LinkedHashSet<>(values);
        if (normalized.contains("all")) {
            if (previouslyAll && normalized.size() > 1) {
                normalized.remove("all");
            } else if (!previouslyAll) {
                normalized.clear();
                normalized.add("all");
            }
        }
        selection.clear();
        normalized.stream().filter(value -> !"all".equals(value)).forEach(selection::add);
        syncDropdownSelection(dropdown, selection);
        this.tab.scrollingAreaComponent().scrollTo(0.0f);
    }

    private static void syncDropdownSelection(MultiSelectDropdown<String> dropdown,
                                              Set<String> selection) {
        dropdown.selectedValues.clear();
        if (selection.isEmpty()) {
            dropdown.selectedValues.add("all");
        } else {
            dropdown.selectedValues.addAll(selection);
        }
        dropdown.updateText();
    }

    private Set<String> selection(FilterGroup group) {
        return switch (group) {
            case EVENT_STATUS -> this.eventStatuses;
            case EVENT_TYPE -> this.eventTypes;
            case MINE_CURRENT -> this.mineRarities;
            case MINE_NEXT -> this.nextMineRarities;
        };
    }

    @Override
    public void initialize(MenuTabElement tabElement) {
        super.initialize(tabElement);
        ScrollArea scrollArea = tabElement.scrollingAreaComponent();
        Supplier<Float> viewport = () -> MenuWindow.MENU_HEIGHT - MenuWindow.HEADER_HEIGHT
                - HEADER_HEIGHT;
        this.scrollbar = new ScrollbarWidget(scrollArea::scrollY, this::getContentHeight,
                viewport, scrollArea::scrollTo, 16.0f, 3.0f, 24.0f);
    }

    @Override
    public void render(DrawCtx drawCtx) {
        loadPreferences();
        this.service.refreshIfStale();
        ColorStack colors = drawCtx.colorStack();
        colors.push();
        Snapshot snapshot = this.service.snapshot();
        int visibleCount;
        if (this.kind == Kind.EVENTS) {
            List<Event> events = filteredEvents(snapshot);
            visibleCount = events.size();
            renderHeader(drawCtx, visibleCount);
            this.tab.scrollingAreaComponent().beginArea(drawCtx, originX(),
                    originY() + HEADER_HEIGHT, width(), height() - HEADER_HEIGHT);
            renderEvents(drawCtx, snapshot, events);
        } else {
            List<Mine> mines = filteredMines(snapshot);
            visibleCount = mines.size();
            renderHeader(drawCtx, visibleCount);
            this.tab.scrollingAreaComponent().beginArea(drawCtx, originX(),
                    originY() + HEADER_HEIGHT, width(), height() - HEADER_HEIGHT);
            renderMines(drawCtx, snapshot, mines);
        }
        this.tab.scrollingAreaComponent().endArea(drawCtx, contentHeight(visibleCount));
        this.scrollbar.render(drawCtx);
        colors.pop();
    }

    @Override
    public void renderOverlays(DrawCtx drawCtx) {
        ColorStack colors = drawCtx.colorStack();
        colors.push();
        renderDropdowns(drawCtx);
        colors.pop();
    }

    private void renderHeader(DrawCtx drawCtx, int visibleCount) {
        ColorStack colors = drawCtx.colorStack();
        int primary = MenuStyle.primaryText(drawCtx);
        int secondary = primary;
        String title = this.kind == Kind.EVENTS ? "Ивенты" : "Шахты";
        float textX = originX() + 10.0f;
        drawCtx.text(this.semiBold, title, 14, textX, originY() + 7.0f, primary);
        drawCtx.text(this.semiBold, primaryFilterLabel(), 9, this.primaryFilter.x(),
                originY() + 42.0f, secondary);
        drawCtx.text(this.semiBold, secondaryFilterLabel(), 9, this.secondaryFilter.x(),
                originY() + 42.0f, secondary);
        float thirdX = thirdColumnX();
        if (this.kind == Kind.EVENTS) {
            drawCtx.text(this.semiBold, "ТИП МЕТКИ",
                    9, thirdX, originY() + 42.0f, secondary);
            renderMarkerMode(drawCtx, thirdX, originY() + 54.0f);
        } else {
            this.autoWarpToggle.render(drawCtx);
            drawCtx.text(this.regular, "Авто /warp mine после входа", 11,
                    thirdX + 37.0f, originY() + 58.0f, primary);
        }
    }

    private void renderMarkerMode(DrawCtx drawCtx, float x, float y) {
        MenuStyle.drawSegmentedControl(drawCtx, this.semiBold, 10, new String[]{"GPS", "Waypoints"},
                x, y, filterColumnWidth(), MenuStyle.DROPDOWN_FIELD_HEIGHT,
                1.0f - this.markerModeAnimation.smoothAnimation(), 0.0f);
    }

    private void renderDropdowns(DrawCtx drawCtx) {
        this.primaryFilter.render(drawCtx);
        this.secondaryFilter.render(drawCtx);
    }

    private String primaryFilterLabel() {
        return this.kind == Kind.EVENTS ? "СТАТУС" : "ТЕКУЩАЯ РЕДКОСТЬ";
    }

    private String secondaryFilterLabel() {
        return this.kind == Kind.EVENTS ? "ТИП СОБЫТИЯ" : "СЛЕДУЮЩАЯ РЕДКОСТЬ";
    }

    private void renderEvents(DrawCtx drawCtx, Snapshot snapshot, List<Event> events) {
        if (events.isEmpty()) {
            renderEmpty(drawCtx, snapshot, "Нет ивентов по этому фильтру");
            return;
        }
        float cardWidth = cardWidth();
        for (int index = 0; index < events.size(); index++) {
            float cardX = originX() + 10.0f + (index % 2) * (cardWidth + 6.0f);
            float cardY = originY() + HEADER_HEIGHT
                    + (index / 2) * (105.0f + 6.0f);
            if (visible(cardY, 105.0f)) {
                renderEventCard(drawCtx, snapshot, events.get(index), cardX, cardY, cardWidth);
            }
        }
    }

    private void renderEventCard(DrawCtx drawCtx, Snapshot snapshot, Event event,
                                 float x, float y, float width) {
        ColorStack colors = drawCtx.colorStack();
        int primary = MenuStyle.primaryText(drawCtx);
        int secondary = primary;
        int phaseColor = colors.computeColor(phaseColor(event.phase()));
        MenuStyle.drawModulePanel(drawCtx, x, y, width, 105.0f, MenuStyle.SECTION_RADIUS);
        float iconArea = 43.0f;
        int iconColor = switch (event.id()) {
            case "beacon" -> colors.computeColor(0xFFFF646C);
            case "myst_beacon" -> colors.computeColor(0xFFF3CE63);
            default -> primary;
        };
        MenuStyle.drawCenteredIcon(drawCtx, this.waypointFont, eventGlyph(event.id()),
                eventGlyphSize(event.id()), x + 7.0f, y + 8.0f, iconArea - 12.0f,
                105.0f - 16.0f, iconColor);
        drawCtx.fillRect(x + iconArea, y + 8.0f, 1.0f, 105.0f - 16.0f,
                MenuStyle.outline(colors));

        float textX = x + iconArea + 10.0f;
        float right = x + width - 10.0f;
        String time = formatDuration(snapshot.remainingSeconds(event.secondsLeft()));
        float timeWidth = drawCtx.textWidthPhysical(this.semiBold, time, 11);
        MenuStyle.drawFadingText(drawCtx, this.semiBold, eventName(event.id()), 12,
                textX, y + 9.0f, Math.max(20.0f, right - timeWidth - 8.0f - textX), primary);
        drawCtx.text(this.semiBold, time, 11, right - timeWidth, y + 9.5f, primary);
        MenuStyle.drawFadingText(drawCtx, this.regular, event.server(), 10, textX, y + 29.0f,
                width - iconArea - 20.0f, secondary);
        drawCtx.text(this.regular, phaseName(event.phase()), 10, textX, y + 46.0f, phaseColor);
        boolean hasCoordinates = event.x() != null && event.z() != null;
        String details = hasCoordinates
                ? "X " + event.x() + (event.y() == null ? "" : "  Y " + event.y())
                        + "  Z " + event.z()
                : event.loot();
        if (!event.loot().isBlank() && hasCoordinates) {
            details += "  |  " + event.loot();
        }
        if (!details.isBlank()) {
            MenuStyle.drawFadingText(drawCtx, this.regular, details, 9,
                    textX, y + 61.0f, right - textX, secondary);
        }
        renderActionButton(drawCtx, textX, y + 79.0f, right - textX,
                canMark(event) ? "Поставить метку" : "Нет координат", canMark(event));
    }

    private void renderMines(DrawCtx drawCtx, Snapshot snapshot, List<Mine> mines) {
        if (mines.isEmpty()) {
            renderEmpty(drawCtx, snapshot, "Нет шахт по этому фильтру");
            return;
        }
        float cardWidth = cardWidth();
        for (int index = 0; index < mines.size(); index++) {
            float cardX = originX() + 10.0f + (index % 2) * (cardWidth + 6.0f);
            float cardY = originY() + HEADER_HEIGHT
                    + (index / 2) * (90.0f + 6.0f);
            if (visible(cardY, 90.0f)) {
                renderMineCard(drawCtx, snapshot, mines.get(index), cardX, cardY, cardWidth);
            }
        }
    }

    private void renderMineCard(DrawCtx drawCtx, Snapshot snapshot, Mine mine,
                                float x, float y, float width) {
        ColorStack colors = drawCtx.colorStack();
        int primary = MenuStyle.primaryText(drawCtx);
        int secondary = primary;
        int rarityColor = colors.computeColor(rarityColor(mine.rarity()));
        MenuStyle.drawModulePanel(drawCtx, x, y, width, 90.0f, MenuStyle.SECTION_RADIUS);
        MenuStyle.drawCenteredIcon(drawCtx, this.categoryFont, MenuStyle.tabGlyph("mines"), 20,
                x + 9.0f, y + 8.0f, 28.0f, 90.0f - 16.0f, rarityColor);
        drawCtx.fillRect(x + 45.0f, y + 8.0f, 1.0f, 90.0f - 16.0f,
                MenuStyle.outline(colors));

        float textX = x + 55.0f;
        float right = x + width - 10.0f;
        String time = formatDuration(snapshot.remainingSeconds(mine.secondsLeft()));
        float timeWidth = drawCtx.textWidthPhysical(this.semiBold, time, 11);
        MenuStyle.drawFadingText(drawCtx, this.semiBold, mine.server(), 12,
                textX, y + 10.0f, Math.max(20.0f, right - timeWidth - 8.0f - textX), primary);
        drawCtx.text(this.semiBold, time, 11, right - timeWidth, y + 10.5f, primary);
        drawCtx.text(this.regular, rarityName(mine.rarity()), 10, textX, y + 32.0f, rarityColor);
        String nextPrefix = "Дальше: ";
        String nextRarity = rarityName(mine.nextRarity());
        float prefixWidth = drawCtx.textWidthPhysical(this.regular, nextPrefix, 9);
        drawCtx.text(this.regular, nextPrefix, 9, textX, y + 47.0f, secondary);
        MenuStyle.drawFadingText(drawCtx, this.regular, nextRarity, 9,
                textX + prefixWidth, y + 47.0f,
                Math.max(10.0f, right - textX - prefixWidth),
                primary);
        int anarchy = anarchyNumber(mine.server());
        renderActionButton(drawCtx, textX, y + 64.0f, right - textX,
                anarchy > 0 ? "Зайти" : "Нет номера сервера",
                anarchy > 0);
    }

    private void renderActionButton(DrawCtx drawCtx, float x, float y, float width,
                                    String label, boolean enabled) {
        ColorStack colors = drawCtx.colorStack();
        colors.push();
        colors.alpha(enabled ? 1.0f : 0.5f);
        MenuStyle.drawSettingButton(drawCtx, x, y, width, 20.0f, 3.0f, 0.0f);
        float textWidth = drawCtx.textWidthPhysical(this.semiBold, label, 10);
        drawCtx.text(this.semiBold, label, 10, x + (width - textWidth) / 2.0f,
                y + (20.0f - this.semiBold.getHeight(10.0f)) / 2.0f, MenuStyle.primaryText(drawCtx));
        colors.pop();
    }

    private void renderEmpty(DrawCtx drawCtx, Snapshot snapshot, String message) {
        int color = MenuStyle.primaryText(drawCtx);
        String text = snapshot.loading() && snapshot.syncedAt() == 0L
                ? "Загрузка данных FunTime..." : message;
        float textWidth = drawCtx.textWidthPhysical(this.regular, text, 11);
        drawCtx.text(this.regular, text, 11, originX() + (width() - textWidth) / 2.0f,
                originY() + HEADER_HEIGHT + 35.0f, color);
    }

    private List<Event> filteredEvents(Snapshot snapshot) {
        return snapshot.events().stream()
                .filter(event -> this.eventStatuses.isEmpty()
                        || this.eventStatuses.stream().anyMatch(status -> matchesStatus(event, status)))
                .filter(event -> this.eventTypes.isEmpty() || this.eventTypes.contains(event.id()))
                .sorted(Comparator.comparingInt((Event event) -> phaseRank(event.phase()))
                        .thenComparingInt(Event::secondsLeft)
                        .thenComparing(Event::server, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<Mine> filteredMines(Snapshot snapshot) {
        return snapshot.mines().stream()
                .filter(mine -> matchesRarity(this.mineRarities, mine.rarity()))
                .filter(mine -> matchesRarity(this.nextMineRarities, mine.nextRarity()))
                .sorted(Comparator.comparingInt((Mine mine) -> rarityRank(mine.rarity()))
                        .thenComparingInt(Mine::secondsLeft)
                        .thenComparing(Mine::server, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private float contentHeight(int count) {
        float cardHeight = this.kind == Kind.EVENTS ? 105.0f : 90.0f;
        return Math.max(1, (count + 1) / 2) * (cardHeight + 6.0f) + 10.0f;
    }

    @Override
    public float getContentHeight() {
        Snapshot snapshot = this.service.snapshot();
        int count = this.kind == Kind.EVENTS
                ? filteredEvents(snapshot).size() : filteredMines(snapshot).size();
        return contentHeight(count);
    }

    private void loadPreferences() {
        if (this.preferencesLoaded || Raven.INSTANCE.configManager() == null) {
            return;
        }
        LocalPreferencesFile preferences = Raven.INSTANCE.configManager().menuStateConfig();
        this.markerMode = preferences.eventMarkerMode();
        this.markerModeAnimation.force("GPS".equals(this.markerMode));
        this.autoWarpMine = preferences.autoWarpMine();
        this.preferencesLoaded = true;
    }

    private void savePreferences() {
        try {
            Raven.INSTANCE.configManager().saveMenuState();
        } catch (IOException exception) {
            Raven.LOGGER.error("Failed to save FunTime activity preferences", exception);
        }
    }

    private void selectMarkerMode(String mode) {
        this.markerMode = mode;
        this.markerModeAnimation.state("GPS".equals(mode));
        Raven.INSTANCE.configManager().menuStateConfig().setEventMarkerMode(mode);
        savePreferences();
    }

    private void toggleAutoWarpMine() {
        this.autoWarpMine = !this.autoWarpMine;
        if (!this.autoWarpMine) {
            this.pendingMine = -1;
        }
        Raven.INSTANCE.configManager().menuStateConfig().setAutoWarpMine(this.autoWarpMine);
        savePreferences();
    }

    private void setEventMarker(Event event) {
        if (!canMark(event)) {
            return;
        }
        String name = eventName(event.id());
        if ("GPS".equals(this.markerMode)) {
            ClientCommand command = Raven.INSTANCE.commandDispatcher().getRepository()
                    .getCommand("gps").orElse(null);
            if (command instanceof GpsCommand gps) {
                gps.setTarget(event.x(), event.z(), name);
            }
        } else {
            String visibleName = eventWaypointPrefix(event.id());
            String waypointName = visibleName.replace(" ", "") + anarchyNumber(event.server())
                    + String.format(Locale.ROOT, "%03d", System.currentTimeMillis() % 1000L);
            String previousName = event.id() + "-" + event.server().replace(' ', '_');
            String legacyName = (name + "-" + event.server()).replace(' ', '_');
            Raven.INSTANCE.waysRepository().getWaypoints().removeIf(
                    waypoint -> waypoint.name().equalsIgnoreCase(previousName)
                            || waypoint.name().equalsIgnoreCase(legacyName));
            Raven.INSTANCE.waysRepository().add(waypointName,
                    new Vector3i(event.x(), event.y(), event.z()), eventGlyph(event.id()), visibleName);
            name = waypointName;
        }
        ChatUtil.addChatMessage(Component.literal(
                "Метка поставлена: " + name).withStyle(ChatFormatting.GRAY));
    }

    private void joinMine(Mine mine) {
        int anarchy = anarchyNumber(mine.server());
        Mc mc = Mc.INSTANCE;
        if (anarchy <= 0 || !mc.isWorldLoaded() || mc.getServerInfo() == null) {
            ChatUtil.addChatMessage(Component.literal("Сначала зайдите на FunTime")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        this.pendingMine = -1;
        if (this.autoWarpMine) {
            if (!this.autoWarpListenerRegistered) {
                Raven.INSTANCE.eventDispatcher().register(ClientTickEvent.class, event -> tickAutoWarpMine());
                this.autoWarpListenerRegistered = true;
            }
            this.pendingMine = anarchy;
            this.pendingMineStartedAt = System.currentTimeMillis();
            this.mineReachedAt = 0L;
            this.pendingServerAddress = mc.getServerInfo().ip;
        }
        mc.getNetworkHandler().sendCommand("an" + anarchy);
    }

    private void tickAutoWarpMine() {
        if (this.pendingMine <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        Mc mc = Mc.INSTANCE;
        if (now - this.pendingMineStartedAt > 30_000L) {
            this.pendingMine = -1;
            return;
        }
        if (!mc.isWorldLoaded()) {
            this.mineReachedAt = 0L;
            return;
        }
        if (mc.getServerInfo() == null
                || !this.pendingServerAddress.equals(mc.getServerInfo().ip)) {
            this.pendingMine = -1;
            return;
        }
        Objective sidebar = mc.getWorld().getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        int currentAnarchy = sidebar == null ? -1 : anarchyNumber(sidebar.getDisplayName().getString());
        if (currentAnarchy != this.pendingMine) {
            this.mineReachedAt = 0L;
            return;
        }
        if (this.mineReachedAt == 0L) {
            this.mineReachedAt = now;
        } else if (now - this.mineReachedAt >= 1_350L) {
            this.pendingMine = -1;
            mc.getNetworkHandler().sendCommand("warp mine");
        }
    }

    private static int anarchyNumber(String server) {
        Matcher matcher = ANARCHY_NUMBER.matcher(server);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private boolean canMark(Event event) {
        return event.x() != null && event.z() != null
                && ("GPS".equals(this.markerMode)
                || event.y() != null && anarchyNumber(event.server()) > 0);
    }

    private static boolean matchesStatus(Event event, String status) {
        return switch (status) {
            case "active" -> "RUNNING".equals(event.phase());
            case "soon" -> "ACTIVATING".equals(event.phase()) || "WAITING".equals(event.phase());
            case "open" -> "LOOTING".equals(event.phase()) || "OPENED".equals(event.phase())
                    || "STOPPING".equals(event.phase());
            default -> false;
        };
    }

    private static boolean matchesRarity(Set<String> selection, Rarity rarity) {
        return selection.isEmpty() || selection.contains(rarity.name().toLowerCase(Locale.ROOT));
    }

    private float cardWidth() {
        return (width() - 10.0f * 2.0f - 6.0f - 13.0f) / 2.0f;
    }

    private float filterColumnWidth() {
        return Math.round((width() - 10.0f * 2.0f - 15.0f - 8.0f * 2.0f) / 3.0f);
    }

    private float thirdColumnX() {
        return originX() + 10.0f + (filterColumnWidth() + 8.0f) * 2.0f;
    }

    private boolean visible(float y, float cardHeight) {
        float scroll = this.tab.scrollingAreaComponent().scrollY();
        float viewportTop = originY() + HEADER_HEIGHT + scroll;
        return y + cardHeight >= viewportTop - 10.0f
                && y <= viewportTop + height() - HEADER_HEIGHT + 10.0f;
    }

    @Override
    public void layout(LayoutScaleContext layoutScaleContext) {
        this.scrollbar.setPosition(originX() + width() - 16.0f - this.scrollbar.width(),
                originY() + HEADER_HEIGHT);
        this.scrollbar.setSize(this.scrollbar.width(), height() - HEADER_HEIGHT);
        this.scrollbar.layout(layoutScaleContext);
        float columnWidth = filterColumnWidth();
        float filterY = originY() + 54.0f;
        this.primaryFilter.setPosition(originX() + 10.0f, filterY);
        this.primaryFilter.layout(layoutScaleContext);
        fitFilter(this.primaryFilter, columnWidth);
        this.secondaryFilter.setPosition(originX() + 10.0f + columnWidth + 8.0f,
                filterY);
        this.secondaryFilter.layout(layoutScaleContext);
        fitFilter(this.secondaryFilter, columnWidth);
        if (this.kind == Kind.MINES) {
            this.autoWarpToggle.setPosition(thirdColumnX(), filterY + 2.0f);
            this.autoWarpToggle.layout(layoutScaleContext);
            this.autoWarpToggle.setClickableArea(thirdColumnX(), filterY,
                    columnWidth, MenuStyle.DROPDOWN_FIELD_HEIGHT);
        }
    }

    private static void fitFilter(MultiSelectDropdown<String> filter, float width) {
        filter.setSize(width, MenuStyle.DROPDOWN_FIELD_HEIGHT);
        filter.headerBounds.withPosition(filter.x(), filter.y()).withSize(width, filter.height());
        filter.clickBehavior.setDimensions(filter.x(), filter.y(), width, filter.height());
    }

    @Override
    public void animation(WeightedEngine weightedEngine) {
        this.scrollbar.animation(weightedEngine);
        this.markerModeAnimation.animate(weightedEngine);
        this.primaryFilter.animation(weightedEngine);
        this.secondaryFilter.animation(weightedEngine);
        if (this.kind == Kind.MINES) {
            this.autoWarpToggle.animation(weightedEngine);
        }
    }

    @Override
    public boolean handleInput(InputEventContext context, boolean consumed) {
        if (!consumed && context.inputEvent() instanceof MouseButtonInput) {
            if (context.inArea(thirdColumnX(), originY() + 54.0f,
                    filterColumnWidth(), MenuStyle.DROPDOWN_FIELD_HEIGHT)) {
                this.primaryFilter.close();
                this.secondaryFilter.close();
            } else if (this.primaryFilter.isOpen() && context.inArea(
                    this.secondaryFilter.headerBounds.x(), this.secondaryFilter.headerBounds.y(),
                    this.secondaryFilter.headerBounds.width(), this.secondaryFilter.headerBounds.height())) {
                this.primaryFilter.close();
            } else if (this.secondaryFilter.isOpen() && context.inArea(
                    this.primaryFilter.headerBounds.x(), this.primaryFilter.headerBounds.y(),
                    this.primaryFilter.headerBounds.width(), this.primaryFilter.headerBounds.height())) {
                this.secondaryFilter.close();
            }
        }
        boolean handled = consumed | this.scrollbar.handleInput(context, consumed);
        handled |= this.secondaryFilter.handleInput(context, handled);
        handled |= this.primaryFilter.handleInput(context, handled);
        if (this.kind == Kind.MINES) {
            handled |= this.autoWarpToggle.handleInput(context, handled);
        } else if (!handled && context.inputEvent() instanceof MouseButtonInput mouse
                && mouse.button() == 0 && mouse.action().press()
                && context.inArea(thirdColumnX(), originY() + 54.0f,
                        filterColumnWidth(), MenuStyle.DROPDOWN_FIELD_HEIGHT)) {
            selectMarkerMode(context.logicalMousePosition().x() < thirdColumnX() + filterColumnWidth() / 2.0f
                    ? "GPS" : "WAYPOINT");
            handled = true;
        }
        if (!handled && context.inputEvent() instanceof MouseButtonInput mouse
                && mouse.button() == 0 && mouse.action().press()) {
            handled = handleCardAction(context);
        }
        return handled | this.tab.scrollingAreaComponent().handleInput(context, handled);
    }

    private boolean handleCardAction(InputEventContext context) {
        if (!context.inArea(originX(), originY() + HEADER_HEIGHT,
                width(), height() - HEADER_HEIGHT)) {
            return false;
        }
        InputEventContext scrolled = context.withMouseOffset(0.0f,
                this.tab.scrollingAreaComponent().scrollY());
        float cardWidth = cardWidth();
        if (this.kind == Kind.EVENTS) {
            List<Event> events = filteredEvents(this.service.snapshot());
            for (int index = 0; index < events.size(); index++) {
                Event event = events.get(index);
                if (!canMark(event)) {
                    continue;
                }
                float x = originX() + 10.0f + (index % 2) * (cardWidth + 6.0f) + 53.0f;
                float y = originY() + HEADER_HEIGHT
                        + (index / 2) * (105.0f + 6.0f) + 79.0f;
                if (scrolled.inArea(x, y, cardWidth - 63.0f, 20.0f)) {
                    setEventMarker(event);
                    return true;
                }
            }
        } else {
            List<Mine> mines = filteredMines(this.service.snapshot());
            for (int index = 0; index < mines.size(); index++) {
                Mine mine = mines.get(index);
                if (anarchyNumber(mine.server()) <= 0) {
                    continue;
                }
                float x = originX() + 10.0f + (index % 2) * (cardWidth + 6.0f) + 55.0f;
                float y = originY() + HEADER_HEIGHT
                        + (index / 2) * (90.0f + 6.0f) + 64.0f;
                if (scrolled.inArea(x, y, cardWidth - 65.0f, 20.0f)) {
                    joinMine(mine);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void positionFrames() {
    }

    @Override
    public float width() {
        return MenuWindow.MENU_WIDTH - MenuStyle.WORKSPACE_X;
    }

    private String eventName(String id) {
        return switch (id) {
            case "beacon" -> "Маяк-убийца";
            case "myst_beacon" -> "Загадочный маяк";
            case "altarundead" -> "Алтарь нежити";
            case "airdrop" -> "Аирдроп";
            case "vulkan" -> "Вулкан";
            case "meteor_rain" -> "Метеоритный дождь";
            case "hellm" -> "Адская резня";
            case "geyser" -> "Гейзер";
            case "deathchest" -> "Сундук смерти";
            default -> id;
        };
    }

    private static String eventWaypointPrefix(String id) {
        return switch (id) {
            case "beacon" -> "Маяк";
            case "myst_beacon" -> "Загадочный маяк";
            case "altarundead" -> "Алтарь нежити";
            case "airdrop" -> "Аирдроп";
            case "vulkan" -> "Вулкан";
            case "meteor_rain" -> "Метеоритный дождь";
            case "hellm" -> "Адская резня";
            case "geyser" -> "Гейзер";
            case "deathchest" -> "Сундук смерти";
            default -> "Ивент";
        };
    }

    private static String eventGlyph(String id) {
        return switch (id) {
            case "geyser" -> "a";
            case "meteor_rain" -> "b";
            case "deathchest" -> "c";
            case "beacon", "myst_beacon" -> "d";
            case "altarundead" -> "e";
            case "hellm" -> "f";
            case "airdrop" -> "g";
            case "vulkan" -> "h";
            default -> "a";
        };
    }

    private static int eventGlyphSize(String id) {
        return switch (id) {
            case "deathchest", "altarundead" -> 23;
            case "airdrop", "geyser" -> 25;
            default -> 24;
        };
    }

    private String phaseName(String phase) {
        return switch (phase) {
            case "ACTIVATING" -> "Активация";
            case "RUNNING" -> "Активен";
            case "WAITING" -> "Ожидание";
            case "LOOTING" -> "Лут / открыт";
            case "STOPPING" -> "Завершается";
            case "OPENED" -> "Открыт";
            case "CLOSED" -> "Закрыт";
            default -> phase.isBlank() ? "Запланирован" : phase;
        };
    }

    private static int phaseColor(String phase) {
        return switch (phase) {
            case "RUNNING" -> 0xFF65D19A;
            case "ACTIVATING", "WAITING" -> 0xFFF0C35A;
            case "LOOTING", "OPENED" -> 0xFF67C9F1;
            default -> 0xFF9CA3AF;
        };
    }

    private String rarityName(Rarity rarity) {
        return switch (rarity) {
            case MYTHICAL -> "Мифическая";
            case LEGENDARY -> "Легендарная";
            default -> "Обычная";
        };
    }

    private static int rarityColor(Rarity rarity) {
        return switch (rarity) {
            case MYTHICAL -> 0xFFC5A0FF;
            case LEGENDARY -> 0xFF69D5FF;
            default -> 0xFF9CA3AF;
        };
    }

    private static int rarityRank(Rarity rarity) {
        return switch (rarity) {
            case LEGENDARY -> 0;
            case MYTHICAL -> 1;
            default -> 2;
        };
    }

    private static int phaseRank(String phase) {
        return switch (phase) {
            case "RUNNING" -> 0;
            case "ACTIVATING", "WAITING" -> 1;
            case "LOOTING", "OPENED" -> 2;
            default -> 3;
        };
    }

    private static String formatDuration(int seconds) {
        int safe = Math.max(0, seconds);
        int minutes = safe / 60;
        int remainder = safe % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, remainder);
    }

    public enum Kind {
        EVENTS,
        MINES
    }

    private enum FilterGroup {
        EVENT_STATUS,
        EVENT_TYPE,
        MINE_CURRENT,
        MINE_NEXT
    }
}
