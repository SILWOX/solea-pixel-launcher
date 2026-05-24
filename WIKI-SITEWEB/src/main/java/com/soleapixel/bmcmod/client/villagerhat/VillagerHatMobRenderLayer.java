package com.soleapixel.bmcmod.client.villagerhat;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.registry.ModItems;

/**
 * Rend la mesh Undead Crown sur les entités non-joueurs (armureaux, mobs humanoïdes), comme {@link VillagerHatRenderLayer}.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public final class VillagerHatMobRenderLayer<T extends LivingEntity> extends RenderLayer<T, EntityModel<T>> {

    private final Map<VillagerHatModelKey, ModelPart> hatRoots = new EnumMap<>(VillagerHatModelKey.class);

    public VillagerHatMobRenderLayer(LivingEntityRenderer<T, ? extends EntityModel<T>> renderer, EntityModelSet models) {
        super((RenderLayerParent<T, EntityModel<T>>) (Object) renderer);
        for (VillagerHatModelKey key : VillagerHatModelKey.values()) {
            ModelPart root = models.bakeLayer(key.layer());
            hatRoots.put(key, root.getChild("head"));
        }
    }

    static ModelPart resolveHead(EntityModel<?> model) {
        if (model instanceof HumanoidModel<?> hm) {
            return hm.head;
        }
        if (model instanceof HeadedModel headed) {
            return headed.getHead();
        }
        return null;
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T entity,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
            float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        ItemStack headStack = entity.getItemBySlot(EquipmentSlot.HEAD);
        if (headStack.isEmpty() || !headStack.is(ModItems.UNDEAD_CROWN.get())) {
            return;
        }
        VillagerHatModelKey key = VillagerHatModelKey.UNDEAD_CROWN;
        ModelPart hatHead = hatRoots.get(key);
        ModelPart entityHead = resolveHead(getParentModel());
        if (hatHead == null || entityHead == null) {
            return;
        }

        poseStack.pushPose();
        float hover = (float) Math.sin((ageInTicks + partialTick) * 0.18F) * 0.045F;
        poseStack.translate(0.0D, hover, 0.0D);
        hatHead.copyFrom(entityHead);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(key.entityTexture()));
        hatHead.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
    }
}
