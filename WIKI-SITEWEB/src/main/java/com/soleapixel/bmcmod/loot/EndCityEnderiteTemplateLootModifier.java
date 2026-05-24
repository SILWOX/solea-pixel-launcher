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
 * Ajoute rarement un modèle de forge Enderite dans les coffres d’End City (comme l’upgrade Netherite).
 */
public final class EndCityEnderiteTemplateLootModifier extends LootModifier {
    public static final MapCodec<EndCityEnderiteTemplateLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, EndCityEnderiteTemplateLootModifier::new));

    public EndCityEnderiteTemplateLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() > 0.055f) {
            return generatedLoot;
        }
        generatedLoot.add(new ItemStack(ModItems.ENDERITE_UPGRADE_SMITHING_TEMPLATE.get()));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
