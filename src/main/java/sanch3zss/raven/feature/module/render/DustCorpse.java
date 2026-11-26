package sanch3zss.raven.feature.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import sanch3zss.raven.client.Mc;

final class DustCorpse {
    private static final float DISSOLVE_START = 2.6f;
    private static final float DISSOLVE_DURATION = 1.9f;
    private static final float TILE = 0.11f;
    private static final float GRAVITY = 24.0f;
    private static final float FRICTION = 0.7f;
    private static final float STEP = 1.0f / 180.0f;
    private static final Random RANDOM = new Random();

    private final EntityModel<LivingEntityRenderState> model;
    private final LivingEntityRenderState state;
    private final Identifier texture;
    private final float facing;
    private final float modelScale;
    private final float height;
    private final float[] pose = new float[10];
    private final float windX;
    private final float windZ;
    private final BitSet gone = new BitSet();
    private final List<Dust> dust = new ArrayList<>();
    private final Vector3f centerOfMass = new Vector3f();
    private final List<Vector3f> points = new ArrayList<>();
    private final Matrix3f inverseInertia = new Matrix3f();
    private final Quaternionf orientation = new Quaternionf();
    private final Vector3f velocity = new Vector3f();
    private final Vector3f spin = new Vector3f();
    private float[] thresholds;
    private double x;
    private double y;
    private double z;
    private boolean sleeping;
    private float restTime;
    private float time;
    private float accumulator;
    private int tiles;

