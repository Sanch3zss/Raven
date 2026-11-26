package sanch3zss.raven.feature.module.render.pets;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.Raven;
import sanch3zss.raven.client.chat.DisplayNamed;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.event.network.WorldLoadEvent;
import sanch3zss.raven.event.player.AttackEntityEvent;
import sanch3zss.raven.event.render.WorldRenderEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.render.shape.ImmediateRenderLayers;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.ModeSetting;

@Aliases(aliases = {"Pet"})
public final class PetsModule extends Module {
    private final PetBrain brain = new PetBrain();
    private final PetFlyModel flyModel = new PetFlyModel();
    private final Map<PetType, PetModel> models = new EnumMap<>(PetType.class);
    private final RandomSource random = RandomSource.create();
    private final ModeSetting<PetType> petType = new ModeSetting<PetType>(Translation.clearText("Питомец"))
            .values(PetType.class);
    private final BooleanSetting perch = new BooleanSetting(Translation.clearText("Садиться на плечо"))
            .setValue(true).visible(() -> petType.currentValue().kind == PetBrain.Kind.BIRD);
    private final BooleanSetting sounds = new BooleanSetting(Translation.clearText("Звуки"))
            .setValue(true).visible(() -> petType.currentValue().sound != null);
    private final BooleanSetting glowingEyes = new BooleanSetting(Translation.clearText("Светящиеся глаза"))
            .visible(() -> petType.currentValue().glowmask != null);

