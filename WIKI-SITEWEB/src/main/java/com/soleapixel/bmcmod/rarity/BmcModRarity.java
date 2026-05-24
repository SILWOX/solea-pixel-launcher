package com.soleapixel.bmcmod.rarity;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.Mth;

/**
 * Raretés personnalisées pour le mod. Les effets animés sont appliqués côté client dans {@link com.soleapixel.bmcmod.client.RarityTooltipHandler}.
 */
public enum BmcModRarity {
    UNCOMMON(0xFFFFFF, false, TooltipAccent.NONE),
    /** Jaune vanilla §e (même teinte que ChatFormatting.YELLOW). */
    COMMON(0xFFFF55, false, TooltipAccent.NONE),
    RARE(0x3399FF, false, TooltipAccent.NONE),
    EPIC(0xAA44FF, true, TooltipAccent.PURPLE),
    LEGENDARY(0xFFB020, true, TooltipAccent.GOLD),
    /** Aperçu statique : milieu de la palette periwinkle → cyan (cf. infobulle animée). */
    EXOTIC(0x6DCED1, true, TooltipAccent.SKY),
    MYTHIC(0xE8C547, true, TooltipAccent.MYTHIC_GOLD),
    CURSE(0xC62020, true, TooltipAccent.CURSE_RED),
    /** Violet vif en inventaire ; l’infobulle applique un dégradé « fracture » dédié. */
    FRAGMENTED(0xD86CFF, true, TooltipAccent.GLITCH);

    private final int staticRgb;
    private final boolean animatedTooltip;
    private final TooltipAccent accent;

    BmcModRarity(int staticRgb, boolean animatedTooltip, TooltipAccent accent) {
        this.staticRgb = staticRgb;
        this.animatedTooltip = animatedTooltip;
        this.accent = accent;
    }

    public boolean hasAnimatedTooltip() {
        return animatedTooltip;
    }

    public TooltipAccent tooltipAccent() {
        return accent;
    }