    @SuppressWarnings("unchecked")
    private DustCorpse(LivingEntity entity, LivingEntityRenderer<?, ?, ?> renderer, LivingEntityRenderState state,
                       Identifier texture, Vec3 from) {
        this.model = (EntityModel<LivingEntityRenderState>) renderer.getModel();
        this.state = state;
        this.texture = texture;
        this.facing = state.bodyRot;
        this.modelScale = Math.max(0.01f, state.scale * state.ageScale);
        this.height = Math.max(0.3f, entity.getBbHeight());
        float away = from == null ? RANDOM.nextFloat() * 360.0f
                : (float) Math.toDegrees(Math.atan2(-(entity.getX() - from.x), entity.getZ() - from.z));
        float fallYaw = (away + (RANDOM.nextFloat() - 0.5f) * 50.0f) * Mth.DEG_TO_RAD;
        float windYaw = fallYaw + (RANDOM.nextFloat() - 0.5f) * 2.0f;
        this.windX = -Mth.sin(windYaw);
        this.windZ = Mth.cos(windYaw);
        float[] ranges = {
                -0.7f, 0.2f, -0.8f, 0.8f,
                -3.0f, -0.6f, -0.3f, 1.3f,
                -3.0f, -0.6f, -1.3f, 0.3f,
        };
        for (int i = 0; i < 6; i++) {
            pose[i] = Mth.lerp(RANDOM.nextFloat(), ranges[i * 2], ranges[i * 2 + 1]);
        }
        pose[6] = Mth.lerp(RANDOM.nextFloat(), -0.9f, 0.3f);
        pose[7] = RANDOM.nextFloat() * 0.35f;
        pose[8] = Mth.lerp(RANDOM.nextFloat(), -0.9f, 0.3f);
        pose[9] = -RANDOM.nextFloat() * 0.35f;

        buildShape();
        this.x = entity.getX() + centerOfMass.x;
        this.y = entity.getY() + centerOfMass.y;
        this.z = entity.getZ() + centerOfMass.z;
        Vector3f direction = new Vector3f(-Mth.sin(fallYaw), 0.0f, Mth.cos(fallYaw));
        float push = 1.6f + RANDOM.nextFloat();
        velocity.set(direction).mul(push).add(0.0f, 1.2f + RANDOM.nextFloat() * 0.8f, 0.0f);
        spin.set(0.0f, 1.0f, 0.0f).cross(direction).mul(3.0f + RANDOM.nextFloat() * 2.0f)
                .add((RANDOM.nextFloat() - 0.5f) * 2.0f, (RANDOM.nextFloat() - 0.5f) * 2.0f, (RANDOM.nextFloat() - 0.5f) * 2.0f);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static DustCorpse capture(LivingEntity entity, Vec3 from) {
        try {
            Object entityRenderer = Mc.INSTANCE.getEntityRenderDispatcher().getRenderer(entity);
            if (!(entityRenderer instanceof LivingEntityRenderer renderer)) return null;
            LivingEntityRenderState state = (LivingEntityRenderState) renderer.createRenderState();
            renderer.extractRenderState(entity, state, Mc.INSTANCE.getTickDelta());
            state.deathTime = 0.0f;
            state.walkAnimationSpeed = 0.0f;
            Identifier texture = renderer.getTextureLocation(state);
            if (texture == null) return null;
            return new DustCorpse(entity, renderer, state, texture, from);
        } catch (Throwable ignored) {
            return null;
        }
    }

    boolean finished() {
        return time > DISSOLVE_START + DISSOLVE_DURATION && dust.isEmpty();
    }

    private PoseStack standingTransform() {
        PoseStack stack = new PoseStack();
        stack.mulPose(Axis.YP.rotationDegrees(180.0f - facing));
        stack.scale(-modelScale, -modelScale, modelScale);
        stack.translate(0.0f, -1.501f, 0.0f);
        return stack;
    }

    private void buildShape() {
        applyPose(1.0f);
        List<float[]> cubes = new ArrayList<>();
        model.root().visit(standingTransform(), (entry, path, index, cube) -> {
            Matrix4f matrix = entry.pose();
            float[] data = new float[28];
            int corner = 0;
            for (int i = 0; i < 8; i++) {
                Vector3f point = new Vector3f((i & 1) == 0 ? cube.minX : cube.maxX, (i & 2) == 0 ? cube.minY : cube.maxY,
                        (i & 4) == 0 ? cube.minZ : cube.maxZ).div(16.0f).mulPosition(matrix);
                data[corner++] = point.x;
                data[corner++] = point.y;
                data[corner++] = point.z;
            }
            float volume = Math.abs((cube.maxX - cube.minX) * (cube.maxY - cube.minY) * (cube.maxZ - cube.minZ))
                    / 4096.0f * modelScale * modelScale * modelScale;
            data[24] = Math.max(volume, 1.0e-5f);
            cubes.add(data);
        });
        float total = 0.0f;
        for (float[] cube : cubes) {
            float cx = 0.0f, cy = 0.0f, cz = 0.0f;
            for (int i = 0; i < 8; i++) {
                cx += cube[i * 3] / 8.0f;
                cy += cube[i * 3 + 1] / 8.0f;
                cz += cube[i * 3 + 2] / 8.0f;
            }
            cube[25] = cx;
            cube[26] = cy;
            cube[27] = cz;
            centerOfMass.add(cx * cube[24], cy * cube[24], cz * cube[24]);
            total += cube[24];
        }
        if (total <= 0.0f) {
            centerOfMass.set(0.0f, height * 0.5f, 0.0f);
            inverseInertia.identity().scale(10.0f);
            points.add(new Vector3f(0.0f, -height * 0.5f, 0.0f));
            return;
        }
        centerOfMass.div(total);
        Matrix3f inertia = new Matrix3f().zero();
        for (float[] cube : cubes) {
            float mass = cube[24] / total;
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
            for (int i = 0; i < 8; i++) {
                float px = cube[i * 3], py = cube[i * 3 + 1], pz = cube[i * 3 + 2];
                minX = Math.min(minX, px);
                minY = Math.min(minY, py);
                minZ = Math.min(minZ, pz);
                maxX = Math.max(maxX, px);
                maxY = Math.max(maxY, py);
                maxZ = Math.max(maxZ, pz);
                points.add(new Vector3f(px, py, pz).sub(centerOfMass));
            }
            float w = maxX - minX, h = maxY - minY, d = maxZ - minZ;
            float ox = cube[25] - centerOfMass.x, oy = cube[26] - centerOfMass.y, oz = cube[27] - centerOfMass.z;
            float lengthSquared = ox * ox + oy * oy + oz * oz;
            inertia.m00 += mass * ((h * h + d * d) / 12.0f + lengthSquared - ox * ox);
            inertia.m11 += mass * ((w * w + d * d) / 12.0f + lengthSquared - oy * oy);
            inertia.m22 += mass * ((w * w + h * h) / 12.0f + lengthSquared - oz * oz);
            inertia.m01 -= mass * ox * oy;
            inertia.m02 -= mass * ox * oz;
            inertia.m12 -= mass * oy * oz;
        }
        inertia.m10 = inertia.m01;
        inertia.m20 = inertia.m02;
        inertia.m21 = inertia.m12;
        inertia.m00 += 0.002f;
        inertia.m11 += 0.002f;
        inertia.m22 += 0.002f;
        inverseInertia.set(inertia).invert();
    }

    private void applyPose(float progress) {
        model.setupAnim(state);
        if (!(model instanceof HumanoidModel<?> humanoid)) return;
        float t = progress * progress * (3.0f - 2.0f * progress);
        humanoid.head.xRot = Mth.lerp(t, humanoid.head.xRot, pose[0]);
        humanoid.head.yRot = Mth.lerp(t, humanoid.head.yRot, pose[1]);
        humanoid.rightArm.xRot = Mth.lerp(t, humanoid.rightArm.xRot, pose[2]);
        humanoid.rightArm.zRot = Mth.lerp(t, humanoid.rightArm.zRot, pose[3]);
        humanoid.leftArm.xRot = Mth.lerp(t, humanoid.leftArm.xRot, pose[4]);
        humanoid.leftArm.zRot = Mth.lerp(t, humanoid.leftArm.zRot, pose[5]);
        humanoid.rightLeg.xRot = Mth.lerp(t, humanoid.rightLeg.xRot, pose[6]);
        humanoid.rightLeg.zRot = Mth.lerp(t, humanoid.rightLeg.zRot, pose[7]);
        humanoid.leftLeg.xRot = Mth.lerp(t, humanoid.leftLeg.xRot, pose[8]);
        humanoid.leftLeg.zRot = Mth.lerp(t, humanoid.leftLeg.zRot, pose[9]);
    }

    private void simulate(float delta) {
        accumulator = Math.min(accumulator + delta, 0.1f);
        while (accumulator >= STEP) {
            accumulator -= STEP;
            step(STEP);
        }
    }

    private void step(float dt) {
        ClientLevel level = Mc.INSTANCE.getWorld();
        if (sleeping || level == null) return;
        velocity.y -= GRAVITY * dt;
        x += velocity.x * dt;
        y += velocity.y * dt;
        z += velocity.z * dt;
        float angularSpeed = spin.length();
        if (angularSpeed > 1.0e-6f) {
            orientation.premul(new Quaternionf().fromAxisAngleRad(spin.x / angularSpeed, spin.y / angularSpeed,
                    spin.z / angularSpeed, angularSpeed * dt)).normalize();
        }
        Matrix3f rotation = new Matrix3f().set(orientation);
        Matrix3f worldInverse = new Matrix3f(rotation).mul(inverseInertia).mul(new Matrix3f(rotation).transpose());
        Vector3f arm = new Vector3f();
        Vector3f pointVelocity = new Vector3f();
        Vector3f impulse = new Vector3f();
        Vector3f temp = new Vector3f();
        float deepest = 0.0f;
        boolean touching = false;
        for (int iteration = 0; iteration < 3; iteration++) {
            for (Vector3f local : points) {
                rotation.transform(local, arm);
                double px = x + arm.x, py = y + arm.y, pz = z + arm.z;
                double top = groundTop(level, px, py, pz);
                if (py >= top) continue;
                touching = true;
                deepest = Math.max(deepest, (float) (top - py));
                pointVelocity.set(spin).cross(arm).add(velocity);
                float normalSpeed = pointVelocity.y;
                if (normalSpeed >= 0.0f) continue;
                float normalMass = 1.0f + impulseFactor(worldInverse, arm, 0.0f, 1.0f, 0.0f, temp);
                float restitution = normalSpeed < -2.0f ? 0.2f : 0.0f;
                float normalImpulse = -(1.0f + restitution) * normalSpeed / normalMass;
                applyImpulse(worldInverse, arm, impulse.set(0.0f, normalImpulse, 0.0f), temp);
                pointVelocity.set(spin).cross(arm).add(velocity);
                float tangentX = pointVelocity.x, tangentZ = pointVelocity.z;
                float tangentSpeed = (float) Math.sqrt(tangentX * tangentX + tangentZ * tangentZ);
                if (tangentSpeed < 1.0e-4f) continue;
                tangentX /= tangentSpeed;
                tangentZ /= tangentSpeed;
                float tangentMass = 1.0f + impulseFactor(worldInverse, arm, tangentX, 0.0f, tangentZ, temp);
                float frictionImpulse = Math.min(tangentSpeed / tangentMass, FRICTION * normalImpulse);
                applyImpulse(worldInverse, arm, impulse.set(-tangentX * frictionImpulse, 0.0f, -tangentZ * frictionImpulse), temp);
            }
        }
        y += deepest;
        spin.mul((float) Math.exp(-(touching ? 2.0 : 0.3) * dt));
        if (touching) {
            velocity.x *= (float) Math.exp(-1.0 * dt);
            velocity.z *= (float) Math.exp(-1.0 * dt);
        }
        if (touching && velocity.length() < 0.08f && spin.length() < 0.3f) {
            restTime += dt;
            if (restTime > 0.3f) {
                sleeping = true;
                velocity.zero();
                spin.zero();
            }
        } else {
            restTime = 0.0f;
        }
    }

    private static float impulseFactor(Matrix3f worldInverse, Vector3f arm, float nx, float ny, float nz, Vector3f temp) {
        Vector3f normal = new Vector3f(nx, ny, nz);
        temp.set(arm).cross(normal);
        worldInverse.transform(temp);
        temp.cross(arm);
        return temp.dot(normal);
    }

    private void applyImpulse(Matrix3f worldInverse, Vector3f arm, Vector3f impulse, Vector3f temp) {
        velocity.add(impulse);
        temp.set(arm).cross(impulse);
        worldInverse.transform(temp);
        spin.add(temp);
    }

    private static double groundTop(ClientLevel level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (shape.isEmpty()) return Double.NEGATIVE_INFINITY;
        double top = pos.getY() + shape.max(Direction.Axis.Y);
        return top - y > 0.6 ? Double.NEGATIVE_INFINITY : top;
    }

    void submit(float delta, PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
        time += delta;
        simulate(delta);
        ClientLevel level = Mc.INSTANCE.getWorld();
        int light = level == null ? LightCoordsUtil.FULL_BRIGHT
                : LightCoordsUtil.getLightCoords(level, BlockPos.containing(x, y + 0.2, z));
        if (time < DISSOLVE_START + DISSOLVE_DURATION) {
            float[] body = buildBody(camera);
            if (body.length > 0) {
                collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(texture),
                        (pose, buffer) -> emit(pose, buffer, body, light, 1.0f));
            }
        }
        float[] ash = buildDust(delta, camera);
        if (ash.length > 0) {
            collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(KillEffectsModule.WHITE_TEXTURE),
                    (pose, buffer) -> emit(pose, buffer, ash, light, -1.0f));
        }
    }

    private float[] buildBody(Vec3 camera) {
        applyPose(Mth.clamp(time / 0.35f, 0.0f, 1.0f));
        Matrix3f rotation = new Matrix3f().set(orientation);
        PoseStack stack = new PoseStack();
        stack.translate(x - camera.x, y - camera.y, z - camera.z);
        stack.mulPose(orientation);
        stack.translate(-centerOfMass.x, -centerOfMass.y, -centerOfMass.z);
        stack.mulPose(Axis.YP.rotationDegrees(180.0f - facing));
        stack.scale(-modelScale, -modelScale, modelScale);
        stack.translate(0.0f, -1.501f, 0.0f);

        float progress = time < DISSOLVE_START ? -1.0f : (time - DISSOLVE_START) / DISSOLVE_DURATION * 1.1f;
        Vector3f axis = rotation.transform(new Vector3f(0.0f, 1.0f, 0.0f));
        Vector3f feet = rotation.transform(new Vector3f(centerOfMass).negate())
                .add((float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z));
        VertexData out = new VertexData();
        tiles = 0;
        Vector3f[] corners = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
        Vector3f normal = new Vector3f();
        Vector3f center = new Vector3f();
        model.root().visit(stack, (entry, path, index, cube) -> {
            Matrix4f matrix = entry.pose();
            for (ModelPart.Polygon polygon : cube.polygons) {
                ModelPart.Vertex[] vertices = polygon.vertices();
                if (vertices.length < 4) continue;
                for (int i = 0; i < 4; i++) {
                    corners[i].set(vertices[i].worldX(), vertices[i].worldY(), vertices[i].worldZ()).mulPosition(matrix);
                }
                normal.set(polygon.normal()).mul(entry.normal()).normalize();
                int across = Math.max(1, Mth.ceil(dist(vertices[0], vertices[1]) * modelScale / TILE));
                int down = Math.max(1, Mth.ceil(dist(vertices[1], vertices[2]) * modelScale / TILE));
                for (int i = 0; i < across; i++) {
                    for (int j = 0; j < down; j++) {
                        int id = tiles++;
                        if (gone.get(id)) continue;
                        float s0 = (float) i / across, s1 = (float) (i + 1) / across;
                        float t0 = (float) j / down, t1 = (float) (j + 1) / down;
                        float ash = 0.0f;
                        if (progress >= 0.0f) {
                            bilinear(corners, (s0 + s1) * 0.5f, (t0 + t1) * 0.5f, center);
                            float threshold = threshold(id, center, feet, axis);
                            if (progress > threshold) {
                                gone.set(id);
                                spawnDust(center, camera);
                                continue;
                            }
                            float front = (1.0f - Mth.clamp(new Vector3f(center).sub(feet).dot(axis) / height, 0.0f, 1.0f)) * 0.75f;
                            ash = Mth.clamp((progress - front + 0.3f) / 0.3f, 0.0f, 1.0f) * 0.7f;
                        }
                        float red = Mth.lerp(ash, 1.0f, 0.4f);
                        float green = Mth.lerp(ash, 1.0f, 0.36f);
                        float blue = Mth.lerp(ash, 1.0f, 0.33f);
                        corner(out, corners, vertices, s1, t0, red, green, blue, normal);
                        corner(out, corners, vertices, s0, t0, red, green, blue, normal);
                        corner(out, corners, vertices, s0, t1, red, green, blue, normal);
                        corner(out, corners, vertices, s1, t1, red, green, blue, normal);
                    }
                }
            }
        });
        return out.toArray();
    }

    private float[] buildDust(float delta, Vec3 camera) {
        if (dust.isEmpty()) return new float[0];
        org.joml.Quaternionfc cameraRotation = Mc.INSTANCE.getCamera().rotation();
        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(cameraRotation);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(cameraRotation);
        VertexData out = new VertexData();
        Iterator<Dust> iterator = dust.iterator();
        while (iterator.hasNext()) {
            Dust particle = iterator.next();
            particle.age += delta;
            if (particle.age >= particle.lifetime) {
                iterator.remove();
                continue;
            }
            float drag = (float) Math.pow(0.35, delta);
            particle.velocityX = particle.velocityX * drag + windX * delta * 0.8f + Mth.sin(particle.age * 6.0f + particle.phase) * delta * 0.6f;
            particle.velocityY = particle.velocityY * drag + delta * 0.15f;
            particle.velocityZ = particle.velocityZ * drag + windZ * delta * 0.8f + Mth.cos(particle.age * 5.0f + particle.phase) * delta * 0.6f;
            particle.x += particle.velocityX * delta;
            particle.y += particle.velocityY * delta;
            particle.z += particle.velocityZ * delta;
            float life = particle.age / particle.lifetime;
            float alpha = (1.0f - life) * Math.min(1.0f, particle.age * 8.0f);
            float half = particle.size * (1.0f - life * 0.6f) * 0.5f;
            float px = (float) (particle.x - camera.x), py = (float) (particle.y - camera.y), pz = (float) (particle.z - camera.z);
            float red = 0.42f * particle.tint, green = 0.38f * particle.tint, blue = 0.34f * particle.tint;
            for (int k = 0; k < 4; k++) {
                float sx = k == 0 || k == 1 ? -half : half;
                float sy = k == 0 || k == 3 ? half : -half;
                out.add(px + right.x * sx + up.x * sy, py + right.y * sx + up.y * sy, pz + right.z * sx + up.z * sy,
                        0.5f, 0.5f, red, green, blue, 0.0f, 1.0f, 0.0f, alpha);
            }
        }
        return out.toArray();
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer buffer, float[] data, int light, float alphaOverride) {
        for (int i = 0; i + 11 < data.length; i += 12) {
            float alpha = alphaOverride >= 0.0f ? alphaOverride : data[i + 11];
            buffer.addVertex(pose, data[i], data[i + 1], data[i + 2])
                    .setColor(data[i + 5], data[i + 6], data[i + 7], alpha)
                    .setUv(data[i + 3], data[i + 4])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, data[i + 8], data[i + 9], data[i + 10]);
        }
    }

    private float threshold(int id, Vector3f center, Vector3f feet, Vector3f axis) {
        if (thresholds == null || thresholds.length <= id) {
            float[] grown = new float[Math.max(256, (id + 1) * 2)];
            Arrays.fill(grown, Float.NaN);
            if (thresholds != null) System.arraycopy(thresholds, 0, grown, 0, thresholds.length);
            thresholds = grown;
        }
        if (Float.isNaN(thresholds[id])) {
            float along = new Vector3f(center).sub(feet).dot(axis) / height;
            thresholds[id] = (1.0f - Mth.clamp(along, 0.0f, 1.0f)) * 0.75f + RANDOM.nextFloat() * 0.25f;
        }
        return thresholds[id];
    }

    private void spawnDust(Vector3f center, Vec3 camera) {
        for (int k = 0; k < 3; k++) {
            float speed = 0.6f + RANDOM.nextFloat() * 0.9f;
            dust.add(new Dust(center.x + camera.x + (RANDOM.nextFloat() - 0.5f) * TILE,
                    center.y + camera.y + (RANDOM.nextFloat() - 0.5f) * TILE,
                    center.z + camera.z + (RANDOM.nextFloat() - 0.5f) * TILE,
                    windX * speed + (RANDOM.nextFloat() - 0.5f) * 0.4f,
                    0.25f + RANDOM.nextFloat() * 0.5f,
                    windZ * speed + (RANDOM.nextFloat() - 0.5f) * 0.4f));
        }
    }

    private static void corner(VertexData out, Vector3f[] corners, ModelPart.Vertex[] vertices, float s, float t,
                               float red, float green, float blue, Vector3f normal) {
        Vector3f point = new Vector3f();
        bilinear(corners, s, t, point);
        float u = Mth.lerp(t, Mth.lerp(s, vertices[0].u(), vertices[1].u()), Mth.lerp(s, vertices[3].u(), vertices[2].u()));
        float v = Mth.lerp(t, Mth.lerp(s, vertices[0].v(), vertices[1].v()), Mth.lerp(s, vertices[3].v(), vertices[2].v()));
        out.add(point.x, point.y, point.z, u, v, red, green, blue, normal.x, normal.y, normal.z, 1.0f);
    }

    private static void bilinear(Vector3f[] corners, float s, float t, Vector3f result) {
        float ax = Mth.lerp(s, corners[0].x, corners[1].x), ay = Mth.lerp(s, corners[0].y, corners[1].y);
        float az = Mth.lerp(s, corners[0].z, corners[1].z);
        float bx = Mth.lerp(s, corners[3].x, corners[2].x), by = Mth.lerp(s, corners[3].y, corners[2].y);
        float bz = Mth.lerp(s, corners[3].z, corners[2].z);
        result.set(Mth.lerp(t, ax, bx), Mth.lerp(t, ay, by), Mth.lerp(t, az, bz));
    }

    private static float dist(ModelPart.Vertex a, ModelPart.Vertex b) {
        float dx = a.worldX() - b.worldX(), dy = a.worldY() - b.worldY(), dz = a.worldZ() - b.worldZ();
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static final class VertexData {
        private float[] data = new float[1024];
        private int size;

        private void add(float... values) {
            if (size + values.length > data.length) data = Arrays.copyOf(data, Math.max(data.length * 2, size + values.length));
            System.arraycopy(values, 0, data, size, values.length);
            size += values.length;
        }

        private float[] toArray() {
            return Arrays.copyOf(data, size);
        }
    }

    private static final class Dust {
        private double x;
        private double y;
        private double z;
        private float velocityX;
        private float velocityY;
        private float velocityZ;
        private float age;
        private final float lifetime = 1.6f + RANDOM.nextFloat() * 1.2f;
        private final float size = 0.035f + RANDOM.nextFloat() * 0.035f;
        private final float tint = 0.8f + RANDOM.nextFloat() * 0.4f;
        private final float phase = RANDOM.nextFloat() * Mth.TWO_PI;

        private Dust(double x, double y, double z, float velocityX, float velocityY, float velocityZ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
        }
    }
}
