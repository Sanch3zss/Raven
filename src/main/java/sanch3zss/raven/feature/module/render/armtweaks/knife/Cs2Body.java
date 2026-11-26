package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;
import sanch3zss.raven.feature.module.render.chinahat.ChinaHatRenderLayer;

public final class Cs2Body {
    public static final Cs2Body INSTANCE = new Cs2Body();
    static final String DEFAULT_AGENT = "ctm_sas";
    private static final String[] DIRECTIONS = {"n", "ne", "e", "se", "s", "sw", "w", "nw"};
    private static final String[] AIR_DIRECTIONS = {"n", "e", "s", "w"};
    private static final String[] BENDS = {"spine_0", "spine_1", "spine_2", "spine_3", "neck_0", "head_0"};
    private static final float[] TWIST_STEPS = {0.35f, 0.35f, 0.3f, 0.0f, 0.0f, 0.0f};
    private static final float[] PITCH_STEPS = {0.0f, 0.15f, 0.15f, 0.2f, 0.2f, 0.3f};
    private static final float WALK_SPEED = 2.8f;
    private static final float RUN_SPEED = 5.0f;
    private static final float CROUCH_SPEED = 2.23f;
    private final Map<String, Map<String, Cs2Clip.Local>> samples = new HashMap<>();
    private final Map<String, String> bendOwners = new HashMap<>();
    private final Map<String, Matrix4f> gripOffsets = new HashMap<>();
    private Vector3f headUp;
    private Map<String, Cs2Clip.Local> locals;
    private long lastFrame;
    private float bodyYaw;
    private float yawSpeed;
    private final Foot[] feet = {new Foot("L"), new Foot("R")};
    private float ikWeight;
    private float phase;
    private float speed;
    private float moveWeight;
    private float runWeight;
    private float crouchWeight;
    private float airWeight;
    private float airTime;
    private boolean jumping;
    private boolean wasOnGround = true;
    private Vec3 velocity = Vec3.ZERO;
    private Vec3 lastPosition;
    private Vec3 offset = Vec3.ZERO;
    private String knife;
    private boolean armed;
    private float gripWeight;
    private String action;
    private Cs2Clip actionClip;
    private float actionTime;
    private float actionWeight;
    private int lightIndex;
    private Item equippedItem;
    private int equippedSlot = -1;

    private Cs2Body() {}

    float bodyYaw() {
        return bodyYaw;
    }

    Vec3 offset() {
        return offset;
    }

    boolean update(LocalPlayer player, float partialTick, float back) {
        if (clip("idle") == null) return false;
        long now = System.nanoTime();
        if (lastFrame != 0L && now - lastFrame < 500_000L && locals != null) return true;
        float headYaw = player.getViewYRot(partialTick) * Mth.DEG_TO_RAD;
        offset = new Vec3(Mth.sin(headYaw) * back, 0.0, -Mth.cos(headYaw) * back);
        boolean fresh = lastFrame == 0L || now - lastFrame > 100_000_000L;
        float delta = fresh ? 0.0f : Math.min(0.1f, (now - lastFrame) * 1.0e-9f);
        if (fresh) {
            bodyYaw = player.getViewYRot(partialTick);
            velocity = Vec3.ZERO;
            yawSpeed = 0.0f;
            lastPosition = null;
            action = null;
            for (Foot foot : feet) {
                foot.locked = false;
                foot.stepping = false;
            }
        }
        lastFrame = now;
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        String model = module.activeKnifeVariant().model;
        if (!model.equals(knife)) {
            knife = model;
            equippedItem = null;
        }
        armed = KnifeRenderer.eligible(InteractionHand.MAIN_HAND, player.getMainHandItem());
        gripWeight = approach(gripWeight, armed ? 1.0f : 0.0f, delta / 0.2f);
        advance(player, partialTick, delta);
        updateAction(player, delta);
        locals = pose();
        plantFeet(delta);
        return true;
    }

