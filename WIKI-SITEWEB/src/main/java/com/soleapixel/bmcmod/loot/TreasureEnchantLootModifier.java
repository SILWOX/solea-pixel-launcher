package com.soleapixel.bmcmod.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModEnchantmentKeys;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Coffres structures / trésors : livres Timber ou Tir explosif ; livre + outil pour la malédiction
 * {@link ModEnchantmentKeys#EMPATHIC_STRIKE}.
 */
public final class TreasureEnchantLootModifier extends LootModifier {
    public static final MapCodec<TreasureEnchantLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).apply(inst, TreasureEnchantLootModifier::new));

    private static final TagKey<Item> REBOUND_CURSE_TOOLS = TagKey.create(Registries.ITEM, BmcMod.loc("enchantable/rebound_curse"));

    public TreasureEnchantLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        var reg = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Optional<Holder.Reference<Enchantment>> timber = reg.get(ModEnchantmentKeys.TIMBER);
        Optional<Holder.Reference<Enchantment>> explosive = reg.get(ModEnchantmentKeys.EXPLOSIVE_SHOT);
        Optional<Holder.Reference<Enchantment>> empathic = reg.get(ModEnchantmentKeys.EMPATHIC_STRIKE);

        float roll = context.getRandom().nextFloat();
        if (roll < 0.07f && (timber.isPresent() || explosive.isPresent())) {
            Holder<Enchantment> pick;
            if (timber.isPresent() && explosive.isPresent()) {
                pick = context.getRandom().nextBoolean() ? timber.get() : explosive.get();
            } else if (timber.isPresent()) {
                pick = timber.get();
            } else {
                pick = explosive.orElseThrow();
            }
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            mut.set(pick, 1);
            book.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
            generatedLoot.add(book);
        }

        if (empathic.isPresent()) {
            if (context.getRandom().nextFloat() < 0.055f) {
                ItemStack curseBook = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mut.set(empathic.get(), 1);
                curseBook.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
                generatedLoot.add(curseBook);
            }
            if (context.getRandom().nextFloat() < 0.038f) {
                tryApplyEmpathicToRandomTool(generatedLoot, empathic.get(), context.getRandom());
            }
        }

        Optional<Holder.Reference<Enchantment>> shieldCharge = reg.get(ModEnchantmentKeys.SHIELD_CHARGE);
        if (shieldCharge.isPresent() && isStrongholdChest(context.getQueriedLootTableId())) {
            if (context.getRandom().nextFloat() < 0.20f) {
                int lvl = 1 + context.getRandom().nextInt(3);
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                mut.set(shieldCharge.get(), lvl);
                book.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
                generatedLoot.add(book);
            }
        }

        tryAddScytheStructureBook(generatedLoot, context);

        return generatedLoot;
    }

    /**
     * Coffres structures (même conditions que le modificateur) : livre de la faux (Tourbillon, Grand balayage, Moisson, Cercle
     * d’épine).
     */
    private static void tryAddScytheStructureBook(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (context.getRandom().nextFloat() >= 0.12f) {
            return;
        }
        var reg = context.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int pick = context.getRandom().nextInt(4);
        Optional<Holder.Reference<Enchantment>> ench = switch (pick) {
            case 0 -> reg.get(ModEnchantmentKeys.WHIRLWIND);
            case 1 -> reg.get(ModEnchantmentKeys.WIDE_SWEEP);
            case 2 -> reg.get(ModEnchantmentKeys.REAPING);
            default -> reg.get(ModEnchantmentKeys.BRIAR_RING);
        };
        if (ench.isEmpty()) {
            return;
        }
        Holder<Enchantment> holder = ench.get();
        int max = holder.value().getMaxLevel();
        int lvl = switch (pick) {
            case 0 -> 1;
            case 1 -> 1 + context.getRandom().nextInt(5);
            case 2 -> 1 + context.getRandom().nextInt(5);
            default -> 1 + context.getRandom().nextInt(2);
        };
        lvl = Math.min(lvl, max);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mut.set(holder, lvl);
        book.set(DataComponents.STORED_ENCHANTMENTS, mut.toImmutable());
        generatedLoot.add(book);
    }

    private static boolean isStrongholdChest(ResourceLocation lootTableId) {
        return lootTableId.equals(ResourceLocation.parse("minecraft:chests/stronghold_library"))
                || lootTableId.equals(ResourceLocation.parse("minecraft:chests/stronghold_corridor"))
                || lootTableId.equals(ResourceLocation.parse("minecraft:chests/stronghold_crossing"));
    }

    private static void tryApplyEmpathicToRandomTool(ObjectArrayList<ItemStack> loot, Holder<Enchantment> empathic, RandomSource random) {
        List<ItemStack> candidates = new ArrayList<>();
        for (ItemStack stack : loot) {
            if (stack.isEmpty() || stack.is(Items.ENCHANTED_BOOK)) {
                continue;
            }
            if (!stack.is(REBOUND_CURSE_TOOLS) || !stack.isDamageableItem()) {
                continue;
            }
            if (EnchantmentHelper.getItemEnchantmentLevel(empathic, stack) > 0) {
                continue;
            }
            candidates.add(stack);
        }
        if (candidates.isEmpty()) {
            return;
        }
        ItemStack chosen = candidates.get(random.nextInt(candidates.size()));
        ItemEnchantments existing = chosen.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(existing);
        mut.set(empathic, 1);
        chosen.set(DataComponents.ENCHANTMENTS, mut.toImmutable());
    }

    @Override
    public MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
        return CODEC;
    }
}
