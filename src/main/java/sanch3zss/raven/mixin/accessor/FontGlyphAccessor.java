package sanch3zss.raven.mixin.accessor;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Font.class)
public interface FontGlyphAccessor {
    @Invoker("getGlyph")
    BakedGlyph raven$getGlyph(int codepoint, Style style);
}
