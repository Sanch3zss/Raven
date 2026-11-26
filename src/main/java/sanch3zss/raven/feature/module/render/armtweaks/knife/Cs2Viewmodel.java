package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;
import sanch3zss.raven.feature.module.render.armtweaks.GlassHandsRenderer;

final class Cs2Viewmodel {
    static final Cs2Viewmodel INSTANCE = new Cs2Viewmodel();
    private static final String ROOT = "/assets/raven/knives/cs2/";
    private static final Identifier GLOVE = Identifier.fromNamespaceAndPath("raven", "knives/cs2/textures/glove.png");
    private static final Identifier ARM = Identifier.fromNamespaceAndPath("raven", "knives/cs2/textures/arm.png");
    private static final float FADE = 0.12f;
    private static final float FOV_SCALE = (float) (Math.tan(Math.toRadians(35.0))
            / Math.tan(Math.atan(Math.tan(Math.toRadians(34.0)) * 0.75)));
    private final Map<String, Cs2Mesh> meshes = new HashMap<>();
    private final Map<String, Cs2Clip> clips = new HashMap<>();
    private final Set<String> missing = new HashSet<>();
    private final List<SoundInstance> playingSounds = new ArrayList<>();
    private final Map<String, Cs2Clip.Local> currentLocals = new HashMap<>();
    private final Map<String, Cs2Clip.Local> previousLocals = new HashMap<>();
    private final Map<String, Cs2Clip.Local> displayedLocals = new HashMap<>();
    private String model;
    private Item equippedItem;
    private int equippedSlot = -1;
    private String clip;
    private float clipTime;
    private String previous;
    private float fade = 1.0f;
    private long lastFrame;
    private float soundsPlayedUntil;
    private float idleFor;
    private float fidgetAfter = 15.0f;
    private boolean inspectHeld;
    private int inspectIndex;
    private int lightIndex;
    private int heavyIndex;
    private boolean handTexturesLoaded;

    private Cs2Viewmodel() {}

    boolean render(AbstractClientPlayer player, ItemStack stack, ItemStack offhandStack, KnifeVariant variant,
                   RenderType knifeType, float knifeUScale, int knifeOverlay, PoseStack poseStack,
                   SubmitNodeCollector collector, int light) {
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (!handTexturesLoaded) {
            handTexturesLoaded = true;
            Minecraft.getInstance().getTextureManager().registerAndLoad(ARM, new KnifeTexture(ARM));
            Minecraft.getInstance().getTextureManager().registerAndLoad(GLOVE, new KnifeTexture(GLOVE));
        }
        GloveSkin gloveSkin = module.activeGloveSkin();
        Identifier gloveTexture = gloveSkin == null ? null
                : PaintedSkins.INSTANCE.glove(gloveSkin, (int) module.glovePattern.currentValue(), module.gloveWear.currentValue());
        Cs2Mesh gloves = gloveTexture == null ? null : mesh("gloves/" + gloveSkin.type.model + ".mesh");
        AgentCatalog.Agent agent = AgentCatalog.get().agent(module.activeAgentId());
        Cs2Mesh agentHands = agent == null ? null : mesh("agents/meshes/" + agent.hands());
        Cs2Mesh sleeves = agent == null || agent.sleeves() == null ? null : mesh("agents/meshes/" + agent.sleeves());
        Cs2Mesh arms = gloves != null ? gloves : agentHands != null ? agentHands : mesh("arms.mesh");
        if (gloves == null) gloveTexture = GLOVE;
        Cs2Mesh knife = mesh("models/" + variant.model + ".mesh");
        if (arms == null || knife == null || clip(variant.model, idleName(variant.model)) == null) return false;
        long now = System.nanoTime();
        float delta = lastFrame == 0L ? 0.0f : Math.min(0.1f, (now - lastFrame) * 1.0e-9f);
        lastFrame = now;
        int slot = player.getInventory().getSelectedSlot();
        if (!variant.model.equals(model) || equippedItem != stack.getItem() || equippedSlot != slot) {
            model = variant.model;
            equippedItem = stack.getItem();
            equippedSlot = slot;
            stopSounds();
            String draw = clip(model, "draw2") != null && ThreadLocalRandom.current().nextInt(3) == 0 ? "draw2" : "draw";
            play(clip(model, draw) != null ? draw : idleName(model), true);
        }
        advance(delta * module.knifeSpeed.currentValue(), module.knifeSounds.isValue());
        Map<String, Matrix4f> world = currentPose();
        boolean hideLeft = !offhandStack.isEmpty();
        poseStack.pushPose();
        module.applyKnifeViewModel(poseStack, player.getMainArm());
        poseStack.scale(FOV_SCALE, FOV_SCALE, 1.0f);
        boolean leftHanded = player.getMainArm() == HumanoidArm.LEFT;
        poseStack.scale(leftHanded ? 1.0f : -1.0f, 1.0f, -1.0f);
        poseStack.translate(-0.0254f, -0.0254f, 0.0254f);
        Matrix4f attach = null;
        if (!knife.hasBone("weapon_hand_r") && world.containsKey("wpn") && world.containsKey("weapon")) {
            attach = new Matrix4f(world.get("wpn")).mul(new Matrix4f(world.get("weapon")).invert());
        }
        knife.pose(world, attach, hideLeft);
        boolean knifeTriangles = knifeType.primitiveTopology() == PrimitiveTopology.TRIANGLES;
        for (Cs2Mesh.Part part : knife.parts) {
            collector.submitCustomGeometry(poseStack, knifeType,
                    (pose, buffer) -> knife.emit(pose, buffer, part, light, knifeOverlay, knifeUScale, knifeTriangles));
        }
        boolean glass = module.shouldRenderGlassHands();
        GlassHandsRenderer glassHands = GlassHandsRenderer.INSTANCE;
        int overlay = glassHands.knifeOverlay(glass);
        if (sleeves != null) sleeves.twistSource = arms;
        for (Cs2Mesh mesh : sleeves == null ? List.of(arms) : List.of(arms, sleeves)) {
            mesh.pose(world, null, hideLeft);
            for (Cs2Mesh.Part part : mesh.parts) {
                Identifier texture = partTexture(part.texture(), gloveTexture, agent);
                if (texture == null) continue;
                boolean skin = part.texture().equals("arm") || part.texture().startsWith("bare_arm");
                RenderType surface = (skin ? HandFinish.SKIN : HandFinish.GLOVE).renderType(texture);
                RenderType type = glassHands.knifeRenderType(texture, surface, glass);
                boolean triangles = type.primitiveTopology() == PrimitiveTopology.TRIANGLES;
                collector.submitCustomGeometry(poseStack, type,
                        (pose, buffer) -> mesh.emit(pose, buffer, part, light, overlay, 0.5f, triangles));
            }
        }
        poseStack.popPose();
        return true;
    }

