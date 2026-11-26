package sanch3zss.raven.client.font;

import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.TrueTypeGlyphProvider;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.providers.BitmapProvider;
import net.minecraft.client.gui.font.providers.UnihexProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.ui.notification.NotificationType;

public final class CustomFonts {
    public static final CustomFonts INSTANCE = new CustomFonts();
    private static final long APPLY_DELAY_MS = 200L;
    private static final int TEXT_SHEET_GLYPHS = 16;
    private static final Set<Identifier> SYMBOL_FONTS = Set.of(
            Identifier.withDefaultNamespace("alt"), Identifier.withDefaultNamespace("illageralt"));

    private @Nullable CustomFontProvider provider;
    private String appliedKey = "";
    private String pendingKey = "";
    private @Nullable String failedKey;
    private long pendingSince;

    private CustomFonts() {
    }

    public void update(boolean enabled, String path, float scale) {
        int guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        String key = enabled && !path.isEmpty() ? path + "|" + scale + "|" + guiScale : "";
        if (key.equals(this.appliedKey) || key.equals(this.failedKey)) {
            this.pendingKey = key;
            return;
        }
        long now = System.currentTimeMillis();
        if (!key.equals(this.pendingKey)) {
            this.pendingKey = key;
            this.pendingSince = now;
        }
        if (now - this.pendingSince >= APPLY_DELAY_MS) {
            apply(key, path, scale, guiScale);
        }
    }

    private void apply(String key, String path, float scale, int guiScale) {
        CustomFontProvider loaded = null;
        if (!key.isEmpty()) {
            try {
                loaded = CustomFontProvider.load(Path.of(path), scale, guiScale);
            } catch (Exception e) {
                Raven.LOGGER.error("Failed to load custom font {}", path, e);
                this.failedKey = key;
                Raven.INSTANCE.notificationRepository().post(NotificationType.ERROR,
                        Component.literal("Не удалось загрузить шрифт."), 4000L);
                return;
            }
        }
        CustomFontProvider previous = this.provider;
        this.provider = loaded;
        this.appliedKey = key;
        this.failedKey = null;
        install(Minecraft.getInstance().fontManager);
        if (previous != null) {
            previous.close();
        }
    }

    public void reinstall(FontManager fontManager) {
        if (this.provider != null) {
            install(fontManager);
        }
    }

    private void install(FontManager fontManager) {
        Set<FontOption> options = FontManager.getFontOptions(Minecraft.getInstance().options);
        fontManager.fontSets.forEach((id, fontSet) -> {
            List<GlyphProvider.Conditional> original = new ArrayList<>();
            for (GlyphProvider.Conditional conditional : fontSet.allProviders) {
                if (!(conditional.provider() instanceof CustomFontProvider)
                        && !(conditional.provider() instanceof CustomFontProvider.TextGlyphs)) {
                    original.add(conditional);
                }
            }
            boolean installed = original.size() != fontSet.allProviders.size();
            boolean replace = this.provider != null && !SYMBOL_FONTS.contains(id)
                    && !id.getNamespace().equals("raven");
            if (!replace && !installed) {
                return;
            }
            List<GlyphProvider.Conditional> providers = new ArrayList<>();
            if (replace) {
                CustomFontProvider custom = Objects.requireNonNull(this.provider);
                providers.add(new GlyphProvider.Conditional(
                        id.equals(Minecraft.DEFAULT_FONT) ? custom : custom.textGlyphs(pictureGlyphs(original)),
                        FontOption.Filter.ALWAYS_PASS));
            }
            providers.addAll(original);
            fontSet.reload(providers, options);
        });
        fontManager.anyGlyphs.invalidate();
        fontManager.nonFishyGlyphs.invalidate();
    }

    private static IntSet pictureGlyphs(List<GlyphProvider.Conditional> providers) {
        IntSet claimed = new IntOpenHashSet();
        IntSet pictures = new IntOpenHashSet();
        for (GlyphProvider.Conditional conditional : providers) {
            GlyphProvider provider = conditional.provider();
            IntSet glyphs = provider.getSupportedGlyphs();
            boolean text = provider instanceof TrueTypeGlyphProvider || provider instanceof UnihexProvider
                    || provider instanceof BitmapProvider && glyphs.size() >= TEXT_SHEET_GLYPHS;
            for (int codepoint : glyphs) {
                if (claimed.add(codepoint) && !text) {
                    pictures.add(codepoint);
                }
            }
        }
        return pictures;
    }
}
