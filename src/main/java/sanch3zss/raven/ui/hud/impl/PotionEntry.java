package sanch3zss.raven.ui.hud.impl;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.util.math.interpolation.Easings;

public final class PotionEntry {
    public Holder<MobEffect> effect;
    public int amplifier;
    public int duration;
    public int maxDuration;
    public int lastUpdateTick;
    public boolean infinite;
    public boolean hasBeenActive;
    public boolean expiryNotified;
    public boolean harmful;
    public final ToggleAnimator animator = new ToggleAnimator(220, Easings.EASE_IN_OUT_QUAD);
    public String translationKey = "";
    public String durationText = "";
    public int previousDuration = Integer.MAX_VALUE;

    public PotionEntry() {
    }

    public String getDisplayName() {
        String strTranslate = I18n.get(this.translationKey);
        return this.amplifier > 0 ? strTranslate + " " + (this.amplifier + 1) : strTranslate;
    }
}
