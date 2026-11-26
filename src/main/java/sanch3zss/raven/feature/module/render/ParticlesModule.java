package sanch3zss.raven.feature.module.render;


import sanch3zss.raven.feature.module.render.targetesp.TargetESPModule;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.render.shape.ImmediateRenderLayers;
import sanch3zss.raven.render.theme.ColorUtil;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.math.MathUtil;

public abstract class ParticlesModule extends Module {
    public final MultiSelectSetting<ParticleTexture> textures;
    public final NumberSetting size;
    public final BooleanSetting brightMode;
    public final ColorSetting color;
    public final ModeSetting<SpawnDirection> spawnDirection;
    public final ModeSetting<MotionDirection> motionDirection;
    public final NumberSetting lifetime;
    public final NumberSetting speed;
    public final NumberSetting gravity;
    public final NumberSetting motionInfluence;
    public final NumberSetting magnetism;
    public final ModeSetting<CollisionMode> collisionMode;
    public final BooleanSetting particleCollisions;
    protected final List<OmniParticle> particles = new ArrayList<>();
    private final Quaternionf cameraRotation = new Quaternionf();
    private final Vector3f cameraRight = new Vector3f();
    private final Vector3f cameraUp = new Vector3f();
    private final Vector3f relative = new Vector3f();
    private final Vector3f quadRight = new Vector3f();
    private final Vector3f quadUp = new Vector3f();

    protected ParticlesModule(String name, boolean worldSource) {
        super(ModuleTab.VISUALS, name);
        this.textures = new MultiSelectSetting<ParticleTexture>(text("Текстуры"))
                .values(ParticleTexture.class).select(ParticleTexture.values());
        this.size = new NumberSetting(text("Макс. масштаб")).range(0.1f, 1.0f)
                .currentValue(0.5f).step(0.075f).unit(SettingUnit.PIXELS);
        this.brightMode = new BooleanSetting(text("Яркий режим")).setValue(true);
        this.color = new ColorSetting(text("Цвет"));
        this.color.themeSyncSetting().setValue(true);
        this.spawnDirection = new ModeSetting<SpawnDirection>(text("Направить при спавне"))
                .values(SpawnDirection.class)
                .select(worldSource ? SpawnDirection.RANDOM : SpawnDirection.AWAY_FROM_SOURCE);
        this.motionDirection = new ModeSetting<MotionDirection>(text("Толкать всегда"))
                .values(MotionDirection.class).select(MotionDirection.NONE);
        this.lifetime = new NumberSetting(text("Время жизни частиц")).range(500.0f, 4000.0f)
                .currentValue(worldSource ? 2500.0f : 1250.0f).step(50.0f)
                .unit(SettingUnit.MILLISECONDS);
        this.speed = new NumberSetting(text("Стартовая скорость")).range(0.05f, 1.0f)
                .currentValue(0.3f).step(0.05f).unit(SettingUnit.UNITS);
        this.gravity = new NumberSetting(text("Фактор уровня гравитации")).range(-0.5f, 1.0f)
                .currentValue(0.2f).step(0.05f).unit(SettingUnit.UNITS);
        this.motionInfluence = new NumberSetting(text("Замедление/ускорение")).range(-1.0f, 1.0f)
                .currentValue(0.0f).step(0.05f).unit(SettingUnit.UNITS);
        this.magnetism = new NumberSetting(text("Резкость поворотов движений частиц"))
                .range(0.1f, 2.0f).currentValue(1.0f).step(0.05f).unit(SettingUnit.UNITS)
                .visible(() -> !this.motionDirection.isSelected(MotionDirection.NONE));
        this.collisionMode = new ModeSetting<CollisionMode>(text("При столкновении"))
                .values(CollisionMode.class).select(CollisionMode.BOUNCE);
        this.particleCollisions = new BooleanSetting(text("Столкновения частиц друг с другом"));
        register(ClientTickEvent.class, event -> updateParticles());
        register(WorldRenderEvent.class, this::renderParticles);
    }

