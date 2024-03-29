package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import pers.solid.ecmd.argument.SuggestedParser;

/**
 * 解析函数形式的内容的解析器。实现时，需要指定函数名称以及函数内各个参数的解析方式，然后得出一个解析结果。当未解析到此函数时，解析会返回 {@code null}。
 */
public interface FunctionParamsParser<T> extends Parser<T> {
  Dynamic2CommandExceptionType PARAMS_TOO_FEW = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.param_too_few", a, b));
  Dynamic2CommandExceptionType PARAMS_TOO_MANY = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.param_too_many", a, b));

  /**
   * 最小参数数量。解析过程中，如果参数数量过少，则抛出错误。
   */
  @Contract(pure = true)
  default int minParamsCount() {
    return 0;
  }

  /**
   * 最大参数数量。解析过程中，如果参数数量过多，则抛出错误。
   */
  @Contract(pure = true)
  default int maxParamsCount() {
    return Integer.MAX_VALUE;
  }

  @Override
  default T parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly, boolean allowSparse) throws CommandSyntaxException {
    if (!(parser.reader.canRead() && parser.reader.peek() == '(')) {
      return null;
    }
    return parseAfterLeftParenthesis(commandRegistryAccess, parser, suggestionsOnly);
  }

  @MustBeInvokedByOverriders
  default T parseAfterLeftParenthesis(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    parser.reader.skip();
    // after the left parentheses
    parser.reader.skipWhitespace();

    int paramsCount = 0;

    // when allows zero params, deal with empty
    if (paramsCount >= minParamsCount()) {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest(")");
        }
      });
    }
    if (parser.reader.canRead() && parser.reader.peek() == ')') {
      if (paramsCount >= minParamsCount()) {
        // In this case, the parameters are empty
        parser.reader.skip();
        parser.suggestionProviders.clear();
        return getParseResult(commandRegistryAccess, parser);
      } else {
        throw PARAMS_TOO_FEW.createWithContext(parser.reader, paramsCount, minParamsCount());
      }
    }
    while (true) {
      parser.suggestionProviders.clear();
      parseParameter(commandRegistryAccess, parser, paramsCount, suggestionsOnly);
      paramsCount++;
      parser.reader.skipWhitespace();
      final int finalParamsCount = paramsCount;
      parser.suggestionProviders.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          if (finalParamsCount < maxParamsCount()) {
            suggestionsBuilder.suggest(",");
          }
          if (finalParamsCount >= minParamsCount()) {
            suggestionsBuilder.suggest(")");
          }
        }
      });
      // end of an expression, except a comma or right parentheses
      if (!parser.reader.canRead()) {
        if (paramsCount < minParamsCount()) {
          // params not enough, suggest comma
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, ",");
        } else if (paramsCount < maxParamsCount()) {
          // params enough but not full, suggest both
          throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.createWithContext(parser.reader, ",", ")");
        } else {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, ")");
        }
      } else if (parser.reader.peek() == ',') {
        if (paramsCount >= maxParamsCount()) {
          throw PARAMS_TOO_MANY.createWithContext(parser.reader, paramsCount + 1, maxParamsCount());
        }
        parser.reader.skip();
        parser.reader.skipWhitespace();
        parser.suggestionProviders.clear();
      } else if (parser.reader.peek() == ')') {
        if (paramsCount < minParamsCount()) {
          throw PARAMS_TOO_FEW.createWithContext(parser.reader, paramsCount, minParamsCount());
        }
        parser.reader.skip();
        parser.suggestionProviders.clear();
        break;
      } else {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
      }
    }
    return getParseResult(commandRegistryAccess, parser);
  }

  /**
   * 在完成所有参数的解析后，返回结果。通常在此接口的实现过程中，解析参数时会设置字段的一些值，此方法则使用字段中的值。
   */
  T getParseResult(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser) throws CommandSyntaxException;

  /**
   * 解析特定位置的参数。实现时需要覆盖此方法以实现对具体各参数的解析。
   *
   * @param paramIndex 参数的位置。例如，解析第一个参数时，{@code paramIndex} 为 0。
   */
  void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException;

  /**
   * 指定函数名称，从而让对象知晓是在解析的函数名称。通霄来说，在函数命令是由 {@link FunctionsParser} 解析的，解析后分配各自的 {@link FunctionsParser}。有时抛出的异常的信息中会使用到函数名称。如果不需要使用，可以不实现此方法。
   */
  default void setFunctionName(String functionName) {
  }

  /**
   * 指定函数名称前的 cursor 的位置，从而在特定情况下抛出的异常能够指出出错的函数语法的位置。
   */
  default void setCursorBeforeFunctionName(int cursorBeforeFunctionName) {
  }
}
