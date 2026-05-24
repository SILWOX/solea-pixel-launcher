package com.soleapixel.bmcmod.equipment;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.Mth;

/**
 * Tier interpolé entre deux tiers vanilla (ex. pierre → fer) pour stats de minage / dégâts / durabilité.
 * Réparation : lingot de cuivre. À réutiliser pour les futurs équipements moddés sauf indication contraire.
 */
public record InterpolatedTier(Tier from, Tier to, float t, Ingredient repairIngredient) implements Tier {
    public static final Tier COPPER = new InterpolatedTier(Tiers.STONE, Tiers.IRON, 0.5F, Ingredient.of(Items.COPPER_INGOT));

    public InterpolatedTier {
        t = Mth.clamp(t, 0.0F, 1.0F);
    }

    @Override
    public int getUses() {
        return Mth.floor(Mth.lerp(t, from.getUses(), to.getUses()));
    }

    @Override
    public float getSpeed() {
        return Mth.lerp(t, from.getSpeed(), to.getSpeed());
    }

    @Override
    public float getAttackDamageBonus() {
        return Mth.lerp(t, from.getAttackDamageBonus(), to.getAttackDamageBonus());
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        return from.getIncorrectBlocksForDrops();
    }

    @Override
    public int getEnchantmentValue() {
        return Mth.floor(Mth.lerp(t, from.getEnchantmentValue(), to.getEnchantmentValue()));
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient;
    }
}
