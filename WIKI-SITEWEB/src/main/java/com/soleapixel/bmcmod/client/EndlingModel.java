package com.soleapixel.bmcmod.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.entity.Endling;

/**
 * Modèle Blockbench exporté ({@code bmcmod_asset/textures/1.1/endlings.java}), adapté à 1.21.
 */
public class EndlingModel extends EntityModel<Endling> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "endling"), "main");

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public EndlingModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.head = root.getChild("head");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition head = partdefinition.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(26, 10).addBox(-1.0F, -2.0F, -3.0F, 2.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 17.0F, -1.0F));
        head.addOrReplaceChild("cube_r1",
                CubeListBuilder.create().texOffs(16, 0).addBox(-2.0F, -5.0F, -2.0F, 4.0F, 5.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, Mth.PI, 0.0F));

        partdefinition.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(24, 27).addBox(-1.5F, 0.0F, -1.4F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-1.0F, 21.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 27).addBox(-0.5F, 0.0F, -1.4F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(1.0F, 21.0F, 0.0F));
        partdefinition.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 9).addBox(-3.0F, -3.0F, -2.0F, 6.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 0).addBox(-1.5F, 0.0F, -1.7F, 3.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 20.0F, 0.0F));
        partdefinition.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(24, 19).addBox(0.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(3.0F, 17.0F, 0.0F));
        partdefinition.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(16, 19).addBox(-2.0F, -0.5F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-3.0F, 17.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(Endling entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        float swing = limbSwing * 0.6662F;
        float amount = limbSwingAmount;
        this.rightArm.xRot = Mth.cos(swing + Mth.PI) * 2.0F * amount * 0.5F;
        this.leftArm.xRot = Mth.cos(swing) * 2.0F * amount * 0.5F;
        this.rightLeg.xRot = Mth.cos(swing) * 1.4F * amount;
        this.leftLeg.xRot = Mth.cos(swing + Mth.PI) * 1.4F * amount;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
