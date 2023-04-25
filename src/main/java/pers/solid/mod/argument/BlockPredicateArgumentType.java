package pers.solid.mod.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import pers.solid.mod.predicate.block.BlockPredicate;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public record BlockPredicateArgumentType(CommandRegistryAccess commandRegistryAccess) implements ArgumentType<BlockPredicate> {
  @Override
  public BlockPredicate parse(StringReader reader) throws CommandSyntaxException {
    return BlockPredicate.parse(new BlockPredicateArgumentParser(commandRegistryAccess, reader));
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final BlockPredicateArgumentParser parser = new BlockPredicateArgumentParser(commandRegistryAccess, stringReader);
    try {
      BlockPredicate.parse(parser);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    for (Consumer<SuggestionsBuilder> suggestion : parser.suggestions) {
      suggestion.accept(builderOffset);
    }
    return builderOffset.buildFuture();
  }
}
