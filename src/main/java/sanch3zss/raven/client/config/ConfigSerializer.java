package sanch3zss.raven.client.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.google.gson.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.config.repository.ModuleRepository;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;
import sanch3zss.raven.feature.module.misc.InterfaceStyle;
import sanch3zss.raven.render.theme.ColorMode;
import sanch3zss.raven.feature.module.render.armtweaks.SwingAnimationType;
import sanch3zss.raven.client.config.LayoutNode;
import sanch3zss.raven.ui.hud.*;
import sanch3zss.raven.ui.setting.Setting;

public class ConfigSerializer {
    public static final int CURRENT_VERSION = 2;
    private static final Object SNAPSHOT_LOCK = new Object();
    public final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public byte[] serialize(ModuleRepository moduleRepository2, WidgetStack widgetStack2) throws IOException {
        synchronized (SNAPSHOT_LOCK) {
            return this.gson.toJson(buildConfigFile(moduleRepository2, widgetStack2)).getBytes(StandardCharsets.UTF_8);
        }
    }

    public byte[] serialize(ConfigFile configFile) {
        synchronized (SNAPSHOT_LOCK) {
            return this.gson.toJson(configFile).getBytes(StandardCharsets.UTF_8);
        }
    }

    public ConfigFile deserialize(byte[] bArr) throws IOException {
        if (bArr == null || bArr.length == 0) {
            throw new IOException("Config is empty");
        }
        try {
            ConfigFile config = this.gson.fromJson(new String(bArr, StandardCharsets.UTF_8), ConfigFile.class);
            if (config == null) {
                throw new IOException("Config root is empty");
            }
            if (config.version() > CURRENT_VERSION) {
                throw new IOException("Unsupported config version: " + config.version());
            }
            if (config.modules == null) {
                config.modules = new ArrayList<>();
            }
            if (config.widgets == null) {
                config.widgets = new ArrayList<>();
            }
            return config;
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("Invalid config JSON", e);
        }
    }

