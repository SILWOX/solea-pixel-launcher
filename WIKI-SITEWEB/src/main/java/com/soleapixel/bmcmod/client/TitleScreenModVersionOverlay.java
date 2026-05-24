package com.soleapixel.bmcmod.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import com.soleapixel.bmcmod.BmcMod;

/**
 * Empilé comme le footer vanilla/NeoForge : d’abord notre ligne, puis « Minecraft 1.x », puis NeoForge.
 * L’espacement suit {@link Font#lineHeight} + un petit gap, pour s’adapter police / interface.
 */
@EventBusSubscriber(modid = BmcMod.MODID, value = Dist.CLIENT)
public final class TitleScreenModVersionOverlay {
    /**
     * Marge du bas (ligne la plus basse = NeoForge, alignée sur le bandeau client ~10 px).
     * Même ordre d’idée que le copyright / version en bas d’écran.
     */
    private static final int FOOTER_BOTTOM_MARGIN = 10;
    /** Même écart qu’entre deux lignes de version (compact). */
    private static final int STACK_GAP = 2;
    /** Décale légèrement la ligne vers le bas (sens écran : Y augmente). */
    private static final int NUDGE_DOWN_PX = 3;

    private TitleScreenModVersionOverlay() {
    }

    @SubscribeEvent
    public static void onTitleScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen title)) {
            return;
        }
        String version = ModList.get()
                .getModContainerById(BmcMod.MODID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse("?");
        Component line = Component.literal("Better ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal("Minecraft").withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal(" Mod ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(version).withStyle(ChatFormatting.WHITE));
        Font font = Minecraft.getInstance().font;
        int lh = font.lineHeight;
        /*
         * Bas → haut : NeoForge (~height−10), puis Minecraft 1.x, puis nous — même pas vertical entre chaque.
         * baseline_minecraft ≈ baseline_neoforge − lh − gap ; baseline_nous ≈ baseline_minecraft − lh − gap.
         */
        int neoForgeBaselineApprox = title.height - FOOTER_BOTTOM_MARGIN;
        int minecraftBaselineApprox = neoForgeBaselineApprox - lh - STACK_GAP;
        int y = minecraftBaselineApprox - lh - STACK_GAP + NUDGE_DOWN_PX;
        event.getGuiGraphics().drawString(font, line, 2, y, 0xFFFFFF, true);
    }
}
