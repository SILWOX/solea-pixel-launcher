package com.soleapixel.bmcmod.registry;

import com.mojang.serialization.MapCodec;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.loot.ExcavatorEndCityLootModifier;
import com.soleapixel.bmcmod.loot.EndCityEnchantedDiamondAppleLootModifier;
import com.soleapixel.bmcmod.loot.EndCityEnderiteTemplateLootModifier;
import com.soleapixel.bmcmod.loot.EndStormPotionEndCityLootModifier;
import com.soleapixel.bmcmod.loot.LaunchstrikeCityLootModifier;
import com.soleapixel.bmcmod.loot.TreasureEnchantLootModifier;
import com.soleapixel.bmcmod.loot.UndeadPotionCommonStructureLootModifier;
import com.soleapixel.bmcmod.loot.UndeadPotionHighStructureLootModifier;
import com.soleapixel.bmcmod.loot.UndeadPotionMidStructureLootModifier;
import com.soleapixel.bmcmod.loot.UnknownBookLootModifier;
import com.soleapixel.bmcmod.loot.WardenTendrilLootModifier;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModGlobalLoot {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLOBAL_LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.GLOBAL_LOOT_MODIFIER_SERIALIZERS, BmcMod.MODID);

    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> TREASURE_ENCHANT_BOOKS =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("treasure_enchant_books", () -> TreasureEnchantLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> END_CITY_ENDERITE_TEMPLATE =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("end_city_enderite_template", () -> EndCityEnderiteTemplateLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> END_CITY_ENCHANTED_DIAMOND_APPLE =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("end_city_enchanted_diamond_apple", () -> EndCityEnchantedDiamondAppleLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> LAUNCHSTRIKE_CITY_TREASURE =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("launchstrike_city_treasure", () -> LaunchstrikeCityLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> EXCAVATOR_END_CITY_TREASURE =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("excavator_end_city_treasure", () -> ExcavatorEndCityLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> UNKNOWN_BOOK =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("unknown_book", () -> UnknownBookLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> WARDEN_TENDRIL =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("warden_tendril", () -> WardenTendrilLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> UNDEAD_POTION_COMMON_STRUCTURE =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("undead_potion_common_structure", () -> UndeadPotionCommonStructureLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> UNDEAD_POTION_MID_STRUCTURE =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("undead_potion_mid_structure", () -> UndeadPotionMidStructureLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> UNDEAD_POTION_HIGH_STRUCTURE =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("undead_potion_high_structure", () -> UndeadPotionHighStructureLootModifier.CODEC);
    public static final net.neoforged.neoforge.registries.DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> END_STORM_POTION_END_CITY =
            GLOBAL_LOOT_MODIFIER_SERIALIZERS.register("end_storm_potion_end_city", () -> EndStormPotionEndCityLootModifier.CODEC);

    private ModGlobalLoot() {
    }
}
