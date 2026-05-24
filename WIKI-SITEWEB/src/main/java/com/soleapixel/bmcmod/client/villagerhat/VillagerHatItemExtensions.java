package com.soleapixel.bmcmod.client.villagerhat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.model.Model;

import com.soleapixel.bmcmod.BmcMod;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/**
 * Masque la passe vanilla du casque lorsque le mesh custom dessine le chapeau ({@link VillagerHatRenderLayer} joueurs,
 * {@link VillagerHatMobRenderLayer} autres entités). Undead Crown masque pour tout le monde pour éviter le rendu “plaques” vanilla.
 */
public final class VillagerHatItemExtensions implements IClientItemExtensions {
    @Override
    public Model getGenericArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
        if (slot != EquipmentSlot.HEAD) {
            return original;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!BmcMod.MODID.equals(id.getNamespace())) {
            return original;
        }
        for (VillagerHatModelKey k : VillagerHatModelKey.values()) {
            if (!k.assetId().equals(id.getPath())) {
                continue;
            }
            ModelPart baked = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.PLAYER_INNER_ARMOR);
            HumanoidModel<LivingEntity> invisible = new HumanoidModel<>(baked);
            invisible.setAllVisible(false);
            if (k == VillagerHatModelKey.UNDEAD_CROWN) {
                return invisible;
            }
            if (entity instanceof AbstractClientPlayer) {
                return invisible;
            }
            return original;
        }
        return original;
    }
}