    protected final void addOmniSettings(Setting... sourceSettings) {
        ExpandableSetting source = new ExpandableSetting(text("Появление"))
                .navigationOnly().setSubSettings(Arrays.asList(sourceSettings));
        ExpandableSetting appearance = new ExpandableSetting(text("Внешний вид"))
                .navigationOnly().setSubSettings(List.of(this.textures, this.size,
                        this.brightMode, this.color, this.color.themeSyncSetting()));
        ExpandableSetting motion = new ExpandableSetting(text("Движение"))
                .navigationOnly().setSubSettings(List.of(this.spawnDirection, this.motionDirection,
                        this.lifetime, this.speed, this.gravity, this.motionInfluence,
                        this.magnetism));
        ExpandableSetting collisions = new ExpandableSetting(text("Столкновения"))
                .navigationOnly().setSubSettings(List.of(this.collisionMode, this.particleCollisions));
        addSettings(source, appearance, motion, collisions);
    }

    protected final void spawn(Entity source, int count, float spread) {
        if (source == null || count <= 0 || this.textures.selectedValues().isEmpty()
                || !Mc.INSTANCE.isWorldLoaded()) {
            return;
        }
        List<ParticleTexture> selectedTextures = List.copyOf(this.textures.selectedValues());
        Vec3 center = centerOf(source);
        for (int i = 0; i < count; i++) {
            Vec3 position = randomSpawnPosition(center, spread);
            if (position == null) continue;
            int particleColor = this.color.getGradientColor(random().nextFloat());
            this.particles.add(new OmniParticle(position, center, source,
                    selectedTextures.get(random().nextInt(selectedTextures.size())),
                    particleColor));
        }
    }

    private Vec3 randomSpawnPosition(Vec3 center, float spread) {
        ClientLevel world = Mc.INSTANCE.getWorld();
        for (int i = 0; i < 10; i++) {
            Vec3 position = center.add(direction(randomRange(-180.0f, 180.0f),
                    randomRange(-90.0f, 90.0f)).scale(randomRange(spread / 5.0f, spread)));
            BlockPos blockPos = BlockPos.containing(position);
            if (world.getBlockState(blockPos).getCollisionShape(world, blockPos).isEmpty()) return position;
        }
        return null;
    }

    private void updateParticles() {
        if (!Mc.INSTANCE.isWorldLoaded()) {
            this.particles.clear();
            return;
        }
        this.particles.removeIf(OmniParticle::expired);
        this.particles.forEach(OmniParticle::update);
    }

    private void renderParticles(WorldRenderEvent event) {
        if (this.particles.isEmpty()) return;
        Camera camera = Mc.INSTANCE.getCamera();
        Vec3 cameraPos = camera.position();
        float partialTick = Mc.INSTANCE.getRenderTickCounter().getGameTimeDeltaPartialTick(false);
        this.cameraRotation.set(camera.rotation());
        this.cameraRight.set(1.0f, 0.0f, 0.0f).rotate(this.cameraRotation);
        this.cameraUp.set(0.0f, 1.0f, 0.0f).rotate(this.cameraRotation);
        Map<ParticleTexture, List<OmniParticle>> textured = new EnumMap<>(ParticleTexture.class);
        for (OmniParticle particle : this.particles) {
            if (!event.frustum().isVisible(particle.bounds(partialTick))) continue;
            textured.computeIfAbsent(particle.texture,
                    ignored -> new ArrayList<>()).add(particle);
        }
        textured.forEach((texture, batch) -> renderTexturedBatch(event.matrixStack(),
                cameraPos, partialTick, texture, batch));
    }

