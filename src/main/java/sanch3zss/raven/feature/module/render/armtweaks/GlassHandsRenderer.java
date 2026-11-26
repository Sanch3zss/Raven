package sanch3zss.raven.feature.module.render.armtweaks;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;
import sanch3zss.raven.render.gl.FramebufferUtil;
import sanch3zss.raven.render.gl.GlStateSnapshot;
import sanch3zss.raven.render.shader.BlurEffect;
import sanch3zss.raven.mixin.accessor.RenderSetupAccessor;
import sanch3zss.raven.mixin.accessor.RenderSetupTextureBindingAccessor;
import sanch3zss.raven.mixin.accessor.RenderTypeAccessor;

import java.util.HashMap;
import java.util.Map;

public enum GlassHandsRenderer {
    INSTANCE;

    private static final Identifier BLURRED_SCENE = Identifier.fromNamespaceAndPath("raven", "dynamic/glass_hands_blur");
    private static final Identifier SHADER = Identifier.fromNamespaceAndPath("raven", "core/hands/glass");
    private final BlurEffect blurEffect = new BlurEffect();
    private final BorrowedRenderTargetTexture blurredScene = new BorrowedRenderTargetTexture();
    private RenderTarget sharpScene;
    private final Map<BlockPipelineKey, RenderPipeline> blockPipelines = new HashMap<>();
    private final Map<RenderTypeKey, RenderType> renderTypes = new HashMap<>();
    private final Map<RenderTypeKey, RenderType> knifeRenderTypes = new HashMap<>();
    private final Map<BlockRenderTypeKey, RenderType> blockRenderTypes = new HashMap<>();
    private boolean initialized;
    private HumanoidArm activeArm;
    private Identifier activeTexture;
    private int activeFirstColor;
    private int activeSecondColor;
    private int activeMix;
    private boolean hmiBlockActive;
    private int hmiBlockFirstColor;
    private int hmiBlockSecondColor;
    private int hmiBlockMix;
    private boolean specialItemActive;
    private boolean viewModelItemActive;

