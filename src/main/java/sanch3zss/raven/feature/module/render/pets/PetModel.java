package sanch3zss.raven.feature.module.render.pets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public final class PetModel {
    private final ModelPart root;
    private final Map<String, ModelPart> parts = new HashMap<>();
    private final Map<String, List<String>> paths = new HashMap<>();
    private final Map<String, Animation> animations = new HashMap<>();
    private final Map<String, JsonObject> elements = new HashMap<>();
    private final Map<String, JsonObject> groups = new HashMap<>();

    public PetModel(String name) {
        String path = "/assets/raven/pets/" + name + ".bbmodel";
        JsonObject json;
        try (InputStream input = PetModel.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("Missing pet model " + path);
            }
            json = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        for (JsonElement element : json.getAsJsonArray("elements")) {
            JsonObject cube = element.getAsJsonObject();
            elements.put(cube.get("uuid").getAsString(), cube);
        }
        if (json.has("groups")) {
            for (JsonElement element : json.getAsJsonArray("groups")) {
                JsonObject group = element.getAsJsonObject();
                groups.put(group.get("uuid").getAsString(), group);
            }
        }
        MeshDefinition mesh = new MeshDefinition();
        for (JsonElement node : json.getAsJsonArray("outliner")) {
            if (node.isJsonObject()) {
                addGroup(mesh.getRoot(), node.getAsJsonObject(), new float[]{0.0f, 24.0f, 0.0f}, List.of());
            }
        }
        JsonObject resolution = json.getAsJsonObject("resolution");
        root = LayerDefinition.create(mesh, resolution.get("width").getAsInt(),
                resolution.get("height").getAsInt()).bakeRoot();
        for (Map.Entry<String, List<String>> entry : paths.entrySet()) {
            ModelPart part = root;
            for (String child : entry.getValue()) {
                part = part.getChild(child);
            }
            parts.put(entry.getKey(), part);
        }
        if (json.has("animations")) {
            for (JsonElement element : json.getAsJsonArray("animations")) {
                readAnimation(element.getAsJsonObject());
            }
        }
        elements.clear();
        groups.clear();
    }

    private void addGroup(PartDefinition parent, JsonObject node, float[] parentOrigin, List<String> parentPath) {
        JsonObject group = groups.getOrDefault(node.get("uuid").getAsString(), node);
        String name = group.get("name").getAsString();
        float[] origin = vector(group, "origin", 0.0f);
        float[] rotation = vector(group, "rotation", 0.0f);
        JsonArray children = node.has("children") ? node.getAsJsonArray("children") : new JsonArray();
        CubeListBuilder cubes = CubeListBuilder.create();
        List<JsonObject> rotated = new ArrayList<>();
        for (JsonElement child : children) {
            if (!child.isJsonPrimitive()) continue;
            JsonObject cube = elements.get(child.getAsString());
            if (cube == null) continue;
            float[] cubeRotation = vector(cube, "rotation", 0.0f);
            if (cubeRotation[0] != 0.0f || cubeRotation[1] != 0.0f || cubeRotation[2] != 0.0f) {
                rotated.add(cube);
            } else {
                addCube(cubes, cube, origin);
            }
        }
        PartDefinition part = parent.addOrReplaceChild(name, cubes, PartPose.offsetAndRotation(
                parentOrigin[0] - origin[0], parentOrigin[1] - origin[1], origin[2] - parentOrigin[2],
                -rotation[0] * Mth.DEG_TO_RAD, -rotation[1] * Mth.DEG_TO_RAD, rotation[2] * Mth.DEG_TO_RAD));
        List<String> path = new ArrayList<>(parentPath);
        path.add(name);
        paths.put(name, path);
        for (int i = 0; i < rotated.size(); i++) {
            JsonObject cube = rotated.get(i);
            float[] pivot = vector(cube, "origin", 0.0f);
            float[] angles = vector(cube, "rotation", 0.0f);
            CubeListBuilder single = CubeListBuilder.create();
            addCube(single, cube, pivot);
            part.addOrReplaceChild(name + "_cube" + i, single, PartPose.offsetAndRotation(
                    origin[0] - pivot[0], origin[1] - pivot[1], pivot[2] - origin[2],
                    -angles[0] * Mth.DEG_TO_RAD, -angles[1] * Mth.DEG_TO_RAD, angles[2] * Mth.DEG_TO_RAD));
        }
        for (JsonElement child : children) {
            if (child.isJsonObject()) {
                addGroup(part, child.getAsJsonObject(), origin, path);
            }
        }
    }

    private static void addCube(CubeListBuilder builder, JsonObject cube, float[] origin) {
        float[] from = vector(cube, "from", 0.0f);
        float[] to = vector(cube, "to", 0.0f);
        float width = to[0] - from[0];
        float height = to[1] - from[1];
        float depth = to[2] - from[2];
        JsonObject faces = cube.getAsJsonObject("faces");
        float u;
        float v;
        if (width > 0.0f && height > 0.0f && hasTexture(faces, "north")) {
            float[] uv = faceUv(faces, "north");
            u = uv[0] - depth;
            v = uv[1] - depth;
        } else if (width > 0.0f && depth > 0.0f && hasTexture(faces, "up")) {
            float[] uv = faceUv(faces, "up");
            u = uv[0] - depth;
            v = uv[1];
        } else {
            float[] east = faceUv(faces, "east");
            float[] west = faceUv(faces, "west");
            u = Math.min(east[0], west[0]);
            v = Math.min(east[1], west[1]) - depth;
        }
        CubeDeformation deformation = cube.has("inflate")
                ? new CubeDeformation(cube.get("inflate").getAsFloat()) : CubeDeformation.NONE;
        boolean mirror = cube.has("mirror_uv") && cube.get("mirror_uv").getAsBoolean();
        builder.texOffs(Math.round(u), Math.round(v)).mirror(mirror)
                .addBox(origin[0] - to[0], origin[1] - to[1], from[2] - origin[2], width, height, depth, deformation)
                .mirror(false);
    }

    private static boolean hasTexture(JsonObject faces, String face) {
        return faces.has(face) && faces.getAsJsonObject(face).has("texture")
                && !faces.getAsJsonObject(face).get("texture").isJsonNull();
    }

    private static float[] faceUv(JsonObject faces, String face) {
        JsonArray uv = faces.getAsJsonObject(face).getAsJsonArray("uv");
        return new float[]{
                Math.min(uv.get(0).getAsFloat(), uv.get(2).getAsFloat()),
                Math.min(uv.get(1).getAsFloat(), uv.get(3).getAsFloat())
        };
    }

    private void readAnimation(JsonObject json) {
        if (!json.has("animators")) return;
        Map<String, Track> tracks = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("animators").entrySet()) {
            JsonObject animator = entry.getValue().getAsJsonObject();
            if (!animator.has("keyframes") || !animator.has("name")) continue;
            List<Keyframe> rotation = new ArrayList<>();
            List<Keyframe> position = new ArrayList<>();
            List<Keyframe> scale = new ArrayList<>();
            for (JsonElement element : animator.getAsJsonArray("keyframes")) {
                JsonObject keyframe = element.getAsJsonObject();
                String channel = keyframe.get("channel").getAsString();
                float fallback = channel.equals("scale") ? 1.0f : 0.0f;
                JsonArray points = keyframe.getAsJsonArray("data_points");
                float[] pre = point(points.get(0).getAsJsonObject(), fallback);
                float[] post = points.size() > 1 ? point(points.get(1).getAsJsonObject(), fallback) : pre;
                String interpolation = keyframe.has("interpolation") ? keyframe.get("interpolation").getAsString() : "linear";
                Keyframe value = new Keyframe(keyframe.get("time").getAsFloat(), pre, post, interpolation);
                switch (channel) {
                    case "rotation" -> rotation.add(value);
                    case "position" -> position.add(value);
                    case "scale" -> scale.add(value);
                    default -> {
                    }
                }
            }
            rotation.sort(Comparator.comparingDouble(Keyframe::time));
            position.sort(Comparator.comparingDouble(Keyframe::time));
            scale.sort(Comparator.comparingDouble(Keyframe::time));
            tracks.put(animator.get("name").getAsString(), new Track(rotation, position, scale));
        }
        boolean loop = json.has("loop") && json.get("loop").getAsString().equals("loop");
        animations.put(json.get("name").getAsString(), new Animation(json.get("length").getAsFloat(), loop, tracks));
    }

    private static float[] point(JsonObject point, float fallback) {
        return new float[]{number(point, "x", fallback), number(point, "y", fallback), number(point, "z", fallback)};
    }

    private static float number(JsonObject object, String key, float fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) return fallback;
        String text = object.get(key).getAsString().trim();
        if (text.isEmpty()) return fallback;
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static float[] vector(JsonObject object, String key, float fallback) {
        if (!object.has(key)) return new float[]{fallback, fallback, fallback};
        JsonArray array = object.getAsJsonArray(key);
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    public boolean hasAnimation(String name) {
        return animations.containsKey(name);
    }

    public float length(String name) {
        Animation animation = animations.get(name);
        return animation == null ? 0.0f : animation.length;
    }

    public ModelPart part(String name) {
        return parts.get(name);
    }

    public void reset() {
        for (ModelPart part : parts.values()) {
            part.resetPose();
            part.visible = true;
        }
    }

    public void apply(String name, float time, float weight) {
        Animation animation = animations.get(name);
        if (animation == null || weight <= 0.0f) return;
        if (animation.length > 0.0f) {
            time = animation.loop ? time % animation.length : Math.min(time, animation.length);
        } else {
            time = 0.0f;
        }
        for (Map.Entry<String, Track> entry : animation.tracks.entrySet()) {
            ModelPart part = parts.get(entry.getKey());
            if (part == null) continue;
            Track track = entry.getValue();
            float[] rotation = sample(track.rotation, time);
            if (rotation != null) {
                part.xRot -= rotation[0] * Mth.DEG_TO_RAD * weight;
                part.yRot -= rotation[1] * Mth.DEG_TO_RAD * weight;
                part.zRot += rotation[2] * Mth.DEG_TO_RAD * weight;
            }
            float[] position = sample(track.position, time);
            if (position != null) {
                part.x -= position[0] * weight;
                part.y -= position[1] * weight;
                part.z += position[2] * weight;
            }
            float[] scale = sample(track.scale, time);
            if (scale != null) {
                part.xScale += (scale[0] - 1.0f) * weight;
                part.yScale += (scale[1] - 1.0f) * weight;
                part.zScale += (scale[2] - 1.0f) * weight;
            }
        }
    }

    private static float[] sample(List<Keyframe> keys, float time) {
        if (keys.isEmpty()) return null;
        if (time <= keys.getFirst().time) return keys.getFirst().pre;
        for (int i = 1; i < keys.size(); i++) {
            Keyframe right = keys.get(i);
            if (time > right.time) continue;
            Keyframe left = keys.get(i - 1);
            float range = right.time - left.time;
            if (range <= 0.0f || left.interpolation.equals("step")) return left.post;
            float amount = (time - left.time) / range;
            boolean catmull = left.interpolation.equals("catmullrom") || right.interpolation.equals("catmullrom");
            float[] result = new float[3];
            for (int axis = 0; axis < 3; axis++) {
                float a = left.post[axis];
                float b = right.pre[axis];
                if (catmull) {
                    float before = i > 1 ? keys.get(i - 2).post[axis] : a;
                    float after = i + 1 < keys.size() ? keys.get(i + 1).pre[axis] : b;
                    float squared = amount * amount;
                    float cubed = squared * amount;
                    result[axis] = 0.5f * (2.0f * a + (-before + b) * amount
                            + (2.0f * before - 5.0f * a + 4.0f * b - after) * squared
                            + (-before + 3.0f * a - 3.0f * b + after) * cubed);
                } else {
                    result[axis] = a + (b - a) * amount;
                }
            }
            return result;
        }
        return keys.getLast().post;
    }

    public void render(PoseStack matrices, VertexConsumer vertices, int light) {
        root.render(matrices, vertices, light, OverlayTexture.NO_OVERLAY);
    }

    private record Keyframe(float time, float[] pre, float[] post, String interpolation) {
    }

    private record Track(List<Keyframe> rotation, List<Keyframe> position, List<Keyframe> scale) {
    }

    private record Animation(float length, boolean loop, Map<String, Track> tracks) {
    }
}
