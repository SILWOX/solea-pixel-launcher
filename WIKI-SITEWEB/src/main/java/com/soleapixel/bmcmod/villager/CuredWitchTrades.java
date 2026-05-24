package com.soleapixel.bmcmod.villager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.registry.ModItems;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

@EventBusSubscriber(modid = BmcMod.MODID)
public final class CuredWitchTrades {
    /** 10 minutes max d’effet sur les potions. */
    public static final int MAX_EFFECT_TICKS = 20 * 60 * 10;

    private CuredWitchTrades() {
    }

    private static final List<Holder<Potion>> BASIC_POTIONS = List.of(
            Potions.HEALING,
            Potions.SWIFTNESS,
            Potions.NIGHT_VISION,
            Potions.FIRE_RESISTANCE,
            Potions.WATER_BREATHING,
            Potions.REGENERATION,
            Potions.STRENGTH,
            Potions.WEAKNESS,
            Potions.SLOWNESS,
            Potions.POISON,
            Potions.LEAPING,
            Potions.INVISIBILITY);

    private static final List<Holder<Potion>> MID_POTIONS = List.of(
            Potions.LONG_SWIFTNESS,
            Potions.LONG_NIGHT_VISION,
            Potions.LONG_FIRE_RESISTANCE,
            Potions.STRONG_HEALING,
            Potions.STRONG_HARMING,
            Potions.LONG_REGENERATION,
            Potions.LONG_STRENGTH,
            Potions.LONG_POISON,
            Potions.LONG_WEAKNESS,
            Potions.LONG_SLOWNESS,
            Potions.STRONG_LEAPING,
            Potions.LONG_INVISIBILITY,
            Potions.TURTLE_MASTER);

    /** Paliers expert / maître : effets rares, durées fortes, ou déjà “chargés” côté vanilla. */
    private static final List<Holder<Potion>> EXPERT_POTIONS = List.of(
            Potions.LUCK,
            Potions.SLOW_FALLING,
            Potions.LONG_SLOW_FALLING,
            Potions.TURTLE_MASTER,
            Potions.STRONG_TURTLE_MASTER,
            Potions.LONG_TURTLE_MASTER,
            Potions.STRONG_HARMING,
            Potions.STRONG_HEALING,
            Potions.LONG_REGENERATION,
            Potions.STRONG_LEAPING,
            Potions.LONG_STRENGTH,
            Potions.STRONG_SWIFTNESS,
            Potions.STRONG_STRENGTH,
            Potions.LONG_SWIFTNESS,
            Potions.LONG_INVISIBILITY,
            Potions.LONG_NIGHT_VISION,
            Potions.LONG_FIRE_RESISTANCE,
            Potions.STRONG_REGENERATION,
            Potions.STRONG_POISON);

    private static final List<Holder<Potion>> MASTER_POTIONS = buildMasterPotionPool();

    private static List<Holder<Potion>> buildMasterPotionPool() {
        var out = new ArrayList<Holder<Potion>>(EXPERT_POTIONS);
        for (var p : MID_POTIONS) {
            if (!out.contains(p)) {
                out.add(p);
            }
        }
        return List.copyOf(out);
    }

    private static List<Holder<Potion>> allBasicAndMid() {
        List<Holder<Potion>> out = new ArrayList<>(BASIC_POTIONS);
        out.addAll(MID_POTIONS);
        return out;
    }

    public static RandomSource offerRng(Villager villager, int tableLevel, int offerSlot) {
        long a = villager.getUUID().getMostSignificantBits();
        long b = villager.getUUID().getLeastSignificantBits();
        long s = a ^ (b << 1) ^ 0x9E3779B97F4A7C15L * (long) tableLevel ^ 0x7F4A7C15L * (long) offerSlot;
        return RandomSource.create(s);
    }

