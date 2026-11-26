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

final class SkinCatalog {
    static final String ROOT = "/assets/raven/knives/cs2/skins/";
    private static SkinCatalog instance;

    final Map<String, Knife> knives = new HashMap<>();
    final Map<String, Paint> paints = new HashMap<>();
    final Map<String, Integer> channels = new HashMap<>();

    static SkinCatalog get() {
        if (instance == null) instance = new SkinCatalog();
        return instance;
    }

    private SkinCatalog() {
        JsonObject root;
        try (InputStream input = SkinCatalog.class.getResourceAsStream(ROOT + "skins.json")) {
            if (input == null) return;
            root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception exception) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("knives").entrySet()) {
            JsonObject knife = entry.getValue().getAsJsonObject();
            Map<KnifeSkin, String> skins = new EnumMap<>(KnifeSkin.class);
            for (Map.Entry<String, JsonElement> skin : knife.getAsJsonObject("skins").entrySet()) {
                skins.put(KnifeSkin.valueOf(skin.getKey()), skin.getValue().getAsString());
            }
            knives.put(entry.getKey(), new Knife(knife.get("uvScale").getAsFloat(), knife.get("length").getAsFloat(), skins));
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("paints").entrySet()) {
            paints.put(entry.getKey(), paint(entry.getValue().getAsJsonObject()));
        }
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("textures").entrySet()) {
            channels.put(entry.getKey(), entry.getValue().getAsInt());
        }
    }

    Knife knife(String model) {
        return knives.get(model);
    }

    private static Paint paint(JsonObject json) {
        JsonArray colorArray = json.getAsJsonArray("colors");
        float[][] colors = new float[4][];
        for (int i = 0; i < 4; i++) colors[i] = floats(colorArray.get(i).getAsJsonArray());
        List<Roll> rolls = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("roll")) {
            JsonObject roll = element.getAsJsonObject();
            JsonArray values = roll.getAsJsonArray("values");
            float[][] bounds = new float[values.size()][];
            for (int i = 0; i < bounds.length; i++) bounds[i] = floats(values.get(i).getAsJsonArray());
            rolls.add(new Roll(roll.get("name").getAsString(), bounds));
        }
        JsonElement colorRoughness = json.get("colorRoughness");
        return new Paint(json.get("style").getAsInt(), colors, json.get("brightness").getAsFloat(),
                json.get("roughness").getAsFloat(), json.get("metalness").getAsFloat(),
                floats(json.getAsJsonArray("colorMetalness")),
                colorRoughness == null || colorRoughness.isJsonNull() ? null : floats(colorRoughness.getAsJsonArray()),
                json.get("ignoreSize").getAsBoolean(), json.get("wrapU").getAsInt(), json.get("wrapV").getAsInt(),
                json.get("rotation").getAsFloat(), json.get("scale").getAsFloat(), floats(json.getAsJsonArray("offset")),
                floats(json.getAsJsonArray("sprayBlend")), json.get("adjust").getAsInt(), text(json, "pattern"),
                text(json, "roughnessTexture"), json.get("roughnessPatternUv").getAsBoolean(), rolls,
                floats(json.getAsJsonArray("wear")));
    }

    private static String text(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private static float[] floats(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; i++) values[i] = array.get(i).getAsFloat();
        return values;
    }

    record Knife(float uvScale, float length, Map<KnifeSkin, String> skins) {
    }

    record Roll(String name, float[][] bounds) {
    }

    record Paint(int style, float[][] colors, float brightness, float roughness, float metalness,
                 float[] colorMetalness, float[] colorRoughness, boolean ignoreSize, int wrapU, int wrapV,
                 float rotation, float scale, float[] offset, float[] sprayBlend, int adjust, String pattern,
                 String roughnessTexture, boolean roughnessPatternUv, List<Roll> rolls, float[] wear) {
    }
}
