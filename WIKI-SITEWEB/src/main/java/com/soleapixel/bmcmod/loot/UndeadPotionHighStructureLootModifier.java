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
import net.neoforged.neoforge.common.loot.LootModifier;

public final class UndeadPotionHighStructureLootModifier extends LootModifier {
    public static final MapCodec<UndeadPotionHighStructureLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, UndeadPotionHighStructureLootModifier::new));

    private static final float CHANCE = 0.08F;

    public UndeadPotionHighStructureLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() > CHANCE) {
            return generatedLoot;
        }
        int tier = 3 + context.getRandom().nextInt(4); // 3..6
        generatedLoot.add(new ItemStack(undeadBottleForTier(tier)));
        if (context.getRandom().nextFloat() < 0.35F) {
            generatedLoot.add(new ItemStack(undeadBottleForTier(Mth.clamp(tier + 1, 1, 6))));
        }
        return generatedLoot;
    }

    private static Item undeadBottleForTier(int tier) {
        return switch (Mth.clamp(tier, 1, 6)) {
            case 1 -> ModItems.UNDEAD_BOTTLE_1.get();
            case 2 -> ModItems.UNDEAD_BOTTLE_2.get();
            case 3 -> ModItems.UNDEAD_BOTTLE_3.get();
            case 4 -> ModItems.UNDEAD_BOTTLE_4.get();
            case 5 -> ModItems.UNDEAD_BOTTLE_5.get();
            default -> ModItems.UNDEAD_BOTTLE_6.get();
        };
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
