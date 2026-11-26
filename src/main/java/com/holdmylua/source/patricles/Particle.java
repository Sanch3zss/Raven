
package com.holdmylua.source.patricles;

import com.holdmylua.source.LuaTestHMI;
import com.holdmylua.source.patricles.render.ParticleRenderLayers;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.runtime.SwitchBootstraps;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class Particle {
    private int prevAge = 0;
    private Consumer<Particle> ticker = null;
    public String lifecycleType;
    public String space;
    public boolean dead = false;
    private boolean isBirth = true;
    private final String particleRenderType;
    private double yawPrev = 0.0;
    private double pitchPrev = 0.0;
    private double particleLifetime = 0.0;
    public double x;
    public double y;
    public double z;
    public double dx;
    public double dy;
    public double dz;
    private double alpha = 255.0;
    private double scale = 0.0;
    public double maxScale = 1.0;
    public double rx;
    public double ry;
    public double rz;
    public double drx;
    public double dry;
    public double drz;
    private boolean gravity;
    public InteractionHand hand;
    private double age = 0.0;
    private Identifier texture;
    private List<Identifier> keyframes = new ArrayList<Identifier>();
    public PoseStack p_matrices = null;
    Iterator<Identifier> iterator;

    public Particle(boolean gravity, double x, double y, double z, double dx, double dy, double dz, double rx, double ry, double rz, double drx, double dry, double drz, double maxScale, Identifier texture, String space, InteractionHand hand, String lifecycleType, String particleRenderType, double particleLifetime, double alpha, Consumer<Particle> ticker) {
        this.particleLifetime = particleLifetime;
        this.gravity = gravity;
        this.lifecycleType = lifecycleType;
        this.particleRenderType = particleRenderType;
        this.space = space;
        this.hand = hand;
        this.ticker = ticker;
        this.alpha = alpha;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.drx = drx;
        this.dry = dry;
        this.drz = drz;
        this.maxScale = maxScale;
        this.texture = texture;
        this.iterator = this.keyframes.iterator();
    }

    public Particle(boolean gravity, double x, double y, double z, double dx, double dy, double dz, double rx, double ry, double rz, double drx, double dry, double drz, double maxScale, Identifier texture, String space, InteractionHand hand, String lifecycleType, String particleRenderType, double particleLifetime, double alpha) {
        this.particleLifetime = particleLifetime;
        this.gravity = gravity;
        this.lifecycleType = lifecycleType;
        this.particleRenderType = particleRenderType;
        this.space = space;
        this.hand = hand;
        this.alpha = alpha;
        this.x = x;
        this.y = y;
        this.z = z;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.drx = drx;
        this.dry = dry;
        this.drz = drz;
        this.maxScale = maxScale;
        this.texture = texture;
        this.iterator = this.keyframes.iterator();
    }

    public Particle(boolean gravity, double x, double y, double z, double dx, double dy, double dz, double rx, double ry, double rz, double drx, double dry, double drz, double maxScale, Identifier texture, List<Identifier> keyframes, String space, InteractionHand hand, String lifecycleType, String particleRenderType, double particleLifetime, double alpha, Consumer<Particle> ticker) {
        this.particleLifetime = particleLifetime;
        this.gravity = gravity;
        this.lifecycleType = lifecycleType;
        this.particleRenderType = particleRenderType;
        this.space = space;
        this.hand = hand;
        this.ticker = ticker;
        this.x = x;
        this.y = y;
        this.z = z;
        this.alpha = alpha;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.drx = drx;
        this.dry = dry;
        this.drz = drz;
        this.maxScale = maxScale;
        this.texture = texture;
        this.keyframes = keyframes;
        this.iterator = keyframes.iterator();
    }

    public Particle(boolean gravity, double x, double y, double z, double dx, double dy, double dz, double rx, double ry, double rz, double drx, double dry, double drz, double maxScale, Identifier texture, String space, InteractionHand hand, String lifecycleType, String particleRenderType, double particleLifetime, double alpha, Consumer<Particle> ticker, PoseStack p_matrices) {
        this.particleLifetime = particleLifetime;
        this.gravity = gravity;
        this.lifecycleType = lifecycleType;
        this.particleRenderType = particleRenderType;
        this.space = space;
        this.hand = hand;
        this.ticker = ticker;
        this.x = x;
        this.y = y;
        this.z = z;
        this.alpha = alpha;
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.drx = drx;
        this.dry = dry;
        this.drz = drz;
        this.maxScale = maxScale;
        this.texture = texture;
        this.iterator = this.keyframes.iterator();
        this.p_matrices = p_matrices;
    }

    public void tick() {
        if (!this.dead) {
            this.particleLifetime -= (double)(0.1f * LuaTestHMI.deltaTime * 30.0f);
            this.particleLifetime = Math.clamp(this.particleLifetime, 0.0, 9999.0);
            this.age += (double)(0.1f * LuaTestHMI.deltaTime * 30.0f);
            this.x += this.dx * (double)LuaTestHMI.deltaTime * 30.0;
            this.y += this.dy * (double)LuaTestHMI.deltaTime * 30.0;
            this.z += this.dz * (double)LuaTestHMI.deltaTime * 30.0;
            this.dx *= Math.pow(0.9, LuaTestHMI.deltaTime * 30.0f);
            this.dy *= Math.pow(0.9, LuaTestHMI.deltaTime * 30.0f);
            this.dz *= Math.pow(0.9, LuaTestHMI.deltaTime * 30.0f);
            LocalPlayer player = Minecraft.getInstance().player;
            double yaw = player.getYRot();
            double radians = Math.toRadians(yaw);
            double forwardX = -Math.sin(radians);
            double forwardZ = Math.cos(radians);
            Vec3 horizontalVelocity = player.getDeltaMovement();
            double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
            double crossProduct = player.getDeltaMovement().horizontal().x * forwardZ - horizontalVelocity.z * forwardX;
            if (this.gravity) {
                this.dy -= 0.01 * (double)LuaTestHMI.deltaTime * 30.0;
            }
            this.rx += this.drx * (double)LuaTestHMI.deltaTime * 30.0;
            this.ry += this.dry * (double)LuaTestHMI.deltaTime * 30.0;
            this.rz += this.drz * (double)LuaTestHMI.deltaTime * 30.0;
            if (!this.keyframes.isEmpty() && this.iterator.hasNext() && Minecraft.getInstance().player.tickCount % 4 == 0 && player.tickCount != this.prevAge) {
                this.texture = this.iterator.next();
            }
            if (this.lifecycleType == "SCALE") {
                if (!this.isBirth && this.particleLifetime == 0.0) {
                    this.scale -= 0.03 * (double)LuaTestHMI.deltaTime * 30.0;
                    if (this.scale <= 0.0) {
                        this.dead = true;
                    }
                } else {
                    this.scale += 0.07 * (double)LuaTestHMI.deltaTime * 30.0;
                    if (this.scale >= this.maxScale) {
                        this.scale = this.maxScale;
                        this.isBirth = false;
                    }
                }
            } else if (this.lifecycleType == "OPACITY") {
                if (!this.isBirth && this.particleLifetime == 0.0) {
                    this.alpha -= (double)(10.0f * LuaTestHMI.deltaTime * 30.0f);
                    if (this.alpha <= 0.0) {
                        this.dead = true;
                        this.alpha = 0.0;
                    }
                } else {
                    this.scale = this.maxScale;
                    this.isBirth = false;
                    this.alpha = 255.0;
                }
            } else if (this.lifecycleType == "SPAWN") {
                if (!this.isBirth && this.particleLifetime == 0.0) {
                    this.dead = true;
                } else {
                    this.scale = this.maxScale;
                    this.isBirth = false;
                }
            } else if (this.lifecycleType == "KEYFRAME") {
                if (!this.isBirth && !this.iterator.hasNext()) {
                    this.dead = true;
                } else {
                    this.scale = this.maxScale;
                    this.isBirth = false;
                }
            }
            if (this.ticker != null) {
                this.ticker.accept(this);
            }
            this.prevAge = player.tickCount;
        }
    }

    public void render(PoseStack matrices, SubmitNodeCollector queue, int light, AbstractClientPlayer player, float tickProgress) {
        if (!this.dead) {
            matrices.pushPose();
            matrices.translate(this.x, this.y, this.z);
            matrices.scale((float)this.scale, (float)this.scale, (float)this.scale);
            RenderType renderLayer;
            if ("ADDITIVE".equals(this.particleRenderType)) {
                renderLayer = ParticleRenderLayers.additiveParticle(this.texture);
            } else {
                renderLayer = RenderTypes.breezeWind(this.texture, 0.0f, 0.0f);
            }
            Matrix4f matrix = matrices.last().pose();
            Vector3f translation = new Vector3f();
            Vector3f scale = new Vector3f();
            translation = matrix.getTranslation(translation);
            scale = matrix.getScale(scale);
            matrix = matrix.identity().translate((Vector3fc)translation).scale((Vector3fc)scale);
            matrix.rotateX((float)(this.rx * 0.01745329238474369));
            matrix.rotateY((float)(this.ry * 0.01745329238474369));
            matrix.rotateZ((float)(this.rz * 0.01745329238474369));
            if (Objects.equals(this.particleRenderType, "CUTOUT_L")) {
                light = LightCoordsUtil.lightCoordsWithEmission(15, 15);
            }
            double size = 0.5;
            double halfSize = size / 2.0;
            Vector3f normal = new Vector3f(0.0f, 0.0f, 1.0f);
            Matrix4f finalMatrix = matrix;
            int finalLight = light;
            queue.submitCustomGeometry(matrices, renderLayer, (peek, vertexConsumer) -> {
                vertexConsumer.addVertex(peek, (float)halfSize, (float)halfSize, 0.0f).setColor(255, 255, 255, Math.clamp((long)((int)this.alpha), 0, 255)).setUv(1.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(finalLight).setNormal(normal.x, normal.y, normal.z);
                vertexConsumer.addVertex(peek, (float)(-halfSize), (float)halfSize, 0.0f).setColor(255, 255, 255, Math.clamp((long)((int)this.alpha), 0, 255)).setUv(0.0f, 0.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(finalLight).setNormal(normal.x, normal.y, normal.z);
                vertexConsumer.addVertex(peek, (float)(-halfSize), (float)(-halfSize), 0.0f).setColor(255, 255, 255, Math.clamp((long)((int)this.alpha), 0, 255)).setUv(0.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(finalLight).setNormal(normal.x, normal.y, normal.z);
                vertexConsumer.addVertex(peek, (float)halfSize, (float)(-halfSize), 0.0f).setColor(255, 255, 255, Math.clamp((long)((int)this.alpha), 0, 255)).setUv(1.0f, 1.0f).setOverlay(OverlayTexture.NO_OVERLAY).setLight(finalLight).setNormal(normal.x, normal.y, normal.z);
            });
            matrices.popPose();
        }
    }
}
