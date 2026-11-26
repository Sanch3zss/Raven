package sanch3zss.raven.feature.module.render;


import sanch3zss.raven.render.shape.ImmediateRenderLayers;
import sanch3zss.raven.render.theme.ColorUtil;
import java.awt.Color;
import java.util.*;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.event.player.EntityDeathEvent;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"Kill Effects", "Death Effects", "Kill Particles"})
public final class KillEffectsModule extends Module {
    private static final Identifier GLOW_TEXTURE =
            Identifier.fromNamespaceAndPath("raven", "textures/killeffects_bloom.png");
    private static final Random RANDOM = new Random();

    private static final int RING_SEGMENTS = 24;
    private static final int RING_SIDES = 8;
    private static final float RING_RADIUS = 0.2f;
    private static final Identifier RING_LOSS_SOUND = Identifier.fromNamespaceAndPath("raven", "killeffects.ring_loss");
    static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath("raven", "textures/white.png");
    private static final float[] RING_POSITIONS = new float[RING_SEGMENTS * RING_SIDES * 3];
    private static final float[] RING_NORMALS = new float[RING_SEGMENTS * RING_SIDES * 3];

    static {
        for (int segment = 0; segment < RING_SEGMENTS; segment++) {
            float around = segment * Mth.TWO_PI / RING_SEGMENTS;
            float cos = Mth.cos(around);
            float sin = Mth.sin(around);
            for (int side = 0; side < RING_SIDES; side++) {
                float angle = side * Mth.TWO_PI / RING_SIDES + Mth.PI / RING_SIDES;
                float radial = Mth.cos(angle);
                float depth = Mth.sin(angle);
                float profileRadial = Math.signum(radial) * (float) Math.pow(Math.abs(radial), 0.6) * 0.05f;
                float profileDepth = Math.signum(depth) * (float) Math.pow(Math.abs(depth), 0.6) * 0.022f;
                int index = (segment * RING_SIDES + side) * 3;
                RING_POSITIONS[index] = (RING_RADIUS + profileRadial) * cos;
                RING_POSITIONS[index + 1] = (RING_RADIUS + profileRadial) * sin;
                RING_POSITIONS[index + 2] = profileDepth;
                Vector3f normal = new Vector3f(radial / 0.05f * cos, radial / 0.05f * sin, depth / 0.022f).normalize();
                RING_NORMALS[index] = normal.x;
                RING_NORMALS[index + 1] = normal.y;
                RING_NORMALS[index + 2] = normal.z;
            }
        }
    }

    private final ModeSetting<KillEffectMode> mode;
    private final ColorSetting color;
    private final List<DeathEffect> effects = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Ring> rings = new ArrayList<>();
    private final List<DustCorpse> corpses = new ArrayList<>();
    private final Map<Integer, Long> dissolved = new HashMap<>();
    private long lastRingUpdate;
    private final Quaternionf cameraRotation = new Quaternionf();
    private final Vector3f upVector = new Vector3f();
    private final Vector3f rightVector = new Vector3f();
    private final Vector3f relativePos = new Vector3f();
    private final Vector3f topLeft = new Vector3f();
    private final Vector3f bottomLeft = new Vector3f();
    private final Vector3f bottomRight = new Vector3f();
    private final Vector3f topRight = new Vector3f();

