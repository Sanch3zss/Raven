package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import sanch3zss.raven.client.Raven;

final class PaintedSkins {
    static final PaintedSkins INSTANCE = new PaintedSkins();
    private static final int KEPT = 8;
    private static final long UNUSED_AFTER = 2_000_000_000L;
    private final SkinPainter painter = new SkinPainter(SkinCatalog.get());
    private final GlovePainter glovePainter = new GlovePainter(GloveCatalog.get());
    private final Map<String, Identifier> ready = new LinkedHashMap<>(16, 0.75f, true);
    private final Map<String, Identifier> shown = new HashMap<>();
    private final Map<Identifier, Long> lastUsed = new HashMap<>();
    private final Set<String> pending = new HashSet<>();
    private final Set<String> failed = new HashSet<>();

    boolean supports(KnifeVariant variant) {
        return SkinCatalog.get().knife(variant.model) != null;
    }

    Identifier texture(String model, KnifeSkin skin, int seed, float wear) {
        String key = model + "/" + skin.name().toLowerCase(Locale.ROOT);
        if (skin != KnifeSkin.DEFAULT) {
            Identifier texture = request(model, key + "/" + seed + "_" + Math.round(wear * 1000.0f),
                    () -> painter.paint(model, skin, seed, wear));
            return texture != null ? texture : texture(model, KnifeSkin.DEFAULT, 0, 0.0f);
        }
        Identifier texture = ready.get(key);
        if (texture != null || failed.contains(key)) return used(texture);
        try {
            return used(register(key, painter.paint(model, skin, 0, 0.0f)));
        } catch (Exception exception) {
            failed.add(key);
            Raven.LOGGER.error("Failed to paint knife {}", key, exception);
            return null;
        }
    }

    Identifier glove(GloveSkin skin, int seed, float wear) {
        String key = "glove/" + skin.name().toLowerCase(Locale.ROOT) + "/" + seed + "_" + Math.round(wear * 1000.0f);
        return request("glove/" + skin.type.model, key, () -> glovePainter.paint(skin, seed, wear));
    }

    private Identifier request(String slot, String key, Painting painting) {
        Identifier texture = ready.get(key);
        if (texture != null) {
            shown.put(slot, texture);
            return used(texture);
        }
        if (!failed.contains(key) && pending.add(slot)) {
            CompletableFuture.supplyAsync(() -> {
                try {
                    return painting.paint();
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
            }, Util.backgroundExecutor()).whenCompleteAsync((pixels, error) -> {
                pending.remove(slot);
                if (error != null) {
                    failed.add(key);
                    Raven.LOGGER.error("Failed to paint {}", key, error);
                } else {
                    shown.put(slot, register(key, pixels));
                }
            }, Minecraft.getInstance());
        }
        return used(shown.get(slot));
    }

    private Identifier used(Identifier texture) {
        if (texture != null) lastUsed.put(texture, System.nanoTime());
        return texture;
    }

    private Identifier register(String key, SkinPainter.Painted painted) {
        int width = painted.size() * 2;
        NativeImage image = new NativeImage(width, painted.size(), false);
        for (int y = 0; y < painted.size(); y++) {
            for (int x = 0; x < width; x++) image.setPixel(x, y, painted.pixels()[y * width + x]);
        }
        Identifier texture = Identifier.fromNamespaceAndPath("raven", "painted/" + key);
        PaintedTexture uploaded = new PaintedTexture(texture.toString(), image);
        image.close();
        Minecraft.getInstance().getTextureManager().register(texture, uploaded);
        ready.put(key, texture);
        lastUsed.put(texture, System.nanoTime());
        long now = System.nanoTime();
        Iterator<Map.Entry<String, Identifier>> iterator = ready.entrySet().iterator();
        while (ready.size() > KEPT && iterator.hasNext()) {
            Identifier eldest = iterator.next().getValue();
            Long seen = lastUsed.get(eldest);
            if (seen != null && now - seen < UNUSED_AFTER) continue;
            iterator.remove();
            shown.values().remove(eldest);
            lastUsed.remove(eldest);
            Minecraft.getInstance().getTextureManager().release(eldest);
        }
        return texture;
    }

    private interface Painting {
        SkinPainter.Painted paint() throws Exception;
    }
}
