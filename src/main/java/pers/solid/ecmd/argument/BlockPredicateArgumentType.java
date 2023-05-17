package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import pers.solid.ecmd.predicate.block.BlockPredicate;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * @see net.minecraft.command.argument.BlockPredicateArgumentType
 */
public record BlockPredicateArgumentType(CommandRegistryAccess commandRegistryAccess) implements ArgumentType<BlockPredicate> {
  public static BlockPredicateArgumentType blockPredicate(CommandRegistryAccess commandRegistryAccess) {
    return new BlockPredicateArgumentType(commandRegistryAccess);
  }

  public static BlockPredicate getBlockPredicate(CommandContext<?> context, String name) {
    return context.getArgument(name, BlockPredicate.class);
  }

  @Override
  public BlockPredicate parse(StringReader reader) throws CommandSyntaxException {
    return BlockPredicate.parse(new SuggestedParser(commandRegistryAccess, reader));
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final SuggestedParser parser = new SuggestedParser(commandRegistryAccess, stringReader);
    try {
      BlockPredicate.parse(parser);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    for (BiConsumer<SuggestionsBuilder, CommandContext<?>> suggestion : parser.suggestions) {
      suggestion.accept(builderOffset, context);
    }
    return builderOffset.buildFuture();
  }
}
