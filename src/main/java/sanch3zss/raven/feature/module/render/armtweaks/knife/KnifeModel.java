package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class KnifeModel {
    private static final String[] DIRECTIONS = {"up", "down", "east", "north", "west", "south"};
    private static final float[][] NORMALS = {
            {0, -1, 0}, {0, 1, 0}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}, {0, 0, 1}
    };
    private static final int[][] CORNERS = {
            {5, 4, 0, 1}, {2, 3, 7, 6}, {0, 4, 7, 3},
            {1, 0, 3, 2}, {5, 1, 2, 6}, {4, 5, 6, 7}
    };
    private final List<Node> roots = new ArrayList<>();
    private final Map<String, Node> nodes = new HashMap<>();
    private boolean stabilized;
    private final Map<String, String> trackAliases = new HashMap<>();
    private final Map<String, float[]> pivotShifts = new HashMap<>();

    KnifeModel(String folder, String name) throws Exception {
        String path = "/assets/raven/" + folder + "/geo_models/melee/" + name + "_geo.json";
        try (InputStream input = KnifeModel.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException(path);
            JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject geometry = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject description = geometry.getAsJsonObject("description");
            float textureWidth = description.get("texture_width").getAsFloat();
            float textureHeight = description.get("texture_height").getAsFloat();
            JsonArray bones = geometry.getAsJsonArray("bones");
            for (JsonElement element : bones) {
                JsonObject json = element.getAsJsonObject();
                Node node = new Node(json.get("name").getAsString(),
                        json.has("parent") ? json.get("parent").getAsString() : null,
                        vector(json, "pivot", new float[3]),
                        vector(json, "rotation", new float[3]));
                nodes.put(node.name, node);
            }
            for (JsonElement element : bones) {
                JsonObject json = element.getAsJsonObject();
                Node node = nodes.get(json.get("name").getAsString());
                Node parent = node.parentName == null ? null : nodes.get(node.parentName);
                if (parent == null) roots.add(node);
                else parent.children.add(node);
                float[] pivot = node.pivot;
                float[] parentPivot = parent == null ? new float[]{0, 24, 0} : parent.pivot;
                node.tx = (pivot[0] - parentPivot[0]) / 16.0f;
                node.ty = (parentPivot[1] - pivot[1]) / 16.0f;
                node.tz = (pivot[2] - parentPivot[2]) / 16.0f;
                if (json.has("cubes") && !node.name.equals("righthand_pos") && !node.name.equals("lefthand_pos")) {
                    for (JsonElement cube : json.getAsJsonArray("cubes")) {
                        node.cubes.add(readCube(cube.getAsJsonObject(), pivot, textureWidth, textureHeight));
                    }
                }
                if (json.has("poly_mesh")) {
                    node.cubes.add(readMesh(json.getAsJsonObject("poly_mesh"), pivot, name.equals("flip")));
                }
            }
        }
    }

    float[] pivot(String node) {
        Node target = nodes.get(node);
        return target == null ? null : target.pivot;
    }

    void alias(String node, String track, float[] animatedPivot) {
        Node target = nodes.get(node);
        if (target == null) return;
        trackAliases.put(node, track);
        if (animatedPivot != null) {
            pivotShifts.put(node, new float[]{(animatedPivot[0] - target.pivot[0]) / 16.0f,
                    (target.pivot[1] - animatedPivot[1]) / 16.0f, (animatedPivot[2] - target.pivot[2]) / 16.0f});
        }
    }

    void stabilize(KnifeAnimation.Clip idle) {
        if (stabilized) return;
        stabilized = true;
        List<StaticFace> faces = new ArrayList<>();
        for (Draw draw : frame(idle, 0.0f, null, 0.0f, 0.0f).cubes) {
            for (Face face : draw.cube.faces) {
                if (face.normals != null) continue;
                Vector3f[] corners = new Vector3f[4];
                for (int i = 0; i < 4; i++) {
                    float[] vertex = face.vertices[i];
                    corners[i] = draw.pose.transformPosition(vertex[0], vertex[1], vertex[2], new Vector3f());
                }
                Vector3f normal = draw.pose.transformDirection(
                        new Vector3f(face.normal[0], face.normal[1], face.normal[2])).normalize();
                faces.add(new StaticFace(face, draw.owner, corners, normal));
            }
        }
        for (int i = 0; i < faces.size(); i++) {
            StaticFace first = faces.get(i);
            if (first.face.hidden) continue;
            for (int j = i + 1; j < faces.size(); j++) {
                StaticFace second = faces.get(j);
                if (first.owner == second.owner && first.normal.dot(second.normal) > 0.9999f
                        && first.near(second) && first.sameVertices(second)) {
                    second.face.hidden = true;
                }
            }
        }
        List<StaticFace[]> pairs = new ArrayList<>();
        for (int i = 0; i < faces.size(); i++) {
            StaticFace first = faces.get(i);
            if (first.face.hidden) continue;
            for (int j = i + 1; j < faces.size(); j++) {
                StaticFace second = faces.get(j);
                if (second.face.hidden || first.normal.dot(second.normal) < 0.0f
                        || !first.near(second) || !overlaps(first, second)) continue;
                float gap = first.normal.dot(new Vector3f(second.vertices[0]).sub(first.vertices[0]));
                boolean secondInFront = Math.abs(gap) > 0.00005f ? gap > 0.0f : !first.face.flat || second.face.flat;
                pairs.add(secondInFront ? new StaticFace[]{first, second} : new StaticFace[]{second, first});
            }
        }
        boolean changed = true;
        for (int pass = 0; pass < 4 && changed; pass++) {
            changed = false;
            for (StaticFace[] pair : pairs) {
                int rank = Math.min(4, pair[0].face.biasRank + 1);
                if (pair[1].face.biasRank < rank) {
                    pair[1].face.biasRank = rank;
                    changed = true;
                }
            }
        }
    }

    private static boolean overlaps(StaticFace first, StaticFace second) {
        Vector3f normal = first.normal;
        int drop = Math.abs(normal.x()) >= Math.abs(normal.y())
                ? Math.abs(normal.x()) >= Math.abs(normal.z()) ? 0 : 2
                : Math.abs(normal.y()) >= Math.abs(normal.z()) ? 1 : 2;
        return hasArea(first.vertices, drop) && hasArea(second.vertices, drop)
                && projectedOverlap(first.vertices, second.vertices, drop)
                && projectedOverlap(second.vertices, first.vertices, drop);
    }

    private static boolean hasArea(Vector3f[] vertices, int drop) {
        float area = 0.0f;
        for (int i = 0; i < 4; i++) {
            Vector3f a = vertices[i];
            Vector3f b = vertices[(i + 1) & 3];
            area += coordinate(a, (drop + 1) % 3) * coordinate(b, (drop + 2) % 3)
                    - coordinate(b, (drop + 1) % 3) * coordinate(a, (drop + 2) % 3);
        }
        return Math.abs(area) > 0.00000001f;
    }

    private static boolean projectedOverlap(Vector3f[] axes, Vector3f[] other, int drop) {
        int firstAxis = (drop + 1) % 3;
        int secondAxis = (drop + 2) % 3;
        for (int i = 0; i < 4; i++) {
            Vector3f start = axes[i];
            Vector3f end = axes[(i + 1) & 3];
            float x = coordinate(end, firstAxis) - coordinate(start, firstAxis);
            float y = coordinate(end, secondAxis) - coordinate(start, secondAxis);
            if (x * x + y * y < 0.0000000001f) continue;
            float minA = Float.POSITIVE_INFINITY;
            float maxA = Float.NEGATIVE_INFINITY;
            float minB = Float.POSITIVE_INFINITY;
            float maxB = Float.NEGATIVE_INFINITY;
            for (Vector3f vertex : axes) {
                float projection = -y * coordinate(vertex, firstAxis) + x * coordinate(vertex, secondAxis);
                minA = Math.min(minA, projection);
                maxA = Math.max(maxA, projection);
            }
            for (Vector3f vertex : other) {
                float projection = -y * coordinate(vertex, firstAxis) + x * coordinate(vertex, secondAxis);
                minB = Math.min(minB, projection);
                maxB = Math.max(maxB, projection);
            }
            if (maxA - minB <= 0.0000001f || maxB - minA <= 0.0000001f) return false;
        }
        return true;
    }

    private static float coordinate(Vector3f value, int axis) {
        return axis == 0 ? value.x() : axis == 1 ? value.y() : value.z();
    }

    void positionToIdleView(PoseStack stack) {
        Node node = nodes.get("idle_view");
        Matrix4f inverse = new Matrix4f();
        while (node != null) {
            inverse.rotateX((float) Math.toRadians(-node.rotation[0]));
            inverse.rotateY((float) Math.toRadians(-node.rotation[1]));
            inverse.rotateZ((float) Math.toRadians(-node.rotation[2]));
            inverse.translate(-node.tx, node.parentName == null ? 1.5f - node.ty : -node.ty, -node.tz);
            node = node.parentName == null ? null : nodes.get(node.parentName);
        }
        stack.translate(0.0f, 1.5f, 0.0f);
        stack.mulPose(inverse);
        stack.translate(0.0f, -1.5f, 0.0f);
    }

    Frame frame(KnifeAnimation.Clip idle, float idleTime, KnifeAnimation.Clip action,
                float actionTime, float actionWeight) {
        return frame(idle, idleTime, null, 0.0f, 0.0f, action, actionTime, actionWeight);
    }

    Frame frame(KnifeAnimation.Clip idle, float idleTime, KnifeAnimation.Clip previous,
                float previousTime, float previousWeight, KnifeAnimation.Clip action,
                float actionTime, float actionWeight) {
        return frame(idle, idleTime, previous, previousTime, previousWeight, false,
                action, actionTime, actionWeight, false);
    }

    Frame frame(KnifeAnimation.Clip idle, float idleTime, KnifeAnimation.Clip previous,
                float previousTime, float previousWeight, boolean nativePrevious, KnifeAnimation.Clip action,
                float actionTime, float actionWeight, boolean nativeAction) {
        Frame frame = new Frame();
        PoseStack stack = new PoseStack();
        float aliasedWeight = (1.0f - previousWeight) * (1.0f - actionWeight)
                + (nativePrevious ? 0.0f : previousWeight * (1.0f - actionWeight))
                + (nativeAction ? 0.0f : actionWeight);
        for (Node root : roots) traverse(root, stack, frame, idle, idleTime, previous, previousTime,
                previousWeight, nativePrevious, action, actionTime, actionWeight, nativeAction, aliasedWeight, false);
        return frame;
    }

    private void traverse(Node node, PoseStack stack, Frame frame, KnifeAnimation.Clip idle,
                          float idleTime, KnifeAnimation.Clip previous, float previousTime,
                          float previousWeight, boolean nativePrevious, KnifeAnimation.Clip action,
                          float actionTime, float weight, boolean nativeAction, float aliasedWeight,
                          boolean leftKnife) {
        stack.pushPose();
        leftKnife |= node.name.equals("knife_and_left");
        String trackName = trackAliases.getOrDefault(node.name, node.name);
        KnifeAnimation.BoneTrack idleTrack = idle == null ? null : idle.bone(trackName);
        KnifeAnimation.BoneTrack previousTrack = previous == null ? null
                : previous.bone(nativePrevious ? node.name : trackName);
        KnifeAnimation.BoneTrack actionTrack = action == null ? null
                : action.bone(nativeAction ? node.name : trackName);
        float[] idlePosition = idleTrack == null ? null : idleTrack.position(idleTime);
        float[] idleRotation = idleTrack == null ? null : idleTrack.rotation(idleTime);
        float[] idleScale = idleTrack == null ? null : idleTrack.scale(idleTime);
        float[] previousPosition = previousTrack == null ? null : previousTrack.position(previousTime);
        float[] previousRotation = previousTrack == null ? null : previousTrack.rotation(previousTime);
        float[] previousScale = previousTrack == null ? null : previousTrack.scale(previousTime);
        float[] actionPosition = actionTrack == null ? null : actionTrack.position(actionTime);
        float[] actionRotation = actionTrack == null ? null : actionTrack.rotation(actionTime);
        float[] actionScale = actionTrack == null ? null : actionTrack.scale(actionTime);
        float[] position = blend(blend(idlePosition, previousPosition, previousWeight, 0.0f),
                actionPosition, weight, 0.0f);
        float[] scale = blend(blend(idleScale, previousScale, previousWeight, 1.0f),
                actionScale, weight, 1.0f);
        stack.translate(node.tx + position[0] / 16.0f, node.ty - position[1] / 16.0f,
                node.tz + position[2] / 16.0f);
        float[] pivotShift = pivotShifts.get(node.name);
        float[] shift = pivotShift == null ? null : new float[]{pivotShift[0] * aliasedWeight,
                pivotShift[1] * aliasedWeight, pivotShift[2] * aliasedWeight};
        if (shift != null) stack.translate(shift[0], shift[1], shift[2]);
        stack.mulPose(Axis.ZP.rotationDegrees(node.rotation[2]));
        stack.mulPose(Axis.YP.rotationDegrees(node.rotation[1]));
        stack.mulPose(Axis.XP.rotationDegrees(node.rotation[0]));
        Quaternionf animatedRotation = quaternion(idleRotation);
        if (previousRotation != null) animatedRotation.slerp(quaternion(previousRotation), previousWeight);
        if (actionRotation != null) animatedRotation.slerp(quaternion(actionRotation), weight);
        stack.mulPose(animatedRotation);
        stack.scale(scale[0], scale[1], scale[2]);
        if (shift != null) stack.translate(-shift[0], -shift[1], -shift[2]);
        if (node.name.equals("righthand_pos")) frame.rightHand = new Matrix4f(stack.last().pose());
        if (node.name.equals("lefthand_pos")) frame.leftHand = new Matrix4f(stack.last().pose());
        for (Cube cube : node.cubes) {
            stack.pushPose();
            stack.translate(cube.tx, cube.ty, cube.tz);
            stack.mulPose(Axis.ZP.rotationDegrees(cube.rotation[2]));
            stack.mulPose(Axis.YP.rotationDegrees(cube.rotation[1]));
            stack.mulPose(Axis.XP.rotationDegrees(cube.rotation[0]));
            frame.cubes.add(new Draw(cube, new Matrix4f(stack.last().pose()),
                    new Matrix3f(stack.last().normal()), leftKnife, node));
            stack.popPose();
        }
        for (Node child : node.children) traverse(child, stack, frame, idle, idleTime, previous, previousTime,
                previousWeight, nativePrevious, action, actionTime, weight, nativeAction, aliasedWeight, leftKnife);
        stack.popPose();
    }

    private static float[] blend(float[] idle, float[] action, float weight, float defaultValue) {
        float[] result = new float[3];
        for (int i = 0; i < 3; i++) {
            float start = idle == null ? defaultValue : idle[i];
            float end = action == null ? start : action[i];
            result[i] = start + (end - start) * weight;
        }
        return result;
    }

    private static Quaternionf quaternion(float[] angles) {
        if (angles == null) return new Quaternionf();
        return new Quaternionf().rotationZYX((float) Math.toRadians(angles[2]),
                (float) Math.toRadians(angles[1]), (float) Math.toRadians(angles[0]));
    }

    private static Cube readCube(JsonObject json, float[] bonePivot, float textureWidth, float textureHeight) {
        float[] origin = vector(json, "origin", new float[3]);
        float[] size = vector(json, "size", new float[3]);
        float[] rotation = vector(json, "rotation", new float[3]);
        boolean rotated = json.has("rotation");
        float[] pivot = rotated ? vector(json, "pivot", bonePivot) : bonePivot;
        float inflate = json.has("inflate") ? json.get("inflate").getAsFloat() : 0.0f;
        float x0 = (origin[0] - pivot[0] - inflate) / 16.0f;
        float y0 = (pivot[1] - origin[1] - size[1] - inflate) / 16.0f;
        float z0 = (origin[2] - pivot[2] - inflate) / 16.0f;
        float x1 = x0 + (size[0] + inflate * 2.0f) / 16.0f;
        float y1 = y0 + (size[1] + inflate * 2.0f) / 16.0f;
        float z1 = z0 + (size[2] + inflate * 2.0f) / 16.0f;
        float[][] corners = {
                {x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0},
                {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}
        };
        boolean flat = Math.abs(size[0]) < 0.001f || Math.abs(size[1]) < 0.001f || Math.abs(size[2]) < 0.001f;
        List<Face> faces = new ArrayList<>();
        if (json.has("uv") && json.get("uv").isJsonObject()) {
            JsonObject uv = json.getAsJsonObject("uv");
            for (int side = 0; side < DIRECTIONS.length; side++) {
                if (!uv.has(DIRECTIONS[side])) continue;
                if (size[0] == 0.0f && side != 2 && side != 4
                        || size[1] == 0.0f && side != 0 && side != 1
                        || size[2] == 0.0f && side != 3 && side != 5) continue;
                JsonObject face = uv.getAsJsonObject(DIRECTIONS[side]);
                float[] start = vector(face, "uv", new float[3]);
                float[] sizeUv = vector(face, "uv_size", new float[3]);
                float insetU = Math.signum(sizeUv[0]) * Math.min(Math.abs(sizeUv[0]) * 0.25f, 0.02f);
                float insetV = Math.signum(sizeUv[1]) * Math.min(Math.abs(sizeUv[1]) * 0.25f, 0.02f);
                float u0 = (start[0] + insetU) / textureWidth;
                float u1 = (start[0] + sizeUv[0] - insetU) / textureWidth;
                float v0 = (start[1] + insetV) / textureHeight;
                float v1 = (start[1] + sizeUv[1] - insetV) / textureHeight;
                float[][] vertices = new float[4][5];
                for (int i = 0; i < 4; i++) {
                    float[] corner = corners[CORNERS[side][i]];
                    vertices[i] = new float[]{corner[0], corner[1], corner[2],
                            i == 0 || i == 3 ? u1 : u0, i < 2 ? v0 : v1};
                }
                faces.add(new Face(vertices, NORMALS[side], flat));
            }
        }
        return new Cube((pivot[0] - bonePivot[0]) / 16.0f,
                (bonePivot[1] - pivot[1]) / 16.0f,
                (pivot[2] - bonePivot[2]) / 16.0f, rotation, faces);
    }

    private static Cube readMesh(JsonObject mesh, float[] pivot, boolean smoothFacets) {
        JsonArray positions = mesh.getAsJsonArray("positions");
        JsonArray normals = mesh.getAsJsonArray("normals");
        JsonArray uvs = mesh.getAsJsonArray("uvs");
        boolean flipV = !mesh.has("normalized_uvs") || mesh.get("normalized_uvs").getAsBoolean();
        float[][] position = new float[positions.size()][];
        for (int i = 0; i < position.length; i++) {
            JsonArray value = positions.get(i).getAsJsonArray();
            position[i] = new float[]{(value.get(0).getAsFloat() - pivot[0]) / 16.0f,
                    (pivot[1] - value.get(1).getAsFloat()) / 16.0f, (value.get(2).getAsFloat() - pivot[2]) / 16.0f};
        }
        float[][] normal = new float[normals.size()][];
        for (int i = 0; i < normal.length; i++) {
            JsonArray value = normals.get(i).getAsJsonArray();
            normal[i] = new float[]{value.get(0).getAsFloat(), -value.get(1).getAsFloat(), value.get(2).getAsFloat()};
        }
        float[][] uv = new float[uvs.size()][];
        for (int i = 0; i < uv.length; i++) {
            JsonArray value = uvs.get(i).getAsJsonArray();
            float v = value.get(1).getAsFloat();
            uv[i] = new float[]{value.get(0).getAsFloat(), flipV ? 1.0f - v : v};
        }
        List<Face> faces = new ArrayList<>();
        for (JsonElement element : mesh.getAsJsonArray("polys")) {
            JsonArray poly = element.getAsJsonArray();
            int count = Math.min(4, poly.size());
            if (count < 3) continue;
            int[] order = {0, 1, 2, count == 4 ? 3 : 2};
            float[][] vertices = new float[4][];
            float[][] vertexNormals = new float[4][];
            for (int i = 0; i < 4; i++) {
                JsonArray corner = poly.get(order[i]).getAsJsonArray();
                float[] p = position[corner.get(0).getAsInt()];
                float[] t = uv[corner.get(2).getAsInt()];
                vertices[i] = new float[]{p[0], p[1], p[2], t[0], t[1]};
                vertexNormals[i] = normal[corner.get(1).getAsInt()];
            }
            float[] faceNormal = faceNormal(vertices);
            float dot = 0.0f;
            for (float[] n : vertexNormals) dot += faceNormal[0] * n[0] + faceNormal[1] * n[1] + faceNormal[2] * n[2];
            if (dot < 0.0f) {
                reverse(vertices, count);
                reverse(vertexNormals, count);
                faceNormal = faceNormal(vertices);
            }
            for (int i = 0; i < 4; i++) {
                float[] n = vertexNormals[i];
                if (faceNormal[0] * n[0] + faceNormal[1] * n[1] + faceNormal[2] * n[2] < 0.8f) {
                    vertexNormals[i] = faceNormal;
                }
            }
            faces.add(new Face(vertices, faceNormal, false, vertexNormals));
        }
        if (smoothFacets) smoothShallowEdges(faces);
        else respectCreases(faces);
        return new Cube(0.0f, 0.0f, 0.0f, new float[3], faces);
    }

    private static void smoothShallowEdges(List<Face> faces) {
        int[] groups = new int[faces.size()];
        Map<Edge, List<Integer>> edges = new HashMap<>();
        for (int i = 0; i < faces.size(); i++) {
            groups[i] = i;
            Face face = faces.get(i);
            int count = face.vertices[3] == face.vertices[2] ? 3 : 4;
            for (int k = 0; k < count; k++) {
                long from = positionKey(face.vertices[k]);
                long to = positionKey(face.vertices[(k + 1) % count]);
                if (from == to) continue;
                edges.computeIfAbsent(new Edge(Math.min(from, to), Math.max(from, to)), edge -> new ArrayList<>()).add(i);
            }
        }
        for (List<Integer> shared : edges.values()) {
            for (int i = 0; i < shared.size(); i++) {
                for (int j = i + 1; j < shared.size(); j++) {
                    float[] a = faces.get(shared.get(i)).normal;
                    float[] b = faces.get(shared.get(j)).normal;
                    if (a[0] * b[0] + a[1] * b[1] + a[2] * b[2] >= 0.99f) {
                        groups[group(groups, shared.get(i))] = group(groups, shared.get(j));
                    }
                }
            }
        }
        Map<Integer, Map<Long, float[]>> sums = new HashMap<>();
        for (int i = 0; i < faces.size(); i++) {
            Face face = faces.get(i);
            int count = face.vertices[3] == face.vertices[2] ? 3 : 4;
            Map<Long, float[]> groupSums = sums.computeIfAbsent(group(groups, i), key -> new HashMap<>());
            for (int k = 0; k < count; k++) {
                float angle = cornerAngle(face.vertices[(k + count - 1) % count], face.vertices[k],
                        face.vertices[(k + 1) % count]);
                float[] sum = groupSums.computeIfAbsent(positionKey(face.vertices[k]), key -> new float[3]);
                sum[0] += face.normal[0] * angle;
                sum[1] += face.normal[1] * angle;
                sum[2] += face.normal[2] * angle;
            }
        }
        for (int i = 0; i < faces.size(); i++) {
            Face face = faces.get(i);
            int count = face.vertices[3] == face.vertices[2] ? 3 : 4;
            Map<Long, float[]> groupSums = sums.get(group(groups, i));
            for (int k = 0; k < count; k++) {
                float[] sum = groupSums.get(positionKey(face.vertices[k]));
                float length = (float) Math.sqrt(sum[0] * sum[0] + sum[1] * sum[1] + sum[2] * sum[2]);
                face.normals[k] = length < 1.0e-6f ? face.normal
                        : new float[]{sum[0] / length, sum[1] / length, sum[2] / length};
            }
            if (count == 3) face.normals[3] = face.normals[2];
        }
    }

    private static int group(int[] groups, int face) {
        while (groups[face] != face) {
            groups[face] = groups[groups[face]];
            face = groups[face];
        }
        return face;
    }

    private static float cornerAngle(float[] previous, float[] corner, float[] next) {
        float ax = previous[0] - corner[0];
        float ay = previous[1] - corner[1];
        float az = previous[2] - corner[2];
        float bx = next[0] - corner[0];
        float by = next[1] - corner[1];
        float bz = next[2] - corner[2];
        float lengths = (float) Math.sqrt((ax * ax + ay * ay + az * az) * (bx * bx + by * by + bz * bz));
        if (lengths < 1.0e-12f) return 0.0f;
        return (float) Math.acos(Math.max(-1.0f, Math.min(1.0f, (ax * bx + ay * by + az * bz) / lengths)));
    }

    private static void respectCreases(List<Face> faces) {
        Map<Long, List<Face>> touching = new HashMap<>();
        for (Face face : faces) {
            for (int i = 0; i < 4; i++) {
                if (i == 3 && face.vertices[3] == face.vertices[2]) continue;
                touching.computeIfAbsent(positionKey(face.vertices[i]), key -> new ArrayList<>()).add(face);
            }
        }
        for (Face face : faces) {
            for (int i = 0; i < 4; i++) {
                float[] authored = face.normals[i];
                if (authored[0] * face.normal[0] + authored[1] * face.normal[1] + authored[2] * face.normal[2] >= 0.999f) continue;
                boolean onFlatArea = true;
                for (Face other : touching.get(positionKey(face.vertices[i]))) {
                    float[] n = other.normal;
                    float alignment = face.normal[0] * n[0] + face.normal[1] * n[1] + face.normal[2] * n[2];
                    if (alignment >= 0.3f && alignment < 0.995f) {
                        onFlatArea = false;
                        break;
                    }
                }
                if (onFlatArea) face.normals[i] = face.normal;
            }
        }
    }

    private static long positionKey(float[] vertex) {
        long x = Math.round(vertex[0] * 8192.0f) + (1L << 20);
        long y = Math.round(vertex[1] * 8192.0f) + (1L << 20);
        long z = Math.round(vertex[2] * 8192.0f) + (1L << 20);
        return x << 42 | y << 21 | z;
    }

    private static void reverse(float[][] values, int count) {
        if (count == 3) {
            float[] middle = values[1];
            values[1] = values[2];
            values[2] = middle;
            values[3] = values[2];
            return;
        }
        float[] first = values[0];
        values[0] = values[3];
        values[3] = first;
        float[] second = values[1];
        values[1] = values[2];
        values[2] = second;
    }

    private static float[] faceNormal(float[][] vertices) {
        float ax = vertices[1][0] - vertices[0][0];
        float ay = vertices[1][1] - vertices[0][1];
        float az = vertices[1][2] - vertices[0][2];
        float bx = vertices[2][0] - vertices[0][0];
        float by = vertices[2][1] - vertices[0][1];
        float bz = vertices[2][2] - vertices[0][2];
        if (ax * ax + ay * ay + az * az < 1.0e-14f || bx * bx + by * by + bz * bz < 1.0e-14f) {
            bx = vertices[3][0] - vertices[0][0];
            by = vertices[3][1] - vertices[0][1];
            bz = vertices[3][2] - vertices[0][2];
        }
        float x = ay * bz - az * by;
        float y = az * bx - ax * bz;
        float z = ax * by - ay * bx;
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        return length < 1.0e-12f ? new float[]{0.0f, 1.0f, 0.0f} : new float[]{x / length, y / length, z / length};
    }

    private static float[] vector(JsonObject object, String name, float[] fallback) {
        if (!object.has(name)) return fallback;
        JsonElement element = object.get(name);
        if (element.isJsonPrimitive()) {
            float value = element.getAsFloat();
            return new float[]{value, value, value};
        }
        JsonArray array = element.getAsJsonArray();
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.size() > 2 ? array.get(2).getAsFloat() : 0.0f};
    }

    static final class Frame {
        final List<Draw> cubes = new ArrayList<>();
        Matrix4f rightHand;
        Matrix4f leftHand;

        void emit(PoseStack.Pose base, VertexConsumer buffer, int light, int overlay,
                  boolean showLeftKnife, Matrix4f leftMotion, float uScale) {
            Matrix4f combined = new Matrix4f();
            Matrix3f normalMatrix = new Matrix3f();
            Matrix3f leftNormal = leftMotion == null ? null : new Matrix3f(leftMotion);
            Vector3f normal = new Vector3f();
            for (Draw draw : cubes) {
                if (draw.leftKnife && !showLeftKnife) continue;
                combined.set(base.pose()).mul(draw.pose);
                normalMatrix.set(base.normal()).mul(draw.normal);
                if (draw.leftKnife && leftMotion != null) {
                    combined.set(leftMotion).mul(base.pose()).mul(draw.pose);
                    normalMatrix.set(leftNormal).mul(base.normal()).mul(draw.normal);
                }
                float determinant = combined.determinant3x3();
                boolean mirrored = determinant < 0.0f;
                for (Face face : draw.cube.faces) {
                    if (face.hidden) continue;
                    float offset = face.normals == null ? 0.0003f + face.biasRank * 0.0005f : 0.0f;
                    normalMatrix.transform(face.normal[0], face.normal[1], face.normal[2], normal).normalize();
                    for (int i = 0; i < 4; i++) {
                        int index = mirrored ? 3 - i : i;
                        float[] vertex = face.vertices[index];
                        if (face.normals != null) {
                            float[] vertexNormal = face.normals[index];
                            normalMatrix.transform(vertexNormal[0], vertexNormal[1], vertexNormal[2], normal).normalize();
                        }
                        buffer.addVertex(combined,
                                        vertex[0] + face.normal[0] * offset,
                                        vertex[1] + face.normal[1] * offset,
                                        vertex[2] + face.normal[2] * offset)
                                .setColor(-1).setUv(vertex[3] * uScale, vertex[4])
                                .setOverlay(overlay == 0 ? OverlayTexture.NO_OVERLAY : overlay).setLight(light)
                                .setNormal(normal.x(), normal.y(), normal.z());
                    }
                }
            }
        }
    }

    private static final class Node {
        final String name;
        final String parentName;
        final float[] pivot;
        final float[] rotation;
        final List<Node> children = new ArrayList<>();
        final List<Cube> cubes = new ArrayList<>();
        float tx;
        float ty;
        float tz;

        Node(String name, String parentName, float[] pivot, float[] rotation) {
            this.name = name;
            this.parentName = parentName;
            this.pivot = pivot;
            this.rotation = rotation;
        }
    }

    private record Cube(float tx, float ty, float tz, float[] rotation, List<Face> faces) {}
    private static final class Face {
        final float[][] vertices;
        final float[] normal;
        final boolean flat;
        final float[][] normals;
        int biasRank;
        boolean hidden;

        Face(float[][] vertices, float[] normal, boolean flat) {
            this(vertices, normal, flat, null);
        }

        Face(float[][] vertices, float[] normal, boolean flat, float[][] normals) {
            this.vertices = vertices;
            this.normal = normal;
            this.flat = flat;
            this.normals = normals;
        }
    }

    private static final class StaticFace {
        final Face face;
        final Node owner;
        final Vector3f[] vertices;
        final Vector3f normal;
        final float minX;
        final float minY;
        final float minZ;
        final float maxX;
        final float maxY;
        final float maxZ;

        StaticFace(Face face, Node owner, Vector3f[] vertices, Vector3f normal) {
            this.face = face;
            this.owner = owner;
            this.vertices = vertices;
            this.normal = normal;
            float minX = Float.POSITIVE_INFINITY;
            float minY = Float.POSITIVE_INFINITY;
            float minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY;
            float maxY = Float.NEGATIVE_INFINITY;
            float maxZ = Float.NEGATIVE_INFINITY;
            for (Vector3f vertex : vertices) {
                minX = Math.min(minX, vertex.x());
                minY = Math.min(minY, vertex.y());
                minZ = Math.min(minZ, vertex.z());
                maxX = Math.max(maxX, vertex.x());
                maxY = Math.max(maxY, vertex.y());
                maxZ = Math.max(maxZ, vertex.z());
            }
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        boolean near(StaticFace other) {
            if (maxX + 0.0008f < other.minX || other.maxX + 0.0008f < minX
                    || maxY + 0.0008f < other.minY || other.maxY + 0.0008f < minY
                    || maxZ + 0.0008f < other.minZ || other.maxZ + 0.0008f < minZ) return false;
            if (Math.abs(normal.dot(other.normal)) < 0.999f) return false;
            Vector3f offset = new Vector3f();
            for (Vector3f vertex : other.vertices) {
                if (Math.abs(normal.dot(offset.set(vertex).sub(vertices[0]))) > 0.0008f) return false;
            }
            return true;
        }

        boolean sameVertices(StaticFace other) {
            for (Vector3f vertex : vertices) {
                boolean found = false;
                for (Vector3f candidate : other.vertices) {
                    if (vertex.distanceSquared(candidate) < 0.000000000001f) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
    }
    record Draw(Cube cube, Matrix4f pose, Matrix3f normal, boolean leftKnife, Node owner) {}
    private record Edge(long from, long to) {}

}
