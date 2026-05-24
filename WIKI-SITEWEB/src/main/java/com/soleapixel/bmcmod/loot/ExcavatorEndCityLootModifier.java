package com.soleapixel.bmcmod.loot;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Livre enchanté très rare: Excavator, trouvé en trésors End City. */
public final class ExcavatorEndCityLootModifier extends LootModifier {
    public static final MapCodec<ExcavatorEndCityLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, ExcavatorEndCityLootModifier::new));

    private static final float CHANCE = 0.0032f;

    public ExcavatorEndCityLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() > CHANCE) {
            return generatedLoot;
        }
        var reg = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> h = reg.get(ModEnchantmentKeys.EXCAVATOR);
        if (h.isEmpty()) {
            return generatedLoot;
        }
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mut.set(h.get(), 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
        generatedLoot.add(book);
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
