package sanch3zss.raven.feature.module.render.removals;

import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum RemovedSoundCategory implements DisplayNamed {
    THROW_POTIONS(Lang.REMOVALS_THROW_POTIONS),
    DRAGON_DEATH(Lang.REMOVALS_SOUND_DRAGON_DEATH),
    WITHER_SPAWN(Lang.REMOVALS_SOUND_WITHER_SPAWN),
    WARDEN(Lang.REMOVALS_SOUND_WARDEN),
    ENDERMAN(Lang.REMOVALS_SOUND_ENDERMAN),
    LAVA_WATER(Lang.REMOVALS_SOUND_LAVA_WATER),
    LIGHTNING(Lang.REMOVALS_SOUND_LIGHTNING),
    TRIDENT(Lang.REMOVALS_SOUND_TRIDENT),
    EXPLOSIONS(Lang.REMOVALS_SOUND_EXPLOSIONS);

    final Translation displayName;

    RemovedSoundCategory(Translation translation) {
        this.displayName = translation;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public boolean matches(String sound) {
        if (sound == null) {
            return false;
        }
        return switch (this) {
            case THROW_POTIONS -> sound.equals("entity.experience_bottle.throw") || sound.equals("entity.splash_potion.break") || sound.equals("entity.splash_potion.throw") || sound.equals("entity.lingering_potion.throw");
            case DRAGON_DEATH -> sound.equals("entity.ender_dragon.death");
            case WITHER_SPAWN -> sound.equals("entity.wither.spawn");
            case WARDEN -> sound.startsWith("entity.warden") || sound.equals("block.sculk_shrieker.shriek");
            case ENDERMAN -> sound.startsWith("entity.enderman") || sound.equals("entity.endereye.launch");
            case LAVA_WATER -> sound.startsWith("block.lava") || sound.equals("entity.generic.splash") || sound.equals("entity.generic.swim") || sound.equals("entity.generic.extinguish_fire") || sound.startsWith("block.water");
            case LIGHTNING -> sound.equals("entity.lightning_bolt.thunder") || sound.equals("entity.lightning_bolt.impact");
            case TRIDENT -> sound.startsWith("item.trident") || sound.equals("entity.drowned.shoot");
            case EXPLOSIONS -> sound.equals("entity.generic.explode") || sound.equals("entity.dragon_fireball.explode") || sound.equals("entity.wind_charge.wind_burst") || sound.equals("entity.firework_rocket.blast") || sound.equals("entity.firework_rocket.large_blast") || sound.equals("entity.firework_rocket.twinkle") || sound.equals("entity.firework_rocket.large_blast_far") || sound.equals("entity.firework_rocket.twinkle_far");
        };
    }
}
