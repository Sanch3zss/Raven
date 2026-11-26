package sanch3zss.raven.feature.module.render.pets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;

public final class PetFlyModel {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart abdomen;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart[] legs = new ModelPart[6];

    public PetFlyModel() {
        root = createLayer().bakeRoot();
        ModelPart body = root.getChild("body");
        head = body.getChild("head");
        abdomen = body.getChild("abdomen");
        leftWing = body.getChild("wing_left");
        rightWing = body.getChild("wing_right");
        for (int i = 0; i < legs.length; i++) {
            legs[i] = body.getChild("leg" + i);
        }
    }

    public void render(PoseStack matrices, VertexConsumer vertices, int light, float age, float movement,
                       boolean landed, boolean grooming) {
        for (ModelPart part : root.getAllParts()) {
            part.resetPose();
        }
        head.yRot = (float) Math.sin(age * 0.07f) * 0.15f;
        abdomen.xRot += 0.08f + (float) Math.sin(age * 0.11f) * 0.03f;
        if (landed) {
            leftWing.yRot = 0.12f;
            rightWing.yRot = -0.12f;
            leftWing.zRot = -0.04f;
            rightWing.zRot = 0.04f;
            leftWing.xRot = -0.05f;
            rightWing.xRot = -0.05f;
            float step = movement > 0.01f ? age * 1.4f : 0.0f;
            for (int i = 0; i < legs.length; i++) {
                ModelPart leg = legs[i];
                boolean front = i < 2;
                leg.zRot = i % 2 == 0 ? -0.55f : 0.55f;
                leg.xRot = (front ? -0.35f : i < 4 ? 0.0f : 0.35f)
                        + (float) Math.sin(step + i * 1.7f) * 0.25f * Math.min(1.0f, movement * 6.0f);
                if (front && grooming) {
                    float rub = (float) Math.sin(age * 1.6f + i * Math.PI);
                    leg.xRot = -1.1f + rub * 0.25f;
                    leg.zRot = (i % 2 == 0 ? -0.15f : 0.15f) + rub * 0.12f;
                }
            }
            if (grooming) head.xRot = 0.25f + (float) Math.sin(age * 1.6f) * 0.05f;
        } else {
            float phase = (float) Math.sin(age * 9.5f);
            leftWing.yRot = 1.05f;
            rightWing.yRot = -1.05f;
            leftWing.zRot = -(0.2f + phase * 0.45f);
            rightWing.zRot = 0.2f + phase * 0.45f;
            for (int i = 0; i < legs.length; i++) {
                ModelPart leg = legs[i];
                leg.zRot = i % 2 == 0 ? -0.25f : 0.25f;
                leg.xRot = 0.9f + (float) Math.sin(age * 0.4f + i * 1.3f) * movement * 0.08f;
            }
        }
        root.render(matrices, vertices, light, OverlayTexture.NO_OVERLAY);
    }

    private static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition body = mesh.getRoot().addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0f, -3.0f, -3.0f, 6.0f, 5.0f, 6.0f),
                PartPose.offset(0.0f, 18.0f, 0.0f));
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 11).addBox(-2.5f, -2.5f, -4.0f, 5.0f, 5.0f, 4.0f)
                        .texOffs(18, 11).addBox(1.5f, -3.0f, -4.5f, 2.0f, 4.0f, 4.0f)
                        .texOffs(30, 11).addBox(-3.5f, -3.0f, -4.5f, 2.0f, 4.0f, 4.0f)
                        .texOffs(50, 11).addBox(0.3f, -2.0f, -5.0f, 1.0f, 2.0f, 1.0f)
                        .texOffs(50, 11).addBox(-1.3f, -2.0f, -5.0f, 1.0f, 2.0f, 1.0f),
                PartPose.offset(0.0f, -0.5f, -3.0f));
        head.addOrReplaceChild("proboscis",
                CubeListBuilder.create().texOffs(58, 11).addBox(-0.5f, 0.0f, -1.0f, 1.0f, 2.0f, 1.0f),
                PartPose.offsetAndRotation(0.0f, 2.5f, -2.5f, 0.4f, 0.0f, 0.0f));
        body.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(24, 0).addBox(-2.5f, -2.0f, 0.0f, 5.0f, 4.0f, 7.0f)
                        .texOffs(42, 11).addBox(-1.5f, -1.0f, 7.0f, 3.0f, 2.0f, 1.0f),
                PartPose.offsetAndRotation(0.0f, -0.5f, 3.0f, -0.12f, 0.0f, 0.0f));
        body.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(0, 20).addBox(0.0f, 0.0f, -1.0f, 5.0f, 0.0f, 12.0f),
                PartPose.offset(1.5f, -3.1f, -1.0f));
        body.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(0, 34).addBox(-5.0f, 0.0f, -1.0f, 5.0f, 0.0f, 12.0f),
                PartPose.offset(-1.5f, -3.1f, -1.0f));
        for (int i = 0; i < 6; i++) {
            body.addOrReplaceChild("leg" + i,
                    CubeListBuilder.create().texOffs(54, 11).addBox(-0.5f, 0.0f, -0.5f, 1.0f, 6.0f, 1.0f),
                    PartPose.offset(i % 2 == 0 ? 2.0f : -2.0f, 1.5f, -2.0f + i / 2 * 2.0f));
        }
        return LayerDefinition.create(mesh, 64, 64);
    }
}
