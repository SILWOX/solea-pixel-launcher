package com.soleapixel.bmcmod.loot;

import java.util.Set;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import com.soleapixel.bmcmod.gameplay.UnknownBookLogic;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Livre inconnu : coffres de village (un peu rare) ; coffres de donjons (rare, seulement ~70 % des tables concernées).
 */
public final class UnknownBookLootModifier extends LootModifier {
    public static final MapCodec<UnknownBookLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, UnknownBookLootModifier::new));

    private static final float VILLAGE_CHANCE = 0.09F;
    private static final float DUNGEON_CHANCE = 0.062F;

    private static final Set<ResourceLocation> VILLAGE_CHESTS = Set.of(
            ResourceLocation.parse("minecraft:chests/village/village_weaponsmith"),
            ResourceLocation.parse("minecraft:chests/village/village_toolsmith"),
            ResourceLocation.parse("minecraft:chests/village/village_armorer"),
            ResourceLocation.parse("minecraft:chests/village/village_butcher"),
            ResourceLocation.parse("minecraft:chests/village/village_cartographer"),
            ResourceLocation.parse("minecraft:chests/village/village_desert_house"),
            ResourceLocation.parse("minecraft:chests/village/village_fisher"),
            ResourceLocation.parse("minecraft:chests/village/village_fletcher"),
            ResourceLocation.parse("minecraft:chests/village/village_mason"),
            ResourceLocation.parse("minecraft:chests/village/village_plains_house"),
            ResourceLocation.parse("minecraft:chests/village/village_savanna_house"),
            ResourceLocation.parse("minecraft:chests/village/village_shepherd"),
            ResourceLocation.parse("minecraft:chests/village/village_snowy_house"),
            ResourceLocation.parse("minecraft:chests/village/village_taiga_house"),
            ResourceLocation.parse("minecraft:chests/village/village_tannery"),
            ResourceLocation.parse("minecraft:chests/village/village_temple"));

    private static final Set<ResourceLocation> DUNGEON_CHESTS = Set.of(
            ResourceLocation.parse("minecraft:chests/simple_dungeon"),
            ResourceLocation.parse("minecraft:chests/abandoned_mineshaft"),
            ResourceLocation.parse("minecraft:chests/desert_pyramid"),
            ResourceLocation.parse("minecraft:chests/jungle_temple"),
            ResourceLocation.parse("minecraft:chests/stronghold_library"),
            ResourceLocation.parse("minecraft:chests/stronghold_corridor"),
            ResourceLocation.parse("minecraft:chests/stronghold_crossing"),
            ResourceLocation.parse("minecraft:chests/woodland_mansion"),
            ResourceLocation.parse("minecraft:chests/ancient_city"),
            ResourceLocation.parse("minecraft:chests/ancient_city_ice_box"),
            ResourceLocation.parse("minecraft:chests/end_city_treasure"),
            ResourceLocation.parse("minecraft:chests/nether_bridge"),
            ResourceLocation.parse("minecraft:chests/bastion_treasure"),
            ResourceLocation.parse("minecraft:chests/bastion_hoglin_stable"),
            ResourceLocation.parse("minecraft:chests/bastion_other"),
            ResourceLocation.parse("minecraft:chests/ruined_portal"),
            ResourceLocation.parse("minecraft:chests/pillager_outpost"),
            ResourceLocation.parse("minecraft:chests/igloo_chest"),
            ResourceLocation.parse("minecraft:chests/trial_chambers/reward"),
            ResourceLocation.parse("minecraft:chests/trial_chambers/supply"),
            ResourceLocation.parse("minecraft:chests/trial_chambers/reward_ominous"));

    public UnknownBookLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        ResourceLocation id = context.getQueriedLootTableId();
        var random = context.getRandom();
        if (VILLAGE_CHESTS.contains(id)) {
            if (random.nextFloat() < VILLAGE_CHANCE) {
                generatedLoot.add(UnknownBookLogic.createLootStack(random, context.getLevel().registryAccess()));
            }
            return generatedLoot;
        }
        if (DUNGEON_CHESTS.contains(id)) {
            if (random.nextFloat() > 0.7F) {
                return generatedLoot;
            }
            if (random.nextFloat() < DUNGEON_CHANCE) {
                generatedLoot.add(UnknownBookLogic.createLootStack(random, context.getLevel().registryAccess()));
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
