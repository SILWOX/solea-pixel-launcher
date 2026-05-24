package com.soleapixel.bmcmod.gameplay;

import java.util.ArrayList;
import java.util.List;

import com.soleapixel.bmcmod.BmcMod;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Livre de bienvenue une seule fois, lors du premier déblocage de l’advancement d’accueil (pas à chaque connexion). */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class WelcomeBookGameplay {
    private static final String WELCOME_ADVANCEMENT_PATH = "better_minecraft/welcome_journey";
    private static final String WELCOME_ADV_CRITERION = "granted_from_login";
    private static final String BOOK_AUTHOR = "Better Minecraft";
    private static final String WEBSITE_URL = "https://soleapixel.com";

    private WelcomeBookGameplay() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        AdvancementHolder adv = player.server.getAdvancements().get(BmcMod.loc(WELCOME_ADVANCEMENT_PATH));
        if (adv != null) {
            AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) {
                ItemStack book = createWelcomeBook();
                if (!player.getInventory().add(book)) {
                    player.drop(book, false);
                }
            }
        }
        grantWelcomeAdvancement(player);
    }

    private static void grantWelcomeAdvancement(ServerPlayer player) {
        AdvancementHolder adv = player.server.getAdvancements().get(BmcMod.loc(WELCOME_ADVANCEMENT_PATH));
        if (adv == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
        if (progress.isDone()) {
            return;
        }
        List<String> remaining = new ArrayList<>();
        for (String criterion : progress.getRemainingCriteria()) {
            remaining.add(criterion);
        }
        if (remaining.contains(WELCOME_ADV_CRITERION)) {
            player.getAdvancements().award(adv, WELCOME_ADV_CRITERION);
            return;
        }
        for (String criterion : remaining) {
            player.getAdvancements().award(adv, criterion);
        }
    }

    private static ItemStack createWelcomeBook() {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        Component page1 = Component.empty()
                .append(Component.literal("Better Minecraft\n").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(Component.literal("Welcome, adventurer.\n\n").withStyle(ChatFormatting.YELLOW))
                .append(Component.literal("Thanks for installing the mod.\n")
                        .withStyle(ChatFormatting.GRAY))
                .append(Component.literal("Enjoy the journey, the upgrades,\n")
                        .withStyle(ChatFormatting.AQUA))
                .append(Component.literal("and every challenge ahead.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));

        Component link = Component.literal("soleapixel.com")
                .withStyle(Style.EMPTY
                        .withColor(ChatFormatting.BLUE)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, WEBSITE_URL))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open website"))));

        Component page2 = Component.empty()
                .append(Component.literal("Official Website\n").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN))
                .append(Component.literal("Click the link below:\n\n").withStyle(ChatFormatting.GRAY))
                .append(link)
                .append(Component.literal("\n\nSee news, updates and more.")
                        .withStyle(ChatFormatting.DARK_AQUA));

        Component page3 = Component.empty()
                .append(Component.literal("Thank you for playing!\n\n").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("— Better Minecraft").withStyle(ChatFormatting.ITALIC, ChatFormatting.DARK_PURPLE));

        pages.add(Filterable.passThrough(page1));
        pages.add(Filterable.passThrough(page2));
        pages.add(Filterable.passThrough(page3));
        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough("Welcome"),
                BOOK_AUTHOR,
                0,
                pages,
                true);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        return stack;
    }
}
