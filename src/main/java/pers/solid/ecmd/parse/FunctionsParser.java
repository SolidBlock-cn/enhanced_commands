package pers.solid.ecmd.parse;

import com.google.common.base.Functions;
import com.google.common.base.Supplier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.Optionull;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FunctionsParser<T> implements Parser<T> {
  private final Set<String> functions;
  private final Function<@NotNull String, @Nullable FunctionContentParser<? extends T>> parserFactory;
  private final Function<@NotNull String, @Nullable Component> tooltipProvider;

  public FunctionsParser(Set<String> functions, Function<String, @Nullable FunctionContentParser<? extends T>> parserFactory, Function<String, @Nullable Component> tooltipProvider) {
    this.functions = functions;
    this.parserFactory = parserFactory;
    this.tooltipProvider = tooltipProvider;
  }

  public FunctionsParser(Map<String, Supplier<FunctionContentParser<? extends T>>> functions, Map<String, Component> functionNames) {
    this.functions = functions.keySet();
    this.parserFactory = s -> Optionull.map(functions.get(s), Supplier::get);
    this.tooltipProvider = Functions.forMap(functionNames, null);
  }

  @Override
  public T parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeFunctionName = reader.getCursor();
    parseContext.addSuggestion((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(functions, suggestionsBuilder.createOffset(cursorBeforeFunctionName), s -> s + "(", tooltipProvider::apply));
    final String unquotedString = reader.readUnquotedString();
    if (!unquotedString.isEmpty() && reader.canRead() && reader.peek() == '(') {
      final int cursorAfterFunctionName = reader.getCursor();
      final FunctionContentParser<? extends T> functionContentParser = parserFactory.apply(unquotedString);
      if (functionContentParser != null) {
        functionContentParser.onBeforeParentheses(unquotedString, cursorBeforeFunctionName, cursorAfterFunctionName);
        return parseParenthesis(functionContentParser, parseContext);
      } else {
        reader.setCursor(cursorBeforeFunctionName);
        throw EnhancedCommandSyntaxException.withCursorEnd(EnhancedCommandsCommandExceptionTypes.UNKNOWN_FUNCTION.createWithContext(reader, unquotedString), cursorAfterFunctionName);
      }
    } else {
      return null;
    }
  }

  public T parseParenthesis(FunctionContentParser<? extends T> parser, ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    if (!(reader.canRead() && reader.peek() == '(')) {
      return null;
    }
    reader.skip();
    parser.parseWithinParenthesis(parseContext.withAllowSparse(true));
    reader.skipWhitespace();
    parseContext.addSuggestion((context, builder) -> {
      if (builder.getRemaining().isEmpty()) {
        builder.suggest(")");
      }
      return builder.buildFuture();
    });
    if (reader.canRead() && reader.peek() == ')') {
      reader.skip();
      parser.onAfterParentheses(reader.getCursor());
      parseContext.clearSuggestion();
      return parser.getParseResult(parseContext);
    }
    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, ')');
  }
}