    public Optional<ConfigFile> tryDeserialize(byte[] bArr) {
        try {
            return Optional.of(deserialize(bArr));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void applyConfig(LoadedConfig loadedConfig) {
        if (loadedConfig == null || loadedConfig.bundle() == null) {
            throw new IllegalArgumentException("Config bundle is missing");
        }
        ModuleRepository moduleRepository = Raven.INSTANCE.moduleRepository();
        WidgetStack widgetStack = Raven.INSTANCE.widgetStack();
        ConfigFile bundle = loadedConfig.bundle();
        if (bundle.themeAccent != null && bundle.themeAccentBright != null) {
            Raven.INSTANCE.updateThemeGradient(bundle.themeAccent, bundle.themeAccentBright);
        }
        ColorMode themeMode = ColorMode.GRADIENT;
        if (bundle.themeColorMode != null) {
            try {
                themeMode = ColorMode.valueOf(bundle.themeColorMode);
            } catch (IllegalArgumentException ignored) {
            }
        }
        Raven.INSTANCE.updateThemeColorMode(themeMode);
        if (bundle.interfaceStyle != null) {
            try {
                Raven.INSTANCE.interfaceStyle(InterfaceStyle.valueOf(bundle.interfaceStyle));
            } catch (IllegalArgumentException ignored) {
            }
        }
        Map<String, Module> modulesById = new HashMap<>();
        Map<String, Module> modulesByName = new HashMap<>();
        for (Module module : moduleRepository.getModules()) {
            modulesById.put(normalize(module.getClass().getSimpleName()), module);
            modulesByName.put(normalize(module.getName()), module);
        }
        Module customTags = modulesByName.get(normalize("CustomTags"));
        if (customTags != null) {
            modulesByName.put(normalize("ESP"), customTags);
        }
        Module popTweaks = modulesByName.get(normalize("PopTweaks"));
        if (popTweaks != null) {
            modulesById.put(normalize("PopChamsModule"), popTweaks);
            modulesByName.put(normalize("Pop Chams"), popTweaks);
            modulesByName.put(normalize("Totem Pop"), popTweaks);
        }
        Module equipmentHud = modulesByName.get(normalize("Equipment HUD"));
        if (equipmentHud != null) {
            modulesById.put(normalize("ArmorHud"), equipmentHud);
            modulesByName.put(normalize("Armor HUD"), equipmentHud);
        }
        Module expTweaks = modulesByName.get(normalize("ExpTweaks"));
        if (expTweaks != null) {
            modulesById.put(normalize("AutoExperienceModule"), expTweaks);
            modulesByName.put(normalize("AutoExperience"), expTweaks);
        }
        Module pets = modulesByName.get(normalize("Pets"));
        if (pets != null) {
            modulesById.put(normalize("PetModule"), pets);
            modulesByName.put(normalize("Pet"), pets);
        }

        Map<Module, Boolean> desiredStates = new LinkedHashMap<>();
        for (ModuleConfigData moduleData : bundle.modules()) {
            if (moduleData == null) {
                continue;
            }
            Module module = modulesById.get(normalize(moduleData.id()));
            if (module == null) {
                module = modulesByName.get(normalize(moduleData.name()));
            }
            if (module == null) {
                continue;
            }
            List<Integer> keys = moduleData.keys() == null ? List.of()
                    : moduleData.keys().stream().filter(Objects::nonNull).toList();
            module.setBind(keys,
                    moduleData.bindType() != null ? moduleData.bindType() : module.getType());
            applySettings(module.getSettings(), moduleData.settings());
            if (module instanceof ArmTweaksModule armTweaks) {
                migrateCs2Knives(armTweaks, moduleData.settings());
            }
            desiredStates.put(module, moduleData.enabled());
        }
        for (ModuleConfigEntry widgetData : bundle.widgets()) {
            applyWidgetEntry(widgetStack, widgetData);
        }

        ConfigApplyContext.begin();
        try {
            for (Map.Entry<Module, Boolean> entry : desiredStates.entrySet()) {
                try {
                    entry.getKey().setState(entry.getValue());
                } catch (RuntimeException e) {
                    Raven.LOGGER.warn("Failed to apply state for module {}", entry.getKey().getName(), e);
                }
            }
            Module widgetsModule = modulesByName.get(normalize("HUD Renderer"));
            if (widgetsModule != null) {
                widgetsModule.setState(true);
            }
        } finally {
            ConfigApplyContext.end();
        }
    }

    public void applySettingNode(List<Setting> list, LayoutNode layoutNode2) {
        applySettings(list, layoutNode2 == null ? List.of() : List.of(layoutNode2));
    }

    private void migrateCs2Knives(ArmTweaksModule module, List<LayoutNode> settings) {
        if (settings == null) return;
        for (LayoutNode group : settings) {
            if (group == null) continue;
            String groupName = normalize(group.name());
            if (!groupName.equals(normalize("Ножи CS2")) && !groupName.equals("cs2 knives")) continue;
            if (group.children() != null) {
                for (LayoutNode child : group.children()) {
                    if (child == null) continue;
                    String name = normalize(child.name());
                    if (name.equals(normalize("Нож")) || name.equals("knife")) {
                        applySettingData(module.knife, child);
                    } else if (name.equals(normalize("Скорость анимации")) || name.equals("animation speed")) {
                        applySettingData(module.knifeSpeed, child);
                    } else if (name.equals(normalize("Осмотреть нож")) || name.equals("inspect knife")) {
                        applySettingData(module.knifeInspect, child);
                    }
                }
            }
            if (group.data() != null && Boolean.TRUE.equals(group.data().get("value"))) {
                module.swingAnimationGroup.setValue(true);
                module.swingAnimationType.select(SwingAnimationType.COUNTER_STRIKE);
            }
        }
    }

    public void applyWidgetEntry(WidgetStack widgetStack2, ModuleConfigEntry moduleConfigEntry) {
        if (moduleConfigEntry == null) {
            return;
        }
        String id = normalize(moduleConfigEntry.id()).replace("armorhud", "equipmenthud");
        String name = normalize(moduleConfigEntry.name()).replace("armorhud", "equipmenthud");
        widgetStack2.widgets().stream().filter(Objects::nonNull)
                .filter(widget -> (!id.isEmpty() && normalize(widget.getClass().getSimpleName()).equals(id))
                        || (!name.isEmpty() && normalize(widget.getName()).equals(name)))
                .findFirst().ifPresent(widget -> {
                    applySettings(widget.getSettings(), moduleConfigEntry.settings());
                    if (Float.isFinite(moduleConfigEntry.x()) && Float.isFinite(moduleConfigEntry.y())) {
                        widget.applySavedPosition(moduleConfigEntry.x(), moduleConfigEntry.y());
                    }
                });
    }

    public void applySettingData(Setting setting, LayoutNode layoutNode2) {
        if (setting == null || layoutNode2 == null) {
            return;
        }
        try {
            setting.loadSerializedData(layoutNode2.data() != null ? layoutNode2.data() : Map.of());
        } catch (RuntimeException e) {
            Raven.LOGGER.warn("Skipping invalid config value for setting {}",
                    setting.getName() == null ? setting.getClass().getSimpleName() : setting.getName().original(), e);
        }
        applySettings(setting.getSerializableChildren(), layoutNode2.children());
    }

    public ConfigFile buildConfigFile(ModuleRepository moduleRepository2, WidgetStack widgetStack2) {
        ConfigFile config = new ConfigFile().version(CURRENT_VERSION)
                .modules(serializeModules(moduleRepository2)).widgets(serializeWidgets(widgetStack2));
        config.themeAccent = Raven.INSTANCE.theme().palette().accentStart().argb();
        config.themeAccentBright = Raven.INSTANCE.theme().palette().accentBright().argb();
        config.themeColorMode = Raven.INSTANCE.theme().palette().colorMode.name();
        config.interfaceStyle = Raven.INSTANCE.interfaceStyle().name();
        return config;
    }

    public List<ModuleConfigData> serializeModules(ModuleRepository moduleRepository2) {
        List<Module> modules = new ArrayList<>(moduleRepository2.getModules());
        modules.sort(Comparator.comparing(module -> normalize(module.getClass().getSimpleName())));
        List<ModuleConfigData> result = new ArrayList<>(modules.size());
        for (Module module : modules) {
            result.add(new ModuleConfigData()
                    .id(module.getClass().getSimpleName())
                    .name(module.getName())
                    .enabled(Raven.INSTANCE.featureControl().shouldSaveEnabled(module))
                    .keys(new ArrayList<>(module.getKeyBind()))
                    .bindType(module.getType())
                    .settings(serializeSettings(module.getSettings())));
        }
        return result;
    }

    public List<ModuleConfigEntry> serializeWidgets(WidgetStack widgetStack2) {
        List<Draggable> widgets = new ArrayList<>(widgetStack2.widgets());
        widgets.sort(Comparator.comparing(widget -> normalize(widget.getClass().getSimpleName())));
        List<ModuleConfigEntry> result = new ArrayList<>(widgets.size());
        for (Draggable widget : widgets) {
            result.add(new ModuleConfigEntry()
                    .id(widget.getClass().getSimpleName())
                    .name(widget.getName())
                    .x(widget.getX())
                    .y(widget.getY())
                    .settings(serializeSettings(widget.getSettings())));
        }
        return result;
    }

    public LayoutNode serializeSetting(Setting setting) {
        LayoutNode node = new LayoutNode()
                .name(setting.getName().original())
                .type(setting.getClass().getSimpleName());
        Map<String, Object> data = setting.toSerializedData();
        if (data != null) {
            node.data().putAll(data);
        }
        node.children(serializeSettings(setting.getSerializableChildren()));
        return node;
    }

    private List<LayoutNode> serializeSettings(List<Setting> settings) {
        if (settings == null || settings.isEmpty()) {
            return new ArrayList<>();
        }
        List<LayoutNode> result = new ArrayList<>();
        for (int index = 0; index < settings.size(); index++) {
            Setting setting = settings.get(index);
            if (!isSerializable(setting)) {
                continue;
            }
            LayoutNode node = serializeSetting(setting).index(index);
            if (!node.data().isEmpty() || !node.children().isEmpty()) {
                result.add(node);
            }
        }
        return result;
    }

    private void applySettings(List<Setting> settings, List<LayoutNode> nodes) {
        if (settings == null || settings.isEmpty() || nodes == null || nodes.isEmpty()) {
            return;
        }
        boolean[] consumed = new boolean[settings.size()];
        for (LayoutNode node : nodes) {
            if (node == null) {
                continue;
            }
            int match = matchingSettingIndex(settings, consumed, node);
            if (match >= 0) {
                consumed[match] = true;
                applySettingData(settings.get(match), node);
            } else {
                Setting nested = findNestedSetting(settings, node);
                if (nested != null) {
                    applySettingData(nested, node);
                }
            }
        }
    }

    private Setting findNestedSetting(List<Setting> settings, LayoutNode node) {
        for (Setting parent : settings) {
            for (Setting child : parent.getSerializableChildren()) {
                if (compatible(child, node) && sameName(child, node)) {
                    return child;
                }
                Setting nested = findNestedSetting(child.getSerializableChildren(), node);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private int matchingSettingIndex(List<Setting> settings, boolean[] consumed, LayoutNode node) {
        Integer savedIndex = node.index();
        if (savedIndex != null && savedIndex >= 0 && savedIndex < settings.size()
                && !consumed[savedIndex] && sameName(settings.get(savedIndex), node)) {
            return savedIndex;
        }

        String savedName = normalize(node.name());
        for (int index = 0; index < settings.size(); index++) {
            Setting setting = settings.get(index);
            if (!consumed[index] && compatible(setting, node)
                    && normalize(setting.getName().original()).equals(savedName)) {
                return index;
            }
        }
        for (int index = 0; index < settings.size(); index++) {
            if (!consumed[index] && sameName(settings.get(index), node)) {
                return index;
            }
        }
        return -1;
    }

    private boolean sameName(Setting setting, LayoutNode node) {
        if (!isSerializable(setting)) {
            return false;
        }
        String currentName = normalize(setting.getName().original());
        String savedName = normalize(node.name());
        if (currentName.equals(savedName)) {
            return true;
        }
        return ((savedName.equals("knife") || savedName.equals("нож (new)") || savedName.equals("реалистичный нож"))
                && currentName.equals("нож"))
                || (savedName.equals("тестовые руки") && currentName.equals("реализм"))
                || (savedName.equals("button") && currentName.equals("бинд"))
                || (savedName.equals("knife animation speed") && currentName.equals("скорость анимации ножа"))
                || (savedName.equals("inspect knife") && currentName.equals("осмотреть нож"))
                || (savedName.equals("color grade") && currentName.equals("цветокоррекция"))
                || (savedName.equals("render.targetesp.rotation.speed")
                && currentName.startsWith(savedName + "."));
    }

    private boolean compatible(Setting setting, LayoutNode node) {
        if (!isSerializable(setting)) {
            return false;
        }
        String type = normalize(node.type());
        return type.isEmpty() || normalize(setting.getClass().getSimpleName()).equals(type);
    }

    private boolean isSerializable(Setting setting) {
        return setting != null && setting.getName() != null && setting.getName().original() != null;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