    private void plantFeet(float delta) {
        boolean planted = wasOnGround && airWeight <= 0.0f && speed < 0.3f && lastPosition != null;
        ikWeight = approach(ikWeight, planted ? 1.0f : 0.0f, delta / 0.15f);
        if (ikWeight <= 0.0f || lastPosition == null) {
            for (Foot foot : feet) foot.locked = false;
            return;
        }
        Map<String, Matrix4f> world = Cs2Clip.compose(locals);
        Vector3f[] desired = new Vector3f[feet.length];
        for (int i = 0; i < feet.length; i++) {
            Foot foot = feet[i];
            Matrix4f ankle = world.get("ankle_" + foot.side);
            if (ankle == null || !world.containsKey("leg_upper_" + foot.side) || !world.containsKey("leg_lower_" + foot.side)) return;
            desired[i] = toGround(ankle.getTranslation(new Vector3f()));
            if (!foot.locked) {
                foot.position.set(desired[i]);
                foot.yaw = bodyYaw;
                foot.locked = true;
                foot.stepping = false;
            }
        }
        float stepDuration = Mth.clamp(0.26f - Math.abs(yawSpeed) / 1500.0f, 0.12f, 0.26f);
        for (int i = 0; i < feet.length; i++) {
            Foot foot = feet[i];
            if (foot.stepping) continue;
            Vector3f hip = world.get("leg_upper_" + foot.side).getTranslation(new Vector3f());
            Vector3f knee = world.get("leg_lower_" + foot.side).getTranslation(new Vector3f());
            Vector3f ankle = world.get("ankle_" + foot.side).getTranslation(new Vector3f());
            if (toModel(foot.position).distance(hip) > 0.95f * (hip.distance(knee) + knee.distance(ankle))) startStep(foot);
        }
        if (!feet[0].stepping && !feet[1].stepping) {
            int worst = -1;
            float worstError = 1.0f;
            for (int i = 0; i < feet.length; i++) {
                float drift = (float) Math.hypot(feet[i].position.x - desired[i].x, feet[i].position.z - desired[i].z) / 0.1f;
                float error = Math.max(drift, Math.abs(Mth.wrapDegrees(bodyYaw - feet[i].yaw)) / 25.0f);
                if (error > worstError) {
                    worst = i;
                    worstError = error;
                }
            }
            if (worst >= 0) startStep(feet[worst]);
        }
        for (int i = 0; i < feet.length; i++) {
            Foot foot = feet[i];
            if (foot.stepping) {
                foot.stepTime = Math.min(1.0f, foot.stepTime + delta / stepDuration);
                float t = foot.stepTime * foot.stepTime * (3.0f - 2.0f * foot.stepTime);
                foot.from.lerp(desired[i], t, foot.position);
                foot.position.y += 0.06f * Mth.sin(foot.stepTime * Mth.PI);
                foot.yaw = foot.fromYaw + Mth.wrapDegrees(bodyYaw - foot.fromYaw) * t;
                if (foot.stepTime >= 1.0f) {
                    foot.stepping = false;
                    foot.position.set(desired[i]);
                    foot.yaw = bodyYaw;
                }
            }
            solveLeg(world, foot.side, toModel(foot.position), Mth.wrapDegrees(bodyYaw - foot.yaw), ikWeight);
        }
    }

    private static void startStep(Foot foot) {
        foot.stepping = true;
        foot.stepTime = 0.0f;
        foot.from.set(foot.position);
        foot.fromYaw = foot.yaw;
    }

    private Vector3f toGround(Vector3f model) {
        Vec3 origin = lastPosition.add(offset);
        return model.rotateY(-bodyYaw * Mth.DEG_TO_RAD, new Vector3f())
                .add((float) origin.x, (float) origin.y, (float) origin.z);
    }

    private Vector3f toModel(Vector3f ground) {
        Vec3 origin = lastPosition.add(offset);
        return new Vector3f(ground).sub((float) origin.x, (float) origin.y, (float) origin.z)
                .rotateY(bodyYaw * Mth.DEG_TO_RAD);
    }

