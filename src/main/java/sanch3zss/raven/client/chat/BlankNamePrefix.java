package sanch3zss.raven.client.chat;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import sanch3zss.raven.mixin.accessor.FontGlyphAccessor;

public final class BlankNamePrefix {
    private BlankNamePrefix() {
    }

    public static MutableComponent strip(MutableComponent name) {
        Font font = Minecraft.getInstance().font;
        boolean startsBlank = name.visit((style, text) -> text.isEmpty()
                ? Optional.<Boolean>empty()
                : Optional.of(isBlank(font, text.codePointAt(0), style)), Style.EMPTY).orElse(false);
        if (!startsBlank) {
            return name;
        }
        MutableComponent result = Component.empty();
        boolean[] stripping = {true};
        name.visit((style, text) -> {
            int start = 0;
            while (stripping[0] && start < text.length()) {
                int codepoint = text.codePointAt(start);
                if (!isBlank(font, codepoint, style)) {
                    stripping[0] = false;
                    break;
                }
                start += Character.charCount(codepoint);
            }
            if (start < text.length()) {
                result.append(Component.literal(text.substring(start)).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return result;
    }

    private static boolean isBlank(Font font, int codepoint, Style style) {
        return ((FontGlyphAccessor) font).raven$getGlyph(codepoint, style)
                .createGlyph(0.0f, 0.0f, -1, 0, style, 0.0f, 0.0f) == null;
    }
}
