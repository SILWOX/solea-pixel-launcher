package com.soleapixel.bmcmod.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.HumanoidArm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.soleapixel.bmcmod.BmcMod;

public class SkeletonVillagerModel extends HumanoidModel<com.soleapixel.bmcmod.entity.SkeletonVillager> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "skeleton_villager"), "main");

    public SkeletonVillagerModel(net.minecraft.client.model.geom.ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, CubeDeformation.NONE)
                .texOffs(0, 24).mirror().addBox(-1.0F, -3.0F, -6.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE).mirror(false), PartPose.offset(0.0F, -0.95F, 0.0F));
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        // Synced with updated Blockbench body proportions while preserving Humanoid pivots/animations.
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(17, 21).addBox(-4.0F, 0.0F, -2.5F, 8.0F, 12.0F, 5.0F, CubeDeformation.NONE), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 4).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 4).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE).mirror(false), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE), PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0, 24).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F, CubeDeformation.NONE).mirror(false), PartPose.offset(2.0F, 12.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void prepareMobModel(com.soleapixel.bmcmod.entity.SkeletonVillager entity, float limbSwing, float limbSwingAmount, float partialTick) {
        super.prepareMobModel(entity, limbSwing, limbSwingAmount, partialTick);
        this.rightArmPose = ArmPose.EMPTY;
        this.leftArmPose = ArmPose.EMPTY;
        ItemStack main = entity.getMainHandItem();
        if (main.is(Items.CROSSBOW)) {
            if (entity.isUsingItem()) {
                if (entity.getUsedItemHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
                    this.rightArmPose = ArmPose.CROSSBOW_CHARGE;
                } else {
                    this.leftArmPose = ArmPose.CROSSBOW_CHARGE;
                }
            } else {
                if (entity.getMainArm() == HumanoidArm.RIGHT) {
                    this.rightArmPose = ArmPose.CROSSBOW_HOLD;
                } else {
                    this.leftArmPose = ArmPose.CROSSBOW_HOLD;
                }
            }
        } else if (main.is(Items.TRIDENT)) {
            if (entity.getMainArm() == HumanoidArm.RIGHT) {
                this.rightArmPose = ArmPose.THROW_SPEAR;
            } else {
                this.leftArmPose = ArmPose.THROW_SPEAR;
            }
        } else if (!main.isEmpty()) {
            if (entity.getMainArm() == HumanoidArm.RIGHT) {
                this.rightArmPose = ArmPose.ITEM;
            } else {
                this.leftArmPose = ArmPose.ITEM;
            }
        }
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        super.translateToHand(arm, poseStack);
        float side = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        // Recenter held items in thin custom arm bones.
        poseStack.translate(0.055F * side, -0.06F, -0.02F);
    }
}
