package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import pers.solid.ecmd.predicate.nbt.NbtPredicate;

import java.util.concurrent.CompletableFuture;

public enum NbtPredicateArgumentType implements ArgumentType<NbtPredicate> {
  COMPOUND(true), ELEMENT(false);

  private final boolean onlyCompounds;

  NbtPredicateArgumentType(boolean onlyCompounds) {
    this.onlyCompounds = onlyCompounds;
  }

  public static NbtPredicate getNbtPredicate(CommandContext<?> context, String name) {
    return context.getArgument(name, NbtPredicate.class);
  }

  @Override
  public NbtPredicate parse(StringReader reader) throws CommandSyntaxException {
    final NbtPredicateSuggestedParser parser = new NbtPredicateSuggestedParser(reader);
    return onlyCompounds ? parser.parseCompound(false, false) : parser.parsePredicate(false, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final NbtPredicateSuggestedParser parser = new NbtPredicateSuggestedParser(stringReader);
    try {
      if (onlyCompounds) {
        parser.parseCompound(false, false);
      } else {
        parser.parsePredicate(false, false);
      }
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parser.buildSuggestions(context, builderOffset);
  }
}
