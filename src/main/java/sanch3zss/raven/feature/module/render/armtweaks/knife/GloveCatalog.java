package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sanch3zss.raven.client.Raven;

final class GloveCatalog {
    static final String ROOT = "/assets/raven/knives/cs2/gloves/";
    private static GloveCatalog instance;

    final Map<GloveSkin, Paint> paints = new EnumMap<>(GloveSkin.class);
    final Map<String, Integer> channels = new HashMap<>();

    static GloveCatalog get() {
        if (instance == null) instance = new GloveCatalog();
        return instance;
    }

    private GloveCatalog() {
        JsonObject root;
        try (InputStream input = GloveCatalog.class.getResourceAsStream(ROOT + "gloves.json")) {
            if (input == null) return;
            root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            Raven.LOGGER.error("Failed to read glove skins", exception);
            return;
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("skins").entrySet()) {
            paints.put(GloveSkin.valueOf(entry.getKey()), paint(entry.getValue().getAsJsonObject()));
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("textures").entrySet()) {
            channels.put(entry.getKey(), entry.getValue().getAsInt());
        }
    }

    private static Paint paint(JsonObject json) {
        float[][] tints = new float[8][];
        JsonArray tintArray = json.getAsJsonArray("tints");
        for (int i = 0; i < 8; i++) tints[i] = floats(tintArray.get(i).getAsJsonArray());
        JsonArray paletteArray = json.getAsJsonArray("palette");
        int[] palette = new int[4];
        for (int i = 0; i < 4; i++) palette[i] = paletteArray.get(i).getAsInt();
        Map<String, Float> scalars = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("scalars").entrySet()) {
            scalars.put(entry.getKey(), entry.getValue().getAsFloat());
        }
        Map<String, float[]> layers = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("layers").entrySet()) {
            layers.put(entry.getKey(), floats(entry.getValue().getAsJsonArray()));
        }
        float[][] damageLevels = new float[4][];
        JsonArray levelArray = json.getAsJsonArray("damageLevels");
        for (int i = 0; i < 4; i++) damageLevels[i] = floats(levelArray.get(i).getAsJsonArray());
        List<SkinCatalog.Roll> rolls = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("rolls")) {
            JsonObject roll = element.getAsJsonObject();
            JsonArray values = roll.getAsJsonArray("values");
            float[][] bounds = new float[values.size()][];
            for (int i = 0; i < bounds.length; i++) bounds[i] = floats(values.get(i).getAsJsonArray());
            rolls.add(new SkinCatalog.Roll(roll.get("name").getAsString(), bounds));
        }
        Map<String, String> textures = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("textures").entrySet()) {
            textures.put(entry.getKey(), entry.getValue().getAsString());
        }
        return new Paint(json.get("pattern").getAsBoolean(), json.get("patternMode").getAsInt(),
                json.get("replaceIndex").getAsInt(), palette, json.get("flip").getAsBoolean(), tints, scalars, layers,
                damageLevels, rolls, textures, floats(json.getAsJsonArray("wear")));
    }

    private static float[] floats(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) values[i] = array.get(i).getAsFloat();
        return values;
    }

    record Paint(boolean pattern, int patternMode, int replaceIndex, int[] palette, boolean flip, float[][] tints,
                 Map<String, Float> scalars, Map<String, float[]> layers, float[][] damageLevels,
                 List<SkinCatalog.Roll> rolls, Map<String, String> textures, float[] wear) {
    }
}
