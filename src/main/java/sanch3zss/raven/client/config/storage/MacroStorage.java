package sanch3zss.raven.client.config.storage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

import com.google.gson.*;

import sanch3zss.raven.client.config.io.AtomicFileWriter;
import sanch3zss.raven.client.config.repository.MacroRepository;
import sanch3zss.raven.client.config.serialization.MacroSetTypeToken;
import sanch3zss.raven.feature.macro.MacroEntry;

public class MacroStorage {
    public final Path filePath;
    public final MacroRepository repository;
    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public static final Type macroSetType = new MacroSetTypeToken().getType();

    public void save() throws IOException {
        AtomicFileWriter.writeBytes(this.filePath, gson.toJson(Set.copyOf(this.repository.getMacroList()), macroSetType).getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    public void load() throws IOException {
        if (Files.exists(this.filePath, new LinkOption[0])) {
            Set set = (Set) gson.fromJson(Files.readString(this.filePath, StandardCharsets.UTF_8), macroSetType);
            if (set == null) {
                return;
            }
            List<MacroEntry> macroList = this.repository.getMacroList();
            macroList.clear();
            macroList.addAll(set);
        }
    }

    public MacroStorage(Path path, MacroRepository macroRepo) {
        this.filePath = path;
        this.repository = macroRepo;
    }
}