    private void solveLeg(Map<String, Matrix4f> world, String side, Vector3f target, float yawOffset, float weight) {
        Matrix4f upper = world.get("leg_upper_" + side);
        Matrix4f lower = world.get("leg_lower_" + side);
        Matrix4f ankle = world.get("ankle_" + side);
        Vector3f hip = upper.getTranslation(new Vector3f());
        Vector3f knee = lower.getTranslation(new Vector3f());
        Vector3f foot = ankle.getTranslation(new Vector3f());
        float thigh = hip.distance(knee);
        float shin = knee.distance(foot);
        Vector3f goal = new Vector3f(foot).lerp(target, weight);
        Vector3f reach = goal.sub(hip, new Vector3f());
        float distance = Mth.clamp(reach.length(), Math.abs(thigh - shin) + 1.0e-3f, thigh + shin - 1.0e-3f);
        Vector3f direction = reach.normalize(new Vector3f());
        goal.set(direction).mul(distance).add(hip);
        Vector3f pole = knee.sub(hip, new Vector3f());
        pole.fma(-pole.dot(direction), direction);
        if (pole.lengthSquared() < 1.0e-8f) pole.set(0.0f, 0.0f, 1.0f).fma(-direction.z, direction);
        pole.normalize();
        float cos = Mth.clamp((thigh * thigh + distance * distance - shin * shin) / (2.0f * thigh * distance), -1.0f, 1.0f);
        Vector3f bentKnee = new Vector3f(direction).mul(thigh * cos).fma(thigh * (float) Math.sqrt(1.0f - cos * cos), pole).add(hip);

        Quaternionf hipTurn = new Quaternionf().rotationTo(knee.sub(hip, new Vector3f()), bentKnee.sub(hip, new Vector3f()));
        Matrix4f hipPivot = new Matrix4f().translation(hip).rotate(hipTurn).translate(-hip.x, -hip.y, -hip.z);
        Matrix4f upperNew = new Matrix4f(hipPivot).mul(upper);
        Matrix4f lowerMid = new Matrix4f(hipPivot).mul(lower);
        Vector3f footMid = hipPivot.transformPosition(new Vector3f(foot));
        Quaternionf kneeTurn = new Quaternionf().rotationTo(footMid.sub(bentKnee, new Vector3f()), goal.sub(bentKnee, new Vector3f()));
        Matrix4f lowerNew = new Matrix4f().translation(bentKnee).rotate(kneeTurn)
                .translate(-bentKnee.x, -bentKnee.y, -bentKnee.z).mul(lowerMid);
        Quaternionf footRotation = new Quaternionf().rotationY(yawOffset * weight * Mth.DEG_TO_RAD)
                .mul(ankle.getUnnormalizedRotation(new Quaternionf()).normalize());
        Matrix4f ankleNew = new Matrix4f().translationRotate(goal, footRotation);

        Cs2Clip.Local upperLocal = locals.get("leg_upper_" + side);
        Matrix4f parent = upperLocal == null || upperLocal.parent == null ? null : world.get(upperLocal.parent);
        if (parent == null) return;
        setLocal("leg_upper_" + side, new Matrix4f(parent).invert().mul(upperNew));
        setLocal("leg_lower_" + side, new Matrix4f(upperNew).invert().mul(lowerNew));
        setLocal("ankle_" + side, new Matrix4f(lowerNew).invert().mul(ankleNew));
    }

    private void setLocal(String bone, Matrix4f matrix) {
        Cs2Clip.Local local = locals.get(bone);
        if (local == null) return;
        matrix.getTranslation(local.translation);
        matrix.getUnnormalizedRotation(local.rotation).normalize();
    }

    private static final class Foot {
        final String side;
        final Vector3f position = new Vector3f();
        final Vector3f from = new Vector3f();
        boolean locked;
        boolean stepping;
        float stepTime;
        float yaw;
        float fromYaw;

        Foot(String side) {
            this.side = side;
        }
    }

