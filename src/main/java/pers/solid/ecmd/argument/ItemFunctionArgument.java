package pers.solid.ecmd.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import pers.solid.ecmd.item.function.ItemFunction;
import pers.solid.ecmd.item.function.ItemFunctionParser;
import pers.solid.ecmd.parse.ParseContext;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ItemFunctionArgument implements ArgumentType<ItemFunction> {
  private static final List<String> EXAMPLES = List.of("diamond", "#planks", "diamond_sword[enchantments={sharpness:1}]");
  private final CommandBuildContext commandBuildContext;

  public ItemFunctionArgument(CommandBuildContext commandBuildContext) {
    this.commandBuildContext = commandBuildContext;
  }

  public static ItemFunctionArgument itemFunction(CommandBuildContext context) {
    return new ItemFunctionArgument(context);
  }

  public static ItemFunction getItemFunction(CommandContext<CommandSourceStack> context, String name) {
    return context.getArgument(name, ItemFunction.class);
  }

  @Override
  public ItemFunction parse(StringReader stringReader) throws CommandSyntaxException {
    return ItemFunctionParser.parse(new ParseContext<>(commandBuildContext, stringReader, false, false));
  }

  @Override
  public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
    StringReader stringReader = new StringReader(builder.getInput());
    stringReader.setCursor(builder.getStart());
    final ParseContext<S> parseContext = new ParseContext<>(commandBuildContext, stringReader, true, false);
    try {
      ItemFunctionParser.parse(parseContext);
    } catch (CommandSyntaxException ignore) {
    }
    SuggestionsBuilder builderOffset = builder.createOffset(stringReader.getCursor());
    return parseContext.buildSuggestions(context, builderOffset);
  }

  @Override
  public Collection<String> getExamples() {
    return EXAMPLES;
  }
}
