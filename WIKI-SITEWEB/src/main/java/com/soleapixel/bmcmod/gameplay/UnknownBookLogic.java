package com.soleapixel.bmcmod.gameplay;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.soleapixel.bmcmod.registry.ModDataComponents;
import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;

/**
 * Génération du livre inconnu (loot) et enchantement aléatoire à l’enclume (biais malédiction).
 */
public final class UnknownBookLogic {
    private UnknownBookLogic() {
    }

    public static ItemStack createLootStack(RandomSource random, RegistryAccess registryAccess) {
        ItemStack stack = new ItemStack(ModItems.UNKNOWN_BOOK.get());
        stack.set(ModDataComponents.LATENT_STORED_ENCHANTMENTS.get(), rollRandomBookEnchantments(random, registryAccess));
        return stack;
    }

    /**
     * Livre enchanté prêt à l’emploi (même tirage que le latent du loot) — pour révélation dans le liquide d’XP
     * quand le livre inconnu n’a pas de composant latent.
     */
    public static ItemStack createRevealedEnchantedBook(RandomSource random, RegistryAccess registryAccess) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponents.STORED_ENCHANTMENTS, rollRandomBookEnchantments(random, registryAccess));
        return book;
    }

    private static ItemEnchantments rollRandomBookEnchantments(RandomSource random, RegistryAccess registryAccess) {
        var lookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        int power = 18 + random.nextInt(20);
        List<EnchantmentInstance> picked = selectForBook(random, lookup, power);
        if (picked.isEmpty()) {
            picked = selectForBook(random, lookup, 30 + random.nextInt(15));
        }
        ItemEnchantments.Mutable mut = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (EnchantmentInstance inst : picked) {
            mut.set(inst.enchantment, inst.level);
        }
        return mut.toImmutable();
    }

    /**
     * Applique un enchantement aléatoire compatible ; les malédictions ont un poids multiplié.
     */
    public static ItemStack applyRandomEnchant(RandomSource random, RegistryAccess registryAccess, ItemStack toolTemplate) {
        ItemStack result = toolTemplate.copyWithCount(1);
        int power = 14 + random.nextInt(18);
        var encLookup = registryAccess.lookupOrThrow(Registries.ENCHANTMENT);
        List<EnchantmentInstance> options = EnchantmentHelper.getAvailableEnchantmentResults(power, result, enchantStream(encLookup));
        if (options.isEmpty()) {
            options = EnchantmentHelper.getAvailableEnchantmentResults(22 + random.nextInt(12), result, enchantStream(encLookup));
        }
        if (options.isEmpty()) {
            return result;
        }
        List<WeightedEntry.Wrapper<EnchantmentInstance>> weighted = new ArrayList<>();
        for (EnchantmentInstance inst : options) {
            int w = inst.getWeight().asInt();
            if (inst.enchantment.is(EnchantmentTags.CURSE)) {
                w = Math.max(1, w * 5);
            }
            weighted.add(WeightedEntry.wrap(inst, w));
        }
        WeightedRandom.getRandomItem(random, weighted)
                .ifPresent(w -> result.enchant(w.data().enchantment, w.data().level));
        return result;
    }

    private static List<EnchantmentInstance> selectForBook(RandomSource random, net.minecraft.core.HolderLookup.RegistryLookup<Enchantment> lookup, int power) {
        return EnchantmentHelper.selectEnchantment(random, new ItemStack(Items.BOOK), power, enchantStream(lookup));
    }

    @SuppressWarnings("unchecked")
    private static Stream<Holder<Enchantment>> enchantStream(net.minecraft.core.HolderLookup.RegistryLookup<Enchantment> lookup) {
        return (Stream<Holder<Enchantment>>) (Stream<?>) lookup.listElements();
    }
}
