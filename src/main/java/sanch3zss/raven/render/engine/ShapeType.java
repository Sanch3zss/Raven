package sanch3zss.raven.render.engine;

public enum ShapeType {
    COLOR(0),
    TEXTURE(1),
    ROUNDED_RECTANGLE(2),
    ROUNDED_TEXTURE(3),
    BLUR(4),
    CHECKER(5),
    CIRCLE(6),
    OUTER_MASK(7),
    ALPHA_MASK(8),
    MSDF_FONT(9),
    RADIAL_ROUNDED_RECTANGLE(10),
    SHADOW(11),
    HORIZONTAL_GRADIENT(12),
    GLASS_PANEL(13),
    PIXEL_TEXTURE(14),
    GLASS_RING(15),
    GLYPH_TEXTURE(16),
    CHEVRON(17),
    COLOR_WELL(18),
    SATURATION_VALUE(19),
    SKIN_FACE(20);

    public final int mode;

    public int mode() {
        return this.mode;
    }

    ShapeType(int i) {
        this.mode = i;
    }
}
