package com.soleapixel.bmcmod.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;

/**
 * Comme {@link EntityArgument#entity()} (une seule entité), mais refuse les sélecteurs « joueurs seulement »
 * ({@code @a}, {@code @p}, {@code @r}) et les pseudos joueur — préférer {@code @e} / {@code @n} ou un UUID (éviter {@code @s}
 * si l’exécuteur est un joueur : tu te sélectionnes comme « mob »).
 */
public final class MorphTemplateEntityArgument implements ArgumentType<EntitySelector> {
    private static final Collection<String> EXAMPLES = Arrays.asList("@e[type=minecraft:pig,limit=1,sort=nearest]", "@n");

    public static final SimpleCommandExceptionType ERROR_PLAYER_SELECTOR_NOT_ALLOWED = new SimpleCommandExceptionType(
            Component.translatable("bmcmod.commands.metamorph.no_player_selector_for_template"));

    private MorphTemplateEntityArgument() {
    }

    public static MorphTemplateEntityArgument mobTemplate() {
        return new MorphTemplateEntityArgument();
    }

    @Override
    public EntitySelector parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();
        EntitySelector selector = EntityArgument.entity().parse(reader);
        if (!selector.includesEntities()) {
            reader.setCursor(start);
            throw ERROR_PLAYER_SELECTOR_NOT_ALLOWED.createWithContext(reader);
        }
        return selector;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return EntityArgument.entity().listSuggestions(context, builder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
