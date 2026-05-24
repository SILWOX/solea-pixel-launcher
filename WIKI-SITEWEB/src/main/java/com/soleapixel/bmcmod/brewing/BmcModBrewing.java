package com.soleapixel.bmcmod.brewing;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;

import com.soleapixel.bmcmod.registry.ModPotions;

import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

public final class BmcModBrewing {
    private BmcModBrewing() {
    }

    public static void registerRecipes(RegisterBrewingRecipesEvent event) {
        var reg = event.getRegistryAccess().lookupOrThrow(Registries.POTION);
        var b = event.getBuilder();
        b.addMix(Potions.AWKWARD, Items.PRISMARINE_SHARD, reg.getOrThrow(ModPotions.SHRINK.getKey()));
        b.addMix(Potions.AWKWARD, Items.BLAZE_POWDER, reg.getOrThrow(ModPotions.GROW.getKey()));
        b.addMix(Potions.AWKWARD, Items.ECHO_SHARD, reg.getOrThrow(ModPotions.VEIN_WHISPER.getKey()));
        b.addMix(reg.getOrThrow(ModPotions.VEIN_WHISPER.getKey()), Items.GLOWSTONE_DUST, reg.getOrThrow(ModPotions.STRONG_VEIN_WHISPER.getKey()));
    }
}
