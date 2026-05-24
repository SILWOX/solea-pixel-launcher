package com.soleapixel.bmcmod.villager;

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

/**
 * Matériaux des chapeaux villageois (casques uniquement). Stats légèrement au-dessus du cuivre.
 */
public final class VillagerHatEquipment {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, BmcMod.MODID);

    /** Durabilité casque cuivre vanilla ~130 ; un peu plus + défense 3, résistance légère. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BUTCHER = reg("butcher_headband");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> LIBRARIAN = reg("librarian_hat");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WEAPONSMITH = reg("weaponsmith_eyepatch");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SHEPHERD = reg("shepherd_hat");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FISHERMAN = reg("fisherman_hat");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CARTOGRAPHER = reg("cartographer_monocle");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMORER = reg("armorer_goggles");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FARMER = reg("farmer_hat");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> WITCH = reg("witch_hat");
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> UNDEAD_CROWN = reg("undead_crown");

    private VillagerHatEquipment() {
    }

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> reg(String textureId) {
        return ARMOR_MATERIALS.register("villager_hat_" + textureId, () -> new ArmorMaterial(
                Map.of(
                        ArmorItem.Type.BOOTS, 0,
                        ArmorItem.Type.LEGGINGS, 0,
                        ArmorItem.Type.CHESTPLATE, 0,
                        ArmorItem.Type.HELMET, 3),
                12,
                SoundEvents.ARMOR_EQUIP_LEATHER,
                () -> Ingredient.of(Items.EMERALD),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(BmcMod.MODID, "villager_hat/" + textureId))),
                0.5F,
                0.05F));
    }
}