    Map<String, Matrix4f> world(float twist, float pitch) {
        Map<String, Matrix4f> world = Cs2Clip.compose(locals);
        Matrix4f[] corrections = new Matrix4f[BENDS.length];
        Matrix4f current = new Matrix4f();
        for (int i = 0; i < BENDS.length; i++) {
            Matrix4f bone = world.get(BENDS[i]);
            if (bone != null) {
                Vector3f pivot = current.transformPosition(bone.getTranslation(new Vector3f()));
                Vector3f axis = new Vector3f(1.0f, 0.0f, 0.0f).rotateY(-twist * Mth.DEG_TO_RAD);
                Matrix4f step = new Matrix4f().translation(pivot)
                        .rotateY(-twist * TWIST_STEPS[i] * Mth.DEG_TO_RAD)
                        .rotate(pitch * PITCH_STEPS[i] * Mth.DEG_TO_RAD, axis.x, axis.y, axis.z)
                        .translate(-pivot.x, -pivot.y, -pivot.z);
                current = step.mul(current);
            }
            corrections[i] = new Matrix4f(current);
        }
        for (Map.Entry<String, Matrix4f> entry : world.entrySet()) {
            String owner = bendOwners.computeIfAbsent(entry.getKey(), this::owner);
            if (owner == null) continue;
            int index = owner.equals("wpnPivot") ? 3 : indexOf(owner);
            corrections[index].mul(entry.getValue(), entry.getValue());
        }
        return world;
    }

    private String owner(String bone) {
        String current = bone;
        String root = bone;
        while (current != null) {
            if (current.equals("wpnPivot") || indexOf(current) >= 0) return current;
            root = current;
            Cs2Clip.Local local = locals.get(current);
            current = local == null ? null : local.parent;
        }
        return root.equals("worldmodel") ? null : "wpnPivot";
    }

    private static int indexOf(String bone) {
        for (int i = 0; i < BENDS.length; i++) if (BENDS[i].equals(bone)) return i;
        return -1;
    }

