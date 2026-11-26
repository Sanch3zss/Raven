package sanch3zss.raven.feature.module.misc;

import java.util.*;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import sanch3zss.raven.client.*;
import sanch3zss.raven.client.i18n.Lang;
import sanch3zss.raven.event.network.PacketReceiveEvent;
import sanch3zss.raven.event.player.PlayerTickEvent;
import sanch3zss.raven.feature.annotation.Aliases;
import sanch3zss.raven.feature.module.Module;
import sanch3zss.raven.feature.setting.SettingUnit;
import sanch3zss.raven.feature.server.ServerUtil;
import sanch3zss.raven.ui.menu.layout.ModuleTab;
import sanch3zss.raven.ui.notification.NotificationType;
import sanch3zss.raven.ui.setting.*;

@Aliases(aliases = {"Clan Invest", "Funtime", "Auto Invest"})
public class ClanInvestModule extends Module {
    public final TextFieldSetting thresholdSetting;
    public final NumberSetting percentageSetting;
    public boolean investedThisCycle;
    public boolean awaitingConfirmation;
    public int pendingAmount;
    public long confirmationDeadline;
    static final long confirmationTimeoutMs = 4000;

    public ClanInvestModule() {
        super(ModuleTab.MISC, "ClanInvest");
        this.thresholdSetting = new TextFieldSetting(Lang.CLAN_INVEST_CURRENCY_THRESHOLD, Lang.CLAN_INVEST_CURRENCY_THRESHOLD_DESC)
                .setOnlyDigits(true)
                .setPlaceholder(Lang.CLAN_INVEST_CURRENCY_THRESHOLD_PLACEHOLDER)
                .setText("1000000");
        this.percentageSetting = new NumberSetting(Lang.CLAN_INVEST_INVEST_PERCENTAGE)
                .currentValue(30.0f)
                .range(1.0f, 100.0f)
                .step(1.0f)
                .unit(SettingUnit.PERCENTS);
        this.investedThisCycle = false;
        this.awaitingConfirmation = false;
        this.pendingAmount = 0;
        this.confirmationDeadline = 0L;
        addSettings(this.thresholdSetting, this.percentageSetting);

        register(PacketReceiveEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (isState() && mc.isWorldLoaded()) {
                if (event.getPacket() instanceof ClientboundSystemChatPacket packet) {
                    String lowerCase = packet.content().getString().toLowerCase(Locale.ROOT);
                    if (lowerCase.contains("/clan create - создать клан ($10000)")) {
                        Raven.INSTANCE.notificationRepository().post(NotificationType.ERROR, Component.literal("Вы не состоите в клане. Инвестиции невозможны."), 6L, TimeUnit.SECONDS);
                        applyState();
                        return;
                    }
                    if (lowerCase.contains("вы не можете пополнить баланс клана")) {
                        Raven.INSTANCE.notificationRepository().post(NotificationType.INFO, Component.literal("Включите модуль, когда вы сможете пополнить баланс клана."), 6L, TimeUnit.SECONDS);
                        applyState();
                        return;
                    }
                    if (this.awaitingConfirmation && lowerCase.contains("пополнил баланс казны")) {
                        Raven.INSTANCE.notificationRepository().post(NotificationType.SUCCESS, Component.literal("Инвестировано " + this.pendingAmount + "$ в клан."), 6L, TimeUnit.SECONDS);
                        this.awaitingConfirmation = false;
                        this.pendingAmount = 0;
                        this.confirmationDeadline = 0L;
                        this.investedThisCycle = true;
                    }
                }
            }
        });

        register(PlayerTickEvent.class, event -> {
            Mc mc = Mc.INSTANCE;
            if (isState() && mc.isWorldLoaded() && event.isPre()) {
                if (!ServerUtil.isConnectedToServer("funtime")) {
                    Raven.INSTANCE.notificationRepository().post(NotificationType.ERROR, Component.literal("Данный модуль работает только на Funtime!"), 3L, TimeUnit.SECONDS);
                    switchState();
                    return;
                }
                if (this.awaitingConfirmation && System.currentTimeMillis() > this.confirmationDeadline) {
                    this.awaitingConfirmation = false;
                    this.pendingAmount = 0;
                    this.confirmationDeadline = 0L;
                    Raven.INSTANCE.notificationRepository().post(NotificationType.INFO, Component.literal("Не получено подтверждение инвестиции (таймаут)."), 4L, TimeUnit.SECONDS);
                }
                Optional<Integer> currentBalance = getCurrentBalance();
                if (currentBalance.isEmpty()) {
                    return;
                }
                int balance = currentBalance.get();
                try {
                    int threshold = Integer.parseInt(this.thresholdSetting.getText());
                    if (threshold <= 0) {
                        Raven.INSTANCE.notificationRepository().post(NotificationType.ERROR, Component.literal("Порог должен быть больше нуля."), 3L, TimeUnit.SECONDS);
                        setState(false);
                        return;
                    }
                    if (balance < threshold) {
                        this.investedThisCycle = false;
                        return;
                    }
                    if (this.investedThisCycle || this.awaitingConfirmation) {
                        return;
                    }
                    int investAmount = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, (((long) balance) * ((long) ((int) this.percentageSetting.currentValue()))) / 100));
                    mc.getPlayer().connection.sendCommand("clan invest " + investAmount);
                    this.awaitingConfirmation = true;
                    this.pendingAmount = investAmount;
                    this.confirmationDeadline = System.currentTimeMillis() + confirmationTimeoutMs;
                } catch (NumberFormatException e) {
                    Raven.INSTANCE.notificationRepository().post(NotificationType.ERROR, Component.literal("Неверный формат порога валюты!"), 3L, TimeUnit.SECONDS);
                    setState(false);
                }
            }
        });
    }

    public void applyState() {
        this.awaitingConfirmation = false;
        this.pendingAmount = 0;
        this.confirmationDeadline = 0L;
        this.investedThisCycle = false;
    }

    @Override
    public void deactivate() {
        this.investedThisCycle = false;
        this.awaitingConfirmation = false;
        this.pendingAmount = 0;
        this.confirmationDeadline = 0L;
        super.deactivate();
    }

    public Optional<Integer> getCurrentBalance() {
        LocalPlayer player = Mc.INSTANCE.getPlayer();
        if (player == null) {
            return Optional.empty();
        }
        Scoreboard scoreboard = player.level().getScoreboard();
        Objective objectiveForSlot = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objectiveForSlot == null) {
            return Optional.empty();
        }
        return scoreboard.listPlayerScores(objectiveForSlot).stream()
                .filter(entry -> !entry.isHidden())
                .map(entry -> PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()).getSiblings().stream()
                        .map(Component::getString)
                        .filter(str -> str.toLowerCase(Locale.ROOT).replace('o', 'о').replace('e', 'е').contains("монет")
                                || str.toLowerCase(Locale.ROOT).replace('a', 'а').replace('c', 'с').contains("баланс"))
                        .map(this::parseDigits)
                        .flatMap(Optional::stream)
                        .findFirst().orElse(null))
                .filter(Objects::nonNull)
                .findFirst();
    }

    public Optional<Integer> parseDigits(String str) {
        StringBuilder sb = new StringBuilder();
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                sb.append(c);
            }
        }
        if (sb.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(sb.toString()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
