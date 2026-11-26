package sanch3zss.raven.render.font;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import sanch3zss.raven.client.config.SpriteRenderConfig;
import sanch3zss.raven.render.font.*;

public class FontData {

    @SerializedName("atlas")
    public SpriteRenderConfig atlas;

    @SerializedName("metrics")
    public FontMetrics metrics;

    @SerializedName("glyphs")
    public List<GlyphData> glyphs;

    @SerializedName("kerning")
    public List<KerningPair> kernings;

    public SpriteRenderConfig atlas() {
        return this.atlas;
    }

    public FontMetrics metrics() {
        return this.metrics;
    }

    public List<GlyphData> glyphs() {
        return this.glyphs;
    }

    public List<KerningPair> kernings() {
        return this.kernings;
    }
}
