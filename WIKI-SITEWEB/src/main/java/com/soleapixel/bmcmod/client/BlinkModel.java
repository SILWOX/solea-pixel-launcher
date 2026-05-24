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
import com.soleapixel.bmcmod.entity.Blink;

public class BlinkModel extends EntityModel<Blink> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "blink"), "main");

    /** Bâtons : orbite horizontale + ondulation verticale (même principe que {@link net.minecraft.client.model.BlazeModel}). */
    private static final float ROD_ORBIT_RADIUS = 10.5F;
    private static final float ROD_BASE_Y = -9.0F;
    /** Même gabarit que l’export Blockbench (tex 64×32, {@code texOffs(0,16)} pour 2×6×2) pour éviter les UV qui débordent. */
    private static final float ROD_HALF_WIDTH = 1.0F;
    private static final float ROD_HEIGHT = 6.0F;

    private final ModelPart blink;
    private final ModelPart head;
    private final ModelPart[] sticks;

    public BlinkModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.blink = root.getChild("blink");
        this.head = this.blink.getChild("head");
        this.sticks = new ModelPart[8];
        for (int i = 0; i < 8; i++) {
            this.sticks[i] = this.blink.getChild("stick" + (i + 1));
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition blinkRoot = partdefinition.addOrReplaceChild("blink", CubeListBuilder.create(), PartPose.offset(0.0F, 22.0F, 0.0F));

        blinkRoot.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -5.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -12.0F, 0.0F));

        for (int i = 1; i <= 8; i++) {
            CubeListBuilder rod = CubeListBuilder.create().texOffs(0, 16).addBox(
                    -ROD_HALF_WIDTH,
                    0.0F,
                    -ROD_HALF_WIDTH,
                    ROD_HALF_WIDTH * 2.0F,
                    ROD_HEIGHT,
                    ROD_HALF_WIDTH * 2.0F,
                    CubeDeformation.NONE);
            blinkRoot.addOrReplaceChild("stick" + i, rod, PartPose.ZERO);
        }

        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    @Override
    public void setupAnim(Blink entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float headY = netHeadYaw * Mth.DEG_TO_RAD;
        float headX = headPitch * Mth.DEG_TO_RAD;
        this.head.yRot = headY;
        this.head.xRot = headX;

        float bobT = ageInTicks * 0.05F;
        float bob = Mth.sin(bobT * 0.5F) * 0.08F;
        this.blink.y = bob;

        // Rotation plus lente + phases différentes par bâton et par entité (sans changer le rayon d’orbite).
        float entitySeed = Mth.frac(entity.getUUID().getLeastSignificantBits() * (float) (Math.PI * 2e-9));
        float spin = ageInTicks * Mth.PI * -0.032F;
        for (int i = 0; i < this.sticks.length; i++) {
            ModelPart stick = this.sticks[i];
            float slot = i * Mth.PI / 4.0F;
            float wobbleA = 0.22F * Mth.sin(ageInTicks * 0.038F + slot * 1.31F + entitySeed * 5.7F);
            float wobbleB = 0.11F * Mth.sin(ageInTicks * 0.071F + i * 1.93F + entitySeed * 3.1F);
            float wobbleC = 0.07F * Mth.cos(ageInTicks * 0.025F * (1.0F + 0.35F * Mth.sin(entitySeed + i)));
            float angle = spin + slot + wobbleA + wobbleB + wobbleC;
            float radiusMul = 0.93F + 0.07F * Mth.sin(ageInTicks * 0.045F + i * 0.88F + entitySeed * 4.2F);
            float r = ROD_ORBIT_RADIUS * radiusMul;
            stick.x = Mth.cos(angle) * r;
            stick.z = Mth.sin(angle) * r;
            float layerBob = Mth.cos(ageInTicks * 0.42F + i * 0.71F + entitySeed * 6.0F) * 1.85F
                    + 0.32F * Mth.sin(ageInTicks * 0.53F + i * 1.17F);
            float ringOffset = (i & 1) == 0 ? 0.0F : 0.85F;
            stick.y = ROD_BASE_Y + layerBob + ringOffset;
            stick.yRot = -angle + Mth.HALF_PI;
            // Moitié des bâtons pointent vers le bas (pivot en bas du cube, xRot = π retourne la tige).
            boolean pointDown = (i & 1) == 1;
            float downFlip = pointDown ? Mth.PI : 0.0F;
            stick.xRot = downFlip + 0.09F * Mth.sin(ageInTicks * 0.22F + i * 0.73F + entitySeed);
            stick.zRot = 0.06F * Mth.cos(ageInTicks * 0.19F + i * 0.61F - entitySeed);
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.blink.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
