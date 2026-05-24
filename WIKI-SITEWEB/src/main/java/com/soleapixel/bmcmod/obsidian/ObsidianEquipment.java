package com.soleapixel.bmcmod.obsidian;

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

public final class ObsidianEquipment {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, BmcMod.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> OBSIDIAN_ARMOR_MATERIAL = ARMOR_MATERIALS.register("obsidian",
            () -> new ArmorMaterial(
                    Map.of(
                            ArmorItem.Type.BOOTS, 1,
                            ArmorItem.Type.LEGGINGS, 2,
                            ArmorItem.Type.CHESTPLATE, 3,
                            ArmorItem.Type.HELMET, 1),
                    52,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(Items.OBSIDIAN),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "obsidian/obsidian"))),
                    2.0F,
                    0.05F));

    private ObsidianEquipment() {
    }
}
