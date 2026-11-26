package sanch3zss.raven.feature.module.render.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector2f;

import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.client.resource.ClasspathResource;
import sanch3zss.raven.event.render.*;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.server.ServerUtil;
import sanch3zss.raven.feature.server.ViaVersionBridge;
import sanch3zss.raven.render.effect.SelectiveBloomRenderer;
import sanch3zss.raven.render.engine.DrawEngine;
import sanch3zss.raven.render.font.Fonts;
import sanch3zss.raven.render.gl.StencilBufferUtil;
import sanch3zss.raven.render.shape.ShapeRenderer;
import sanch3zss.raven.render.texture.GlTexture;
import sanch3zss.raven.render.theme.*;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.*;
import sanch3zss.raven.util.Tuple;
import sanch3zss.raven.util.math.*;
import sanch3zss.raven.util.math.rotation.*;
import sanch3zss.raven.util.math.trajectory.ProjectileTrajectory;
import sanch3zss.raven.util.math.trajectory.TrajectoryPoint;

@Aliases(aliases = {"Pearl Prediction", "Projectile Prediction", "Arrow Prediction"})
public class ProjectilePredictionModule extends Module {
    private static final BlockHitResult MISS = new BlockHitResult(Vec3.atCenterOf(BlockPos.ZERO), Direction.DOWN, BlockPos.ZERO.below(999), true);

    public final Mc mc;
    public final ColorSetting color;
    public final GlTexture clockTexture;
    public final BooleanSetting handPrediction;
    public final BooleanSetting areaPrediction;
    public final BooleanSetting bloom;
    private float spawnPartialTick = 1.0f;

    private enum Physics {
        MODERN(0.98f), PRE_26_2(0.99f), LEGACY(0.99f);

        final float throwableDrag;

        Physics(float throwableDrag) {
            this.throwableDrag = throwableDrag;
        }

        static Physics current() {
            if (ViaVersionBridge.serverOlderThan("v1_21_2")) return LEGACY;
            if (ViaVersionBridge.serverOlderThan("v26_2")) return PRE_26_2;
            return MODERN;
        }
    }

    private record Simulation(List<TrajectoryPoint> points, Set<ChunkPos> chunks, BlockHitResult hit) {
    }

