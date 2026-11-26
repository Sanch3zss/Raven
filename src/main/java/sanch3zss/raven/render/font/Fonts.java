package sanch3zss.raven.render.font;

import java.util.function.Supplier;

import com.google.common.base.Suppliers;

public class Fonts {
    public static final Supplier<MsdfFont> INTER_SEMIBOLD = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("inter-semi").data("inter-semi").build();
    });
    public static final Supplier<MsdfFont> INTER_BOLD = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("inter-bold").data("inter-bold").build();
    });
    public static final Supplier<MsdfFont> INTER_MEDIUM = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("inter-medium").data("inter-medium").build();
    });
    public static final Supplier<MsdfFont> INTER_EXTRA_BOLD = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("inter-extrabold").data("inter-extrabold").build();
    });
    public static final Supplier<MsdfFont> ICONS = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("icons").data("icons").build();
    });
    public static final Supplier<MsdfFont> ETC = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("etc").data("etc").build();
    });
    public static final Supplier<MsdfFont> CLICK_GUI = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("clickgui").data("clickgui").build();
    });
    public static final Supplier<MsdfFont> CATEGORIES = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("categories").data("categories").build();
    });
    public static final Supplier<MsdfFont> CATEGORIES2 = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("categories2").data("categories2").build();
    });
    public static final Supplier<MsdfFont> ALPHASMP = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("alphasmp").data("alphasmp").build();
    });
    public static final Supplier<MsdfFont> WAYPOINTS = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("waypoints").data("waypoints").build();
    });
    public static final Supplier<MsdfFont> CLIENT_ICON = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("icon").data("icon").build();
    });
    public static final Supplier<MsdfFont> NUR_ALPHA = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("nur-alpha").data("nur-alpha").build();
    });
    public static final Supplier<MsdfFont> ZENITH_MEDIUM = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("zenith-medium").data("zenith-medium").build();
    });
    public static final Supplier<MsdfFont> ZENITH_SEMIBOLD = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("zenith-semibold").data("zenith-semibold").build();
    });
    public static final Supplier<MsdfFont> MEDIA_PLAYER = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("mediaplayer").data("mediaplayer").build();
    });
    public static final Supplier<MsdfFont> MAP = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("map").data("map").build();
    });
    public static final Supplier<MsdfFont> WATERMARK = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("watermark").data("watermark").build();
    });
    public static final Supplier<MsdfFont> PLAYER = Suppliers.memoize(() -> {
        return MsdfFont.builder().atlas("player").data("player").build();
    });
}
