
package com.holdmylua.source.model;

import com.holdmylua.source.model.interfaces.Poses;
import com.holdmylua.source.model.parents.AbstractPose;
import com.mojang.blaze3d.vertex.PoseStack;

public class PoseZ
extends AbstractPose
implements Poses {
    float amount;

    public PoseZ(float amount) {
        this.amount = amount;
    }

    @Override
    public void applyPose(PoseStack matrices) {
        matrices.translate(0.0f, 0.0f, this.amount);
    }
}

