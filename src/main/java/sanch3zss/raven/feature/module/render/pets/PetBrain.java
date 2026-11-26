package sanch3zss.raven.feature.module.render.pets;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class PetBrain {
    public enum Kind { BIRD, INSECT, WALKER }

    public enum Pose { FLAP, HOVER, GLIDE, STAND, PERCH, WALK, SLIDE, SWIM, DANCE, SLAP }

    private enum State { FLY, PERCH, GROUND, ATTACK }

    private final RandomSource random = RandomSource.create();
    private ClientLevel level;
    private LocalPlayer owner;
    private Kind kind;
    private boolean soaring;
    private State state = State.FLY;
    private Pose pose = Pose.FLAP;
    private Pose previousPose = Pose.FLAP;
    private int poseChangedAt;
    private float previousPoseTime;
    private float animationTime;
    private float previousAnimationTime;
    private Vec3 position;
    private Vec3 previousPosition;
    private Vec3 velocity = Vec3.ZERO;
    private double fallSpeed;
    private Vec3 goal;
    private boolean goalOnShoulder;
    private Vec3 wander;
    private int wanderTicks;
    private LivingEntity chaseTarget;
    private int chaseTicks;
    private int strikeCooldown;
    private int diveTicks;
    private double orbitAngle;
    private int ticks;
    private int stateTicks;
    private int ownerIdleTicks;
    private int steadyTicks;
    private int stuckTicks;
    private int groomTicks;
    private float yaw;
    private float previousYaw;
    private float targetYaw;
    private float turnRate = 0.25f;
    private float yawSpeed;
    private float pitch;
    private float previousPitch;
    private float bank;
    private float previousBank;
    private float headYaw;
    private float previousHeadYaw;
    private float lookOffset;
    private int lookTicks;
    private boolean callRequested;

    public void reset() {
        level = null;
        owner = null;
        position = null;
        previousPosition = null;
        velocity = Vec3.ZERO;
        goal = null;
        goalOnShoulder = false;
        wander = null;
        chaseTarget = null;
        chaseTicks = 0;
        diveTicks = 0;
        yawSpeed = 0.0f;
        ticks = 0;
        state = State.FLY;
        pose = Pose.FLAP;
        previousPose = Pose.FLAP;
        poseChangedAt = 0;
        animationTime = 0.0f;
        previousAnimationTime = 0.0f;
        headYaw = 0.0f;
        previousHeadYaw = 0.0f;
        lookOffset = 0.0f;
    }

    public void chase(LivingEntity target, LocalPlayer owner) {
        if (position == null || target == owner || !target.isAlive() || target.level() != owner.level()
                || target.distanceToSqr(owner) > 100.0) {
            return;
        }
        if (chaseTarget != target || state != State.ATTACK) {
            if (chaseTarget != target) callRequested = true;
            strikeCooldown = 6;
            diveTicks = 0;
            orbitAngle = Math.atan2(position.z - target.getZ(), position.x - target.getX());
        }
        chaseTarget = target;
        chaseTicks = kind == Kind.WALKER ? 90 : 70;
        wander = null;
        if (state != State.ATTACK) setState(State.ATTACK);
    }

    public void tick(LocalPlayer owner, Kind kind, boolean soaring, boolean perchAllowed) {
        ClientLevel currentLevel = (ClientLevel) owner.level();
        if (level != currentLevel || this.kind != kind) {
            reset();
            level = currentLevel;
            this.kind = kind;
        }
        this.owner = owner;
        this.soaring = soaring;
        ticks++;
        stateTicks++;
        double ownerSpeed = owner.position().subtract(owner.xo, owner.yo, owner.zo).horizontalDistance();
        ownerIdleTicks = ownerSpeed < 0.02 && owner.onGround() ? ownerIdleTicks + 1 : 0;
        if (position == null || position.distanceToSqr(owner.position()) > 576.0) {
            spawn();
        }
        previousPosition = position;
        previousYaw = yaw;
        previousPitch = pitch;
        previousBank = bank;
        previousHeadYaw = headYaw;
        previousAnimationTime = animationTime;

        if (state == State.ATTACK && (chaseTicks-- <= 0 && diveTicks == 0 || chaseTarget == null || !chaseTarget.isAlive()
                || chaseTarget.level() != currentLevel || chaseTarget.distanceToSqr(owner) > 196.0)) {
            chaseTarget = null;
            setState(kind == Kind.WALKER ? State.GROUND : State.FLY);
        }

        switch (kind) {
            case BIRD -> tickBird(ownerSpeed, perchAllowed);
            case INSECT -> tickInsect(ownerSpeed);
            case WALKER -> tickWalker();
        }
        float desiredTurn = Mth.clamp(Mth.wrapDegrees(targetYaw - yaw) * turnRate, -14.0f, 14.0f);
        yawSpeed += (desiredTurn - yawSpeed) * 0.3f;
        yaw += yawSpeed;
        updateHead();
        animationTime += 0.05f * animationSpeed();
    }

    private void spawn() {
        if (kind == Kind.WALKER) {
            Vec3 spot = groundSpot(owner.position(), 1.0, 2.0);
            position = spot != null ? spot : owner.position();
            setState(State.GROUND);
        } else {
            position = owner.position().add(side(owner, 1.0f).scale(1.0)).add(0.0, 1.8, 0.0);
            setState(State.FLY);
        }
        previousPosition = position;
        velocity = Vec3.ZERO;
        fallSpeed = 0.0;
        yaw = owner.yBodyRot;
        targetYaw = yaw;
        yawSpeed = 0.0f;
    }

    private void tickBird(double ownerSpeed, boolean perchAllowed) {
        switch (state) {
            case PERCH -> {
                position = shoulder(owner, 1.0f);
                velocity = Vec3.ZERO;
                yaw = owner.yBodyRot;
                targetYaw = yaw;
                yawSpeed = 0.0f;
                settle();
                setPose(Pose.PERCH);
                boolean disturbed = !perchAllowed || owner.isSprinting() || ownerSpeed > 0.3 || owner.isInWater()
                        || owner.isFallFlying() || owner.hurtTime > 0 || owner.getDeltaMovement().y < -0.6;
                if (disturbed) {
                    takeOff(0.25);
                } else if (stateTicks > 600 && random.nextInt(800) == 0) {
                    Vec3 spot = groundSpot(owner.position(), 1.5, 3.5);
                    if (spot != null) {
                        takeOff(0.15);
                        goal = spot;
                    }
                } else if (random.nextInt(1200) == 0) {
                    callRequested = true;
                }
            }
            case GROUND -> {
                double ground = groundY(position.x, position.y + 0.5, position.z);
                if (Double.isNaN(ground) || position.distanceTo(owner.position()) > 6.0 || ownerSpeed > 0.28
                        || owner.isFallFlying()) {
                    takeOff(0.3);
                    return;
                }
                if (goal == null && stateTicks > 30 && random.nextInt(80) == 0) {
                    Vec3 step = groundSpot(position, 0.4, 1.3);
                    if (step != null && step.distanceTo(owner.position()) < 4.5) goal = step;
                }
                if (goal != null) {
                    Vec3 flat = new Vec3(goal.x - position.x, 0.0, goal.z - position.z);
                    if (flat.length() < 0.05) {
                        goal = null;
                    } else {
                        velocity = flat.normalize().scale(Math.min(0.045, flat.length()));
                        position = position.add(velocity);
                        turnTowards(goal, 0.3f);
                    }
                } else {
                    velocity = Vec3.ZERO;
                    if (stateTicks % 40 == 0 && Math.abs(Mth.wrapDegrees(yawTo(owner.position()) - yaw)) > 40.0f) {
                        turnTowards(owner.position(), 0.12f);
                    }
                }
                position = new Vec3(position.x, position.y + (ground - position.y) * 0.5, position.z);
                settle();
                if (goal != null) setPose(Pose.WALK);
                else setPose(Pose.STAND);
                if (perchAllowed && ownerIdleTicks > 200 && stateTicks > 400 && random.nextInt(700) == 0
                        && canPerch()) {
                    takeOff(0.2);
                    goalOnShoulder = true;
                } else if (random.nextInt(1500) == 0) {
                    callRequested = true;
                }
            }
            case FLY -> flyBird(ownerSpeed, perchAllowed);
            case ATTACK -> attackBird();
        }
    }

    private void flyBird(double ownerSpeed, boolean perchAllowed) {
        if ((goal != null || goalOnShoulder) && (ownerSpeed > 0.25 || stateTicks > 400
                || goalOnShoulder && (!perchAllowed || !canPerch()))) {
            goal = null;
            goalOnShoulder = false;
        }
        boolean landing = goal != null || goalOnShoulder;
        boolean circling = !landing && soaring && ownerSpeed < 0.2 && !owner.isFallFlying();
        Vec3 destination;
        if (goalOnShoulder) {
            destination = shoulder(owner, 1.0f);
        } else if (goal != null) {
            destination = goal;
        } else if (circling) {
            double angle = ticks * 0.04;
            destination = owner.position().add(Math.cos(angle) * 2.8, 3.4 + Math.sin(ticks * 0.05) * 0.3,
                    Math.sin(angle) * 2.8);
        } else {
            destination = home(owner);
        }
        double distance = position.distanceTo(destination);
        double limit = landing ? Math.max(0.12, Math.min(0.3, distance * 0.35))
                : Math.max(0.24, ownerSpeed * 1.35 + distance * 0.18);
        steer(destination, 0.11, 0.74, limit);
        keepAboveGround(landing);

        if (goalOnShoulder && position.distanceTo(shoulder(owner, 1.0f)) < 0.2) {
            setState(State.PERCH);
            position = shoulder(owner, 1.0f);
            return;
        }
        if (goal != null && position.distanceTo(goal) < 0.15) {
            position = goal;
            velocity = Vec3.ZERO;
            setState(State.GROUND);
            return;
        }
        if (!landing && ownerIdleTicks > 40 && stateTicks > 60) {
            if (perchAllowed && canPerch() && random.nextInt(soaring ? 200 : 60) == 0) {
                goalOnShoulder = true;
            } else if (random.nextInt(soaring ? 240 : 100) == 0) {
                goal = groundSpot(owner.position(), 1.2, 3.0);
            }
        }

        double horizontal = velocity.horizontalDistance();
        steadyTicks = Math.abs(velocity.y) < 0.03 && horizontal > 0.1 ? steadyTicks + 1 : 0;
        if (landing && distance < 1.2) setPose(Pose.HOVER);
        else if (circling || horizontal > 0.16 && velocity.y < 0.03 && steadyTicks > 12) setPose(Pose.GLIDE);
        else setPose(Pose.FLAP);
        orientFlight(owner.getEyePosition());
    }

    private void attackBird() {
        Vec3 strikePoint = chaseTarget.position().add(0.0, chaseTarget.getBbHeight() * 0.7, 0.0);
        if (diveTicks > 0) {
            diveTicks--;
            steer(strikePoint, 0.2, 0.8, soaring ? 0.6 : 0.45);
            double distance = position.distanceTo(strikePoint);
            if (distance < 0.5 || diveTicks == 0) {
                diveTicks = 0;
                strikeCooldown = 24 + random.nextInt(12);
                orbitAngle = Math.atan2(position.z - chaseTarget.getZ(), position.x - chaseTarget.getX()) + Math.PI;
            }
            setPose(distance < 1.3 ? Pose.FLAP : Pose.GLIDE);
        } else {
            orbitAngle += 0.07;
            double radius = 1.6 + chaseTarget.getBbWidth() * 0.5;
            Vec3 orbit = chaseTarget.position().add(Math.cos(orbitAngle) * radius,
                    chaseTarget.getBbHeight() + 1.3, Math.sin(orbitAngle) * radius);
            steer(orbit, 0.1, 0.8, 0.32);
            if (strikeCooldown > 0) strikeCooldown--;
            else if (chaseTicks > 0 && position.distanceTo(orbit) < 1.2) diveTicks = 24;
            setPose(strikeCooldown < 6 ? Pose.GLIDE : Pose.FLAP);
        }
        keepAboveGround(false);
        orientFlight(strikePoint);
    }

    private void tickInsect(double ownerSpeed) {
        switch (state) {
            case GROUND -> {
                double ground = groundY(position.x, position.y + 0.3, position.z);
                double ownerDistance = new Vec3(owner.getX() - position.x, 0.0, owner.getZ() - position.z).length();
                if (Double.isNaN(ground) || ownerDistance < 0.9 || ownerDistance > 4.0 || ownerSpeed > 0.2
                        || stateTicks > 160 + random.nextInt(200)) {
                    setState(State.FLY);
                    velocity = new Vec3(random.nextGaussian() * 0.05, 0.16, random.nextGaussian() * 0.05);
                    return;
                }
                if (goal == null && groomTicks <= 0 && random.nextInt(40) == 0) {
                    Vec3 step = position.add(random.nextGaussian() * 0.25, 0.0, random.nextGaussian() * 0.25);
                    double stepGround = groundY(step.x, position.y + 0.3, step.z);
                    if (!Double.isNaN(stepGround) && Math.abs(stepGround - ground) < 0.2) {
                        goal = new Vec3(step.x, stepGround, step.z);
                    }
                } else if (goal == null && groomTicks <= 0 && random.nextInt(60) == 0) {
                    groomTicks = 30 + random.nextInt(50);
                }
                if (groomTicks > 0) groomTicks--;
                if (goal != null) {
                    Vec3 flat = new Vec3(goal.x - position.x, 0.0, goal.z - position.z);
                    if (flat.length() < 0.02) {
                        goal = null;
                    } else {
                        velocity = flat.normalize().scale(Math.min(0.014, flat.length()));
                        position = position.add(velocity);
                        turnTowards(goal, 0.4f);
                    }
                } else {
                    velocity = Vec3.ZERO;
                }
                position = new Vec3(position.x, ground, position.z);
                settle();
                setPose(goal != null ? Pose.WALK : Pose.STAND);
            }
            case ATTACK -> {
                if (wander == null || wanderTicks-- <= 0) {
                    wander = chaseTarget.getEyePosition().add(random.nextGaussian() * 0.45,
                            random.nextGaussian() * 0.25, random.nextGaussian() * 0.45);
                    wanderTicks = 3 + random.nextInt(6);
                }
                steer(wander, 0.35, 0.58, 0.55);
                buzz();
                setPose(Pose.FLAP);
                orientFlight(chaseTarget.getEyePosition());
            }
            default -> {
                if (goal == null && (wander == null || wanderTicks-- <= 0
                        || wander.distanceTo(owner.position()) > 3.0)) {
                    Vec3 side = side(owner, 1.0f);
                    wander = owner.position().add(side.scale(0.55 + random.nextDouble() * 0.5))
                            .add(random.nextGaussian() * 0.35, owner.getBbHeight() * 0.75
                                    + random.nextGaussian() * 0.25, random.nextGaussian() * 0.35);
                    wanderTicks = 6 + random.nextInt(24);
                }
                Vec3 destination = goal != null ? goal : wander;
                steer(destination, goal != null ? 0.14 : 0.28, 0.6,
                        goal != null ? 0.16 : Math.max(0.5, ownerSpeed * 1.4));
                if (goal == null) buzz();
                keepAboveGround(goal != null);
                if (goal != null && position.distanceTo(goal) < 0.06) {
                    position = goal;
                    velocity = Vec3.ZERO;
                    setState(State.GROUND);
                    return;
                }
                if (goal == null && ownerIdleTicks > 30 && stateTicks > 80 && random.nextInt(50) == 0) {
                    goal = groundSpot(owner.position(), 1.0, 2.2);
                }
                setPose(Pose.FLAP);
                orientFlight(owner.getEyePosition());
            }
        }
    }

    private void tickWalker() {
        boolean inWater = isWater(position.add(0.0, 0.3, 0.0));
        if (state == State.ATTACK) {
            Vec3 target = chaseTarget.position();
            double distance = new Vec3(target.x - position.x, 0.0, target.z - position.z).length();
            double reach = chaseTarget.getBbWidth() * 0.5 + 0.6;
            if (distance > reach) {
                boolean sliding = distance > 3.5;
                walk(target, sliding ? 0.3 : 0.13);
                setPose(inWater ? Pose.SWIM : sliding ? Pose.SLIDE : Pose.WALK);
            } else {
                velocity = Vec3.ZERO;
                turnTowards(target, 0.35f);
                setPose(Pose.SLAP);
            }
            fall();
            settle();
            return;
        }
        float bodyYaw = owner.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 behind = new Vec3(Math.sin(bodyYaw), 0.0, -Math.cos(bodyYaw));
        Vec3 fromOwner = new Vec3(position.x - owner.getX(), 0.0, position.z - owner.getZ());
        double ownerDistance = fromOwner.length();
        Vec3 home;
        if (ownerIdleTicks > 10 && ownerDistance > 0.01) {
            home = owner.position().add(fromOwner.scale(Mth.clamp(ownerDistance, 1.1, 2.4) / ownerDistance));
        } else {
            home = owner.position().add(side(owner, 1.0f).scale(0.9)).add(behind.scale(0.9));
        }
        double distance = new Vec3(home.x - position.x, 0.0, home.z - position.z).length();
        if (distance > 20.0 || stuckTicks > 40 && distance > 3.0
                || Math.abs(owner.getY() - position.y) > 8.0 && owner.onGround()) {
            Vec3 spot = groundSpot(owner.position(), 1.0, 2.0);
            if (spot != null) {
                position = spot;
                previousPosition = spot;
                stuckTicks = 0;
            }
        }
        if (inWater) {
            Vec3 swimTarget = owner.isInWater() ? owner.position() : home;
            steer(swimTarget, 0.06, 0.8, 0.22);
            if (!isWater(position.add(0.0, 0.5, 0.0)) && velocity.y > 0.0) {
                velocity = new Vec3(velocity.x, 0.0, velocity.z);
            }
            fallSpeed = 0.0;
            setPose(Pose.SWIM);
            turnTowards(position.add(velocity), 0.25f);
            settle();
            return;
        }
        boolean sliding = pose == Pose.SLIDE ? distance > 2.0 : distance > 6.5;
        if (sliding) {
            walk(home, 0.34);
            setPose(Pose.SLIDE);
        } else if (distance > (pose == Pose.WALK ? 0.4 : 1.4)) {
            walk(home, 0.1);
            setPose(Pose.WALK);
        } else {
            velocity = Vec3.ZERO;
            if (stateTicks % 30 == 0 && Math.abs(Mth.wrapDegrees(yawTo(owner.position()) - yaw)) > 40.0f) {
                turnTowards(owner.position(), 0.12f);
            }
            setPose(ownerIdleTicks > 400 ? Pose.DANCE : Pose.STAND);
            if (random.nextInt(1500) == 0) callRequested = true;
        }
        fall();
        settle();
    }

    private void walk(Vec3 toward, double speed) {
        Vec3 flat = new Vec3(toward.x - position.x, 0.0, toward.z - position.z);
        double length = flat.length();
        if (length < 0.001) return;
        Vec3 step = length > speed ? flat.scale(speed / length) : flat;
        double nextX = position.x + step.x;
        double nextZ = position.z + step.z;
        double ground = groundY(nextX, position.y + 1.1, nextZ);
        if (Double.isNaN(ground) || ground > position.y + 1.1 || !isFree(nextX, ground + 1.0, nextZ)) {
            stuckTicks++;
            velocity = Vec3.ZERO;
        } else {
            stuckTicks = 0;
            velocity = step;
            position = new Vec3(nextX, Math.max(position.y, Math.min(ground, position.y + 0.4)), nextZ);
        }
        turnTowards(position.add(step), 0.3f);
    }

    private void fall() {
        double ground = groundY(position.x, position.y + 0.2, position.z);
        if (Double.isNaN(ground)) {
            fallSpeed = Math.max(fallSpeed - 0.08, -0.8);
            position = position.add(0.0, fallSpeed, 0.0);
        } else if (position.y > ground) {
            fallSpeed = Math.max(fallSpeed - 0.08, -0.8);
            position = new Vec3(position.x, Math.max(ground, position.y + fallSpeed), position.z);
        } else {
            fallSpeed = 0.0;
            position = new Vec3(position.x, ground, position.z);
        }
    }

    private void takeOff(double lift) {
        setState(State.FLY);
        velocity = new Vec3(random.nextGaussian() * 0.05, lift, random.nextGaussian() * 0.05);
        if (random.nextInt(4) == 0) callRequested = true;
    }

    private void steer(Vec3 destination, double acceleration, double damping, double limit) {
        velocity = velocity.add(destination.subtract(position).scale(acceleration)).scale(damping);
        if (velocity.lengthSqr() > limit * limit) {
            velocity = velocity.normalize().scale(limit);
        }
        position = position.add(velocity);
    }

    private void buzz() {
        velocity = velocity.add(random.nextGaussian() * 0.012, random.nextGaussian() * 0.01,
                random.nextGaussian() * 0.012);
    }

    private void keepAboveGround(boolean landing) {
        double ground = groundY(position.x, position.y + 0.5, position.z);
        if (!Double.isNaN(ground) && position.y < ground + (landing ? 0.0 : 0.3)) {
            position = new Vec3(position.x, ground + (landing ? 0.0 : 0.3), position.z);
            if (velocity.y < 0.0) velocity = new Vec3(velocity.x, 0.0, velocity.z);
        }
    }

    private void orientFlight(Vec3 lookAt) {
        double speed = velocity.horizontalDistance();
        Vec3 facing = speed > 0.06 ? velocity : lookAt.subtract(position);
        if (facing.horizontalDistanceSqr() > 0.0001) {
            turnTowards(position.add(facing), speed > 0.06 ? 0.22f : 0.1f);
        }
        float targetPitch = (float) -Math.toDegrees(Math.atan2(velocity.y, Math.max(0.05, speed)));
        pitch += (Mth.clamp(targetPitch, -35.0f, 35.0f) - pitch) * 0.25f;
        float targetBank = Mth.clamp(yawSpeed * -2.4f, -32.0f, 32.0f);
        bank += (targetBank - bank) * 0.2f;
    }

    private void settle() {
        pitch += -pitch * 0.3f;
        bank += -bank * 0.3f;
    }

    private void turnTowards(Vec3 point, float speed) {
        double dx = point.x - position.x;
        double dz = point.z - position.z;
        if (dx * dx + dz * dz < 0.0001) return;
        targetYaw = yawTo(point);
        turnRate = speed;
    }

    private float yawTo(Vec3 point) {
        return (float) Math.toDegrees(Math.atan2(-(point.x - position.x), point.z - position.z));
    }

    private void updateHead() {
        float target = 0.0f;
        if (pose == Pose.STAND || pose == Pose.PERCH || pose == Pose.WALK) {
            if (lookTicks-- <= 0) {
                lookOffset = random.nextInt(3) == 0 ? (random.nextFloat() - 0.5f) * 80.0f : 0.0f;
                lookTicks = 40 + random.nextInt(80);
            }
            if (state == State.PERCH) {
                target = lookOffset * 0.6f;
            } else {
                float toOwner = Mth.wrapDegrees(yawTo(owner.getEyePosition()) - yaw);
                target = Math.abs(toOwner) < 90.0f ? toOwner + lookOffset : lookOffset;
            }
        }
        float step = (Mth.clamp(target, -60.0f, 60.0f) - headYaw) * 0.12f;
        headYaw += Mth.clamp(step, -6.0f, 6.0f);
    }

    private float animationSpeed() {
        return switch (pose) {
            case FLAP -> 1.0f + (float) Math.min(1.0, velocity.length() * 3.0) * 0.35f;
            case HOVER -> 1.35f;
            case WALK -> kind == Kind.WALKER ? 1.2f : 1.0f;
            default -> 1.0f;
        };
    }

    private void setState(State state) {
        this.state = state;
        stateTicks = 0;
        goal = null;
        goalOnShoulder = false;
        stuckTicks = 0;
        groomTicks = 0;
    }

    private void setPose(Pose pose) {
        if (this.pose == pose) return;
        previousPose = this.pose;
        previousPoseTime = animationTime;
        this.pose = pose;
        poseChangedAt = ticks;
        animationTime = 0.0f;
        previousAnimationTime = 0.0f;
    }

    private boolean canPerch() {
        return owner.onGround() && !owner.isInWater() && !owner.isFallFlying() && !owner.isSleeping();
    }

    private static Vec3 side(LocalPlayer owner, float partialTick) {
        float angle = Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot) * Mth.DEG_TO_RAD;
        return new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
    }

    private static Vec3 shoulder(LocalPlayer owner, float partialTick) {
        return owner.getPosition(partialTick).add(side(owner, partialTick).scale(0.43))
                .add(0.0, owner.isCrouching() ? 1.17 : 1.42, 0.0);
    }

    private Vec3 home(LocalPlayer owner) {
        float angle = owner.yBodyRot * Mth.DEG_TO_RAD;
        Vec3 behind = new Vec3(Math.sin(angle), 0.0, -Math.cos(angle));
        double drift = Math.sin(ticks * 0.075) * 0.23;
        double bob = Math.sin(ticks * 0.19) * 0.11;
        return owner.position().add(side(owner, 1.0f).scale(1.12 + drift)).add(behind.scale(0.65))
                .add(0.0, 1.9 + bob, 0.0);
    }

    private Vec3 groundSpot(Vec3 center, double minRadius, double maxRadius) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;
            double y = groundY(x, center.y + 1.5, z);
            if (!Double.isNaN(y) && Math.abs(y - center.y) < 2.5 && !isWater(new Vec3(x, y + 0.1, z))) {
                return new Vec3(x, y, z);
            }
        }
        return null;
    }

    private double groundY(double x, double fromY, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int blockX = Mth.floor(x);
        int blockZ = Mth.floor(z);
        for (int y = Mth.floor(fromY); y >= Mth.floor(fromY) - 6; y--) {
            pos.set(blockX, y, blockZ);
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (shape.isEmpty()) continue;
            double top = y + shape.max(Direction.Axis.Y);
            if (top <= fromY && isFree(x, top, z)) return top;
        }
        return Double.NaN;
    }

    private boolean isFree(double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y + 0.05, z);
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private boolean isWater(Vec3 point) {
        return level.getFluidState(BlockPos.containing(point)).is(FluidTags.WATER);
    }

    public boolean consumeCall() {
        boolean call = callRequested;
        callRequested = false;
        return call;
    }

    public boolean visible() {
        return position != null;
    }

    public boolean perched() {
        return state == State.PERCH;
    }

    public boolean landed() {
        return state == State.GROUND;
    }

    public boolean grooming() {
        return groomTicks > 0;
    }

    public Pose pose() {
        return pose;
    }

    public Pose previousPose() {
        return previousPose;
    }

    public float previousPoseTime() {
        return previousPoseTime;
    }

    public float poseBlend(float partialTick) {
        return Mth.clamp((ticks - poseChangedAt + partialTick) / 5.0f, 0.0f, 1.0f);
    }

    public float animationTime(float partialTick) {
        return Mth.lerp(partialTick, previousAnimationTime, animationTime);
    }

    public Vec3 position(float partialTick) {
        if (state == State.PERCH && owner != null) return shoulder(owner, partialTick);
        return previousPosition.lerp(position, partialTick);
    }

    public float yaw(float partialTick) {
        if (state == State.PERCH && owner != null) return Mth.rotLerp(partialTick, owner.yBodyRotO, owner.yBodyRot);
        return previousYaw + Mth.wrapDegrees(yaw - previousYaw) * partialTick;
    }

    public float pitch(float partialTick) {
        return Mth.lerp(partialTick, previousPitch, pitch);
    }

    public float bank(float partialTick) {
        return Mth.lerp(partialTick, previousBank, bank);
    }

    public float headYaw(float partialTick) {
        return Mth.lerp(partialTick, previousHeadYaw, headYaw);
    }

    public float age(float partialTick) {
        return ticks + partialTick;
    }

    public float movement() {
        return (float) Math.min(1.0, velocity.length() * 4.0);
    }
}