    /** Nom dans l'inventaire / hotbar : statique (aperçu lisible hors animation). */
    public Component styleItemName(Component base) {
        if (this == UNCOMMON) {
            return base.copy().withStyle(Style.EMPTY.withItalic(false));
        }
        return base.copy().withStyle(Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(staticRgb)));
    }

    /** Première ligne d'infobulle : animation pour les raretés concernées. */
    public Component styleTooltipTitle(Component base, long millis) {
        if (!animatedTooltip) {
            return styleItemName(base);
        }
        return switch (this) {
            case EPIC -> epicTitle(base, millis);
            case LEGENDARY -> legendaryTitle(base, millis);
            case EXOTIC -> exoticTitle(base, millis);
            case MYTHIC -> mythicTitle(base, millis);
            case CURSE -> curseTitle(base, millis);
            case FRAGMENTED -> fragmentedTitle(base, millis);
            default -> styleItemName(base);
        };
    }

    private static Component curseTitle(Component base, long millis) {
        float t = millis / 180f;
        int redA = 0xC62020;
        int redB = 0x7A0C0C;
        float blend = (float) (0.5 + 0.5 * Math.sin(t));
        int rgb = lerpRgb(redA, redB, blend);
        return base.copy().withStyle(Style.EMPTY
                .withItalic(false)
                .withBold(true)
                .withColor(TextColor.fromRgb(rgb)));
    }

    private static Component epicTitle(Component base, long millis) {
        float pulse = (float) (0.75 + 0.25 * Math.sin(millis / 190.0));
        int r = Mth.clamp((int) (120 * pulse), 80, 200);
        int g = Mth.clamp((int) (55 * pulse), 30, 120);
        int b = Mth.clamp((int) (255 * pulse), 160, 255);
        int rgb = (r << 16) | (g << 8) | b;
        return base.copy().withStyle(Style.EMPTY
                .withItalic(false)
                .withColor(TextColor.fromRgb(rgb)));
    }

    private static Component legendaryTitle(Component base, long millis) {
        float t = millis / 220f;
        int r = Mth.clamp((int) (255 * (0.85 + 0.15 * Math.sin(t))), 200, 255);
        int g = Mth.clamp((int) (220 * (0.75 + 0.25 * Math.sin(t + 1.2))), 160, 255);
        int b = Mth.clamp((int) (80 + 40 * Math.sin(t + 2.1)), 40, 140);
        int rgb = (r << 16) | (g << 8) | b;
        return base.copy().withStyle(Style.EMPTY
                .withItalic(false)
                .withColor(TextColor.fromRgb(rgb))
                .withBold(true));
    }

    /** Periwinkle doux → cyan néon (même famille que la bordure / fond infobulle). */
    private static final int EXOTIC_PERI = 0x8A9ED1;
    private static final int EXOTIC_CYAN = 0x50FFD1;

    private static Component exoticTitle(Component base, long millis) {
        float wave = (float) (0.5 + 0.5 * Math.sin(millis / 220f));
        int body = lerpRgb(EXOTIC_PERI, EXOTIC_CYAN, wave);
        float sheenPhase = millis / 95f;
        int sheen = (int) (22 * (0.5 + 0.5 * Math.sin(sheenPhase)));
        int r = Mth.clamp((body >> 16 & 0xFF) + sheen, 0, 255);
        int g = Mth.clamp((body >> 8 & 0xFF) + (int) (sheen * 0.85f), 0, 255);
        int b = Mth.clamp((body & 0xFF) + (int) (sheen * 0.55f), 0, 255);
        int rgb = (r << 16) | (g << 8) | b;
        return base.copy().withStyle(Style.EMPTY
                .withItalic(false)
                .withBold(true)
                .withColor(TextColor.fromRgb(rgb)));
    }

    /**
     * Même famille or que l’aperçu statique ; se distingue du légendaire par le soulignement et une animation
     * plus « précieuse » (reflets plus étroits, moins orangés que {@link #legendaryTitle}).
     */
    private static Component mythicTitle(Component base, long millis) {
        float t = millis / 320f;
        int goldA = 0xF0C85C;
        int goldB = 0xC99A3A;
        float blend = (float) (0.5 + 0.5 * Math.sin(t));
        int r = lerpChannel(goldA >> 16 & 0xFF, goldB >> 16 & 0xFF, blend);
        int g = lerpChannel(goldA >> 8 & 0xFF, goldB >> 8 & 0xFF, blend);
        int b = lerpChannel(goldA & 0xFF, goldB & 0xFF, blend);
        int sheen = (int) (14 * Math.sin(millis / 155.0));
        g = Mth.clamp(g + sheen, 0, 255);
        b = Mth.clamp(b + (int) (sheen * 0.35f), 0, 255);
        int rgb = (r << 16) | (g << 8) | b;
        return base.copy().withStyle(Style.EMPTY
                .withItalic(false)
                .withBold(true)
                .withUnderlined(true)
                .withColor(TextColor.fromRgb(rgb)));
    }

    /**
     * Fragmenté : dégradé « faille dimensionnelle » (violet profond → magenta → cyan) avec lueur lente ;
     * obfuscation très rare pour garder la lisibilité.
     */
    private static Component fragmentedTitle(Component base, long millis) {
        String text = base.getString();
        MutableComponent out = Component.literal("");
        double slow = millis / 88.0;
        int tick = (int) (millis / 52);
        int[] cps = text.codePoints().toArray();
        int voidRgb = 0x3A2060;
        int magRgb = 0xE848C8;
        int iceRgb = 0x58FFF0;
        for (int i = 0; i < cps.length; i++) {
            int cp = cps[i];
            if (cp == ' ') {
                out.append(Component.literal(" "));
                continue;
            }
            float phase = (float) (i * 0.38 + slow * 0.11);
            float wave = (float) (0.5 + 0.5 * Math.sin(phase + slow * 0.17));
            int body = wave < 0.34F ? lerpRgb(voidRgb, magRgb, wave / 0.34F)
                    : wave < 0.67F ? lerpRgb(magRgb, iceRgb, (wave - 0.34F) / 0.33F)
                    : lerpRgb(iceRgb, voidRgb, (wave - 0.67F) / 0.33F);
            int br = (int) (22 * Math.sin(slow * 1.1 + i * 0.65));
            int r = Mth.clamp((body >> 16 & 0xFF) + br, 0, 255);
            int g = Mth.clamp((body >> 8 & 0xFF) + br / 2, 0, 255);
            int b = Mth.clamp((body & 0xFF) + (int) (br * 0.75F), 0, 255);
            int rgb = (r << 16) | (g << 8) | b;
            Style st = Style.EMPTY.withItalic(false).withColor(TextColor.fromRgb(rgb));
            if (Mth.positiveModulo(i + tick / 3, 6) == 0) {
                st = st.withBold(true);
            }
            if (Mth.positiveModulo(i + tick, 17) == 0) {
                st = st.withObfuscated(true);
            }
            out.append(Component.literal(new String(Character.toChars(cp))).withStyle(st));
        }
        return out;
    }

    private static int lerpChannel(int a, int b, float t) {
        return Mth.clamp((int) Mth.lerp(t, a, b), 0, 255);
    }

    private static int lerpRgb(int from, int to, float t) {
        t = Mth.clamp(t, 0, 1);
        int r = lerpChannel(from >> 16 & 0xFF, to >> 16 & 0xFF, t);
        int g = lerpChannel(from >> 8 & 0xFF, to >> 8 & 0xFF, t);
        int b = lerpChannel(from & 0xFF, to & 0xFF, t);
        return (r << 16) | (g << 8) | b;
    }

    public enum TooltipAccent {
        NONE,
        PURPLE,
        GOLD,
        SKY,
        MYTHIC_GOLD,
        CURSE_RED,
        GLITCH
    }
}
