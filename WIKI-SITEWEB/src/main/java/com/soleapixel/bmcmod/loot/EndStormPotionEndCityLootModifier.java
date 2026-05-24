package com.soleapixel.bmcmod.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.soleapixel.bmcmod.registry.ModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public final class EndStormPotionEndCityLootModifier extends LootModifier {
    public static final MapCodec<EndStormPotionEndCityLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, EndStormPotionEndCityLootModifier::new));

    public EndStormPotionEndCityLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() > 0.20F) {
            return generatedLoot;
        }
        int tier = 1 + context.getRandom().nextInt(4);
        generatedLoot.add(new ItemStack(stormBottle(tier)));
        if (tier >= 3 && context.getRandom().nextFloat() < 0.25F) {
            generatedLoot.add(new ItemStack(ModItems.END_STORM_BOTTLE_4.get()));
        }
        return generatedLoot;
    }

    private static Item stormBottle(int tier) {
        return switch (Mth.clamp(tier, 1, 4)) {
            case 1 -> ModItems.END_STORM_BOTTLE_1.get();
            case 2 -> ModItems.END_STORM_BOTTLE_2.get();
            case 3 -> ModItems.END_STORM_BOTTLE_3.get();
            default -> ModItems.END_STORM_BOTTLE_4.get();
        };
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}

