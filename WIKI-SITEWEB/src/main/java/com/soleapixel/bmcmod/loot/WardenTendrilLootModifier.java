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
 * Chance 50 % d’ajouter un tendril du Warden au butin de {@code minecraft:entities/warden}.
 */
public final class WardenTendrilLootModifier extends LootModifier {
    public static final MapCodec<WardenTendrilLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, WardenTendrilLootModifier::new));

    private static final float DROP_CHANCE = 0.5F;

    public WardenTendrilLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() < DROP_CHANCE) {
            generatedLoot.add(new ItemStack(ModItems.WARDEN_TENDRIL.get()));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
