package sanch3zss.raven.feature.module.render.armtweaks.knife;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class Cs2Clip {
    final float duration;
    final String[] bones;
    final List<Sound> sounds = new ArrayList<>();
    final Map<String, float[]> sections = new HashMap<>();
    private final int[] parents;
    private final Vector3f[] restTranslation;
    private final Quaternionf[] restRotation;
    private final float[][] rotationTimes;
    private final float[][] rotationValues;
    private final float[][] translationTimes;
    private final float[][] translationValues;

    Cs2Clip(String path) throws Exception {
        ByteBuffer data;
        try (InputStream input = Cs2Clip.class.getResourceAsStream(path)) {
            if (input == null) throw new IllegalStateException(path);
            data = ByteBuffer.wrap(input.readAllBytes()).order(ByteOrder.LITTLE_ENDIAN);
        }
        duration = data.getFloat();
        int count = data.getInt();
        bones = new String[count];
        parents = new int[count];
        restTranslation = new Vector3f[count];
        restRotation = new Quaternionf[count];
        rotationTimes = new float[count][];
        rotationValues = new float[count][];
        translationTimes = new float[count][];
        translationValues = new float[count][];
        for (int i = 0; i < count; i++) {
            bones[i] = text(data);
            parents[i] = data.getShort();
            restTranslation[i] = new Vector3f(data.getFloat(), data.getFloat(), data.getFloat());
            restRotation[i] = new Quaternionf(data.getFloat(), data.getFloat(), data.getFloat(), data.getFloat());
            int keys = data.getInt();
            rotationTimes[i] = floats(data, keys);
            rotationValues[i] = new float[keys * 4];
            for (int k = 0; k < keys * 4; k++) rotationValues[i][k] = data.getShort() / 32767.0f;
            keys = data.getInt();
            translationTimes[i] = floats(data, keys);
            translationValues[i] = floats(data, keys * 3);
        }
        int soundCount = data.getInt();
        for (int i = 0; i < soundCount; i++) {
            float time = data.getFloat();
            sounds.add(new Sound(time, Identifier.fromNamespaceAndPath("raven", text(data))));
        }
        int sectionCount = data.getInt();
        for (int i = 0; i < sectionCount; i++) {
            String name = text(data);
            sections.put(name, new float[]{data.getFloat(), data.getFloat()});
        }
    }

    void sample(float time, Map<String, Local> out) {
        for (int i = 0; i < bones.length; i++) {
            Local local = out.computeIfAbsent(bones[i], name -> new Local());
            local.parent = parents[i] < 0 ? null : bones[parents[i]];
            sampleRotation(i, time, local.rotation);
            sampleTranslation(i, time, local.translation);
        }
    }

    Quaternionf restRotation(String bone) {
        for (int i = 0; i < bones.length; i++) {
            if (bones[i].equals(bone)) return restRotation[i];
        }
        return null;
    }

    String parentOf(String bone) {
        for (int i = 0; i < bones.length; i++) {
            if (bones[i].equals(bone)) return parents[i] < 0 ? null : bones[parents[i]];
        }
        return null;
    }

    private void sampleRotation(int bone, float time, Quaternionf out) {
        float[] times = rotationTimes[bone];
        float[] values = rotationValues[bone];
        if (times.length == 0) {
            out.set(restRotation[bone]);
            return;
        }
        int index = keyIndex(times, time);
        out.set(values[index * 4], values[index * 4 + 1], values[index * 4 + 2], values[index * 4 + 3]);
        if (index + 1 < times.length && time > times[index]) {
            float t = (time - times[index]) / Math.max(1.0e-6f, times[index + 1] - times[index]);
            int next = (index + 1) * 4;
            out.slerp(new Quaternionf(values[next], values[next + 1], values[next + 2], values[next + 3]), Math.min(1.0f, t));
        }
        out.normalize();
    }

    private void sampleTranslation(int bone, float time, Vector3f out) {
        float[] times = translationTimes[bone];
        float[] values = translationValues[bone];
        if (times.length == 0) {
            out.set(restTranslation[bone]);
            return;
        }
        int index = keyIndex(times, time);
        out.set(values[index * 3], values[index * 3 + 1], values[index * 3 + 2]);
        if (index + 1 < times.length && time > times[index]) {
            float t = Math.min(1.0f, (time - times[index]) / Math.max(1.0e-6f, times[index + 1] - times[index]));
            int next = (index + 1) * 3;
            out.lerp(new Vector3f(values[next], values[next + 1], values[next + 2]), t);
        }
    }

    private static int keyIndex(float[] times, float time) {
        int low = 0;
        int high = times.length - 1;
        while (low < high) {
            int middle = (low + high + 1) >>> 1;
            if (times[middle] <= time) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    static Map<String, Matrix4f> compose(Map<String, Local> locals) {
        Map<String, Matrix4f> world = new HashMap<>();
        for (String name : locals.keySet()) resolve(name, locals, world);
        return world;
    }

    private static Matrix4f resolve(String name, Map<String, Local> locals, Map<String, Matrix4f> world) {
        Matrix4f cached = world.get(name);
        if (cached != null) return cached;
        Local local = locals.get(name);
        Matrix4f matrix = new Matrix4f().translationRotate(local.translation, local.rotation);
        if (local.parent != null && locals.containsKey(local.parent)) {
            matrix = new Matrix4f(resolve(local.parent, locals, world)).mul(matrix);
        }
        world.put(name, matrix);
        return matrix;
    }

    private static String text(ByteBuffer data) {
        byte[] bytes = new byte[data.getShort() & 0xFFFF];
        data.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static float[] floats(ByteBuffer data, int count) {
        float[] values = new float[count];
        for (int i = 0; i < count; i++) values[i] = data.getFloat();
        return values;
    }

    static final class Local {
        String parent;
        final Vector3f translation = new Vector3f();
        final Quaternionf rotation = new Quaternionf();

        void blend(Local other, float weight) {
            translation.lerp(other.translation, weight);
            rotation.slerp(other.rotation, weight).normalize();
        }
    }

    record Sound(float time, Identifier event) {
    }
}