    private static Identifier partTexture(String part, Identifier gloveTexture, AgentCatalog.Agent agent) {
        if (part.equals("glove")) return gloveTexture;
        if (part.equals("arm")) return agent == null ? ARM : AgentCatalog.get().texture(agent.arm());
        return AgentCatalog.get().texture(part);
    }

    void reset() {
        if (model == null) return;
        stopSounds();
        model = null;
        equippedItem = null;
        equippedSlot = -1;
        clip = null;
        previous = null;
        previousLocals.clear();
        displayedLocals.clear();
        inspectHeld = false;
        lastFrame = 0L;
    }

    void swing(boolean heavy) {
        if (model == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        Entity attacked = KnifeRenderer.INSTANCE.attackTarget();
        if (attacked == null && minecraft.hitResult instanceof EntityHitResult result) attacked = result.getEntity();
        boolean hit = attacked != null;
        boolean backstab = hit && minecraft.player != null && attacked instanceof LivingEntity target
                && behind(minecraft.player.getLookAngle(), target);
        String next;
        if (backstab) {
            next = heavy ? "heavy_backstab"
                    : clip(model, "light_backstab2") != null && (lightIndex++ & 1) == 1 ? "light_backstab2" : "light_backstab";
        } else if (heavy) {
            next = numbered(hit ? "heavy_hit" : "heavy_miss", heavyIndex++);
        } else {
            next = numbered(hit ? "light_hit" : "light_miss", lightIndex++);
        }
        if (clip(model, next) == null) next = numbered(heavy ? "heavy_miss" : "light_miss", heavyIndex++);
        play(next, false);
        inspectHeld = false;
    }

    void inspect(boolean held) {
        inspectHeld = held;
        if (!held || model == null) return;
        List<String> inspects = new ArrayList<>();
        for (String name : new String[]{"lookat01", "lookat02", "lookat03"}) {
            if (clip(model, name) != null) inspects.add(name);
        }
        if (inspects.isEmpty()) return;
        play(inspects.get(inspectIndex++ % inspects.size()), false);
    }

    static boolean behind(Vec3 look, LivingEntity target) {
        Vec3 facing = Vec3.directionFromRotation(0.0f, target.getYHeadRot());
        Vec3 flat = new Vec3(look.x, 0.0, look.z).normalize();
        return flat.dot(facing) > 0.5;
    }

    private String numbered(String prefix, int index) {
        int count = 0;
        while (clip(model, prefix + (count + 1)) != null) count++;
        return count == 0 ? prefix + 1 : prefix + (index % count + 1);
    }

    private void play(String next, boolean instant) {
        if (clip != null && !instant && !displayedLocals.isEmpty()) {
            for (Map.Entry<String, Cs2Clip.Local> entry : displayedLocals.entrySet()) {
                Cs2Clip.Local frozen = previousLocals.computeIfAbsent(entry.getKey(), name -> new Cs2Clip.Local());
                frozen.parent = entry.getValue().parent;
                frozen.translation.set(entry.getValue().translation);
                frozen.rotation.set(entry.getValue().rotation);
            }
            previous = clip;
            fade = 0.0f;
        } else {
            previous = null;
            fade = 1.0f;
        }
        clip = next;
        clipTime = 0.0f;
        soundsPlayedUntil = -1.0f;
        idleFor = 0.0f;
    }

    private void advance(float delta, boolean sounds) {
        Cs2Clip active = clip(model, clip);
        if (active == null) {
            play(idleName(model), true);
            active = clip(model, clip);
        }
        clipTime += delta;
        fade = Math.min(1.0f, fade + delta / FADE);
        if (fade >= 1.0f) previous = null;
        float[] loop = active.sections.get("WPN_INSPECT_LOOP");
        if (inspectHeld && clip.startsWith("lookat") && loop != null && clipTime >= loop[1] && loop[1] > loop[0]) {
            clipTime = loop[0] + (clipTime - loop[1]) % (loop[1] - loop[0]);
            soundsPlayedUntil = clipTime;
        }
        if (sounds) playSounds(active);
        boolean idle = clip.equals(idleName(model));
        if (idle) {
            idleFor += delta;
            if (clipTime >= active.duration) {
                clipTime %= Math.max(0.01f, active.duration);
                soundsPlayedUntil = -1.0f;
            }
            if (idleFor > fidgetAfter && clip(model, "idle2") != null) {
                fidgetAfter = 15.0f + ThreadLocalRandom.current().nextFloat() * 15.0f;
                play("idle2", false);
            }
        } else if (clipTime >= active.duration) {
            play(idleName(model), false);
        }
    }

    private void playSounds(Cs2Clip active) {
        for (Cs2Clip.Sound sound : active.sounds) {
            if (sound.time() <= soundsPlayedUntil || sound.time() > clipTime) continue;
            if (Minecraft.getInstance().getSoundManager().getSoundEvent(sound.event()) == null) continue;
            SoundInstance instance = new SimpleSoundInstance(sound.event(), SoundSource.PLAYERS,
                    1.0f, 1.0f, SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE,
                    0.0, 0.0, 0.0, true);
            Minecraft.getInstance().getSoundManager().play(instance);
            playingSounds.add(instance);
        }
        soundsPlayedUntil = clipTime;
        if (playingSounds.size() > 32) playingSounds.subList(0, playingSounds.size() - 32).clear();
    }

    private void stopSounds() {
        for (SoundInstance instance : playingSounds) Minecraft.getInstance().getSoundManager().stop(instance);
        playingSounds.clear();
    }

    private Map<String, Matrix4f> currentPose() {
        clip(model, clip).sample(clipTime, currentLocals);
        if (previous != null && fade < 1.0f && !previousLocals.isEmpty()) {
            float weight = fade * fade * (3.0f - 2.0f * fade);
            Map<String, Cs2Clip.Local> blended = new HashMap<>();
            for (Map.Entry<String, Cs2Clip.Local> entry : previousLocals.entrySet()) {
                Cs2Clip.Local target = currentLocals.get(entry.getKey());
                if (target == null) continue;
                Cs2Clip.Local local = new Cs2Clip.Local();
                local.parent = target.parent;
                local.translation.set(entry.getValue().translation);
                local.rotation.set(entry.getValue().rotation);
                local.blend(target, weight);
                blended.put(entry.getKey(), local);
            }
            for (Map.Entry<String, Cs2Clip.Local> entry : currentLocals.entrySet()) {
                blended.putIfAbsent(entry.getKey(), entry.getValue());
            }
            remember(blended);
            return Cs2Clip.compose(blended);
        }
        remember(currentLocals);
        return Cs2Clip.compose(currentLocals);
    }

    private void remember(Map<String, Cs2Clip.Local> shown) {
        for (Map.Entry<String, Cs2Clip.Local> entry : shown.entrySet()) {
            Cs2Clip.Local copy = displayedLocals.computeIfAbsent(entry.getKey(), name -> new Cs2Clip.Local());
            copy.parent = entry.getValue().parent;
            copy.translation.set(entry.getValue().translation);
            copy.rotation.set(entry.getValue().rotation);
        }
    }

    private String idleName(String model) {
        return clip(model, "idle1") != null ? "idle1" : "idle";
    }

    Cs2Mesh mesh(String path) {
        Cs2Mesh cached = meshes.get(path);
        if (cached != null || missing.contains(path)) return cached;
        try {
            cached = Cs2Mesh.load(ROOT + path);
            meshes.put(path, cached);
        } catch (Exception error) {
            missing.add(path);
            Raven.LOGGER.warn("Failed to load CS2 mesh {}", path, error);
        }
        return cached;
    }

    Cs2Clip clip(String model, String name) {
        if (model == null || name == null) return null;
        String path = "anims/" + model + "/" + name + ".anim";
        Cs2Clip cached = clips.get(path);
        if (cached != null || missing.contains(path)) return cached;
        if (Cs2Viewmodel.class.getResource(ROOT + path) == null) {
            missing.add(path);
            return null;
        }
        try {
            cached = new Cs2Clip(ROOT + path);
            clips.put(path, cached);
        } catch (Exception error) {
            missing.add(path);
            Raven.LOGGER.warn("Failed to load CS2 animation {}", path, error);
        }
        return cached;
    }
}
