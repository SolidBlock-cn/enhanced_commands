package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;

public interface SequentialParamListParser {
  Dynamic2CommandExceptionType PARAMS_TOO_FEW = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.param_too_few", a, b));
  Dynamic2CommandExceptionType PARAMS_TOO_MANY = new Dynamic2CommandExceptionType((a, b) -> Text.translatable("enhanced_commands.param_too_many", a, b));

  /**
   * 最小参数数量。解析过程中，如果参数数量过少，则抛出错误。
   */
  @Contract(pure = true)
  default int minSequentialParamsCount() {
    return 0;
  }

  /**
   * 最大参数数量。解析过程中，如果参数数量过多，则抛出错误。
   */
  @Contract(pure = true)
  default int maxSequentialParamsCount() {
    return Integer.MAX_VALUE;
  }

  default void parseSequentialParameters(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    reader.skipWhitespace();

    int paramsCount = 0;

    // when allows zero params, deal with empty
    if (paramsCount >= minSequentialParamsCount()) {
      parseContext.addSuggestion((context, builder) -> {
        if (builder.getRemaining().isEmpty()) {
          builder.suggest(Character.toString(terminateChar()));
        }
        return builder.buildFuture();
      });
    }
    if (!reader.canRead() || reader.peek() != terminateChar()) while (true) {
      parseContext.clearSuggestion();

      // 检查参数数量

      if (paramsCount >= maxSequentialParamsCount()) {
        throw PARAMS_TOO_MANY.createWithContext(reader, paramsCount + 1, maxSequentialParamsCount());
      }

      parseSequentialParameter(parseContext, paramsCount);
      paramsCount++;
      reader.skipWhitespace();

      // 解析完参数后，提供建议

      final boolean hasMoreParams = paramsCount < maxSequentialParamsCount();
      final boolean satisfiedParams = paramsCount >= minSequentialParamsCount();
      parseContext.addSuggestion((context, builder) -> {
        if (builder.getRemaining().isEmpty()) {
          if (hasMoreParams) {
            builder.suggest(Character.toString(separatorChar()));
          }
          if (satisfiedParams) {
            builder.suggest(Character.toString(terminateChar()));
          }
        }
        return builder.buildFuture();
      });

      if (reader.canRead() && reader.peek() == separatorChar()) {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();

        if (reader.canRead() && reader.peek() == terminateChar()) {
          break;
        }
      } else {
        break;
      }
    }

    // 解析完成后，检查最小参数数量
    if (paramsCount < minSequentialParamsCount()) {
      throw PARAMS_TOO_FEW.createWithContext(reader, paramsCount, minSequentialParamsCount());
    }
  }

  /**
   * 解析特定位置的参数。实现时需要覆盖此方法以实现对具体各参数的解析。
   *
   * @param paramIndex 参数的位置。例如，解析第一个参数时，{@code paramIndex} 为 0。
   */
  void parseSequentialParameter(ParseContext<?> parseContext, int paramIndex) throws CommandSyntaxException;

  default char separatorChar() {
    return ',';
  }

  default char terminateChar() {
    return ')';
  }
}
