package com.soleapixel.bmcmod.villager;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

import java.util.List;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class UndeadInvasionPotionTrades {
    /** Prix émeraudes par niveau I–VI (aligné sur l’ancien Clerc). */
    private static final int[] CLERIC_EMERALD_BY_TIER = {18, 22, 26, 30, 36, 42};
    private static final int[] WANDERING_GENERIC_COST = {20, 24};
    private static final int[] WANDERING_RARE_COST = {30, 38};
    /**
     * Probabilité qu’une entrée d’échange Undead apparaisse pour ce palier. Plusieurs listages remplissaient
     * tout le Clerc maître ; une seule entrée + chance libère les autres offres (vanilla, table d’amélioration…).
     */
    private static final float CLERIC_EXPERT_UNDEAD_CHANCE = 0.38F;
    private static final float CLERIC_MASTER_UNDEAD_CHANCE = 0.32F;
    private static final float WANDERING_GENERIC_UNDEAD_CHANCE = 0.35F;
    private static final float WANDERING_RARE_UNDEAD_CHANCE = 0.30F;

    private UndeadInvasionPotionTrades() {
    }

    @SubscribeEvent
    public static void onClericTrades(VillagerTradesEvent event) {
        if (!event.getType().equals(VillagerProfession.CLERIC)) {
            return;
        }
        var trades = event.getTrades();
        // Expert : une offre max, niveau I–III au hasard si le tirage passe.
        trades.get(4).add(probableRandomUndeadBottleTrade(1, 3, CLERIC_EMERALD_BY_TIER, CLERIC_EXPERT_UNDEAD_CHANCE));
        // Maître : une offre max, niveau IV–VI au hasard si le tirage passe (ne monopolise plus 4 emplacements).
        trades.get(5).add(probableRandomUndeadBottleTrade(4, 6, CLERIC_EMERALD_BY_TIER, CLERIC_MASTER_UNDEAD_CHANCE));
    }

    @SubscribeEvent
    public static void onWanderingTrades(WandererTradesEvent event) {
        event.getGenericTrades().add(probableWanderingBottle(1, 2, WANDERING_GENERIC_COST, WANDERING_GENERIC_UNDEAD_CHANCE));
        event.getRareTrades().add(probableWanderingBottle(3, 4, WANDERING_RARE_COST, WANDERING_RARE_UNDEAD_CHANCE));
    }

    public static List<VillagerTrades.ItemListing> curedWitchInvasionOffers() {
        return List.of(
                fixedUndeadBottleTrade(22, 2),
                fixedUndeadBottleTrade(28, 3),
                fixedUndeadBottleTrade(36, 4),
                fixedUndeadBottleTrade(46, 5),
                fixedUndeadBottleTrade(54, 6)
        );
    }

    private static VillagerTrades.ItemListing fixedUndeadBottleTrade(int emeraldCost, int tier) {
        return (Entity trader, RandomSource random) -> {
            ItemStack out = new ItemStack(bottleItemForTier(tier));
            return new MerchantOffer(new ItemCost(Items.EMERALD, emeraldCost), out, 1, 20, 0.15F);
        };
    }

    /** Échange Undead pour Clerc : une tentative ; si {@code appearChance} échoue, aucune ligne Undead pour ce palier. */
    private static VillagerTrades.ItemListing probableRandomUndeadBottleTrade(
            int minTier, int maxTier, int[] costByTier, float appearChance) {
        return (Entity trader, RandomSource random) -> randomUndeadBottleOffer(random, minTier, maxTier, costByTier, appearChance);
    }

    @Nullable
    private static MerchantOffer randomUndeadBottleOffer(
            RandomSource random, int minTier, int maxTier, int[] costByTier, float appearChance) {
        if (random.nextFloat() >= appearChance) {
            return null;
        }
        int tier = minTier + random.nextInt(maxTier - minTier + 1);
        ItemStack out = new ItemStack(bottleItemForTier(tier));
        int cost = costByTier[tier - 1];
        return new MerchantOffer(new ItemCost(Items.EMERALD, cost), out, 1, 20, 0.15F);
    }

    private static VillagerTrades.ItemListing probableWanderingBottle(
            int minTier, int maxTier, int[] costsAlignedToMinTier, float appearChance) {
        return (Entity trader, RandomSource random) -> {
            if (random.nextFloat() >= appearChance) {
                return null;
            }
            int tier = minTier + random.nextInt(maxTier - minTier + 1);
            ItemStack out = new ItemStack(bottleItemForTier(tier));
            int cost = costsAlignedToMinTier[tier - minTier];
            return new MerchantOffer(new ItemCost(Items.EMERALD, cost), out, 1, 20, 0.15F);
        };
    }

    private static Item bottleItemForTier(int tier) {
        int t = Math.max(1, Math.min(6, tier));
        return switch (t) {
            case 1 -> ModItems.UNDEAD_BOTTLE_1.get();
            case 2 -> ModItems.UNDEAD_BOTTLE_2.get();
            case 3 -> ModItems.UNDEAD_BOTTLE_3.get();
            case 4 -> ModItems.UNDEAD_BOTTLE_4.get();
            case 5 -> ModItems.UNDEAD_BOTTLE_5.get();
            default -> ModItems.UNDEAD_BOTTLE_6.get();
        };
    }
}
