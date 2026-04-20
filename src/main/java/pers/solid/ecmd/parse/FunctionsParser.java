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

/**
 * <p>此接口用于解析函数式语法（{@code 函数名称(函数内容)}），并提供一系列相关的解析处理机制（包括提供建议、异常处理等）。接口的默认实现见于 {@link Impl}。可以调用 {@code register} 方法以注册函数名称及其解析方式。
 *
 * <p>示例（其中 {@code T} 可以替换为你需要的类型）：
 * <pre>{@code
 * // 创建新的 parser
 * FunctionsParser<T> functionsParser = FunctionsParser.create();
 *
 * // 注册函数名称
 * functionsParser.register("example", Component.literal("example"), () -> ... );  // 此 lambda 返回一个 FunctionContentParser
 *
 * // 进行解析
 * functionsParser.parse(...)
 * }</pre>
 *
 * @param <T> 需要解析的函数式语法所表示的对象的类型。
 * @see #create()
 * @see #parse
 * @see #register(String, Component, Supplier)
 * @see #register(String, Supplier, Supplier)
 */
public interface FunctionsParser<T> extends Parser<T> {
  /**
   * 使用此接口的标准实现，创建一个新的 {@link FunctionsParser} 对象。可以对此解析器注册函数名称及其对应的解析方式。
   */
  static <T> FunctionsParser<T> create() {
    return new Impl<>(new LinkedHashMap<>(), new HashMap<>());
  }

  /**
   * 解析已经注册的函数语法。如果没有发现任何的函数语法，将返回 {@code null}，但 reader 的 cursor 不会还原；如果解析到了函数语法，但是函数名称不识别，会直接抛出异常。
   *
   * @throws CommandSyntaxException 函数名称无效，或者注册的解析器在解析函数内容时出现异常。
   */
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

  /**
   * @return 可识别的函数名称的 iterable（通常是集合），用于在命令中提供建议。
   */
  Iterable<String> functionNames();

  @Nullable FunctionContentParser<? extends T> getParserForFunction(String functionName);

  /**
   * @return 函数名称对应的提示文本，用于在命令中提供建议。
   */
  @Nullable Component getTooltipForFunction(String functionName);

  /**
   * 注册一个函数名称及其对应的提示文本和解析器。
   */
  void register(String functionName, @Nullable Supplier<@Nullable Component> tooltip, @Nullable Supplier<@Nullable FunctionContentParser<? extends T>> parser);

  /**
   * 注册一个函数名称及其对应的提示文本和解析器。
   */
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
