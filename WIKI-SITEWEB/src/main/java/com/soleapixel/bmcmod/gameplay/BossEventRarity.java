package com.soleapixel.bmcmod.gameplay;

import com.soleapixel.bmcmod.ServerGameplayConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import javax.annotation.Nullable;

public enum BossEventRarity {
    COMMON(38, ChatFormatting.GRAY),
    UNCOMMON(16, ChatFormatting.GREEN),
    RARE(24, ChatFormatting.BLUE),
    EPIC(15, ChatFormatting.LIGHT_PURPLE),
    LEGENDARY(7, ChatFormatting.GOLD),
    /** Nom violet ; glow d’équipe jaune-or clair via {@link #teamGlowColor()} (distinct du légendaire {@link ChatFormatting#GOLD}). */
    MYTHIC(5, ChatFormatting.DARK_PURPLE);

    private final int weight;
    private final ChatFormatting formatting;

    BossEventRarity(int weight, ChatFormatting formatting) {
        this.weight = weight;
        this.formatting = formatting;
    }

    public int weight() {
        return weight;
    }

    public ChatFormatting formatting() {
        return formatting;
    }

    /** Couleur du glow scoreboard : mythique en jaune-or clair (proche du §e des noms peu communs vanilla), pas le §6 or foncé du légendaire. */
    public ChatFormatting teamGlowColor() {
        if (this == MYTHIC) {
            return ChatFormatting.YELLOW;
        }
        return formatting;
    }

    /**
     * Multiplicateur HP / dégâts : plus la rareté est haute, plus le miniboss est dangereux.
     * COMMON 1,0 → MYTHIC ~3,65.
     */
    public double statMultiplier() {
        return switch (this) {
            case COMMON -> 1.0D;
            case UNCOMMON -> 1.38D;
            case RARE -> 1.82D;
            case EPIC -> 2.35D;
            case LEGENDARY -> 2.95D;
            case MYTHIC -> 3.65D;
        };
    }

    public static int totalWeight() {
        int s = 0;
        for (BossEventRarity r : values()) {
            s += r.weight;
        }
        return s;
    }

    /** Natural miniboss spawn : uses {@link ServerGameplayConfig} weights; high tiers scale up in the End. */
    public static BossEventRarity roll(RandomSource random, boolean inEnd) {
        BossEventRarity[] order = values();
        int[] weights = new int[order.length];
        int total = 0;
        for (int i = 0; i < order.length; i++) {
            int w = configuredBaseWeight(order[i]);
            if (inEnd && isHighTier(order[i])) {
                w = Mth.ceil(w * ServerGameplayConfig.endHighTierWeightMultiplier());
            }
            w = Math.max(0, w);
            weights[i] = w;
            total += w;
        }
        if (total <= 0) {
            return rollLegacyEnumWeights(random, inEnd);
        }
        int roll = random.nextInt(total);
        int acc = 0;
        for (int i = 0; i < order.length; i++) {
            acc += weights[i];
            if (roll < acc) {
                return order[i];
            }
        }
        return MYTHIC;
    }

    private static int configuredBaseWeight(BossEventRarity r) {
        return switch (r) {
            case COMMON -> ServerGameplayConfig.bossWeightCommon();
            case UNCOMMON -> ServerGameplayConfig.bossWeightUncommon();
            case RARE -> ServerGameplayConfig.bossWeightRare();
            case EPIC -> ServerGameplayConfig.bossWeightEpic();
            case LEGENDARY -> ServerGameplayConfig.bossWeightLegendary();
            case MYTHIC -> ServerGameplayConfig.bossWeightMythic();
        };
    }

    private static boolean isHighTier(BossEventRarity r) {
        return r == RARE || r == EPIC || r == LEGENDARY || r == MYTHIC;
    }

    private static BossEventRarity rollLegacyEnumWeights(RandomSource random, boolean inEnd) {
        BossEventRarity[] order = values();
        int[] weights = new int[order.length];
        int total = 0;
        for (int i = 0; i < order.length; i++) {
            int w = order[i].weight;
            if (inEnd && isHighTier(order[i])) {
                w = Mth.ceil(w * ServerGameplayConfig.endHighTierWeightMultiplier());
            }
            w = Math.max(0, w);
            weights[i] = w;
            total += w;
        }
        if (total <= 0) {
            return COMMON;
        }
        int roll = random.nextInt(total);
        int acc = 0;
        for (int i = 0; i < order.length; i++) {
            acc += weights[i];
            if (roll < acc) {
                return order[i];
            }
        }
        return MYTHIC;
    }

    public static @Nullable BossEventRarity parse(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        String n = id.toLowerCase(java.util.Locale.ROOT);
        return switch (n) {
            case "common", "commun", "c" -> COMMON;
            case "uncommon", "peu_commun", "u" -> UNCOMMON;
            case "rare", "r" -> RARE;
            case "epic", "épique", "e" -> EPIC;
            case "legendary", "légendaire", "l" -> LEGENDARY;
            case "mythic", "mythique", "m" -> MYTHIC;
            default -> null;
        };
    }
}
