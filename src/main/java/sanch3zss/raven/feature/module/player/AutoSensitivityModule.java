package sanch3zss.raven.feature.module.player;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.ChatUtil;
import sanch3zss.raven.client.config.ConfigApplyContext;
import sanch3zss.raven.event.client.MouseButtonEvent;
import sanch3zss.raven.event.player.ChangeLookDirectionEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.ButtonAction;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

@Aliases(aliases = {"Auto Sensitivity", "AutoSens", "Auto Sens", "Сенса", "Чувствительность", "Подбор сенсы"})
public class AutoSensitivityModule extends Module {
    public static final Identifier APPLY = Identifier.fromNamespaceAndPath("raven", "auto_sensitivity/apply");
    public static final Identifier KEEP = Identifier.fromNamespaceAndPath("raven", "auto_sensitivity/keep");

    private final List<Float> flicks = new ArrayList<>();
    private Entity target;
    private float lastTargetYaw;
    private float lastTargetPitch;
    private float errorYaw;
    private float errorPitch;
    private float targetRadius;
    private float moveYaw;
    private float movePitch;
    private boolean flicking;
    private float flickYaw;
    private float flickPitch;
    private float flickPeak;
    private float flickTime;
    private float trackTime;
    private float trackLag;
    private int hits;
    private int misses;
    private int overshootMisses;
    private int undershootMisses;
    private long lastFrame;
    private long startedAt;
    private Double pendingSensitivity;

    public AutoSensitivityModule() {
        super(ModuleTab.PLAYER, "AutoSensitivity");
        register(ChangeLookDirectionEvent.class, event -> {
            if (isState()) onLook((float) event.deltaX() * 0.15f, (float) event.deltaY() * 0.15f);
        });
        register(MouseButtonEvent.class, event -> {
            if (isState() && event.action() == ButtonAction.PRESS && event.button() == 0) onAttack();
        });
    }

    @Override
    public void activate() {
        super.activate();
        if (ConfigApplyContext.isApplying()) return;
        reset();
        startedAt = System.currentTimeMillis();
        ChatUtil.addChatMessage("Слежу за наводкой. Поиграй как обычно и выключи модуль, чтобы получить совет по сенсе.");
    }

    @Override
    public void deactivate() {
        super.deactivate();
        if (ConfigApplyContext.isApplying() || startedAt == 0L || Mc.INSTANCE.getPlayer() == null) return;
        suggest();
        startedAt = 0L;
    }

    public void onClick(Identifier id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (id.equals(APPLY) && pendingSensitivity != null) {
            minecraft.options.sensitivity().set(pendingSensitivity);
            minecraft.options.save();
            ChatUtil.addChatMessage("Сенса изменена на " + percent(pendingSensitivity) + "%.");
        } else if (id.equals(KEEP) && pendingSensitivity != null) {
            ChatUtil.addChatMessage("Оставил сенсу " + percent(minecraft.options.sensitivity().get()) + "%.");
        }
        pendingSensitivity = null;
    }

    private void reset() {
        flicks.clear();
        target = null;
        flicking = false;
        trackTime = 0.0f;
        trackLag = 0.0f;
        hits = 0;
        misses = 0;
        overshootMisses = 0;
        undershootMisses = 0;
        lastFrame = 0L;
        pendingSensitivity = null;
    }