    public void captureScene(int radius) {
        ensureInitialized();
        GlStateSnapshot state = GlStateSnapshot.create(1);
        int previousFramebuffer = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        boolean previousDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDepthMask(false);
            RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            if (radius <= 0) {
                sharpScene = FramebufferUtil.ensureFramebuffer(sharpScene, main.width, main.height,
                        () -> new MainTarget(main.width, main.height));
                FramebufferUtil.resizeIfNeeded(sharpScene, main.width, main.height);
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, FramebufferUtil.getFramebufferId(main));
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, FramebufferUtil.getFramebufferId(sharpScene));
                GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                        GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
                blurredScene.update(sharpScene);
                return;
            }
            int mainTexture = FramebufferUtil.getColorAttachmentId(main);
            int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainTexture);
            int previousMinFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER);
            int previousMagFilter = GL11.glGetTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            try {
                blurEffect.apply(radius);
            } finally {
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, mainTexture);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, previousMinFilter);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, previousMagFilter);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
            }
            blurredScene.update(blurEffect.getBlurFramebuffer());
        } finally {
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, previousFramebuffer);
            state.revert();
            GL11.glDepthMask(previousDepthMask);
        }
    }

    public void renderHand(HumanoidArm arm, Identifier texture, int firstColor, int secondColor,
                           float textureMix, Runnable renderer) {
        ensureInitialized();
        HumanoidArm previousArm = activeArm;
        Identifier previousTexture = activeTexture;
        int previousFirstColor = activeFirstColor;
        int previousSecondColor = activeSecondColor;
        int previousMix = activeMix;
        activeArm = arm;
        activeTexture = texture;
        activeFirstColor = firstColor;
        activeSecondColor = secondColor;
        activeMix = Math.clamp(Math.round(textureMix * 10.0f), 0, 5);
        try {
            renderer.run();
        } finally {
            activeArm = previousArm;
            activeTexture = previousTexture;
            activeFirstColor = previousFirstColor;
            activeSecondColor = previousSecondColor;
            activeMix = previousMix;
        }
    }

    public RenderType knifeRenderType(Identifier texture, RenderType fallback, boolean enabled) {
        if (!enabled || !blurredScene.isReady()) return fallback;
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        RenderTypeKey key = new RenderTypeKey(texture,
                Math.clamp(Math.round(module.handTextureMix.currentValue() * 10.0f), 0, 5),
                module.handColor.getFirstColor(), module.handColor.getGradientEndColor());
        return knifeRenderTypes.computeIfAbsent(key, this::createKnifeRenderType);
    }

    public int knifeOverlay(boolean enabled) {
        if (!enabled || !blurredScene.isReady()) return 0;
        long cycleNanos = 5_000_000_000L;
        int phase = (int) ((System.nanoTime() % cycleNanos) * 65535L / cycleNanos);
        return phase | phase << 16;
    }

    public RenderType replaceRenderType(RenderType original) {
        if (!blurredScene.isReady()) {
            return original;
        }
        RenderTypeKey key;
        if (activeArm != null && activeTexture != null) {
            key = new RenderTypeKey(activeTexture, activeMix, activeFirstColor, activeSecondColor);
        } else if (specialItemActive && original != RenderTypes.entityGlint()
                && original != RenderTypes.glint() && original != RenderTypes.glintTranslucent()
                && original != RenderTypes.armorEntityGlint()) {
            Object binding = ((RenderSetupAccessor) (Object) ((RenderTypeAccessor) original).raven$state())
                    .raven$textures().get("Sampler0");
            if (!(binding instanceof RenderSetupTextureBindingAccessor textureBinding)) {
                return original;
            }
            ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
            key = new RenderTypeKey(textureBinding.raven$location(),
                    Math.clamp(Math.round(module.handTextureMix.currentValue() * 10.0f), 0, 5),
                    module.handColor.getFirstColor(), module.handColor.getGradientEndColor());
        } else {
            return original;
        }
        return renderTypes.computeIfAbsent(key, this::createRenderType);
    }

    public int replaceColor(int original) {
        return ((activeArm == null || activeTexture == null) && !specialItemActive) || !blurredScene.isReady()
                ? original : 0xFFFFFFFF;
    }

    public int replaceOverlay(int original) {
        return ((activeArm == null || activeTexture == null) && !specialItemActive) || !blurredScene.isReady()
                ? original : gradientOverlayCoords();
    }

    public void renderItemSubmission(ItemDisplayContext displayContext, Runnable renderer) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        boolean previousActive = specialItemActive;
        specialItemActive = (displayContext.firstPerson() || viewModelItemActive
                || module.isHmiMode() && isHmiHand(displayContext))
                && module.shouldRenderGlassHands() && blurredScene.isReady();
        try {
            renderer.run();
        } finally {
            specialItemActive = previousActive;
        }
    }

    public void renderViewModelItem(Runnable renderer) {
        boolean previousActive = viewModelItemActive;
        viewModelItemActive = true;
        try {
            renderer.run();
        } finally {
            viewModelItemActive = previousActive;
        }
    }

    public boolean isViewModelItemActive() {
        return viewModelItemActive;
    }

    public RenderType replaceItemRenderType(RenderType original, ItemDisplayContext displayContext,
                                             boolean viewModel, Identifier atlas) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!displayContext.firstPerson() && !viewModel || !module.shouldRenderGlassHands()
                || !blurredScene.isReady()) {
            return original;
        }
        int mix = Math.clamp(Math.round(module.handTextureMix.currentValue() * 10.0f), 0, 5);
        Identifier texture = atlas != null ? atlas : TextureAtlas.LOCATION_ITEMS;
        return renderTypes.computeIfAbsent(new RenderTypeKey(texture, mix,
                module.handColor.getFirstColor(), module.handColor.getGradientEndColor()),
                this::createRenderType);
    }

    public int replaceItemColor(int original, ItemDisplayContext displayContext, boolean viewModel) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!displayContext.firstPerson() && !viewModel || !module.shouldRenderGlassHands()
                || !blurredScene.isReady()) {
            return original;
        }
        return 0xFFFFFFFF;
    }

    public int replaceItemOverlay(int original, ItemDisplayContext displayContext, boolean viewModel) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!displayContext.firstPerson() && !viewModel || !module.shouldRenderGlassHands()
                || !blurredScene.isReady()) {
            return original;
        }
        return gradientOverlayCoords();
    }

    public RenderType replaceHmiItemRenderType(RenderType original, ItemDisplayContext displayContext,
                                                Identifier atlas) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!module.isHmiMode() || !module.shouldRenderGlassHands() || !isHmiHand(displayContext)
                || !blurredScene.isReady()) {
            return original;
        }
        int mix = Math.clamp(Math.round(module.handTextureMix.currentValue() * 10.0f), 0, 5);
        Identifier texture = atlas != null ? atlas : TextureAtlas.LOCATION_ITEMS;
        return renderTypes.computeIfAbsent(new RenderTypeKey(texture, mix,
                module.handColor.getFirstColor(), module.handColor.getGradientEndColor()),
                this::createRenderType);
    }

    public int replaceHmiItemColor(int original, ItemDisplayContext displayContext) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!module.isHmiMode() || !module.shouldRenderGlassHands() || !isHmiHand(displayContext)
                || !blurredScene.isReady()) {
            return original;
        }
        return 0xFFFFFFFF;
    }

    public int replaceHmiItemOverlay(int original, ItemDisplayContext displayContext) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!module.isHmiMode() || !module.shouldRenderGlassHands() || !isHmiHand(displayContext)
                || !blurredScene.isReady()) {
            return original;
        }
        return gradientOverlayCoords();
    }

    public int gradientOverlayCoords() {
        long cycleNanos = 2_400_000_000L;
        int phase = (int) ((System.nanoTime() % cycleNanos) * 65535L / cycleNanos);
        return phase | phase << 16;
    }

    public void renderHmiBlock(Runnable renderer) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!module.isHmiMode() || !module.shouldRenderGlassHands() || !blurredScene.isReady()) {
            renderer.run();
            return;
        }
        boolean previousActive = hmiBlockActive;
        int previousFirstColor = hmiBlockFirstColor;
        int previousSecondColor = hmiBlockSecondColor;
        int previousMix = hmiBlockMix;
        hmiBlockActive = true;
        hmiBlockFirstColor = module.handColor.getFirstColor();
        hmiBlockSecondColor = module.handColor.getGradientEndColor();
        hmiBlockMix = Math.clamp(Math.round(module.handTextureMix.currentValue() * 10.0f), 0, 5);
        try {
            renderer.run();
        } finally {
            hmiBlockActive = previousActive;
            hmiBlockFirstColor = previousFirstColor;
            hmiBlockSecondColor = previousSecondColor;
            hmiBlockMix = previousMix;
        }
    }

    public RenderType replaceHmiBlockRenderType(RenderType original) {
        if (!hmiBlockActive) {
            return original;
        }
        BlockRenderTypeKey key = new BlockRenderTypeKey(TextureAtlas.LOCATION_BLOCKS, hmiBlockMix,
                hmiBlockFirstColor, hmiBlockSecondColor);
        return blockRenderTypes.computeIfAbsent(key, this::createBlockRenderType);
    }

    public int replaceHmiBlockOverlay(int original) {
        return hmiBlockActive && blurredScene.isReady() ? gradientOverlayCoords() : original;
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        blurEffect.init();
        Minecraft.getInstance().getTextureManager().register(BLURRED_SCENE, blurredScene);
        initialized = true;
    }

    private RenderType createRenderType(RenderTypeKey key) {
        BlockPipelineKey pipelineKey = new BlockPipelineKey(key.textureMix,
                key.firstColor, key.secondColor, false);
        RenderPipeline pipeline = blockPipelines.computeIfAbsent(pipelineKey, this::createBlockPipeline);
        RenderSetup setup = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", key.texture)
                .withTexture("Sampler2", BLURRED_SCENE)
                .sortOnUpload()
                .createRenderSetup();
        return RenderType.create("raven_glass_hands", setup);
    }

    private RenderType createKnifeRenderType(RenderTypeKey key) {
        int color = key.firstColor;
        int second = key.secondColor;
        Identifier pipelineId = Identifier.fromNamespaceAndPath("raven",
                "pipeline/glass_knife_" + key.textureMix + "_" + Integer.toUnsignedString(color, 16)
                        + "_" + Integer.toUnsignedString(second, 16));
        RenderPipeline pipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withLocation(pipelineId)
                .withVertexShader(SHADER)
                .withFragmentShader(SHADER)
                .withShaderDefine("TEXTURE_MIX", key.textureMix / 10.0f)
                .withShaderDefine("ALPHA_CUTOUT", 0.05f)
                .withShaderDefine("BLOCK_TINT")
                .withShaderDefine("KNIFE_TINT")
                .withShaderDefine("BLOCK_TINT_RED", ((color >> 16) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_GREEN", ((color >> 8) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_BLUE", (color & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_ALPHA", ((color >>> 24) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_RED2", ((second >> 16) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_GREEN2", ((second >> 8) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_BLUE2", (second & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_ALPHA2", ((second >>> 24) & 0xFF) / 255.0f)
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
                .withCull(false)
                .build());
        RenderSetup setup = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", key.texture)
                .withTexture("Sampler2", BLURRED_SCENE)
                .createRenderSetup();
        return RenderType.create("raven_glass_knife", setup);
    }

    private RenderType createBlockRenderType(BlockRenderTypeKey key) {
        BlockPipelineKey pipelineKey = new BlockPipelineKey(key.textureMix,
                key.firstColor, key.secondColor, true);
        RenderPipeline pipeline = blockPipelines.computeIfAbsent(pipelineKey, this::createBlockPipeline);
        RenderSetup setup = RenderSetup.builder(pipeline)
                .withTexture("Sampler0", key.texture)
                .withTexture("Sampler2", BLURRED_SCENE)
                .sortOnUpload()
                .createRenderSetup();
        return RenderType.create("raven_glass_hmi_block", setup);
    }

    private RenderPipeline createBlockPipeline(BlockPipelineKey key) {
        int color = key.firstColor;
        int second = key.secondColor;
        Identifier pipelineId = Identifier.fromNamespaceAndPath("raven",
                "pipeline/glass_hmi_block_" + key.textureMix + "_" + Integer.toUnsignedString(color, 16)
                        + "_" + Integer.toUnsignedString(second, 16) + "_" + key.cull);
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                .withLocation(pipelineId)
                .withVertexShader(SHADER)
                .withFragmentShader(SHADER)
                .withShaderDefine("TEXTURE_MIX", key.textureMix / 10.0f)
                .withShaderDefine("ALPHA_CUTOUT", 0.05f)
                .withShaderDefine("BLOCK_TINT")
                .withShaderDefine("BLOCK_TINT_RED", ((color >> 16) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_GREEN", ((color >> 8) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_BLUE", (color & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_ALPHA", ((color >>> 24) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_RED2", ((second >> 16) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_GREEN2", ((second >> 8) & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_BLUE2", (second & 0xFF) / 255.0f)
                .withShaderDefine("BLOCK_TINT_ALPHA2", ((second >>> 24) & 0xFF) / 255.0f)
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
                .withCull(key.cull)
                .build());
    }

    private boolean isHmiHand(ItemDisplayContext displayContext) {
        return displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    private record RenderTypeKey(Identifier texture, int textureMix, int firstColor, int secondColor) {
    }

    private record BlockRenderTypeKey(Identifier texture, int textureMix, int firstColor, int secondColor) {
    }

    private record BlockPipelineKey(int textureMix, int firstColor, int secondColor, boolean cull) {
    }

    private static final class BorrowedRenderTargetTexture extends AbstractTexture {
        private BorrowedRenderTargetTexture() {
            this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        }

        private void update(RenderTarget target) {
            if (target == null) {
                return;
            }
            this.texture = target.getColorTexture();
            this.textureView = target.getColorTextureView();
        }

        private boolean isReady() {
            return this.texture != null && this.textureView != null;
        }

        @Override
        public void close() {
            this.texture = null;
            this.textureView = null;
        }
    }
}
