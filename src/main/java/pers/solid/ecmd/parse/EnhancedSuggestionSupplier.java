package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.FieldsAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import net.minecraft.util.parsing.packrat.commands.Grammar;
import pers.solid.ecmd.util.mixin.MixinShared;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * @see ParseContext#buildSuggestions(CommandContext, SuggestionsBuilder)
 * @see Grammar#parseForSuggestions(SuggestionsBuilder)
 */
@MethodsReturnNonnullByDefault
@FieldsAreNonnullByDefault
public record EnhancedSuggestionSupplier<S>(SuggestionProvider<S> suggestionProvider) implements SuggestionSupplier<StringReader> {
  @Override
  public Stream<String> possibleValues(ParseState parseState) {
    return Stream.empty();
  }

  @SuppressWarnings("unchecked")
  public CompletableFuture<Suggestions> forceGetSuggestionsUnchecked(SuggestionsBuilder builder) throws CommandSyntaxException {
    return suggestionProvider().getSuggestions((CommandContext<S>) MixinShared.commandContextForPackrat, builder);
  }
}