    private void renderTexturedBatch(PoseStack matrices, Vec3 cameraPos, float partialTick,
                                     ParticleTexture texture, List<OmniParticle> batch) {
        ByteBufferBuilder memory = new ByteBufferBuilder(Math.max(2048, batch.size() * 128));
        BufferBuilder buffer = new BufferBuilder(memory, PrimitiveTopology.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        Matrix4f matrix = matrices.last().pose();
        boolean glow = texture == ParticleTexture.POINT;
        TextureAtlasSprite sprite = glow ? null : texture.sprite(true);
        float u0 = glow ? 0.0f : sprite.getU0();
        float u1 = glow ? 1.0f : sprite.getU1();
        float v0 = glow ? 0.0f : sprite.getV0();
        float v1 = glow ? 1.0f : sprite.getV1();
        for (OmniParticle particle : batch) {
            Vec3 position = particle.position(partialTick);
            float alpha = particle.alpha(partialTick) * (ColorUtil.alpha(particle.color) / 255.0f);
            float scale = particle.scale(alpha) * (glow ? 1.65f : 1.0f);
            float angle = (float) Math.toRadians(particle.angle(partialTick));
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            this.quadRight.set(this.cameraRight).mul(cos).fma(sin, this.cameraUp);
            this.quadUp.set(this.cameraUp).mul(cos).fma(-sin, this.cameraRight);
            this.relative.set((float) (position.x - cameraPos.x),
                    (float) (position.y - cameraPos.y + scale / 5.0f),
                    (float) (position.z - cameraPos.z));
            int tint = withAlpha(particle.color, alpha * (glow ? 0.68f : 1.0f));
            vertex(buffer, matrix, this.relative, this.quadRight, -scale, this.quadUp, scale, u0, v0, tint);
            vertex(buffer, matrix, this.relative, this.quadRight, -scale, this.quadUp, -scale, u0, v1, tint);
            vertex(buffer, matrix, this.relative, this.quadRight, scale, this.quadUp, -scale, u1, v1, tint);
            vertex(buffer, matrix, this.relative, this.quadRight, scale, this.quadUp, scale, u1, v0, tint);
        }
        ImmediateRenderLayers.draw(buffer.buildOrThrow(), "omni_particles_" + texture.name(),
                DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS,
                glow ? TargetESPModule.soulsTexture : TextureAtlas.LOCATION_PARTICLES,
                glow || this.brightMode.isValue(), true, false, false);
        memory.close();
    }

    private void renderCubeBatch(PoseStack matrices, Vec3 cameraPos, float partialTick,
                                 List<OmniParticle> batch) {
        ByteBufferBuilder fillMemory = new ByteBufferBuilder(Math.max(2048, batch.size() * 768));
        ByteBufferBuilder outlineMemory = new ByteBufferBuilder(Math.max(2048, batch.size() * 384));
        BufferBuilder fillBuffer = new BufferBuilder(fillMemory, PrimitiveTopology.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        BufferBuilder outlineBuffer = new BufferBuilder(outlineMemory, PrimitiveTopology.LINES,
                DefaultVertexFormat.POSITION_COLOR);
        Matrix4f matrix = matrices.last().pose();
        for (OmniParticle particle : batch) {
            Vec3 position = particle.position(partialTick).subtract(cameraPos);
            float alpha = particle.alpha(partialTick) * (ColorUtil.alpha(particle.color) / 255.0f);
            float half = particle.scale(alpha) / 4.5f;
            addCube(fillBuffer, matrix, (float) position.x, (float) position.y,
                    (float) position.z, half, withAlpha(particle.color, alpha * 0.03f));
            addCubeEdges(outlineBuffer, matrix, (float) position.x, (float) position.y,
                    (float) position.z, half, withAlpha(particle.color, alpha * 0.3f));
        }
        ImmediateRenderLayers.draw(fillBuffer.buildOrThrow(), "omni_particle_cubes_fill",
                DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.QUADS,
                null, this.brightMode.isValue(), true, false, false);
        ImmediateRenderLayers.draw(outlineBuffer.buildOrThrow(), "omni_particle_cubes_outline",
                DefaultVertexFormat.POSITION_COLOR, PrimitiveTopology.LINES,
                null, this.brightMode.isValue(), true, false, false);
        fillMemory.close();
        outlineMemory.close();
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, Vector3f center,
                               Vector3f right, float rightScale, Vector3f up, float upScale,
                               float u, float v, int color) {
        buffer.addVertex(matrix, center.x + right.x * rightScale + up.x * upScale,
                center.y + right.y * rightScale + up.y * upScale,
                center.z + right.z * rightScale + up.z * upScale)
                .setUv(u, v).setColor(color);
    }

    private static void addCube(BufferBuilder buffer, Matrix4f matrix, float x, float y,
                                float z, float half, int color) {
        float x0 = x - half, x1 = x + half, y0 = y - half, y1 = y + half, z0 = z - half, z1 = z + half;
        face(buffer, matrix, x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, color);
        face(buffer, matrix, x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, color);
        face(buffer, matrix, x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, color);
        face(buffer, matrix, x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, color);
        face(buffer, matrix, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, color);
        face(buffer, matrix, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, color);
    }

    private static void face(BufferBuilder buffer, Matrix4f matrix,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3, int color) {
        buffer.addVertex(matrix, x0, y0, z0).setColor(color);
        buffer.addVertex(matrix, x1, y1, z1).setColor(color);
        buffer.addVertex(matrix, x2, y2, z2).setColor(color);
        buffer.addVertex(matrix, x3, y3, z3).setColor(color);
    }

    private static void addCubeEdges(BufferBuilder buffer, Matrix4f matrix, float x, float y,
                                     float z, float half, int color) {
        float x0 = x - half, x1 = x + half, y0 = y - half, y1 = y + half, z0 = z - half, z1 = z + half;
        edge(buffer, matrix, x0, y0, z0, x1, y0, z0, color);
        edge(buffer, matrix, x1, y0, z0, x1, y0, z1, color);
        edge(buffer, matrix, x1, y0, z1, x0, y0, z1, color);
        edge(buffer, matrix, x0, y0, z1, x0, y0, z0, color);
        edge(buffer, matrix, x0, y1, z0, x1, y1, z0, color);
        edge(buffer, matrix, x1, y1, z0, x1, y1, z1, color);
        edge(buffer, matrix, x1, y1, z1, x0, y1, z1, color);
        edge(buffer, matrix, x0, y1, z1, x0, y1, z0, color);
        edge(buffer, matrix, x0, y0, z0, x0, y1, z0, color);
        edge(buffer, matrix, x1, y0, z0, x1, y1, z0, color);
        edge(buffer, matrix, x1, y0, z1, x1, y1, z1, color);
        edge(buffer, matrix, x0, y0, z1, x0, y1, z1, color);
    }

    private static void edge(BufferBuilder buffer, Matrix4f matrix, float x0, float y0,
                             float z0, float x1, float y1, float z1, int color) {
        buffer.addVertex(matrix, x0, y0, z0).setColor(color);
        buffer.addVertex(matrix, x1, y1, z1).setColor(color);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        this.particles.clear();
    }

    protected final class OmniParticle {
        private final Entity source;
        private final ParticleTexture texture;
        private final int color;
        private final float uniqueScale = random().nextFloat();
        private final int spin = random().nextBoolean() ? 1 : -1;
        private final int lifetimeTicks;
        private double x, y, z, previousX, previousY, previousZ;
        private double velocityX, velocityY, velocityZ;
        private float yaw, pitch;
        private float angle = randomRange(-15.0f, 15.0f);
        private float previousAngle = this.angle;
        private int age, previousAge;
        private boolean stuck;

        private OmniParticle(Vec3 position, Vec3 sourcePosition, Entity source,
                             ParticleTexture texture, int color) {
            this.source = source;
            this.texture = texture;
            this.color = color;
            this.x = this.previousX = position.x;
            this.y = this.previousY = position.y;
            this.z = this.previousZ = position.z;
            float[] rotations = startRotations(sourcePosition, position);
            this.yaw = rotations[0];
            this.pitch = rotations[1];
            float initialSpeed = randomRange(speed.currentValue() / 1.5f, speed.currentValue() * 1.5f);
            Vec3 velocity = direction(this.yaw, this.pitch).scale(initialSpeed);
            if (spawnDirection.isSelected(SpawnDirection.NO_XZ)) velocity = new Vec3(0.0d, velocity.y, 0.0d);
            this.velocityX = velocity.x;
            this.velocityY = velocity.y;
            this.velocityZ = velocity.z;
            this.lifetimeTicks = Math.max(1, Math.round(lifetime.currentValue() / 50.0f * randomRange(0.98f, 1.02f)));
        }

        private void update() {
            this.previousAge = this.age++;
            this.previousX = this.x;
            this.previousY = this.y;
            this.previousZ = this.z;
            this.previousAngle = this.angle;
            float currentSpeed = (float) Math.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY + this.velocityZ * this.velocityZ);
            this.angle -= 60.0f * this.spin * currentSpeed;
            MotionDirection rule = activeMotionRule();
            if (rule != MotionDirection.NONE) redirect(rule, currentSpeed);
            if (particleCollisions.isValue()) avoidParticles();
            float influence = motionInfluence.currentValue();
            float multiplier = influence > 0.0f ? 1.0f + cubicOut(influence) * 0.05f : 1.0f + sineOut(influence) * 0.1f;
            this.velocityX *= multiplier;
            this.velocityY *= multiplier;
            this.velocityZ *= multiplier;
            float gravityValue = gravity.currentValue();
            this.velocityY -= gravityValue / 17.5f;
            if (gravityValue > 0.0f && this.velocityY > 0.0d) this.velocityY /= 1.0f + gravityValue * 0.05f;
            this.velocityX = MathUtil.clamp((float) this.velocityX, -5.0f, 5.0f);
            this.velocityY = MathUtil.clamp((float) this.velocityY, -5.0f, 5.0f);
            this.velocityZ = MathUtil.clamp((float) this.velocityZ, -5.0f, 5.0f);
            moveWithCollisions();
        }

        private void redirect(MotionDirection rule, float currentSpeed) {
            float[] target = targetRotations(rule);
            float rotationSpeed = currentSpeed / 5.0f * rotationMultiplier(rule) * magnetism.currentValue();
            this.yaw = stepAngle(this.yaw, target[0], rotationSpeed * 180.0f);
            this.pitch = stepAngle(this.pitch, target[1], rotationSpeed * 90.0f);
            Vec3 redirected = direction(this.yaw, this.pitch).scale(currentSpeed);
            this.velocityX = redirected.x;
            this.velocityY = redirected.y;
            this.velocityZ = redirected.z;
        }

        private MotionDirection activeMotionRule() {
            if (!motionDirection.isSelected(MotionDirection.ALTERNATE_SOURCE)) return motionDirection.currentValue();
            return this.age < this.lifetimeTicks / 2 ? MotionDirection.TOWARD_SOURCE : MotionDirection.AWAY_FROM_SOURCE;
        }

        private void moveWithCollisions() {
            CollisionMode mode = collisionMode.currentValue();
            if (mode == CollisionMode.IGNORE) {
                move();
                return;
            }
            boolean collideX = blocked(this.x + this.velocityX, this.y, this.z);
            boolean collideY = blocked(this.x, this.y + this.velocityY, this.z);
            boolean collideZ = blocked(this.x, this.y, this.z + this.velocityZ);
            if (!collideX && !collideY && !collideZ && !this.stuck) {
                move();
                return;
            }
            if (mode == CollisionMode.FATAL) {
                this.age = this.lifetimeTicks;
                return;
            }
            if (mode == CollisionMode.STICK) {
                this.velocityX = this.velocityY = this.velocityZ = 0.0d;
                this.stuck = true;
                return;
            }
            float bounce = mode.bounceMultiplier;
            float slide = mode == CollisionMode.SLIDE_SLOW ? 0.875f : 1.0f;
            if (collideY || this.stuck) {
                this.velocityY = mode.bounces ? -this.velocityY * bounce / (Math.max(gravity.currentValue(), 0.0f) + 1.0f) : 0.0d;
                if (!mode.bounces) {
                    this.velocityX *= slide;
                    this.velocityZ *= slide;
                }
            }
            if (collideX || this.stuck) {
                this.velocityX = mode.bounces ? -this.velocityX * bounce : 0.0d;
                if (!mode.bounces) this.velocityZ *= slide;
            }
            if (collideZ || this.stuck) {
                this.velocityZ = mode.bounces ? -this.velocityZ * bounce : 0.0d;
                if (!mode.bounces) this.velocityX *= slide;
            }
            this.stuck = Math.sqrt(this.velocityX * this.velocityX + this.velocityY * this.velocityY + this.velocityZ * this.velocityZ) < 0.008d;
            move();
        }

        private void move() {
            this.x += this.velocityX;
            this.y += this.velocityY;
            this.z += this.velocityZ;
        }

        private boolean blocked(double x, double y, double z) {
            ClientLevel world = Mc.INSTANCE.getWorld();
            BlockPos pos = BlockPos.containing(x, y, z);
            return !world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
        }

        private void avoidParticles() {
            int checks = 0;
            float minDistance = scale(1.0f) / 6.0f;
            for (OmniParticle other : particles) {
                if (other == this) continue;
                if (checks++ >= 60) break;
                double dx = this.x - other.x, dy = this.y - other.y, dz = this.z - other.z;
                if (dx * dx + dy * dy + dz * dz < minDistance * minDistance) {
                    float[] rotation = rotations(new Vec3(dx, dy, dz));
                    this.yaw = rotation[0];
                    this.pitch = rotation[1];
                    return;
                }
            }
        }

        private float[] targetRotations(MotionDirection rule) {
            float cameraYaw = Mc.INSTANCE.getCamera().yRot();
            return switch (rule) {
                case CAMERA_LEFT -> new float[]{cameraYaw - 90.0f, this.pitch + 18.0f};
                case CAMERA_RIGHT -> new float[]{cameraYaw + 90.0f, this.pitch + 18.0f};
                case CAMERA_FORWARD -> new float[]{cameraYaw, this.pitch + 18.0f};
                case CAMERA_BACK -> new float[]{cameraYaw + 180.0f, this.pitch + 18.0f};
                case SPIN_LEFT -> new float[]{this.yaw - 180.0f, this.pitch + 18.0f};
                case SPIN_RIGHT -> new float[]{this.yaw + 180.0f, this.pitch + 18.0f};
                case SPIN_RANDOM -> new float[]{this.yaw + 180.0f * this.spin, this.pitch + 18.0f};
                case TOWARD_SOURCE -> rotations(centerOf(this.source).subtract(this.x, this.y, this.z));
                case AWAY_FROM_SOURCE -> rotations(new Vec3(this.x, this.y, this.z).subtract(centerOf(this.source)));
                default -> new float[]{this.yaw, this.pitch};
            };
        }

        private float rotationMultiplier(MotionDirection rule) {
            return switch (rule) {
                case TOWARD_SOURCE, AWAY_FROM_SOURCE -> 2.0f;
                case CAMERA_LEFT, CAMERA_RIGHT, CAMERA_FORWARD, CAMERA_BACK -> 0.5f;
                default -> 1.0f;
            };
        }

        private boolean expired() {
            return this.age >= this.lifetimeTicks;
        }

        private float progress(float partialTick) {
            return Math.min((this.previousAge + (this.age - this.previousAge) * partialTick) / (float) this.lifetimeTicks, 1.0f);
        }

        private float alpha(float partialTick) {
            float progress = progress(partialTick);
            float triangle = (progress > 0.5f ? 1.0f - progress : progress) * 2.0f;
            return 1.0f - (float) Math.pow(1.0f - triangle, 5.0f);
        }

        private float scale(float alpha) {
            float unique = size.currentValue() / 5.0f + (size.currentValue() - size.currentValue() / 5.0f) * this.uniqueScale;
            return unique * alpha;
        }

        private Vec3 position(float partialTick) {
            return new Vec3(this.previousX + (this.x - this.previousX) * partialTick,
                    this.previousY + (this.y - this.previousY) * partialTick,
                    this.previousZ + (this.z - this.previousZ) * partialTick);
        }

        private float angle(float partialTick) {
            return this.previousAngle + (this.angle - this.previousAngle) * partialTick;
        }

        private AABB bounds(float partialTick) {
            Vec3 position = position(partialTick);
            float scale = size.currentValue();
            return new AABB(position.x - scale, position.y - scale, position.z - scale,
                    position.x + scale, position.y + scale, position.z + scale);
        }

        private float[] startRotations(Vec3 sourcePosition, Vec3 particlePosition) {
            return switch (spawnDirection.currentValue()) {
                case NO_XZ -> new float[]{0.0f, randomRange(-90.0f, 90.0f)};
                case RANDOM -> new float[]{randomRange(0.0f, 360.0f), randomRange(-90.0f, 90.0f)};
                case TOWARD_SOURCE -> rotations(sourcePosition.subtract(particlePosition));
                case AWAY_FROM_SOURCE -> rotations(particlePosition.subtract(sourcePosition));
                case CAMERA_LEFT -> new float[]{Mc.INSTANCE.getCamera().yRot() - 90.0f, 0.0f};
                case CAMERA_RIGHT -> new float[]{Mc.INSTANCE.getCamera().yRot() + 90.0f, 0.0f};
                case CAMERA_FORWARD -> new float[]{Mc.INSTANCE.getCamera().yRot(), 0.0f};
                case CAMERA_BACK -> new float[]{Mc.INSTANCE.getCamera().yRot() + 180.0f, 0.0f};
                default -> new float[]{randomRange(0.0f, 360.0f), randomRange(-90.0f, 90.0f)};
            };
        }
    }

    private static Vec3 centerOf(Entity entity) {
        return entity.getEyePosition().add(0.0d, -entity.getEyeHeight() * 0.5d, 0.0d);
    }

    private static float[] rotations(Vec3 vector) {
        double horizontal = Math.sqrt(vector.x * vector.x + vector.z * vector.z);
        return new float[]{(float) Math.toDegrees(Math.atan2(-vector.x, vector.z)),
                (float) Math.toDegrees(Math.atan2(-vector.y, horizontal))};
    }

    private static Vec3 direction(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw), pitchRad = Math.toRadians(pitch), cosPitch = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch);
    }

    private static float stepAngle(float current, float target, float step) {
        float delta = (target - current + 540.0f) % 360.0f - 180.0f;
        return current + MathUtil.clamp(delta, -Math.abs(step), Math.abs(step));
    }

    private static float cubicOut(float value) {
        return 1.0f - (float) Math.pow(1.0f - value, 3.0f);
    }

    private static float sineOut(float value) {
        return (float) Math.sin(value * Math.PI / 2.0d);
    }

    private static float randomRange(float min, float max) {
        return min == max ? min : random().nextFloat(min, max);
    }

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    private static int withAlpha(int color, float alpha) {
        return color & 0x00FFFFFF | Math.round(MathUtil.clamp(alpha, 0.0f, 1.0f) * 255.0f) << 24;
    }

    private static Translation text(String value) {
        return Translation.clearText(value);
    }

    public enum ParticleTexture implements DisplayNamed {
        CROSS("Крестик", "cross.png"), CROWN("Коронка", "crown.png"),
        DOLLAR("Доллар", "dollar.png"), HEART("Сердечко", "heart.png"),
        LIGHTNING("Молния", "lighting.png"), LINE("Линия", "line.png"),
        POINT("Точка", "point.png"), RHOMBUS("Ромбик", "rhombus.png"),
        SNOWFLAKE("Снежинка", "snowflake.png"), STAR("Звёздочка", "star.png"),
        TRIANGLE("Треугольник", "triangle.png");

        private final Translation displayName;
        private final Identifier identifier;
        private final Identifier smoothIdentifier;

        ParticleTexture(String name, String file) {
            this.displayName = text(name);
            String path = file.substring(0, file.length() - ".png".length());
            this.identifier = Identifier.fromNamespaceAndPath("raven", "particle/" + path);
            this.smoothIdentifier = Identifier.fromNamespaceAndPath("raven", "particle_smooth/" + path);
        }

        private TextureAtlasSprite sprite(boolean smooth) {
            Identifier spriteId = smooth ? this.smoothIdentifier : this.identifier;
            return Mc.INSTANCE.getMinecraft().getAtlasManager()
                    .getAtlasOrThrow(AtlasIds.PARTICLES).getSprite(spriteId);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    public enum ParticleRenderMode implements DisplayNamed {
        TEXTURES("Текстуры"), CUBES("Кубики"),
        TEXTURES_AND_CUBES("Текстуры и кубики"), RANDOM("Случайный пункт");

        private final Translation displayName;

        ParticleRenderMode(String name) {
            this.displayName = text(name);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    public enum MotionDirection implements DisplayNamed {
        NONE("Не использовать"), TOWARD_SOURCE("На источник"), AWAY_FROM_SOURCE("От источника"),
        CAMERA_LEFT("Влево от камеры"), CAMERA_RIGHT("Вправо от камеры"),
        CAMERA_FORWARD("Вперёд от камеры"), CAMERA_BACK("Назад от камеры"),
        SPIN_LEFT("Закручивать влево"), SPIN_RIGHT("Закручивать вправо"),
        SPIN_RANDOM("Закручивать случайно"), ALTERNATE_SOURCE("Чередовать по источнику");

        private final Translation displayName;

        MotionDirection(String name) {
            this.displayName = text(name);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    public enum SpawnDirection implements DisplayNamed {
        NO_XZ("Без толчка по XZ"), RANDOM("Случайно"),
        TOWARD_SOURCE("На источник"), AWAY_FROM_SOURCE("От источника"),
        CAMERA_LEFT("Влево от камеры"), CAMERA_RIGHT("Вправо от камеры"),
        CAMERA_FORWARD("Вперёд от камеры"), CAMERA_BACK("Назад от камеры");

        private final Translation displayName;

        SpawnDirection(String name) {
            this.displayName = text(name);
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }

    public enum CollisionMode implements DisplayNamed {
        IGNORE("Игнорировать", false, 1.0f), BOUNCE("Отскок", true, 1.0f),
        STRONG_BOUNCE("Сильный отскок", true, 1.5f), VERY_STRONG_BOUNCE("Очень сильный отскок", true, 2.0f),
        WEAK_BOUNCE("Слабый отскок", true, 0.75f), VERY_WEAK_BOUNCE("Очень слабый отскок", true, 0.5f),
        STICK("Застрять", false, 1.0f), FATAL("Фатальное столкновение", false, 1.0f),
        SLIDE("Скользить", false, 1.0f), SLIDE_SLOW("Скользить с замедлением", false, 1.0f);

        private final Translation displayName;
        private final boolean bounces;
        private final float bounceMultiplier;

        CollisionMode(String name, boolean bounces, float bounceMultiplier) {
            this.displayName = text(name);
            this.bounces = bounces;
            this.bounceMultiplier = bounceMultiplier;
        }

        @Override
        public Translation getDisplayName() {
            return this.displayName;
        }
    }
}
