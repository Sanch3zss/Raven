package sanch3zss.raven.mixin.accessor;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BakedSheetGlyph.class)
public interface BakedSheetGlyphAccessor {
    @Accessor("textureView") GpuTextureView raven$getTextureView();
    @Accessor("u0") float raven$getU0();
    @Accessor("u1") float raven$getU1();
    @Accessor("v0") float raven$getV0();
    @Accessor("v1") float raven$getV1();
    @Accessor("left") float raven$getLeft();
    @Accessor("right") float raven$getRight();
    @Accessor("up") float raven$getUp();
    @Accessor("down") float raven$getDown();
}
