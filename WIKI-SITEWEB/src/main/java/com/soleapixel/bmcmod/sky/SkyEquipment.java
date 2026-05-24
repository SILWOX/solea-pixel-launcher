package com.soleapixel.bmcmod.sky;

import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.soleapixel.bmcmod.BmcMod;

/** Matériau d’armure des bottes célestes (texture + valeurs de défense type émeraude). */
public final class SkyEquipment {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, BmcMod.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SKY_BOOTS_MATERIAL = ARMOR_MATERIALS.register(
            "sky_boots",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 3,
                            ArmorItem.Type.LEGGINGS, 0,
                            ArmorItem.Type.CHESTPLATE, 0,
                            ArmorItem.Type.HELMET, 0),
                    12,
                    SoundEvents.ARMOR_EQUIP_GOLD,
                    () -> Ingredient.of(Items.FEATHER),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "sky_boots"))),
                    2.0F,
                    0.05F));

    private SkyEquipment() {
    }
}
