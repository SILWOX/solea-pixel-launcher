package com.soleapixel.bmcmod.client;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class InfusionSoulTooltipClient {
    private static final ResourceLocation ALT_FONT = ResourceLocation.withDefaultNamespace("alt");

    private InfusionSoulTooltipClient() {
    }

    /**
     * Toute ligne en {@code minecraft:alt} (illisible en inventaire) repasse en texte standard gris.
     * Le coût d’âmes reste visible sur la table d’infusion ({@code gui.bmcmod.infusion.soul_cost}), pas ici.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onItemTooltip(ItemTooltipEvent event) {
        for (int i = 0; i < event.getToolTip().size(); i++) {
            Component c = event.getToolTip().get(i);
            Component c2 = demoteAltToPlain(c);
            if (c2 != c) {
                event.getToolTip().set(i, c2);
            }
        }
    }

    private static Component demoteAltToPlain(Component c) {
        if (usesAltFont(c.getStyle())) {
            return plainGrayLine(c);
        }
        return c;
    }

    private static boolean usesAltFont(Style s) {
        return ALT_FONT.equals(s.getFont());
    }

    private static Component plainGrayLine(Component c) {
        return Component.literal(c.getString())
                .withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_GRAY).withItalic(false).withUnderlined(false)
                        .withStrikethrough(false)
                        .withObfuscated(false));
    }

}