    public boolean submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                          Vec3 renderOffset, boolean gui) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || state.id != player.getId() || state.isInvisible) return false;
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!module.shouldRenderCs2Hands() || player.isSleeping() || player.isPassenger() || player.isFallFlying()
                || player.isVisuallySwimming() || player.isAutoSpinAttack()) return false;
        AgentCatalog.Agent agent = AgentCatalog.get().agent(module.activeAgentId());
        if (agent == null || agent.body() == null) agent = AgentCatalog.get().agent(DEFAULT_AGENT);
        if (agent == null || agent.body() == null) return false;
        Cs2Mesh body = Cs2Viewmodel.INSTANCE.mesh("agents/meshes/" + agent.body());
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        if (body == null || !update(player, partialTick, 0.0f)) return false;
        ViewmodelLighting.INSTANCE.update(player, partialTick);

        float yaw = gui ? state.bodyRot : bodyYaw;
        float twist = gui ? state.yRot : Mth.wrapDegrees(player.getViewYRot(partialTick) - bodyYaw);
        Map<String, Matrix4f> world = world(Mth.clamp(twist, -90.0f, 90.0f), state.xRot);
        body.pose(world, null, false);
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0f);
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(-renderOffset.x, -renderOffset.y, -renderOffset.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        for (Cs2Mesh.Part part : body.parts) {
            Identifier texture = AgentCatalog.get().texture(part.texture());
            if (texture == null) continue;
            HandFinish finish = part.texture().contains("head") ? HandFinish.SKIN : HandFinish.CLOTH;
            collector.submitCustomGeometry(poseStack, finish.renderType(texture),
                    (pose, buffer) -> body.emit(pose, buffer, part, light, overlay, 0.5f, true));
        }
        Matrix4f head = world.get("head_0");
        Vector3f headMin = new Vector3f();
        Vector3f headMax = new Vector3f();
        Vector3f up = headUp();
        if (head != null && up != null && body.bounds("head_0", new Matrix4f(head).invert(), headMin, headMax)) {
            Vector3f center = new Vector3f(headMin).add(headMax).mul(0.5f);
            Vector3f size = new Vector3f(headMax).sub(headMin);
            float halfHeight = 0.5f * (Math.abs(up.x) * size.x + Math.abs(up.y) * size.y + Math.abs(up.z) * size.z);
            float headRadius = (size.x + size.y + size.z - 2.0f * halfHeight) / 4.0f;
            center.fma(halfHeight - headRadius * 0.55f, up);
            poseStack.pushPose();
            poseStack.mulPose(head);
            poseStack.translate(center.x, center.y, center.z);
            poseStack.mulPose(new Quaternionf().rotationTo(0.0f, 1.0f, 0.0f, up.x, up.y, up.z));
            ChinaHatRenderLayer.submitOnHead(poseStack, collector, state, headRadius * 2.1f);
            poseStack.popPose();
        }
        ItemStack held = player.getMainHandItem();
        Matrix4f hand = world.get("hand_R");
        Matrix4f gripOffset = gripOffset();
        if (gripOffset != null && action != null && actionWeight > 0.0f && samples.containsKey("action")) {
            Map<String, Matrix4f> actionWorld = Cs2Clip.compose(samples.get("action"));
            Matrix4f actionHand = actionWorld.get("hand_R");
            Matrix4f actionWeapon = actionWorld.get("wpn");
            if (actionHand != null && actionWeapon != null) {
                Matrix4f actionOffset = new Matrix4f(actionHand).invert().mul(actionWeapon);
                Vector3f translation = gripOffset.getTranslation(new Vector3f())
                        .lerp(actionOffset.getTranslation(new Vector3f()), actionWeight);
                Quaternionf rotation = gripOffset.getUnnormalizedRotation(new Quaternionf()).normalize()
                        .slerp(actionOffset.getUnnormalizedRotation(new Quaternionf()).normalize(), actionWeight);
                gripOffset = new Matrix4f().translationRotate(translation, rotation);
            }
        }
        Matrix4f wpn = hand != null && gripOffset != null ? new Matrix4f(hand).mul(gripOffset) : world.get("wpn");
        if (wpn != null && KnifeRenderer.eligible(InteractionHand.MAIN_HAND, held)) {
            KnifeVariant variant = module.activeKnifeVariant();
            Cs2Mesh knife = Cs2Viewmodel.INSTANCE.mesh("models/" + variant.model + ".mesh");
            if (knife != null) {
                KnifeSkin skin = module.activeKnifeSkin();
                RenderType knifeType = KnifeRenderer.INSTANCE.knifeRenderType(variant, skin);
                float uScale = variant.finish(skin).uScale();
                if (knife.hasBone("weapon_hand_r") && world.containsKey("hand_R") && world.containsKey("hand_L")) {
                    knife.pose(Map.of("weapon_hand_r", world.get("hand_R"), "weapon_hand_l", world.get("hand_L")), null, false);
                } else {
                    knife.pose(Map.of(), new Matrix4f(wpn).mul(knife.restWorld("weapon").invert()), false);
                }
                for (Cs2Mesh.Part part : knife.parts) {
                    collector.submitCustomGeometry(poseStack, knifeType,
                            (pose, buffer) -> knife.emit(pose, buffer, part, light, overlay, uScale, false));
                }
            }
        }
        poseStack.popPose();
        return true;
    }

    private Vector3f headUp() {
        if (headUp == null) {
            Cs2Clip idle = clip("idle");
            if (idle == null) return null;
            Map<String, Cs2Clip.Local> pose = new HashMap<>();
            idle.sample(0.0f, pose);
            Matrix4f head = Cs2Clip.compose(pose).get("head_0");
            if (head == null) return null;
            headUp = new Matrix4f(head).invert().transformDirection(new Vector3f(0.0f, 1.0f, 0.0f)).normalize();
        }
        return headUp;
    }

    private Matrix4f gripOffset() {
        if (knife == null) return null;
        return gripOffsets.computeIfAbsent(knife, ignored -> {
            Cs2Clip grip = knifeClip("idle");
            if (grip == null) grip = clip("idle");
            if (grip == null) return null;
            Map<String, Cs2Clip.Local> pose = new HashMap<>();
            grip.sample(0.0f, pose);
            Map<String, Matrix4f> world = Cs2Clip.compose(pose);
            Matrix4f gripHand = world.get("hand_R");
            Matrix4f gripWeapon = world.get("wpn");
            return gripHand == null || gripWeapon == null ? null : new Matrix4f(gripHand).invert().mul(gripWeapon);
        });
    }

    void attack(boolean heavy) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        Entity attacked = KnifeRenderer.INSTANCE.attackTarget();
        if (attacked == null && minecraft.hitResult instanceof EntityHitResult result) attacked = result.getEntity();
        boolean backstab = attacked instanceof LivingEntity target && Cs2Viewmodel.behind(player.getLookAngle(), target);
        boolean crouched = crouchWeight > 0.5f;
        boolean moving = speed > 0.5f;
        boolean second = (lightIndex++ & 1) == 1;
        if (knifeClip("light_miss1") != null) {
            boolean hit = attacked != null;
            String next = backstab ? (heavy ? "heavy_backstab" : second ? "light_backstab2" : "light_backstab")
                    : heavy ? (hit ? "heavy_hit1" : "heavy_miss1")
                    : (hit ? "light_hit" : "light_miss") + (second ? "2" : "1");
            if (!crouched || !play(next + "_crouch")) play(next);
            return;
        }
        String suffix = crouched ? (moving ? "_crouching_moving" : "_crouching") : "";
        String next;
        if (backstab) {
            next = "backstab" + suffix;
        } else if (heavy) {
            next = "frontstab" + suffix;
        } else {
            next = !crouched && moving ? (second ? "frontswing_moving_b" : "frontswing_moving")
                    : (second ? "frontswing_b" : "frontswing") + suffix;
        }
        if (!play(next)) play(next.replace("_moving", ""));
    }

    private boolean play(String next) {
        Cs2Clip found = knifeClip(next);
        if (found == null) found = clip(next);
        if (found == null) return false;
        action = next;
        actionClip = found;
        actionTime = 0.0f;
        return true;
    }

    private Cs2Clip knifeClip(String name) {
        return knife == null ? null : Cs2Viewmodel.INSTANCE.clip("body/" + knife, name);
    }

    private void updateAction(LocalPlayer player, float delta) {
        int slot = player.getInventory().getSelectedSlot();
        Item item = player.getMainHandItem().getItem();
        if (item != equippedItem || slot != equippedSlot) {
            if (armed) {
                String draw = knifeClip("draw2") != null && ThreadLocalRandom.current().nextInt(3) == 0 ? "draw2" : "draw";
                if (crouchWeight <= 0.5f || !play(draw + "_crouch")) play(draw);
            }
            equippedItem = item;
            equippedSlot = slot;
        }
        if (action == null) {
            actionWeight = 0.0f;
            return;
        }
        actionTime += delta;
        float duration = actionClip.duration;
        float fadeOut = Mth.clamp((duration - actionTime) / 0.15f, 0.0f, 1.0f);
        actionWeight = Math.min(Mth.clamp(actionTime / 0.06f, 0.0f, 1.0f), fadeOut);
        if (actionTime >= duration) action = null;
    }

    private void advance(LocalPlayer player, float partialTick, float delta) {
        Vec3 position = player.getPosition(partialTick);
        if (lastPosition != null && delta > 0.0f) {
            Vec3 target = new Vec3(position.x - lastPosition.x, 0.0, position.z - lastPosition.z).scale(1.0f / delta);
            if (target.horizontalDistance() > 30.0) target = Vec3.ZERO;
            velocity = velocity.add(target.subtract(velocity).scale(Math.min(1.0f, delta * 10.0f)));
        }
        lastPosition = position;
        speed = (float) velocity.horizontalDistance();
        float headYaw = player.getViewYRot(partialTick);

        float yawGap = Mth.wrapDegrees(headYaw - bodyYaw);
        float stiffness = Mth.lerp(Mth.clamp(speed / 1.5f, 0.0f, 1.0f), 6.0f, 14.0f);
        float step = Math.min(delta, 1.0f / 30.0f);
        for (float left = delta; left > 0.0f; left -= step) {
            float dt = Math.min(step, left);
            yawSpeed += (stiffness * stiffness * Mth.wrapDegrees(headYaw - bodyYaw) - 2.0f * stiffness * yawSpeed) * dt;
            bodyYaw += yawSpeed * dt;
        }
        yawGap = Mth.wrapDegrees(headYaw - bodyYaw);
        if (Math.abs(yawGap) > 90.0f) bodyYaw = headYaw - Math.signum(yawGap) * 90.0f;
        bodyYaw = Mth.wrapDegrees(bodyYaw);

        boolean onGround = player.onGround();
        if (wasOnGround && !onGround && player.getY() > player.yo) {
            jumping = true;
            airTime = 0.0f;
        }
        if (onGround) jumping = false;
        wasOnGround = onGround;
        airTime += delta;
        airWeight = approach(airWeight, onGround ? 0.0f : 1.0f, delta / (onGround ? 0.12f : 0.1f));
        crouchWeight = approach(crouchWeight, player.isCrouching() ? 1.0f : 0.0f, delta / 0.15f);
        runWeight = approach(runWeight, Mth.clamp((speed - WALK_SPEED) / (RUN_SPEED - WALK_SPEED), 0.0f, 1.0f), delta / 0.2f);
        moveWeight = approach(moveWeight, Mth.clamp(speed / 0.8f, 0.0f, 1.0f), delta / 0.12f);

        float standDuration = Mth.lerp(runWeight, clip("walk_n").duration, clip("run_n").duration);
        float standSpeed = Mth.lerp(runWeight, WALK_SPEED, RUN_SPEED);
        float cycle = Mth.lerp(crouchWeight, standSpeed * standDuration, CROUCH_SPEED * clip("crouch_n").duration);
        phase = (phase + delta * speed / cycle) % 1.0f;
    }

    private Map<String, Cs2Clip.Local> pose() {
        float angle = moveAngle();
        Map<String, Cs2Clip.Local> ground = null;
        if (crouchWeight < 1.0f) {
            Map<String, Cs2Clip.Local> standing = blend(directional("walk", angle), directional("run", angle), runWeight);
            ground = blend(sample("idle", 0.0f), standing, moveWeight);
        }
        if (crouchWeight > 0.0f) {
            Map<String, Cs2Clip.Local> crouching = blend(sample("idle_crouch", 0.0f), directional("crouch", angle), moveWeight);
            ground = ground == null ? crouching : blend(ground, crouching, crouchWeight);
        }
        boolean crouched = crouchWeight > 0.5f;
        Map<String, Cs2Clip.Local> result = ground;
        if (airWeight > 0.0f) {
            String jump = crouched ? "jump_crouch" : "jump";
            String fall = crouched ? "inair_crouch" : "inair";
            float jumpEnd = clip(jump + "_stand").duration;
            float fallStart = jumping ? jumpEnd - 0.1f : 0.0f;
            float fallTime = Mth.clamp(airTime - fallStart, 0.0f, clip(fall + "_stand").duration);
            Map<String, Cs2Clip.Local> air = airPose(fall, fallTime, angle, "fall");
            if (jumping && airTime < jumpEnd) {
                air = blend(airPose(jump, airTime, angle, "jump"), air, Mth.clamp((airTime - fallStart) / 0.1f, 0.0f, 1.0f));
            }
            result = blend(ground, air, airWeight);
        }
        Cs2Clip grip = knifeClip("idle");
        if (grip != null && gripWeight > 0.0f) {
            Map<String, Cs2Clip.Local> hold = sample("grip", grip, 0.0f);
            Cs2Clip crouchGrip = knifeClip("idle_crouch");
            if (crouchGrip != null && crouchWeight > 0.0f) hold = blend(hold, sample("grip_crouch", crouchGrip, 0.0f), crouchWeight);
            overlay(result, hold, gripWeight, true);
        }
        if (!armed || gripWeight < 1.0f) relaxFingers(result, 1.0f - gripWeight);
        if (action != null && actionWeight > 0.0f) {
            overlay(result, sample("action", actionClip, actionTime), actionWeight, false);
        }
        return result;
    }

    private Map<String, Cs2Clip.Local> airPose(String prefix, float time, float angle, String key) {
        Map<String, Cs2Clip.Local> air = sample(key + "_stand", clip(prefix + "_stand"), time);
        if (speed <= 0.1f) return air;
        float step = angle / 90.0f;
        int index = Mth.floor(step);
        String first = prefix + "_" + AIR_DIRECTIONS[Math.floorMod(index, 4)];
        String second = prefix + "_" + AIR_DIRECTIONS[Math.floorMod(index + 1, 4)];
        Map<String, Cs2Clip.Local> moving = blend(sample(key + "_a", clip(first), time), sample(key + "_b", clip(second), time), step - index);
        return blend(air, moving, Mth.clamp(speed / 3.0f, 0.0f, 1.0f));
    }

    private static void overlay(Map<String, Cs2Clip.Local> base, Map<String, Cs2Clip.Local> layer, float weight, boolean armsOnly) {
        for (Map.Entry<String, Cs2Clip.Local> entry : layer.entrySet()) {
            String bone = entry.getKey();
            if (lowerBody(bone) || armsOnly && !armBone(bone)) continue;
            Cs2Clip.Local target = base.get(bone);
            if (target == null) {
                target = new Cs2Clip.Local();
                target.parent = entry.getValue().parent;
                target.translation.set(entry.getValue().translation);
                target.rotation.set(entry.getValue().rotation);
                base.put(bone, target);
            } else {
                target.blend(entry.getValue(), weight);
            }
        }
    }

    private void relaxFingers(Map<String, Cs2Clip.Local> pose, float weight) {
        Cs2Clip skeleton = clip("idle");
        for (Map.Entry<String, Cs2Clip.Local> entry : pose.entrySet()) {
            if (!entry.getKey().startsWith("finger_")) continue;
            Quaternionf rest = skeleton.restRotation(entry.getKey());
            if (rest != null) entry.getValue().rotation.slerp(rest, weight).normalize();
        }
    }

    private static boolean armBone(String bone) {
        return !bone.startsWith("spine") && !bone.startsWith("neck") && !bone.startsWith("head")
                && !bone.startsWith("jiggle") && !bone.equals("camera") && !bone.startsWith("eye");
    }

    private static boolean lowerBody(String bone) {
        return bone.startsWith("leg_") || bone.startsWith("ankle_") || bone.startsWith("ball_")
                || bone.startsWith("attachFoot") || bone.equals("pelvis") || bone.equals("root_motion")
                || bone.equals("worldmodel");
    }

    private float moveAngle() {
        float yaw = bodyYaw * Mth.DEG_TO_RAD;
        double forward = -velocity.x * Mth.sin(yaw) + velocity.z * Mth.cos(yaw);
        double right = -velocity.x * Mth.cos(yaw) - velocity.z * Mth.sin(yaw);
        float angle = (float) Math.toDegrees(Math.atan2(right, forward));
        return angle < 0.0f ? angle + 360.0f : angle;
    }

    private Map<String, Cs2Clip.Local> directional(String gait, float angle) {
        float step = angle / 45.0f;
        int index = Mth.floor(step);
        String first = gait + "_" + DIRECTIONS[Math.floorMod(index, 8)];
        String second = gait + "_" + DIRECTIONS[Math.floorMod(index + 1, 8)];
        return blend(sample(first, phase * clip(first).duration), sample(second, phase * clip(second).duration), step - index);
    }

    private Map<String, Cs2Clip.Local> sample(String name, float time) {
        return sample(name, clip(name), time);
    }

    private Map<String, Cs2Clip.Local> sample(String key, Cs2Clip source, float time) {
        Map<String, Cs2Clip.Local> sampled = samples.computeIfAbsent(key, ignored -> new HashMap<>());
        source.sample(Math.min(time, source.duration), sampled);
        return sampled;
    }

    private static Map<String, Cs2Clip.Local> blend(Map<String, Cs2Clip.Local> from, Map<String, Cs2Clip.Local> to, float weight) {
        if (weight <= 0.0f) return from;
        if (weight >= 1.0f) return to;
        for (Map.Entry<String, Cs2Clip.Local> entry : from.entrySet()) {
            Cs2Clip.Local target = to.get(entry.getKey());
            if (target != null) entry.getValue().blend(target, weight);
        }
        return from;
    }

    static Cs2Clip clip(String name) {
        return Cs2Viewmodel.INSTANCE.clip("body", name);
    }

    private static float approach(float value, float target, float step) {
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }
}
