package sanch3zss.raven.feature.module.render.enchantcolor;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.enchantcolor.CustomEnchantColorModule;
import sanch3zss.raven.ui.setting.ColorSetting;

public enum CustomEnchantColorRenderer implements SimpleSynchronousResourceReloadListener {
    INSTANCE;

    private static final Identifier ITEM_TEXTURE = Identifier.fromNamespaceAndPath("raven", "dynamic/enchanted_glint_item");
    private static final Identifier ARMOR_TEXTURE = Identifier.fromNamespaceAndPath("raven", "dynamic/enchanted_glint_armor");
    private static final Identifier GRADIENT_TEXTURE = Identifier.fromNamespaceAndPath("raven", "dynamic/enchanted_glint_gradient");
    private static final Identifier STYLED_SHADER = Identifier.fromNamespaceAndPath("raven", "core/glint/styled");
    private static final double TAU = Math.PI * 2.0;
    private final Map<GlintStyle, RenderType[]> styledTypes = new EnumMap<>(GlintStyle.class);
    private DynamicTexture gradientTexture;
    private GlintStyle style = GlintStyle.VANILLA;

    private NativeImage itemSource;
    private NativeImage armorSource;
    private DynamicTexture itemTexture;
    private DynamicTexture armorTexture;
    private RenderType itemGlint;
    private RenderType translucentGlint;
    private RenderType entityGlint;
    private RenderType armorGlint;
    private int firstColor;
    private int secondColor;
    private volatile boolean reloadRequested = true;
    private long retryAt;

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath("raven", "custom_enchant_color");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        this.reloadRequested = true;
    }

    public RenderType itemGlint(RenderType original) {
        if (!ready()) return original;
        return this.style == GlintStyle.VANILLA ? this.itemGlint : styled(this.style)[0];
    }

    public RenderType translucentGlint(RenderType original) {
        if (!ready()) return original;
        return this.style == GlintStyle.VANILLA ? this.translucentGlint : styled(this.style)[1];
    }

    public RenderType entityGlint(RenderType original) {
        if (!ready()) return original;
        return this.style == GlintStyle.VANILLA ? this.entityGlint : styled(this.style)[2];
    }

    public RenderType armorGlint(RenderType original) {
        if (!ready()) return original;
        return this.style == GlintStyle.VANILLA ? this.armorGlint : styled(this.style)[3];
    }

    private RenderType[] styled(GlintStyle style) {
        return this.styledTypes.computeIfAbsent(style, this::createStyledTypes);
    }

    private RenderType[] createStyledTypes(GlintStyle style) {
        String name = style.name().toLowerCase(Locale.ROOT);
        RenderPipeline atlasPipeline = styledPipeline(style, name + "_item", 0.0f, 0.0f);
        RenderPipeline entityPipeline = styledPipeline(style, name + "_entity", 64.0f, 64.0f);
        RenderPipeline armorPipeline = styledPipeline(style, name + "_armor", 64.0f, 32.0f);
        return new RenderType[] {
                RenderType.create("raven_glint_" + name, RenderSetup.builder(atlasPipeline)
                        .withTexture("Sampler0", GRADIENT_TEXTURE)
                        .withTexture("Sampler1", TextureAtlas.LOCATION_ITEMS)
                        .setTextureTransform(TextureTransform.GLINT_TEXTURING)
                        .createRenderSetup()),
                RenderType.create("raven_glint_translucent_" + name, RenderSetup.builder(atlasPipeline)
                        .withTexture("Sampler0", GRADIENT_TEXTURE)
                        .withTexture("Sampler1", TextureAtlas.LOCATION_ITEMS)
                        .setTextureTransform(TextureTransform.GLINT_TEXTURING)
                        .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                        .createRenderSetup()),
                RenderType.create("raven_entity_glint_" + name, RenderSetup.builder(entityPipeline)
                        .withTexture("Sampler0", GRADIENT_TEXTURE)
                        .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
                        .createRenderSetup()),
                RenderType.create("raven_armor_entity_glint_" + name, RenderSetup.builder(armorPipeline)
                        .withTexture("Sampler0", GRADIENT_TEXTURE)
                        .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .createRenderSetup())
        };
    }

    private static RenderPipeline styledPipeline(GlintStyle style, String name, float gridX, float gridY) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION)
                .withBindGroupLayout(BindGroupLayouts.FOG)
                .withLocation(Identifier.fromNamespaceAndPath("raven", "pipeline/glint_" + name))
                .withVertexShader(STYLED_SHADER)
                .withFragmentShader(STYLED_SHADER)
                .withShaderDefine("SPEED", 1.0f)
                .withShaderDefine("STRENGTH", 1.0f)
                .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
                .withCull(false)
                .withColorTargetState(new ColorTargetState(BlendFunction.GLINT))
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                .withPrimitiveTopology(PrimitiveTopology.QUADS)
                .withDepthStencilState(new DepthStencilState(CompareOp.EQUAL, false));
        if (style == GlintStyle.PIXEL) {
            builder.withShaderDefine("PIXEL");
        }
        if (gridX <= 0.0f) {
            builder.withShaderDefine("ATLAS_GRID").withBindGroupLayout(BindGroupLayouts.SAMPLER1);
        } else {
            builder.withShaderDefine("GRID_X", gridX).withShaderDefine("GRID_Y", gridY);
        }
        return RenderPipelines.register(builder.build());
    }

    private boolean ready() {
        Raven raven = Raven.INSTANCE;
        if (raven == null || raven.moduleRepository() == null) return false;
        CustomEnchantColorModule module = raven.moduleRepository().get(CustomEnchantColorModule.class);
        if (!module.isState()) return false;
        this.style = module.style.currentValue;
        int first = module.color.getFirstColor();
        int second = module.color.getGradientEndColor();
        if (this.reloadRequested) {
            if (System.currentTimeMillis() < this.retryAt) return false;
            if (!reload(module.color, first, second)) return false;
        } else if (first != this.firstColor || second != this.secondColor) {
            updateColors(module.color, first, second);
        }
        return this.itemTexture != null && this.armorTexture != null && this.gradientTexture != null;
    }

    private boolean reload(ColorSetting color, int first, int second) {
        NativeImage nextItemSource = null;
        NativeImage nextArmorSource = null;
        DynamicTexture nextItemTexture = null;
        DynamicTexture nextArmorTexture = null;
        try {
            ResourceManager resources = Minecraft.getInstance().getResourceManager();
            nextItemSource = read(resources, ItemFeatureRenderer.ENCHANTED_GLINT_ITEM);
            nextArmorSource = read(resources, ItemFeatureRenderer.ENCHANTED_GLINT_ARMOR);
            int[] gradient = gradient(color);
            nextItemTexture = new LinearDynamicTexture("Raven item glint", recolor(nextItemSource, gradient));
            nextArmorTexture = new LinearDynamicTexture("Raven armor glint", recolor(nextArmorSource, gradient));
            Minecraft.getInstance().getTextureManager().register(ITEM_TEXTURE, nextItemTexture);
            Minecraft.getInstance().getTextureManager().register(ARMOR_TEXTURE, nextArmorTexture);
            NativeImage strip = new NativeImage(gradient.length, 1, false);
            fillGradient(strip, gradient);
            this.gradientTexture = new LinearDynamicTexture("Raven glint gradient", strip);
            Minecraft.getInstance().getTextureManager().register(GRADIENT_TEXTURE, this.gradientTexture);
            if (this.itemSource != null) this.itemSource.close();
            if (this.armorSource != null) this.armorSource.close();
            this.itemSource = nextItemSource;
            this.armorSource = nextArmorSource;
            this.itemTexture = nextItemTexture;
            this.armorTexture = nextArmorTexture;
            this.firstColor = first;
            this.secondColor = second;
            this.reloadRequested = false;
            createRenderTypes();
            return true;
        } catch (Exception error) {
            if (nextItemSource != null) nextItemSource.close();
            if (nextArmorSource != null) nextArmorSource.close();
            if (nextItemTexture != null) nextItemTexture.close();
            if (nextArmorTexture != null) nextArmorTexture.close();
            this.retryAt = System.currentTimeMillis() + 5000L;
            Raven.LOGGER.warn("Unable to prepare custom enchantment glint", error);
            return false;
        }
    }

    private void updateColors(ColorSetting color, int first, int second) {
        int[] gradient = gradient(color);
        fill(this.itemTexture.getPixels(), this.itemSource, gradient);
        fill(this.armorTexture.getPixels(), this.armorSource, gradient);
        fillGradient(this.gradientTexture.getPixels(), gradient);
        this.itemTexture.upload();
        this.armorTexture.upload();
        this.gradientTexture.upload();
        this.firstColor = first;
        this.secondColor = second;
    }

    private static NativeImage read(ResourceManager resources, Identifier identifier) throws IOException {
        try (InputStream input = resources.getResourceOrThrow(identifier).open()) {
            return NativeImage.read(input);
        }
    }

    private static int[] gradient(ColorSetting color) {
        int[] colors = new int[256];
        for (int index = 0; index < colors.length; index++) {
            colors[index] = color.getGradientColor(index / 255.0f);
        }
        return colors;
    }

    private static void fillGradient(NativeImage target, int[] gradient) {
        for (int x = 0; x < gradient.length; x++) {
            int color = gradient[x];
            float alpha = ((color >>> 24) & 0xFF) / 255.0f;
            int red = Math.round(((color >> 16) & 0xFF) * alpha);
            int green = Math.round(((color >> 8) & 0xFF) * alpha);
            int blue = Math.round((color & 0xFF) * alpha);
            target.setPixel(x, 0, 0xFF000000 | (red << 16) | (green << 8) | blue);
        }
    }

    private static NativeImage recolor(NativeImage source, int[] gradient) {
        NativeImage image = new NativeImage(source.getWidth(), source.getHeight(), false);
        fill(image, source, gradient);
        return image;
    }

    private static void fill(NativeImage target, NativeImage source, int[] gradient) {
        int width = source.getWidth();
        int height = source.getHeight();
        for (int y = 0; y < height; y++) {
            double v = (y + 0.5) / height;
            for (int x = 0; x < width; x++) {
                double u = (x + 0.5) / width;
                double wave = 0.5 + 0.36 * Math.sin(TAU * (u - v))
                        + 0.14 * Math.sin(TAU * (u + v));
                int color = gradient[Math.clamp((int) Math.round(wave * 255.0), 0, 255)];
                int sourcePixel = source.getPixel(x, y);
                int brightness = Math.max((sourcePixel >> 16) & 0xFF,
                        Math.max((sourcePixel >> 8) & 0xFF, sourcePixel & 0xFF));
                float strength = brightness / 255.0f * ((color >>> 24) & 0xFF) / 255.0f;
                int red = Math.round(((color >> 16) & 0xFF) * strength);
                int green = Math.round(((color >> 8) & 0xFF) * strength);
                int blue = Math.round((color & 0xFF) * strength);
                target.setPixel(x, y, (sourcePixel & 0xFF000000) | (red << 16) | (green << 8) | blue);
            }
        }
    }

    private void createRenderTypes() {
        if (this.itemGlint != null) return;
        this.itemGlint = RenderType.create("raven_glint", RenderSetup.builder(RenderPipelines.GLINT)
                .withTexture("Sampler0", ITEM_TEXTURE)
                .setTextureTransform(TextureTransform.GLINT_TEXTURING)
                .createRenderSetup());
        this.translucentGlint = RenderType.create("raven_glint_translucent", RenderSetup.builder(RenderPipelines.GLINT)
                .withTexture("Sampler0", ITEM_TEXTURE)
                .setTextureTransform(TextureTransform.GLINT_TEXTURING)
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .createRenderSetup());
        this.entityGlint = RenderType.create("raven_entity_glint", RenderSetup.builder(RenderPipelines.GLINT)
                .withTexture("Sampler0", ITEM_TEXTURE)
                .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
                .createRenderSetup());
        this.armorGlint = RenderType.create("raven_armor_entity_glint", RenderSetup.builder(RenderPipelines.GLINT)
                .withTexture("Sampler0", ARMOR_TEXTURE)
                .setTextureTransform(TextureTransform.ARMOR_ENTITY_GLINT_TEXTURING)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .createRenderSetup());
    }

    private static final class LinearDynamicTexture extends DynamicTexture {
        private LinearDynamicTexture(String label, NativeImage image) {
            super(() -> label, image);
            this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
        }
    }
}