    private static void shuffleIndices(int n, int[] out, RandomSource rng) {
        for (int i = 0; i < n; i++) {
            out[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int t = out[i];
            out[i] = out[j];
            out[j] = t;
        }
    }

    private static Holder<Potion> potionForSlot(
            Villager v, int tableLevel, int offerSlot, List<Holder<Potion>> pool) {
        int n = pool.size();
        if (n == 0) {
            return Potions.HEALING;
        }
        int[] order = new int[n];
        shuffleIndices(n, order, offerRng(v, tableLevel, 0));
        return pool.get(order[offerSlot % n]);
    }

    /** Durées plafonnées à {@link #MAX_EFFECT_TICKS} ; effets pris sur la définition {@link Potion}. */
    private static ItemStack capDurationsOnStack(Item bottle, Holder<Potion> pot) {
        List<MobEffectInstance> capped = new ArrayList<>();
        for (MobEffectInstance e : pot.value().getEffects()) {
            Holder<MobEffect> h = e.getEffect();
            int d = e.getDuration() <= 0 ? e.getDuration() : Math.min(e.getDuration(), MAX_EFFECT_TICKS);
            capped.add(new MobEffectInstance(h, d, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
        }
        PotionContents after = new PotionContents(Optional.of(Potions.WATER), Optional.empty(), capped);
        ItemStack s = new ItemStack(bottle);
        s.set(DataComponents.POTION_CONTENTS, after);
        return s;
    }

    @SubscribeEvent
    public static void registerTrades(VillagerTradesEvent event) {
        if (!event.getType().equals(ModProfessions.CURED_WITCH.get())) {
            return;
        }
        var trades = event.getTrades();
        trades.put(1, tier1());
        trades.put(2, tier2());
        trades.put(3, tier3());
        trades.put(4, tier4());
        trades.put(5, tier5());
    }

    private static List<VillagerTrades.ItemListing> tier1() {
        List<VillagerTrades.ItemListing> out = new ArrayList<>();
        int s = 0;
        for (int i = 0; i < 6; i++) {
            out.add(vanillaTrade(1, s++, 1, 3, 8, 2, BASIC_POTIONS, false, false, false, true));
        }
        for (int i = 0; i < 3; i++) {
            Ing g = TIER1_ING.get(i);
            out.add(ingredientTrade(1, s++, 1, 3, 8, 2, g, BASIC_POTIONS, false, false, false, true));
        }
        return out;
    }

    private static List<VillagerTrades.ItemListing> tier2() {
        var pool = allBasicAndMid();
        List<VillagerTrades.ItemListing> out = new ArrayList<>();
        int s = 0;
        for (int i = 0; i < 8; i++) {
            out.add(vanillaTrade(2, s++, 2, 6, 10, 5, pool, false, true, false, true));
        }
        for (int i = 0; i < 3; i++) {
            Ing g = TIER2_ING.get(i);
            out.add(ingredientTrade(2, s++, 2, 6, 10, 5, g, pool, false, true, false, true));
        }
        return out;
    }

    private static List<VillagerTrades.ItemListing> tier3() {
        List<VillagerTrades.ItemListing> out = new ArrayList<>();
        int s = 0;
        for (int i = 0; i < 6; i++) {
            out.add(vanillaTrade(3, s++, 3, 10, 12, 10, MID_POTIONS, false, true, true, true));
        }
        for (int i = 0; i < 3; i++) {
            Ing g = TIER3_ING.get(i);
            out.add(ingredientTrade(3, s++, 3, 10, 12, 10, g, MID_POTIONS, false, true, true, true));
        }
        return out;
    }

    private static List<VillagerTrades.ItemListing> tier4() {
        List<VillagerTrades.ItemListing> out = new ArrayList<>();
        int s = 0;
        for (int i = 0; i < 4; i++) {
            out.add(vanillaTrade(4, s++, 5, 16, 12, 20, EXPERT_POTIONS, false, true, true, true));
        }
        for (int i = 0; i < 2; i++) {
            Ing g = TIER4_ING.get(i);
            out.add(ingredientTrade(4, s++, 3, 8, 12, 20, g, EXPERT_POTIONS, false, true, true, true));
        }
        for (int i = 0; i < 2; i++) {
            out.add(customTrade(4, s++, 4, 10, 6, 20, true, BrewStyle.EXPERT_ARCANE, true));
        }
        for (int i = 0; i < 2; i++) {
            out.add(customTrade(4, s++, 3, 9, 6, 20, true, BrewStyle.EXPERT_VENOM, true));
        }
        out.add(UndeadInvasionPotionTrades.curedWitchInvasionOffers().get(0));
        out.add(UndeadInvasionPotionTrades.curedWitchInvasionOffers().get(1));
        return out;
    }

    private static List<VillagerTrades.ItemListing> tier5() {
        List<VillagerTrades.ItemListing> out = new ArrayList<>();
        int s = 0;
        for (int i = 0; i < 2; i++) {
            out.add(vanillaTrade(5, s++, 8, 22, 16, 30, MASTER_POTIONS, false, true, true, true));
        }
        for (int i = 0; i < 2; i++) {
            Ing g = TIER5_ING.get(i);
            out.add(ingredientTrade(5, s++, 4, 10, 16, 30, g, MASTER_POTIONS, true, true, true, true));
        }
        for (int i = 0; i < 2; i++) {
            out.add(customTrade(5, s++, 4, 12, 5, 30, true, BrewStyle.MASTER_ARCANE, true));
        }
        for (int i = 0; i < 2; i++) {
            out.add(customTrade(5, s++, 4, 12, 5, 30, true, BrewStyle.MASTER_VENOM, true));
        }
        for (int i = 0; i < 2; i++) {
            out.add(customTrade(5, s++, 5, 14, 4, 30, true, BrewStyle.MASTER_CHAOS, true));
        }
        out.add(UndeadInvasionPotionTrades.curedWitchInvasionOffers().get(2));
        out.add(UndeadInvasionPotionTrades.curedWitchInvasionOffers().get(3));
        out.add(UndeadInvasionPotionTrades.curedWitchInvasionOffers().get(4));
        return out;
    }

    private record Ing(Item item, int count) {
    }

    private static final List<Ing> TIER1_ING = List.of(
            new Ing(Items.NETHER_WART, 2), new Ing(Items.SUGAR, 1), new Ing(Items.SPIDER_EYE, 1));
    private static final List<Ing> TIER2_ING = List.of(
            new Ing(Items.NETHER_WART, 1), new Ing(Items.GUNPOWDER, 1), new Ing(Items.SPIDER_EYE, 2));
    private static final List<Ing> TIER3_ING = List.of(
            new Ing(Items.NETHER_WART, 1), new Ing(Items.FERMENTED_SPIDER_EYE, 1), new Ing(Items.PUFFERFISH, 1));
    private static final List<Ing> TIER4_ING = List.of(new Ing(Items.GHAST_TEAR, 1), new Ing(Items.MAGMA_CREAM, 1));
    private static final List<Ing> TIER5_ING = List.of(
            new Ing(Items.ENDER_PEARL, 1), new Ing(Items.PHANTOM_MEMBRANE, 1));

    private static Item chooseBottle(RandomSource r, boolean splash, boolean pickB, boolean ling) {
        if (ling) {
            return switch (r.nextInt(3)) {
                case 0 -> Items.POTION;
                case 1 -> Items.SPLASH_POTION;
                default -> Items.LINGERING_POTION;
            };
        }
        if (pickB) {
            return r.nextBoolean() ? Items.POTION : Items.SPLASH_POTION;
        }
        if (splash) {
            return Items.SPLASH_POTION;
        }
        return Items.POTION;
    }

    private static VillagerTrades.ItemListing vanillaTrade(
            int table,
            int slot,
            int minR,
            int maxR,
            int uses,
            int xp,
            List<Holder<Potion>> pool,
            boolean splash,
            boolean pickB,
            boolean ling,
            boolean cap) {
        return (e, wRnd) -> {
            if (!(e instanceof Villager v)) {
                return new MerchantOffer(new ItemCost(ModItems.RUBY.get(), 1), ItemStack.EMPTY, uses, xp, 0.05f);
            }
            var r = offerRng(v, table, slot);
            int n = minR + r.nextInt(maxR - minR + 1);
            var bottle = chooseBottle(r, splash, pickB, ling);
            var pot = potionForSlot(v, table, slot, pool);
            ItemStack out = cap ? capDurationsOnStack(bottle, pot) : PotionContents.createItemStack(bottle, pot);
            return new MerchantOffer(new ItemCost(ModItems.RUBY.get(), n), out, uses, xp, 0.05f);
        };
    }

    private static VillagerTrades.ItemListing ingredientTrade(
            int table, int slot, int minR, int maxR, int uses, int xp, Ing ing, List<Holder<Potion>> pool,
            boolean splash, boolean pickB, boolean ling, boolean cap) {
        return (e, w) -> {
            if (!(e instanceof Villager v)) {
                return new MerchantOffer(
                        new ItemCost(ModItems.RUBY.get(), 1),
                        Optional.of(new ItemCost(ing.item, ing.count)),
                        ItemStack.EMPTY,
                        uses,
                        xp,
                        0.05f);
            }
            var r = offerRng(v, table, slot);
            int n = minR + r.nextInt(maxR - minR + 1);
            var bottle = chooseBottle(r, splash, pickB, ling);
            var pot = potionForSlot(v, table, slot, pool);
            ItemStack out = cap ? capDurationsOnStack(bottle, pot) : PotionContents.createItemStack(bottle, pot);
            return new MerchantOffer(
                    new ItemCost(ModItems.RUBY.get(), n), Optional.of(new ItemCost(ing.item, ing.count)), out, uses, xp, 0.05f);
        };
    }

    private static VillagerTrades.ItemListing customTrade(
            int table, int slot, int minR, int maxR, int maxUses, int xp, boolean pickB, BrewStyle style, boolean cap) {
        return (e, w) -> {
            if (!(e instanceof Villager v)) {
                return new MerchantOffer(new ItemCost(ModItems.RUBY.get(), 1), ItemStack.EMPTY, maxUses, xp, 0.05f);
            }
            var r = offerRng(v, table, slot);
            int n = minR + r.nextInt(maxR - minR + 1);
            Item bottle = chooseBottle(r, false, pickB, true);
            List<MobEffectInstance> fx = customBrewEffects(r, style);
            ItemStack draft = new ItemStack(bottle);
            draft.set(DataComponents.POTION_CONTENTS, new PotionContents(
                    Optional.empty(), Optional.empty(), capCustoms(fx, cap)));
            return new MerchantOffer(new ItemCost(ModItems.RUBY.get(), n), draft, maxUses, xp, 0.05f);
        };
    }

    private static List<MobEffectInstance> capCustoms(List<MobEffectInstance> f, boolean cap) {
        if (!cap) {
            return f;
        }
        List<MobEffectInstance> o = new ArrayList<>();
        for (var e : f) {
            int d = e.getDuration() <= 0 ? e.getDuration() : Math.min(e.getDuration(), MAX_EFFECT_TICKS);
            o.add(new MobEffectInstance(e.getEffect(), d, e.getAmplifier(), e.isAmbient(), e.isVisible(), e.showIcon()));
        }
        return o;
    }

    /** Potion personnalisée : combos “arcane” (rare, Luck / Absorption / buffs) ou “venin” (offensif). */
    private enum BrewStyle {
        EXPERT_ARCANE,
        EXPERT_VENOM,
        MASTER_ARCANE,
        MASTER_VENOM,
        MASTER_CHAOS
    }

    private static final List<Holder<MobEffect>> ARCANE_BREW_ORDER = List.of(
            MobEffects.LUCK,
            MobEffects.ABSORPTION,
            MobEffects.REGENERATION,
            MobEffects.DAMAGE_RESISTANCE,
            MobEffects.DAMAGE_BOOST,
            MobEffects.DIG_SPEED,
            MobEffects.MOVEMENT_SPEED,
            MobEffects.NIGHT_VISION,
            MobEffects.FIRE_RESISTANCE,
            MobEffects.SLOW_FALLING,
            MobEffects.JUMP,
            MobEffects.INVISIBILITY);

    private static final List<Holder<MobEffect>> VENOM_BREW_EXPERT = List.of(
            MobEffects.POISON,
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.WEAKNESS,
            MobEffects.HUNGER,
            MobEffects.DARKNESS);

    private static final List<Holder<MobEffect>> VENOM_BREW_MASTER = List.of(
            MobEffects.HARM,
            MobEffects.POISON,
            MobEffects.WITHER,
            MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.WEAKNESS,
            MobEffects.DARKNESS,
            MobEffects.HUNGER);

    /** Troisième “couche” des philtres du chaos (pas dans les venins pour garder 3 rôles distincts). */
    private static final List<Holder<MobEffect>> CHAOS_SPICE_ORDER = List.of(
            MobEffects.GLOWING,
            MobEffects.BLINDNESS,
            MobEffects.DARKNESS,
            MobEffects.CONFUSION,
            MobEffects.LUCK);

    private static List<MobEffectInstance> customBrewEffects(RandomSource r, BrewStyle style) {
        return switch (style) {
            case EXPERT_ARCANE -> brewConsecutive(r, false, ARCANE_BREW_ORDER, 2);
            case EXPERT_VENOM -> brewConsecutive(r, false, VENOM_BREW_EXPERT, 2);
            case MASTER_ARCANE -> brewConsecutive(r, true, ARCANE_BREW_ORDER, 2 + r.nextInt(2));
            case MASTER_VENOM -> brewConsecutive(r, true, VENOM_BREW_MASTER, 2 + r.nextInt(2));
            case MASTER_CHAOS -> brewChaos(r);
        };
    }

    private static List<MobEffectInstance> brewConsecutive(
            RandomSource r, boolean master, List<Holder<MobEffect>> order, int count) {
        int n = order.size();
        int start = n > 0 ? r.nextInt(n) : 0;
        List<MobEffectInstance> list = new ArrayList<>();
        for (int k = 0; k < count; k++) {
            list.add(strongFromEffect(r, master, order.get((start + k) % n)));
        }
        return list;
    }

    private static List<MobEffectInstance> brewChaos(RandomSource r) {
        List<MobEffectInstance> list = new ArrayList<>();
        list.add(strongFromEffect(r, true, pickEffect(r, ARCANE_BREW_ORDER)));
        list.add(strongFromEffect(r, true, pickEffect(r, VENOM_BREW_MASTER)));
        list.add(strongFromEffect(r, true, pickEffect(r, CHAOS_SPICE_ORDER)));
        return list;
    }

    private static Holder<MobEffect> pickEffect(RandomSource r, List<Holder<MobEffect>> pool) {
        return pool.get(r.nextInt(pool.size()));
    }

    private static MobEffectInstance strongFromEffect(RandomSource r, boolean master, Holder<MobEffect> effect) {
        if (effect.is(MobEffects.HARM) || effect.is(MobEffects.HEAL)) {
            int amp = effect.is(MobEffects.HARM) && master ? r.nextInt(2) : 0;
            return new MobEffectInstance(effect, 1, amp, false, true, true);
        }
        int amp;
        if (effect.is(MobEffects.DAMAGE_BOOST) || effect.is(MobEffects.DAMAGE_RESISTANCE)) {
            amp = Math.max(0, (master ? r.nextInt(3) : r.nextInt(2)) - 1);
        } else if (effect.is(MobEffects.ABSORPTION) || effect.is(MobEffects.JUMP)) {
            amp = Math.min(2, master ? r.nextInt(3) : r.nextInt(2));
        } else if (effect.is(MobEffects.MOVEMENT_SLOWDOWN)) {
            amp = master ? 1 + r.nextInt(3) : r.nextInt(3);
        } else if (effect.is(MobEffects.POISON)) {
            amp = master ? r.nextInt(2) : Math.max(0, r.nextInt(2) - 1);
        } else if (effect.is(MobEffects.WITHER)) {
            amp = 0;
        } else if (effect.is(MobEffects.WEAKNESS) || effect.is(MobEffects.HUNGER)) {
            amp = r.nextInt(2);
        } else {
            amp = r.nextInt(2);
        }
        int d = 1200 + r.nextInt(master ? 5000 : 4000);
        d = Math.min(d, MAX_EFFECT_TICKS);
        if (effect.is(MobEffects.WITHER)) {
            d = 80 + r.nextInt(160);
        } else if (effect.is(MobEffects.REGENERATION)) {
            d = d / 2;
        } else if (effect.is(MobEffects.ABSORPTION)) {
            d = d / 3;
        } else if (effect.is(MobEffects.SLOW_FALLING) || effect.is(MobEffects.INVISIBILITY)) {
            d = d / 2;
        } else if (effect.is(MobEffects.DARKNESS) || effect.is(MobEffects.BLINDNESS) || effect.is(MobEffects.CONFUSION)) {
            d = d * 2 / 3;
        } else if (effect.is(MobEffects.HUNGER)) {
            d = d / 2;
        } else if (effect.is(MobEffects.GLOWING)) {
            d = Math.min(d, 200 + r.nextInt(400));
        }
        d = Math.min(d, MAX_EFFECT_TICKS);
        return new MobEffectInstance(effect, d, amp, false, true, true);
    }
}
