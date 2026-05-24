package com.soleapixel.bmcmod.boreal;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.equipment.BorealToolTier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;

public final class BorealEquipment {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, BmcMod.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BOREAL_ARMOR_MATERIAL = ARMOR_MATERIALS.register("boreal",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 6,
                            ArmorItem.Type.LEGGINGS, 12,
                            ArmorItem.Type.CHESTPLATE, 14,
                            ArmorItem.Type.HELMET, 6),
                    22,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(BorealToolTier.BOREAL_INGOTS),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "boreal/boreal"))),
                    6.0F,
                    0.22F));

    private BorealEquipment() {
    }
}

