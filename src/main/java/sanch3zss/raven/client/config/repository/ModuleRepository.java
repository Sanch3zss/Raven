package sanch3zss.raven.client.config.repository;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import sanch3zss.raven.feature.module.Module;

public class ModuleRepository {
    public final Map<Class<? extends Module>, Module> moduleMap;

    public ModuleRepository(List<Supplier<Module>> list) {
        this.moduleMap = list.stream()
                .map(Supplier::get)
                .collect(Collectors.toMap(Module::getClass, Function.identity(), (existing, replacement) -> existing, HashMap::new));
    }

    public Collection<Module> getModules() {
        return Collections.unmodifiableCollection(this.moduleMap.values());
    }

    public <T extends Module> T get(Class<T> cls) {
        Module module = this.moduleMap.get(cls);
        if (module == null) {
            throw new NoSuchElementException("Module not found: " + cls.getName());
        }
        return cls.cast(module);
    }

    public <T extends Module> Optional<T> find(Class<T> cls) {
        return Optional.ofNullable(this.moduleMap.get(cls)).map(cls::cast);
    }

    public Map<Class<? extends Module>, Module> getModuleMap() {
        return this.moduleMap;
    }
}
