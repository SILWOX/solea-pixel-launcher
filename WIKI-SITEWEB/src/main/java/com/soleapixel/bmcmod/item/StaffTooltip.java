package com.soleapixel.bmcmod.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

/**
 * Infobulles des bâtons : détails longs uniquement si Maj est maintenue.
 */
public final class StaffTooltip {
    private StaffTooltip() {
    }

    /**
     * @return {@code true} si on peut ajouter les lignes détaillées (Maj enfoncée) ; sinon ajoute l’invite et renvoie {@code false}.
     */
    public static boolean showDetailedLines(List<Component> tooltip, TooltipFlag flag) {
        if (!flag.hasShiftDown()) {
            tooltip.add(Component.translatable("item.bmcmod.staff.hold_shift").withStyle(ChatFormatting.DARK_GRAY));
            return false;
        }
        return true;
    }
}
