package com.soleapixel.bmcmod.quest;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;

public enum QuestDifficulty {
    EASY("easy", ChatFormatting.GREEN),
    NORMAL("normal", ChatFormatting.YELLOW),
    HARD("hard", ChatFormatting.GOLD),
    EXTREME("extreme", ChatFormatting.RED),
    SPECIAL("special", ChatFormatting.LIGHT_PURPLE),
    /** Contrat unique : cible « Bounty Hunter » après un délai aléatoire ; plus rare que SPECIAL. */
    BOUNTY_HUNTER("bounty_hunter", ChatFormatting.DARK_RED);

    /** Durée contrat chasseur de primes : 30–60 min (ticks). */
    public static final int BOUNTY_TIME_MIN_TICKS = 30 * 60 * 20;
    public static final int BOUNTY_TIME_MAX_TICKS = 60 * 60 * 20;

    private final String key;
    private final ChatFormatting color;

    QuestDifficulty(String key, ChatFormatting color) {
        this.key = key;
        this.color = color;
    }

    public String translationKey() {
        return "quest.bmcmod.difficulty." + key;
    }

    public MutableComponent title() {
        return Component.translatable(translationKey()).withStyle(color);
    }

    /** Couleur du libellé de difficulté (tooltip, HUD). */
    public ChatFormatting labelColor() {
        return color;
    }

    /**
     * Parse pour {@code /bmc quest give <difficulté> …} (anglais + quelques alias FR).
     *
     * @return null si inconnu
     */
    public static QuestDifficulty tryParseCommand(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.toLowerCase(Locale.ROOT).trim();
        return switch (s) {
            case "easy", "e", "facile" -> EASY;
            case "normal", "n", "medium", "moyen" -> NORMAL;
            case "hard", "h", "difficile" -> HARD;
            case "extreme", "x", "extrême" -> EXTREME;
            case "special", "s", "spécial" -> SPECIAL;
            case "bounty_hunter", "bounty", "bh", "chasseur" -> BOUNTY_HUNTER;
            default -> null;
        };
    }

    public static QuestDifficulty roll(RandomSource random) {
        float f = random.nextFloat();
        if (f < 0.26F) {
            return EASY;
        }
        if (f < 0.52F) {
            return NORMAL;
        }
        if (f < 0.78F) {
            return HARD;
        }
        if (f < 0.97F) {
            return EXTREME;
        }
        if (f < 0.993F) {
            return SPECIAL;
        }
        return BOUNTY_HUNTER;
    }

    /** Toutes les quêtes : entre 10 et 60 minutes (ticks). */
    public static final int QUEST_TIME_MIN_TICKS = 10 * 60 * 20;
    public static final int QUEST_TIME_MAX_TICKS = 60 * 60 * 20;

    /**
     * Fenêtre de temps (ticks) après le début, bornée [10 min, 60 min] : facile = plus large, extrême = courte.
     */
    public int rollDurationTicks(RandomSource random) {
        int min;
        int max;
        switch (this) {
            case EASY -> {
                min = 38 * 60 * 20;
                max = 60 * 60 * 20;
            }
            case NORMAL -> {
                min = 28 * 60 * 20;
                max = 50 * 60 * 20;
            }
            case HARD -> {
                min = 18 * 60 * 20;
                max = 42 * 60 * 20;
            }
            case EXTREME -> {
                min = QUEST_TIME_MIN_TICKS;
                max = 30 * 60 * 20;
            }
            case BOUNTY_HUNTER -> {
                min = BOUNTY_TIME_MIN_TICKS;
                max = BOUNTY_TIME_MAX_TICKS;
            }
            default -> {
                min = 24 * 60 * 20;
                max = 52 * 60 * 20;
            }
        }
        min = Math.max(QUEST_TIME_MIN_TICKS, min);
        max = Math.min(QUEST_TIME_MAX_TICKS, Math.max(min, max));
        return min + random.nextInt(Math.max(1, max - min + 1));
    }
}
