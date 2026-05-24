package com.soleapixel.bmcmod.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import com.soleapixel.bmcmod.registry.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Trésor des vaisseaux d’End City (table {@code minecraft:chests/end_city_treasure}).
 * En 1.21.1 la pomme dorée enchantée n’y figure plus ; on calibre la probabilité sur le <strong>premier
 * pool</strong> du coffre (somme des poids = 85), comme si on y ajoutait une entrée poids 1 — même
 * logique qu’une entrée rare type « un seul item dans le pool » (ex. ancienne EGA en poids 1 / total).
 */
public final class EndCityEnchantedDiamondAppleLootModifier extends LootModifier {
    /** Équivalent d’un tirage poids 1 sur le pool 1 d’{@code end_city_treasure} (total des poids = 85). */
    public static final float CHANCE_MATCHING_WEIGHT_1_IN_85 = 1.0F / 85.0F;

    public static final MapCodec<EndCityEnchantedDiamondAppleLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, EndCityEnchantedDiamondAppleLootModifier::new));

    public EndCityEnchantedDiamondAppleLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < CHANCE_MATCHING_WEIGHT_1_IN_85) {
            generatedLoot.add(new ItemStack(ModItems.ENCHANTED_DIAMOND_APPLE.get()));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
