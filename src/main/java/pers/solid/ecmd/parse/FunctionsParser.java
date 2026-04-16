package pers.solid.ecmd.parse;

import com.google.common.base.Suppliers;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pers.solid.ecmd.util.EnhancedCommandSyntaxException;
import pers.solid.ecmd.util.EnhancedCommandsCommandExceptionTypes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface FunctionsParser<T> extends Parser<T> {
  static <T> FunctionsParser<T> create() {
    return new Impl<>(new LinkedHashMap<>(), new HashMap<>());
  }

  @Override
  default @Nullable T parse(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    final int cursorBeforeFunctionName = reader.getCursor();
    final Iterable<String> functionNames = functionNames();
    parseContext.addSuggestion((context, suggestionsBuilder) -> SharedSuggestionProvider.suggest(functionNames, suggestionsBuilder.createOffset(cursorBeforeFunctionName), s -> s + "(", this::getTooltipForFunction));
    final String unquotedString = reader.readUnquotedString();
    if (!unquotedString.isEmpty() && reader.canRead() && reader.peek() == '(') {
      final int cursorAfterFunctionName = reader.getCursor();
      final FunctionContentParser<? extends T> functionContentParser = getParserForFunction(unquotedString);
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

  Iterable<String> functionNames();

  @Nullable FunctionContentParser<? extends T> getParserForFunction(String functionName);

  @Nullable Component getTooltipForFunction(String functionName);

  void register(String functionName, @Nullable Supplier<@Nullable Component> tooltip, @Nullable Supplier<@Nullable FunctionContentParser<? extends T>> parser);

  default void register(String functionName, @Nullable Component tooltip, @Nullable Supplier<@Nullable FunctionContentParser<? extends T>> parser) {
    register(functionName, tooltip == null ? null : Suppliers.ofInstance(tooltip), parser);
  }

  default @Nullable T parseParenthesis(FunctionContentParser<? extends T> parser, ParseContext<?> parseContext) throws CommandSyntaxException {
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

  record Impl<T>(Map<String, @Nullable Supplier<@Nullable FunctionContentParser<? extends T>>> parsers, Map<String, Supplier<@Nullable Component>> tooltips) implements FunctionsParser<T> {

    @Override
    public Iterable<String> functionNames() {
      return parsers.keySet();
    }

    @Override
    public @Nullable FunctionContentParser<? extends T> getParserForFunction(String functionName) {
      final Supplier<@Nullable FunctionContentParser<? extends T>> supplier = parsers.get(functionName);
      return supplier == null ? null : supplier.get();
    }

    @Override
    public @Nullable Component getTooltipForFunction(String functionName) {
      return tooltips.get(functionName).get();
    }

    @Override
    public void register(String functionName, @Nullable Supplier<@Nullable Component> tooltip, @Nullable Supplier<@Nullable FunctionContentParser<? extends T>> parser) {
      if (tooltips.containsKey(functionName) || parsers.containsKey(functionName)) {
        throw new IllegalArgumentException("Duplicate function names: " + functionName);
      }
      if (tooltip != null) {
        tooltips.put(functionName, tooltip);
      }
      if (parser != null) {
        parsers.put(functionName, parser);
      }
    }
  }
}