    public ProjectilePredictionModule() {
        super(ModuleTab.VISUALS, "ProjectilePrediction");
        this.mc = Mc.INSTANCE;
        this.color = new ColorSetting(Lang.PROJECTILE_PREDICTION_COLOR);
        this.clockTexture = new GlTexture(new ClasspathResource("/icons/trajectory/clock.png"));
        this.handPrediction = new BooleanSetting(Lang.PROJECTILE_PREDICTION_HAND, Lang.PROJECTILE_PREDICTION_HAND_DESC).setValue(true);
        this.areaPrediction = new BooleanSetting(Lang.PROJECTILE_PREDICTION_AREA, Lang.PROJECTILE_PREDICTION_AREA_DESC).setValue(true);
        this.bloom = new BooleanSetting(sanch3zss.raven.client.chat.Translation.clearText("Bloom точки падения")).setValue(true);
        addSettings(this.handPrediction, this.areaPrediction, this.bloom, this.color);
        register(Render2DEvent.class, event -> {
            if (this.mc.isWorldLoaded() && isState() && event.isPre()) {
                DrawEngine drawEngine = Raven.INSTANCE.drawEngine();
                ColorStack colorStack = drawEngine.colorStack();
                Matrix4f positionMatrix = event.matrixStack().last().pose();
                boolean z = false;
                for (ProjectileTrajectory projectileTrajectory : PlayerSnapshotManager.INSTANCE.getProjectiles()) {
                    List<TrajectoryPoint> listSteps = projectileTrajectory.steps();
                    if (listSteps.isEmpty()) continue;
                    Vec3 vec3dPos = listSteps.getLast().pos();
                    if (!z) {
                        drawEngine.begin();
                        z = true;
                    }
                    Optional<Vector2f> optionalWorldToScreen = ProjectionUtil.worldToScreen(vec3dPos);
                    if (optionalWorldToScreen.isPresent()) {
                        Vector2f vector2f = optionalWorldToScreen.get();
                        float remainingTicks = Math.max(0.0f, listSteps.getLast().tick() - (this.mc.getPlayer().tickCount + event.tickCounter().getGameTimeDeltaPartialTick(false)));
                        String str = String.format(java.util.Locale.US, "%.1f", (double) (remainingTicks / 20.0f));
                        int width = (int) Fonts.INTER_SEMIBOLD.get().getWidth(str, 12.0f);
                        int iX = (int) (vector2f.x() + 5.0f);
                        int iY = (int) (vector2f.y() - 20.0f);
                        drawEngine.roundedRectangle(positionMatrix, iX, iY, width + 10 + 6 + (6 * 2), 21, 4.0f, 2.5f, colorStack.computeColor(0.12f, 129, 135, StencilBufferUtil.STENCIL_MASK), colorStack.computeColor(0.7f, 6, 7, 18));
                        int i = iX + 6;
                        int i2 = (int) (iY + (21 / 2.0f));
                        drawEngine.msdfFontVerticalC(positionMatrix, Fonts.INTER_SEMIBOLD.get(), str, iX + 6, i2, 12.0f, 0.0f, colorStack.computeColor(13948641));
                        drawEngine.texture(positionMatrix, i + width + 6, (int) (i2 - (10 / 2.0f)), 10, 10, drawEngine.bindTexture(this.clockTexture.textureWithSTB()), colorStack.white());
                        int i3 = (iY - 3) - 20;
                        drawEngine.roundedRectangle(positionMatrix, iX, i3, 20.0f, 20.0f, 4.0f, 2.5f, colorStack.computeColor(0.12f, 129, 135, StencilBufferUtil.STENCIL_MASK), colorStack.computeColor(0.7f, 6, 7, 18));
                        drawEngine.itemStack(positionMatrix, projectileTrajectory.stack(), (iX + 10) - 6, (i3 + 10) - 6, 0.375f, 1.0f);
                    }
                }
                if (z) {
                    drawEngine.end();
                }
            }
        });
        register(WorldRenderEvent.class, event2 -> {
            if (isState() && this.mc.isWorldLoaded()) {
                PoseStack matrixStack = event2.matrixStack();
                ColorStack colorStack = Raven.INSTANCE.drawEngine().colorStack();
                LocalPlayer player = this.mc.getPlayer();
                float partialTick = this.mc.getTickDelta();
                Rotation interpolatedRotation = RotationManager.INSTANCE.getCurrentRotationOriginal() != null
                        ? RotationManager.INSTANCE.getInterpolatedRotation()
                        : new Rotation(player.getViewYRot(partialTick), player.getViewXRot(partialTick));
                this.spawnPartialTick = partialTick;
                if (this.handPrediction.isValue()) {
                    for (ItemStack handStack : List.of(this.mc.getPlayer().getMainHandItem(), this.mc.getPlayer().getOffhandItem())) {
                        drawPredictionItem(matrixStack, handStack, interpolatedRotation);
                    }
                }
                for (ProjectileTrajectory projectileTrajectory : PlayerSnapshotManager.INSTANCE.getProjectiles()) {
                    List<TrajectoryPoint> listSteps = projectileTrajectory.steps();
                    if (listSteps != null && listSteps.size() >= 2) {
                        ItemStack itemStackStack = projectileTrajectory.stack();
                        for (int i = 1; i < listSteps.size(); i++) {
                            TrajectoryPoint trajectoryPoint2 = listSteps.get(i - 1);
                            TrajectoryPoint trajectoryPoint = listSteps.get(i);
                            float progress = (float) i / (listSteps.size() - 1);
                            ShapeRenderer.INSTANCE.addDepthTestedLine(matrixStack.last().pose(), trajectoryPoint2.pos(), trajectoryPoint.pos(), colorStack.computeColor(this.color.getAnimatedGradientColor(progress * 0.5f), MathUtil.clamp((trajectoryPoint.tick() - this.mc.getPlayer().tickCount) / 25.0f, 0.0f, 1.0f)), 3.0f);
                        }
                        Vec3 vec3dPos = listSteps.getLast().pos();
                        if (bloom.isValue()) {
                            SelectiveBloomRenderer.INSTANCE.add(vec3dPos.add(0.0, 0.05, 0.0),
                                    0.4f, this.color.getColor());
                        }
                        if (this.areaPrediction.isValue()) {
                            float radius = areaRadius(itemStackStack);
                            if (radius > 0.0f) {
                                drawAreaCircle(matrixStack, vec3dPos, radius, colorStack.computeColor(this.color.getColor(), 0.5f));
                            }
                        }
                    }
                }
            }
        });
    }

