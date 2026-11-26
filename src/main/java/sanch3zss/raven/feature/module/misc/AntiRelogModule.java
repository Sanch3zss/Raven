package sanch3zss.raven.feature.module.misc;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.event.client.ClientTickEvent;
import sanch3zss.raven.event.network.WorldLoadEvent;
import sanch3zss.raven.event.player.PlayerDamageEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.ui.menu.layout.ModuleTab;

@Aliases(aliases = {"Anti Relog", "PvP Timer", "Combat Timer"})
public class AntiRelogModule extends Module {
    private static final long DISPLAY_NANOS = 30_000_000_000L;
    private static final long COMBAT_NANOS = 31_000_000_000L;
    private static final UUID BAR_ID = UUID.fromString("8f364af3-42b5-4f25-8d5f-94539873bb11");
    private long combatStartedAt;
    private boolean active;

    public AntiRelogModule() {
        super(ModuleTab.ALPHA_PLAYER, "AntiRelog");
        register(PlayerDamageEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (isState() && mc.isWorldLoaded()
                    && (event.target() == mc.getPlayer() && event.attacker() != mc.getPlayer()
                    || event.attacker() == mc.getPlayer() && event.target() != mc.getPlayer())) {
                this.combatStartedAt = System.nanoTime();
                this.active = true;
                updateBar(this.combatStartedAt);
            }
        });
        register(ClientTickEvent.class, event -> {
            if (!this.active) {
                return;
            }
            if (!isState() || !Mc.INSTANCE.isWorldLoaded()) {
                clearCombat();
            } else {
                updateBar(System.nanoTime());
            }
        });
        register(WorldLoadEvent.class, event -> {
            if (event.world() == null) {
                clearCombat();
            }
        });
    }

    public boolean isInCombat() {
        return isState() && this.active && System.nanoTime() - this.combatStartedAt < COMBAT_NANOS;
    }

    private void updateBar(long now) {
        long elapsed = now - this.combatStartedAt;
        if (elapsed >= COMBAT_NANOS) {
            clearCombat();
            return;
        }

        long displayRemaining = Math.max(0L, DISPLAY_NANOS - elapsed);
        int seconds = (int) ((displayRemaining + 999_999_999L) / 1_000_000_000L);
        Component name = Component.literal("Pvp режим - " + seconds + " секунд").withStyle(ChatFormatting.RED);
        float progress = Math.max(0.0f, 1.0f - (float) elapsed / DISPLAY_NANOS);
        BossHealthOverlay overlay = Mc.INSTANCE.getInGameHud().getBossOverlay();
        LerpingBossEvent bar = overlay.events.get(BAR_ID);
        if (bar == null) {
            overlay.events.put(BAR_ID, new LerpingBossEvent(BAR_ID, name, progress,
                    BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS,
                    false, false, false));
        } else {
            bar.setName(name);
            bar.setProgress(progress);
        }
    }

    private void clearCombat() {
        this.active = false;
        if (Mc.INSTANCE.getMinecraft() != null) {
            Mc.INSTANCE.getInGameHud().getBossOverlay().events.remove(BAR_ID);
        }
    }

    @Override
    public void deactivate() {
        clearCombat();
        super.deactivate();
    }
}
