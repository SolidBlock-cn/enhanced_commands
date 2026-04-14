package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import pers.solid.ecmd.block.function.BlockFunction;
import pers.solid.ecmd.parse.ParseContext;

import java.util.concurrent.CompletableFuture;

public record BlockFunctionArgument(CommandBuildContext commandBuildContext) implements ArgumentType<BlockFunction> {
  public static BlockFunctionArgument blockFunction(CommandBuildContext commandBuildContext) {
    return new BlockFunctionArgument(commandBuildContext);
  }

  public static BlockFunction getBlockFunction(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, BlockFunction.class);
  }

  @Override
  public BlockFunction parse(StringReader reader) throws CommandSyntaxException {
    return BlockFunction.parse(new ParseContext<>(commandBuildContext, reader, false, false));
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(commandBuildContext, stringReader, true, false);
    try {
      BlockFunction.parse(parseContext);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parseContext.buildSuggestions(context, builderOffset);
  }
}
