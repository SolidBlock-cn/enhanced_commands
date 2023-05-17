package pers.solid.ecmd.util;

import com.google.common.base.Functions;
import com.google.common.base.Suppliers;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.Direction;
import pers.solid.ecmd.EnhancedCommands;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SuggestionUtil {
  private SuggestionUtil() {
  }

  public static boolean shouldSuggest(String remaining, String candidate) {
    return CommandSource.shouldSuggest(remaining, candidate);
  }

  public static <T> CompletableFuture<Suggestions> suggestMatchingWithTooltip(Iterable<T> iterable, Function<T, String> suggestion, Function<T, Message> tooltip, SuggestionsBuilder builder) {
    final String remaining = builder.getRemainingLowerCase();
    for (T t : iterable) {
      final String candidate = suggestion.apply(t);
      if (shouldSuggest(remaining, candidate)) {
        builder.suggest(candidate, tooltip.apply(t));
      }
    }
    return builder.buildFuture();
  }

  public static <T extends StringIdentifiable> CompletableFuture<Suggestions> suggestMatchingEnumWithTooltip(Iterable<T> enumIterable, Function<T, Message> tooltip, SuggestionsBuilder builder) {
    return suggestMatchingWithTooltip(enumIterable, StringIdentifiable::asString, tooltip, builder);
  }

  public static CompletableFuture<Suggestions> suggestMatchingStringWithTooltip(Iterable<String> candidates, Function<String, Message> tooltip, SuggestionsBuilder builder) {
    return suggestMatchingWithTooltip(candidates, Functions.identity(), tooltip, builder);
  }

  public static CompletableFuture<Suggestions> suggestDirections(Iterable<Direction> directions, SuggestionsBuilder builder) {
    return suggestMatchingEnumWithTooltip(directions, EnhancedCommands::wrapDirection, builder);
  }

  public static CompletableFuture<Suggestions> suggestDirections(SuggestionsBuilder builder) {
    return suggestDirections(Arrays.asList(Direction.values()), builder);
  }

  public static void suggestString(String candidate, Supplier<Message> tooltip, SuggestionsBuilder builder) {
    if (shouldSuggest(builder.getRemainingLowerCase(), candidate.toLowerCase())) {
      builder.suggest(candidate, tooltip.get());
    }
  }

  public static void suggestString(String candidate, Message tooltip, SuggestionsBuilder builder) {
    suggestString(candidate, Suppliers.ofInstance(tooltip), builder);
  }
}