    public void drawPredictionItem(PoseStack matrixStack, ItemStack itemStack, Rotation rotation) {
        List<BlockHitResult> results = getTrajectoryResult(itemStack, rotation);
        for (BlockHitResult result : results) {
            Vec3 pos = result.getLocation();
            int argb = result.getBlockPos().equals(BlockPos.ZERO) ? ThemePalette.darkRed.argb() : this.color.getColor();
            if (this.areaPrediction.isValue()) {
                float radius = areaRadius(itemStack);
                if (radius > 0.0f) drawAreaCircle(matrixStack, pos, radius, argb);
            }
            drawLandingMarker(matrixStack, pos, result.getDirection(), argb);
        }
    }

    public List<BlockHitResult> getTrajectoryResult(ItemStack stack, Rotation rotation) {
        LocalPlayer player = this.mc.getPlayer();
        ClientLevel level = this.mc.getWorld();
        boolean using = player.isUsingItem() && player.getUseItem() == stack;
        if (stack.getItem() instanceof TridentItem) {
            if (!using || player.getTicksUsingItem() < 10) return List.of();
            return List.of(launch(new ThrownTrident(level, player, stack), rotation, 0.0f, 2.5f));
        }
        if (stack.getItem() instanceof BowItem) {
            if (!using) return List.of();
            float power = BowItem.getPowerForTime(player.getTicksUsingItem());
            if (power < 0.1f) return List.of();
            return List.of(launch(new Arrow(level, player, stack, stack), rotation, 0.0f, power * 3.0f));
        }
        if (stack.getItem() instanceof CrossbowItem) {
            return crossbowResults(stack, rotation);
        }
        if (stack.getItem() instanceof SnowballItem) {
            return List.of(launch(new Snowball(level, player, stack), rotation, 0.0f, 1.5f));
        }
        if (stack.getItem() instanceof EggItem) {
            return List.of(launch(new ThrownEgg(level, player, stack), rotation, 0.0f, 1.5f));
        }
        if (stack.getItem() instanceof EnderpearlItem) {
            return List.of(launch(new ThrownEnderpearl(level, player, stack), rotation, 0.0f, 1.5f));
        }
        if (stack.getItem() instanceof SplashPotionItem) {
            return List.of(launch(new ThrownSplashPotion(level, player, stack), rotation, -20.0f, 0.5f));
        }
        if (stack.getItem() instanceof LingeringPotionItem) {
            return List.of(launch(new ThrownLingeringPotion(level, player, stack), rotation, -20.0f, 0.5f));
        }
        if (stack.getItem() instanceof ExperienceBottleItem) {
            return List.of(launch(new ThrownExperienceBottle(level, player, stack), rotation, -20.0f, 0.7f));
        }
        return List.of();
    }

