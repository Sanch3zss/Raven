package sanch3zss.raven.feature.module.render.armtweaks.knife;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;

public final class Cs2Legs {
    public static final Cs2Legs INSTANCE = new Cs2Legs();
    private final Map<String, Cs2Mesh> legs = new HashMap<>();

    private Cs2Legs() {}

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, Vec3 camera) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ArmTweaksModule module = Raven.INSTANCE.moduleRepository().get(ArmTweaksModule.class);
        if (player == null || minecraft.level == null || !visible(minecraft, player, module)) return;
        AgentCatalog.Agent agent = AgentCatalog.get().agent(module.activeAgentId());
        if (agent == null || agent.body() == null) agent = AgentCatalog.get().agent(Cs2Body.DEFAULT_AGENT);
        if (agent == null || agent.body() == null) return;
        Cs2Mesh legs = legs(agent.body());
        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Cs2Body body = Cs2Body.INSTANCE;
        if (legs == null || !body.update(player, partialTick, 0.3f)) return;
        float halfFov = (minecraft.options.fov().get() + 10.0f) * 0.5f;
        if (player.getViewXRot(partialTick) + halfFov < 55.0f) return;
        ViewmodelLighting.INSTANCE.update(player, partialTick);

        Map<String, Matrix4f> world = body.world(Mth.wrapDegrees(player.getViewYRot(partialTick) - body.bodyYaw()), 0.0f);
        for (String cap : legs.caps) {
            Matrix4f joint = world.get(cap);
            if (joint == null) continue;
            world.put(cap, new Matrix4f().translation(joint.getTranslation(new Vector3f())).scale(0.001f).mul(legs.restWorld(cap)));
        }
        legs.pose(world, null, false);

        Vec3 position = player.getPosition(partialTick).add(body.offset());
        int light = LightCoordsUtil.getLightCoords(minecraft.level, BlockPos.containing(position.add(0.0, 0.5, 0.0)));
        poseStack.pushPose();
        poseStack.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(-body.bodyYaw()));
        for (Cs2Mesh.Part part : legs.parts) {
            Identifier texture = AgentCatalog.get().texture(part.texture());
            if (texture == null) continue;
            collector.submitCustomGeometry(poseStack, HandFinish.CLOTH.renderType(texture),
                    (pose, buffer) -> legs.emit(pose, buffer, part, light, 0, 0.5f, true));
        }
        poseStack.popPose();
    }

    private Cs2Mesh legs(String file) {
        if (legs.containsKey(file)) return legs.get(file);
        Cs2Mesh body = Cs2Viewmodel.INSTANCE.mesh("agents/meshes/" + file);
        Cs2Clip skeleton = Cs2Body.clip("idle");
        Cs2Mesh result = body == null || skeleton == null ? null : body.subset(bone -> torso(bone, body, skeleton));
        legs.put(file, result);
        return result;
    }

    private static boolean torso(String bone, Cs2Mesh mesh, Cs2Clip skeleton) {
        String current = bone;
        while (current != null) {
            if (current.startsWith("neck") || current.startsWith("head") || current.startsWith("arm_")
                    || current.startsWith("hand_")) return false;
            if (current.equals("pelvis") || current.startsWith("spine") || current.startsWith("clavicle")
                    || current.startsWith("leg_")) return true;
            String parent = mesh.parentOf(current);
            current = parent != null ? parent : skeleton.parentOf(current);
        }
        return false;
    }

    private static boolean visible(Minecraft minecraft, LocalPlayer player, ArmTweaksModule module) {
        return module.shouldRenderCs2Hands() && minecraft.options.getCameraType().isFirstPerson()
                && minecraft.gameRenderer.mainCamera().entity() == player
                && !player.isSpectator() && !player.isInvisible() && !player.isSleeping() && !player.isPassenger()
                && !player.isFallFlying() && !player.isVisuallySwimming() && !player.isAutoSpinAttack();
    }
}
