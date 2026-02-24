package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import pers.solid.ecmd.parse.ParseContext;
import pers.solid.ecmd.predicate.block.BlockPredicate;

import java.util.concurrent.CompletableFuture;

/**
 * @see net.minecraft.commands.arguments.blocks.BlockPredicateArgument
 */
public record BlockPredicateArgument(CommandBuildContext commandBuildContext) implements ArgumentType<BlockPredicate> {
  public static BlockPredicateArgument blockPredicate(CommandBuildContext commandBuildContext) {
    return new BlockPredicateArgument(commandBuildContext);
  }

  public static BlockPredicate getBlockPredicate(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, BlockPredicate.class);
  }

  @Override
  public BlockPredicate parse(StringReader reader) throws CommandSyntaxException {
    return BlockPredicate.parse(new ParseContext<>(commandBuildContext, reader, false, false));
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(commandBuildContext, stringReader, true, false);
    try {
      BlockPredicate.parse(parseContext);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parseContext.buildSuggestions(context, builderOffset);
  }
}
