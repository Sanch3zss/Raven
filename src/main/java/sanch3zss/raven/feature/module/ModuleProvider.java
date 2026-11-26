package sanch3zss.raven.feature.module;


import sanch3zss.raven.feature.module.misc.minehelper.MineHelperModule;
import sanch3zss.raven.feature.module.render.armtweaks.ArmTweaksModule;
import sanch3zss.raven.feature.module.render.chinahat.ChinaHatModule;
import sanch3zss.raven.feature.module.render.enchantcolor.CustomEnchantColorModule;
import sanch3zss.raven.feature.module.render.hitcolor.HitColorModule;
import sanch3zss.raven.feature.module.render.jumpcircle.JumpCircleModule;
import sanch3zss.raven.feature.module.render.leavetracker.LeaveTrackerModule;
import sanch3zss.raven.feature.module.render.party.PartyModule;
import sanch3zss.raven.feature.module.render.pets.PetsModule;
import sanch3zss.raven.feature.module.render.poptweaks.PopTweaksModule;
import sanch3zss.raven.feature.module.render.projectile.ProjectilePredictionModule;
import sanch3zss.raven.feature.module.render.removals.RemovalsModule;
import sanch3zss.raven.feature.module.render.targetesp.TargetESPModule;
import java.util.*;
import java.util.function.Supplier;

import sanch3zss.raven.feature.module.misc.*;
import sanch3zss.raven.feature.module.movement.*;
import sanch3zss.raven.feature.module.player.*;
import sanch3zss.raven.feature.module.render.*;

public class ModuleProvider {
    public List<Supplier<Module>> getAll() {
        ArrayList<Supplier<Module>> arrayList = new ArrayList<>();
        arrayList.addAll(movementModules());
        arrayList.addAll(renderModules());
        arrayList.addAll(interfaceModules());
        arrayList.addAll(playerModules());
        arrayList.addAll(miscModules());
        return arrayList;
    }

    public List<Supplier<Module>> movementModules() {
        return List.of(
            withCategory(SprintModule::new, ModuleCategory.AUTOMATION),
            withCategory(AutoJumpModule::new, ModuleCategory.AUTOMATION)
        );
    }

    public List<Supplier<Module>> renderModules() {
        return List.of(
            withCategory(ArmTweaksModule::new, ModuleCategory.VISUAL_PLAYERS),
            withCategory(AspectRatioModule::new, ModuleCategory.VISUAL_SCREEN),
            withCategory(ChinaHatModule::new, ModuleCategory.VISUAL_PLAYERS),
            withCategory(CustomEnchantColorModule::new, ModuleCategory.VISUAL_EFFECTS),
            withCategory(CustomHitBoxModule::new, ModuleCategory.VISUAL_WORLD),
            withCategory(TwoDItemsModule::new, ModuleCategory.VISUAL_SCREEN),
            withCategory(TrailsModule::new, ModuleCategory.VISUAL_WORLD),
            withCategory(ClientUsersModule::new, ModuleCategory.VISUAL_PLAYERS),
            withCategory(CustomTagsModule::new, ModuleCategory.VISUAL_PLAYERS),
            withCategory(CrosshairModule::new, ModuleCategory.VISUAL_SCREEN),
            withCategory(CustomFontModule::new, ModuleCategory.VISUAL_SCREEN),
            withCategory(AmbienceModule::new, ModuleCategory.VISUAL_WORLD),
            withCategory(MotionBlurModule::new, ModuleCategory.VISUAL_SCREEN),
            withCategory(JumpCircleModule::new, ModuleCategory.VISUAL_WORLD),
            withCategory(KillEffectsModule::new, ModuleCategory.VISUAL_EFFECTS),
            withCategory(LeaveTrackerModule::new, ModuleCategory.VISUAL_PLAYERS),
            withCategory(HitParticlesModule::new, ModuleCategory.VISUAL_EFFECTS),
            withCategory(HitColorModule::new, ModuleCategory.VISUAL_EFFECTS),
            withCategory(WorldParticlesModule::new, ModuleCategory.VISUAL_WORLD),
            withCategory(PopTweaksModule::new, ModuleCategory.VISUAL_EFFECTS),
            withCategory(PartyModule::new, ModuleCategory.VISUAL_PLAYERS),
            withCategory(PetsModule::new, ModuleCategory.VISUAL_WORLD),
            withCategory(ProjectilePredictionModule::new, ModuleCategory.VISUAL_WORLD),
            withCategory(RemovalsModule::new, ModuleCategory.VISUAL_SCREEN),
            withCategory(SeeInvisiblesModule::new, ModuleCategory.VISUAL_PLAYERS),
            withCategory(ShulkerBoxTooltipModule::new, ModuleCategory.VISUAL_SCREEN),
            withCategory(TargetESPModule::new, ModuleCategory.VISUAL_EFFECTS),
            WidgetsModule::new
        );
    }

