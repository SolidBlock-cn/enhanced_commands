package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import pers.solid.ecmd.function.block.BlockFunction;

import java.util.concurrent.CompletableFuture;

public record BlockFunctionArgumentType(CommandRegistryAccess commandRegistryAccess) implements ArgumentType<BlockFunction> {
  public static BlockFunctionArgumentType blockFunction(CommandRegistryAccess commandRegistryAccess) {
    return new BlockFunctionArgumentType(commandRegistryAccess);
  }

  public static BlockFunction getBlockFunction(CommandContext<?> context, String name) {
    return context.getArgument(name, BlockFunction.class);
  }

  @Override
  public BlockFunction parse(StringReader reader) throws CommandSyntaxException {
    return BlockFunction.parse(commandRegistryAccess, new SuggestedParser(reader), false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final SuggestedParser parser = new SuggestedParser(stringReader);
    try {
      BlockFunction.parse(commandRegistryAccess, parser, true);
    } catch (
        CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parser.buildSuggestions(context, builderOffset);
  }
}
