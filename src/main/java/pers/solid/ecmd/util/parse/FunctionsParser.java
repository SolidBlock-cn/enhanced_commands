package pers.solid.ecmd.util.parse;

import com.google.common.base.Functions;
import com.google.common.base.Supplier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Nullables;
import pers.solid.ecmd.mixins.ext.CommandSyntaxExceptionExtension;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FunctionsParser<T> implements Parser<T> {
  private final Set<String> functions;
  private final Function<String, FunctionLikeParser<? extends T>> parserFactory;
  private final Function<String, Text> tooltipProvider;

  public FunctionsParser(Set<String> functions, Function<String, FunctionLikeParser<? extends T>> parserFactory, Function<String, Text> tooltipProvider) {
    this.functions = functions;
    this.parserFactory = parserFactory;
    this.tooltipProvider = tooltipProvider;
  }

  public FunctionsParser(Map<String, Supplier<FunctionLikeParser<? extends T>>> functions, Map<String, Text> functionNames) {
    this.functions = functions.keySet();
    this.parserFactory = s -> Nullables.map(functions.get(s), Supplier::get);
    this.tooltipProvider = Functions.forMap(functionNames, null);
  }

  @Override
  public T parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorOnStart = reader.getCursor();
    parseContext.addSuggestion((context, suggestionsBuilder) -> CommandSource.suggestMatching(functions, suggestionsBuilder.createOffset(cursorOnStart), s -> s + "(", tooltipProvider::apply));
    final String unquotedString = reader.readUnquotedString();
    if (!unquotedString.isEmpty() && reader.canRead() && reader.peek() == '(') {
      final FunctionLikeParser<? extends T> functionParamsParser = parserFactory.apply(unquotedString);
      if (functionParamsParser != null) {
        functionParamsParser.setFunctionName(unquotedString);
        functionParamsParser.setCursorBeforeFunctionName(cursorOnStart);
        reader.skip();
        return functionParamsParser.parseAfterLeftParenthesis(parseContext);
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
