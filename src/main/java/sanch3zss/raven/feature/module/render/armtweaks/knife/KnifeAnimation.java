package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

final class KnifeAnimation {
    private final Map<String, Clip> clips = new HashMap<>();

    KnifeAnimation(String folder, String name) throws Exception {
        String path = "/assets/raven/" + folder + "/animations/melee/" + name + ".animation.json";
        try (InputStream input = KnifeAnimation.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException(path);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("animations").entrySet()) {
                JsonObject json = entry.getValue().getAsJsonObject();
                float duration = json.has("animation_length") ? json.get("animation_length").getAsFloat() : 0.0f;
                Map<String, BoneTrack> tracks = new HashMap<>();
                if (json.has("bones")) {
                    for (Map.Entry<String, JsonElement> bone : json.getAsJsonObject("bones").entrySet()) {
                        JsonObject channels = bone.getValue().getAsJsonObject();
                        tracks.put(bone.getKey(), new BoneTrack(
                                readChannel(channels, "position"),
                                readChannel(channels, "rotation"),
                                readChannel(channels, "scale")));
                    }
                }
                List<Sound> sounds = new ArrayList<>();
                if (json.has("sound_effects")) {
                    for (Map.Entry<String, JsonElement> sound : json.getAsJsonObject("sound_effects").entrySet()) {
                        String effect = sound.getValue().getAsJsonObject().get("effect").getAsString();
                        if (!effect.startsWith("cs2knives:")) continue;
                        sounds.add(new Sound(Float.parseFloat(sound.getKey()), Identifier.fromNamespaceAndPath("raven",
                                "knife." + effect.substring(effect.lastIndexOf('/') + 1))));
                    }
                    sounds.sort(Comparator.comparingDouble(Sound::time));
                }
                clips.put(entry.getKey(), new Clip(duration, tracks, sounds));
            }
        }
    }

    Clip clip(String name) {
        return clips.get(name);
    }

    void borrowClips(KnifeAnimation source, Set<String> clipNames, Set<String> bones) {
        for (String name : clipNames) {
            Clip clip = source.clip(name);
            if (clip == null) continue;
            Map<String, BoneTrack> tracks = new HashMap<>();
            for (Map.Entry<String, BoneTrack> entry : clip.bones().entrySet()) {
                if (bones.contains(entry.getKey())) tracks.put(entry.getKey(), entry.getValue());
            }
            Clip replaced = clips.get(name);
            clips.put(name, new Clip(clip.duration(), tracks, replaced == null ? clip.sounds() : replaced.sounds()));
        }
    }

    void borrowSounds(KnifeAnimation source) {
        for (Map.Entry<String, Clip> entry : clips.entrySet()) {
            String name = entry.getKey();
            Clip clip = entry.getValue();
            Clip match = source.clip(name);
            if (match == null) {
                String kind = name.contains("_") ? name.substring(0, name.indexOf('_')) : name;
                float closest = 0.1f;
                for (Map.Entry<String, Clip> candidate : source.clips.entrySet()) {
                    if (!candidate.getKey().startsWith(kind) || clips.containsKey(candidate.getKey())) continue;
                    float difference = Math.abs(candidate.getValue().duration() - clip.duration());
                    if (difference < closest) {
                        closest = difference;
                        match = candidate.getValue();
                    }
                }
            }
            if (match == null) continue;
            float stretch = match.duration() > 0.0f ? clip.duration() / match.duration() : 1.0f;
            List<Sound> sounds = new ArrayList<>();
            for (Sound sound : match.sounds()) sounds.add(new Sound(sound.time() * stretch, sound.event()));
            entry.setValue(new Clip(clip.duration(), clip.bones(), sounds));
        }
    }

    static Clip withSounds(Clip motion, Clip sounds, Clip timing) {
        List<Sound> result = new ArrayList<>();
        int count = Math.min(sounds.sounds().size(), timing.sounds().size());
        for (int i = 0; i < count; i++) {
            result.add(new Sound(timing.sounds().get(i).time(), sounds.sounds().get(i).event()));
        }
        return new Clip(motion.duration(), motion.bones(), result);
    }

    Set<String> boneNames() {
        Set<String> names = new HashSet<>();
        for (Clip clip : clips.values()) names.addAll(clip.bones().keySet());
        return names;
    }

    private static Channel readChannel(JsonObject bone, String name) {
        if (!bone.has(name)) return null;
        JsonElement element = bone.get(name);
        if (!element.isJsonObject()) {
            float[] value = vector(element);
            return new Channel(List.of(new Key(0.0f, value, value, false)));
        }
        List<Key> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            float time = Float.parseFloat(entry.getKey());
            JsonElement frame = entry.getValue();
            if (frame.isJsonObject()) {
                JsonObject object = frame.getAsJsonObject();
                JsonElement post = object.has("post") ? object.get("post") : object.get("pre");
                JsonElement pre = object.has("pre") ? object.get("pre") : post;
                boolean catmull = object.has("lerp_mode") && "catmullrom".equals(object.get("lerp_mode").getAsString());
                keys.add(new Key(time, vector(pre), vector(post), catmull));
            } else {
                float[] value = vector(frame);
                keys.add(new Key(time, value, value, false));
            }
        }
        keys.sort(Comparator.comparingDouble(Key::time));
        return new Channel(keys);
    }

    private static float[] vector(JsonElement element) {
        if (element.isJsonPrimitive()) {
            float value = element.getAsFloat();
            return new float[]{value, value, value};
        }
        JsonArray array = element.getAsJsonArray();
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    record Key(float time, float[] pre, float[] post, boolean catmull) {}

    static final class Channel {
        private final List<Key> keys;

        Channel(List<Key> keys) {
            this.keys = keys;
        }

        float[] sample(float time) {
            if (keys.isEmpty()) return null;
            if (time <= keys.getFirst().time) return keys.getFirst().post;
            for (int i = 1; i < keys.size(); i++) {
                Key right = keys.get(i);
                if (time > right.time) continue;
                Key left = keys.get(i - 1);
                float range = right.time - left.time;
                if (range <= 0.0f) return right.post;
                float amount = (time - left.time) / range;
                float[] result = new float[3];
                boolean catmull = left.catmull || right.catmull;
                for (int axis = 0; axis < 3; axis++) {
                    float a = catmull ? left.pre[axis] : left.post[axis];
                    float b = right.pre[axis];
                    if (catmull) {
                        float previous = i > 1 ? keys.get(i - 2).post[axis] : left.post[axis];
                        float next = i + 1 < keys.size() ? keys.get(i + 1).pre[axis] : b;
                        float t2 = amount * amount;
                        float t3 = t2 * amount;
                        result[axis] = 0.5f * (2.0f * a + (-previous + b) * amount
                                + (2.0f * previous - 5.0f * a + 4.0f * b - next) * t2
                                + (-previous + 3.0f * a - 3.0f * b + next) * t3);
                    } else {
                        result[axis] = a + (b - a) * amount;
                    }
                }
                return result;
            }
            return keys.getLast().post;
        }
    }

    record BoneTrack(Channel position, Channel rotation, Channel scale) {
        float[] position(float time) { return position == null ? null : position.sample(time); }
        float[] rotation(float time) { return rotation == null ? null : rotation.sample(time); }
        float[] scale(float time) { return scale == null ? null : scale.sample(time); }
    }

    record Sound(float time, Identifier event) {}

    record Clip(float duration, Map<String, BoneTrack> bones, List<Sound> sounds) {
        BoneTrack bone(String name) { return bones.get(name); }
    }
}