    public KillEffectsModule() {
        super(ModuleTab.VISUALS, "KillEffects");
        this.mode = new ModeSetting<KillEffectMode>(Translation.clearText("Эффект")).values(KillEffectMode.class);
        this.color = new ColorSetting(
                Translation.clearText("Цвет")
        ).value(new Color(100, 150, 255, 255).getRGB())
                .visible(() -> this.mode.isSelected(KillEffectMode.PARTICLES));
        addSettings(this.mode, this.color);

        register(EntityDeathEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()
                    && !event.entity().isRemoved()) {
                spawnEffect(event.entity());
            }
        });
        register(WorldRenderEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                render(event);
            }
        });
    }

    @Override
    public void deactivate() {
        this.effects.clear();
        this.particles.clear();
        this.rings.clear();
        this.corpses.clear();
        this.dissolved.clear();
        super.deactivate();
    }

    public boolean hides(Entity entity) {
        if (!isState() || this.dissolved.isEmpty()) {
            return false;
        }
        Long until = this.dissolved.get(entity.getId());
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until || !(entity instanceof LivingEntity living) || !living.isDeadOrDying()) {
            this.dissolved.remove(entity.getId());
            return false;
        }
        return true;
    }

    private void spawnEffect(LivingEntity entity) {
        if (this.mode.isSelected(KillEffectMode.RINGS)) {
            spawnRings(entity);
            return;
        }
        if (this.mode.isSelected(KillEffectMode.DUST)) {
            LocalPlayer player = Mc.INSTANCE.getPlayer();
            Vec3 from = player != null && player != entity && player.distanceToSqr(entity) < 64.0 ? player.position() : null;
            DustCorpse corpse = DustCorpse.capture(entity, from);
            if (corpse != null) {
                this.corpses.add(corpse);
                this.dissolved.put(entity.getId(), System.currentTimeMillis() + 10000L);
                return;
            }
        }
        int effectColor = this.color.getColor();
        spawnParticles(entity, effectColor);
    }

    private void spawnRings(LivingEntity entity) {
        Vec3 center = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);
        float pixel = 1.8f / 39.0f;
        float heading = RANDOM.nextFloat() * 360.0f;
        for (int circle = 0; circle < 2; circle++) {
            float speed = (circle == 0 ? 4.0f : 2.0f) * pixel * 60.0f;
            for (int pair = 0; pair < 8; pair++) {
                float angle = (101.25f + pair * 22.5f) * Mth.DEG_TO_RAD;
                float yaw = (heading + pair * 22.5f + circle * 11.25f) * Mth.DEG_TO_RAD;
                for (int side = -1; side <= 1; side += 2) {
                    float horizontal = Mth.cos(angle) * speed * side;
                    this.rings.add(new Ring(center, new Vec3(-Mth.sin(yaw) * horizontal,
                            Mth.sin(angle) * speed, Mth.cos(yaw) * horizontal), pixel));
                }
            }
        }
        this.dissolved.put(entity.getId(), System.currentTimeMillis() + 3000L);
        Mc.INSTANCE.getMinecraft().getSoundManager().play(new SimpleSoundInstance(
                SoundEvent.createVariableRangeEvent(RING_LOSS_SOUND), SoundSource.PLAYERS, 1.0f, 1.0f,
                SoundInstance.createUnseededRandom(), center.x, center.y, center.z));
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
        if (!isState() || this.rings.isEmpty() && this.corpses.isEmpty()) {
            this.lastRingUpdate = 0L;
            return;
        }
        long now = System.nanoTime();
        float delta = this.lastRingUpdate == 0L ? 0.0f : Math.min(0.05f, (now - this.lastRingUpdate) * 1.0e-9f);
        this.lastRingUpdate = now;
        RenderType ringType = RenderTypes.entityCutout(WHITE_TEXTURE);
        Iterator<Ring> rings = this.rings.iterator();
        while (rings.hasNext()) {
            Ring ring = rings.next();
            ring.update(delta);
            if (ring.age >= Ring.LIFETIME) {
                rings.remove();
            } else if (ring.prepare(camera)) {
                collector.submitCustomGeometry(poseStack, ringType, ring::emit);
            }
        }
        Iterator<DustCorpse> corpses = this.corpses.iterator();
        while (corpses.hasNext()) {
            DustCorpse corpse = corpses.next();
            corpse.submit(delta, poseStack, collector, camera);
            if (corpse.finished()) corpses.remove();
        }
    }

    private void render(WorldRenderEvent event) {
        if (this.effects.isEmpty() && this.particles.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        Camera camera = Mc.INSTANCE.getCamera();
        Vec3 cameraPos = camera.position();
        PoseStack matrices = event.matrixStack();
        Matrix4f matrix = matrices.last().pose();
        this.cameraRotation.set(camera.rotation());
        this.upVector.set(0.0f, 1.0f, 0.0f).rotate(this.cameraRotation);
        this.rightVector.set(1.0f, 0.0f, 0.0f).rotate(this.cameraRotation);

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(2048);
        BufferBuilder builder = new BufferBuilder(
                byteBuffer,
                PrimitiveTopology.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR
        );
        boolean rendered = false;

        Iterator<DeathEffect> effectIterator = this.effects.iterator();
        while (effectIterator.hasNext()) {
            DeathEffect effect = effectIterator.next();
            if (effect.isFinished(now)) {
                effectIterator.remove();
                continue;
            }
            rendered |= effect.render(builder, matrix, cameraPos, now);
        }

        Iterator<Particle> particleIterator = this.particles.iterator();
        while (particleIterator.hasNext()) {
            Particle particle = particleIterator.next();
            if (particle.isDead(now)) {
                particleIterator.remove();
                continue;
            }
            particle.update(now);
            rendered |= particle.render(builder, matrix, cameraPos, now);
        }

        if (rendered) {
            ImmediateRenderLayers.draw(
                    builder.buildOrThrow(),
                    "kill_effects",
                    DefaultVertexFormat.POSITION_TEX_COLOR,
                    PrimitiveTopology.QUADS,
                    GLOW_TEXTURE,
                    true,
                    true,
                    false,
                    false
            );
        } else {
            MeshData empty = builder.build();
            if (empty != null) {
                empty.close();
            }
        }
        byteBuffer.close();
    }

    private void spawnParticles(LivingEntity entity, int effectColor) {
        Vec3 pos = entity.position();
        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        float yaw = (float) Math.toRadians(-entity.yRotO + 90.0f);
        ParticleBehavior behavior = ParticleBehavior.SCATTER;
        Vec3 shapeCenter = pos.add(0.0, height * 0.5, 0.0);
        int count = 250;

        if (behavior == ParticleBehavior.HOVER) {
            if (!spawnModelSilhouette(entity, effectColor, shapeCenter)) {
                spawnSphere(
                        shapeCenter,
                        Math.max(width, height) * 0.25f,
                        count,
                        effectColor,
                        behavior,
                        shapeCenter
                );
            }
            return;
        }

        spawnSphere(
                pos.add(0.0, height - 0.2f, 0.0),
                width * 0.4f,
                count / 10,
                effectColor,
                behavior,
                shapeCenter
        );
        spawnBody(
                pos,
                height * 0.4f,
                height * 0.85f,
                width * 0.4f,
                width * 0.2f,
                yaw,
                count / 4,
                effectColor,
                behavior,
                shapeCenter
        );
        for (int side = -1; side <= 1; side += 2) {
            Vec3 armPos = new Vec3(
                    Math.sin(yaw) * width * 0.5 * side,
                    height * 0.75f,
                    Math.cos(yaw) * width * 0.5 * side
            );
            spawnLimb(
                    pos.add(armPos),
                    height * 0.4f,
                    width * 0.15f,
                    count / 8,
                    effectColor,
                    behavior,
                    shapeCenter
            );

            Vec3 legPos = new Vec3(
                    Math.sin(yaw) * width * 0.15 * side,
                    height * 0.4f,
                    Math.cos(yaw) * width * 0.15 * side
            );
            spawnLimb(
                    pos.add(legPos),
                    height * 0.45f,
                    width * 0.15f,
                    count / 6,
                    effectColor,
                    behavior,
                    shapeCenter
            );
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean spawnModelSilhouette(LivingEntity entity, int effectColor,
                                         Vec3 shapeCenter) {
        try {
            Object entityRenderer = Mc.INSTANCE.getEntityRenderDispatcher()
                    .getRenderer(entity);
            if (!(entityRenderer instanceof LivingEntityRenderer renderer)) {
                return false;
            }

            float tickDelta = Mc.INSTANCE.getTickDelta();
            LivingEntityRenderState state =
                    (LivingEntityRenderState) renderer.createRenderState();
            renderer.extractRenderState(entity, state, tickDelta);
            EntityModel model = (EntityModel) renderer.getModel();
            model.setupAnim(state);

            PoseStack matrices = new PoseStack();
            Vec3 position = entity.position();
            matrices.translate(position.x, position.y, position.z);
            matrices.mulPose(Axis.YP.rotationDegrees(
                    180.0f - state.bodyRot
            ));
            float modelScale = Math.max(
                    0.01f, state.scale * state.ageScale
            );
            matrices.scale(-modelScale, -modelScale, modelScale);
            matrices.translate(0.0f, -1.501f, 0.0f);

            List<ModelFace> faces = new ArrayList<>();
            model.root().visit(
                    matrices,
                    (entry, path, index, cuboid) ->
                            collectCuboidFaces(entry.pose(),
                                    cuboid, faces)
            );
            if (faces.isEmpty()) {
                return false;
            }

            float totalArea = 0.0f;
            for (ModelFace face : faces) {
                totalArea += face.area;
            }
            if (totalArea <= 1.0E-5f) {
                return false;
            }

            int count = Mth.clamp(
                    300 + faces.size() * 3,
                    340,
                    560
            );
            for (int i = 0; i < count; i++) {
                float choice = RANDOM.nextFloat() * totalArea;
                ModelFace selected = faces.get(faces.size() - 1);
                for (ModelFace face : faces) {
                    choice -= face.area;
                    if (choice <= 0.0f) {
                        selected = face;
                        break;
                    }
                }
                Vector3f point = selected.randomPoint();
                this.particles.add(new Particle(
                        new Vec3(point.x, point.y, point.z),
                        shapeCenter,
                        effectColor,
                        ParticleBehavior.HOVER
                ));
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void collectCuboidFaces(Matrix4f matrix, ModelPart.Cube cuboid,
                                    List<ModelFace> destination) {
        for (ModelPart.Polygon quad : cuboid.polygons) {
            if (quad == null || quad.vertices() == null
                    || quad.vertices().length < 4) {
                continue;
            }
            ModelPart.Vertex[] vertices = quad.vertices();
            Vector3f a = transformVertex(matrix, vertices[0]);
            Vector3f b = transformVertex(matrix, vertices[1]);
            Vector3f c = transformVertex(matrix, vertices[2]);
            Vector3f d = transformVertex(matrix, vertices[3]);
            float area = triangleArea(a, b, c) + triangleArea(a, c, d);
            if (area > 1.0E-5f) {
                destination.add(new ModelFace(a, b, c, d, area));
            }
        }
    }

    private Vector3f transformVertex(Matrix4f matrix,
                                     ModelPart.Vertex vertex) {
        return new Vector3f(
                vertex.worldX(),
                vertex.worldY(),
                vertex.worldZ()
        ).mulPosition(matrix);
    }

    private float triangleArea(Vector3f a, Vector3f b, Vector3f c) {
        return new Vector3f(b).sub(a)
                .cross(new Vector3f(c).sub(a))
                .length() * 0.5f;
    }

    private void spawnSphere(Vec3 pos, float radius, int count,
                             int effectColor, ParticleBehavior behavior,
                             Vec3 shapeCenter) {
        for (int i = 0; i < count; i++) {
            float angle1 = RANDOM.nextFloat() * (float) (Math.PI * 2.0);
            float angle2 = (float) Math.acos(2.0f * RANDOM.nextFloat() - 1.0f);
            float radiusSample = radius
                    * (float) Math.cbrt(RANDOM.nextFloat());
            float x = radiusSample
                    * (float) (Math.sin(angle2) * Math.cos(angle1));
            float y = radiusSample
                    * (float) (Math.sin(angle2) * Math.sin(angle1));
            float z = radiusSample * (float) Math.cos(angle2);
            this.particles.add(new Particle(
                    pos.add(x, y, z), shapeCenter, effectColor, behavior
            ));
        }
    }

    private void spawnBody(Vec3 pos, float minHeight, float maxHeight,
                           float radiusX, float spread, float yaw, int count,
                           int effectColor, ParticleBehavior behavior,
                           Vec3 shapeCenter) {
        for (int i = 0; i < count; i++) {
            float offsetX = (RANDOM.nextFloat() - 0.5f) * radiusX * 2.0f;
            float offsetY = minHeight + RANDOM.nextFloat() * (maxHeight - minHeight);
            float offsetZ = (RANDOM.nextFloat() - 0.5f) * spread * 2.0f;
            float x = (float) (offsetX * Math.cos(yaw)
                    - offsetZ * Math.sin(yaw));
            float z = (float) (offsetX * Math.sin(yaw)
                    + offsetZ * Math.cos(yaw));
            this.particles.add(new Particle(
                    pos.add(x, offsetY, z), shapeCenter, effectColor, behavior
            ));
        }
    }

    private void spawnLimb(Vec3 pos, float height, float radius,
                           int count, int effectColor,
                           ParticleBehavior behavior, Vec3 shapeCenter) {
        for (int i = 0; i < count; i++) {
            float y = -RANDOM.nextFloat() * height;
            float x = (RANDOM.nextFloat() - 0.5f) * radius * 2.0f;
            float z = (RANDOM.nextFloat() - 0.5f) * radius * 2.0f;
            this.particles.add(new Particle(
                    pos.add(x, y, z), shapeCenter, effectColor, behavior
            ));
        }
    }

    private void emitBillboard(BufferBuilder builder, Matrix4f matrix,
                               Vec3 worldPos, Vec3 cameraPos,
                               float size, int color, float alpha) {
        this.relativePos.set(
                (float) (worldPos.x - cameraPos.x),
                (float) (worldPos.y - cameraPos.y),
                (float) (worldPos.z - cameraPos.z)
        );
        float half = size * 0.5f;
        this.topLeft.set(this.relativePos)
                .fma(-half, this.rightVector).fma(half, this.upVector);
        this.bottomLeft.set(this.relativePos)
                .fma(-half, this.rightVector).fma(-half, this.upVector);
        this.bottomRight.set(this.relativePos)
                .fma(half, this.rightVector).fma(-half, this.upVector);
        this.topRight.set(this.relativePos)
                .fma(half, this.rightVector).fma(half, this.upVector);

        float red = ColorUtil.red(color) / 255.0f;
        float green = ColorUtil.green(color) / 255.0f;
        float blue = ColorUtil.blue(color) / 255.0f;
        float colorAlpha = ColorUtil.alpha(color) / 255.0f * alpha;
        builder.addVertex(matrix, this.topLeft.x, this.topLeft.y, this.topLeft.z)
                .setUv(0.0f, 0.0f).setColor(red, green, blue, colorAlpha);
        builder.addVertex(matrix, this.bottomLeft.x, this.bottomLeft.y,
                        this.bottomLeft.z)
                .setUv(0.0f, 1.0f).setColor(red, green, blue, colorAlpha);
        builder.addVertex(matrix, this.bottomRight.x, this.bottomRight.y,
                        this.bottomRight.z)
                .setUv(1.0f, 1.0f).setColor(red, green, blue, colorAlpha);
        builder.addVertex(matrix, this.topRight.x, this.topRight.y,
                        this.topRight.z)
                .setUv(1.0f, 0.0f).setColor(red, green, blue, colorAlpha);
    }

    private static float smootherStep(float value) {
        float t = Mth.clamp(value, 0.0f, 1.0f);
        return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
    }

    private static final class ModelFace {
        private final Vector3f a;
        private final Vector3f b;
        private final Vector3f c;
        private final Vector3f d;
        private final float area;

        private ModelFace(Vector3f a, Vector3f b, Vector3f c, Vector3f d,
                          float area) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.area = area;
        }

        private Vector3f randomPoint() {
            Vector3f first;
            Vector3f second;
            Vector3f third;
            if (RANDOM.nextBoolean()) {
                first = this.a;
                second = this.b;
                third = this.c;
            } else {
                first = this.a;
                second = this.c;
                third = this.d;
            }
            float root = (float) Math.sqrt(RANDOM.nextFloat());
            float split = RANDOM.nextFloat();
            float firstWeight = 1.0f - root;
            float secondWeight = root * (1.0f - split);
            float thirdWeight = root * split;
            return new Vector3f(first).mul(firstWeight)
                    .add(new Vector3f(second).mul(secondWeight))
                    .add(new Vector3f(third).mul(thirdWeight));
        }
    }

    private final class Particle {
        private double x;
        private double y;
        private double z;
        private float velocityX;
        private float velocityY;
        private float velocityZ;
        private final long startTime;
        private long lastUpdate;
        private final float gravityFactor;
        private final long lifetime;
        private final ParticleBehavior behavior;
        private final float friction;
        private final int color;
        private final float colorPhase;

        private Particle(Vec3 position, Vec3 shapeCenter, int color,
                         ParticleBehavior behavior) {
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
            this.color = color;
            this.colorPhase = RANDOM.nextFloat();
            this.startTime = this.lastUpdate = System.currentTimeMillis();
            this.behavior = behavior;
            this.gravityFactor = 0.005f + RANDOM.nextFloat() * 0.005f;
            this.lifetime = behavior == ParticleBehavior.HOVER
                    ? 1750L
                    : 1500L + RANDOM.nextInt(1500);
            this.friction = behavior == ParticleBehavior.GRAVITY
                    ? 0.999f
                    : 0.995f;

            if (behavior == ParticleBehavior.HOVER) {
                this.velocityX = 0.0f;
                this.velocityY = 0.0f;
                this.velocityZ = 0.0f;
            } else if (behavior == ParticleBehavior.GRAVITY) {
                this.velocityX = (RANDOM.nextFloat() - 0.5f) * 0.04f;
                this.velocityY = 0.025f + RANDOM.nextFloat() * 0.035f;
                this.velocityZ = (RANDOM.nextFloat() - 0.5f) * 0.04f;
            } else {
                this.velocityX = (RANDOM.nextFloat() - 0.5f) * 0.03f;
                this.velocityY = (RANDOM.nextFloat() - 0.5f) * 0.03f;
                this.velocityZ = (RANDOM.nextFloat() - 0.5f) * 0.03f;
            }
        }

        private void update(long now) {
            float delta = Math.min(5.0f, (now - this.lastUpdate) / 16.67f);
            this.lastUpdate = now;
            if (this.behavior == ParticleBehavior.HOVER) {
                return;
            }
            if (this.behavior == ParticleBehavior.GRAVITY) {
                this.velocityY -= this.gravityFactor * delta;
            }
            float drag = (float) Math.pow(this.friction, delta);
            this.velocityX *= drag;
            this.velocityY *= drag;
            this.velocityZ *= drag;

            double nextX = this.x + this.velocityX * delta;
            double nextY = this.y + this.velocityY * delta;
            double nextZ = this.z + this.velocityZ * delta;
            if (this.behavior == ParticleBehavior.GRAVITY
                    && Mc.INSTANCE.getWorld() != null) {
                if (!Mc.INSTANCE.getWorld().getBlockState(BlockPos.containing(
                        this.x, nextY - 0.05, this.z)).isAir()) {
                    this.velocityY = -this.velocityY * 0.5f;
                    nextY = this.y;
                }
                if (!Mc.INSTANCE.getWorld().getBlockState(BlockPos.containing(
                        nextX, this.y, this.z)).isAir()) {
                    this.velocityX = -this.velocityX * 0.5f;
                    nextX = this.x;
                }
                if (!Mc.INSTANCE.getWorld().getBlockState(BlockPos.containing(
                        this.x, this.y, nextZ)).isAir()) {
                    this.velocityZ = -this.velocityZ * 0.5f;
                    nextZ = this.z;
                }
            }
            if (Math.abs(this.velocityY) <= 1.0E-4f) {
                this.velocityX = 0.0f;
                this.velocityZ = 0.0f;
            }
            this.x = nextX;
            this.y = nextY;
            this.z = nextZ;
        }

        private float alpha(long now) {
            long elapsed = now - this.startTime;
            if (this.behavior == ParticleBehavior.HOVER) {
                if (elapsed < 250L) {
                    return smootherStep(elapsed / 250.0f);
                }
                if (elapsed < 1250L) {
                    return 1.0f;
                }
                return 1.0f - smootherStep((elapsed - 1250L) / 500.0f);
            }
            float progress = Mth.clamp(
                    (float) elapsed / this.lifetime,
                    0.0f,
                    1.0f
            );
            return 1.0f - progress;
        }

        private boolean isDead(long now) {
            return now - this.startTime >= this.lifetime;
        }

        private boolean render(BufferBuilder builder, Matrix4f matrix,
                               Vec3 cameraPos, long now) {
            float alpha = alpha(now);
            if (alpha <= 0.0f) {
                return false;
            }
            int animatedColor = KillEffectsModule.this.color.getAnimatedGradientColor(this.colorPhase);
            emitBillboard(
                    builder,
                    matrix,
                    new Vec3(this.x, this.y, this.z),
                    cameraPos,
                    0.5f,
                    animatedColor,
                    0.15f * alpha
            );
            emitBillboard(
                    builder,
                    matrix,
                    new Vec3(this.x, this.y, this.z),
                    cameraPos,
                    0.1f,
                    animatedColor,
                    0.9f * alpha
            );
            return true;
        }
    }

    private final class Ring {
        private static final float LIFETIME = 256.0f / 60.0f;
        private final float[] vertices = new float[RING_SEGMENTS * RING_SIDES * 3];
        private final float[] colors = new float[RING_SEGMENTS * RING_SIDES * 3];
        private final float[] normals = new float[RING_SEGMENTS * RING_SIDES * 3];
        private int light;
        private final float gravity;
        private final float radius;
        private final float scale;
        private double x;
        private double y;
        private double z;
        private double velocityX;
        private double velocityY;
        private double velocityZ;
        private float age;
        private float spin = RANDOM.nextFloat() * 360.0f;

        private Ring(Vec3 position, Vec3 velocity, float pixel) {
            this.gravity = 0.09375f * pixel * 3600.0f;
            this.radius = 5.5f * pixel;
            this.scale = this.radius / (RING_RADIUS + 0.05f);
            this.x = position.x;
            this.y = position.y;
            this.z = position.z;
            this.velocityX = velocity.x;
            this.velocityY = velocity.y;
            this.velocityZ = velocity.z;
        }

        private void update(float delta) {
            this.age += delta;
            float remaining = Math.max(1.0f, (LIFETIME - this.age) * 60.0f);
            float frameDuration = Math.max(1.0f, (float) Math.floor(512.0f / remaining));
            this.spin += delta * 2700.0f / frameDuration;
            for (float left = delta; left > 0.0f; left -= 1.0f / 120.0f) {
                float step = Math.min(left, 1.0f / 120.0f);
                this.velocityY -= this.gravity * step;
                double nextX = this.x + this.velocityX * step;
                double nextY = this.y + this.velocityY * step;
                double nextZ = this.z + this.velocityZ * step;
                if (this.velocityY < 0.0 && solid(this.x, nextY - this.radius, this.z)) {
                    this.velocityY = this.velocityY < -1.0 ? -this.velocityY * 0.75 : 0.0;
                    nextY = this.y;
                }
                if (solid(nextX, this.y, this.z)) {
                    this.velocityX = -this.velocityX * 0.5;
                    nextX = this.x;
                }
                if (solid(this.x, this.y, nextZ)) {
                    this.velocityZ = -this.velocityZ * 0.5;
                    nextZ = this.z;
                }
                this.x = nextX;
                this.y = nextY;
                this.z = nextZ;
            }
        }

        private boolean solid(double x, double y, double z) {
            BlockPos pos = BlockPos.containing(x, y, z);
            return Mc.INSTANCE.getWorld() != null
                    && !Mc.INSTANCE.getWorld().getBlockState(pos).getCollisionShape(Mc.INSTANCE.getWorld(), pos).isEmpty();
        }

        private boolean prepare(Vec3 cameraPos) {
            if (this.age > LIFETIME - 1.0f && ((int) (this.age * 15.0f) & 1) == 1) {
                return false;
            }
            ClientLevel level = Mc.INSTANCE.getWorld();
            this.light = level == null ? LightCoordsUtil.FULL_BRIGHT
                    : LightCoordsUtil.getLightCoords(level, BlockPos.containing(this.x, this.y, this.z));
            float cos = Mth.cos(this.spin * Mth.DEG_TO_RAD);
            float sin = Mth.sin(this.spin * Mth.DEG_TO_RAD);
            float centerX = (float) (this.x - cameraPos.x);
            float centerY = (float) (this.y - cameraPos.y);
            float centerZ = (float) (this.z - cameraPos.z);
            Vector3f light = new Vector3f(0.4f, 1.0f, 0.3f).normalize();
            Vector3f normal = new Vector3f();
            Vector3f view = new Vector3f();
            Vector3f half = new Vector3f();
            for (int i = 0; i < RING_SEGMENTS * RING_SIDES; i++) {
                int index = i * 3;
                float localX = RING_POSITIONS[index] * this.scale;
                float localZ = RING_POSITIONS[index + 2] * this.scale;
                float px = centerX + localX * cos + localZ * sin;
                float py = centerY + RING_POSITIONS[index + 1] * this.scale;
                float pz = centerZ - localX * sin + localZ * cos;
                this.vertices[index] = px;
                this.vertices[index + 1] = py;
                this.vertices[index + 2] = pz;
                normal.set(RING_NORMALS[index] * cos + RING_NORMALS[index + 2] * sin, RING_NORMALS[index + 1],
                        -RING_NORMALS[index] * sin + RING_NORMALS[index + 2] * cos);
                view.set(-px, -py, -pz).normalize();
                if (normal.dot(view) < 0.0f) {
                    normal.negate();
                }
                this.normals[index] = normal.x;
                this.normals[index + 1] = normal.y;
                this.normals[index + 2] = normal.z;
                half.set(light).add(view).normalize();
                float specular = (float) Math.pow(Math.max(0.0f, normal.dot(half)), 28.0) * 0.6f;
                float rim = (float) Math.pow(1.0f - Math.max(0.0f, normal.dot(view)), 2.0) * 0.25f;
                this.colors[index] = Math.min(1.0f, 0.95f + specular + rim);
                this.colors[index + 1] = Math.min(1.0f, 0.72f + specular + rim * 0.8f);
                this.colors[index + 2] = Math.min(1.0f, 0.1f + specular * 0.7f + rim * 0.3f);
            }
            return true;
        }

        private void emit(PoseStack.Pose pose, VertexConsumer buffer) {
            for (int segment = 0; segment < RING_SEGMENTS; segment++) {
                int nextSegment = (segment + 1) % RING_SEGMENTS;
                for (int side = 0; side < RING_SIDES; side++) {
                    int nextSide = (side + 1) % RING_SIDES;
                    vertex(pose, buffer, segment * RING_SIDES + side);
                    vertex(pose, buffer, nextSegment * RING_SIDES + side);
                    vertex(pose, buffer, nextSegment * RING_SIDES + nextSide);
                    vertex(pose, buffer, segment * RING_SIDES + nextSide);
                }
            }
        }

        private void vertex(PoseStack.Pose pose, VertexConsumer buffer, int vertex) {
            int index = vertex * 3;
            buffer.addVertex(pose, this.vertices[index], this.vertices[index + 1], this.vertices[index + 2])
                    .setColor(this.colors[index], this.colors[index + 1], this.colors[index + 2], 1.0f)
                    .setUv(0.5f, 0.5f)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(this.light)
                    .setNormal(pose, this.normals[index], this.normals[index + 1], this.normals[index + 2]);
        }
    }

    private final class DeathEffect {
        private static final long APPEAR_DURATION = 500L;
        private static final long FADE_DURATION = 800L;
        private final Vec3 origin;
        private final int color;
        private final long startTime = System.currentTimeMillis();
        private final List<Vec3> positions = new ArrayList<>(200);

        private DeathEffect(Vec3 origin, int color) {
            this.origin = origin;
            this.color = color;
            Vec3 current = origin;
            for (int i = 0; i < 200; i++) {
                current = current.add(
                        RANDOM.nextFloat() * 0.5f - 0.25f,
                        0.25,
                        RANDOM.nextFloat() * 0.5f - 0.25f
                );
                this.positions.add(current);
            }
        }

        private boolean isFinished(long now) {
            return now - this.startTime >= APPEAR_DURATION + FADE_DURATION;
        }

        private float alpha(long now) {
            long elapsed = now - this.startTime;
            float appear = smootherStep(Mth.clamp(
                    (float) elapsed / APPEAR_DURATION, 0.0f, 1.0f
            ));
            if (elapsed <= APPEAR_DURATION) {
                return appear;
            }
            float fade = (float) (elapsed - APPEAR_DURATION) / FADE_DURATION;
            return appear * (1.0f - smootherStep(fade));
        }

        private boolean render(BufferBuilder builder, Matrix4f matrix,
                               Vec3 cameraPos, long now) {
            float alpha = alpha(now);
            if (alpha <= 0.0f) {
                return false;
            }
            for (int i = 0; i < this.positions.size(); i++) {
                Vec3 position = this.positions.get(i);
                float size = (float) (1.0
                        + 3.0 * (position.y - this.origin.y) / 50.0);
                emitBillboard(
                        builder,
                        matrix,
                        position,
                        cameraPos,
                        size,
                        KillEffectsModule.this.color.getAnimatedGradientColor(
                                (float) i / this.positions.size()),
                        alpha * 0.4f
                );
            }
            return true;
        }
    }

    private enum KillEffectMode implements DisplayNamed {
        PARTICLES("Частицы"),
        RINGS("Кольца"),
        DUST("Пепел");

        private final Translation displayName;

        KillEffectMode(String name) {
            this.displayName = Translation.clearText(name);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    private enum ParticleBehavior implements DisplayNamed {
        GRAVITY("Гравитация"),
        SCATTER("Разлёт"),
        HOVER("Зависание");

        private final Translation displayName;

        ParticleBehavior(String name) {
            this.displayName = Translation.clearText(name);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }
}
