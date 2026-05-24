package com.soleapixel.bmcmod.villager;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import javax.annotation.Nullable;

/**
 * Le Clerc Maître peut proposer la table d'amélioration : chaque villageois a 60 % de chance d'obtenir ce
 * contrat parmi ses offres de niveau Maître (une fois l'offre tirée, elle reste jusqu'à épuisement).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class ClericUpgradeTableTrades {
    /** Probabilité qu'un Clerc Maître affiche cet échange lors de la génération des offres de ce palier. */
    private static final float TRADE_APPEAR_CHANCE = 0.60F;
    private static final int EMERALD_COST = 36;
    private static final int MAX_USES = 1;
    private static final int VILLAGER_XP = 30;
    private static final float PRICE_MULTIPLIER = 0.05F;

    private ClericUpgradeTableTrades() {
    }

    @SubscribeEvent
    public static void onClericTrades(VillagerTradesEvent event) {
        if (!event.getType().equals(VillagerProfession.CLERIC)) {
            return;
        }
        event.getTrades().get(5).add(ClericUpgradeTableTrades::upgradeTableOffer);
    }

    @Nullable
    private static MerchantOffer upgradeTableOffer(Entity trader, RandomSource random) {
        if (random.nextFloat() >= TRADE_APPEAR_CHANCE) {
            return null;
        }
        ItemStack out = new ItemStack(ModItems.UPGRADE_TABLE_ITEM.get());
        return new MerchantOffer(new ItemCost(Items.EMERALD, EMERALD_COST), out, MAX_USES, VILLAGER_XP, PRICE_MULTIPLIER);
    }
}
