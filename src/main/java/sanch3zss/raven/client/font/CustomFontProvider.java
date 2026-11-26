package sanch3zss.raven.client.font;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.UnbakedGlyph;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Objects;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Glyph_Metrics;
import org.lwjgl.util.freetype.FreeType;

public final class CustomFontProvider implements GlyphProvider {
    private static final int LOAD_FLAGS = FreeType.FT_FT_LOAD_TARGET_LIGHT | FreeType.FT_LOAD_NO_BITMAP;
    private static final int DETAIL_LOAD_FLAGS = FreeType.FT_LOAD_NO_HINTING | FreeType.FT_LOAD_NO_BITMAP
            | FreeType.FT_LOAD_RENDER;
    private static final int PADDING = 1;
    private static final float CAP_HEIGHT = 7.0f;
    private static final float WORLD_DENSITY = 8.0f;
    private static final String SMALL_CAPITAL = "LATIN LETTER SMALL CAPITAL ";
    private static final int[] LOOKALIKES = {
            0x01EB, 'Q', 0x0405, 'S', 0x0455, 'S', 0x0492, 'F', 0x0493, 'F', 0x20A3, 'F'};
    private static int generation;

    private final ByteBuffer fontData;
    private final FT_Face face;
    private final FT_Face detailFace;
    private final float oversample;
    private final int detail;
    private final float boldOffset;
    private final CustomFontAtlas atlas;
    private final IntSet supportedGlyphs = new IntOpenHashSet();
    private final Int2ObjectMap<UnbakedGlyph> glyphs = new Int2ObjectOpenHashMap<>();
    private final IntSet missingGlyphs = new IntOpenHashSet();
    private final Int2ObjectMap<BakedGlyph> bakedGlyphs = new Int2ObjectOpenHashMap<>();
    private boolean closed;

