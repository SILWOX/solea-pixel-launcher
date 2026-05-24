package com.soleapixel.bmcmod.client.villagerhat;

import java.util.EnumMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.client.MorphVisualClient;
import com.soleapixel.bmcmod.client.WitchMetamorphClient;

public final class VillagerHatRenderLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private final Map<VillagerHatModelKey, ModelPart> hatRoots = new EnumMap<>(VillagerHatModelKey.class);

    public VillagerHatRenderLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent, EntityModelSet models) {
        super(parent);
        for (VillagerHatModelKey key : VillagerHatModelKey.values()) {
            ModelPart root = models.bakeLayer(key.layer());
            hatRoots.put(key, root.getChild("head"));
        }
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (MorphVisualClient.isMorphed(player.getUUID()) || WitchMetamorphClient.isDisguised(player.getUUID())) {
            return;
        }
        if (player.isInvisible()) {
            return;
        }
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) {
            return;
        }
        ResourceLocation itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(head.getItem());
        if (!BmcMod.MODID.equals(itemId.getNamespace())) {
            return;
        }
        VillagerHatModelKey key = byAssetId(itemId.getPath());
        if (key == null) {
            return;
        }
        ModelPart hatHead = hatRoots.get(key);
        if (hatHead == null) {
            return;
        }
        poseStack.pushPose();
        if (key == VillagerHatModelKey.UNDEAD_CROWN) {
            // Gentle hover animation for the Undead Crown.
            float hover = (float) Math.sin((ageInTicks + partialTick) * 0.18F) * 0.045F;
            poseStack.translate(0.0D, hover, 0.0D);
        }
        hatHead.copyFrom(getParentModel().head);
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(key.entityTexture()));
        hatHead.render(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
    }

    private static VillagerHatModelKey byAssetId(String path) {
        for (VillagerHatModelKey k : VillagerHatModelKey.values()) {
            if (k.assetId().equals(path)) {
                return k;
            }
        }
        return null;
    }
}