    private List<BlockHitResult> crossbowResults(ItemStack stack, Rotation rotation) {
        if (!CrossbowItem.isCharged(stack)) return List.of();
        ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (charged == null || charged.isEmpty() || charged.items().getFirst().is(Items.FIREWORK_ROCKET)) return List.of();
        LocalPlayer player = this.mc.getPlayer();
        Vec3 view = direction(rotation.getPitch(), rotation.getYaw(), 0.0f);
        Vec3 up = direction(rotation.getPitch() - 90.0f, rotation.getYaw(), 0.0f);
        List<BlockHitResult> results = new ArrayList<>();
        float[] angles = charged.items().size() > 1 ? new float[]{0.0f, -10.0f, 10.0f} : new float[]{0.0f};
        for (float angle : angles) {
            Vec3 shot = rotateAround(view, up, angle * Mth.DEG_TO_RAD);
            Arrow arrow = new Arrow(this.mc.getWorld(), player, stack, stack);
            results.add(simulate(arrow, player, spawnPosition(player), shot.normalize().scale(3.15), 0).hit());
        }
        return results;
    }

    private BlockHitResult launch(Projectile projectile, Rotation rotation, float pitchOffset, float power) {
        LocalPlayer player = this.mc.getPlayer();
        Vec3 velocity = direction(rotation.getPitch(), rotation.getYaw(), pitchOffset).normalize().scale(power)
                .add(shooterMovement(player));
        return simulate(projectile, player, spawnPosition(player), velocity, 0).hit();
    }