    private CustomFontProvider(ByteBuffer fontData, FT_Face face, FT_Face detailFace, float oversample, int detail) {
        this.fontData = fontData;
        this.face = face;
        this.detailFace = detailFace;
        this.oversample = oversample;
        this.detail = detail;
        this.boldOffset = Math.max(1, Math.round(oversample * 0.5f)) / oversample;
        this.atlas = new CustomFontAtlas("custom_font/" + generation++, detail);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer index = stack.mallocInt(1);
            long codepoint = FreeType.FT_Get_First_Char(face, index);
            while (index.get(0) != 0) {
                this.supportedGlyphs.add((int) codepoint);
                codepoint = FreeType.FT_Get_Next_Char(face, codepoint, index);
            }
        }
    }

    public static CustomFontProvider load(Path path, float scale, float oversample) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        ByteBuffer fontData = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
        FT_Face face = null;
        FT_Face detailFace = null;
        try {
            face = openFace(fontData);
            detailFace = openFace(fontData);
            float unitsPerEm = Short.toUnsignedInt(face.units_per_EM());
            float emPixels = CAP_HEIGHT * scale * unitsPerEm / capHeight(face, unitsPerEm) * oversample;
            int detail = Math.max(1, Integer.highestOneBit((int) Math.ceil(WORLD_DENSITY / oversample) - 1) << 1);
            FreeTypeUtil.assertError(FreeType.FT_Set_Char_Size(face, 0L, Math.round(emPixels * 64.0f), 72, 72),
                    "Setting font size");
            FreeTypeUtil.assertError(FreeType.FT_Set_Char_Size(detailFace, 0L, Math.round(emPixels * detail * 64.0f),
                    72, 72), "Setting detail font size");
            return new CustomFontProvider(fontData, face, detailFace, oversample, detail);
        } catch (RuntimeException e) {
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                if (face != null) {
                    FreeType.FT_Done_Face(face);
                }
                if (detailFace != null) {
                    FreeType.FT_Done_Face(detailFace);
                }
            }
            MemoryUtil.memFree(fontData);
            throw new IOException("Unable to load font " + path, e);
        }
    }

    private static FT_Face openFace(ByteBuffer fontData) {
        FT_Face face;
        synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer pointer = stack.mallocPointer(1);
                FreeTypeUtil.assertError(FreeType.FT_New_Memory_Face(FreeTypeUtil.getLibrary(), fontData, 0L, pointer),
                        "Initializing font face");
                face = FT_Face.create(pointer.get());
            }
        }
        FreeTypeUtil.assertError(FreeType.FT_Select_Charmap(face, FreeType.FT_ENCODING_UNICODE), "Find unicode charmap");
        return face;
    }

    private static float capHeight(FT_Face face, float unitsPerEm) {
        int index = FreeType.FT_Get_Char_Index(face, 'H');
        if (index != 0 && FreeType.FT_Load_Glyph(face, index, FreeType.FT_LOAD_NO_SCALE) == 0) {
            long height = Objects.requireNonNull(face.glyph()).metrics().height();
            if (height > 0) {
                return height;
            }
        }
        return unitsPerEm * 0.7f;
    }

    private static int lookalike(int codepoint) {
        String name = Character.getName(codepoint);
        if (name != null && name.startsWith(SMALL_CAPITAL) && name.length() == SMALL_CAPITAL.length() + 1) {
            return name.charAt(SMALL_CAPITAL.length());
        }
        for (int i = 0; i < LOOKALIKES.length; i += 2) {
            if (LOOKALIKES[i] == codepoint) {
                return LOOKALIKES[i + 1];
            }
        }
        return -1;
    }

    private static boolean isText(int codepoint) {
        return codepoint < 0x80 ? codepoint >= 0x20 : Character.isLetterOrDigit(codepoint);
    }

    public GlyphProvider textGlyphs(IntSet kept) {
        return new TextGlyphs(kept);
    }

    @Override
    public synchronized @Nullable UnbakedGlyph getGlyph(int codepoint) {
        if (this.closed || this.missingGlyphs.contains(codepoint)) {
            return null;
        }
        UnbakedGlyph glyph = this.glyphs.get(codepoint);
        if (glyph == null) {
            int source = resolve(codepoint);
            glyph = source < 0 ? null : loadGlyph(source);
            if (glyph == null) {
                this.missingGlyphs.add(codepoint);
            } else {
                this.glyphs.put(codepoint, glyph);
            }
        }
        return glyph;
    }

    private int resolve(int codepoint) {
        int substitute = lookalike(codepoint);
        if (substitute >= 0 && this.supportedGlyphs.contains(substitute)) {
            return substitute;
        }
        if (this.supportedGlyphs.contains(codepoint)) {
            return codepoint;
        }
        String normalized = Normalizer.normalize(Character.toString(codepoint), Normalizer.Form.NFKC);
        if (normalized.codePointCount(0, normalized.length()) == 1) {
            int plain = normalized.codePointAt(0);
            if (plain != codepoint && this.supportedGlyphs.contains(plain)) {
                return plain;
            }
        }
        return -1;
    }

    private @Nullable UnbakedGlyph loadGlyph(int codepoint) {
        int index = FreeType.FT_Get_Char_Index(this.face, codepoint);
        if (index == 0 || FreeType.FT_Load_Glyph(this.face, index, LOAD_FLAGS) != 0) {
            return null;
        }
        FT_GlyphSlot slot = Objects.requireNonNull(this.face.glyph());
        float advance = Math.round(slot.advance().x() / 64.0f) / this.oversample;
        FT_Glyph_Metrics metrics = slot.metrics();
        if (metrics.width() <= 0 || metrics.height() <= 0) {
            return new EmptyGlyph(advance);
        }
        return new Glyph(index, new GlyphInfo() {
            @Override
            public float getAdvance() {
                return advance;
            }

            @Override
            public float getBoldOffset() {
                return CustomFontProvider.this.boldOffset;
            }
        });
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return this.supportedGlyphs;
    }

    @Override
    public synchronized void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.atlas.close();
        this.glyphs.clear();
        this.bakedGlyphs.clear();
        synchronized (FreeTypeUtil.LIBRARY_LOCK) {
            FreeType.FT_Done_Face(this.face);
            FreeType.FT_Done_Face(this.detailFace);
        }
        MemoryUtil.memFree(this.fontData);
    }

    private synchronized BakedGlyph bake(int index, GlyphInfo info, UnbakedGlyph.Stitcher fallback) {
        BakedGlyph baked = this.bakedGlyphs.get(index);
        if (baked != null) {
            return baked;
        }
        if (this.closed) {
            return fallback.getMissing();
        }
        CustomFontAtlas.GlyphRaster raster = rasterize(index);
        if (raster == null) {
            return new EmptyGlyph(info.getAdvance()).bake(fallback);
        }
        baked = Objects.requireNonNullElse(this.atlas.add(info, raster, this.oversample), fallback.getMissing());
        this.bakedGlyphs.put(index, baked);
        return baked;
    }

    private CustomFontAtlas.@Nullable GlyphRaster rasterize(int index) {
        if (FreeType.FT_Load_Glyph(this.face, index, LOAD_FLAGS | FreeType.FT_LOAD_RENDER) != 0) {
            return null;
        }
        FT_GlyphSlot slot = Objects.requireNonNull(this.face.glyph());
        FT_Bitmap bitmap = slot.bitmap();
        if (!isGrayBitmap(bitmap)) {
            return null;
        }
        int width = bitmap.width() + PADDING * 2;
        int height = bitmap.rows() + PADDING * 2;
        int left = slot.bitmap_left() - PADDING;
        int top = slot.bitmap_top() + PADDING;
        byte[] coverage = new byte[width * height];
        copyBitmap(bitmap, coverage, width, height, PADDING, PADDING);
        int detailWidth = width * this.detail;
        int detailHeight = height * this.detail;
        byte[] detailCoverage = new byte[detailWidth * detailHeight];
        if (FreeType.FT_Load_Glyph(this.detailFace, index, DETAIL_LOAD_FLAGS) == 0) {
            FT_GlyphSlot detailSlot = Objects.requireNonNull(this.detailFace.glyph());
            FT_Bitmap detailBitmap = detailSlot.bitmap();
            if (isGrayBitmap(detailBitmap)) {
                copyBitmap(detailBitmap, detailCoverage, detailWidth, detailHeight,
                        -left * this.detail + detailSlot.bitmap_left(), top * this.detail - detailSlot.bitmap_top());
            }
        }
        return new CustomFontAtlas.GlyphRaster(width, height, left, top, coverage, detailCoverage);
    }

    private static boolean isGrayBitmap(FT_Bitmap bitmap) {
        return bitmap.width() > 0 && bitmap.rows() > 0 && bitmap.pitch() > 0
                && bitmap.pixel_mode() == FreeType.FT_PIXEL_MODE_GRAY;
    }

    private static void copyBitmap(FT_Bitmap bitmap, byte[] target, int targetWidth, int targetHeight,
                                   int offsetX, int offsetY) {
        int pitch = bitmap.pitch();
        ByteBuffer source = Objects.requireNonNull(bitmap.buffer(pitch * bitmap.rows()));
        for (int y = 0; y < bitmap.rows(); y++) {
            int targetY = y + offsetY;
            if (targetY < 0 || targetY >= targetHeight) {
                continue;
            }
            for (int x = 0; x < bitmap.width(); x++) {
                int targetX = x + offsetX;
                if (targetX >= 0 && targetX < targetWidth) {
                    target[targetY * targetWidth + targetX] = source.get(y * pitch + x);
                }
            }
        }
    }

    private final class Glyph implements UnbakedGlyph {
        private final int index;
        private final GlyphInfo info;

        private Glyph(int index, GlyphInfo info) {
            this.index = index;
            this.info = info;
        }

        @Override
        public GlyphInfo info() {
            return this.info;
        }

        @Override
        public BakedGlyph bake(UnbakedGlyph.Stitcher fallback) {
            return CustomFontProvider.this.bake(this.index, this.info, fallback);
        }
    }

    public final class TextGlyphs implements GlyphProvider {
        private final IntSet supported = new IntOpenHashSet();
        private final IntSet kept;

        private TextGlyphs(IntSet kept) {
            this.kept = kept;
            for (int codepoint : CustomFontProvider.this.supportedGlyphs) {
                if (isText(codepoint) && !kept.contains(codepoint)) {
                    this.supported.add(codepoint);
                }
            }
        }

        @Override
        public @Nullable UnbakedGlyph getGlyph(int codepoint) {
            return isText(codepoint) && !this.kept.contains(codepoint) ? CustomFontProvider.this.getGlyph(codepoint) : null;
        }

        @Override
        public IntSet getSupportedGlyphs() {
            return this.supported;
        }
    }
}
