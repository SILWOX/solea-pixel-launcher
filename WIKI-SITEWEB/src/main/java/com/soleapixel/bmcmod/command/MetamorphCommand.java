package com.soleapixel.bmcmod.command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.soleapixel.bmcmod.BmcMod;
import com.soleapixel.bmcmod.morph.MorphBossBlacklist;
import com.soleapixel.bmcmod.morph.MorphCrystalServer;
import com.soleapixel.bmcmod.morph.MorphSoulSanitizer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /metamorph set <joueurs> <type>} — ex. {@code /metamorph set @p pig} ou {@code set @a minecraft:zombie}.
 * {@code /metamorph set <joueurs> <entité>} — modèle monde : {@code @e[type=...,limit=1]} (une seule entité non joueur).
 * Seul l’ordre « cibles joueurs d’abord » est enregistré : évite les ambiguïtés Brigadier quand plusieurs branches
 * acceptent la même entrée.
 * {@code /metamorph reset [joueurs]} — fin de métamorphose (joueurs uniquement).
 */
@EventBusSubscriber(modid = BmcMod.MODID)
public final class MetamorphCommand {
    private MetamorphCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> resetBranch = Commands.literal("reset")
                .executes(ctx -> runReset(ctx.getSource(), List.of(ctx.getSource().getPlayerOrException())))
                .then(Commands.argument("joueurs", EntityArgument.players())
                        .suggests(MetamorphCommand::suggestJoueursOnly)
                        .executes(ctx -> runReset(ctx.getSource(), EntityArgument.getPlayers(ctx, "joueurs"))));
        // Un seul nœud « joueurs » avec deux enfants : évite l’écrasement Brigadier (même nom de nœud frère).
        ArgumentBuilder<CommandSourceStack, ?> setPlayersThenTarget = Commands.argument("joueurs", EntityArgument.players())
                .suggests(MetamorphCommand::suggestJoueursOnly)
                .then(Commands.argument("mobType", ResourceLocationArgument.id())
                        .suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
                        .executes(ctx -> runSetFromMobType(
                                ctx.getSource(),
                                EntityArgument.getPlayers(ctx, "joueurs"),
                                ResourceLocationArgument.getId(ctx, "mobType"))))
                .then(Commands.argument("entite", MorphTemplateEntityArgument.mobTemplate())
                        .suggests(MetamorphCommand::suggestEntiteOnly)
                        .executes(ctx -> runSet(
                                ctx.getSource(),
                                EntityArgument.getPlayers(ctx, "joueurs"),
                                EntityArgument.getEntity(ctx, "entite"))));
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("metamorph")
                .requires(src -> src.hasPermission(4))
                .then(resetBranch)
                .then(Commands.literal("set")
                        .then(setPlayersThenTarget));
        d.register(root);
    }

    /** 1er argument : pas de {@code @e} / {@code @n} dans les suggestions (réservé au 2e argument). */
    private static CompletableFuture<Suggestions> suggestJoueursOnly(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return EntityArgument.players().listSuggestions(ctx, builder).thenApply(MetamorphCommand::withoutEntitySelectors);
    }

    /** 2e argument : pas de {@code @a} / {@code @p} / {@code @r} dans les suggestions (réservé aux joueurs). */
    private static CompletableFuture<Suggestions> suggestEntiteOnly(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return EntityArgument.entity().listSuggestions(ctx, builder).thenApply(MetamorphCommand::withoutPlayerSelectors);
    }

    private static Suggestions withoutEntitySelectors(Suggestions s) {
        List<Suggestion> out = new ArrayList<>();
        for (Suggestion su : s.getList()) {
            String t = su.getText();
            if (t.startsWith("@e") || t.startsWith("@n")) {
                continue;
            }
            out.add(su);
        }
        return new Suggestions(s.getRange(), out);
    }

    private static Suggestions withoutPlayerSelectors(Suggestions s) {
        List<Suggestion> out = new ArrayList<>();
        for (Suggestion su : s.getList()) {
            String t = su.getText();
            // @s en 2e argument choisit souvent le joueur exécuteur → erreur « pas un joueur comme mob » ; on pousse @e / @n.
            if (t.startsWith("@a") || t.startsWith("@p") || t.startsWith("@r") || t.equals("@s")) {
                continue;
            }
            out.add(su);
        }
        return new Suggestions(s.getRange(), out);
    }

    private static int runSetFromMobType(CommandSourceStack source, Collection<ServerPlayer> targets, ResourceLocation id) {
        if (!(source.getLevel() instanceof ServerLevel serverLevel)) {
            return 0;
        }
        Optional<Holder.Reference<EntityType<?>>> holderOpt =
                source.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).get(ResourceKey.create(Registries.ENTITY_TYPE, id));
        if (holderOpt.isEmpty()) {
            source.sendFailure(Component.translatable("bmcmod.commands.metamorph.unknown_type"));
            return 0;
        }
        EntityType<?> type = holderOpt.get().value();
        if (type == EntityType.PLAYER) {
            source.sendFailure(Component.translatable("bmcmod.commands.metamorph.no_player_mob"));
            return 0;
        }
        // Minecraft 1.21+: EntityType#getBaseClass() always returns Entity.class — cannot use for LivingEntity checks.
        if (!isLivingMorphTemplate(type, serverLevel)) {
            source.sendFailure(Component.translatable("bmcmod.commands.metamorph.not_living_entity", id.toString()));
            return 0;
        }
        if (type == EntityType.WITHER || type == EntityType.ENDER_DRAGON || type == EntityType.WARDEN) {
            source.sendFailure(Component.translatable("bmcmod.commands.metamorph.boss_blocked"));
            return 0;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        CompoundTag soul = MorphSoulSanitizer.sanitize(tag);
        if (!soul.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            source.sendFailure(Component.translatable("bmcmod.commands.metamorph.unknown_type"));
            return 0;
        }
        String idMsg = soul.getString("id");
        int n = 0;
        for (ServerPlayer target : targets) {
            MorphCrystalServer.applyMorphFromCommand(target, soul.copy());
            n++;
        }
        final int applied = n;
        final String idMsgFinal = idMsg;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.metamorph.success", applied, idMsgFinal), true);
        return n;
    }

    private static int runSet(CommandSourceStack source, Collection<ServerPlayer> targets, Entity mob) {
        if (!(mob instanceof LivingEntity living)) {
            source.sendFailure(Component.translatable(
                    "bmcmod.commands.metamorph.not_living_entity", mob.getName().getString()));
            return 0;
        }
        if (mob instanceof Player) {
            source.sendFailure(Component.translatable("bmcmod.commands.metamorph.no_player_mob"));
            return 0;
        }
        if (MorphBossBlacklist.isBoss(living)) {
            source.sendFailure(Component.translatable("bmcmod.commands.metamorph.boss_blocked"));
            return 0;
        }
        String encodeId = living.getEncodeId();
        if (encodeId == null) {
            source.sendFailure(Component.translatable("bmcmod.commands.metamorph.unknown_type"));
            return 0;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("id", encodeId);
        living.saveWithoutId(tag);
        CompoundTag soul = MorphSoulSanitizer.sanitize(tag);
        if (!soul.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            source.sendFailure(Component.translatable("bmcmod.commands.metamorph.unknown_type"));
            return 0;
        }
        String idMsg = soul.getString("id");
        int n = 0;
        for (ServerPlayer target : targets) {
            MorphCrystalServer.applyMorphFromCommand(target, soul.copy());
            n++;
        }
        final int applied = n;
        final String idMsgFinal = idMsg;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.metamorph.success", applied, idMsgFinal), true);
        return n;
    }

    private static int runReset(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int n = 0;
        for (ServerPlayer target : targets) {
            MorphCrystalServer.forceEndMorph(target, true);
            n++;
        }
        final int count = n;
        source.sendSuccess(() -> Component.translatable("bmcmod.commands.metamorph.reset.success", count), true);
        return count;
    }

    /**
     * Whether this entity type can be used as a morph template (must be a {@link LivingEntity}, not the player).
     * Uses a short-lived probe instance because {@link EntityType#getBaseClass()} no longer reflects the runtime class.
     */
    private static boolean isLivingMorphTemplate(EntityType<?> type, ServerLevel level) {
        final Entity probe;
        try {
            probe = type.create(level);
        } catch (Throwable t) {
            return false;
        }
        if (probe == null) {
            return false;
        }
        try {
            return probe instanceof LivingEntity;
        } finally {
            probe.discard();
        }
    }
}