    private void onLook(float deltaYaw, float deltaPitch) {
        Mc mc = Mc.INSTANCE;
        LocalPlayer player = mc.getPlayer();
        long now = System.nanoTime();
        float delta = lastFrame == 0L ? 0.0f : Math.min(0.1f, (now - lastFrame) * 1.0e-9f);
        lastFrame = now;
        if (player == null || mc.getWorld() == null || mc.getCurrentScreen() != null || delta <= 0.0f) {
            target = null;
            flicking = false;
            return;
        }
        float partialTick = mc.getRenderTickCounter().getGameTimeDeltaPartialTick(true);
        float viewYaw = player.getYRot() + deltaYaw;
        float viewPitch = Mth.clamp(player.getXRot() + deltaPitch, -90.0f, 90.0f);
        Vec3 eye = player.getEyePosition(partialTick);
        moveYaw += (deltaYaw / delta - moveYaw) * Math.min(1.0f, delta * 20.0f);
        movePitch += (deltaPitch / delta - movePitch) * Math.min(1.0f, delta * 20.0f);

        Entity best = null;
        float bestError = 45.0f;
        for (Entity entity : mc.getWorld().entitiesForRendering()) {
            if (entity == player || !(entity instanceof LivingEntity living) || !living.isAlive() || entity.isSpectator()
                    || entity.distanceToSqr(player) > 48.0 * 48.0) continue;
            Vec3 center = entity.getPosition(partialTick).add(0.0, entity.getBbHeight() * 0.5, 0.0);
            float error = (float) Math.hypot(Mth.wrapDegrees(yawTo(eye, center) - viewYaw), pitchTo(eye, center) - viewPitch);
            if (error < bestError) {
                best = entity;
                bestError = error;
            }
        }
        if (best == null) {
            target = null;
            flicking = false;
            return;
        }
        Vec3 center = best.getPosition(partialTick).add(0.0, best.getBbHeight() * 0.5, 0.0);
        float targetYaw = yawTo(eye, center);
        float targetPitch = pitchTo(eye, center);
        double distance = Math.max(0.5, eye.distanceTo(center));
        targetRadius = Math.max(0.75f, (float) Math.toDegrees(Math.atan(best.getBbWidth() * 0.5 / distance)));
        errorYaw = Mth.wrapDegrees(targetYaw - viewYaw);
        errorPitch = targetPitch - viewPitch;
        float error = (float) Math.hypot(errorYaw, errorPitch);
        boolean sameTarget = best == target;
        float targetSpeedYaw = sameTarget ? Mth.wrapDegrees(targetYaw - lastTargetYaw) / delta : 0.0f;
        float targetSpeedPitch = sameTarget ? (targetPitch - lastTargetPitch) / delta : 0.0f;
        target = best;
        lastTargetYaw = targetYaw;
        lastTargetPitch = targetPitch;
        if (!sameTarget) flicking = false;

        float mouseSpeed = (float) Math.hypot(deltaYaw, deltaPitch) / delta;
        if (!flicking) {
            float startError = (float) Math.hypot(errorYaw + deltaYaw, errorPitch + deltaPitch);
            float towards = (deltaYaw * (errorYaw + deltaYaw) + deltaPitch * (errorPitch + deltaPitch))
                    / Math.max(1.0e-4f, (float) Math.hypot(deltaYaw, deltaPitch) * startError);
            if (startError > Math.max(6.0f, targetRadius * 4.0f) && mouseSpeed > 60.0f && towards > 0.7f) {
                flicking = true;
                flickYaw = errorYaw + deltaYaw;
                flickPitch = errorPitch + deltaPitch;
                flickPeak = mouseSpeed;
                flickTime = 0.0f;
            }
        } else {
            flickPeak = Math.max(flickPeak, mouseSpeed);
            flickTime += delta;
            float length = (float) Math.hypot(flickYaw, flickPitch);
            boolean reversed = deltaYaw * flickYaw + deltaPitch * flickPitch < 0.0f;
            if (reversed || mouseSpeed < Math.min(40.0f, flickPeak * 0.2f) || flickTime > 0.6f) {
                float left = (errorYaw * flickYaw + errorPitch * flickPitch) / length;
                flicks.add(Mth.clamp(1.0f - left / length, 0.3f, 2.0f));
                flicking = false;
            }
        }

        float targetSpeed = (float) Math.hypot(targetSpeedYaw, targetSpeedPitch);
        if (!flicking && error < Math.min(15.0f, targetRadius * 6.0f) && targetSpeed > 15.0f && targetSpeed < 400.0f) {
            float lag = (errorYaw * targetSpeedYaw + errorPitch * targetSpeedPitch) / targetSpeed / targetRadius;
            trackLag += Mth.clamp(lag, -3.0f, 3.0f) * delta;
            trackTime += delta;
        }
    }

    private void onAttack() {
        Mc mc = Mc.INSTANCE;
        if (target == null || mc.getPlayer() == null || mc.getCurrentScreen() != null) return;
        if (mc.getCrosshairTarget() instanceof EntityHitResult) {
            hits++;
            return;
        }
        if (Math.hypot(errorYaw, errorPitch) > 20.0f) return;
        misses++;
        float ahead = -(errorYaw * moveYaw + errorPitch * movePitch);
        if (Math.hypot(moveYaw, movePitch) < 5.0f) return;
        if (ahead > 0.0f) {
            overshootMisses++;
        } else {
            undershootMisses++;
        }
    }

