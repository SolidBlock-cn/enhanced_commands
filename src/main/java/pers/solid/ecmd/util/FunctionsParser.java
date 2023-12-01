package pers.solid.ecmd.util;

import com.google.common.base.Functions;
import com.google.common.base.Supplier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import net.minecraft.util.Nullables;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.mixin.CommandSyntaxExceptionExtension;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FunctionsParser<T> implements Parser<T> {
  private final Set<String> functions;
  private final Function<String, FunctionParamsParser<? extends T>> parserFactory;
  private final Function<String, Text> tooltipProvider;

  public FunctionsParser(Set<String> functions, Function<String, FunctionParamsParser<? extends T>> parserFactory, Function<String, Text> tooltipProvider) {
    this.functions = functions;
    this.parserFactory = parserFactory;
    this.tooltipProvider = tooltipProvider;
  }

  public FunctionsParser(Map<String, Supplier<FunctionParamsParser<? extends T>>> functions, Map<String, Text> functionNames) {
    this.functions = functions.keySet();
    this.parserFactory = s -> Nullables.map(functions.get(s), Supplier::get);
    this.tooltipProvider = Functions.forMap(functionNames, null);
  }

  @Override
  public T parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException {
    final StringReader reader = parser.reader;
    final int cursorOnStart = reader.getCursor();
    parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestMatchingWithTooltip(functions, s -> s + "(", tooltipProvider::apply, suggestionsBuilder));
    final String unquotedString = reader.readUnquotedString();
    if (!unquotedString.isEmpty() && reader.canRead() && reader.peek() == '(') {
      final FunctionParamsParser<? extends T> functionParamsParser = parserFactory.apply(unquotedString);
      if (functionParamsParser != null) {
        functionParamsParser.setFunctionName(unquotedString);
        functionParamsParser.setCursorBeforeFunctionName(cursorOnStart);
        return functionParamsParser.parseAfterLeftParenthesis(commandRegistryAccess, parser, suggestionsOnly);
      } else {
        final int cursorAfterFunctionName = reader.getCursor();
        reader.setCursor(cursorOnStart);
        throw CommandSyntaxExceptionExtension.withCursorEnd(ModCommandExceptionTypes.UNKNOWN_FUNCTION.createWithContext(reader, unquotedString), cursorAfterFunctionName);
      }
    } else {
      return null;
    }
  }
}
