package pers.solid.ecmd.util.parse;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import pers.solid.ecmd.argument.SuggestedParser;
import pers.solid.ecmd.util.ModCommandExceptionTypes;

/**
 * 解析函数形式的内容的解析器。实现时，需要指定函数名称以及函数内各个参数的解析方式，然后得出一个解析结果。当未解析到此函数时，解析会返回 {@code null}。
 */
public interface FunctionParamsParser<T> extends FunctionLikeParser<T> {
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
  default void parseWithinParenthesis(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, boolean suggestionsOnly) throws CommandSyntaxException {
    parser.reader.skip();
    // after the left parentheses
    parser.reader.skipWhitespace();

    int paramsCount = 0;

    // when allows zero params, deal with empty
    if (paramsCount >= minParamsCount()) {
      parser.suggestionProviders.add((context, suggestionsBuilder) -> {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
          suggestionsBuilder.suggest(rightOpenString());
        }
      });
    }
    if (parser.reader.canRead() && parser.reader.peek() == rightOpen()) {
      if (paramsCount >= minParamsCount()) {
        // In this case, the parameters are empty
        return;
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
            suggestionsBuilder.suggest(separatorString());
          }
          if (finalParamsCount >= minParamsCount()) {
            suggestionsBuilder.suggest(rightOpenString());
          }
        }
      });
      // end of an expression, except a comma or right parentheses
      if (!parser.reader.canRead()) {
        if (paramsCount < minParamsCount()) {
          // params not enough, suggest comma
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, separator());
        } else if (paramsCount < maxParamsCount()) {
          // params enough but not full, suggest both
          throw ModCommandExceptionTypes.EXPECTED_2_SYMBOLS.createWithContext(parser.reader, separator(), rightOpen());
        } else {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(parser.reader, rightOpen());
        }
      } else if (parser.reader.peek() == separator()) {
        if (paramsCount >= maxParamsCount()) {
          throw PARAMS_TOO_MANY.createWithContext(parser.reader, paramsCount + 1, maxParamsCount());
        }
        parser.reader.skip();
        parser.reader.skipWhitespace();
        parser.suggestionProviders.clear();
      } else if (parser.reader.peek() == rightOpen()) {
        if (paramsCount < minParamsCount()) {
          throw PARAMS_TOO_FEW.createWithContext(parser.reader, paramsCount, minParamsCount());
        }
        return;
      } else {
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(parser.reader);
      }
    }
  }

  /**
   * 解析特定位置的参数。实现时需要覆盖此方法以实现对具体各参数的解析。
   *
   * @param paramIndex 参数的位置。例如，解析第一个参数时，{@code paramIndex} 为 0。
   */
  void parseParameter(CommandRegistryAccess commandRegistryAccess, SuggestedParser parser, int paramIndex, boolean suggestionsOnly) throws CommandSyntaxException;
}
