package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;

public interface MixedParamListParser extends SequentialParamListParser, NamedParamListParser {
  default void parseMixedParameters(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    reader.skipWhitespace();

    int sequentialParamsCount = 0;
    int namedParamsCount = 0;

    // when allows zero params, deal with empty
    if (sequentialParamsCount >= minSequentialParamsCount()) {
      parseContext.addSuggestion((context, builder) -> {
        if (builder.getRemaining().isEmpty()) {
          builder.suggest(Character.toString(terminateChar()));
        }
        return builder.buildFuture();
      });
    }

    if (reader.canRead() && reader.peek() == terminateChar()) {
      return;
    }

    while (true) {
      parseContext.clearSuggestion();

      final char KEY_VALUE_SEP = keyValueSeparatorChar();
      final char PARAMS_SEP = separatorChar();

      // 提供命名参数名称的建议
      final int cursorBeforeParamName = reader.getCursor();
      parseContext.addSuggestion((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(supportedParams().stream().filter(this::isValidParamName).map(s -> s + KEY_VALUE_SEP), suggestionsBuilder.createOffset(cursorBeforeParamName)));
      final String paramName = reader.readUnquotedString();
      final int cursorAfterParamName = reader.getCursor();

      // 输入完参数名称后，如果是有效的参数名称，建议等号
      if (supportedParams().contains(paramName) && isValidParamName(paramName)) {
        parseContext.setSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.createOffset(cursorAfterParamName).suggest(Character.toString(KEY_VALUE_SEP)).buildFuture());
      }
      reader.skipWhitespace();
      if (reader.canRead() && reader.peek() == KEY_VALUE_SEP) {
        // 读到等号，表示此时确实为命名参数，直接按照命名参数处理。
        checkParamNameValidity(paramName, reader, cursorBeforeParamName, cursorAfterParamName);

        reader.skipWhitespace();
        reader.skip(); // 跳过等号
        reader.skipWhitespace();

        parseContext.clearSuggestion();
        parseNamedParameter(paramName, parseContext);
        namedParamsCount++;
      } else {
        reader.setCursor(cursorBeforeParamName);
        reader.skipWhitespace();

        // 检查位置参数的数量是否符合要求
        if (sequentialParamsCount >= maxSequentialParamsCount()) {
          throw PARAMS_TOO_MANY.createWithContext(reader, sequentialParamsCount + 1, maxSequentialParamsCount());
        }

        // 解析位置参数
        parseSequentialParameter(parseContext, sequentialParamsCount);
        sequentialParamsCount++;
      }
      reader.skipWhitespace();

      // 提供逗号的参议

      final boolean hasMoreSequentialParams = sequentialParamsCount < maxSequentialParamsCount();
      final boolean satisfiedSequentialParams = sequentialParamsCount >= minSequentialParamsCount();
      final boolean hasMoreNamedParams = supportedParams().stream().anyMatch(this::isValidParamName);
      parseContext.addSuggestion((context, builder) -> {
        if (builder.getRemaining().isEmpty()) {
          if (hasMoreSequentialParams || hasMoreNamedParams) {
            builder.suggest(Character.toString(PARAMS_SEP));
          }
          if (satisfiedSequentialParams) {
            builder.suggest(Character.toString(terminateChar()));
          }
        }
        return builder.buildFuture();
      });

      if (reader.canRead() && reader.peek() == PARAMS_SEP) {
        reader.skip();
        reader.skipWhitespace();
        parseContext.clearSuggestion();

        if (reader.canRead() && reader.peek() == PARAMS_SEP) {
          break;
        }
      } else {
        break;
      }
    }

    // 解析完成后，检查最小参数数量
    if (sequentialParamsCount < minSequentialParamsCount()) {
      throw PARAMS_TOO_FEW.createWithContext(reader, sequentialParamsCount, minSequentialParamsCount());
    }
  }

  @Override
  default char separatorChar() {
    return ',';
  }

  @Override
  default char terminateChar() {
    return ')';
  }
}
