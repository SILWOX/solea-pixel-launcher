package com.soleapixel.bmcmod.registry;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import com.soleapixel.bmcmod.BmcMod;

/** Clés des enchantements définis en JSON ({@code data/bmcmod/enchantment}). */
public final class ModEnchantmentKeys {
    public static final ResourceKey<Enchantment> BLEEDING = key("bleeding");
    public static final ResourceKey<Enchantment> LIFE_STEAL = key("life_steal");
    public static final ResourceKey<Enchantment> TIMBER = key("timber");
    public static final ResourceKey<Enchantment> CRUSHING_BLOW = key("crushing_blow");
    public static final ResourceKey<Enchantment> FIRE_THORN = key("fire_thorn");
    public static final ResourceKey<Enchantment> EXPLOSIVE_SHOT = key("explosive_shot");
    public static final ResourceKey<Enchantment> EXCAVATOR = key("excavator");
    public static final ResourceKey<Enchantment> AUTO_SMELT = key("auto_smelt");
    /** Malédiction : les dégâts de mêlée infligés à une cible sont subis par l'attaquant. */
    public static final ResourceKey<Enchantment> EMPATHIC_STRIKE = key("empathic_strike");
    /**
     * Malédiction (arbalète seule) : le tir se « substitue » par une propulsion du tireur sur la trajectoire, avec malus.
     * Incompatible avec tout autre enchantement d'arbalète.
     */
    public static final ResourceKey<Enchantment> CURSE_OF_LAUNCHSTRIKE = key("curse_of_launchstrike");

    /**
     * Blocage réussi : chance de repousser fort l’attaquant, et petite chance de dégâts (niveaux I–III).
     */
    public static final ResourceKey<Enchantment> SHIELD_CHARGE = key("shield_charge");

    /** Tourbillon visuel : trois rotations ; durabilité du coup spécial ×3. Faux uniquement. */
    public static final ResourceKey<Enchantment> WHIRLWIND = key("whirlwind");
    /** Rayon du tourbillon augmenté (niveaux I–V). Faux uniquement. */
    public static final ResourceKey<Enchantment> WIDE_SWEEP = key("wide_sweep");
    /** Chance de bonus sur une unité de drop lors de la récolte en zone (faux). */
    public static final ResourceKey<Enchantment> REAPING = key("reaping");
    /** Tourbillon : marque au sol + faiblesse brève sur les cibles. */
    public static final ResourceKey<Enchantment> BRIAR_RING = key("briar_ring");

    private ModEnchantmentKeys() {
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, BmcMod.loc(path));
    }

    /**
     * Niveau d’un enchantement datapack sur la stack : lit d’abord {@link DataComponents#ENCHANTMENTS}
     * (clé {@link net.minecraft.core.Holder#is(ResourceKey)}), puis retombe sur {@link EnchantmentHelper}.
     */
    public static int enchantmentLevel(ItemStack stack, RegistryAccess reg, ResourceKey<Enchantment> key) {
        if (stack.isEmpty()) {
            return 0;
        }
        ItemEnchantments on = stack.get(DataComponents.ENCHANTMENTS);
        if (on != null && !on.isEmpty()) {
            for (var ent : on.entrySet()) {
                var holder = ent.getKey();
                boolean match = holder.unwrapKey().map(k -> k.equals(key)).orElse(false)
                        || holder.is(key);
                if (match) {
                    return ent.getIntValue();
                }
            }
        }
        return reg.lookupOrThrow(Registries.ENCHANTMENT)
                .get(key)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, stack))
                .orElse(0);
    }
}
