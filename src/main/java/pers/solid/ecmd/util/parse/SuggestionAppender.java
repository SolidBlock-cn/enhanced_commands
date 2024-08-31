package pers.solid.ecmd.util.parse;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface SuggestionAppender {
  void accept(CommandContext<?> context, SuggestionsBuilder suggestionsBuilder);

  @FunctionalInterface
  interface Offset extends SuggestionAppender {
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
