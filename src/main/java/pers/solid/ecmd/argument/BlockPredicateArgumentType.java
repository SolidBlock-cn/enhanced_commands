package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.predicate.block.BlockPredicate;
import pers.solid.ecmd.parse.ParseContext;

import java.util.concurrent.CompletableFuture;

/**
 * @see net.minecraft.command.argument.BlockPredicateArgumentType
 */
public record BlockPredicateArgumentType(CommandRegistryAccess registryAccess) implements ArgumentType<BlockPredicate> {
  public static BlockPredicateArgumentType blockPredicate(CommandRegistryAccess registryAccess) {
    return new BlockPredicateArgumentType(registryAccess);
  }

  public static BlockPredicate getBlockPredicate(CommandContext<ServerCommandSource> context, String name) {
    return context.getArgument(name, BlockPredicate.class);
  }

  @Override
  public BlockPredicate parse(StringReader reader) throws CommandSyntaxException {
    return BlockPredicate.parse(new ParseContext<>(registryAccess, reader, false, false));
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(registryAccess, stringReader, true, false);
    try {
      BlockPredicate.parse(parseContext);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parseContext.buildSuggestions(context, builderOffset);
  }
}
