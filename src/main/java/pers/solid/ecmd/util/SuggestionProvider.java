package pers.solid.ecmd.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface SuggestionProvider {
  void accept(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder);

  @FunctionalInterface
  interface Offset extends SuggestionProvider {
    CompletableFuture<Suggestions> apply(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder);

    @Override
    default void accept(CommandContext<?> context, SuggestionsBuilder builder) {
      apply(context, builder);
    }
  }

  static Offset offset(Offset value) {
    return value;
  }
}
