package com.soleapixel.bmcmod.command;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.block.feeder.FeederAutoFeed;
import com.soleapixel.bmcmod.villager.ModProfessions;
import com.soleapixel.bmcmod.villager.WitchCuringEvents;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /bmc} et {@code /betterminecraft} : aide paginée ({@code /bmc}, {@code /bmc help}, {@code /bmc <page>}),
 * {@code /bmc book} pour un livre signé.
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class BmcModHelpCommand {
    /** Auteur affiché sur le livre (signature). */
    public static final String BOOK_AUTHOR = "SOLEA PIXEL";

    private static final int LINES_PER_PAGE = 2;
    private static final String[] HELP_LINE_KEYS = {
            "bmcmod.commands.help.givehead",
            "bmcmod.commands.help.metamorph",
            "bmcmod.commands.help.undeadinvasion",
            "bmcmod.commands.help.endstorm",
            "bmcmod.commands.help.quest",
            "bmcmod.commands.help.crystal",
            "bmcmod.commands.help.bossevent",
            "bmcmod.commands.help.nether",
            "bmcmod.commands.help.end",
            "bmcmod.commands.help.overworld",
            "bmcmod.commands.help.eatevent",
    };

    private BmcModHelpCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        registerHelp(d, "bmc");
        registerHelp(d, "betterminecraft");
    }

    private static void registerHelp(CommandDispatcher<CommandSourceStack> dispatcher, String name) {
        int maxPage = maxPages();
        dispatcher.register(Commands.literal(name)
                .executes(ctx -> sendHelpPage(ctx.getSource(), 1, maxPage))
                .then(Commands.literal("help").executes(ctx -> sendHelpPage(ctx.getSource(), 1, maxPage)))
                .then(Commands.literal("book").executes(ctx -> giveHelpBook(ctx.getSource())))
                .then(Commands.argument("page", IntegerArgumentType.integer(1, maxPage))
                        .executes(ctx -> sendHelpPage(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "page"),
                                maxPage)))
                .then(UndeadInvasionCommand.buildUndeadInvasion())
                .then(EndStormCommand.buildEndStorm())
                .then(UndeadInvasionCommand.buildQuestGive())
                .then(BmcCrystalCommand.buildCrystalBranch())
                .then(BossEventCommand.build())
                .then(buildSpawnCuredWitch())
                .then(Commands.literal("eatevent")
                        .then(Commands.literal("run")
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getLevel() instanceof ServerLevel sl)) {
                                        ctx.getSource().sendFailure(
                                                Component.translatable("bmcmod.commands.eatevent.run.not_overworld"));
                                        return 0;
                                    }
                                    int fed = FeederAutoFeed.runDebugEatCycle(sl);
                                    ctx.getSource().sendSuccess(
                                            () -> Component.translatable("bmcmod.commands.eatevent.run.success", fed),
                                            true);
                                    return 1;
                                }))));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> buildSpawnCuredWitch() {
        return Commands.literal("CuredWitch")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> spawnCuredWitch(ctx.getSource()))
                .then(Commands.literal("spawn")
                        .executes(ctx -> spawnCuredWitch(ctx.getSource())));
    }

    private static int spawnCuredWitch(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos pos;
        if (source.getEntity() != null) {
            pos = BlockPos.containing(source.getEntity().position()).relative(source.getEntity().getDirection());
        } else {
            pos = BlockPos.containing(source.getPosition());
        }

        VillagerType type = VillagerType.byBiome(level.getBiome(pos));
        Villager villager = (Villager) EntityType.VILLAGER.spawn(level, null, pos, MobSpawnType.COMMAND, true, false);
        if (villager == null) {
            source.sendFailure(Component.translatable("bmcmod.commands.curedwitch.spawn_failed"));
            return 0;
        }
        villager.getPersistentData().putBoolean(WitchCuringEvents.TAG_CURED_WITCH_LOCK, true);
        villager.setVillagerData(new VillagerData(type, ModProfessions.CURED_WITCH.get(), 1));
        villager.setVillagerXp(0);
        villager.refreshBrain(level);
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.curedwitch.spawn_success"), true);
        return 1;
    }

    private static int maxPages() {
        return Math.max(1, (HELP_LINE_KEYS.length + LINES_PER_PAGE - 1) / LINES_PER_PAGE);
    }

    private static int sendHelpPage(CommandSourceStack source, int page, int maxPage) {
        if (page < 1) {
            page = 1;
        }
        if (page > maxPage) {
            page = maxPage;
        }
        source.sendSuccess(
                () -> Component.translatable("bmcmod.commands.help.title")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                false);
        source.sendSuccess(
                () -> Component.literal("SOLEA PIXEL")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                        .append(Component.translatable("bmcmod.commands.help.subtitle_suffix")
                                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)),
                false);
        source.sendSuccess(
                () -> Component.literal("························")
                        .withStyle(ChatFormatting.DARK_GRAY),
                false);
        if (page == 1) {
            source.sendSuccess(
                    () -> Component.translatable("bmcmod.commands.help.intro")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    false);
        }
        int start = (page - 1) * LINES_PER_PAGE;
        for (int i = 0; i < LINES_PER_PAGE; i++) {
            int idx = start + i;
            if (idx < HELP_LINE_KEYS.length) {
                final String key = HELP_LINE_KEYS[idx];
                source.sendSuccess(() -> Component.translatable(key).withStyle(ChatFormatting.GRAY), false);
            }
        }
        if (page == maxPage) {
            source.sendSuccess(
                    () -> Component.translatable("bmcmod.commands.help.footer")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
                    false);
        }
        final int pageFinal = page;
        final int maxFinal = maxPage;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.help.page_hint", pageFinal, maxFinal), false);
        return 1;
    }

    private static int giveHelpBook(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.translatable("bmcmod.commands.help.book.players_only"));
            return 0;
        }
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        pages.add(Filterable.passThrough(Component.translatable("bmcmod.commands.help.book.p1")));
        pages.add(Filterable.passThrough(Component.translatable("bmcmod.commands.help.book.p2")));
        pages.add(Filterable.passThrough(Component.translatable("bmcmod.commands.help.book.p3")));
        WrittenBookContent content = new WrittenBookContent(
                Filterable.passThrough("Better Minecraft"),
                BOOK_AUTHOR,
                0,
                pages,
                true);
        stack.set(DataComponents.WRITTEN_BOOK_CONTENT, content);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.help.book.received"), true);
        return 1;
    }
}
