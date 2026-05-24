package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.Dummy;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

public final class TargetDummyModel extends HumanoidModel<Dummy> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "target_dummy"),
            "main");

    private final ModelPart left;
    private final ModelPart waist;
    private final ModelPart base;

    public TargetDummyModel(ModelPart root) {
        super(root);
        this.left = root.getChild("left");
        this.waist = root.getChild("waist");
        this.base = root.getChild("base");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = meshdefinition.getRoot();

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 1.0F, 0.0F));

        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 26)
                        .addBox(-6.0F, 0.0F, -1.5F, 12.0F, 3.0F, 3.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 16).mirror()
                        .addBox(0.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE).mirror(false),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(32, 16)
                        .addBox(-2.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(40, 16).mirror()
                        .addBox(-0.95F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F, CubeDeformation.NONE).mirror(false),
                PartPose.offset(1.85F, 12.0F, 0.0F));

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(40, 16)
                        .addBox(-1.2F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(-1.85F, 12.0F, 0.0F));

        root.addOrReplaceChild("left",
                CubeListBuilder.create().texOffs(48, 0)
                        .addBox(-3.0F, 3.0F, -1.0F, 6.0F, 7.0F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        root.addOrReplaceChild("waist",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(-4.0F, 10.0F, -1.0F, 8.0F, 2.0F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        root.addOrReplaceChild("base",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-6.0F, 11.0F, -6.0F, 12.0F, 1.0F, 12.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 12.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(Dummy entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        float hit = entity.hitAnimationProgress(0.0F);
        if (hit > 0.0F) {
            float wobble = (float) Math.sin(ageInTicks * 1.8F) * 0.22F * hit;
            float tilt = 0.08F * hit;
            this.body.zRot = wobble * 0.4F;
            this.head.zRot = wobble * 0.55F;
            this.rightArm.xRot = -0.45F * hit;
            this.leftArm.xRot = -0.45F * hit;
            this.rightArm.zRot = -tilt + wobble;
            this.leftArm.zRot = tilt - wobble;
        } else {
            this.body.zRot = 0.0F;
            this.head.zRot = 0.0F;
            this.rightArm.xRot = 0.0F;
            this.leftArm.xRot = 0.0F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        super.renderToBuffer(poseStack, buffer, packedLight, packedOverlay, color);
        this.left.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.waist.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.base.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
