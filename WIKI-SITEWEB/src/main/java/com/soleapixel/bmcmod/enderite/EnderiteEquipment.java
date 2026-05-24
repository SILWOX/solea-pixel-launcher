package com.soleapixel.bmcmod.enderite;

import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.equipment.EnderiteToolTier;

public final class EnderiteEquipment {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, BmcMod.MODID);

    /** Au-dessus du Netherite : points d’armure, ténacité et résistance au recul. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ENDERITE_ARMOR_MATERIAL = ARMOR_MATERIALS.register("enderite",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 4,
                            ArmorItem.Type.LEGGINGS, 7,
                            ArmorItem.Type.CHESTPLATE, 9,
                            ArmorItem.Type.HELMET, 4),
                    18,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(EnderiteToolTier.ENDERITE_INGOTS),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "enderite/enderite"))),
                    4.0F,
                    0.15F));

    private EnderiteEquipment() {
    }
}