    private static Vec3 direction(float xRot, float yRot, float yOffset) {
        float x = -Mth.sin(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        float y = -Mth.sin((xRot + yOffset) * Mth.DEG_TO_RAD);
        float z = Mth.cos(yRot * Mth.DEG_TO_RAD) * Mth.cos(xRot * Mth.DEG_TO_RAD);
        return new Vec3(x, y, z);
    }

    private static Vec3 rotateAround(Vec3 vector, Vec3 axis, double angle) {
        Vec3 k = axis.normalize();
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return vector.scale(cos).add(k.cross(vector).scale(sin)).add(k.scale(k.dot(vector) * (1.0 - cos)));
    }

    private Vec3 spawnPosition(LocalPlayer player) {
        return player.getEyePosition(this.spawnPartialTick).subtract(0.0, 0.1f, 0.0);
    }

    private Vec3 shooterMovement(LocalPlayer player) {
        if (ViaVersionBridge.serverOlderThan("v1_20_5") && !ServerUtil.isConnectedToServer("holyworld")) {
            return Vec3.ZERO;
        }
        Vec3 movement = new Vec3(player.getX() - player.xo, player.getY() - player.yo, player.getZ() - player.zo);
        return new Vec3(movement.x, player.onGround() ? 0.0 : movement.y, movement.z);
    }

    public Tuple<List<ChunkPos>, List<TrajectoryPoint>> predictEntity(Entity entity, Vec3 velocity, Vec3 position) {
        Entity owner = entity instanceof Projectile projectile ? projectile.getOwner() : null;
        Simulation simulation = simulate(entity, owner, position, velocity, entity.tickCount);
        return new Tuple<>(new ArrayList<>(simulation.chunks()), simulation.points());
    }

    private Simulation simulate(Entity projectile, Entity owner, Vec3 start, Vec3 startVelocity, int age) {
        ClientLevel level = this.mc.getWorld();
        Physics physics = Physics.current();
        boolean arrow = projectile instanceof AbstractArrow;
        double gravity = projectile.getGravity();
        float waterInertia = projectile instanceof ThrownTrident ? 0.99f : 0.6f;
        List<Entity> targets = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity != projectile && entity != owner && entity.isAlive() && !entity.isSpectator() && entity.canBeHitByProjectile()) {
                targets.add(entity);
            }
        }
        List<TrajectoryPoint> points = new ArrayList<>();
        Set<ChunkPos> chunks = new HashSet<>();
        int firstTick = this.mc.getPlayer().tickCount;
        Vec3 pos = start;
        Vec3 velocity = startVelocity;
        AABB ownerBox = owner != null && owner.isAlive() && !owner.isSpectator() ? owner.getBoundingBox() : null;
        boolean leftOwner = ownerBox == null;
        for (int i = 0; i < 300; i++) {
            if (!leftOwner && !ownerBox.inflate(1.0).intersects(
                    projectile.getDimensions(projectile.getPose()).makeBoundingBox(pos))) {
                leftOwner = true;
                targets.add(owner);
            }
            Vec3 stepVelocity = velocity;
            boolean water = inWater(projectile, pos);
            if (arrow) {
                if (water && physics != Physics.LEGACY) velocity = velocity.scale(waterInertia);
            } else if (physics != Physics.LEGACY) {
                velocity = velocity.add(0.0, -gravity, 0.0).scale(water ? 0.8f : physics.throwableDrag);
            }
            Vec3 next = pos.add(velocity);
            BlockHitResult hit = stepHit(projectile, targets, pos, next, age + i);
            chunks.add(ChunkPos.containing(BlockPos.containing(pos)));
            points.add(new TrajectoryPoint(firstTick + i, stepVelocity, pos, hit != null ? hit.getLocation() : next));
            if (hit != null) return new Simulation(points, chunks, hit);
            if (next.y < level.getMinY() - 64) break;
            if (arrow) {
                if (physics == Physics.LEGACY) velocity = velocity.scale(water ? waterInertia : 0.99f);
                else if (!water) velocity = velocity.scale(0.99f);
                velocity = velocity.add(0.0, -gravity, 0.0);
            } else if (physics == Physics.LEGACY) {
                velocity = velocity.scale(water ? 0.8f : 0.99f).add(0.0, -gravity, 0.0);
            }
            pos = next;
        }
        return new Simulation(points, chunks, MISS);
    }

    private BlockHitResult stepHit(Entity projectile, List<Entity> targets, Vec3 from, Vec3 to, int tick) {
        BlockHitResult block = this.mc.getWorld().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile));
        Vec3 end = block.getType() != HitResult.Type.MISS ? block.getLocation() : to;
        double margin = Math.max(0.0f, Math.min(0.3f, (tick - 2) / 20.0f));
        AABB sweep = new AABB(from, end).inflate(1.0 + margin);
        List<AABB> boxes = new ArrayList<>();
        for (Entity target : targets) {
            AABB box = target.getBoundingBox();
            if (box.intersects(sweep)) boxes.add(box.inflate(margin));
        }
        BlockHitResult entity = boxes.isEmpty() ? null : AABB.clip(boxes, from, end, BlockPos.ZERO);
        if (entity != null && entity.getType() != HitResult.Type.MISS) return entity;
        return block.getType() != HitResult.Type.MISS ? block : null;
    }

    private boolean inWater(Entity projectile, Vec3 pos) {
        ClientLevel level = this.mc.getWorld();
        AABB box = projectile.getDimensions(projectile.getPose()).makeBoundingBox(pos).deflate(0.001);
        for (BlockPos block : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            FluidState fluid = level.getFluidState(block);
            if (fluid.is(FluidTags.WATER) && block.getY() + fluid.getHeight(level, block) >= box.minY) return true;
        }
        return false;
    }

    private float areaRadius(ItemStack stack) {
        if (stack.getItem() instanceof SplashPotionItem) return 4.0f;
        if (stack.getItem() instanceof LingeringPotionItem) return 3.0f;
        if (isSpecialSnowball(stack)) return 7.0f;
        return 0.0f;
    }

    public void drawLandingMarker(PoseStack matrixStack, Vec3 vec3d, Direction direction, int i) {
        if (bloom.isValue()) {
            SelectiveBloomRenderer.INSTANCE.add(vec3d.add(Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(0.04d)),
                    0.4f, i);
        }
        Vec3 vec3dAdd = vec3d.subtract(this.mc.getEntityRenderDispatcher().camera.position()).add(Vec3.atLowerCornerOf(direction.getUnitVec3i()).scale(0.001d));
        matrixStack.pushPose();
        matrixStack.translate(vec3dAdd.x, vec3dAdd.y, vec3dAdd.z);
        switch (direction) {
            case WEST:
            case EAST:
                matrixStack.mulPose(Axis.ZP.rotationDegrees(90.0f));
                break;
            case SOUTH:
            case NORTH:
                matrixStack.mulPose(Axis.XP.rotationDegrees(90.0f));
                break;
        }
        Matrix4f positionMatrix = matrixStack.last().pose();
        for (int i2 = 0; i2 < 48; i2++) {
            float f = (float) (((((double) i2) * 3.141592653589793d) * 2.0d) / 48.0d);
            float f2 = (float) (((((double) (i2 + 1)) * 3.141592653589793d) * 2.0d) / 48.0d);
            float fCos = (float) Math.cos(f);
            float fSin = (float) Math.sin(f);
            float fCos2 = (float) Math.cos(f2);
            float fSin2 = (float) Math.sin(f2);
            float f3 = 0.35f - 0.02f;
            float f4 = 0.35f + 0.02f;
            ShapeRenderer.INSTANCE.addFilledQuad(positionMatrix, new Vec3(fCos * f3, 0.0d, fSin * f3), new Vec3(fCos * f4, 0.0d, fSin * f4), new Vec3(fCos2 * f4, 0.0d, fSin2 * f4), new Vec3(fCos2 * f3, 0.0d, fSin2 * f3), i);
        }
        float f5 = 0.35f * 0.6f;
        ShapeRenderer.INSTANCE.addFilledQuad(positionMatrix, new Vec3(-f5, 0.0d, -0.02f), new Vec3(-f5, 0.0d, 0.02f), new Vec3(f5, 0.0d, 0.02f), new Vec3(f5, 0.0d, -0.02f), i);
        ShapeRenderer.INSTANCE.addFilledQuad(positionMatrix, new Vec3(-0.02f, 0.0d, -f5), new Vec3(-0.02f, 0.0d, f5), new Vec3(0.02f, 0.0d, f5), new Vec3(0.02f, 0.0d, -f5), i);
        matrixStack.popPose();
    }

    public void drawAreaCircle(PoseStack matrixStack, Vec3 vec3d, float f, int i) {
        Vec3 vec3dSubtract = vec3d.subtract(this.mc.getEntityRenderDispatcher().camera.position());
        matrixStack.pushPose();
        matrixStack.translate(vec3dSubtract.x, vec3dSubtract.y + 0.01d, vec3dSubtract.z);
        Matrix4f positionMatrix = matrixStack.last().pose();
        for (int i2 = 0; i2 < 64; i2++) {
            float f2 = (float) (((((double) i2) * 3.141592653589793d) * 2.0d) / 64.0d);
            float f3 = (float) (((((double) (i2 + 1)) * 3.141592653589793d) * 2.0d) / 64.0d);
            float fCos = (float) Math.cos(f2);
            float fSin = (float) Math.sin(f2);
            float fCos2 = (float) Math.cos(f3);
            float fSin2 = (float) Math.sin(f3);
            float f4 = f - 0.03f;
            float f5 = f + 0.03f;
            ShapeRenderer.INSTANCE.addFilledQuad(positionMatrix, new Vec3(fCos * f4, 0.0d, fSin * f4), new Vec3(fCos * f5, 0.0d, fSin * f5), new Vec3(fCos2 * f5, 0.0d, fSin2 * f5), new Vec3(fCos2 * f4, 0.0d, fSin2 * f4), i);
        }
        matrixStack.popPose();
    }

    public boolean isSpecialSnowball(ItemStack itemStack) {
        return (itemStack.getItem() instanceof SnowballItem) && ServerUtil.isConnectedToAllFuntimeServers() && ((CustomData) itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)).copyTag().contains("don-item");
    }
}
