package pers.solid.ecmd.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import pers.solid.ecmd.argument.SuggestedParser;

/**
 * 解析函数形式的内容的解析器。实现时，需要指定函数名称以及函数内各个参数的解析方式，然后得出一个解析结果。当未解析到此函数时，解析会返回 {@code null}。
 */
public interface FunctionLikeParser<T> {
  Dynamic2CommandExceptionType PARAMS_TOO_FEW = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.param_too_few", a, b));
  Dynamic2CommandExceptionType PARAMS_TOO_MANY = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.param_too_many", a, b));

  /**
   * 函数形式的名称，即括号前的内容。通常应该是常量。
   */
  @Contract(pure = true)
  @NotNull
  String functionName();

  /**
   * 在显示建议时，为此函数名称提供建议时的提示文本。
   */
  @Contract(pure = true)
  Text tooltip();

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

  default T parse(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    final String name = functionName();
    parser.suggestionProviders.add((context, suggestionsBuilder) -> ParsingUtil.suggestString(name + "(", tooltip(), suggestionsBuilder));
    final int cursorBeforeUnion = parser.reader.getCursor();
    final String s = parser.reader.readUnquotedString();
    if (!(s.equals(name) && parser.reader.canRead() && parser.reader.peek() == '(')) {
      parser.reader.setCursor(cursorBeforeUnion);
      return null;
    }
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
        return getParseResult(parser);
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
    return getParseResult(parser);
  }

  T getParseResult(SuggestedParser parser) throws CommandSyntaxException;

  void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException;
}
