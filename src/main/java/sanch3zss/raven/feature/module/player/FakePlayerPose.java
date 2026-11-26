package sanch3zss.raven.feature.module.player;

import net.minecraft.world.entity.Pose;
import sanch3zss.raven.client.chat.*;
import sanch3zss.raven.client.i18n.Lang;

public enum FakePlayerPose implements DisplayNamed {
    NORMAL(Lang.FAKEPLAYER_POSE_NORMAL, Pose.STANDING),
    CROUCHING(Lang.FAKEPLAYER_POSE_CROUCHING, Pose.CROUCHING),
    SWIMMING(Lang.FAKEPLAYER_POSE_SWIMMING, Pose.SWIMMING);

    private final Translation displayName;
    private final Pose entityPose;

    FakePlayerPose(Translation displayName, Pose entityPose) {
        this.displayName = displayName;
        this.entityPose = entityPose;
    }

    @Override
    public Translation getDisplayName() {
        return this.displayName;
    }

    public Pose entityPose() {
        return this.entityPose;
    }
}
