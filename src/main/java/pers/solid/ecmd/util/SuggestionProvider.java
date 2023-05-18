package pers.solid.ecmd.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

@FunctionalInterface
public interface SuggestionProvider extends BiConsumer<CommandContext<?>, SuggestionsBuilder> {
  @FunctionalInterface
  interface Modifying extends SuggestionProvider, BiFunction<CommandContext<?>, SuggestionsBuilder, CompletableFuture<Suggestions>> {
    @Override
    default void accept(CommandContext<?> context, SuggestionsBuilder builder) {
      apply(context, builder);
    }
  }

  static Modifying modifying(Modifying value) {
    return value;
  }
}
