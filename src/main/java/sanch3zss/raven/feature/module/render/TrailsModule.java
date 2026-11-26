package sanch3zss.raven.feature.module.render;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.render.shape.ImmediateRenderLayers;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.ColorSetting;
import sanch3zss.raven.ui.setting.NumberSetting;

public class TrailsModule extends Module {
    private static final RenderType LINES = RenderType.create("raven_trails_lines", RenderSetup.builder(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("raven", "pipeline/trails_lines"))
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
                    .withPrimitiveTopology(PrimitiveTopology.LINES)
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .withCull(false)
                    .build()).createRenderSetup());
    private final Mc mc = Mc.INSTANCE;
    private final Deque<Point> points = new ArrayDeque<>();
    private final NumberSetting length;
    public final ColorSetting color;
    private ClientLevel level;
    private LocalPlayer player;

    public TrailsModule() {
        super(ModuleTab.VISUALS, "Trails");
        this.length = new NumberSetting(Translation.clearText("Длина"))
                .range(150.0f, 350.0f).currentValue(250.0f).step(5.0f)
                .unit(SettingUnit.MILLISECONDS);
        this.color = new ColorSetting(Translation.clearText("Цвет"))
                .value(Raven.INSTANCE.theme().palette().accentStart().argb());
        addSettings(this.length, this.color);
        register(WorldRenderEvent.class, this::render);
    }

    @Override
    public void activate() {
        clear();
        super.activate();
    }

    @Override
    public void deactivate() {
        clear();
        super.deactivate();
    }

    private void render(WorldRenderEvent event) {
        if (!isState() || !mc.isWorldLoaded()) {
            return;
        }
        ClientLevel currentLevel = mc.getWorld();
        LocalPlayer currentPlayer = mc.getPlayer();
        if (currentLevel != level || currentPlayer != player) {
            clear();
            level = currentLevel;
            player = currentPlayer;
        }
        if (mc.getGameOptions().getCameraType().isFirstPerson()) {
            points.clear();
            return;
        }

        long now = System.nanoTime();
        long lifetime = (long) (length.currentValue() * 1_000_000.0f);
        while (!points.isEmpty() && now - points.peekFirst().createdAt > lifetime) {
            points.removeFirst();
        }
        Vec3 position = currentPlayer.getPosition(event.renderTickCounter().getGameTimeDeltaPartialTick(false));
        Point last = points.peekLast();
        if (last == null || last.position.distanceToSqr(position) > 1.0E-8 || now - last.createdAt > 8_000_000L) {
            points.addLast(new Point(position, now));
        }
        while (points.size() > 128) {
            points.removeFirst();
        }
        if (points.size() >= 2) {
            draw(event.matrixStack(), currentPlayer.getBbHeight());
        }
    }

    private void draw(PoseStack poseStack, float height) {
        Point[] trail = points.toArray(Point[]::new);
        Vec3 camera = mc.getCamera().position();
        int accent = this.color.getFirstColor();
        int bright = this.color.getGradientEndColor();
        int surface = Raven.INSTANCE.theme().palette().surfaceBackground().tone(900).argb();
        Matrix4f matrix = poseStack.last().pose();
        int segments = trail.length - 1;

        try (ByteBufferBuilder memory = new ByteBufferBuilder(segments * 80)) {
            BufferBuilder buffer = new BufferBuilder(memory, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int i = 0; i < segments; i++) {
                Vec3 a = trail[i].position.subtract(camera);
                Vec3 b = trail[i + 1].position.subtract(camera);
                int colorA = trailColor(accent, bright, surface, i, trail.length);
                int colorB = trailColor(accent, bright, surface, i + 1, trail.length);
                buffer.addVertex(matrix, (float) a.x, (float) a.y + 0.0005f, (float) a.z).setColor(colorA);
                buffer.addVertex(matrix, (float) a.x, (float) a.y + height, (float) a.z).setColor(colorA);
                buffer.addVertex(matrix, (float) b.x, (float) b.y + height, (float) b.z).setColor(colorB);
                buffer.addVertex(matrix, (float) b.x, (float) b.y + 0.0005f, (float) b.z).setColor(colorB);
            }
            MeshData mesh = buffer.buildOrThrow();
            ImmediateRenderLayers.draw(mesh, "trails_fill", DefaultVertexFormat.POSITION_COLOR,
                    PrimitiveTopology.QUADS, null, true, true, false, false);
        }
        try (ByteBufferBuilder memory = new ByteBufferBuilder(segments * 160)) {
            BufferBuilder buffer = new BufferBuilder(memory, PrimitiveTopology.LINES,
                    DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH);
            float width = Math.max(2.0f, mc.getGameRenderer().gameRenderState().windowRenderState.appropriateLineWidth);
            for (int i = 0; i < segments; i++) {
                Vec3 a = trail[i].position.subtract(camera);
                Vec3 b = trail[i + 1].position.subtract(camera);
                float dx = (float) (b.x - a.x);
                float dy = (float) (b.y - a.y);
                float dz = (float) (b.z - a.z);
                float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance < 1.0E-5f) {
                    continue;
                }
                dx /= distance;
                dy /= distance;
                dz /= distance;
                int colorA = lineColor(accent, bright, i, trail.length);
                int colorB = lineColor(accent, bright, i + 1, trail.length);
                lineVertex(buffer, matrix, a, 0.0005f, colorA, dx, dy, dz, width);
                lineVertex(buffer, matrix, b, 0.0005f, colorB, dx, dy, dz, width);
                lineVertex(buffer, matrix, a, height, colorA, dx, dy, dz, width);
                lineVertex(buffer, matrix, b, height, colorB, dx, dy, dz, width);
            }
            MeshData mesh = buffer.build();
            ImmediateRenderLayers.draw(LINES, mesh);
        }
    }

    private static int trailColor(int accent, int bright, int surface, int index, int size) {
        int color = ColorUtil.gradientWave(accent, bright, (float) index / size);
        float alpha = ColorUtil.alpha(color) / 255.0f;
        color = ColorUtil.interpolateColor(color, surface, 0.28f);
        return ColorUtil.replAlpha(color, alpha * Math.min((float) index / size, 1.0f) * 0.5f);
    }

    private static int lineColor(int accent, int bright, int index, int size) {
        int color = ColorUtil.gradientWave(accent, bright, (float) index / size);
        return ColorUtil.replAlpha(color, ColorUtil.alpha(color) / 255.0f
                * Math.min((float) index / size, 1.0f) * 0.5f);
    }

    private static void lineVertex(BufferBuilder buffer, Matrix4f matrix, Vec3 position, float yOffset,
                                   int color, float nx, float ny, float nz, float width) {
        buffer.addVertex(matrix, (float) position.x, (float) position.y + yOffset, (float) position.z)
                .setColor(color).setNormal(nx, ny, nz).setLineWidth(width);
    }

    private void clear() {
        points.clear();
        level = null;
        player = null;
    }

    private record Point(Vec3 position, long createdAt) {
    }
}
