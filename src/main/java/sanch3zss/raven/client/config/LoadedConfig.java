package sanch3zss.raven.client.config;

import java.util.Objects;

public final class LoadedConfig {
    public final ConfigFile bundle;

    public LoadedConfig(ConfigFile configFile) {
        this.bundle = configFile;
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "bundle=" + this.bundle + "]";
    }

    @Override
    public final int hashCode() {
        return Objects.hash(this.bundle);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof LoadedConfig)) return false;
        LoadedConfig o = (LoadedConfig) obj;
        return Objects.equals(this.bundle, o.bundle);
    }

    public ConfigFile bundle() {
        return this.bundle;
    }
}

