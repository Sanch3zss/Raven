package sanch3zss.raven.client.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;

import com.google.gson.*;

import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.account.PreferencesData;
import sanch3zss.raven.client.config.io.AtomicFileWriter;

public class LocalPreferencesFile {
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public final Path filePath;
    public PreferencesData data = new PreferencesData();

    public void load() {
        if (Files.exists(this.filePath, new LinkOption[0])) {
            PreferencesData preferencesData;
            try {
                preferencesData = (PreferencesData) gson.fromJson(Files.readString(this.filePath, StandardCharsets.UTF_8), PreferencesData.class);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
            if (preferencesData != null) {
                this.data = preferencesData;
                ensureCollections();
                applyDpiScale();
            }
        }
    }

    public void save() throws IOException {
        ensureCollections();
        AtomicFileWriter.writeBytes(this.filePath, gson.toJson(this.data).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    public void setDpiScale(boolean z, float f) {
        this.data.autoDpiScale = Boolean.valueOf(z);
        this.data.dpiScaleFactor = Float.valueOf(f);
    }

    public String eventMarkerMode() {
        return "GPS".equals(this.data.eventMarkerMode) ? "GPS" : "WAYPOINT";
    }

    public void setEventMarkerMode(String mode) {
        this.data.eventMarkerMode = mode;
    }

    public boolean autoWarpMine() {
        return Boolean.TRUE.equals(this.data.autoWarpMine);
    }

    public void setAutoWarpMine(boolean enabled) {
        this.data.autoWarpMine = enabled;
    }

    public boolean hasGpsTarget() {
        return Boolean.TRUE.equals(this.data.gpsEnabled)
                && this.data.gpsX != null && this.data.gpsZ != null;
    }

    public int gpsX() {
        return this.data.gpsX;
    }

    public int gpsZ() {
        return this.data.gpsZ;
    }

    public String gpsName() {
        return this.data.gpsName == null ? "" : this.data.gpsName;
    }

    public void setGpsTarget(int x, int z, String name) {
        this.data.gpsX = x;
        this.data.gpsZ = z;
        this.data.gpsName = name;
        this.data.gpsEnabled = true;
    }

    public void disableGps() {
        this.data.gpsEnabled = false;
    }

    public void applyDpiScale() {
        if (this.data.autoDpiScale == null && this.data.dpiScaleFactor == null) {
            return;
        }
        if (Boolean.TRUE.equals(this.data.autoDpiScale)) {
            Raven.INSTANCE.windowController().enableAutoDpiScale();
        } else if (this.data.dpiScaleFactor != null) {
            Raven.INSTANCE.windowController().setManualDpiScaleFactor(this.data.dpiScaleFactor.floatValue());
        }
    }

    public boolean isAutoSaveDisabled(String str) {
        if (str == null) {
            return false;
        }
        ensureCollections();
        return this.data.autoSaveDisabled.contains(str);
    }

    public void setAutoSaveDisabled(String str, boolean z) {
        if (str == null) {
            return;
        }
        ensureCollections();
        if (z) {
            this.data.autoSaveDisabled.add(str);
        } else {
            this.data.autoSaveDisabled.remove(str);
        }
    }

    public void ensureCollections() {
        if (this.data.autoSaveDisabled == null) {
            this.data.autoSaveDisabled = new HashSet();
        }
    }

    public LocalPreferencesFile(Path path) {
        this.filePath = path;
    }
}
