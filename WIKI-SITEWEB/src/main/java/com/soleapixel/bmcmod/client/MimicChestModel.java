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
import com.soleapixel.bmcmod.entity.MimicChest;

/**
 * Mesh Blockbench {@code bmcmod_asset/textures/1.2/mimic.java} (64×64) ;
 * marche / course : interpolation des keyframes de {@code mimic_walk.java} (cycle 1,2083 s).
 */
public class MimicChestModel extends EntityModel<MimicChest> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "mimic_chest"), "main");

    /** Pose repos du couvercle (Blockbench {@code PartPose… 2.9671F} rad). */
    private static final float LID_REST_X_ROT = 2.9671F;

    private static final float[] LID_TIMES_SEC = { 0.0F, 0.2083F, 0.4167F, 0.6667F, 0.9583F, 1.2083F };
    private static final float[] LID_DEG_X = { 0.0F, -55.0F, -15.0F, -20.0F, -112.17F, 2.5F };

    private static final float[] LEG_TIMES_SEC = { 0.0F, 0.25F, 0.5F, 0.75F, 1.0F, 1.2083F };
    private static final float[] LEFT_DEG_X = { 0.0F, 57.5F, -45.0F, 42.5F, -42.5F, 47.5F };
    private static final float[] RIGHT_DEG_X = { 0.0F, -45.0F, 42.5F, -42.5F, 45.0F, -40.0F };

    private static final float WALK_LOOP_SEC = 1.2083F;

    private final ModelPart mimicChest;
    private final ModelPart left;
    private final ModelPart right;
    private final ModelPart lid;

    public MimicChestModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.mimicChest = root.getChild("mimic_chest");
        this.left = root.getChild("left");
        this.right = root.getChild("right");
        this.lid = this.mimicChest.getChild("lid");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition mimicChest = partdefinition.addOrReplaceChild(
                "mimic_chest",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 18.0F, 0.0F));

        mimicChest.addOrReplaceChild(
                "base",
                CubeListBuilder.create()
                        .texOffs(0, 19)
                        .addBox(1.0F, 0.0F, 1.0F, 14.0F, 10.0F, 14.0F, CubeDeformation.NONE)
                        .texOffs(0, 46)
                        .addBox(1.0F, 10.0F, 1.0F, 14.0F, 2.0F, 14.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-8.0F, 0.0F, 8.0F, 3.1416F, 0.0F, 0.0F));

        PartDefinition lidDef = mimicChest.addOrReplaceChild(
                "lid",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, 0.0F, 0.0F, 14.0F, 5.0F, 14.0F, CubeDeformation.NONE)
                        .texOffs(0, 48)
                        .addBox(-7.0F, -2.0F, 0.0F, 14.0F, 2.0F, 14.0F, CubeDeformation.NONE)
                        // Bois intérieur : évite un seul quad rouge « plat » pour le haut / le fond de la gueule.
                        .texOffs(28, 19)
                        .addBox(-6.5F, 2.25F, 0.3F, 13.0F, 1.0F, 13.0F, CubeDeformation.NONE)
                        .texOffs(28, 19)
                        .addBox(-6.5F, 0.2F, 12.25F, 13.0F, 4.5F, 1.25F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -10.0F, 7.0F, LID_REST_X_ROT, 0.0F, 0.0F));

        lidDef.addOrReplaceChild(
                "knob",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(7.0F, -2.0F, 14.0F, 2.0F, 4.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-8.0F, 0.0F, 29.0F, 3.1416F, 0.0F, 0.0F));

        partdefinition.addOrReplaceChild(
                "left",
                CubeListBuilder.create()
                        .texOffs(42, 19)
                        .addBox(-2.0F, 0.0F, -3.0F, 5.0F, 6.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offset(3.0F, 18.0F, 0.0F));

        partdefinition.addOrReplaceChild(
                "right",
                CubeListBuilder.create()
                        .texOffs(42, 19)
                        .addBox(-2.0F, 0.0F, -3.0F, 5.0F, 6.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offset(-4.0F, 18.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(MimicChest entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float yHead = netHeadYaw * Mth.DEG_TO_RAD;
        this.mimicChest.yRot = yHead;
        this.left.yRot = yHead;
        this.right.yRot = yHead;

        double speedSqr = entity.getDeltaMovement().horizontalDistanceSqr();
        boolean run = entity.isSprinting() || speedSqr > 0.08;
        float speedMul = run ? 1.22F : 1.0F;

        // Même échelle de phase que les pattes vanilla (cos(limbSwing * 0.6662F)), bouclée sur la durée Blockbench.
        float moving = limbSwingAmount > 0.02F ? Mth.clamp(limbSwingAmount, 0.0F, 1.0F) : 0.0F;
        float walkPhase = limbSwing * 0.6662F * speedMul;
        float timeSecWalk = Mth.frac(walkPhase / Mth.TWO_PI) * WALK_LOOP_SEC;

        // Blockbench : rotations du couvercle sont additives sur la pose du modèle (x = 0 au début du clip).
        float lidAnimDeg = sampleDegrees(LID_TIMES_SEC, LID_DEG_X, timeSecWalk);
        float lidDeltaRad = lidAnimDeg * Mth.DEG_TO_RAD * moving;
        this.lid.xRot = LID_REST_X_ROT + lidDeltaRad;

        float leftDeg = sampleDegrees(LEG_TIMES_SEC, LEFT_DEG_X, timeSecWalk);
        float rightDeg = sampleDegrees(LEG_TIMES_SEC, RIGHT_DEG_X, timeSecWalk);
        this.left.xRot = leftDeg * Mth.DEG_TO_RAD * moving;
        this.right.xRot = rightDeg * Mth.DEG_TO_RAD * moving;
    }

    /** Interpolation linéaire entre keyframes (temps en secondes, boucle implicite via {@code timeSec} déjà modulo). */
    private static float sampleDegrees(float[] timesSec, float[] degX, float timeSec) {
        if (timesSec.length == 0 || degX.length != timesSec.length) {
            return 0.0F;
        }
        float t = timeSec;
        if (t <= timesSec[0]) {
            return degX[0];
        }
        for (int i = 0; i < timesSec.length - 1; i++) {
            if (t <= timesSec[i + 1]) {
                float t0 = timesSec[i];
                float t1 = timesSec[i + 1];
                float a = degX[i];
                float b = degX[i + 1];
                return Mth.lerp((t - t0) / (t1 - t0), a, b);
            }
        }
        return degX[degX.length - 1];
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.mimicChest.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.left.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.right.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
