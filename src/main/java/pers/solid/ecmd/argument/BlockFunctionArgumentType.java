package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import pers.solid.ecmd.function.block.BlockFunction;
import pers.solid.ecmd.function.block.BlockFunctionArgument;

import java.util.concurrent.CompletableFuture;

public record BlockFunctionArgumentType(CommandRegistryAccess registryAccess) implements ArgumentType<BlockFunctionArgument> {
  public static BlockFunctionArgumentType blockFunction(CommandRegistryAccess registryAccess) {
    return new BlockFunctionArgumentType(registryAccess);
  }

  public static BlockFunction getBlockFunction(CommandContext<ServerCommandSource> context, String name) throws CommandSyntaxException {
    return context.getArgument(name, BlockFunctionArgument.class).apply(context.getSource());
  }

  @Override
  public BlockFunctionArgument parse(StringReader reader) throws CommandSyntaxException {
    return BlockFunctionArgument.parse(registryAccess, new SuggestedParser(reader), false, false);
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final SuggestedParser parser = new SuggestedParser(stringReader);
    try {
      BlockFunctionArgument.parse(registryAccess, parser, true, false);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parser.buildSuggestions(context, builderOffset);
  }
}
