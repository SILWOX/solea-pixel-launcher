package com.soleapixel.bmcmod.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.network.chat.Component;

/**
 * Une page par enchantement : même GUI que le livre vanilla ({@link BookViewScreen}).
 */
public final class EnchantmentCodexScreen extends BookViewScreen {
    /** Ordre d’affichage (identifiants des clés {@code bmcmod.codex.entry.<id>.title|body}). */
    public static final String[] PAGE_IDS = {
            "bleeding",
            "life_steal",
            "timber",
            "crushing_blow",
            "fire_thorn",
            "explosive_shot",
            "excavator",
            "auto_smelt",
            "empathic_strike",
            "curse_of_launchstrike",
            "shield_charge",
            "whirlwind",
            "wide_sweep",
            "reaping",
            "briar_ring",
    };

    private EnchantmentCodexScreen(BookViewScreen.BookAccess access) {
        super(access);
    }

    public static void openFromClient() {
        Minecraft mc = Minecraft.getInstance();
        List<Component> pages = new ArrayList<>(PAGE_IDS.length);
        for (String id : PAGE_IDS) {
            Component title = Component.translatable("bmcmod.codex.entry." + id + ".title")
                    .withStyle(s -> s.withBold(true));
            Component body = Component.translatable("bmcmod.codex.entry." + id + ".body");
            pages.add(Component.empty().append(title).append(Component.literal("\n\n")).append(body));
        }
        BookViewScreen.BookAccess access = new BookViewScreen.BookAccess(pages);
        mc.setScreen(new EnchantmentCodexScreen(access));
    }
}
