package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class Cs2Mesh {
    final String[] bones;
    final String[] parents;
    final Matrix4f[] restLocal;
    final Matrix4f[] inverseBind;
    final List<Part> parts = new ArrayList<>();
    private final float[] positions;
    private final float[] normals;
    private final float[] uvs;
    private final byte[] joints;
    private final byte[] weights;
    private final float[] skinnedPositions;
    private final float[] skinnedNormals;
    private final float[] emitPositions;
    private final float[] emitNormals;
    private final float[] skinData;
    private final Matrix4f[] skin;
    private final Matrix4f[] world;
    private final float[] twistAngles = new float[2];
    Cs2Mesh twistSource;
    Set<String> caps = Set.of();

    static Cs2Mesh load(String path) throws Exception {
        ByteBuffer data;
        try (InputStream input = Cs2Mesh.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException(path);
            data = ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        }
        int header = data.getInt();
        boolean packed = header < 0;
        int boneCount = Math.abs(header);
        String[] bones = new String[boneCount];
        String[] parents = new String[boneCount];
        Matrix4f[] restLocal = new Matrix4f[boneCount];
        Matrix4f[] inverseBind = new Matrix4f[boneCount];
        for (int i = 0; i < boneCount; i++) {
            bones[i] = text(data);
            parents[i] = text(data);
            restLocal[i] = matrix(data);
            inverseBind[i] = matrix(data);
        }
        int vertexCount = data.getInt();
        float[] positions = new float[vertexCount * 3];
        float[] normals = new float[vertexCount * 3];
        float[] uvs = new float[vertexCount * 2];
        if (packed) {
            float extent = data.getFloat() / 32767.0f;
            for (int i = 0; i < positions.length; i++) positions[i] = data.getShort() * extent;
            for (int i = 0; i < normals.length; i++) normals[i] = data.get() / 127.0f;
            float u = data.getFloat(), v = data.getFloat(), uSpan = data.getFloat() / 65535.0f, vSpan = data.getFloat() / 65535.0f;
            for (int i = 0; i < vertexCount; i++) {
                uvs[i * 2] = u + (data.getShort() & 0xFFFF) * uSpan;
                uvs[i * 2 + 1] = v + (data.getShort() & 0xFFFF) * vSpan;
            }
        } else {
            positions = floats(data, positions.length);
            normals = floats(data, normals.length);
            uvs = floats(data, uvs.length);
        }
        byte[] joints = new byte[vertexCount * 4];
        byte[] weights = new byte[vertexCount * 4];
        data.get(joints);
        data.get(weights);
        List<Part> parts = new ArrayList<>();
        int partCount = data.getInt();
        for (int i = 0; i < partCount; i++) {
            String texture = text(data);
            int[] indices = new int[data.getInt()];
            for (int j = 0; j < indices.length; j++) indices[j] = data.getShort() & 0xFFFF;
            parts.add(new Part(texture, indices, IntStream.of(indices).distinct().toArray()));
        }
        return new Cs2Mesh(bones, parents, restLocal, inverseBind, positions, normals, uvs, joints, weights, parts);
    }

    private Cs2Mesh(String[] bones, String[] parents, Matrix4f[] restLocal, Matrix4f[] inverseBind, float[] positions,
                    float[] normals, float[] uvs, byte[] joints, byte[] weights, List<Part> parts) {
        int boneCount = bones.length;
        this.bones = bones;
        this.parents = parents;
        this.restLocal = restLocal;
        this.inverseBind = inverseBind;
        this.positions = positions;
        this.normals = normals;
        this.uvs = uvs;
        this.joints = joints;
        this.weights = weights;
        this.parts.addAll(parts);
        skinnedPositions = new float[positions.length];
        skinnedNormals = new float[normals.length];
        emitPositions = new float[positions.length];
        emitNormals = new float[normals.length];
        skinData = new float[boneCount * 12];
        skin = new Matrix4f[boneCount];
        world = new Matrix4f[boneCount];
        for (int i = 0; i < boneCount; i++) {
            skin[i] = new Matrix4f();
            world[i] = new Matrix4f();
        }
    }

    boolean hasBone(String name) {
        for (String bone : bones) if (bone.equals(name)) return true;
        return false;
    }

    boolean bounds(String boneName, Matrix4f frame, Vector3f min, Vector3f max) {
        int bone = index(boneName);
        if (bone < 0) return false;
        min.set(Float.MAX_VALUE);
        max.set(-Float.MAX_VALUE);
        boolean found = false;
        Vector3f local = new Vector3f();
        int vertexCount = positions.length / 3;
        for (int v = 0; v < vertexCount; v++) {
            for (int k = 0; k < 4; k++) {
                if ((joints[v * 4 + k] & 0xFF) != bone || (weights[v * 4 + k] & 0xFF) < 128) continue;
                int p = v * 3;
                frame.transformPosition(skinnedPositions[p], skinnedPositions[p + 1], skinnedPositions[p + 2], local);
                min.min(local);
                max.max(local);
                found = true;
                break;
            }
        }
        return found;
    }

    void pose(Map<String, Matrix4f> pose, Matrix4f attach, boolean hideLeft) {
        for (int i = 0; i < bones.length; i++) {
            Matrix4f animated = pose.get(bones[i]);
            if (animated != null) {
                world[i].set(animated);
            } else {
                Matrix4f parent = pose.get(parents[i]);
                if (parent == null) parent = parentWorld(i);
                world[i].set(parent).mul(restLocal[i]);
            }
        }
        for (int i = 0; i < bones.length; i++) {
            if (bones[i].startsWith("arm_lower_") && bones[i].contains("_TWIST")) twist(i, pose);
        }
        for (int i = 0; i < bones.length; i++) {
            if (hideLeft && leftSide(bones[i])) {
                skin[i].zero();
            } else {
                skin[i].set(attach == null ? world[i] : new Matrix4f(attach).mul(world[i])).mul(inverseBind[i]);
            }
            Matrix4f m = skin[i];
            int o = i * 12;
            skinData[o] = m.m00();
            skinData[o + 1] = m.m01();
            skinData[o + 2] = m.m02();
            skinData[o + 3] = m.m10();
            skinData[o + 4] = m.m11();
            skinData[o + 5] = m.m12();
            skinData[o + 6] = m.m20();
            skinData[o + 7] = m.m21();
            skinData[o + 8] = m.m22();
            skinData[o + 9] = m.m30();
            skinData[o + 10] = m.m31();
            skinData[o + 11] = m.m32();
        }
        float[] s = skinData;
        int vertexCount = positions.length / 3;
        for (int v = 0; v < vertexCount; v++) {
            float a0 = 0, a1 = 0, a2 = 0, a3 = 0, a4 = 0, a5 = 0, a6 = 0, a7 = 0, a8 = 0, a9 = 0, a10 = 0, a11 = 0;
            for (int k = 0; k < 4; k++) {
                int weight = weights[v * 4 + k] & 0xFF;
                if (weight == 0) continue;
                float w = weight * (1.0f / 255.0f);
                int o = (joints[v * 4 + k] & 0xFF) * 12;
                a0 += s[o] * w;
                a1 += s[o + 1] * w;
                a2 += s[o + 2] * w;
                a3 += s[o + 3] * w;
                a4 += s[o + 4] * w;
                a5 += s[o + 5] * w;
                a6 += s[o + 6] * w;
                a7 += s[o + 7] * w;
                a8 += s[o + 8] * w;
                a9 += s[o + 9] * w;
                a10 += s[o + 10] * w;
                a11 += s[o + 11] * w;
            }
            int p = v * 3;
            float x = positions[p], y = positions[p + 1], z = positions[p + 2];
            skinnedPositions[p] = a0 * x + a3 * y + a6 * z + a9;
            skinnedPositions[p + 1] = a1 * x + a4 * y + a7 * z + a10;
            skinnedPositions[p + 2] = a2 * x + a5 * y + a8 * z + a11;
            x = normals[p];
            y = normals[p + 1];
            z = normals[p + 2];
            skinnedNormals[p] = a0 * x + a3 * y + a6 * z;
            skinnedNormals[p + 1] = a1 * x + a4 * y + a7 * z;
            skinnedNormals[p + 2] = a2 * x + a5 * y + a8 * z;
        }
    }

    private void twist(int bone, Map<String, Matrix4f> pose) {
        String handName = "hand_" + bones[bone].charAt("arm_lower_".length());
        Cs2Mesh reference = index(handName) >= 0 ? this : twistSource;
        int hand = reference == null ? -1 : reference.index(handName);
        Matrix4f lower = pose.get(parents[bone]);
        Matrix4f handWorld = pose.get(handName);
        if (hand < 0 || lower == null || handWorld == null) return;
        int side = handName.endsWith("L") ? 0 : 1;
        if (reference == this) {
            Quaternionf current = new Matrix4f(lower).invert().mul(handWorld)
                    .getUnnormalizedRotation(new Quaternionf()).normalize();
            Quaternionf rest = restLocal[hand].getUnnormalizedRotation(new Quaternionf()).normalize();
            Quaternionf delta = current.mul(rest.invert(), new Quaternionf());
            float axial = (float) Math.sqrt(delta.x * delta.x + delta.w * delta.w);
            if (axial < 1.0e-4f) return;
            if (bones[bone].endsWith("_TWIST")) {
                float target = 2.0f * (float) Math.atan2(delta.w < 0.0f ? -delta.x : delta.x, Math.abs(delta.w));
                float confidence = Math.clamp((axial - 0.35f) / 0.35f, 0.0f, 1.0f);
                float step = (target - twistAngles[side]) * (0.15f + 0.6f * confidence);
                twistAngles[side] += Math.clamp(step, -0.35f, 0.35f);
            }
        }
        float angle = reference.twistAngles[side];
        Vector3f offset = restLocal[bone].getTranslation(new Vector3f());
        float fraction = offset.length() / Math.max(1.0e-6f, reference.restLocal[hand].getTranslation(new Vector3f()).length());
        Quaternionf partial = new Quaternionf().rotationX(angle * Math.min(1.0f, fraction));
        Quaternionf restRotation = restLocal[bone].getUnnormalizedRotation(new Quaternionf()).normalize();
        world[bone].set(lower).mul(new Matrix4f().translation(offset).rotate(partial.mul(restRotation)));
    }

    Cs2Mesh subset(Predicate<String> boneFilter) {
        boolean[] keptBones = new boolean[bones.length];
        for (int i = 0; i < bones.length; i++) keptBones[i] = boneFilter.test(bones[i]);
        int vertexCount = positions.length / 3;
        boolean[] keptVertices = new boolean[vertexCount];
        int[] capOf = new int[vertexCount];
        for (int v = 0; v < vertexCount; v++) {
            int share = 0;
            int dropped = -1;
            int droppedWeight = 0;
            for (int k = 0; k < 4; k++) {
                int bone = joints[v * 4 + k] & 0xFF;
                int weight = weights[v * 4 + k] & 0xFF;
                if (keptBones[bone]) {
                    share += weight;
                } else if (weight > droppedWeight) {
                    dropped = bone;
                    droppedWeight = weight;
                }
            }
            keptVertices[v] = share >= 128;
            while (dropped >= 0) {
                int parent = index(parents[dropped]);
                if (parent < 0 || keptBones[parent]) break;
                dropped = parent;
            }
            capOf[v] = dropped;
        }
        int[] remap = new int[vertexCount];
        Arrays.fill(remap, -1);
        int count = 0;
        List<Part> keptParts = new ArrayList<>();
        List<Integer> order = new ArrayList<>();
        for (Part part : parts) {
            int[] indices = part.indices;
            int[] kept = new int[indices.length];
            int length = 0;
            for (int t = 0; t + 2 < indices.length; t += 3) {
                int keptCorners = (keptVertices[indices[t]] ? 1 : 0) + (keptVertices[indices[t + 1]] ? 1 : 0)
                        + (keptVertices[indices[t + 2]] ? 1 : 0);
                if (keptCorners == 0) continue;
                for (int c = 0; c < 3; c++) {
                    int vertex = indices[t + c];
                    if (remap[vertex] < 0) {
                        remap[vertex] = count++;
                        order.add(vertex);
                    }
                    kept[length++] = remap[vertex];
                }
            }
            if (length == 0) continue;
            int[] trimmed = Arrays.copyOf(kept, length);
            keptParts.add(new Part(part.texture, trimmed, IntStream.of(trimmed).distinct().toArray()));
        }
        float[] newPositions = new float[count * 3];
        float[] newNormals = new float[count * 3];
        float[] newUvs = new float[count * 2];
        byte[] newJoints = new byte[count * 4];
        byte[] newWeights = new byte[count * 4];
        Set<String> caps = new LinkedHashSet<>();
        for (int i = 0; i < count; i++) {
            int source = order.get(i);
            System.arraycopy(positions, source * 3, newPositions, i * 3, 3);
            System.arraycopy(normals, source * 3, newNormals, i * 3, 3);
            System.arraycopy(uvs, source * 2, newUvs, i * 2, 2);
            System.arraycopy(joints, source * 4, newJoints, i * 4, 4);
            System.arraycopy(weights, source * 4, newWeights, i * 4, 4);
            if (!keptVertices[source] && capOf[source] >= 0) {
                Arrays.fill(newJoints, i * 4, i * 4 + 4, (byte) capOf[source]);
                Arrays.fill(newWeights, i * 4, i * 4 + 4, (byte) 0);
                newWeights[i * 4] = (byte) 255;
                caps.add(bones[capOf[source]]);
            }
        }
        Cs2Mesh result = new Cs2Mesh(bones, parents, restLocal, inverseBind, newPositions, newNormals, newUvs, newJoints,
                newWeights, keptParts);
        result.caps = caps;
        return result;
    }

    String parentOf(String name) {
        int bone = index(name);
        return bone < 0 || parents[bone].isEmpty() ? null : parents[bone];
    }

    Matrix4f restWorld(String name) {
        int bone = index(name);
        Matrix4f result = new Matrix4f();
        while (bone >= 0) {
            result = new Matrix4f(restLocal[bone]).mul(result);
            bone = index(parents[bone]);
        }
        return result;
    }

    private int index(String name) {
        for (int i = 0; i < bones.length; i++) if (bones[i].equals(name)) return i;
        return -1;
    }

    private static boolean leftSide(String bone) {
        return bone.endsWith("_L") || bone.endsWith("_l") || bone.contains("_L_");
    }

    private Matrix4f parentWorld(int index) {
        for (int i = 0; i < index; i++) {
            if (bones[i].equals(parents[index])) return world[i];
        }
        return new Matrix4f();
    }

    void emit(PoseStack.Pose base, VertexConsumer buffer, Part part, int light, int overlay, float uScale, boolean triangles) {
        Matrix4f m = base.pose();
        Matrix3f n = base.normal();
        float m00 = m.m00(), m01 = m.m01(), m02 = m.m02(), m10 = m.m10(), m11 = m.m11(), m12 = m.m12();
        float m20 = m.m20(), m21 = m.m21(), m22 = m.m22(), m30 = m.m30(), m31 = m.m31(), m32 = m.m32();
        float n00 = n.m00(), n01 = n.m01(), n02 = n.m02(), n10 = n.m10(), n11 = n.m11(), n12 = n.m12();
        float n20 = n.m20(), n21 = n.m21(), n22 = n.m22();
        for (int vertex : part.vertices) {
            int p = vertex * 3;
            float x = skinnedPositions[p], y = skinnedPositions[p + 1], z = skinnedPositions[p + 2];
            emitPositions[p] = m00 * x + m10 * y + m20 * z + m30;
            emitPositions[p + 1] = m01 * x + m11 * y + m21 * z + m31;
            emitPositions[p + 2] = m02 * x + m12 * y + m22 * z + m32;
            x = skinnedNormals[p];
            y = skinnedNormals[p + 1];
            z = skinnedNormals[p + 2];
            float nx = n00 * x + n10 * y + n20 * z;
            float ny = n01 * x + n11 * y + n21 * z;
            float nz = n02 * x + n12 * y + n22 * z;
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            float scale = length > 1.0e-8f ? 1.0f / length : 0.0f;
            emitNormals[p] = nx * scale;
            emitNormals[p + 1] = ny * scale;
            emitNormals[p + 2] = nz * scale;
        }
        boolean mirrored = m.determinant3x3() < 0.0f;
        int overlayCoords = overlay == 0 ? OverlayTexture.NO_OVERLAY : overlay;
        int[] indices = part.indices;
        for (int t = 0; t + 2 < indices.length; t += 3) {
            int first = indices[t];
            int second = mirrored ? indices[t + 2] : indices[t + 1];
            int third = mirrored ? indices[t + 1] : indices[t + 2];
            vertex(buffer, first, light, overlayCoords, uScale);
            vertex(buffer, second, light, overlayCoords, uScale);
            vertex(buffer, third, light, overlayCoords, uScale);
            if (!triangles) vertex(buffer, third, light, overlayCoords, uScale);
        }
    }

    private void vertex(VertexConsumer buffer, int vertex, int light, int overlay, float uScale) {
        int p = vertex * 3;
        buffer.addVertex(emitPositions[p], emitPositions[p + 1], emitPositions[p + 2], -1,
                uvs[vertex * 2] * uScale, uvs[vertex * 2 + 1], overlay, light,
                emitNormals[p], emitNormals[p + 1], emitNormals[p + 2]);
    }

    private static String text(ByteBuffer data) {
        byte[] bytes = new byte[data.getShort() & 0xFFFF];
        data.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static Matrix4f matrix(ByteBuffer data) {
        float[] values = floats(data, 16);
        return new Matrix4f().set(values);
    }

    private static float[] floats(ByteBuffer data, int count) {
        float[] values = new float[count];
        data.asFloatBuffer().get(values);
        data.position(data.position() + count * 4);
        return values;
    }

    record Part(String texture, int[] indices, int[] vertices) {
    }
}
