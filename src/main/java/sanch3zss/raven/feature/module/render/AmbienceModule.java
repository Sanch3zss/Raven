package sanch3zss.raven.feature.module.render;

import java.util.List;
import java.util.Map;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;
import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.chat.Translation;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.network.PacketReceiveEvent;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.event.render.GammaEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.setting.BooleanSetting;
import sanch3zss.raven.ui.setting.ColorSetting;
import sanch3zss.raven.ui.setting.ExpandableSetting;
import sanch3zss.raven.ui.setting.NumberSetting;

@Aliases(aliases = {"Ambience", "Brightness", "Night Vision", "World Time"})
public class AmbienceModule extends Module {
    public final ExpandableSetting customTime;
    public final NumberSetting time;
    public final NumberSetting brightness;
    public final ExpandableSetting colorGrade;
    public final NumberSetting saturation;
    public final NumberSetting contrast;
    public final NumberSetting warmth;
    public final ExpandableSetting fog;
    public final NumberSetting fogDistance;
    public final ColorSetting fogColor;
    public final ExpandableSetting customSky;
    public final ColorSetting skyColor;
    private long previousTime;
    private long previousGameTime;
    private long serverClockGameTime;
    private ClockNetworkState serverClock;
    private ClientLevel timeWorld;
    private boolean timeOverrideApplied;

    public AmbienceModule() {
        super(ModuleTab.VISUALS, "Ambience");
        customTime = new ExpandableSetting(Lang.ambience("custom_time"));
        time = new NumberSetting(Lang.ambience("time"))
                .range(0.0f, 24000.0f)
                .currentValue(17000.0f)
                .step(500.0f)
                .unit(SettingUnit.TICKS);
        customTime.setSubSettings(List.of(time));
        brightness = new NumberSetting(Translation.clearText("Яркость"))
                .range(0.0f, 16.0f).currentValue(1.0f).step(0.5f).unit(SettingUnit.UNITS);
        colorGrade = new ExpandableSetting(Translation.clearText("Цветокоррекция"));
        saturation = new NumberSetting(Translation.clearText("Насыщенность"))
                .range(0.0f, 2.0f).currentValue(1.0f).step(0.05f).unit(SettingUnit.UNITS);
        contrast = new NumberSetting(Translation.clearText("Контраст"))
                .range(0.5f, 1.5f).currentValue(1.0f).step(0.05f).unit(SettingUnit.UNITS);
        warmth = new NumberSetting(Translation.clearText("Теплота"))
                .range(-1.0f, 1.0f).currentValue(0.0f).step(0.05f).unit(SettingUnit.UNITS);
        colorGrade.setSubSettings(List.of(saturation, contrast, warmth));
        fog = new ExpandableSetting(Translation.clearText("Туман"));
        fogDistance = new NumberSetting(Translation.clearText("Дальность"))
                .range(8.0f, 256.0f).currentValue(96.0f).step(4.0f).unit(SettingUnit.BLOCKS);
        fogColor = new ColorSetting(Translation.clearText("Цвет")).value(0xFFB59CFF);
        fogColor.setSecondColor(0xFF7FD4FF);
        fog.setSubSettings(List.of(fogDistance, fogColor, fogColor.themeSyncSetting()));
        customSky = new ExpandableSetting(Translation.clearText("Кастом небо"));
        skyColor = new ColorSetting(Translation.clearText("Цвет неба")).value(0xFF1B2A6B);
        skyColor.setSecondColor(0xFF7FB8FF);
        customSky.setSubSettings(List.of(skyColor, skyColor.themeSyncSetting()));
        addSettings(customTime, colorGrade, fog, customSky, brightness);
        register(GammaEvent.class, event -> {
            if (isState()) {
                event.setGamma(brightness.currentValue());
                event.cancel();
            }
        });

        register(PacketReceiveEvent.class, event -> {
            if (isState() && customTime.isValue() && Mc.INSTANCE.isWorldLoaded()
                    && event.getPacket() instanceof ClientboundSetTimePacket packet) {
                prepareTimeOverride();
                ClockNetworkState update = packet.clockUpdates().get(overworldClock());
                if (update != null) {
                    this.serverClock = update;
                    this.serverClockGameTime = packet.gameTime();
                }
                var world = Mc.INSTANCE.getWorld();
                world.setTimeFromServer(packet.gameTime());
                world.clockManager().handleUpdates(packet.gameTime(), packet.clockUpdates());
                setWorldTime((long) time.currentValue());
                event.cancel();
            }
        });
        register(PlayerTickEvent.class, event -> {
            if (!event.isPre() || !Mc.INSTANCE.isWorldLoaded()) return;
            if (!isState()) {
                return;
            }
            if (customTime.isValue()) {
                prepareTimeOverride();
                setWorldTime((long) time.currentValue());
            } else if (this.timeOverrideApplied) {
                restoreWorldTime();
            }
        });
    }

    @Override
    public void activate() {
        this.timeOverrideApplied = false;
        this.timeWorld = null;
        this.serverClock = null;
        super.activate();
    }

    @Override
    public void deactivate() {
        if (this.timeOverrideApplied) {
            restoreWorldTime();
        }
        super.deactivate();
    }

    private Holder<WorldClock> overworldClock() {
        return Mc.INSTANCE.getWorld().registryAccess().lookupOrThrow(Registries.WORLD_CLOCK)
                .getOrThrow(WorldClocks.OVERWORLD);
    }

    private void prepareTimeOverride() {
        ClientLevel world = Mc.INSTANCE.getWorld();
        if (this.timeOverrideApplied && this.timeWorld == world) {
            return;
        }
        this.timeWorld = world;
        this.previousTime = world.clockManager().getTotalTicks(overworldClock());
        this.previousGameTime = world.getGameTime();
        this.serverClock = null;
        this.timeOverrideApplied = true;
    }

    private void restoreWorldTime() {
        if (Mc.INSTANCE.isWorldLoaded() && Mc.INSTANCE.getWorld() == this.timeWorld) {
            long gameTime = this.timeWorld.getGameTime();
            float rate = this.serverClock == null ? 1.0f : this.serverClock.rate();
            long baseTime = this.serverClock == null ? this.previousTime : this.serverClock.totalTicks();
            long baseGameTime = this.serverClock == null ? this.previousGameTime : this.serverClockGameTime;
            setWorldTime(baseTime + Math.round((gameTime - baseGameTime) * rate), rate);
        }
        this.timeOverrideApplied = false;
        this.timeWorld = null;
        this.serverClock = null;
    }

    private void setWorldTime(long ticks) {
        setWorldTime(ticks, 0.0f);
    }

    private void setWorldTime(long ticks, float rate) {
        var world = Mc.INSTANCE.getWorld();
        world.clockManager().handleUpdates(world.getGameTime(),
                Map.of(overworldClock(), new ClockNetworkState(ticks, 0.0f, rate)));
        world.environmentAttributes().invalidateTickCache();
    }

}
