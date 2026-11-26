package sanch3zss.raven.feature.module.render.poptweaks;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

import sanch3zss.raven.client.Mc;
import sanch3zss.raven.client.service.scheduling.WeightedEngine;
import sanch3zss.raven.render.animation.ToggleAnimator;
import sanch3zss.raven.util.math.interpolation.Easings;

public class PopChamsModel {
    public final PlayerModel model = new PlayerModel(Mc.INSTANCE.createEntityRendererContext().bakeLayer(ModelLayers.PLAYER), false);

    public final Player player;
    public final ToggleAnimator animation;
    public final Identifier texture;
    public final int color;

    public PopChamsModel(Player playerEntity, Identifier identifier, int color) {
        this.model.getHead().offsetScale(new Vector3f(-0.2f, -0.2f, -0.2f));
        this.animation = new ToggleAnimator(3000, Easings.LINEAR);
        this.animation.force(true);
        this.texture = identifier;
        this.player = playerEntity;
        this.color = color;
    }

    public void animate(WeightedEngine weightedEngine) {
        this.animation.animate(weightedEngine);
    }

    public boolean tick() {
        this.animation.state(false);
        if (!this.animation.isZero()) {
            return false;
        }
        this.player.remove(Entity.RemovalReason.KILLED);
        this.player.onClientRemoval();
        return true;
    }

    public PlayerModel getModel() {
        return this.model;
    }

    public Player getPlayer() {
        return this.player;
    }

    public ToggleAnimator getAnimation() {
        return this.animation;
    }

    public Identifier getTexture() {
        return this.texture;
    }

    public int getColor() {
        return this.color;
    }
}