    public List<Supplier<Module>> interfaceModules() {
        return List.of(
            withCategory(ClickGuiModule::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.Watermark::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.PotionList::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.TargetHud::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.MediaPlayer::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.Gif::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.Hotkeys::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.Coords::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.InventoryHud::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.EquipmentHud::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.Cooldowns::new, ModuleCategory.INTERFACE),
            withCategory(HudElementModules.Notifications::new, ModuleCategory.INTERFACE)
        );
    }

    public List<Supplier<Module>> playerModules() {
        return List.of(
            withCategory(AutoLeaveModule::new, ModuleCategory.AUTOMATION),
            withCategory(AutoRespawnModule::new, ModuleCategory.AUTOMATION),
            withCategory(AutoSensitivityModule::new, ModuleCategory.UTILITIES),
            withCategory(DeathCoordinatesModule::new, ModuleCategory.CONVENIENCE),
            withCategory(FakePlayerModule::new, ModuleCategory.UTILITIES),
            withCategory(FreeLookModule::new, ModuleCategory.CONVENIENCE),
            withCategory(ItemScrollerModule::new, ModuleCategory.CONVENIENCE)
        );
    }

    public List<Supplier<Module>> miscModules() {
        return List.of(
            withCategory(HitSoundsModule::new, ModuleCategory.CONVENIENCE),
            withCategory(PvPSafeModule::new, ModuleCategory.DEFENSE),
            withCategory(AntiRelogModule::new, ModuleCategory.ALPHA_PLAYER),
            withCategory(AutoSwapModule::new, ModuleCategory.AUTOMATION),
            withCategory(ElytraSwapModule::new, ModuleCategory.AUTOMATION),
            withCategory(LockSlotModule::new, ModuleCategory.DEFENSE),
            withCategory(AutoAuthModule::new, ModuleCategory.AUTOMATION),
            withCategory(DiscordRpcModule::new, ModuleCategory.UTILITIES),
            withCategory(ExpTweaksModule::new, ModuleCategory.AUTOMATION),
            withCategory(BetterChatModule::new, ModuleCategory.CONVENIENCE),
            withCategory(ClanInvestModule::new, ModuleCategory.AUTOMATION),
            withCategory(ClickFriendModule::new, ModuleCategory.CONVENIENCE),
            withCategory(FTHelperModule::new, ModuleCategory.UTILITIES),
            withCategory(HWHelperModule::new, ModuleCategory.UTILITIES),
            withCategory(MineHelperModule::new, ModuleCategory.UTILITIES),
            withCategory(NameProtectModule::new, ModuleCategory.UTILITIES),
            withCategory(SoundsModule::new, ModuleCategory.CONVENIENCE),
            withCategory(TPAcceptModule::new, ModuleCategory.AUTOMATION),
            withCategory(TapeMouseModule::new, ModuleCategory.UTILITIES),
            withCategory(NbtGetterModule::new, ModuleCategory.UTILITIES)
        );
    }

    public Supplier<Module> withCategory(Supplier<Module> supplier, ModuleCategory category) {
        return () -> {
            Module module = supplier.get();
            module.setCategory(category);
            return module;
        };
    }
}