    private void suggest() {
        Minecraft minecraft = Minecraft.getInstance();
        OptionInstance<Double> option = minecraft.options.sensitivity();
        double current = option.get();
        int attacks = hits + misses;
        if (flicks.size() < 8 && trackTime < 10.0f && attacks < 10) {
            ChatUtil.addChatMessage("Мало данных для совета. Поиграй подольше с противниками и выключи модуль ещё раз.");
            return;
        }

        List<Float> sorted = new ArrayList<>(flicks);
        sorted.sort(Float::compare);
        float median = sorted.isEmpty() ? 1.0f : sorted.get(sorted.size() / 2);
        int overshoots = 0;
        int undershoots = 0;
        for (float flick : flicks) {
            if (flick > 1.1f) overshoots++;
            if (flick < 0.9f) undershoots++;
        }
        float flickWeight = Math.min(1.0f, flicks.size() / 20.0f);
        float trackWeight = Math.min(1.0f, trackTime / 20.0f);
        int classified = overshootMisses + undershootMisses;
        float missWeight = Math.min(1.0f, classified / 15.0f);
        float trackBias = trackTime > 0.0f ? Mth.clamp(trackLag / trackTime, -2.0f, 2.0f) : 0.0f;
        float missBias = classified > 0 ? (undershootMisses - overshootMisses) / (float) classified : 0.0f;
        double scale = Math.exp(flickWeight * 0.8 * -Math.log(median) + trackWeight * 0.08 * trackBias + missWeight * 0.06 * missBias);
        scale = Mth.clamp(scale, 0.7, 1.4);

        double base = current * 0.6 + 0.2;
        double suggested = Mth.clamp((base * Math.cbrt(scale) - 0.2) / 0.6, 0.0, 1.0);
        suggested = Math.round(suggested * 200.0) / 200.0;

        long seconds = (System.currentTimeMillis() - startedAt) / 1000L;
        ChatUtil.addChatMessage(Component.literal("Итоги за " + seconds / 60 + " мин " + seconds % 60 + " сек").withStyle(ChatFormatting.WHITE));
        ChatUtil.addChatMessage(line("Попадания: ", attacks == 0 ? "нет ударов" : hits + "/" + attacks + " (" + Math.round(hits * 100.0f / attacks) + "%)"));
        ChatUtil.addChatMessage(line("Наводки: ", flicks.size() + ", перелёт " + share(overshoots, flicks.size())
                + ", недолёт " + share(undershoots, flicks.size())));
        ChatUtil.addChatMessage(line("Промахи: ", "перелёт " + overshootMisses + ", недолёт " + undershootMisses));
        ChatUtil.addChatMessage(line("Ведение цели: ", trackTime < 1.0f ? "мало данных"
                : trackBias > 0.3f ? "отстаёшь от цели" : trackBias < -0.3f ? "обгоняешь цель" : "ровно"));

        if (Math.abs(suggested - current) < 0.0125) {
            ChatUtil.addChatMessage("Сенса " + percent(current) + "% тебе подходит, менять не нужно.");
            return;
        }
        pendingSensitivity = suggested;
        ChatUtil.addChatMessage(Component.literal("Советую сенсу ").withStyle(ChatFormatting.WHITE)
                .append(Component.literal(percent(suggested) + "%").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" (сейчас " + percent(current) + "%) ").withStyle(ChatFormatting.GRAY))
                .append(button("[Сменить]", ChatFormatting.GREEN, APPLY, "Поставить " + percent(suggested) + "%"))
                .append(Component.literal(" "))
                .append(button("[Оставить]", ChatFormatting.RED, KEEP, "Оставить " + percent(current) + "%")));
    }

    private static Component line(String name, String value) {
        return Component.literal(name).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(value).withStyle(ChatFormatting.WHITE));
    }

    private static Component button(String text, ChatFormatting color, Identifier action, String hover) {
        return Component.literal(text).withStyle(style -> style.withColor(color)
                .withClickEvent(new ClickEvent.Custom(action, Optional.empty()))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover))));
    }

    private static String share(int count, int total) {
        return total == 0 ? "0%" : Math.round(count * 100.0f / total) + "%";
    }

    private static int percent(double sensitivity) {
        return (int) Math.round(sensitivity * 200.0);
    }

    private static float yawTo(Vec3 eye, Vec3 point) {
        return (float) Math.toDegrees(Math.atan2(-(point.x - eye.x), point.z - eye.z));
    }

    private static float pitchTo(Vec3 eye, Vec3 point) {
        double horizontal = Math.hypot(point.x - eye.x, point.z - eye.z);
        return (float) -Math.toDegrees(Math.atan2(point.y - eye.y, horizontal));
    }
}
