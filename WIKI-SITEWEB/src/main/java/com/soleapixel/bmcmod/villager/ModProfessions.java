package com.soleapixel.bmcmod.villager;

import com.google.common.collect.ImmutableSet;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Items;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModProfessions {
    public static final DeferredRegister<VillagerProfession> PROFESSIONS =
            DeferredRegister.create(Registries.VILLAGER_PROFESSION, BmcMod.MODID);

    /**
     * Aucun POI requis : {@code heldJobSite} et {@code acquirableJobSite} toujours faux (comme le villageois
     * n’est lié à aucun bloc de métier). Le métier n’est obtenu que par guérison de sorcière (tag NBT).
     */
    public static final DeferredHolder<VillagerProfession, VillagerProfession> CURED_WITCH = PROFESSIONS.register("cured_witch",
            () -> new VillagerProfession(
                    "cured_witch",
                    h -> false,
                    h -> false,
                    ImmutableSet.of(
                            Items.REDSTONE,
                            Items.GLOWSTONE_DUST,
                            Items.GLASS_BOTTLE,
                            Items.GUNPOWDER,
                            Items.SPIDER_EYE,
                            ModItems.RUBY.get()),
                    ImmutableSet.of(),
                    SoundEvents.VILLAGER_WORK_CLERIC));

    private ModProfessions() {
    }
}
