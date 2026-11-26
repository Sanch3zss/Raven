package sanch3zss.raven.feature.module.render.armtweaks.knife;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;

final class SkinPainter {
    static final int SIZE = 1024;
    static final int WRAP = 0;
    private static final int MIRROR = 1;
    private static final int CLAMP = 2;
    static final float[] SRGB_TO_LINEAR = new float[256];
    private static final int[] LINEAR_TO_SRGB = new int[4096];
    private static final float[] EXPOSED = {0.38f, 0.37f, 0.35f};

    static {
        for (int i = 0; i < 256; i++) {
            float c = i / 255.0f;
            SRGB_TO_LINEAR[i] = c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
        }
        for (int i = 0; i < 4096; i++) {
            float c = i / 4095.0f;
            float encoded = c <= 0.0031308f ? c * 12.92f : 1.055f * (float) Math.pow(c, 1.0f / 2.4f) - 0.055f;
            LINEAR_TO_SRGB[i] = Math.round(Math.clamp(encoded, 0.0f, 1.0f) * 255.0f);
        }
    }

    private final SkinCatalog catalog;
    private final Map<String, Channels> patterns = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Channels> eldest) {
            return size() > 12;
        }
    };
    private final Map<String, Inputs> inputs = new HashMap<>();

    SkinPainter(SkinCatalog catalog) {
        this.catalog = catalog;
    }

    Painted paint(String model, KnifeSkin skin, int seed, float wear) throws IOException {
        SkinCatalog.Knife knife = catalog.knife(model);
        Inputs in = inputs(model);
        String paintId = knife.skins().get(skin);
        SkinCatalog.Paint paint = paintId == null ? null : catalog.paints.get(paintId);
        int size = paint == null ? in.base.width : SIZE;
        int[] pixels = new int[size * 2 * size];
        Pixel pixel = new Pixel();
        Job job = paint == null ? null : new Job(knife, paint, seed, wear);
        float[] sample = new float[4];
        for (int y = 0; y < size; y++) {
            float v = (y + 0.5f) / size;
            for (int x = 0; x < size; x++) {
                float u = (x + 0.5f) / size;
                in.base.sample(u, v, CLAMP, CLAMP, sample);
                pixel.baseR = sample[0];
                pixel.baseG = sample[1];
                pixel.baseB = sample[2];
                pixel.baseRough = sample[3];
                in.occlusion.sample(u, v, CLAMP, CLAMP, sample);
                pixel.cavity = sample[0];
                pixel.ao = sample[1];
                pixel.noPaint = sample[2];
                pixel.baseMetal = sample[3];
                if (job == null) {
                    pixel.r = pixel.baseR;
                    pixel.g = pixel.baseG;
                    pixel.b = pixel.baseB;
                    pixel.rough = pixel.baseRough;
                    pixel.metal = pixel.baseMetal;
                } else {
                    in.masks.sample(u, v, CLAMP, CLAMP, sample);
                    pixel.mx = sample[0];
                    pixel.my = sample[1];
                    pixel.mz = sample[2];
                    pixel.side = sample[3];
                    job.shade(in, u, v, pixel);
                }
                int index = y * size * 2 + x;
                pixels[index] = 0xFF000000 | encode(pixel.r) << 16 | encode(pixel.g) << 8 | encode(pixel.b);
                int rough = Math.round(Math.clamp(pixel.rough, 0.0f, 1.0f) * 255.0f);
                int metal = Math.round(Math.clamp(pixel.metal, 0.0f, 1.0f) * 255.0f);
                pixels[index + size] = 0xFF000000 | rough << 16 | metal << 8;
            }
        }
        return new Painted(size, pixels);
    }

    record Painted(int size, int[] pixels) {
    }

    private synchronized Inputs inputs(String model) throws IOException {
        Inputs cached = inputs.get(model);
        if (cached != null) return cached;
        if (inputs.size() >= 2) inputs.clear();
        String root = SkinCatalog.ROOT + model + "/";
        Inputs loaded = new Inputs(Channels.read(root + "base.jpg", 4, 3), Channels.read(root + "occlusion.png", 4, 3),
                Channels.read(root + "masks.png", 4, 0), Channels.position(root + "position.bin", 256));
        inputs.put(model, loaded);
        return loaded;
    }

    private synchronized Channels pattern(String name, boolean srgb) throws IOException {
        String key = name + (srgb ? "#srgb" : "");
        Channels cached = patterns.get(key);
        if (cached != null) return cached;
        int count = catalog.channels.getOrDefault(name, 4);
        Channels loaded = Channels.pattern(SkinCatalog.ROOT + "patterns/" + name + ".jpg", count, srgb);
        patterns.put(key, loaded);
        return loaded;
    }

    static int encode(float linear) {
        return LINEAR_TO_SRGB[(int) (Math.clamp(linear, 0.0f, 1.0f) * 4095.0f + 0.5f)];
    }

    static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.clamp((x - edge0) / (edge1 != edge0 ? edge1 - edge0 : 1.0e-9f), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    static float mix(float a, float b, float t) {
        return a + (b - a) * t;
    }

    static float saturate(float value) {
        return Math.clamp(value, 0.0f, 1.0f);
    }

    static float luminance(float r, float g, float b) {
        return r * 0.2125f + g * 0.7154f + b * 0.0721f;
    }

    private static float round2(float value) {
        return (float) (Math.floor((value + 0.005) * 100.0) / 100.0);
    }

    private static float[] xform(float rotation, float scale, float offsetX, float offsetY) {
        float v0 = round2(rotation);
        float v1 = round2(scale);
        double v3 = v0 * 3.1415927 / 180.0;
        double v4 = Math.cos(v3);
        double v5 = Math.sin(v3);
        double v6 = 0.5 / (v1 != 0.0f ? v1 : 1.0f);
        double v7 = Math.cos(-v3);
        double v8 = Math.sin(-v3);
        double v9 = v6 * v7 - v6 * v8;
        double v10 = v9 * v8 + v6 * v7;
        float ox = round2(offsetX);
        float oy = round2(offsetY);
        return new float[]{(float) (v4 * v1), (float) (-v5 * v1), (float) (v1 * v4 * v9 + v1 * -v5 * v10 + (ox - 0.5)),
                (float) (v5 * v1), (float) (v4 * v1), (float) (v1 * v5 * v9 + v1 * v4 * v10 + (oy - 0.5))};
    }

    private final class Job {
        final SkinCatalog.Paint paint;
        final int style;
        final float wear;
        final float wearFactor;
        final float[] pattern;
        final float[] wearUv;
        final float[] grungeUv;
        final Channels patternTexture;
        final Channels roughnessTexture;
        final Channels wearTexture;
        final Channels grungeTexture;
        final float[][] colors;
        final float[] sample = new float[4];
        final float[] second = new float[4];
        final float[] third = new float[4];
        final float[] grungeSample = new float[4];
        final float[] grungeMix = new float[4];
        final float[] tint = new float[3];
        final float[] rgb = new float[3];
        float scratch;

        Job(SkinCatalog.Knife knife, SkinCatalog.Paint paint, int seed, float wear) throws IOException {
            this.paint = paint;
            this.style = paint.style();
            this.colors = paint.colors();
            this.wear = Math.clamp(wear, paint.wear()[0], paint.wear()[1]);
            this.wearFactor = this.wear * 6.0f + 1.0f;
            Map<String, float[]> rolled = new HashMap<>();
            UniformRandom random = new UniformRandom(seed);
            for (SkinCatalog.Roll roll : paint.rolls()) {
                float[][] bounds = roll.bounds();
                float[] values = new float[bounds.length];
                for (int i = 0; i < bounds.length; i++) {
                    values[i] = bounds[i].length == 1 ? bounds[i][0] : random.uniform(bounds[i][0], bounds[i][1]);
                }
                rolled.put(roll.name(), values);
            }
            boolean positional = style == 2 || style == 5;
            float factor = paint.ignoreSize() ? 1.0f : positional ? knife.length() / 36.0f : knife.uvScale();
            float[] offset = rolled.getOrDefault("g_vPatternTexCoordOffset", paint.offset());
            float rotation = rolled.getOrDefault("g_flPatternTexCoordRotation", new float[]{paint.rotation()})[0];
            pattern = xform(rotation, paint.scale() * factor, offset[0], offset[1]);
            float[] wearOffset = rolled.getOrDefault("g_vWearTexCoordOffset", new float[2]);
            wearUv = xform(rolled.getOrDefault("g_flWearTexCoordRotation", new float[1])[0],
                    rolled.getOrDefault("g_flWearTexCoordScale", new float[]{1.6f})[0] * factor, wearOffset[0], wearOffset[1]);
            float[] grungeOffset = rolled.getOrDefault("g_vGrungeTexCoordOffset", new float[2]);
            grungeUv = xform(rolled.getOrDefault("g_flGrungeTexCoordRotation", new float[1])[0],
                    rolled.getOrDefault("g_flGrungeTexCoordScale", new float[]{1.6f})[0] * factor, grungeOffset[0], grungeOffset[1]);
            patternTexture = paint.pattern() == null ? null : pattern(paint.pattern(), style >= 6);
            roughnessTexture = paint.roughnessTexture() == null ? null : pattern(paint.roughnessTexture(), false);
            wearTexture = pattern("paint_wear", false);
            grungeTexture = pattern("gun_grunge", true);
        }

        void shade(Inputs in, float u, float v, Pixel p) {
            wearTexture.sample(wearUv[0] * u + wearUv[1] * v + wearUv[2], wearUv[3] * u + wearUv[4] * v + wearUv[5], WRAP, WRAP, sample);
            scratch = sample[0];
            float edge = p.noPaint + scratch * p.cavity;
            grungeTexture.sample(grungeUv[0] * u + grungeUv[1] * v + grungeUv[2], grungeUv[3] * u + grungeUv[4] * v + grungeUv[5],
                    WRAP, WRAP, grungeSample);
            float dirt = (float) Math.pow(1.0f - p.cavity, 4.0f) * 0.25f + 0.75f * wear;
            for (int c = 0; c < 4; c++) grungeMix[c] = 1.0f + (grungeSample[c] - 1.0f) * dirt;
            if (style == 2 || style == 5) {
                positional(in, u, v, p);
            } else if (patternTexture != null) {
                patternTexture.sample(pattern[0] * u + pattern[1] * v + pattern[2],
                        pattern[3] * u + pattern[4] * v + pattern[5], paint.wrapU(), paint.wrapV(), sample);
            }
            float alpha = sample[3];
            float alphaBand = smoothstep(0.5f, 0.6f, alpha) * smoothstep(1.0f, 0.9f, alpha);
            switch (style) {
                case 0 -> solid(edge, p);
                case 1, 2 -> camo(edge, alpha, alphaBand, p);
                case 4, 5 -> anodized(edge, alphaBand, p);
                case 6 -> custom(edge, alpha, alphaBand, p);
                default -> aged(u, v, alpha, alphaBand, p);
            }
        }

        private void positional(Inputs in, float u, float v, Pixel p) {
            in.position.sample(u, v, CLAMP, CLAMP, third);
            float px = third[0] * 2.0f;
            float py = third[1] * 2.0f;
            float pz = third[2] * 2.0f;
            float top = third[3];
            int wu = paint.wrapU();
            int wv = paint.wrapV();
            patternTexture.sample(pattern[0] * py + pattern[1] * pz + pattern[2], pattern[3] * py + pattern[4] * pz + pattern[5], wu, wv, sample);
            patternTexture.sample(pattern[0] * px + pattern[1] * pz + pattern[2], pattern[3] * px + pattern[4] * pz + pattern[5], wu, wv, second);
            float sideBlend = paint.sprayBlend()[0] * p.side;
            for (int i = 0; i < 4; i++) sample[i] = mix(sample[i], second[i], sideBlend);
            patternTexture.sample(pattern[0] * py + pattern[1] * px + pattern[2], pattern[3] * py + pattern[4] * px + pattern[5], wu, wv, second);
            float topBlend = paint.sprayBlend()[1] * top;
            for (int i = 0; i < 4; i++) sample[i] = mix(sample[i], second[i], topBlend);
        }

        private void finish(Pixel p, float bare, float rough, float metal) {
            p.r = mix(rgb[0] * grungeMix[0], p.baseR, bare);
            p.g = mix(rgb[1] * grungeMix[1], p.baseG, bare);
            p.b = mix(rgb[2] * grungeMix[2], p.baseB, bare);
            p.rough = mix(Math.min(1.0f, rough), p.baseRough, bare);
            p.metal = mix(metal, p.baseMetal, bare);
        }

        private float chain(int channel, float w1, float w2, float w3) {
            return mix(mix(mix(colors[0][channel], colors[1][channel], w1), colors[2][channel], w2), colors[3][channel], w3);
        }

        private float chain(float[] values, float w1, float w2, float w3) {
            return mix(mix(mix(values[0], values[1], w1), values[2], w2), values[3], w3);
        }

        private float grungeRoughness() {
            return (1.0f - grungeMix[3]) * wear * wear * 0.5f;
        }

        private void solid(float edge, Pixel p) {
            float bare = smoothstep(0.58f, 0.68f, edge * wearFactor);
            float rough = paint.colorRoughness() == null ? paint.roughness() : chain(paint.colorRoughness(), p.mx, p.my, p.mz);
            for (int c = 0; c < 3; c++) rgb[c] = chain(c, p.mx, p.my, p.mz);
            finish(p, bare, rough, chain(paint.colorMetalness(), p.mx, p.my, p.mz));
        }

        private void camo(float edge, float alpha, float alphaBand, Pixel p) {
            float amount = style == 1
                    ? (edge * wearFactor + alphaBand) * Math.max(saturate(p.my + p.mz), smoothstep(0.0f, 0.5f, alpha))
                    : edge * wearFactor;
            float first = 0.56f - 0.06f * wear;
            float middle = 0.54f - 0.12f * wear;
            float w1 = sample[0] * smoothstep(0.58f, first, amount);
            float w2 = sample[1] * smoothstep(first, middle, amount);
            float w3 = sample[2] * smoothstep(middle, 0.52f - 0.18f * wear, amount);
            float bare = smoothstep(0.58f, 0.68f, amount);
            float level = mix(paint.brightness(), 1.0f, smoothstep(0.0f, 0.01f, bare) * (1.0f - alphaBand));
            for (int c = 0; c < 3; c++) {
                float color = chain(c, w1, w2, w3);
                if (style == 1) color = mix(mix(color, colors[2][c], p.my), colors[3][c], p.mz);
                rgb[c] = saturate(color * level);
            }
            float metal = chain(paint.colorMetalness(), w1, w2, w3);
            float rough = paint.roughness();
            if (paint.colorRoughness() != null) rough = chain(paint.colorRoughness(), w1, w2, w3);
            if (style == 1) {
                metal = mix(mix(metal, paint.colorMetalness()[2], p.my), paint.colorMetalness()[3], p.mz);
                if (paint.colorRoughness() != null) {
                    rough = mix(mix(rough, paint.colorRoughness()[2], p.my), paint.colorRoughness()[3], p.mz);
                }
            }
            finish(p, bare, rough + grungeRoughness(), metal);
        }

        private void anodized(float edge, float alphaBand, Pixel p) {
            boolean multi = style == 4;
            float amount = multi ? edge * wearFactor + alphaBand : edge * wearFactor;
            float worn = smoothstep(0.58f, 0.68f, amount);
            float w1 = multi ? sample[0] * p.mx : sample[0];
            float w2 = multi ? sample[1] * p.mx : sample[1];
            float w3 = multi ? sample[2] * p.mx : sample[2];
            float bare = Math.max(1.0f - p.mx, worn);
            float exposed = smoothstep(0.0f, 0.01f, worn) * (1.0f - alphaBand) * p.mx;
            float level = mix(paint.brightness(), 1.0f, exposed);
            float[] base = {p.baseR, p.baseG, p.baseB};
            for (int c = 0; c < 3; c++) {
                float color = mix(mix(chain(c, w1, w2, w3), colors[2][c], p.my), colors[3][c], p.mz);
                color = mix(base[c], color, p.mx);
                color = mix(color, EXPOSED[c], exposed);
                color = saturate(saturate(color * level) * level);
                rgb[c] = mix(color * mix(grungeMix[c], 1.0f, exposed), base[c], bare);
            }
            float rough = paint.roughness();
            if (multi) {
                float shine = 1.0f - Math.min(1.0f, sample[3] * 2.0f);
                rough = sample[3] >= 0.5f ? paint.roughness() : shine * shine * 0.85f + 0.15f;
                rough = mix(rough, paint.roughness(), Math.max(Math.max(p.my, p.mz), bare));
            }
            rough = Math.min(1.0f, mix(rough, 0.35f, exposed) + (1.0f - luminance(grungeMix[0], grungeMix[1], grungeMix[2])) * 0.2f);
            p.r = rgb[0];
            p.g = rgb[1];
            p.b = rgb[2];
            p.rough = mix(p.baseRough, rough, Math.max(0.0f, 1.0f - bare));
            p.metal = mix(p.mx, p.baseMetal, bare);
        }

        private void custom(float edge, float alpha, float alphaBand, Pixel p) {
            float strength = smoothstep(0.0f, 0.5f, alpha);
            float bare = smoothstep(0.58f, 0.68f, (edge * wearFactor + alphaBand) * strength);
            for (int c = 0; c < 3; c++) rgb[c] = saturate(sample[c] * paint.brightness());
            finish(p, bare, paint.roughness() + grungeRoughness(), paint.metalness());
        }

        private float paintRoughness(float u, float v) {
            if (roughnessTexture == null) return 0.5f;
            float su = paint.roughnessPatternUv() ? pattern[0] * u + pattern[1] * v + pattern[2] : u;
            float sv = paint.roughnessPatternUv() ? pattern[3] * u + pattern[4] * v + pattern[5] : v;
            roughnessTexture.sample(su, sv, WRAP, WRAP, second);
            return second[0];
        }

        private void aged(float u, float v, float alpha, float alphaBand, Pixel p) {
            float patinaWear = smoothstep(0.1f, 0.2f, scratch * p.ao * p.cavity * p.cavity * wear);
            float grime = saturate(grungeSample[0] * grungeSample[1] * grungeSample[2]);
            float patina = smoothstep(0.0f, 0.15f, saturate(p.cavity * p.ao - wear * 0.1f) - grime * 0.23f + 0.08f);
            float dirtLevel = luminance(grungeMix[0], grungeMix[1], grungeMix[2]);
            float wornAlpha = (1.0f - grungeMix[3]) * wear;
            float patinaRough = saturate(paintRoughness(u, v) * mix(1.0f, 0.9f, patinaWear) + (1.0f - dirtLevel) * wear * 0.05f
                    + (1.0f - patina) * 0.15f * wear + wornAlpha * 0.15f);
            float patinaMetal = mix(mix(1.0f, (float) Math.sqrt(Math.max(0.0f, patina * grungeMix[3] * dirtLevel)), wear), 1.0f, patinaWear);
            for (int c = 0; c < 3; c++) tint[c] = mix(sample[c], sample[c] * paint.brightness(), Math.max(p.mx, paint.adjust()));
            float tintLevel = luminance(tint[0], tint[1], tint[2]);
            float aging = (float) Math.sqrt(wear);
            float unmasked = 1.0f - p.mx;
            float paintedRough = mix(Math.min(1.0f, patinaRough + wornAlpha * wear * 0.5f), patinaRough, p.mx);
            float[] base = {p.baseR, p.baseG, p.baseB};
            if (style == 7) {
                for (int c = 0; c < 3; c++) {
                    float shade = mix(mix(colors[1][c], colors[3][c], aging), mix(colors[1][c], colors[2][c], wear), patina);
                    float color = mix(shade * tint[c], colors[0][c] * tintLevel, patinaWear) * grungeMix[c];
                    rgb[c] = mix(color, base[c], unmasked);
                }
                p.rough = mix(p.baseRough, paintedRough, (p.noPaint <= 0.996f ? 1.0f : 0.0f) * p.mx);
                p.metal = mix(p.baseMetal, patinaMetal, p.mx);
            } else {
                float strength = smoothstep(0.0f, 0.5f, alpha);
                float amount = ((Math.min(p.noPaint, unmasked) + scratch * p.cavity) * wearFactor + alphaBand) * strength;
                boolean full = p.mx > 0.99f;
                float worn = full ? amount : smoothstep(0.58f, 0.68f, amount);
                for (int c = 0; c < 3; c++) {
                    float shade = mix(mix(colors[1][c], colors[3][c], aging), mix(colors[1][c], colors[2][c], wear), patina);
                    float metallic = mix(shade * tint[c], colors[0][c] * tintLevel, patinaWear);
                    rgb[c] = mix(mix(tint[c], metallic, p.mx) * grungeMix[c], base[c], worn * unmasked);
                }
                p.rough = mix(p.baseRough, paintedRough, full ? 1.0f : Math.max(0.0f, 1.0f - worn));
                p.metal = mix(mix(paint.metalness(), p.baseMetal, worn), patinaMetal, p.mx);
            }
            p.r = rgb[0];
            p.g = rgb[1];
            p.b = rgb[2];
        }
    }

    private static final class Pixel {
        float baseR;
        float baseG;
        float baseB;
        float baseRough;
        float baseMetal;
        float cavity;
        float ao;
        float noPaint;
        float mx;
        float my;
        float mz;
        float side;
        float r;
        float g;
        float b;
        float rough;
        float metal;
    }

    private record Inputs(Channels base, Channels occlusion, Channels masks, Channels position) {
    }

    static final class Channels {
        final int width;
        final int height;
        final int count;
        final float[] data;

        Channels(int width, int height, int count) {
            this.width = width;
            this.height = height;
            this.count = count;
            this.data = new float[width * height * count];
        }

        static Channels read(String path, int count, int srgbChannels) throws IOException {
            Raster raster = image(path).getRaster();
            int width = raster.getWidth();
            int height = raster.getHeight() / count;
            Channels channels = new Channels(width, height, count);
            int[] row = new int[width];
            for (int c = 0; c < count; c++) {
                for (int y = 0; y < height; y++) {
                    raster.getSamples(0, c * height + y, width, 1, 0, row);
                    for (int x = 0; x < width; x++) {
                        channels.data[(y * width + x) * count + c] = c < srgbChannels ? SRGB_TO_LINEAR[row[x]] : row[x] / 255.0f;
                    }
                }
            }
            return channels;
        }

        static Channels pattern(String path, int stored, boolean srgb) throws IOException {
            Raster raster = image(path).getRaster();
            int width = raster.getWidth();
            int height = raster.getHeight() / stored;
            Channels channels = new Channels(width, height, 4);
            int[] row = new int[width];
            boolean gray = stored <= 2;
            boolean alpha = stored == 2 || stored == 4;
            for (int i = 0; i < width * height; i++) channels.data[i * 4 + 3] = 1.0f;
            for (int c = 0; c < stored; c++) {
                boolean isAlpha = alpha && c == stored - 1;
                for (int y = 0; y < height; y++) {
                    raster.getSamples(0, c * height + y, width, 1, 0, row);
                    for (int x = 0; x < width; x++) {
                        float value = !isAlpha && srgb ? SRGB_TO_LINEAR[row[x]] : row[x] / 255.0f;
                        int index = (y * width + x) * 4;
                        if (isAlpha) {
                            channels.data[index + 3] = value;
                        } else if (gray) {
                            channels.data[index] = value;
                            channels.data[index + 1] = value;
                            channels.data[index + 2] = value;
                        } else {
                            channels.data[index + c] = value;
                        }
                    }
                }
            }
            return channels;
        }

        static Channels position(String path, int size) throws IOException {
            byte[] bytes;
            try (InputStream input = SkinPainter.class.getResourceAsStream(path)) {
                if (input == null) throw new IOException(path);
                bytes = input.readAllBytes();
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            Channels channels = new Channels(size, size, 4);
            for (int i = 0; i < channels.data.length; i++) channels.data[i] = (buffer.getShort() & 0xFFFF) / 65535.0f;
            return channels;
        }

        private static BufferedImage image(String path) throws IOException {
            try (InputStream input = SkinPainter.class.getResourceAsStream(path)) {
                if (input == null) throw new IOException(path);
                BufferedImage image = ImageIO.read(input);
                if (image == null) throw new IOException(path);
                return image;
            }
        }

        void sample(float u, float v, int wrapU, int wrapV, float[] out) {
            float x = u * width - 0.5f;
            float y = v * height - 0.5f;
            int x0 = (int) Math.floor(x);
            int y0 = (int) Math.floor(y);
            float fx = x - x0;
            float fy = y - y0;
            int xa = address(x0, width, wrapU);
            int xb = address(x0 + 1, width, wrapU);
            int ya = address(y0, height, wrapV);
            int yb = address(y0 + 1, height, wrapV);
            int i00 = (ya * width + xa) * count;
            int i10 = (ya * width + xb) * count;
            int i01 = (yb * width + xa) * count;
            int i11 = (yb * width + xb) * count;
            for (int c = 0; c < count; c++) {
                float top = data[i00 + c] + (data[i10 + c] - data[i00 + c]) * fx;
                float bottom = data[i01 + c] + (data[i11 + c] - data[i01 + c]) * fx;
                out[c] = top + (bottom - top) * fy;
            }
        }

        private static int address(int coordinate, int size, int mode) {
            if (mode == WRAP) return Math.floorMod(coordinate, size);
            if (mode == MIRROR) {
                int period = Math.floorMod(coordinate, size * 2);
                return period >= size ? size * 2 - 1 - period : period;
            }
            return Math.clamp(coordinate, 0, size - 1);
        }
    }

    static final class UniformRandom {
        private static final int NTAB = 32;
        private static final int IA = 16807;
        private static final int IM = 2147483647;
        private static final int IQ = 127773;
        private static final int IR = 2836;
        private static final int NDIV = 1 + (IM - 1) / NTAB;
        private static final double AM = 1.0 / IM;
        private static final double RNMX = 1.0 - 1.2e-7;
        private final int[] table = new int[NTAB];
        private int idum;
        private int iy;

        UniformRandom(int seed) {
            idum = seed >= 0 ? -seed : seed;
        }

        private int next() {
            if (idum <= 0 || iy == 0) {
                idum = -idum < 1 ? 1 : -idum;
                for (int j = NTAB + 7; j >= 0; j--) {
                    idum = step(idum);
                    if (j < NTAB) table[j] = idum;
                }
                iy = table[0];
            }
            idum = step(idum);
            int j = iy / NDIV;
            iy = table[j];
            table[j] = idum;
            return iy;
        }

        private static int step(int value) {
            int k = value / IQ;
            long next = (long) IA * (value - k * IQ) - (long) IR * k;
            if (next < 0) next += IM;
            return (int) next;
        }

        float uniform(float low, float high) {
            double value = AM * next();
            if (value > RNMX) value = RNMX;
            return (float) (value * (high - low) + low);
        }
    }
}