    public PetsModule() {
        super(ModuleTab.VISUALS, "Pets");
        addSettings(petType, perch, sounds, glowingEyes);
        register(ClientTickEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded()) {
                PetType type = petType.currentValue();
                brain.tick(Mc.INSTANCE.getPlayer(), type.kind, type == PetType.HAWK, perch.isValue());
                if (brain.consumeCall() && sounds.isValue() && type.sound != null) {
                    Vec3 position = brain.position(1.0f);
                    Mc.INSTANCE.getMinecraft().getSoundManager().play(new SimpleSoundInstance(
                            SoundEvent.createVariableRangeEvent(type.sound), SoundSource.NEUTRAL, 0.5f,
                            0.9f + random.nextFloat() * 0.2f, random, position.x, position.y, position.z));
                }
            } else if (brain.visible()) {
                brain.reset();
            }
        });
        register(AttackEntityEvent.class, event -> {
            if (isState() && Mc.INSTANCE.isWorldLoaded() && event.attacker() instanceof LivingEntity target) {
                brain.chase(target, Mc.INSTANCE.getPlayer());
            }
        });
        register(WorldLoadEvent.class, event -> brain.reset());
        register(WorldRenderEvent.class, this::render);
    }

    @Override
    public void deactivate() {
        brain.reset();
        super.deactivate();
    }

    private void render(WorldRenderEvent event) {
        Mc mc = Mc.INSTANCE;
        if (!isState() || !mc.isWorldLoaded() || !brain.visible()) {
            return;
        }
        if (brain.perched() && mc.getGameOptions().getCameraType().isFirstPerson()) {
            return;
        }
        float partialTick = mc.getTickDelta();
        Vec3 worldPosition = brain.position(partialTick);
        if (event.frustum() != null && !event.frustum().isVisible(
                new net.minecraft.world.phys.AABB(worldPosition, worldPosition).inflate(1.0))) {
            return;
        }
        PetType type = petType.currentValue();
        Vec3 camera = mc.getCamera().position();
        int light = LightCoordsUtil.getLightCoords(mc.getWorld(), BlockPos.containing(worldPosition.add(0.0, 0.2, 0.0)));
        PoseStack matrices = event.matrixStack();
        matrices.pushPose();
        try {
            matrices.translate(worldPosition.x - camera.x, worldPosition.y - camera.y + type.pivot,
                    worldPosition.z - camera.z);
            matrices.mulPose(Axis.YP.rotationDegrees(180.0f - brain.yaw(partialTick)));
            matrices.mulPose(Axis.XP.rotationDegrees(-brain.pitch(partialTick)));
            matrices.mulPose(Axis.ZP.rotationDegrees(-brain.bank(partialTick)));
            matrices.translate(0.0f, -type.pivot, 0.0f);
            matrices.scale(-type.scale, -type.scale, type.scale);
            if (type == PetType.FLY) {
                matrices.translate(0.0f, brain.landed() ? -1.53f : -1.125f, 0.0f);
                draw(matrices, type.renderType, vertices -> flyModel.render(matrices, vertices, light,
                        brain.age(partialTick), brain.movement(), brain.landed(), brain.grooming()));
                return;
            }
            PetModel model;
            try {
                model = models.computeIfAbsent(type, value -> new PetModel(value.model));
            } catch (RuntimeException exception) {
                Raven.LOGGER.error("Failed to load pet model {}", type.model, exception);
                setState(false);
                return;
            }
            pose(model, type, partialTick);
            matrices.translate(0.0f, -1.501f, 0.0f);
            draw(matrices, type.renderType, vertices -> model.render(matrices, vertices, light));
            if (glowingEyes.isValue() && type.glowmask != null) {
                draw(matrices, type.glowmask, vertices -> model.render(matrices, vertices, light));
            }
        } finally {
            matrices.popPose();
        }
    }

    private void pose(PetModel model, PetType type, float partialTick) {
        model.reset();
        float blend = brain.poseBlend(partialTick);
        if (blend < 1.0f) {
            model.apply(type.animation(brain.previousPose()), brain.previousPoseTime(), 1.0f - blend);
        }
        PetBrain.Pose pose = brain.pose();
        if (pose == PetBrain.Pose.SLAP) {
            float time = brain.animationTime(partialTick);
            model.apply(type.animation(PetBrain.Pose.STAND), time, blend);
            slap(model, time, blend);
        } else {
            model.apply(type.animation(pose), brain.animationTime(partialTick), blend);
        }
        boolean wingsOpen = pose == PetBrain.Pose.FLAP || pose == PetBrain.Pose.HOVER || pose == PetBrain.Pose.GLIDE;
        setVisible(model, "left_wing", !wingsOpen);
        setVisible(model, "right_wing", !wingsOpen);
        setVisible(model, "left_wing_open", wingsOpen);
        setVisible(model, "right_wing_open", wingsOpen);
        setVisible(model, "baby", false);
        ModelPart head = model.part("head");
        if (head != null) {
            head.yRot += brain.headYaw(partialTick) * Mth.DEG_TO_RAD;
        }
    }

    private static void slap(PetModel model, float time, float weight) {
        float cycle = time * 2.2f;
        float swing = Mth.sin(cycle * Mth.TWO_PI);
        float strike = Math.max(0.0f, swing);
        float windup = Math.max(0.0f, -swing);
        boolean leftTurn = ((int) cycle & 1) == 0;
        ModelPart body = model.part("body");
        if (body != null) body.xRot += (strike * 0.32f - windup * 0.12f) * weight;
        ModelPart neck = model.part("neck");
        if (neck != null) neck.xRot += (strike * 0.45f - windup * 0.2f) * weight;
        ModelPart active = model.part(leftTurn ? "left_wing" : "right_wing");
        ModelPart resting = model.part(leftTurn ? "right_wing" : "left_wing");
        float outward = leftTurn ? -1.0f : 1.0f;
        if (active != null) {
            active.zRot += outward * (0.25f + windup * 1.1f + strike * 0.3f) * weight;
            active.xRot -= strike * 0.9f * weight;
        }
        if (resting != null) resting.zRot -= outward * 0.2f * weight;
    }

    private static void setVisible(PetModel model, String name, boolean visible) {
        ModelPart part = model.part(name);
        if (part != null) part.visible = visible;
    }

    private void draw(PoseStack matrices, RenderType renderType, Consumer<VertexConsumer> renderer) {
        ByteBufferBuilder bytes = new ByteBufferBuilder(16384);
        try {
            BufferBuilder vertices = new BufferBuilder(bytes, renderType.primitiveTopology(), renderType.format());
            renderer.accept(vertices);
            ImmediateRenderLayers.draw(renderType, vertices.buildOrThrow());
        } finally {
            bytes.close();
        }
    }

    private enum PetType implements DisplayNamed {
        FLY("Муха", null, "fly", null, null, PetBrain.Kind.INSECT, 0.5f, 0.0f),
        RAVEN("Ворон", "raven", "raven", "raven_glowmask", "raven:pet.raven.call", PetBrain.Kind.BIRD, 0.55f, 0.2f),
        CROW("Ворона", "crow", "crow", "crow_glowmask", "raven:pet.crow.call", PetBrain.Kind.BIRD, 0.7f, 0.15f),
        HAWK("Ястреб", "hawk", "hawk", null, "raven:pet.hawk.call", PetBrain.Kind.BIRD, 0.55f, 0.2f),
        MACAW("Ара", "macaw", "macaw", null, "minecraft:entity.parrot.ambient", PetBrain.Kind.BIRD, 0.55f, 0.2f),
        PENGUIN("Пингвин", "penguin", "penguin", null, "raven:pet.penguin.call", PetBrain.Kind.WALKER, 0.5f, 0.0f);

        private final Translation name;
        private final String model;
        private final RenderType renderType;
        private final RenderType glowmask;
        private final Identifier sound;
        private final PetBrain.Kind kind;
        private final float scale;
        private final float pivot;

        PetType(String name, String model, String texture, String glowmask, String sound, PetBrain.Kind kind,
                float scale, float pivot) {
            this.name = Translation.clearText(name);
            this.model = model;
            this.renderType = RenderTypes.entityCutout(
                    Identifier.fromNamespaceAndPath("raven", "textures/entity/pet/" + texture + ".png"));
            this.glowmask = glowmask == null ? null : RenderTypes.eyes(
                    Identifier.fromNamespaceAndPath("raven", "textures/entity/pet/" + glowmask + ".png"));
            this.sound = sound == null ? null : Identifier.parse(sound);
            this.kind = kind;
            this.scale = scale;
            this.pivot = pivot;
        }

        private String animation(PetBrain.Pose pose) {
            if (this == PENGUIN) {
                return switch (pose) {
                    case WALK -> "WALKING";
                    case SLIDE -> "SLIDING";
                    case SWIM -> "SWIMMING";
                    case DANCE -> "DANCING";
                    case SLAP -> "FLAPPING";
                    default -> "STANDING";
                };
            }
            boolean crow = this == CROW;
            return switch (pose) {
                case FLAP, HOVER -> crow ? "CROW_FLAP" : "FLAPPING";
                case GLIDE -> crow ? "CROW_GLIDE" : "GLIDING";
                case WALK -> crow ? "CROW_WALK" : "WALKING";
                case PERCH -> this == MACAW ? "PERCHING" : crow ? "CROW_IDLE" : "STANDING";
                default -> crow ? "CROW_IDLE" : "STANDING";
            };
        }

        @Override
        public Translation getDisplayName() {
            return name;
        }
    }
}
