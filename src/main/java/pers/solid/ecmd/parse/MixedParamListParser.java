package pers.solid.ecmd.parse;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;

public interface MixedParamListParser extends SequentialParamListParser, NamedParamListParser {
  default void parseMixedParameters(ParseContext<?> parseContext) throws CommandSyntaxException {
    final StringReader reader = parseContext.reader();
    reader.skipWhitespace();

    int positionalParamsCount = 0;
    int namedParamsCount = 0;

    while (true) {
      parseContext.clearSuggestion();

      // 先尝试解析命名参数
      final char KEY_VALUE_SEP = keyValueSeparatorChar();
      final char PARAMS_SEP = separatorChar();

      // 提供参数名称的建议
      parseContext.addSuggestion((commandContext, suggestionsBuilder) -> CommandSource.suggestMatching(supportedParams().stream().filter(this::isValidParamName).map(s -> s + KEY_VALUE_SEP), suggestionsBuilder));

      final int cursorBeforeParamName = reader.getCursor();
      final String paramName = reader.readUnquotedString();
      final int cursorAfterParamName = reader.getCursor();

      if (namedParamsCount == 0 && paramName.isEmpty()) {
        // 考虑一个参数也没有的情况
        break;
      } else {
        namedParamsCount++;
      }

      // 输入完参数名称后，建议等号
      parseContext.setSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(Character.toString(KEY_VALUE_SEP)).buildFuture());
      if (reader.canRead() && reader.peek() == KEY_VALUE_SEP) {
        checkParamNameValidity(paramName, reader, cursorBeforeParamName, cursorAfterParamName);

        reader.skipWhitespace();

        reader.skip();
        reader.skipWhitespace();

        parseContext.clearSuggestion();
        parseNamedParameter(paramName, parseContext);
        if (supportedParams().stream().anyMatch(this::isValidParamName)) {
          // 如果后面没有可用的参数名称了，就不再逗号。
          parseContext.setSuggestion((commandContext, suggestionsBuilder) -> suggestionsBuilder.suggest(Character.toString(PARAMS_SEP)).buildFuture());
        } else {
          parseContext.clearSuggestion();
        }
        if (reader.canRead() && reader.peek() == PARAMS_SEP) {
          reader.skip();
          reader.skipWhitespace();
          parseContext.clearSuggestion();
        } else {
          break;
        }
      } else {
        reader.setCursor(cursorBeforeParamName);
        reader.skipWhitespace();

        // 解析位置参数

        parsePositionalParameter(parseContext, positionalParamsCount);
        positionalParamsCount++;
        reader.skipWhitespace();

        if (!reader.canRead()) {
          return;
        } else if (reader.peek() == separatorChar()) {
          reader.skip();
          reader.skipWhitespace();
          parseContext.clearSuggestion();
        } else {
          throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
        }
      }
    }
  }

  @Override
  default char separatorChar() {
    return ',';
  }
}
