package sanch3zss.raven.feature.module.render.armtweaks.knife;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;

public enum KnifeVariant implements DisplayNamed {
    BAYONET("Bayonet", "bayonet", Map.of("bayonet", "tactical")),
    BOWIE("Bowie Knife", "bowie", null),
    BUTTERFLY("Butterfly Knife", "butterfly", Map.of()),
    CLASSIC("Classic Knife", "css", Map.of("blade", "tactical")),
    CT("CT Knife", "default_ct", null),
    FALCHION("Falchion Knife", "falchion", null),
    FLIP("Flip Knife", "flip", null),
    GUT("Gut Knife", "gut", null),
    HUNTSMAN("Huntsman Knife", "tactical", Map.of()),
    KARAMBIT("Karambit", "karambit", null),
    KUKRI("Kukri Knife", "kukri", null),
    M9("M9 Bayonet", "m9", Map.of("m9", "tactical")),
    NAVAJA("Navaja Knife", "navaja", null),
    NOMAD("Nomad Knife", "outdoor", null),
    PARACORD("Paracord Knife", "cord", null),
    SHADOW_DAGGERS("Shadow Daggers", "push", Map.of()),
    SKELETON("Skeleton Knife", "skeleton", Map.of()),
    STILETTO("Stiletto", "stiletto", Map.of("stiletto", "tactical", "b", "flip_blade")),
    SURVIVAL("Survival Knife", "canis", null),
    T("T Knife", "default_t", null),
    TALON("Talon Knife", "talon", null),
    URSUS("Ursus Knife", "ursus", null);

    private final Translation displayName;
    public final String model;
    public final Map<String, String> boneAliases;

    KnifeVariant(String name, String model, Map<String, String> borrowedAnimationAliases) {
        this.displayName = Translation.clearText(name);
        this.model = model;
        this.boneAliases = borrowedAnimationAliases;
    }

    public boolean daggers() {
        return model.equals("push");
    }

    public KnifeSkin[] skins() {
        SkinCatalog.Knife knife = SkinCatalog.get().knife(model);
        if (knife == null) return new KnifeSkin[]{KnifeSkin.DEFAULT};
        List<KnifeSkin> skins = new ArrayList<>();
        skins.add(KnifeSkin.DEFAULT);
        skins.addAll(knife.skins().keySet());
        return skins.toArray(KnifeSkin[]::new);
    }

    boolean painted() {
        return PaintedSkins.INSTANCE.supports(this);
    }

    KnifeFinish finish(KnifeSkin skin) {
        return painted() ? KnifeFinish.PAINTED : finish();
    }

    KnifeFinish finish() {
        return switch (model) {
            case "default_ct", "default_t", "css" -> KnifeFinish.SATIN;
            default -> KnifeFinish.STEEL;
        };
    }

    String borrowedInspect() {
        return this == STILETTO ? "inspect_2" : null;
    }

    public boolean borrowsAnimation() {
        return boneAliases != null;
    }

    public static String migrateSavedName(String saved) {
        String name = saved;
        if (name.startsWith("REALISTIC_")) name = name.substring(10);
        else if (name.startsWith("NEW_")) name = name.substring(4);
        else if (name.startsWith("BLOCKY_")) name = name.substring(7);
        return switch (name) {
            case "BAYONET_AUTO" -> BAYONET.name();
            case "BUTTERFLY_EMERALD", "BUTTERFLY_SNOWFALL", "BUTTERFLY_DOPPLER" -> BUTTERFLY.name();
            case "M9_AMETHYST", "M9_AUTO", "M9_EMERALD", "M9_SAPPHIRE" -> M9.name();
            case "STILETTO_SAPPHIRE", "STILETTO_TT" -> STILETTO.name();
            case "SKELETON_SLAUGHTER" -> SKELETON.name();
            default -> name;
        };
    }

    @Override
    public Translation getDisplayName() {
        return displayName;
    }
}
