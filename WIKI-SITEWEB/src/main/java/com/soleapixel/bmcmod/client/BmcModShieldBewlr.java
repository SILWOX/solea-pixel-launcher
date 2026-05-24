package com.soleapixel.bmcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.item.BmcShieldItem;

/**
 * Même forme 3D que le bouclier vanilla, texture personnalisée 64x64.
 * Les bannières sur bouclier&nbsp;: affichage simplifié (la plaque utilise un seul calque) ;
 * l’inventaire reste cohérent, les détails bannière peuvent différer légèrement.
 */
public final class BmcModShieldBewlr extends BlockEntityWithoutLevelRenderer {
    private final ShieldModel model;

    public BmcModShieldBewlr() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext context,
            PoseStack pose,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (!(stack.getItem() instanceof BmcShieldItem s)) {
            return;
        }
        Material mat = new Material(
                Sheets.SHIELD_SHEET,
                BmcMod.loc("entity/shield/" + (switch (s.kind) {
                    case DIAMOND -> "diamond_shield";
                    case NETHERITE -> "netherite_shield";
                    case ENDERITE -> "enderite_shield";
                })));
        pose.pushPose();
        pose.scale(1.0F, -1.0F, -1.0F);
        this.model.renderToBuffer(
                pose,
                mat.buffer(buffer, RenderType::entityCutout, stack.hasFoil()),
                packedLight,
                packedOverlay,
                0xFFFFFFFF);
        pose.popPose();
    }
}
