package sanch3zss.raven.feature.module.render.armtweaks.knife;

import static sanch3zss.raven.feature.module.render.armtweaks.knife.SkinPainter.WRAP;
import static sanch3zss.raven.feature.module.render.armtweaks.knife.SkinPainter.encode;
import static sanch3zss.raven.feature.module.render.armtweaks.knife.SkinPainter.mix;
import static sanch3zss.raven.feature.module.render.armtweaks.knife.SkinPainter.saturate;
import static sanch3zss.raven.feature.module.render.armtweaks.knife.SkinPainter.smoothstep;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

final class GlovePainter {
    static final int SIZE = 1024;
    private static final float[][] BLUR = {{0, 0, 0.31972790f}, {-1, -1, 0.05102041f}, {-1, 0, 0.11904762f},
            {-1, 1, 0.05102041f}, {0, -1, 0.11904762f}, {0, 1, 0.11904762f}, {1, -1, 0.05102041f},
            {1, 0, 0.11904762f}, {1, 1, 0.05102041f}};
    private static final int[][] CORNERS = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
    private static final float LEVEL_POWER = 1.3219281f;

    private final GloveCatalog catalog;
    private final Map<String, SkinPainter.Channels> textures = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, SkinPainter.Channels> eldest) {
            return size() > 24;
        }
    };

    GlovePainter(GloveCatalog catalog) {
        this.catalog = catalog;
    }

    SkinPainter.Painted paint(GloveSkin skin, int seed, float wear) throws IOException {
        GloveCatalog.Paint paint = catalog.paints.get(skin);
        ThreadLocal<Job> jobs = ThreadLocal.withInitial(() -> {
            try {
                return new Job(paint, seed, wear);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }
        });
        int size = SIZE;
        float[] detailField = new float[size * size];
        float[] colorField = new float[size * size];
        float[] surfaceField = new float[size * size];
        IntStream.range(0, size).parallel().forEach(y -> {
            Job job = jobs.get();
            float v = (y + 0.5f) / size;
            for (int x = 0; x < size; x++) {
                job.prepare((x + 0.5f) / size, v);
                int index = y * size + x;
                detailField[index] = job.detailY * job.curvature;
                colorField[index] = job.dX * job.curvature;
                surfaceField[index] = job.surface[0] + job.surface[1] + job.surface[2];
            }
        });
        float ratio = size / 2048.0f;
        int[] pixels = new int[size * 2 * size];
        IntStream.range(0, size).parallel().forEach(y -> {
            Job job = jobs.get();
            float v = (y + 0.5f) / size;
            for (int x = 0; x < size; x++) {
                int index = y * size + x;
                int right = y * size + (x + 1) % size;
                int down = (y + 1) % size * size + x;
                job.prepare((x + 0.5f) / size, v);
                job.shade((detailField[right] - detailField[index]) * ratio, (detailField[down] - detailField[index]) * ratio,
                        (colorField[right] - colorField[index]) * ratio, (colorField[down] - colorField[index]) * ratio,
                        (surfaceField[right] - surfaceField[index]) * ratio, (surfaceField[down] - surfaceField[index]) * ratio);
                int target = y * size * 2 + x;
                pixels[target] = 0xFF000000 | encode(job.albedo[0]) << 16 | encode(job.albedo[1]) << 8 | encode(job.albedo[2]);
                int nx = Math.round(saturate(job.normal[0] * 0.5f + 0.5f) * 255.0f);
                int ny = Math.round(saturate(job.normal[1] * 0.5f + 0.5f) * 255.0f);
                int nz = Math.round(saturate(job.normal[2] * 0.5f + 0.5f) * 255.0f);
                int rough = Math.round(saturate(job.rough) * 255.0f);
                pixels[target + size] = rough << 24 | nx << 16 | ny << 8 | nz;
            }
        });
        return new SkinPainter.Painted(size, pixels);
    }

    private synchronized SkinPainter.Channels texture(String name, int srgbChannels) throws IOException {
        SkinPainter.Channels cached = textures.get(name);
        if (cached != null) return cached;
        String path = GloveCatalog.ROOT + "textures/" + name;
        path += GlovePainter.class.getResource(path + ".jpg") != null ? ".jpg" : ".png";
        SkinPainter.Channels loaded = SkinPainter.Channels.read(path, catalog.channels.getOrDefault(name, 4), srgbChannels);
        textures.put(name, loaded);
        return loaded;
    }

    private static float[] xform(float rotation, float scale, float offsetX, float offsetY) {
        double angle = rotation * 3.14159 / 180.0;
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        double half = 0.5 / (scale != 0.0f ? scale : 1.0f);
        double c2 = Math.cos(-angle);
        double s2 = Math.sin(-angle);
        double v7 = half * c2 - half * s2;
        double v8 = v7 * s2 + half * c2;
        return new float[]{(float) (c * scale), (float) (-s * scale), (float) (scale * c * v7 + scale * -s * v8 + (offsetX - 0.5)),
                (float) (s * scale), (float) (c * scale), (float) (scale * s * v7 + scale * c * v8 + (offsetY - 0.5))};
    }

    private static void decodeNormal(float[] sample, float[] out) {
        float x = sample[0] * 2.0f - 1.0f;
        float y = sample[1] * 2.0f - 1.0f;
        float z = (float) Math.sqrt(Math.max(0.0f, 1.0f - x * x - y * y));
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        out[0] = x / length;
        out[1] = y / length;
        out[2] = z / length;
    }

    private static void normalize(float[] vector) {
        float length = Math.max((float) Math.sqrt(vector[0] * vector[0] + vector[1] * vector[1] + vector[2] * vector[2]), 1.0e-9f);
        vector[0] /= length;
        vector[1] /= length;
        vector[2] /= length;
    }

    private static float luminance(float[] color) {
        return SkinPainter.luminance(color[0], color[1], color[2]);
    }

    private static float max(float[] color) {
        return Math.max(color[0], Math.max(color[1], color[2]));
    }

    private static float srgbToLinear(float value) {
        return value <= 0.04045f ? value * 0.07739938f : (float) Math.pow(value * 0.94786733f + 0.0521327f, 2.4f);
    }

    private static void recolor(float[] color, float[] reference, float target, float strength, float[] out) {
        if (strength <= 0.0f) {
            float level = luminance(color);
            for (int c = 0; c < 3; c++) out[c] = mix(color[c], level, -strength);
            return;
        }
        float referenceLevel = luminance(reference);
        float x = reference[0] * referenceLevel;
        float y = reference[1] * referenceLevel;
        float z = reference[2] * referenceLevel;
        float length = Math.max((float) Math.sqrt(x * x + y * y + z * z), 1.0e-9f);
        float dx = x / length - 0.577f;
        float dy = y / length - 0.577f;
        float dz = z / length - 0.577f;
        float directionLength = Math.max((float) Math.sqrt(dx * dx + dy * dy + dz * dz), 1.0e-9f);
        float hx = saturate(dx / directionLength * 2.0f + 1.0f);
        float hy = saturate(dy / directionLength * 2.0f + 1.0f);
        float hz = saturate(dz / directionLength * 2.0f + 1.0f);
        float scale = target / Math.max(SkinPainter.luminance(hx, hy, hz), 1.0e-9f);
        float weight = saturate(strength * (float) Math.pow(Math.abs((dx + dy + dz) * 0.577f), 0.2f));
        out[0] = mix(color[0], hx * scale, weight);
        out[1] = mix(color[1], hy * scale, weight);
        out[2] = mix(color[2], hz * scale, weight);
    }

    private final class Job {
        final GloveCatalog.Paint paint;
        final float wear;
        final float flip;
        final float[] patternUv;
        final float[] grungeUv;
        final SkinPainter.Channels mask;
        final SkinPainter.Channels surfaceTexture;
        final SkinPainter.Channels normalTexture;
        final SkinPainter.Channels noise;
        final SkinPainter.Channels pattern;
        final SkinPainter.Channels[] details = new SkinPainter.Channels[4];
        final SkinPainter.Channels[] detailNormals = new SkinPainter.Channels[4];
        final SkinPainter.Channels[] damageNormals = new SkinPainter.Channels[4];
        final SkinPainter.Channels[] grunges = new SkinPainter.Channels[4];
        final float[] blackPoint;
        final float[] detailScale;
        final float[] curvaturePower;
        final float[] roughnessContrast;
        final float[] roughnessBrightness;
        final float[] detailMetalness;
        final float[] wearBoost;
        final float[] wearBleaching;
        final float[] damageBleaching;
        final float[] grimeSaturation;
        final float[] grimeBrightness;
        final float[] detailGrunge;
        final float[] damageSaturation;
        final float[] damageBrightness;
        final float[] grungeMax;
        final float[] edgeWidth;
        final float[] damageMetalness;
        final float[] edgeMetalness;
        final float[] grungeRoughness;
        final float[] damageRoughnessContrast;
        final float[] damageRoughnessBrightness;
        final float[] edgeRoughness;
        final float[] normalContrast;
        final float thickness;
        final float colorMaskBlur;
        final float detailInfluence;
        final float paintDurability;
        final float patternRoughnessBrightness;
        final float patternMetalness;
        final float[] sample = new float[4];
        final float[][] layerSamples = new float[4][4];
        final float[] layerWeight = new float[4];
        final float[] surface = new float[4];
        final float[] weights = new float[8];
        final float[] patternColor = new float[3];
        final float[] grunge = new float[4];
        final float[] noiseSample = new float[4];
        final float[] baseNormal = new float[3];
        final float[] detailNormal = new float[3];
        final float[] damageNormal = new float[3];
        final float[] decoded = new float[3];
        final float[] color = new float[3];
        final float[] work = new float[3];
        final float[] grime = new float[3];
        final float[] mixed = new float[3];
        final float[] dirty = new float[3];
        final float[] first = new float[3];
        final float[] second = new float[3];
        final float[] hueA = new float[3];
        final float[] hueB = new float[3];
        final float[] grungeRgb = new float[3];
        final float[] lit = new float[3];
        final float[] albedo = new float[3];
        final float[] normal = new float[3];
        float m1;
        float m2;
        float m3;
        float u;
        float v;
        float curvature;
        float du;
        float dv;
        float dX;
        float dY;
        float dZ;
        float detailRough;
        float replace;
        float cover;
        float durability;
        float detailY;
        float thicknessSum;
        float shadow;
        float patternRough;
        float metal;
        float rough;

        Job(GloveCatalog.Paint paint, int seed, float wear) throws IOException {
            this.paint = paint;
            float clamped = Math.clamp(wear, paint.wear()[0], paint.wear()[1]);
            this.wear = (float) Math.pow(clamped, paint.scalars().get("g_fWearExponent"));
            this.flip = paint.flip() ? 1.0f : -1.0f;
            Map<String, float[]> rolled = new HashMap<>();
            SkinPainter.UniformRandom random = new SkinPainter.UniformRandom(seed);
            for (SkinCatalog.Roll roll : paint.rolls()) {
                float[][] bounds = roll.bounds();
                float[] values = new float[bounds.length];
                for (int i = 0; i < bounds.length; i++) {
                    values[i] = bounds[i].length == 1 ? bounds[i][0] : random.uniform(bounds[i][0], bounds[i][1]);
                }
                rolled.put(roll.name(), values);
            }
            Map<String, Float> scalars = paint.scalars();
            float rotation = rolled.getOrDefault("g_fPatternTexCoordRotation", new float[]{scalars.get("g_fPatternTexCoordRotation")})[0];
            float[] offset = rolled.getOrDefault("g_fPatternTexCoordOffset", new float[2]);
            patternUv = xform(flip < 0.0f ? -rotation : rotation, scalars.get("g_fPatternTexCoordScale"), offset[0], offset[1]);
            float[] grungeOffset = rolled.getOrDefault("g_fGrungeTexCoordOffset", new float[2]);
            grungeUv = xform(rolled.getOrDefault("g_fGrungeTexCoordRotation", new float[]{scalars.get("g_fGrungeTexCoordRotation")})[0],
                    scalars.get("g_fGrungeTexCoordScale"), grungeOffset[0], grungeOffset[1]);
            Map<String, String> names = paint.textures();
            mask = texture(names.get("g_tLayerMask"), 0);
            surfaceTexture = texture(names.get("g_tSurface"), 0);
            normalTexture = texture(names.get("g_tNormal"), 0);
            noise = texture(names.get("g_tNoise"), 3);
            pattern = paint.pattern() && names.containsKey("g_tPattern") ? texture(names.get("g_tPattern"), 0) : null;
            for (int i = 0; i < 4; i++) {
                details[i] = texture(names.get("g_tDetail" + (i + 1)), 0);
                detailNormals[i] = texture(names.get("g_tDetailNormal" + (i + 1)), 0);
                damageNormals[i] = texture(names.get("g_tDamageNormal" + (i + 1)), 0);
                grunges[i] = texture(names.get("g_tGrunge" + (i + 1)), 3);
            }
            Map<String, float[]> layers = paint.layers();
            blackPoint = layers.get("g_fDetailBlackPoint");
            detailScale = layers.get("g_fDetailScale");
            curvaturePower = layers.get("g_fCurvaturePower");
            roughnessContrast = layers.get("g_fDetailRoughnessContrast");
            roughnessBrightness = layers.get("g_fDetailRoughnessBrightness");
            detailMetalness = layers.get("g_fDetailMetalness");
            wearBoost = layers.get("g_fCurvatureWearBoost");
            wearBleaching = layers.get("g_fWearBleaching");
            damageBleaching = layers.get("g_fDamageBleaching");
            grimeSaturation = layers.get("g_fGrimeSaturation");
            grimeBrightness = layers.get("g_fGrimeBrightness");
            detailGrunge = layers.get("g_fDetailGrunge");
            damageSaturation = layers.get("g_fDamageSaturation");
            damageBrightness = layers.get("g_fDamageBrightness");
            grungeMax = layers.get("g_fGrungeMax");
            edgeWidth = layers.get("g_fDamageNormalEdgeWidth");
            damageMetalness = layers.get("g_fDamageMetalness");
            edgeMetalness = layers.get("g_fDamageEdgeMetalness");
            grungeRoughness = layers.get("g_fGrungeRoughnessBrightness");
            damageRoughnessContrast = layers.get("g_fDamageRoughnessContrast");
            damageRoughnessBrightness = layers.get("g_fDamageRoughnessBrightness");
            edgeRoughness = layers.get("g_fDamageEdgeRoughness");
            normalContrast = layers.get("g_fDetailNormalContrast");
            thickness = scalars.get("g_fPaintThickness");
            colorMaskBlur = scalars.get("g_fColorMaskBlur");
            detailInfluence = scalars.get("g_fPatternDetailInfluence");
            paintDurability = scalars.get("g_fPaintDurability");
            patternRoughnessBrightness = scalars.get("g_fPatternRoughnessBrightness");
            patternMetalness = scalars.get("g_fPatternMetalness");
        }

        float layer(float[] values) {
            return mix(mix(mix(values[0], values[1], m1), values[2], m2), values[3], m3);
        }

        float chain(int channel) {
            float sum = 0.0f;
            for (int i = 0; i < 4; i++) {
                if (layerWeight[i] != 0.0f) sum += layerSamples[i][channel] * layerWeight[i];
            }
            return sum;
        }

        void chainNormals(SkinPainter.Channels[] textures, float[] out) {
            for (int i = 0; i < 4; i++) {
                if (layerWeight[i] == 0.0f) continue;
                textures[i].sample(du, dv, WRAP, WRAP, sample);
                decodeNormal(sample, decoded);
                System.arraycopy(decoded, 0, layerSamples[i], 0, 3);
            }
            for (int c = 0; c < 3; c++) out[c] = chain(c);
            normalize(out);
        }

        void prepare(float u, float v) {
            this.u = u;
            this.v = v;
            mask.sample(u, v, WRAP, WRAP, sample);
            m1 = sample[0];
            m2 = sample[1];
            m3 = sample[2];
            layerWeight[0] = (1.0f - m1) * (1.0f - m2) * (1.0f - m3);
            layerWeight[1] = m1 * (1.0f - m2) * (1.0f - m3);
            layerWeight[2] = m2 * (1.0f - m3);
            layerWeight[3] = m3;
            surfaceTexture.sample(u, v, WRAP, WRAP, surface);
            curvature = (float) Math.pow(surface[0], layer(curvaturePower));
            float scale = layer(detailScale);
            du = u * scale;
            dv = v * scale;
            for (int i = 0; i < 4; i++) {
                if (layerWeight[i] == 0.0f) continue;
                details[i].sample(du, dv, WRAP, WRAP, layerSamples[i]);
                layerSamples[i][0] = blackPoint[i] + layerSamples[i][0] * (1.0f - blackPoint[i]);
                layerSamples[i][1] = blackPoint[i] + layerSamples[i][1] * (1.0f - blackPoint[i]);
            }
            dX = blackPoint[0] + chain(0) * (1.0f - blackPoint[0]);
            dY = blackPoint[0] + chain(1) * (1.0f - blackPoint[0]);
            dZ = chain(2);
            float dW = chain(3);
            detailRough = saturate((layer(roughnessContrast) * ((1.0f - dW) * (1.0f - dW) * 0.85f - 0.35f) + 0.5f)
                    * layer(roughnessBrightness));
            Arrays.fill(weights, 0.0f);
            int width = mask.width;
            int height = mask.height;
            for (float[] tap : BLUR) {
                int px = Math.floorMod((int) Math.floor((u + tap[0] / 1024.0f * colorMaskBlur) * width), width);
                int py = Math.floorMod((int) Math.floor((v + tap[1] / 1024.0f * colorMaskBlur) * height), height);
                int slot = (int) Math.floor(mask.data[(py * width + px) * mask.count + 3] * 8.0f);
                if (slot >= 0 && slot < 8) weights[slot] += tap[2];
            }
            durability = surface[3];
            detailY = dY;
            cover = 0.0f;
            replace = 1.0f;
            thicknessSum = 1.0f;
            shadow = 1.0f;
            patternRough = detailRough;
            metal = layer(detailMetalness);
            patternColor[0] = patternColor[1] = patternColor[2] = 0.0f;
            if (pattern == null) return;
            int mode = paint.patternMode();
            replace = mode == 1 || mode == 3 ? 1.0f : weights[paint.replaceIndex()];
            float shift = (dY * 2.0f - 1.0f) / 1024.0f * detailInfluence;
            float pu = patternUv[0] * u * flip + patternUv[1] * v + patternUv[2] + shift;
            float pv = patternUv[3] * u * flip + patternUv[4] * v + patternUv[5] + shift;
            pattern.sample(pu, pv, WRAP, WRAP, sample);
            if (mode < 2) {
                float pr = sample[0];
                float pg = sample[1];
                float pb = sample[2];
                float total = pr + pg + pb;
                if (thickness > 0.0f) {
                    float power = Math.max(0.01f, thickness * 0.15f);
                    float shade = mix((float) Math.pow(1.0f - pr, power), 1.0f, pr) * mix((float) Math.pow(1.0f - pg, power), 1.0f, pg)
                            * mix((float) Math.pow(1.0f - pb, power), 1.0f, pb);
                    shadow = (float) Math.pow(mix(1.0f, shade, replace), 0.2f);
                }
                float amount = saturate(total) * replace * saturate(thickness - dZ * wear * wear * wear * wear);
                cover = amount * Math.min(1.0f, thickness);
                durability -= cover * paintDurability;
                detailY = mix(dY, 0.5f, amount);
                thicknessSum = total * thickness;
                int[] palette = paint.palette();
                float[][] tints = paint.tints();
                for (int c = 0; c < 3; c++) {
                    patternColor[c] = mix(mix(mix(tints[palette[0]][c], tints[palette[1]][c], pr), tints[palette[2]][c], pg),
                            tints[palette[3]][c], pb);
                }
                patternRough = detailRough * mix(1.0f, patternRoughnessBrightness, cover);
            } else {
                System.arraycopy(sample, 0, patternColor, 0, 3);
            }
            metal = mix(metal, patternMetalness, replace);
        }

        void shade(float ax, float ay, float bx, float by, float sx, float sy) {
            float gu = grungeUv[0] * u + grungeUv[1] * v + grungeUv[2];
            float gv = grungeUv[3] * u + grungeUv[4] * v + grungeUv[5];
            for (int i = 0; i < 4; i++) {
                if (layerWeight[i] != 0.0f) grunges[i].sample(gu, gv, WRAP, WRAP, layerSamples[i]);
            }
            for (int c = 0; c < 4; c++) grunge[c] = chain(c);
            System.arraycopy(grunge, 0, grungeRgb, 0, 3);
            float grungeLevel = luminance(grungeRgb);
            noise.sample(u * 2.0f, v * 2.0f, WRAP, WRAP, noiseSample);
            normalTexture.sample(u, v, WRAP, WRAP, sample);
            decodeNormal(sample, baseNormal);
            chainNormals(detailNormals, detailNormal);
            chainNormals(damageNormals, damageNormal);

            float[][] tints = paint.tints();
            float[] tintSum = work;
            for (int c = 0; c < 3; c++) {
                float sum = 0.0f;
                for (int i = 0; i < 8; i++) sum += tints[i][c] * weights[i];
                tintSum[c] = sum;
            }
            float wearAmount = curvature * durability;
            float boost = layer(wearBoost) * curvature;
            float wearBase = saturate(wearAmount * grunge[3] + boost);
            float low = mix(mix(mix(paint.damageLevels()[0][0], paint.damageLevels()[1][0], m1), paint.damageLevels()[2][0], m2),
                    paint.damageLevels()[3][0], m3);
            float high = mix(mix(mix(paint.damageLevels()[0][1], paint.damageLevels()[1][1], m1), paint.damageLevels()[2][1], m2),
                    paint.damageLevels()[3][1], m3);
            float damage = smoothstep(low, high, wearBase * wear);
            float undamaged = 1.0f - damage;
            float exposed = saturate(damage + saturate(smoothstep(low, high, wearBase * wear * 4.0f)) * undamaged * dZ);
            for (int c = 0; c < 3; c++) {
                float base = srgbToLinear(tintSum[c]);
                color[c] = pattern == null ? base
                        : mix(base, mix(srgbToLinear(patternColor[c]), base, exposed), replace);
            }
            float colorLevel = luminance(color);
            float edges = mix((1.0f - Math.abs(ax)) * (1.0f - Math.abs(ay)) * saturate(Math.abs(ax) + Math.abs(ay)),
                    (1.0f - Math.abs(bx)) * (1.0f - Math.abs(by)) * saturate(Math.abs(bx) + Math.abs(by)), damage);
            float brightness = Math.min(1.0f, mix(detailY, dX, exposed)
                    + edges * (1.0f - saturate(Math.abs(sx))) * (1.0f - saturate(Math.abs(sy))));
            float lift = 1.0f + curvature * 0.5f;
            float brightMax = 0.0f;
            for (int c = 0; c < 3; c++) {
                work[c] = Math.max(color[c], color[c] * lift);
                brightMax = Math.max(brightMax, work[c]);
            }
            for (int c = 0; c < 3; c++) work[c] = Math.max(work[c], 0.001f);
            normalize(work);
            float level = Math.min(brightness / luminance(work), 3.0f * brightness * brightMax);
            for (int c = 0; c < 3; c++) lit[c] = saturate(work[c] * level);
            float bleach = mix(wear * surface[2] * layer(wearBleaching) * mix(dZ, 0.0f, saturate(thicknessSum - 1.0f)),
                    layer(damageBleaching), damage);
            recolor(grungeRgb, grungeRgb, grungeLevel, layer(grimeSaturation), grime);
            float grungeAmount = layer(detailGrunge);
            float grimeLevel = 1.0f + layer(grimeBrightness);
            float grungeBlend = grungeAmount * (1.0f + wear * grungeAmount) * undamaged;
            for (int c = 0; c < 3; c++) {
                float grunged = lit[c] * grungeRgb[c] * 2.0f;
                mixed[c] = mix(mix(lit[c], grunged, grungeBlend), grime[c] * grimeLevel * brightness, bleach);
            }
            recolor(mixed, color, luminance(mixed), layer(damageSaturation), second);
            float damageLevel = 1.0f + layer(damageBrightness);
            float dirtBlend = smoothstep(0.45f, 0.75f, (1.0f - surface[0] * surface[1] * surface[1]) * wear)
                    * (1.0f - colorLevel * 0.5f) * layer(grungeMax);
            for (int c = 0; c < 3; c++) {
                float damaged = mix(mixed[c], second[c] * damageLevel, damage);
                dirty[c] = mix(damaged, grungeRgb[c] * damaged, dirtBlend);
            }
            float half = luminance(dirty) * 0.5f;
            float dx = dirty[0] - half;
            float dy = dirty[1] - half;
            float dz = dirty[2] - half;
            float amount = (float) Math.pow(1.0f - colorLevel, 3.5f) * 0.3f + (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * 0.15f + 0.05f;
            float r = dirty[0];
            float g = dirty[1];
            float b = dirty[2];
            boolean red = r > g && r > b;
            boolean green = g > r && g > b;
            first[0] = 0.6f;
            first[1] = 1.0f;
            first[2] = 0.0f;
            second[0] = 0.2f;
            second[1] = 0.6f;
            second[2] = 1.0f;
            if (red || green) {
                first[0] += b;
                first[1] += r;
                first[2] += g;
            }
            if (red) {
                second[0] += b;
                second[1] += g;
                second[2] += r;
            } else if (green) {
                second[0] += g;
                second[1] += b;
                second[2] += r;
            }
            recolor(dirty, second, half, amount, hueA);
            recolor(dirty, first, half, amount, hueB);
            float ao8 = (float) Math.pow(surface[1], 8.0f);
            float hueBlend = saturate(ao8 * noiseSample[0]);
            float keep = saturate(noiseSample[2] * (0.5f + ao8 * 0.5f));
            float[] finalColor = mixed;
            for (int c = 0; c < 3; c++) finalColor[c] = saturate(mix(mix(hueA[c], hueB[c], hueBlend), dirty[c], keep));

            float edge = 0.0f;
            if (wear > 0.0f) {
                float reach = layer(edgeWidth) / 1024.0f * (1.0f + thicknessSum);
                for (int[] corner : CORNERS) {
                    for (int i = 0; i < 4; i++) {
                        if (layerWeight[i] == 0.0f) continue;
                        grunges[i].sample(gu + corner[0] * reach, gv + corner[1] * reach, WRAP, WRAP, layerSamples[i]);
                    }
                    float alpha = chain(3);
                    edge += Math.abs(damage - smoothstep(low, high, saturate(alpha * wearAmount + boost) * wear));
                }
                edge *= 0.25f;
            }
            float metalness = mix(mix(metal, layer(damageMetalness), damage), layer(edgeMetalness), edge);
            float dirtyRough = mix(Math.min(1.0f, patternRough), 1.0f - grungeLevel + layer(grungeRoughness), bleach);
            float damagedRough = saturate((layer(damageRoughnessContrast) * ((1.0f - dX) * (1.0f - dX) * 0.85f - 0.35f) + 0.5f)
                    * layer(damageRoughnessBrightness));
            rough = mix(mix(dirtyRough, damagedRough, damage), layer(edgeRoughness), edge);

            float levelLow = mix(0.045f, 0.08f, metalness);
            float levelHigh = mix(1.0f, 0.92f, metalness);
            for (int c = 0; c < 3; c++) work[c] = mix(lit[c], lit[c] * grungeRgb[c] * 2.0f, grungeAmount);
            float grungedLevel = luminance(work);
            for (int c = 0; c < 3; c++) work[c] = Math.max(finalColor[c], 0.0003f);
            normalize(work);
            float unitMax = max(work);
            float target = mix(Math.min(levelLow, grungedLevel), levelHigh, saturate((float) Math.pow(max(finalColor), LEVEL_POWER)));
            for (int c = 0; c < 3; c++) albedo[c] = mix(finalColor[c], work[c] * target / unitMax, wear);

            float contrast = layer(normalContrast);
            for (int c = 0; c < 3; c++) {
                float covered = mix(detailNormal[c], c == 2 ? 1.0f : 0.0f, cover);
                work[c] = mix(covered, damageNormal[c], damage);
            }
            float surfaceZ = work[2];
            normal[0] = baseNormal[0] + work[0] * contrast;
            normal[1] = baseNormal[1] + work[1] * contrast;
            normal[2] = baseNormal[2];
            normalize(normal);
            float occlusion = saturate(surface[1] * (float) Math.pow(Math.max(surfaceZ, 0.0f), 0.2f) * (1.0f - edge * 0.2f)
                    * mix(shadow, 1.0f, damage));
            float shading = 0.15f + 0.85f * occlusion;
            for (int c = 0; c < 3; c++) albedo[c] *= shading;
        }
    }
}
